package me.aurautils;

import me.aurautils.commands.*;
import me.aurautils.listeners.*;
import me.aurautils.managers.BackManager;
import me.aurautils.managers.PlayerDataManager;
import me.aurautils.managers.TeleportStoreManager;
import me.aurautils.managers.TpaManager;
import me.aurautils.menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public final class AuraUtils extends JavaPlugin {

    private static AuraUtils instance;
    private PlayerDataManager playerDataManager;
    private TpaManager tpaManager;
    private TeleportStoreManager teleportStoreManager;
    private BackManager backManager;
    private Set<Material> damageWeapons = new HashSet<>();
    private final Map<Material, Double> damageWeaponMultipliers = new HashMap<>();
    
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
        loadDamageWeapons();

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
        getCommand("weapondamage").setExecutor(new me.aurautils.commands.WeaponDamageCommand(this));
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

    public void reloadPluginConfig() {
        reloadConfig();
        loadDamageWeapons();
    }

    public Set<Material> getDamageWeapons() { return damageWeapons; }

    public void setWeaponDamageMultiplier(Material material, Double multiplier) {
        if (material == null) return;
        if (multiplier == null) {
            // remove
            getConfig().set("damage-multipliers." + material.name(), null);
        } else {
            getConfig().set("damage-multipliers." + material.name(), multiplier);
        }
        saveConfig();
        loadDamageWeapons();
    }

    public boolean isDamageWeapon(Material material) {
        return material != null && damageWeapons.contains(material);
    }

    private void loadDamageWeapons() {
        damageWeapons.clear();
        damageWeaponMultipliers.clear();

        // New: load per-weapon multipliers from config if present.
        ConfigurationSection multSection = getConfig().getConfigurationSection("damage-multipliers");
        if (multSection != null && !multSection.getKeys(false).isEmpty()) {
            for (String materialName : multSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(materialName);
                    double val = multSection.getDouble(materialName, getConfig().getDouble("damage-multiplier-default", 1.0));
                    damageWeaponMultipliers.put(mat, val);
                    damageWeapons.add(mat);
                } catch (IllegalArgumentException exception) {
                    getLogger().warning("Ignoring invalid damage weapon material in damage-multipliers: " + materialName);
                }
            }
            return; // we loaded multipliers explicitly, done.
        }

        // Backwards-compatible: load list of weapons (no per-weapon multiplier)
        List<String> configuredWeapons = getConfig().getStringList("damage-weapons");
        if (configuredWeapons != null && !configuredWeapons.isEmpty()) {
            for (String materialName : configuredWeapons) {
                try {
                    damageWeapons.add(Material.valueOf(materialName));
                } catch (IllegalArgumentException exception) {
                    getLogger().warning("Ignoring invalid damage weapon material: " + materialName);
                }
            }
        }

        if (damageWeapons.isEmpty()) {
            damageWeapons.add(Material.WOODEN_SWORD);
            damageWeapons.add(Material.STONE_SWORD);
            damageWeapons.add(Material.IRON_SWORD);
            damageWeapons.add(Material.GOLDEN_SWORD);
            damageWeapons.add(Material.DIAMOND_SWORD);
            damageWeapons.add(Material.NETHERITE_SWORD);
            damageWeapons.add(Material.WOODEN_AXE);
            damageWeapons.add(Material.STONE_AXE);
            damageWeapons.add(Material.IRON_AXE);
            damageWeapons.add(Material.GOLDEN_AXE);
            damageWeapons.add(Material.DIAMOND_AXE);
            damageWeapons.add(Material.NETHERITE_AXE);
            damageWeapons.add(Material.BOW);
            damageWeapons.add(Material.CROSSBOW);
            damageWeapons.add(Material.TRIDENT);
        }
    }

    public Double getWeaponDamageMultiplier(Material material) {
        return damageWeaponMultipliers.get(material);
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
