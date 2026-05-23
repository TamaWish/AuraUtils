package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {

    public enum TpaType {
        TO_TARGET,
        TO_REQUESTER
    }

    private record PendingRequest(UUID requesterId, TpaType type) {
    }

    private final AuraUtils plugin;
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();

    public TpaManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public boolean sendRequest(Player from, Player to) {
        return sendRequest(from, to, TpaType.TO_TARGET);
    }

    public boolean sendRequest(Player from, Player to, TpaType type) {
        if (from.getUniqueId().equals(to.getUniqueId())) {
            return false;
        }
        if (pendingRequests.containsKey(to.getUniqueId())) {
            return false;
        }

        pendingRequests.put(to.getUniqueId(), new PendingRequest(from.getUniqueId(), type));

        UUID targetId = to.getUniqueId();
        UUID requesterId = from.getUniqueId();
        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(targetId, requesterId), timeout * 20L);

        expiryTasks.put(to.getUniqueId(), task);
        return true;
    }

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

    public void accept(Player target) {
        PendingRequest pending = pendingRequests.get(target.getUniqueId());
        if (pending == null) {
            return;
        }
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

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = plugin.getTeleportHelper();
        if (tpCountdown > 0) {
            helper.scheduleTeleport(traveler, destinationHolder.getLocation(), tpCountdown);
            plugin.send(traveler, "tpa.accepted-countdown-traveler",
                    MessagePlaceholders.of("seconds", String.valueOf(tpCountdown)));
            plugin.send(destinationHolder, "tpa.accepted-countdown-destination",
                    MessagePlaceholders.of("traveler", traveler.getName()));
        } else {
            plugin.getBackManager().skipNextRecord(traveler.getUniqueId());
            traveler.teleport(destinationHolder.getLocation());
            plugin.send(traveler, "tpa.accepted-instant-traveler",
                    MessagePlaceholders.of("destination", destinationHolder.getName()));
            plugin.send(destinationHolder, "tpa.accepted-instant-destination",
                    MessagePlaceholders.of("traveler", traveler.getName()));
        }
    }

    public void deny(Player target) {
        PendingRequest pending = pendingRequests.get(target.getUniqueId());
        if (pending == null) {
            return;
        }
        UUID requesterId = pending.requesterId();
        cancelTask(target.getUniqueId());
        clear(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            plugin.send(requester, "tpa.denied-requester",
                    MessagePlaceholders.of("target", target.getName()));
        }
        plugin.send(target, "tpa.denied-target", MessagePlaceholders.of("requester",
                requester != null ? requester.getName() : "the player"));
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
            plugin.send(requester, "tpa.expired-requester",
                    MessagePlaceholders.of("target", targetName));
        }
        if (target != null && target.isOnline()) {
            plugin.send(target, "tpa.expired-target",
                    MessagePlaceholders.of("requester", requesterName));
        }
    }

    private void cancelTask(UUID id) {
        BukkitTask task = expiryTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private void clear(UUID targetId) {
        pendingRequests.remove(targetId);
        expiryTasks.remove(targetId);
    }

    public void cancelAll() {
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        pendingRequests.clear();
    }
}
