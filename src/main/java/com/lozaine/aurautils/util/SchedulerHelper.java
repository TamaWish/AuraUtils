package com.lozaine.aurautils.util;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.lozaine.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Thin cross-platform scheduler facade over FoliaLib.
 * Works on Spigot, Paper and Folia with a single API.
 */
public final class SchedulerHelper {

    private final FoliaLib foliaLib;
    private final Platform platform;

    public SchedulerHelper(AuraUtils plugin) {
        this.foliaLib = new FoliaLib(plugin);
        // Timers may intentionally start at delay 0; FoliaLib bumps those to 1 tick.
        this.foliaLib.disableInvalidTickValueWarning();
        this.platform = new Platform(plugin, foliaLib.isFolia());
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public Platform platform() {
        return platform;
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    public void runAtEntity(Entity entity, Runnable task) {
        if (entity == null) {
            return;
        }
        foliaLib.getScheduler().runAtEntity(entity, wt -> task.run());
    }

    public WrappedTask runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runAtEntityLater(entity, task, delayTicks);
    }

    public WrappedTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return foliaLib.getScheduler().runAtEntityTimer(entity, task, delayTicks, periodTicks);
    }

    public void runAtLocation(Location location, Runnable task) {
        if (location == null || !location.isWorldLoaded()) {
            return;
        }
        foliaLib.getScheduler().runAtLocation(location, wt -> task.run());
    }

    public WrappedTask runAtLocationLater(Location location, Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runAtLocationLater(location, task, delayTicks);
    }

    public WrappedTask runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return foliaLib.getScheduler().runAtLocationTimer(location, task, delayTicks, periodTicks);
    }

    public void runNextTick(Runnable task) {
        foliaLib.getScheduler().runNextTick(wt -> task.run());
    }

    public WrappedTask runLater(Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runLater(task, delayTicks);
    }

    public WrappedTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return foliaLib.getScheduler().runTimer(task, delayTicks, periodTicks);
    }

    public void runAsync(Runnable task) {
        foliaLib.getScheduler().runAsync(wt -> task.run());
    }

    public WrappedTask runAsyncLater(Runnable task, long delayTicks) {
        return foliaLib.getScheduler().runLaterAsync(task, delayTicks);
    }

    public WrappedTask runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        return foliaLib.getScheduler().runLaterAsync(task, delay, unit);
    }

    /**
     * Platform-aware teleport. Paper/Folia use async chunk load + teleport;
     * Spigot falls back to next-tick sync teleport. The callback always runs
     * on the entity scheduler so sounds/messages are legal on Folia.
     */
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location,
                                                    PlayerTeleportEvent.TeleportCause cause,
                                                    Consumer<Boolean> resultCallback) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (entity == null || location == null) {
            complete(result, resultCallback, false);
            return result;
        }
        foliaLib.getScheduler().teleportAsync(entity, location, cause)
                .whenComplete((success, error) -> {
                    boolean ok = error == null && Boolean.TRUE.equals(success);
                    runAtEntity(entity, () -> complete(result, resultCallback, ok));
                });
        return result;
    }

    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location,
                                                    Consumer<Boolean> resultCallback) {
        return teleportAsync(entity, location, PlayerTeleportEvent.TeleportCause.PLUGIN, resultCallback);
    }

    private static void complete(CompletableFuture<Boolean> result,
                                 Consumer<Boolean> callback,
                                 boolean success) {
        if (callback != null) {
            try {
                callback.accept(success);
            } catch (Exception ignored) {
                // plugin may have unloaded mid-callback
            }
        }
        result.complete(success);
    }

    public void cancelAllTasks() {
        foliaLib.getScheduler().cancelAllTasks();
    }
}
