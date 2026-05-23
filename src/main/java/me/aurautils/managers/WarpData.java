package me.aurautils.managers;

import me.aurautils.storage.WorldResolver;
import me.aurautils.util.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Per-warp location and optional metadata stored in {@code warps.yml}. */
public final class WarpData {

    private final Location location;
    private final int cooldownSeconds;
    private final String category;
    private final List<String> aliases;

    public WarpData(Location location, int cooldownSeconds, String category, List<String> aliases) {
        this.location = location.clone();
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.category = normalizeCategory(category);
        this.aliases = List.copyOf(aliases);
    }

    public static WarpData locationOnly(Location location) {
        return new WarpData(location, 0, null, List.of());
    }

    public static WarpData fromSection(WorldResolver worlds, ConfigurationSection section) {
        Location location = LocationIO.read(worlds, section);
        if (location == null) {
            return null;
        }
        int cooldown = Math.max(0, section.getInt("cooldown", 0));
        String category = section.getString("category");
        List<String> aliases = new ArrayList<>();
        for (String alias : section.getStringList("aliases")) {
            if (alias != null && !alias.isBlank()) {
                aliases.add(alias.toLowerCase(Locale.ROOT));
            }
        }
        return new WarpData(location, cooldown, category, aliases);
    }

    public void writeTo(ConfigurationSection section, String worldNameFallback) {
        LocationIO.write(section, location, worldNameFallback);
        if (cooldownSeconds > 0) {
            section.set("cooldown", cooldownSeconds);
        }
        if (category != null) {
            section.set("category", category);
        }
        if (!aliases.isEmpty()) {
            section.set("aliases", aliases);
        }
    }

    public Location getLocation() {
        return location.clone();
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /** Lowercase category label, or {@code null} when uncategorized. */
    public String getCategory() {
        return category;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public WarpData withLocation(Location newLocation) {
        return new WarpData(newLocation, cooldownSeconds, category, aliases);
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.toLowerCase(Locale.ROOT).trim();
    }
}
