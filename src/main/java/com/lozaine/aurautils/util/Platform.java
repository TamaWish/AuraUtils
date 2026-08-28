package com.lozaine.aurautils.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Runtime capability checks so a single Spigot-API jar can use Paper async
 * chunk loading when present, without linking Paper at compile time.
 */
public final class Platform {

    private final boolean folia;
    private final JavaPlugin plugin;
    private final ConcurrentMap<Class<?>, Optional<Method>> asyncChunkMethods = new ConcurrentHashMap<>();

    public Platform(JavaPlugin plugin, boolean folia) {
        this.folia = folia;
        this.plugin = plugin;
    }

    public boolean isFolia() {
        return folia;
    }

    public boolean hasAsyncChunkLoad() {
        // Paper adds this to its runtime World implementation, not Spigot's
        // compile-time World interface. Detection therefore happens per world.
        return asyncChunkMethods.values().stream().anyMatch(Optional::isPresent);
    }

    /**
     * Load (or generate) a chunk without blocking the caller when Paper/Folia
     * async chunk API exists. On Spigot this returns an already-completed
     * future only if the chunk is loaded; otherwise {@code null} so the caller
     * can decide whether a sync generate is allowed.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Chunk> loadChunkAsync(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (world.isChunkLoaded(chunkX, chunkZ)) {
            return CompletableFuture.completedFuture(world.getChunkAt(chunkX, chunkZ));
        }
        Optional<Method> asyncMethod = asyncChunkMethods.computeIfAbsent(
                world.getClass(), this::findChunkAtAsync);
        if (asyncMethod.isEmpty()) {
            return null;
        }
        Method method = asyncMethod.get();
        try {
            Object result = method.invoke(world, chunkX, chunkZ);
            if (result instanceof CompletableFuture<?> future) {
                return (CompletableFuture<Chunk>) future;
            }
        } catch (Throwable t) {
            // fall through
        }
        return null;
    }

    public static int chunkCoord(int block) {
        return block >> 4;
    }

    public static Location atBlock(World world, int x, double y, int z) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private Optional<Method> findChunkAtAsync(Class<?> worldClass) {
        try {
            Method method = worldClass.getMethod("getChunkAtAsync", int.class, int.class);
            if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
                plugin.getLogger().info("Paper async chunk loading detected — RTP will not generate chunks on the tick thread.");
                return Optional.of(method);
            }
        } catch (NoSuchMethodException ignored) {
            plugin.getLogger().info("Spigot/CraftBukkit detected — RTP will prefer loaded chunks and cap sync generation.");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Could not inspect getChunkAtAsync", t);
        }
        return Optional.empty();
    }
}
