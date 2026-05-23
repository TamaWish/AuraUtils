package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.util.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public class HomeManager {

    private final AuraUtils plugin;
    private final File homesFile;
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public HomeManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        homes.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(homesFile);
        ConfigurationSection homesSection = config.getConfigurationSection("homes");
        if (homesSection == null) {
            return;
        }

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
                Location location = LocationIO.read(plugin, playerHomes.getConfigurationSection(homeName));
                if (location != null) {
                    playerMap.put(normalize(homeName), location);
                }
            }
            if (!playerMap.isEmpty()) {
                homes.put(playerId, playerMap);
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection homesSection = config.createSection("homes");
        for (Map.Entry<UUID, Map<String, Location>> playerEntry : homes.entrySet()) {
            ConfigurationSection playerSection = homesSection.createSection(playerEntry.getKey().toString());
            for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                LocationIO.write(playerSection.createSection(homeEntry.getKey()), homeEntry.getValue());
            }
        }
        try {
            config.save(homesFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save homes.yml: " + exception.getMessage());
        }
    }

    public int getMaxHomesPerPlayer(Player player) {
        if (player.hasPermission("aura.admin")) {
            return -1;
        }

        int defaultLimit = plugin.getConfig().getInt("homes.default-limit",
                plugin.getConfig().getInt("homes.max-per-player", 5));
        int highestConfiguredLimit = defaultLimit;

        List<String> limitNodes = plugin.getConfig().getStringList("homes.permission-limits");
        if (limitNodes.isEmpty()) {
            return defaultLimit;
        }

        for (String limitNode : limitNodes) {
            int limit = parseHomeLimit(limitNode);
            if (limit < 0) {
                continue;
            }
            if (player.hasPermission(limitNode)) {
                highestConfiguredLimit = Math.max(highestConfiguredLimit, limit);
            }
        }
        return highestConfiguredLimit;
    }

    public int getHomeCount(UUID playerId) {
        Map<String, Location> playerHomes = homes.get(playerId);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    public boolean hasHome(UUID playerId, String name) {
        Map<String, Location> playerHomes = homes.get(playerId);
        return playerHomes != null && playerHomes.containsKey(normalize(name));
    }

    public boolean canSetHome(Player player, String name) {
        int max = getMaxHomesPerPlayer(player);
        if (max < 0) {
            return true;
        }
        UUID playerId = player.getUniqueId();
        if (hasHome(playerId, name)) {
            return true;
        }
        return getHomeCount(playerId) < max;
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

    private String normalize(String name) {
        return name.toLowerCase();
    }

    private int parseHomeLimit(String permissionNode) {
        if (permissionNode == null) {
            return -1;
        }

        String trimmed = permissionNode.trim();
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot < 0 || lastDot == trimmed.length() - 1) {
            return -1;
        }

        try {
            return Integer.parseInt(trimmed.substring(lastDot + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
