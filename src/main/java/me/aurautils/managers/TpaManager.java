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
 * Key = target UUID (who will accept/deny), Value = pending request details.
 */
public class TpaManager {

    public enum TpaType {
        /** Requester teleports to the target (/tpa). */
        TO_TARGET,
        /** Target teleports to the requester (/tpahere). */
        TO_REQUESTER
    }

    private record PendingRequest(UUID requesterId, TpaType type) {
    }

    private final AuraUtils plugin;

    /** target → pending request */
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();
    /** target → expiry task */
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();

    public TpaManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /** Send a TPA request from `from` to `to`. Returns false if one is already pending. */
    public boolean sendRequest(Player from, Player to) {
        return sendRequest(from, to, TpaType.TO_TARGET);
    }

    /** Send a TPA request from `from` to `to` with the given type. Returns false if one is already pending. */
    public boolean sendRequest(Player from, Player to, TpaType type) {
        if (pendingRequests.containsKey(to.getUniqueId())) return false;

        pendingRequests.put(to.getUniqueId(), new PendingRequest(from.getUniqueId(), type));

        UUID targetId = to.getUniqueId();
        UUID requesterId = from.getUniqueId();
        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            expire(targetId, requesterId);
        }, timeout * 20L);

        expiryTasks.put(to.getUniqueId(), task);
        return true;
    }

    /** Returns the requester UUID pending for `target`, or null. */
    public UUID getPendingRequester(UUID targetId) {
        PendingRequest pending = pendingRequests.get(targetId);
        return pending == null ? null : pending.requesterId();
    }

    public TpaType getPendingType(UUID targetId) {
        PendingRequest pending = pendingRequests.get(targetId);
        return pending == null ? null : pending.type();
    }

    public boolean hasPending(UUID targetId) {
        return pendingRequests.containsKey(targetId);
    }

    /** Accept: teleport the moving player, then clean up. */
    public void accept(Player target) {
        PendingRequest pending = pendingRequests.get(target.getUniqueId());
        if (pending == null) return;
        UUID requesterId = pending.requesterId();
        TpaType type = pending.type();
        cancelTask(target.getUniqueId());
        clear(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            return;
        }

        Player traveler = type == TpaType.TO_TARGET ? requester : target;
        Player destinationHolder = type == TpaType.TO_TARGET ? target : requester;
        String destinationName = destinationHolder.getName();

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(traveler, destinationHolder.getLocation(), tpCountdown);
            traveler.sendMessage(plugin.prefix("&eYour TPA request was accepted. Teleporting in &6" + tpCountdown + "&e..."));
            destinationHolder.sendMessage(plugin.prefix("&e" + traveler.getName() + " &ahas a teleport scheduled to you."));
        } else {
            plugin.getBackManager().skipNextRecord(traveler.getUniqueId());
            traveler.teleport(destinationHolder.getLocation());
            traveler.sendMessage(plugin.prefix("&aTeleported to &e" + destinationName + "&a!"));
            destinationHolder.sendMessage(plugin.prefix("&e" + traveler.getName() + " &ahas teleported to you."));
        }
    }

    /** Deny: notify both players, then clean up. */
    public void deny(Player target) {
        PendingRequest pending = pendingRequests.get(target.getUniqueId());
        if (pending == null) return;
        UUID requesterId = pending.requesterId();
        cancelTask(target.getUniqueId());
        clear(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.prefix("&c" + target.getName() + " &cdeclined your TPA request."));
        }
        target.sendMessage(plugin.prefix("&cYou denied &e" + 
            (requester != null ? requester.getName() : "the") + "&c's TPA request."));
    }

    private void expire(UUID targetId, UUID requesterId) {
        PendingRequest pending = pendingRequests.get(targetId);
        if (pending == null || !pending.requesterId().equals(requesterId)) {
            return;
        }
        cancelTask(targetId);
        clear(targetId);

        Player requester = plugin.getServer().getPlayer(requesterId);
        Player target = plugin.getServer().getPlayer(targetId);
        String targetName = target != null ? target.getName() : "the player";
        String requesterName = requester != null ? requester.getName() : "a player";

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.prefix("&eYour TPA request to &b" + targetName + " &eexpired."));
        }
        if (target != null && target.isOnline()) {
            target.sendMessage(plugin.prefix("&eTPA request from &b" + requesterName + " &eexpired."));
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
