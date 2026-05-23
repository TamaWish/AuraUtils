package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.util.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class WarpManager {

    private final AuraUtils plugin;
    private final File warpsFile;
    private final Map<String, Location> warps = new TreeMap<>();

    public WarpManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        warps.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
        ConfigurationSection warpsSection = config.getConfigurationSection("warps");
        if (warpsSection == null) {
            return;
        }

        for (String name : warpsSection.getKeys(false)) {
            Location location = LocationIO.read(plugin, warpsSection.getConfigurationSection(name));
            if (location != null) {
                warps.put(normalize(name), location);
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration existing = warpsFile.exists()
                ? YamlConfiguration.loadConfiguration(warpsFile)
                : null;
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection warpsSection = config.createSection("warps");
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            String fallbackWorld = existing == null
                    ? null
                    : existing.getString("warps." + entry.getKey() + ".world");
            try {
                LocationIO.write(warpsSection.createSection(entry.getKey()), entry.getValue(), fallbackWorld);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping warp '" + entry.getKey()
                        + "' during save: " + exception.getMessage());
            }
        }
        try {
            config.save(warpsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save warps.yml: " + exception.getMessage());
        }
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

    private String normalize(String name) {
        return name.toLowerCase();
    }
}
