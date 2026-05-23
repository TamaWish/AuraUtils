package me.aurautils.config;

import java.util.List;

/**
 * Validated, runtime-normalized settings from {@code config.yml}.
 */
public record AuraConfig(
        int tpaTimeout,
        int teleportCountdown,
        boolean teleportAsyncChunkLoad,
        boolean teleportSyncChunkFallback,
        int teleportChunkLoadTimeoutSeconds,
        TeleportFeatureOverride adminTeleportOverride,
        TeleportFeatureOverride rtpTeleportOverride,
        int rtpRadius,
        int rtpMinDistance,
        int rtpAttempts,
        int rtpAttemptsPerTick,
        boolean rtpCenterOnPlayer,
        boolean rtpOnlyLoadedChunksExplicitlySet,
        boolean rtpOnlyLoadedChunks,
        boolean rtpGenerateChunks,
        boolean rtpAsyncUrgent,
        int rtpMaxPendingChunkLoads,
        int chunkLoadMaxInFlightGlobal,
        int chunkLoadMaxInFlightPerPlayer,
        int chunkLoadMaxQueueSize,
        int rtpCountdown,
        int rtpCooldown,
        int homesDefaultLimit,
        boolean vanishEnabled,
        String vanishSeePermission,
        List<String> vanishMetadataKeys,
        String messagesDefaultLocale,
        String messagesFallbackLocale,
        boolean messagesUseClientLocale
) {
}
