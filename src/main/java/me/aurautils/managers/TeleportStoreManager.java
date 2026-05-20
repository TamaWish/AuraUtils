package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public class TeleportStoreManager {

    private final AuraUtils plugin;
    private final File warpsFile;
    private final File homesFile;

    private final Map<String, Location> warps = new TreeMap<>();
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public TeleportStoreManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        warps.clear();
        homes.clear();

        YamlConfiguration warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        ConfigurationSection warpsSection = warpsConfig.getConfigurationSection("warps");
        if (warpsSection != null) {
            for (String name : warpsSection.getKeys(false)) {
                Location location = readLocation(warpsSection.getConfigurationSection(name));
                if (location != null) {
                    warps.put(normalize(name), location);
                }
            }
        }

        YamlConfiguration homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        ConfigurationSection homesSection = homesConfig.getConfigurationSection("homes");
        if (homesSection != null) {
            for (String playerKey : homesSection.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(playerKey);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                ConfigurationSection playerHomes = homesSection.getConfigurationSection(playerKey);
                if (playerHomes == null) {
                    continue;
                }

                Map<String, Location> playerMap = new TreeMap<>();
                for (String homeName : playerHomes.getKeys(false)) {
                    Location location = readLocation(playerHomes.getConfigurationSection(homeName));
                    if (location != null) {
                        playerMap.put(normalize(homeName), location);
                    }
                }
                if (!playerMap.isEmpty()) {
                    homes.put(playerId, playerMap);
                }
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        saveWarps();
        saveHomes();
    }

    public Set<String> getWarpNames() {
        return new TreeSet<>(warps.keySet());
    }

    public Location getWarp(String name) {
        Location location = warps.get(normalize(name));
        return location == null ? null : location.clone();
    }

    public void setWarp(String name, Location location) {
        warps.put(normalize(name), location.clone());
    }

    public boolean deleteWarp(String name) {
        return warps.remove(normalize(name)) != null;
    }

    public List<String> getHomeNames(UUID playerId) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new TreeSet<>(playerHomes.keySet()));
    }

    public Location getHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return null;
        }
        Location location = playerHomes.get(normalize(name));
        return location == null ? null : location.clone();
    }

    public void setHome(UUID playerId, String name, Location location) {
        homes.computeIfAbsent(playerId, ignored -> new TreeMap<>())
                .put(normalize(name), location.clone());
    }

    public boolean deleteHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return false;
        }
        boolean removed = playerHomes.remove(normalize(name)) != null;
        if (playerHomes.isEmpty()) {
            homes.remove(playerId);
        }
        return removed;
    }

    private void saveWarps() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection warpsSection = config.createSection("warps");
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            writeLocation(warpsSection.createSection(entry.getKey()), entry.getValue());
        }
        try {
            config.save(warpsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save warps.yml: " + exception.getMessage());
        }
    }

    private void saveHomes() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection homesSection = config.createSection("homes");
        for (Map.Entry<UUID, Map<String, Location>> playerEntry : homes.entrySet()) {
            ConfigurationSection playerSection = homesSection.createSection(playerEntry.getKey().toString());
            for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                writeLocation(playerSection.createSection(homeEntry.getKey()), homeEntry.getValue());
            }
        }
        try {
            config.save(homesFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save homes.yml: " + exception.getMessage());
        }
    }

    private void writeLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    private String normalize(String name) {
        return name.toLowerCase();
    }
}