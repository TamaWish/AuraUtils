package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-wide settings (keep inventory, persisted spawns). Spawns and gamerules are
 * re-applied when worlds load so resource worlds recreated by ResourceWorldResetter
 * keep AuraUtils configuration.
 */
public class ServerSettingsManager {

    private final AuraUtils plugin;
    private final File spawnsFile;
    private final Map<String, SpawnData> savedSpawns = new LinkedHashMap<>();

    public ServerSettingsManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.spawnsFile = new File(plugin.getDataFolder(), "server-spawns.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        savedSpawns.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(spawnsFile);
        ConfigurationSection spawnsSection = config.getConfigurationSection("spawns");
        if (spawnsSection == null) {
            return;
        }

        for (String worldName : spawnsSection.getKeys(false)) {
            ConfigurationSection section = spawnsSection.getConfigurationSection(worldName);
            if (section != null) {
                savedSpawns.put(worldName, SpawnData.fromSection(worldName, section));
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection spawnsSection = config.createSection("spawns");
        for (Map.Entry<String, SpawnData> entry : savedSpawns.entrySet()) {
            entry.getValue().write(spawnsSection.createSection(entry.getKey()));
        }
        try {
            config.save(spawnsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save server-spawns.yml: " + exception.getMessage());
        }
    }

    public boolean isKeepInventoryEnabled() {
        return plugin.getConfig().getBoolean("server.keep-inventory", false);
    }

    public void setKeepInventoryEnabled(boolean enabled) {
        plugin.getConfig().set("server.keep-inventory", enabled);
        plugin.saveConfig();
        applyKeepInventoryToAllWorlds();
    }

    public void setWorldSpawn(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.setSpawnLocation(location);
        savedSpawns.put(world.getName(), SpawnData.fromLocation(location));
        save();
    }

    public Location getSavedSpawn(String worldName) {
        SpawnData data = savedSpawns.get(worldName);
        return data == null ? null : data.toLocation();
    }

    public void applyToWorld(World world) {
        if (world == null) {
            return;
        }
        applyKeepInventory(world);
        SpawnData saved = savedSpawns.get(world.getName());
        if (saved != null) {
            world.setSpawnLocation(saved.toLocation(world));
        }
    }

    public void applyToAllWorlds() {
        for (World world : plugin.getServer().getWorlds()) {
            applyToWorld(world);
        }
    }

    private void applyKeepInventoryToAllWorlds() {
        for (World world : plugin.getServer().getWorlds()) {
            applyKeepInventory(world);
        }
    }

    private void applyKeepInventory(World world) {
        world.setGameRule(GameRule.KEEP_INVENTORY, isKeepInventoryEnabled());
    }

    private record SpawnData(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        static SpawnData fromLocation(Location location) {
            return new SpawnData(
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()
            );
        }

        static SpawnData fromSection(String worldName, ConfigurationSection section) {
            return new SpawnData(
                    worldName,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch")
            );
        }

        void write(ConfigurationSection section) {
            section.set("world", worldName);
            section.set("x", x);
            section.set("y", y);
            section.set("z", z);
            section.set("yaw", yaw);
            section.set("pitch", pitch);
        }

        Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            return world == null ? null : toLocation(world);
        }

        Location toLocation(World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }
    }
}
