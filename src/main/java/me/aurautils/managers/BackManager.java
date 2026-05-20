package me.aurautils.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackManager {

    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public void record(UUID playerId, Location location) {
        if (location == null) {
            return;
        }
        lastLocations.put(playerId, location.clone());
    }

    public Location get(UUID playerId) {
        Location location = lastLocations.get(playerId);
        return location == null ? null : location.clone();
    }

    public boolean has(UUID playerId) {
        return lastLocations.containsKey(playerId);
    }

    public void remove(UUID playerId) {
        lastLocations.remove(playerId);
    }
}