package com.lozaine.aurautils.managers;

import org.bukkit.Location;

import java.util.UUID;

/**
 * A saved home or warp: exact location, original display name, and who set it.
 */
public final class StoredDestination {

    private final String key;
    private final String displayName;
    private final Location location;
    private final UUID setBy;
    private final String setByName;

    public StoredDestination(String key, String displayName, Location location, UUID setBy, String setByName) {
        this.key = key;
        this.displayName = displayName == null || displayName.isEmpty() ? key : displayName;
        this.location = location;
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

    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    public UUID getSetBy() {
        return setBy;
    }

    public String getSetByName() {
        return setByName;
    }
}
