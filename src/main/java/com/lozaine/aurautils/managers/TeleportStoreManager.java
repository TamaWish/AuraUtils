package com.lozaine.aurautils.managers;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TeleportStoreManager {

    private final AuraUtils plugin;
    private final File warpsFile;
    private final File homesFile;

    /** key (lowercase) -> destination */
    private final Map<String, StoredDestination> warps = new TreeMap<>();
    /** player UUID -> (key -> destination) */
    private final Map<UUID, Map<String, StoredDestination>> homes = new HashMap<>();

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
                StoredDestination dest = readDestination(warpsSection.getConfigurationSection(name), name);
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

                Map<String, StoredDestination> playerMap = new TreeMap<>();
                for (String homeName : playerHomes.getKeys(false)) {
                    StoredDestination dest = readDestination(playerHomes.getConfigurationSection(homeName), homeName);
                    if (dest != null) {
                        // Homes belong to the player folder; default set-by to that player if missing
                        if (dest.getSetBy() == null) {
                            String ownerName = resolveName(playerId, null);
                            dest = new StoredDestination(
                                    dest.getKey(),
                                    dest.getDisplayName(),
                                    dest.getLocation(),
                                    playerId,
                                    ownerName
                            );
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
        saveWarps();
        saveHomes();
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

    public void setWarp(String name, Location location, Player setter) {
        Location exact = snapshot(location);
        if (exact == null || name == null || name.isBlank()) {
            return;
        }
        String key = normalize(name);
        String display = name.trim();
        UUID setterId = setter != null ? setter.getUniqueId() : null;
        String setterName = setter != null ? setter.getName() : "Unknown";
        warps.put(key, new StoredDestination(key, display, exact, setterId, setterName));
    }

    /** @deprecated use setWarp(name, location, setter) */
    @Deprecated
    public void setWarp(String name, Location location) {
        setWarp(name, location, null);
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

    public void setHome(UUID playerId, String name, Location location, Player setter) {
        Location exact = snapshot(location);
        if (exact == null || name == null || name.isBlank()) {
            return;
        }
        String key = normalize(name);
        String display = name.trim();
        UUID setterId = setter != null ? setter.getUniqueId() : playerId;
        String setterName = setter != null ? setter.getName() : resolveName(playerId, null);
        homes.computeIfAbsent(playerId, ignored -> new TreeMap<>())
                .put(key, new StoredDestination(key, display, exact, setterId, setterName));
    }

    /** @deprecated use setHome(..., setter) */
    @Deprecated
    public void setHome(UUID playerId, String name, Location location) {
        setHome(playerId, name, location, null);
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
            writeDestination(warpsSection.createSection(dest.getKey()), dest);
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
                writeDestination(playerSection.createSection(dest.getKey()), dest);
            }
        }
        try {
            config.save(homesFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save homes.yml: " + exception.getMessage());
        }
    }

    private void writeDestination(ConfigurationSection section, StoredDestination dest) {
        section.set("display-name", dest.getDisplayName());
        Location location = dest.getLocation();
        if (location != null && location.getWorld() != null) {
            section.set("world", location.getWorld().getName());
            section.set("x", Double.valueOf(location.getX()));
            section.set("y", Double.valueOf(location.getY()));
            section.set("z", Double.valueOf(location.getZ()));
            section.set("yaw", Float.valueOf(location.getYaw()));
            section.set("pitch", Float.valueOf(location.getPitch()));
        }
        if (dest.getSetBy() != null) {
            section.set("set-by", dest.getSetBy().toString());
        }
        section.set("set-by-name", dest.getSetByName());
    }

    private StoredDestination readDestination(ConfigurationSection section, String fallbackKey) {
        if (section == null) {
            return null;
        }

        Location location = readLocation(section);
        // Legacy format: location fields at section root; also support nested "location" if ever used
        if (location == null) {
            return null;
        }

        String key = normalize(fallbackKey);
        String displayName = section.getString("display-name");
        if (displayName == null || displayName.isEmpty()) {
            // Legacy: section key may be lowercase only; keep as-is
            displayName = fallbackKey;
        }

        UUID setBy = null;
        String setByRaw = section.getString("set-by");
        if (setByRaw != null && !setByRaw.isEmpty()) {
            try {
                setBy = UUID.fromString(setByRaw);
            } catch (IllegalArgumentException ignored) {
                // ignore invalid uuid
            }
        }
        String setByName = section.getString("set-by-name");
        if (setByName == null || setByName.isEmpty()) {
            setByName = resolveName(setBy, "Unknown");
        }

        return new StoredDestination(key, displayName, location, setBy, setByName);
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        if (!section.contains("x") || !section.contains("y") || !section.contains("z")) {
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0D);
        float pitch = (float) section.getDouble("pitch", 0.0D);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private static Location snapshot(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(
                location.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
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
