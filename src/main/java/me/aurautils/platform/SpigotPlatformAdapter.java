package me.aurautils.platform;

import me.aurautils.AuraUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;

public class SpigotPlatformAdapter implements PlatformAdapter {

    protected final AuraUtils plugin;

    public SpigotPlatformAdapter() {
        this.plugin = null;
    }

    public SpigotPlatformAdapter(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlatformName() {
        return "Spigot";
    }

    @Override
    public boolean isPaper() {
        return false;
    }

    @Override
    public boolean supportsAsyncChunkLoading() {
        return false;
    }

    @Override
    public boolean isChunkLoaded(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public void whenChunkReady(Location location, ChunkLoadPolicy policy, boolean generate, boolean urgent,
                               Runnable onReady, Runnable onFailed) {
        World world = location.getWorld();
        if (world == null) {
            runOnMain(onFailed);
            return;
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        if (world.isChunkLoaded(chunkX, chunkZ)) {
            runOnMain(onReady);
            return;
        }

        if (policy == ChunkLoadPolicy.LOADED_ONLY) {
            runOnMain(onFailed);
            return;
        }

        if (policy == ChunkLoadPolicy.SYNC_FALLBACK && generate) {
            runOnMain(() -> {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (!chunk.isLoaded()) {
                    chunk.load(generate);
                }
                if (chunk.isLoaded()) {
                    onReady.run();
                } else {
                    onFailed.run();
                }
            });
            return;
        }

        runOnMain(onFailed);
    }

    protected void runOnMain(Runnable action) {
        if (plugin == null) {
            action.run();
            return;
        }
        if (plugin.getServer().isPrimaryThread()) {
            action.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, action);
        }
    }

    @Override
    public boolean hasPlayerMoved(PlayerMoveEvent event, boolean horizontalOnly) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return false;
        }
        return hasMoved(from, to, horizontalOnly);
    }

    @Override
    public boolean usesEventBasedMovementDetection() {
        return false;
    }

    static boolean hasMoved(Location from, Location to, boolean horizontalOnly) {
        if (horizontalOnly) {
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            return (dx * dx + dz * dz) > 0.01;
        }
        return to.distanceSquared(from) > 0.01;
    }
}
