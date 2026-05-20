package me.aurautils;

import me.aurautils.commands.*;
import me.aurautils.listeners.*;
import me.aurautils.managers.BackManager;
import me.aurautils.managers.PlayerDataManager;
import me.aurautils.managers.TeleportStoreManager;
import me.aurautils.managers.TpaManager;
import me.aurautils.menus.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraUtils extends JavaPlugin {

    private static AuraUtils instance;
    private PlayerDataManager playerDataManager;
    private TpaManager tpaManager;
    private TeleportStoreManager teleportStoreManager;
    private BackManager backManager;
    
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Managers
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();
        tpaManager = new TpaManager(this);
        teleportStoreManager = new TeleportStoreManager(this);
        teleportStoreManager.load();
        backManager = new BackManager();
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
        getCommand("damage").setExecutor(new DamageCommand(this));
        getCommand("rtp").setExecutor(new RtpCommand(this));
        getCommand("aura").setExecutor(new AuraCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new GodModeListener(this), this);
        getServer().getPluginManager().registerEvents(new FlyListener(this), this);
        getServer().getPluginManager().registerEvents(new NoFallListener(this), this);
        getServer().getPluginManager().registerEvents(new NoHungerListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageMultiplierListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new BackListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        for (Player player : getServer().getOnlinePlayers()) {
            playerDataManager.applyTo(player);
        }

        getLogger().info("AuraUtils v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) tpaManager.cancelAll();
        if (teleportStoreManager != null) teleportStoreManager.save();
        if (playerDataManager != null) playerDataManager.save();
        
        getLogger().info("AuraUtils disabled.");
    }

    public static AuraUtils getInstance() { return instance; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public TpaManager getTpaManager() { return tpaManager; }
    public TeleportStoreManager getTeleportStoreManager() { return teleportStoreManager; }
    public BackManager getBackManager() { return backManager; }
    
    public MenuManager getMenuManager() { return menuManager; }

    /** Colorize a message using the config prefix. */
    public String prefix(String msg) {
        String raw = getConfig().getString("prefix", "&8[&bAura&8] &r") + msg;
        return colorize(raw);
    }

    public static String colorize(String s) {
        return s.replace("&", "\u00A7");
    }
}
