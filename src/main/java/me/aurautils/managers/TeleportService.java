package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.config.TeleportFeatureOverride;
import me.aurautils.platform.ChunkLoadPolicy;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Single entry point for player teleports: countdown sessions and instant chunk-safe execution.
 */
public class TeleportService {

    public enum TeleportKind {
        STANDARD,
        ADMIN,
        RTP
    }

    private final AuraUtils plugin;
    private final TeleportHelper helper;

    public TeleportService(AuraUtils plugin, TeleportHelper helper) {
        this.plugin = plugin;
        this.helper = helper;
    }

    public void teleport(Player player, Location destination, TeleportOptions options) {
        Location dest = destination.clone();
        if (!validateDestination(player, dest)) {
            return;
        }

        if (options.skipBackRecord()) {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
        }

        if (options.countdownSeconds() > 0) {
            String messageKey = options.successMessageKey() != null
                    ? options.successMessageKey()
                    : "teleport.success-default";
            helper.scheduleTeleport(
                    player,
                    dest,
                    options.countdownSeconds(),
                    options.horizontalMovementOnly(),
                    messageKey,
                    options.successPlaceholders(),
                    options.onSuccess());
            return;
        }

        helper.executeTeleport(player, dest, options);
    }

    /**
     * @return {@code false} if the destination world is missing or not loaded (no sync world load).
     */
    boolean validateDestination(Player player, Location dest) {
        World world = dest.getWorld();
        if (world == null) {
            plugin.send(player, "teleport.world-invalid");
            return false;
        }
        World loaded = Bukkit.getWorld(world.getUID());
        if (loaded == null) {
            plugin.send(player, "teleport.world-unloaded",
                    MessagePlaceholders.of("world", world.getName()));
            return false;
        }
        if (loaded != world) {
            dest.setWorld(loaded);
        }
        return true;
    }

    public int countdownFor(TeleportKind kind) {
        AuraConfig config = plugin.getAuraConfig();
        return switch (kind) {
            case ADMIN -> resolveCountdown(config.adminTeleportOverride(), 0);
            case RTP -> resolveCountdown(config.rtpTeleportOverride(), config.rtpCountdown());
            case STANDARD -> config.teleportCountdown();
        };
    }

    public TeleportOptions.Builder standardOptions() {
        return TeleportOptions.builder().countdownSeconds(countdownFor(TeleportKind.STANDARD));
    }

    public TeleportOptions.Builder adminOptions() {
        AuraConfig config = plugin.getAuraConfig();
        TeleportFeatureOverride override = config.adminTeleportOverride();
        TeleportOptions.Builder builder = TeleportOptions.builder()
                .countdownSeconds(countdownFor(TeleportKind.ADMIN))
                .skipBackRecord(true)
                .horizontalMovementOnly(false);
        if (override.asyncChunkLoad() != null) {
            ChunkLoadPolicy policy = Boolean.TRUE.equals(override.asyncChunkLoad())
                    && plugin.getPlatform().supportsAsyncChunkLoading()
                    ? ChunkLoadPolicy.ASYNC
                    : ChunkLoadPolicy.LOADED_ONLY;
            builder.chunkPolicyOverride(policy);
        }
        return builder;
    }

    private static int resolveCountdown(TeleportFeatureOverride override, int fallback) {
        if (override != null && override.countdown() != null) {
            return Math.max(0, override.countdown());
        }
        return fallback;
    }

    public boolean hasPendingTeleport(Player player) {
        return helper.hasActiveSession(player.getUniqueId());
    }

    public boolean cancelPendingTeleport(Player player) {
        return helper.cancelPendingTeleport(player);
    }

    public void clearPendingOnQuit(java.util.UUID playerId) {
        helper.clearSession(playerId);
    }

    /** Chunk settings when RTP finishes without a separate countdown. */
    public TeleportOptions rtpInstant(AuraUtils plugin) {
        AuraConfig config = plugin.getAuraConfig();
        ChunkLoadPolicy policy = plugin.getPlatform().supportsAsyncChunkLoading()
                ? ChunkLoadPolicy.ASYNC
                : ChunkLoadPolicy.LOADED_ONLY;
        return TeleportOptions.builder()
                .countdownSeconds(0)
                .noSuccessMessage()
                .skipBackRecord(true)
                .chunkPolicyOverride(policy)
                .generateChunksOverride(config.rtpGenerateChunks())
                .asyncUrgentOverride(config.rtpAsyncUrgent())
                .sendChunkFailureMessage(false)
                .build();
    }

    public static TeleportOptions rtpWithCountdown(int seconds) {
        return TeleportOptions.builder()
                .countdownSeconds(seconds)
                .horizontalMovementOnly(true)
                .successMessageKey("teleport.success-rtp")
                .build();
    }
}
