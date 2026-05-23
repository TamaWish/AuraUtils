package me.aurautils.config;

import me.aurautils.managers.RtpMode;

import java.util.List;
import java.util.Set;

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
        int rtpSolidBlocksBelow,
        int rtpCeilingClearance,
        int rtpMaxCandidates,
        boolean rtpAdaptiveEnabled,
        int rtpAdaptiveFailThresholdPercent,
        int rtpAdaptiveMinSamples,
        int rtpAdaptiveRadiusBonus,
        int rtpAdaptiveMinDistanceReduction,
        int rtpAdaptiveMaxRadius,
        Set<String> rtpAllowedBiomes,
        Set<String> rtpDeniedBiomes,
        List<String> rtpWorlds,
        RtpMode rtpMode,
        boolean rtpStratifiedRings,
        int rtpRingBands,
        boolean rtpChunkCentric,
        boolean rtpGridJitter,
        int rtpGridCellSize,
        int rtpChunkRetryLimit,
        boolean rtpPreloadNeighbors,
        int rtpPreloadRadius,
        int rtpCaveSurfaceBuffer,
        int rtpCaveMinY,
        int rtpCaveMaxY,
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
