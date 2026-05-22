package me.aurautils.platform;

import me.aurautils.AuraUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Paper enhancements loaded via reflection so the plugin compiles against Spigot API only.
 */
public class PaperPlatformAdapter extends SpigotPlatformAdapter {

    private final Method getChunkAtAsync;
    private final boolean asyncChunkLoadAvailable;

    public PaperPlatformAdapter(AuraUtils plugin) {
        super(plugin);
        Method method = null;
        try {
            method = World.class.getMethod(
                    "getChunkAtAsync",
                    int.class, int.class, boolean.class, boolean.class, Consumer.class);
        } catch (NoSuchMethodException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Paper detected but World.getChunkAtAsync(..., Consumer) is missing; RTP uses conservative Spigot-style search.");
        }
        this.getChunkAtAsync = method;
        this.asyncChunkLoadAvailable = method != null;
    }

    @Override
    public String getPlatformName() {
        return "Paper";
    }

    @Override
    public boolean isPaper() {
        return true;
    }

    @Override
    public boolean supportsAsyncChunkLoading() {
        return asyncChunkLoadAvailable;
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

        if (policy == ChunkLoadPolicy.ASYNC && asyncChunkLoadAvailable) {
            Consumer<Chunk> callback = chunk -> runOnMain(() -> {
                if (chunk != null && chunk.isLoaded()) {
                    onReady.run();
                } else {
                    onFailed.run();
                }
            });
            try {
                getChunkAtAsync.invoke(world, chunkX, chunkZ, generate, urgent, callback);
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().log(Level.FINE, "getChunkAtAsync failed", e);
                runOnMain(onFailed);
            }
            return;
        }

        if (policy == ChunkLoadPolicy.SYNC_FALLBACK && generate) {
            super.whenChunkReady(location, policy, generate, urgent, onReady, onFailed);
            return;
        }

        runOnMain(onFailed);
    }

    @Override
    public boolean hasPlayerMoved(PlayerMoveEvent event, boolean horizontalOnly) {
        if (horizontalOnly) {
            return hasHorizontalBlockChange(event);
        }
        Method hasChangedPosition = resolveHasChangedPosition();
        if (hasChangedPosition != null) {
            try {
                return (boolean) hasChangedPosition.invoke(event);
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().log(Level.FINE, "hasChangedPosition() failed, falling back to Spigot check", e);
            }
        }
        return super.hasPlayerMoved(event, horizontalOnly);
    }

    @Override
    public boolean usesEventBasedMovementDetection() {
        return resolveHasChangedPosition() != null;
    }

    private Method resolveHasChangedPosition() {
        try {
            return PlayerMoveEvent.class.getMethod("hasChangedPosition");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean hasHorizontalBlockChange(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return false;
        }
        return from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ();
    }
}
