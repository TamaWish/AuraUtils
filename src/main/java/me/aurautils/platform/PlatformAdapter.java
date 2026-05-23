package me.aurautils.platform;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Runtime platform abstraction. Spigot provides safe defaults; Paper adapters
 * delegate to Paper APIs when present on the server classpath.
 */
public interface PlatformAdapter {

    String getPlatformName();

    boolean isPaper();

    /**
     * Whether {@link #whenChunkReady} can load chunks off-thread (Paper {@code getChunkAtAsync}).
     */
    boolean supportsAsyncChunkLoading();

    /**
     * Creates a chest-style inventory. On Paper, {@code title} is passed as a native Adventure
     * component (gradients/RGB work). On Spigot, the title is flattened to legacy section codes.
     */
    Inventory createInventory(InventoryHolder owner, int size, Component title);

    boolean isChunkLoaded(Location location);

    /**
     * Invokes {@code onReady} on the main server thread once the chunk at {@code location} is usable.
     * {@code onFailed} runs on the main thread when the chunk cannot be obtained under {@code policy}.
     */
    void whenChunkReady(Location location, ChunkLoadPolicy policy, boolean generate, boolean urgent,
                        Runnable onReady, Runnable onFailed);

    /**
     * @param event standard Bukkit move event (Paper adds {@code hasChangedPosition()} at runtime)
     */
    boolean hasPlayerMoved(PlayerMoveEvent event, boolean horizontalOnly);

    /**
     * Whether teleport countdown movement should use {@link me.aurautils.listeners.paper.PaperMoveListener}.
     */
    boolean usesEventBasedMovementDetection();
}
