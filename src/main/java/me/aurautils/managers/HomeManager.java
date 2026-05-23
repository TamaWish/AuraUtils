package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.storage.BukkitPlayerLookup;
import me.aurautils.storage.BukkitWorldResolver;
import me.aurautils.storage.DataStore;
import me.aurautils.storage.PlayerLookup;
import me.aurautils.storage.StoragePaths;
import me.aurautils.storage.WorldResolver;
import me.aurautils.storage.YamlDataStore;
import me.aurautils.util.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

public class HomeManager {

    private final DataStore dataStore;
    private final WorldResolver worldResolver;
    private final PlayerLookup playerLookup;
    private final HomeLimitPolicy homeLimitPolicy;
    private final Logger logger;
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public HomeManager(AuraUtils plugin) {
        this(
                new YamlDataStore(plugin),
                new BukkitWorldResolver(plugin),
                new BukkitPlayerLookup(plugin),
                new HomeLimitPolicy.AuraHomeLimitPolicy(plugin),
                plugin.getLogger()
        );
    }

    public HomeManager(
            DataStore dataStore,
            WorldResolver worldResolver,
            PlayerLookup playerLookup,
            HomeLimitPolicy homeLimitPolicy,
            Logger logger
    ) {
        this.dataStore = dataStore;
        this.worldResolver = worldResolver;
        this.playerLookup = playerLookup;
        this.homeLimitPolicy = homeLimitPolicy;
        this.logger = logger;
    }

    public void load() {
        homes.clear();

        YamlConfiguration config = dataStore.load(StoragePaths.HOMES);
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
                Location location = LocationIO.read(worldResolver, playerHomes.getConfigurationSection(homeName));
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
        YamlConfiguration existing = dataStore.exists(StoragePaths.HOMES)
                ? dataStore.load(StoragePaths.HOMES)
                : null;
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection homesSection = config.createSection("homes");
        for (Map.Entry<UUID, Map<String, Location>> playerEntry : homes.entrySet()) {
            String playerKey = playerEntry.getKey().toString();
            ConfigurationSection playerSection = homesSection.createSection(playerKey);
            for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                String fallbackWorld = existing == null
                        ? null
                        : existing.getString("homes." + playerKey + "." + homeEntry.getKey() + ".world");
                try {
                    LocationIO.write(
                            playerSection.createSection(homeEntry.getKey()),
                            homeEntry.getValue(),
                            fallbackWorld
                    );
                } catch (IllegalArgumentException exception) {
                    logger.warning("Skipping home '" + homeEntry.getKey() + "' for "
                            + playerKey + " during save: " + exception.getMessage());
                }
            }
        }
        dataStore.save(StoragePaths.HOMES, config);
    }

    public int getMaxHomesPerPlayer(Player player) {
        return homeLimitPolicy.getMaxHomes(player);
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

    /**
     * Resolves a player UUID from an admin token (UUID string, online name, or offline name with homes).
     */
    public UUID resolvePlayerId(String token) {
        return playerLookup.resolveToken(token, homes.keySet());
    }

    public String getPlayerDisplayName(UUID playerId) {
        return playerLookup.displayName(playerId);
    }

    public List<String> knownPlayerTokens() {
        return playerLookup.tabCompleteTokens(homes.keySet());
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
}
