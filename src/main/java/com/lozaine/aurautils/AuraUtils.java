package com.lozaine.aurautils;

import com.lozaine.aurautils.commands.*;
import com.lozaine.aurautils.listeners.*;
import com.lozaine.aurautils.managers.BackManager;
import com.lozaine.aurautils.managers.PlayerDataManager;
import com.lozaine.aurautils.managers.TeleportStoreManager;
import com.lozaine.aurautils.managers.TeleportHelper;
import com.lozaine.aurautils.managers.TpaManager;
import com.lozaine.aurautils.menus.MenuManager;
import com.lozaine.aurautils.util.MessageService;
import com.lozaine.aurautils.util.SchedulerHelper;
import com.lozaine.aurautils.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AuraUtils extends JavaPlugin {

    private static AuraUtils instance;
    private SchedulerHelper scheduler;
    private PlayerDataManager playerDataManager;
    private TpaManager tpaManager;
    private TeleportStoreManager teleportStoreManager;
    private BackManager backManager;
    private TeleportHelper teleportHelper;
    private MenuManager menuManager;
    private MessageService messageService;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Cross-platform scheduler (Spigot / Paper / Folia)
        scheduler = new SchedulerHelper(this);
        messageService = new MessageService(this);
        updateChecker = new UpdateChecker(this);

        // bStats metrics (plugin id from https://bstats.org)
        setupMetrics();

        // Managers
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();
        tpaManager = new TpaManager(this);
        teleportStoreManager = new TeleportStoreManager(this);
        teleportStoreManager.load();
        backManager = new BackManager();
        teleportHelper = new TeleportHelper(this);
        menuManager = new MenuManager(this);

        // Commands
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpaccept").setExecutor(new TpaAcceptCommand(this));
        getCommand("tpadeny").setExecutor(new TpaDenyCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("delwarp").setExecutor(new DelWarpCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        getCommand("back").setExecutor(new BackCommand(this));
        getCommand("menu").setExecutor(new MenuCommand(this));
        getCommand("god").setExecutor(new GodCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("nofall").setExecutor(new NoFallCommand(this));
        getCommand("nohunger").setExecutor(new NoHungerCommand(this));
        getCommand("rtp").setExecutor(new RtpCommand(this));
        getCommand("aura").setExecutor(new AuraCommand(this));
        getCommand("tpacancel").setExecutor(new TpaCancelCommand(this));
        TpaTrustCommand trustCmd = new TpaTrustCommand(this);
        getCommand("tpatrust").setExecutor(trustCmd);
        getCommand("tpatrust").setTabCompleter(trustCmd);
        TpaTrustCommand untrustCmd = new TpaTrustCommand(this, true);
        getCommand("tpauntrust").setExecutor(untrustCmd);
        getCommand("tpauntrust").setTabCompleter(untrustCmd);

        // Listeners
        getServer().getPluginManager().registerEvents(new GodModeListener(this), this);
        getServer().getPluginManager().registerEvents(new FlyListener(this), this);
        getServer().getPluginManager().registerEvents(new NoFallListener(this), this);
        getServer().getPluginManager().registerEvents(new NoHungerListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new BackListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportDamageListener(this), this);

        for (Player player : getServer().getOnlinePlayers()) {
            playerDataManager.applyTo(player);
        }

        getLogger().info("AuraUtils v" + getDescription().getVersion() + " enabled!");
        updateChecker.checkAsync();
    }

    /**
     * Registers bStats with custom charts. Chart IDs must also be created on
     * https://bstats.org under this plugin (id 33574) → Edit → Custom Charts.
     */
    private void setupMetrics() {
        int pluginId = 33574;
        Metrics metrics = new Metrics(this, pluginId);

        // --- Config: SimplePie (one value per server) ---
        metrics.addCustomChart(new SimplePie("teleport_countdown", () ->
                String.valueOf(getConfig().getInt("teleport.countdown", 5))));

        metrics.addCustomChart(new SimplePie("countdown_display", () ->
                getConfig().getString("teleport.countdown-display", "both").toLowerCase()));

        metrics.addCustomChart(new SimplePie("cancel_on_move", () ->
                String.valueOf(getConfig().getBoolean("teleport.cancel-on-move", true))));

        metrics.addCustomChart(new SimplePie("cancel_on_damage", () ->
                String.valueOf(getConfig().getBoolean("teleport.cancel-on-damage", false))));

        metrics.addCustomChart(new SimplePie("teleport_sound", () ->
                String.valueOf(getConfig().getBoolean("teleport.sound", true))));

        metrics.addCustomChart(new SimplePie("rtp_countdown", () ->
                String.valueOf(getConfig().getInt("rtp.countdown", 5))));

        metrics.addCustomChart(new SimplePie("tpa_timeout", () ->
                String.valueOf(getConfig().getInt("tpa.timeout", 60))));

        // --- Online feature usage: AdvancedPie (counts per category) ---
        metrics.addCustomChart(new AdvancedPie("online_feature_toggles", () -> {
            Map<String, Integer> map = new HashMap<>();
            if (playerDataManager == null) {
                return map;
            }
            int god = 0, fly = 0, noFall = 0, noHunger = 0;
            for (Player player : getServer().getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                if (playerDataManager.isGod(id)) god++;
                if (playerDataManager.isFly(id)) fly++;
                if (playerDataManager.isNoFall(id)) noFall++;
                if (playerDataManager.isNoHunger(id)) noHunger++;
            }
            // Only report slices that are in use (keeps charts readable)
            if (god > 0) map.put("God", god);
            if (fly > 0) map.put("Fly", fly);
            if (noFall > 0) map.put("NoFall", noFall);
            if (noHunger > 0) map.put("NoHunger", noHunger);
            return map;
        }));

        // --- Storage size: SingleLineChart ---
        metrics.addCustomChart(new SingleLineChart("warp_count", () -> {
            if (teleportStoreManager == null) {
                return 0;
            }
            return teleportStoreManager.getWarps().size();
        }));
    }

    @Override
    public void onDisable() {
        // Cancel pending TPA timers first
        if (tpaManager != null) {
            tpaManager.cancelAll();
        }
        if (teleportHelper != null) {
            teleportHelper.cancelAll();
        }

        // Flush player data synchronously — never schedule tasks while disabled
        if (playerDataManager != null) {
            playerDataManager.prepareShutdown();
        }

        // Teleport store is already synchronous
        if (teleportStoreManager != null) {
            teleportStoreManager.save();
        }

        // Cancel any remaining FoliaLib tasks
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("AuraUtils disabled.");
    }

    public static AuraUtils getInstance() { return instance; }
    public SchedulerHelper getScheduler() { return scheduler; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public TpaManager getTpaManager() { return tpaManager; }
    public TeleportStoreManager getTeleportStoreManager() { return teleportStoreManager; }
    public BackManager getBackManager() { return backManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public TeleportHelper getTeleportHelper() { return teleportHelper; }
    public MessageService messages() { return messageService; }
    public UpdateChecker updateChecker() { return updateChecker; }

    public void reloadPluginConfig() {
        reloadConfig();
        if (messageService == null) {
            messageService = new MessageService(this);
        } else {
            messageService.reload();
        }
        if (updateChecker == null) {
            updateChecker = new UpdateChecker(this);
        }
        updateChecker.checkAsync();
    }

    /** Colorize a message using the config prefix. */
    public String prefix(String msg) {
        String raw = getConfig().getString("prefix", "&8[&bAura&8] &r") + msg;
        return colorize(raw);
    }

    public static String colorize(String s) {
        return s.replace("&", "\u00A7");
    }
}
