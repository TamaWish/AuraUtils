package me.aurautils.managers;

import org.bukkit.Location;

/** A validated RTP destination with a lower-is-better hazard score. */
public record RtpCandidate(Location location, int hazardScore) {
}
