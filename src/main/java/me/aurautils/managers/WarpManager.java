package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.storage.BukkitWorldResolver;
import me.aurautils.storage.DataStore;
import me.aurautils.storage.StoragePaths;
import me.aurautils.storage.WorldResolver;
import me.aurautils.storage.YamlDataStore;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class WarpManager {

    private final DataStore dataStore;
    private final WorldResolver worldResolver;
    private final Logger logger;
    private final Map<String, WarpData> warps = new TreeMap<>();
    private final Map<String, String> aliasToCanonical = new TreeMap<>();

    public WarpManager(AuraUtils plugin) {
        this(new YamlDataStore(plugin), new BukkitWorldResolver(plugin), plugin.getLogger());
    }

    public WarpManager(DataStore dataStore, WorldResolver worldResolver, Logger logger) {
        this.dataStore = dataStore;
        this.worldResolver = worldResolver;
        this.logger = logger;
    }

    public void load() {
        warps.clear();
        aliasToCanonical.clear();

        YamlConfiguration config = dataStore.load(StoragePaths.WARPS);
        ConfigurationSection warpsSection = config.getConfigurationSection("warps");
        if (warpsSection == null) {
            return;
        }

        for (String name : warpsSection.getKeys(false)) {
            String canonical = normalize(name);
            WarpData data = WarpData.fromSection(worldResolver, warpsSection.getConfigurationSection(name));
            if (data != null) {
                warps.put(canonical, data);
            }
        }
        rebuildAliasIndex();
    }

    public void save() {
        YamlConfiguration existing = dataStore.exists(StoragePaths.WARPS)
                ? dataStore.load(StoragePaths.WARPS)
                : null;
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection warpsSection = config.createSection("warps");
        for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
            String fallbackWorld = existing == null
                    ? null
                    : existing.getString("warps." + entry.getKey() + ".world");
            try {
                entry.getValue().writeTo(warpsSection.createSection(entry.getKey()), fallbackWorld);
            } catch (IllegalArgumentException exception) {
                logger.warning("Skipping warp '" + entry.getKey()
                        + "' during save: " + exception.getMessage());
            }
        }
        dataStore.save(StoragePaths.WARPS, config);
    }

    public Set<String> getWarpNames() {
        return new TreeSet<>(warps.keySet());
    }

    /** Canonical names and aliases (for tab completion). */
    public List<String> getAllResolvableNames() {
        List<String> names = new ArrayList<>();
        for (String canonical : warps.keySet()) {
            names.add(canonical);
            names.addAll(warps.get(canonical).getAliases());
        }
        return names;
    }

    /**
     * Resolves a warp name or alias to the canonical warp key, or {@code null} if unknown.
     */
    public String resolveWarpName(String nameOrAlias) {
        if (nameOrAlias == null || nameOrAlias.isBlank()) {
            return null;
        }
        String key = normalize(nameOrAlias);
        if (warps.containsKey(key)) {
            return key;
        }
        return aliasToCanonical.get(key);
    }

    public WarpData getWarpData(String name) {
        String canonical = resolveWarpName(name);
        if (canonical == null) {
            return null;
        }
        WarpData data = warps.get(canonical);
        return data == null ? null : data;
    }

    public Location getWarp(String name) {
        WarpData data = getWarpData(name);
        return data == null ? null : data.getLocation();
    }

    public void setWarp(String name, Location location) {
        String canonical = normalize(name);
        WarpData existing = warps.get(canonical);
        if (existing != null) {
            warps.put(canonical, existing.withLocation(location));
        } else {
            warps.put(canonical, WarpData.locationOnly(location));
        }
        rebuildAliasIndex();
    }

    public boolean deleteWarp(String name) {
        String canonical = resolveWarpName(name);
        if (canonical == null) {
            return false;
        }
        if (warps.remove(canonical) == null) {
            return false;
        }
        rebuildAliasIndex();
        return true;
    }

    /** Distinct non-empty categories among all warps, sorted. */
    public Set<String> getCategories() {
        Set<String> categories = new TreeSet<>();
        for (WarpData data : warps.values()) {
            if (data.getCategory() != null) {
                categories.add(data.getCategory());
            }
        }
        return categories;
    }

    public boolean hasUncategorizedWarps() {
        return warps.values().stream().anyMatch(data -> data.getCategory() == null);
    }

    /**
     * Warps visible in menus/lists, optionally filtered by category.
     * {@code categoryFilter} {@code null} = all; empty string = uncategorized only.
     */
    public List<String> getWarpNamesSorted(String categoryFilter) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
            if (!matchesCategory(entry.getValue(), categoryFilter)) {
                continue;
            }
            names.add(entry.getKey());
        }
        names.sort((a, b) -> {
            String catA = warps.get(a).getCategory();
            String catB = warps.get(b).getCategory();
            if (catA == null && catB != null) {
                return 1;
            }
            if (catA != null && catB == null) {
                return -1;
            }
            if (catA != null && catB != null) {
                int catCmp = catA.compareTo(catB);
                if (catCmp != 0) {
                    return catCmp;
                }
            }
            return a.compareTo(b);
        });
        return names;
    }

    private static boolean matchesCategory(WarpData data, String categoryFilter) {
        if (categoryFilter == null) {
            return true;
        }
        if (categoryFilter.isEmpty()) {
            return data.getCategory() == null;
        }
        return categoryFilter.equals(data.getCategory());
    }

    private void rebuildAliasIndex() {
        aliasToCanonical.clear();
        for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
            String canonical = entry.getKey();
            for (String alias : entry.getValue().getAliases()) {
                if (alias.equals(canonical)) {
                    logger.warning("Warp '" + canonical + "' lists itself as an alias; ignored.");
                    continue;
                }
                if (warps.containsKey(alias)) {
                    logger.warning("Warp alias '" + alias + "' conflicts with warp '" + canonical + "'; ignored.");
                    continue;
                }
                String previous = aliasToCanonical.put(alias, canonical);
                if (previous != null && !previous.equals(canonical)) {
                    logger.warning("Warp alias '" + alias + "' is used by both '"
                            + previous + "' and '" + canonical + "'; keeping '" + previous + "'.");
                    aliasToCanonical.put(alias, previous);
                }
            }
        }
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }
}
