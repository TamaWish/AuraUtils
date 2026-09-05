package com.lozaine.aurautils.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.UUID;

/**
 * A saved home or warp: exact coordinates, original display name, and who set it.
 *
 * <p>World is stored by name, not as a live {@link Location} World reference.
 * Paper throws {@code IllegalArgumentException: World unloaded} from
 * {@link Location#getWorld()} after shutdown unloads worlds, so serialization
 * must never call {@code getWorld()}.
 */
public final class StoredDestination {

    private final String key;
    private final String displayName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final UUID setBy;
    private final String setByName;

    public StoredDestination(String key, String displayName, Location location, UUID setBy, String setByName) {
        this(
                key,
                displayName,
                worldNameOf(location),
                location == null ? 0.0D : location.getX(),
                location == null ? 0.0D : location.getY(),
                location == null ? 0.0D : location.getZ(),
                location == null ? 0.0F : location.getYaw(),
                location == null ? 0.0F : location.getPitch(),
                setBy,
                setByName
        );
    }

    public StoredDestination(
            String key,
            String displayName,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            UUID setBy,
            String setByName
    ) {
        this.key = key;
        this.displayName = displayName == null || displayName.isEmpty() ? key : displayName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.setBy = setBy;
        this.setByName = setByName == null || setByName.isEmpty() ? "Unknown" : setByName;
    }

    public String getKey() {
        return key;
    }

    /** Original casing as typed when set (e.g. ExampleHomeOne). */
    public String getDisplayName() {
        return displayName;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    /**
     * @return a fresh Location with a live World, or null if the world is not loaded
     */
    public Location getLocation() {
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public UUID getSetBy() {
        return setBy;
    }

    public String getSetByName() {
        return setByName;
    }

    public StoredDestination withSetBy(UUID newSetBy, String newSetByName) {
        return new StoredDestination(key, displayName, worldName, x, y, z, yaw, pitch, newSetBy, newSetByName);
    }

    public void writeTo(ConfigurationSection section) {
        section.set("display-name", displayName);
        if (worldName != null && !worldName.isEmpty()) {
            section.set("world", worldName);
            section.set("x", Double.valueOf(x));
            section.set("y", Double.valueOf(y));
            section.set("z", Double.valueOf(z));
            section.set("yaw", Float.valueOf(yaw));
            section.set("pitch", Float.valueOf(pitch));
        }
        if (setBy != null) {
            section.set("set-by", setBy.toString());
        }
        section.set("set-by-name", setByName);
    }

    public static StoredDestination fromSection(ConfigurationSection section, String fallbackKey) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.isEmpty()) {
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

        String key = fallbackKey == null ? "" : fallbackKey.trim().toLowerCase(Locale.ROOT);
        String displayName = section.getString("display-name");
        if (displayName == null || displayName.isEmpty()) {
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

        return new StoredDestination(key, displayName, worldName, x, y, z, yaw, pitch, setBy, setByName);
    }

    /**
     * Paper throws if {@link Location#getWorld()} is used after unload.
     * {@link Location#isWorldLoaded()} is the safe check.
     */
    static String worldNameOf(Location location) {
        if (location == null || !location.isWorldLoaded()) {
            return null;
        }
        return location.getWorld().getName();
    }
}
