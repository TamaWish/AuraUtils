package me.aurautils.storage;

import me.aurautils.AuraUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class BukkitPlayerLookup implements PlayerLookup {

    private final AuraUtils plugin;

    public BukkitPlayerLookup(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public UUID resolveToken(String token, Set<UUID> knownHomeOwners) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException ignored) {
            // not a UUID
        }

        Player online = Bukkit.getPlayerExact(token);
        if (online != null) {
            return online.getUniqueId();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(token);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline.getUniqueId();
        }

        for (UUID playerId : knownHomeOwners) {
            String name = Bukkit.getOfflinePlayer(playerId).getName();
            if (name != null && name.equalsIgnoreCase(token)) {
                return playerId;
            }
        }
        return null;
    }

    @Override
    public String displayName(UUID playerId) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name != null ? name : playerId.toString();
    }

    @Override
    public List<String> tabCompleteTokens(Set<UUID> knownHomeOwners) {
        TreeSet<String> tokens = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Player online : Bukkit.getOnlinePlayers()) {
            tokens.add(online.getName());
        }
        for (UUID playerId : knownHomeOwners) {
            String name = Bukkit.getOfflinePlayer(playerId).getName();
            if (name != null) {
                tokens.add(name);
            } else {
                tokens.add(playerId.toString());
            }
        }
        return new ArrayList<>(tokens);
    }
}
