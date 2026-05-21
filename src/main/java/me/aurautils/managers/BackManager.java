package me.aurautils.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackManager {

    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Set<UUID> skipNextRecord = new HashSet<>();

    /** Do not record the next teleport for this player (AuraUtils-initiated). */
    public void skipNextRecord(UUID playerId) {
        skipNextRecord.add(playerId);
    }

    public boolean consumeSkip(UUID playerId) {
        return skipNextRecord.remove(playerId);
    }

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