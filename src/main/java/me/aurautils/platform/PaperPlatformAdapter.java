package me.aurautils.platform;

import me.aurautils.AuraUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Paper enhancements loaded via reflection so the plugin compiles against Spigot API only.
 */
public class PaperPlatformAdapter extends SpigotPlatformAdapter {

    private final Method getChunkAtAsync;
    private final boolean asyncChunkLoadAvailable;
    private final Method createInventoryWithComponent;
    private final boolean componentInventoryTitlesAvailable;
    private final Method hasChangedPosition;

    public PaperPlatformAdapter(AuraUtils plugin) {
        super(plugin);
        Method chunkMethod = null;
        try {
            chunkMethod = World.class.getMethod(
                    "getChunkAtAsync",
                    int.class, int.class, boolean.class, boolean.class, Consumer.class);
        } catch (NoSuchMethodException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Paper detected but World.getChunkAtAsync(..., Consumer) is missing; RTP uses conservative Spigot-style search.");
        }
        this.getChunkAtAsync = chunkMethod;
        this.asyncChunkLoadAvailable = chunkMethod != null;

        Method inventoryMethod = null;
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            inventoryMethod = org.bukkit.Bukkit.class.getMethod(
                    "createInventory", InventoryHolder.class, int.class, componentClass);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            plugin.getLogger().log(Level.FINE,
                    "Paper detected but Bukkit.createInventory(..., Component) is missing; menu titles use legacy formatting.");
        }
        this.createInventoryWithComponent = inventoryMethod;
        this.componentInventoryTitlesAvailable = inventoryMethod != null;

        Method positionMethod = null;
        try {
            positionMethod = PlayerMoveEvent.class.getMethod("hasChangedPosition");
        } catch (NoSuchMethodException ignored) {
        }
        this.hasChangedPosition = positionMethod;
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
    public Inventory createInventory(InventoryHolder owner, int size, net.kyori.adventure.text.Component title) {
        if (componentInventoryTitlesAvailable) {
            try {
                Object serverComponent = plugin.getMessages().toServerComponent(title);
                if (serverComponent != null) {
                    return (Inventory) createInventoryWithComponent.invoke(null, owner, size, serverComponent);
                }
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().log(Level.FINE, "Component inventory title failed, falling back to legacy", e);
            }
        }
        return super.createInventory(owner, size, title);
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
        return hasChangedPosition != null;
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
