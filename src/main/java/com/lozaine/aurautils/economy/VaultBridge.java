package com.lozaine.aurautils.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

/**
 * Isolated Vault references so {@link EconomyService} can load when Vault is absent.
 */
final class VaultBridge implements EconomyHandler {

    private final Economy economy;

    private VaultBridge(Economy economy) {
        this.economy = economy;
    }

    static EconomyHandler tryHook(ServicesManager services) {
        if (services == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> registration = services.getRegistration(Economy.class);
        if (registration == null) {
            return null;
        }
        Economy provider = registration.getProvider();
        if (provider == null || !provider.isEnabled()) {
            return null;
        }
        return new VaultBridge(provider);
    }

    @Override
    public String name() {
        String name = economy.getName();
        return name == null || name.isBlank() ? "Vault" : name;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        ensureAccount(player);
        return economy.has(player, amount);
    }

    @Override
    public double balance(OfflinePlayer player) {
        ensureAccount(player);
        return economy.getBalance(player);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        ensureAccount(player);
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        ensureAccount(player);
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        try {
            String formatted = economy.format(amount);
            return formatted != null ? formatted : Double.toString(amount);
        } catch (Exception ignored) {
            return Double.toString(amount);
        }
    }

    private void ensureAccount(OfflinePlayer player) {
        if (player == null) {
            return;
        }
        try {
            if (!economy.hasAccount(player)) {
                economy.createPlayerAccount(player);
            }
        } catch (Exception ignored) {
            // some providers reject account creation
        }
    }
}
