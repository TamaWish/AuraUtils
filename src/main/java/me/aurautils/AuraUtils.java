package me.aurautils;

import me.aurautils.commands.*;
import me.aurautils.listeners.*;
import me.aurautils.listeners.paper.PaperMoveListener;
import me.aurautils.managers.AsyncRtpEngine;
import me.aurautils.managers.BackManager;
import me.aurautils.managers.HomeManager;
import me.aurautils.managers.MessagesManager;
import me.aurautils.managers.PlayerDataManager;
import me.aurautils.managers.RtpCooldownManager;
import me.aurautils.managers.ServerSettingsManager;
import me.aurautils.managers.TeleportHelper;
import me.aurautils.managers.TpaManager;
import me.aurautils.managers.WarpManager;
import me.aurautils.menus.MenuManager;
import me.aurautils.platform.PlatformAdapter;
import me.aurautils.platform.PlatformFactory;
import me.aurautils.util.MessagePlaceholders;
import me.aurautils.util.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraUtils extends JavaPlugin {

    private static AuraUtils instance;
    private PlatformAdapter platform;
    private MessagesManager messages;
    private PlayerDataManager playerDataManager;
    private TpaManager tpaManager;
    private WarpManager warpManager;
    private HomeManager homeManager;
    private BackManager backManager;
    private MenuManager menuManager;
    private RtpCooldownManager rtpCooldownManager;
    private ServerSettingsManager serverSettingsManager;
    private TeleportHelper teleportHelper;
    private AsyncRtpEngine asyncRtpEngine;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        platform = PlatformFactory.create(this);
        getLogger().info("Platform: " + platform.getPlatformName());

        messages = new MessagesManager(this);
        messages.load();

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
        serverSettingsManager = new ServerSettingsManager(this);
        serverSettingsManager.load();
        serverSettingsManager.applyToAllWorlds();

        teleportHelper = new TeleportHelper(this);
        asyncRtpEngine = new AsyncRtpEngine(this, teleportHelper);

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
        registerCommand("spawn", new SpawnCommand(this));
        registerCommand("god", new GodCommand(this));
        registerCommand("fly", new FlyCommand(this));
        registerCommand("nofall", new NoFallCommand(this));
        registerCommand("nohunger", new NoHungerCommand(this));
        registerCommand("heal", new HealCommand(this));
        registerCommand("feed", new FeedCommand(this));
        registerCommand("rtp", new RtpCommand(this));
        registerCommand("setspawn", new SetSpawnCommand(this));
        registerCommand("keepinventory", new KeepInventoryCommand(this));
        registerCommand("aura", new AuraCommand(this));
        registerCommand("auracanceltp", new CancelTpCommand(this));

        getServer().getPluginManager().registerEvents(new GodModeListener(this), this);
        getServer().getPluginManager().registerEvents(new FlyListener(this), this);
        getServer().getPluginManager().registerEvents(new NoFallListener(this), this);
        getServer().getPluginManager().registerEvents(new NoHungerListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new BackListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldSettingsListener(this), this);

        if (platform.usesEventBasedMovementDetection()) {
            getServer().getPluginManager().registerEvents(new PaperMoveListener(this, teleportHelper), this);
        }

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
            try {
                warpManager.save();
            } catch (Exception exception) {
                getLogger().severe("Failed to save warps on disable: " + exception.getMessage());
            }
        }
        if (homeManager != null) {
            try {
                homeManager.save();
            } catch (Exception exception) {
                getLogger().severe("Failed to save homes on disable: " + exception.getMessage());
            }
        }
        if (playerDataManager != null) {
            playerDataManager.flushSave();
        }
        if (serverSettingsManager != null) {
            serverSettingsManager.save();
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

    public MessagesManager getMessages() {
        return messages;
    }

    public void send(CommandSender sender, String key) {
        messages.send(sender, key);
    }

    public void send(CommandSender sender, String key, MessagePlaceholders placeholders) {
        messages.send(sender, key, placeholders);
    }

    public void sendToggle(CommandSender sender, String key, boolean enabled) {
        messages.sendToggle(sender, key, enabled);
    }

    public void send(CommandSender sender, String key, TagResolver additionalResolvers) {
        messages.send(sender, key, additionalResolvers);
    }

    public boolean isFeatureEnabled(String featureKey) {
        return getConfig().getBoolean("features." + featureKey, true);
    }

    public boolean requireFeature(CommandSender sender, String featureKey) {
        if (isFeatureEnabled(featureKey)) {
            return true;
        }

        send(sender, "feature.disabled");
        return false;
    }

    public PlatformAdapter getPlatform() {
        return platform;
    }

    public TeleportHelper getTeleportHelper() {
        return teleportHelper;
    }

    public AsyncRtpEngine getAsyncRtpEngine() {
        return asyncRtpEngine;
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

    public ServerSettingsManager getServerSettingsManager() {
        return serverSettingsManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        messages.load();
        if (serverSettingsManager != null) {
            serverSettingsManager.applyToAllWorlds();
        }
    }

    public static String colorize(String text) {
        return MessageUtil.colorize(text);
    }
}