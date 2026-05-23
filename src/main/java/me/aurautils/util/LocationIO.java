package me.aurautils.util;

import me.aurautils.storage.WorldResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationIO {

    private LocationIO() {
    }

    public static void write(ConfigurationSection section, Location location) {
        write(section, location, null);
    }

    /**
     * Writes a location to config. During server shutdown worlds may already be unloaded;
     * {@code worldNameFallback} should be the last persisted world name (e.g. from the existing file).
     */
    public static void write(ConfigurationSection section, Location location, String worldNameFallback) {
        String worldName = resolveWorldName(location);
        if (worldName == null) {
            worldName = worldNameFallback;
        }
        if (worldName == null) {
            throw new IllegalArgumentException("Cannot resolve world for location");
        }

        section.set("world", worldName);
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    /**
     * Returns the world name when the world is loaded; {@code null} if the world reference is gone.
     */
    public static String resolveWorldName(Location location) {
        if (location == null) {
            return null;
        }
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Server server = Bukkit.getServer();
        if (server == null) {
            return world.getName();
        }
        if (!location.isWorldLoaded()) {
            return null;
        }
        return world.getName();
    }

    public static Location read(WorldResolver worlds, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        World world = worldName == null ? null : worlds.getWorld(worldName);
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
}
