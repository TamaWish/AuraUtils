package me.aurautils.storage;

/** Runs all tasks immediately on the calling thread (for unit tests). */
public final class DirectTaskExecutor implements TaskExecutor {

    private volatile boolean enabled = true;

    @Override
    public boolean isPluginEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void runAsync(Runnable task) {
        task.run();
    }

    @Override
    public Cancellable runSyncLater(Runnable task, long delayTicks) {
        task.run();
        return Cancellable.NOOP;
    }
}
