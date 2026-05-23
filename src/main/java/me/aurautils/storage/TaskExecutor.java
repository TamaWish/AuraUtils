package me.aurautils.storage;

/**
 * Schedules async/deferred work for {@link me.aurautils.managers.PlayerDataManager}.
 * Use {@link DirectTaskExecutor} in unit tests for synchronous persistence.
 */
public interface TaskExecutor {

    boolean isPluginEnabled();

    void runAsync(Runnable task);

    Cancellable runSyncLater(Runnable task, long delayTicks);
}
