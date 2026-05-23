package me.aurautils.storage;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves player identities for admin/home commands without tying managers to {@code Bukkit} statics.
 */
public interface PlayerLookup {

    UUID resolveToken(String token, Set<UUID> knownHomeOwners);

    String displayName(UUID playerId);

    List<String> tabCompleteTokens(Set<UUID> knownHomeOwners);
}
