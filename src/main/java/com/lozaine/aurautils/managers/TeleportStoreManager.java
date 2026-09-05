package com.lozaine.aurautils.managers;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TeleportStoreManager {

    private final AuraUtils plugin;
    private final File warpsFile;
    private final File homesFile;

    /** key (lowercase) -> destination */
    private final Map<String, StoredDestination> warps = new ConcurrentHashMap<>();
    /** player UUID -> (key -> destination) */
    private final Map<UUID, Map<String, StoredDestination>> homes = new ConcurrentHashMap<>();

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
                StoredDestination dest = StoredDestination.fromSection(warpsSection.getConfigurationSection(name), name);
                dest = completeLoaded(dest);
                if (dest != null) {
                    warps.put(dest.getKey(), dest);
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

                Map<String, StoredDestination> playerMap = new ConcurrentHashMap<>();
                for (String homeName : playerHomes.getKeys(false)) {
                    StoredDestination dest = StoredDestination.fromSection(playerHomes.getConfigurationSection(homeName), homeName);
                    dest = completeLoaded(dest);
                    if (dest != null) {
                        // Homes belong to the player folder; default set-by to that player if missing
                        if (dest.getSetBy() == null) {
                            dest = dest.withSetBy(playerId, resolveName(playerId, null));
                        }
                        playerMap.put(dest.getKey(), dest);
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
        try {
            saveWarps();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save warps.yml", exception);
        }
        try {
            saveHomes();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save homes.yml", exception);
        }
    }

    public List<String> getWarpNames() {
        List<String> names = new ArrayList<>();
        for (StoredDestination dest : warps.values()) {
            names.add(dest.getDisplayName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<StoredDestination> getWarps() {
        List<StoredDestination> list = new ArrayList<>(warps.values());
        list.sort(Comparator.comparing(StoredDestination::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public Location getWarp(String name) {
        StoredDestination dest = warps.get(normalize(name));
        return dest == null ? null : dest.getLocation();
    }

    public StoredDestination getWarpDestination(String name) {
        return warps.get(normalize(name));
    }

    public boolean setWarp(String name, Location location, Player setter) {
        if (location == null || !location.isWorldLoaded() || name == null || name.isBlank()) {
            return false;
        }
        String key = normalize(name);
        String display = name.trim();
        UUID setterId = setter != null ? setter.getUniqueId() : null;
        String setterName = setter != null ? setter.getName() : "Unknown";
        warps.put(key, new StoredDestination(key, display, location, setterId, setterName));
        return true;
    }

    /** @deprecated use setWarp(name, location, setter) */
    @Deprecated
    public boolean setWarp(String name, Location location) {
        return setWarp(name, location, null);
    }

    public boolean deleteWarp(String name) {
        return warps.remove(normalize(name)) != null;
    }

    public List<String> getHomeNames(UUID playerId) {
        Map<String, StoredDestination> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (StoredDestination dest : playerHomes.values()) {
            names.add(dest.getDisplayName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<StoredDestination> getHomes(UUID playerId) {
        Map<String, StoredDestination> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return Collections.emptyList();
        }
        List<StoredDestination> list = new ArrayList<>(playerHomes.values());
        list.sort(Comparator.comparing(StoredDestination::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public Location getHome(UUID playerId, String name) {
        StoredDestination dest = getHomeDestination(playerId, name);
        return dest == null ? null : dest.getLocation();
    }

    public StoredDestination getHomeDestination(UUID playerId, String name) {
        Map<String, StoredDestination> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return null;
        }
        return playerHomes.get(normalize(name));
    }

    public boolean setHome(UUID playerId, String name, Location location, Player setter) {
        if (location == null || !location.isWorldLoaded() || name == null || name.isBlank()) {
            return false;
        }
        String key = normalize(name);
        String display = name.trim();
        UUID setterId = setter != null ? setter.getUniqueId() : playerId;
        String setterName = setter != null ? setter.getName() : resolveName(playerId, null);
        homes.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(key, new StoredDestination(key, display, location, setterId, setterName));
        return true;
    }

    /** @deprecated use setHome(..., setter) */
    @Deprecated
    public boolean setHome(UUID playerId, String name, Location location) {
        return setHome(playerId, name, location, null);
    }

    public boolean deleteHome(UUID playerId, String name) {
        Map<String, StoredDestination> playerHomes = homes.get(playerId);
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
        for (StoredDestination dest : warps.values()) {
            dest.writeTo(warpsSection.createSection(dest.getKey()));
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
        for (Map.Entry<UUID, Map<String, StoredDestination>> entry : homes.entrySet()) {
            ConfigurationSection playerSection = homesSection.createSection(entry.getKey().toString());
            for (StoredDestination dest : entry.getValue().values()) {
                dest.writeTo(playerSection.createSection(dest.getKey()));
            }
        }
        try {
            config.save(homesFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save homes.yml: " + exception.getMessage());
        }
    }

    private StoredDestination completeLoaded(StoredDestination dest) {
        if (dest == null) {
            return null;
        }
        if (dest.getSetBy() != null && "Unknown".equals(dest.getSetByName())) {
            return dest.withSetBy(dest.getSetBy(), resolveName(dest.getSetBy(), "Unknown"));
        }
        return dest;
    }

    private String resolveName(UUID id, String fallback) {
        if (id == null) {
            return fallback != null ? fallback : "Unknown";
        }
        OfflinePlayer offline = plugin.getServer().getOfflinePlayer(id);
        String name = offline.getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return fallback != null ? fallback : "Unknown";
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
