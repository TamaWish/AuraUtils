package me.aurautils;

import me.aurautils.commands.*;
import me.aurautils.listeners.*;
import me.aurautils.managers.BackManager;
import me.aurautils.managers.HomeManager;
import me.aurautils.managers.PlayerDataManager;
import me.aurautils.managers.RtpCooldownManager;
import me.aurautils.managers.TpaManager;
import me.aurautils.managers.WarpManager;
import me.aurautils.menus.MenuManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraUtils extends JavaPlugin {

    private static AuraUtils instance;
    private PlayerDataManager playerDataManager;
    private TpaManager tpaManager;
    private WarpManager warpManager;
    private HomeManager homeManager;
    private BackManager backManager;
    private MenuManager menuManager;
    private RtpCooldownManager rtpCooldownManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();
        tpaManager = new TpaManager(this);
        warpManager = new WarpManager(this);
        warpManager.load();
        homeManager = new HomeManager(this);
        homeManager.load();
        backManager = new BackManager();
        menuManager = new MenuManager(this);
        rtpCooldownManager = new RtpCooldownManager();

        registerCommand("tpa", new TpaCommand(this));
        registerCommand("tpahere", new TpaHereCommand(this));
        registerCommand("tpaccept", new TpaAcceptCommand(this));
        registerCommand("tpadeny", new TpaDenyCommand(this));
        registerCommand("warp", new WarpCommand(this));
        registerCommand("setwarp", new SetWarpCommand(this));
        registerCommand("delwarp", new DelWarpCommand(this));
        registerCommand("home", new HomeCommand(this));
        registerCommand("sethome", new SetHomeCommand(this));
        registerCommand("delhome", new DelHomeCommand(this));
        registerCommand("back", new BackCommand(this));
        registerCommand("menu", new MenuCommand(this));
        registerCommand("god", new GodCommand(this));
        registerCommand("fly", new FlyCommand(this));
        registerCommand("nofall", new NoFallCommand(this));
        registerCommand("nohunger", new NoHungerCommand(this));
        registerCommand("rtp", new RtpCommand(this));
        registerCommand("aura", new AuraCommand(this));

        getServer().getPluginManager().registerEvents(new GodModeListener(this), this);
        getServer().getPluginManager().registerEvents(new FlyListener(this), this);
        getServer().getPluginManager().registerEvents(new NoFallListener(this), this);
        getServer().getPluginManager().registerEvents(new NoHungerListener(this), this);
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
        if (tpaManager != null) {
            tpaManager.cancelAll();
        }
        if (warpManager != null) {
            warpManager.save();
        }
        if (homeManager != null) {
            homeManager.save();
        }
        if (playerDataManager != null) {
            playerDataManager.flushSave();
        }

        getLogger().info("AuraUtils disabled.");
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command not defined in plugin.yml: " + name);
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor commandExecutor) {
            command.setExecutor(commandExecutor);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public static AuraUtils getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public BackManager getBackManager() {
        return backManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public RtpCooldownManager getRtpCooldownManager() {
        return rtpCooldownManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
    }

    public String prefix(String msg) {
        String raw = getConfig().getString("prefix", "&8[&bAura&8] &r") + msg;
        return colorize(raw);
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
