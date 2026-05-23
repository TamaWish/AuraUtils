package me.aurautils.storage;

import me.aurautils.AuraUtils;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitTaskExecutor implements TaskExecutor {

    private final AuraUtils plugin;

    public BukkitTaskExecutor(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isPluginEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public Cancellable runSyncLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }
}
