package me.aurautils.platform;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Gates {@link PlatformAdapter#whenChunkReady} through {@link ChunkLoadCoordinator}
 * so RTP and teleports share one server-wide async chunk pipeline.
 */
public final class ChunkLoadService {

    private final AuraUtils plugin;
    private final PlatformAdapter platform;
    private volatile ChunkLoadCoordinator coordinator;

    public ChunkLoadService(AuraUtils plugin, PlatformAdapter platform) {
        this.plugin = plugin;
        this.platform = platform;
        rebuildCoordinator();
    }

    public void rebuildCoordinator() {
        AuraConfig config = plugin.getAuraConfig();
        this.coordinator = new ChunkLoadCoordinator(
                config.chunkLoadMaxInFlightGlobal(),
                config.chunkLoadMaxInFlightPerPlayer(),
                config.chunkLoadMaxQueueSize(),
                task -> plugin.getServer().getScheduler().runTask(plugin, task));
    }

    public ChunkLoadCoordinator coordinator() {
        return coordinator;
    }

    public boolean hasImmediateCapacity(UUID playerId) {
        return coordinator.hasImmediateCapacity(playerId);
    }

    public void whenChunkReady(UUID playerId, Location location, ChunkLoadPolicy policy,
                               boolean generate, boolean urgent,
                               Runnable onReady, Runnable onFailed) {
        whenChunkReady(playerId, location, policy, generate, urgent, null, onReady, onFailed,
                ChunkLoadCoordinator.QueuePolicy.QUEUE);
    }

    public void whenChunkReady(UUID playerId, Location location, ChunkLoadPolicy policy,
                               boolean generate, boolean urgent,
                               Runnable onReady, Runnable onFailed,
                               ChunkLoadCoordinator.QueuePolicy queuePolicy) {
        whenChunkReady(playerId, location, policy, generate, urgent, null, onReady, onFailed, queuePolicy);
    }

    /**
     * @param onSlotAcquired runs on the main thread when a global/per-player slot is acquired,
     *                       before the platform begins loading (not called when the chunk is already loaded).
     */
    public void whenChunkReady(UUID playerId, Location location, ChunkLoadPolicy policy,
                               boolean generate, boolean urgent,
                               Runnable onSlotAcquired,
                               Runnable onReady, Runnable onFailed,
                               ChunkLoadCoordinator.QueuePolicy queuePolicy) {
        if (platform.isChunkLoaded(location)) {
            plugin.getServer().getScheduler().runTask(plugin, onReady);
            return;
        }

        ChunkLoadCoordinator gate = coordinator;
        gate.schedule(playerId, () -> {
            if (onSlotAcquired != null) {
                onSlotAcquired.run();
            }
            platform.whenChunkReady(
                    location,
                    policy,
                    generate,
                    urgent,
                    releaseWrap(playerId, onReady),
                    releaseWrap(playerId, onFailed));
        }, onFailed, queuePolicy);
    }

    private Runnable releaseWrap(UUID playerId, Runnable delegate) {
        return () -> {
            try {
                delegate.run();
            } finally {
                coordinator.release(playerId);
            }
        };
    }
}
