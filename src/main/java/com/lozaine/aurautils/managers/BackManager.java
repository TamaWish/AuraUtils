package com.lozaine.aurautils.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores each player's last teleport origin for {@code /back}.
 *
 * <p>Locations are stored as a durable world-name + coordinates snapshot so
 * the World reference cannot go null across region ticks or world reloads
 * (important on Folia and with dynamic world managers).
 */
public class BackManager {

    private static final class StoredBack {
        final String worldName;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        StoredBack(Location loc) {
            this.worldName = loc.getWorld().getName();
            this.x = loc.getX();
            this.y = loc.getY();
            this.z = loc.getZ();
            this.yaw = loc.getYaw();
            this.pitch = loc.getPitch();
        }

        Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    private final Map<UUID, StoredBack> lastLocations = new ConcurrentHashMap<>();

    /**
     * Record the player's current position as the next {@code /back} target.
     * Ignores null locations or locations with no loaded world.
     */
    public void record(UUID playerId, Location location) {
        if (playerId == null || location == null || location.getWorld() == null) {
            return;
        }
        lastLocations.put(playerId, new StoredBack(location));
    }

    /**
     * @return a fresh Location with a live World reference, or null if missing / world unloaded
     */
    public Location get(UUID playerId) {
        StoredBack stored = lastLocations.get(playerId);
        return stored == null ? null : stored.toLocation();
    }

    public boolean has(UUID playerId) {
        return lastLocations.containsKey(playerId);
    }

    public void remove(UUID playerId) {
        lastLocations.remove(playerId);
    }
}
