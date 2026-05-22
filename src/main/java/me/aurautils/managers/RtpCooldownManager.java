package me.aurautils.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin-scoped RTP cooldown timestamps (survives command re-registration on reload).
 */
public class RtpCooldownManager {

    private final Map<UUID, Long> lastUseMillis = new ConcurrentHashMap<>();

    /** Seconds remaining on cooldown, or 0 if the player may use RTP. */
    public long remainingSeconds(UUID playerId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long lastUse = lastUseMillis.get(playerId);
        if (lastUse == null) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        if (elapsed >= cooldownSeconds) {
            return 0;
        }
        return cooldownSeconds - elapsed;
    }

    public void recordUse(UUID playerId) {
        lastUseMillis.put(playerId, System.currentTimeMillis());
    }
}
