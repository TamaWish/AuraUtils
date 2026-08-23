package me.aurautils.util;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Thin cross-platform scheduler facade over FoliaLib.
 * Works on Spigot, Paper and Folia with a single API.
 *
 * <ul>
 *   <li>Entity tasks follow the player across regions on Folia.</li>
 *   <li>Location tasks run on the owning region.</li>
 *   <li>Global / async tasks behave correctly on all platforms.</li>
 * </ul>
 */
public final class SchedulerHelper {

    private final FoliaLib foliaLib;

    public SchedulerHelper(AuraUtils plugin) {
        this.foliaLib = new FoliaLib(plugin);
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    // ------------------------------------------------------------------
    // Entity-bound (preferred for anything that touches a Player)
    // ------------------------------------------------------------------

    public void runAtEntity(Entity entity, Runnable task) {
        foliaLib.getScheduler().runAtEntity(entity, wt -> task.run());
    }

    public WrappedTask runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runAtEntityLater(entity, task, delayTicks);
    }

    /**
     * Repeating entity task. Returns a cancellable WrappedTask.
     * Prefer this over the Consumer form when you need to store/cancel the task.
     */
    public WrappedTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return foliaLib.getScheduler().runAtEntityTimer(entity, task, delayTicks, periodTicks);
    }

    // ------------------------------------------------------------------
    // Location / region
    // ------------------------------------------------------------------

    public void runAtLocation(Location location, Runnable task) {
        foliaLib.getScheduler().runAtLocation(location, wt -> task.run());
    }

    public WrappedTask runAtLocationLater(Location location, Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runAtLocationLater(location, task, delayTicks);
    }

    public WrappedTask runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return foliaLib.getScheduler().runAtLocationTimer(location, task, delayTicks, periodTicks);
    }

    // ------------------------------------------------------------------
    // Global (no specific region)
    // ------------------------------------------------------------------

    public void runNextTick(Runnable task) {
        foliaLib.getScheduler().runNextTick(wt -> task.run());
    }

    public WrappedTask runLater(Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runLater(task, delayTicks);
    }

    public WrappedTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return foliaLib.getScheduler().runTimer(task, delayTicks, periodTicks);
    }

    // ------------------------------------------------------------------
    // Async
    // ------------------------------------------------------------------

    public void runAsync(Runnable task) {
        foliaLib.getScheduler().runAsync(wt -> task.run());
    }

    public WrappedTask runAsyncLater(Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runLaterAsync(task, delayTicks);
    }

    public WrappedTask runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        return foliaLib.getScheduler().runLaterAsync(task, delay, unit);
    }

    // ------------------------------------------------------------------
    // Teleport
    // ------------------------------------------------------------------

    /**
     * Teleport an entity asynchronously when the platform supports it
     * (Paper / Folia). Falls back safely on Spigot.
     */
    public void teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause,
                              Consumer<Boolean> resultCallback) {
        foliaLib.getScheduler().teleportAsync(entity, location, cause)
                .thenAccept(success -> {
                    if (resultCallback != null) {
                        resultCallback.accept(success);
                    }
                });
    }

    public void teleportAsync(Entity entity, Location location, Consumer<Boolean> resultCallback) {
        teleportAsync(entity, location, PlayerTeleportEvent.TeleportCause.PLUGIN, resultCallback);
    }

    // ------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------

    public void cancelAllTasks() {
        foliaLib.getScheduler().cancelAllTasks();
    }
}
