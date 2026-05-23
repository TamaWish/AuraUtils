package me.aurautils.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player, per-warp cooldown timestamps (in-memory; cleared on restart). */
public class WarpCooldownManager {

    private final Map<UUID, Map<String, Long>> lastUseMillis = new ConcurrentHashMap<>();

    /** Seconds remaining on cooldown for the canonical warp name, or 0 if the player may warp. */
    public long remainingSeconds(UUID playerId, String canonicalWarpName, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Map<String, Long> perWarp = lastUseMillis.get(playerId);
        if (perWarp == null) {
            return 0;
        }
        Long lastUse = perWarp.get(canonicalWarpName);
        if (lastUse == null) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        if (elapsed >= cooldownSeconds) {
            return 0;
        }
        return cooldownSeconds - elapsed;
    }

    public void recordUse(UUID playerId, String canonicalWarpName) {
        lastUseMillis
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(canonicalWarpName, System.currentTimeMillis());
    }
}
