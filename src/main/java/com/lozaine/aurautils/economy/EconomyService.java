package com.lozaine.aurautils.economy;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;

/**
 * Optional Vault economy hook. Missing Vault or a missing provider leaves every
 * action free. Costs of {@code 0} are free even when hooked.
 */
public final class EconomyService implements Listener {

    private static final String VAULT_ECONOMY_SERVICE = "net.milkbowl.vault.economy.Economy";
    private static final String VAULT_BRIDGE_CLASS = "com.lozaine.aurautils.economy.VaultBridge";

    private final AuraUtils plugin;
    private volatile EconomyHandler handler;
    private boolean loggedMissingVault;
    private boolean loggedMissingProvider;

    public EconomyService(AuraUtils plugin) {
        this.plugin = plugin;
        hook();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        hook();
    }

    public boolean isHooked() {
        return handler != null;
    }

    public String providerName() {
        EconomyHandler hooked = handler;
        return hooked != null ? hooked.name() : "";
    }

    public double cost(EconomyAction action) {
        return EconomyCosts.cost(plugin.getConfig(), action);
    }

    public boolean isFree(Player player, EconomyAction action) {
        if (handler == null) {
            return true;
        }
        return EconomyCosts.isFree(plugin.getConfig(), action,
                player == null ? node -> false : player::hasPermission);
    }

    public String format(double amount) {
        EconomyHandler hooked = handler;
        if (hooked != null) {
            return hooked.format(amount);
        }
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    /**
     * GUI lore line when this player would be charged, otherwise {@code null}.
     */
    public String costLine(Player player, EconomyAction action) {
        if (isFree(player, action)) {
            return null;
        }
        return plugin.messages().get("economy.cost-lore", "cost", format(cost(action)));
    }

    /**
     * {@code true} when the player can pay (or the action is free). Sends
     * {@code economy.cannot-afford} when they cannot.
     */
    public boolean ensureCanPay(Player player, EconomyAction action) {
        if (isFree(player, action)) {
            return true;
        }
        if (player == null || !player.isOnline()) {
            return false;
        }
        EconomyHandler hooked = handler;
        if (hooked == null) {
            return true;
        }
        double price = cost(action);
        if (hooked.has(player, price)) {
            return true;
        }
        sendMoneyMessage(player, "economy.cannot-afford", action, hooked, price);
        return false;
    }

    /**
     * Withdraws when a cost is due. Free actions succeed without a transaction.
     * Sends cannot-afford / charge-failed when the player cannot pay.
     * Call {@link #announceCharge} after the action succeeds so chat names
     * the amount and the action ({@code /home}, {@code /setwarp}, …).
     */
    public boolean tryBeginCharge(Player player, EconomyAction action) {
        if (isFree(player, action)) {
            return true;
        }
        if (!ensureCanPay(player, action)) {
            return false;
        }
        return charge(player, action);
    }

    /**
     * Refund a previous {@link #tryBeginCharge} after a failed teleport.
     * No-op when the action is free or economy is unhooked.
     */
    public void abortCharge(Player player, EconomyAction action) {
        EconomyHandler hooked = handler;
        if (hooked == null || player == null || action == null) {
            return;
        }
        double price = EconomyCosts.cost(plugin.getConfig(), action);
        if (price <= 0) {
            return;
        }
        if (!hooked.deposit(player, price)) {
            plugin.getLogger().warning("Vault refund failed for " + player.getName()
                    + " (" + hooked.format(price) + ").");
            return;
        }
        if (EconomyCosts.notify(plugin.getConfig()) && player.isOnline()) {
            sendMoneyMessage(player, "economy.refunded", action, hooked, price);
        }
    }

    /**
     * Chat after a paid action succeeds. No-op when the action was free,
     * {@code economy.notify} is false, or nothing was charged.
     */
    public void announceCharge(Player player, EconomyAction action) {
        if (!EconomyCosts.notify(plugin.getConfig()) || player == null || !player.isOnline() || action == null) {
            return;
        }
        if (isFree(player, action)) {
            return;
        }
        EconomyHandler hooked = handler;
        if (hooked == null) {
            return;
        }
        double price = cost(action);
        if (price <= 0) {
            return;
        }
        sendMoneyMessage(player, "economy.charged", action, hooked, price);
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (handler != null) {
            return;
        }
        if (VAULT_ECONOMY_SERVICE.equals(event.getProvider().getService().getName())) {
            hook();
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (handler != null) {
            return;
        }
        if ("Vault".equals(event.getPlugin().getName())) {
            hook();
        }
    }

    private boolean charge(Player player, EconomyAction action) {
        EconomyHandler hooked = handler;
        if (hooked == null || player == null) {
            return true;
        }
        double price = cost(action);
        if (price <= 0) {
            return true;
        }
        if (!hooked.withdraw(player, price)) {
            plugin.getLogger().warning("Vault withdraw failed for " + player.getName()
                    + " (" + hooked.format(price) + ").");
            if (player.isOnline()) {
                sendMoneyMessage(player, "economy.charge-failed", action, hooked, price);
            }
            return false;
        }
        return true;
    }

    private String actionLabel(EconomyAction action) {
        if (action == null) {
            return "";
        }
        return plugin.messages().get("economy.action-" + action.configKey());
    }

    private void sendMoneyMessage(Player player, String key, EconomyAction action,
                                  EconomyHandler hooked, double price) {
        String formatted = hooked.format(price);
        plugin.messages().send(player, key,
                "cost", formatted,
                "amount", formatted,
                "action", actionLabel(action),
                "balance", hooked.format(hooked.balance(player)));
    }

    private void hook() {
        if (!EconomyCosts.enabled(plugin.getConfig())) {
            handler = null;
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            if (!loggedMissingVault) {
                plugin.getLogger().info("Vault not found — economy costs are disabled until Vault and an economy plugin are installed.");
                loggedMissingVault = true;
            }
            handler = null;
            return;
        }
        try {
            Class<?> type = Class.forName(VAULT_BRIDGE_CLASS);
            var connect = type.getDeclaredMethod("tryHook", org.bukkit.plugin.ServicesManager.class);
            connect.setAccessible(true);
            EconomyHandler hooked = (EconomyHandler) connect.invoke(null, plugin.getServer().getServicesManager());
            if (hooked == null) {
                if (!loggedMissingProvider) {
                    plugin.getLogger().info("Vault is installed but no economy provider is registered — costs stay free.");
                    loggedMissingProvider = true;
                }
                handler = null;
                return;
            }
            EconomyHandler previous = handler;
            if (previous == null || !hooked.name().equals(previous.name())) {
                plugin.getLogger().info("Hooked Vault economy: " + hooked.name());
            }
            handler = hooked;
        } catch (NoClassDefFoundError | ReflectiveOperationException exception) {
            plugin.getLogger().warning("Could not hook Vault economy: " + exception.getMessage());
            handler = null;
        }
    }
}
