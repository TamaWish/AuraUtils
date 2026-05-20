package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import me.aurautils.managers.TeleportHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending TPA requests and handles automatic expiry.
 * Key = target UUID (who will accept/deny), Value = requester UUID.
 */
public class TpaManager {

    private final AuraUtils plugin;

    /** target → requester */
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    /** target → expiry task */
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();

    public TpaManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /** Send a TPA request from `from` to `to`. Returns false if one is already pending. */
    public boolean sendRequest(Player from, Player to) {
        if (pendingRequests.containsKey(to.getUniqueId())) return false;

        pendingRequests.put(to.getUniqueId(), from.getUniqueId());

        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            expire(to, from);
        }, timeout * 20L);

        expiryTasks.put(to.getUniqueId(), task);
        return true;
    }

    /** Returns the requester UUID pending for `target`, or null. */
    public UUID getPendingRequester(UUID targetId) {
        return pendingRequests.get(targetId);
    }

    public boolean hasPending(UUID targetId) {
        return pendingRequests.containsKey(targetId);
    }

    /** Accept: teleport requester to target, then clean up. */
    public void accept(Player target) {
        UUID requesterId = pendingRequests.get(target.getUniqueId());
        if (requesterId == null) return;
        cancelTask(target.getUniqueId());
        clear(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
            TeleportHelper helper = new TeleportHelper(plugin);
            if (tpCountdown > 0) {
                helper.scheduleTeleport(requester, target.getLocation(), tpCountdown);
                requester.sendMessage(plugin.prefix("&eYour TPA request was accepted. Teleporting in &6" + tpCountdown + "&e..."));
                target.sendMessage(plugin.prefix("&e" + requester.getName() + " &ahas a teleport scheduled to you."));
            } else {
                requester.teleport(target.getLocation());
                requester.sendMessage(plugin.prefix("&aTeleported to &e" + target.getName() + "&a!"));
                target.sendMessage(plugin.prefix("&e" + requester.getName() + " &ahas teleported to you."));
            }
        }
    }

    /** Deny: notify both players, then clean up. */
    public void deny(Player target) {
        UUID requesterId = pendingRequests.get(target.getUniqueId());
        if (requesterId == null) return;
        cancelTask(target.getUniqueId());
        clear(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.prefix("&c" + target.getName() + " &cdeclined your TPA request."));
        }
        target.sendMessage(plugin.prefix("&cYou denied &e" + 
            (requester != null ? requester.getName() : "the") + "&c's TPA request."));
    }

    private void expire(Player target, Player requester) {
        clear(target.getUniqueId());
        if (requester.isOnline()) {
            requester.sendMessage(plugin.prefix("&eYour TPA request to &b" + target.getName() + " &eexpired."));
        }
        if (target.isOnline()) {
            target.sendMessage(plugin.prefix("&eTPA request from &b" + requester.getName() + " &eexpired."));
        }
    }

    private void cancelTask(UUID id) {
        BukkitTask task = expiryTasks.remove(id);
        if (task != null) task.cancel();
    }

    private void clear(UUID targetId) {
        pendingRequests.remove(targetId);
        expiryTasks.remove(targetId);
    }

    /** Cancel all pending tasks on shutdown. */
    public void cancelAll() {
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        pendingRequests.clear();
    }
}
