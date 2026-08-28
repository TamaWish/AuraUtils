package com.lozaine.aurautils.managers;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending TPA requests and handles automatic expiry.
 * Key = target UUID (who will accept/deny), Value = request details.
 *
 * <p>Only one pending request is allowed per target at a time.
 * Requesters can cancel their own outgoing requests via
 * {@link #cancelOutgoing(Player)} (wired to /tpacancel).
 */
public class TpaManager {

    private final AuraUtils plugin;

    /** target → pending request (concurrent for Folia cross-region safety) */
    private final Map<UUID, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    /** target → expiry task */
    private final Map<UUID, WrappedTask> expiryTasks = new ConcurrentHashMap<>();

    public TpaManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Result of attempting to send a TPA (normal pending, trusted instant, or failure).
     */
    public enum SendResult {
        /** Request stored; target must /tpaccept. */
        PENDING,
        /** Target trusts the requester — teleport scheduled/executed immediately. */
        TRUSTED_INSTANT,
        /** Target already has a pending request. */
        BUSY,
        /** Plugin disabled or other hard failure. */
        FAILED
    }

    /**
     * Send a TPA request from {@code from} to {@code to}.
     * If {@code to} has {@code from} on their trusted list, the request is
     * auto-accepted (instant trusted TPA) and never enters the pending map.
     */
    public SendResult sendRequest(Player from, Player to) {
        if (!plugin.isEnabled()) {
            return SendResult.FAILED;
        }
        // Trusted / instant TPA: target has added the requester to their list
        if (plugin.getPlayerDataManager().isTrusted(to.getUniqueId(), from.getUniqueId())) {
            executeTrustedTeleport(from, to);
            return SendResult.TRUSTED_INSTANT;
        }

        UUID targetId = to.getUniqueId();
        PendingRequest request = new PendingRequest(
                from.getUniqueId(),
                from.getName(),
                to.getName()
        );
        // The target may be on another Folia region thread.  This must be one
        // atomic operation; containsKey followed by put allowed two requests.
        if (pendingRequests.putIfAbsent(targetId, request) != null) {
            return SendResult.BUSY;
        }

        int timeout = Math.max(1, plugin.getConfig().getInt("tpa.timeout", 60));
        WrappedTask task = plugin.getScheduler().runLater(() -> {
            expire(targetId, request);
        }, timeout * 20L);

        WrappedTask previous = expiryTasks.putIfAbsent(targetId, task);
        if (previous != null || pendingRequests.get(targetId) != request) {
            task.cancel();
        }
        return SendResult.PENDING;
    }

    /**
     * Auto-accept path when the target trusts the requester.
     * Uses the same countdown system as a normal accept (unless countdown is 0
     * or config forces trusted-instant, or the requester has aura.teleport.bypass).
     * Runs teleport scheduling on the requester's entity thread for Folia safety.
     */
    private void executeTrustedTeleport(Player requester, Player target) {
        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        if (plugin.getConfig().getBoolean("tpa.trusted-instant", false)
                || requester.hasPermission("aura.teleport.bypass")) {
            tpCountdown = 0;
        }

        // One clear message each; wording matches countdown vs instant
        var msg = plugin.messages();
        if (tpCountdown > 0) {
            msg.send(requester, "tpa.trusted-countdown-requester", "player", target.getName());
            msg.send(target, "tpa.trusted-countdown-target", "player", requester.getName());
        } else {
            msg.send(requester, "tpa.trusted-instant-requester", "player", target.getName());
            msg.send(target, "tpa.trusted-instant-target", "player", requester.getName());
        }

        var helper = plugin.getTeleportHelper();
        final int countdown = tpCountdown;
        plugin.getScheduler().runAtEntity(requester, () -> {
            if (!requester.isOnline() || !target.isOnline()) {
                return;
            }
            if (countdown > 0) {
                // scheduleTeleport handles countdown UI and success messages
                helper.scheduleTeleport(requester, target.getLocation(), countdown, target.getName());
            } else {
                // Instant path: messages already sent above (no extra success spam)
                helper.teleportExact(requester, target.getLocation());
            }
        });
    }

    /** Returns the requester UUID pending for {@code targetId}, or null. */
    public UUID getPendingRequester(UUID targetId) {
        PendingRequest request = pendingRequests.get(targetId);
        return request != null ? request.requesterId : null;
    }

    /** Returns the stored requester name for a pending request, or null. */
    public String getPendingRequesterName(UUID targetId) {
        PendingRequest request = pendingRequests.get(targetId);
        return request != null ? request.requesterName : null;
    }

    /** True if {@code targetId} has an incoming pending request. */
    public boolean hasPending(UUID targetId) {
        return pendingRequests.containsKey(targetId);
    }

    /** True if this player has at least one outgoing request waiting. */
    public boolean hasOutgoing(UUID requesterId) {
        for (PendingRequest request : pendingRequests.values()) {
            if (request.requesterId.equals(requesterId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Accept: clear the request, notify both sides, then schedule (or run) the teleport
     * for the requester to the target's current location.
     */
    public void accept(Player target) {
        UUID targetId = target.getUniqueId();
        PendingRequest request = pendingRequests.remove(targetId);
        if (request == null) {
            return;
        }

        cancelTask(targetId);

        Player requester = plugin.getServer().getPlayer(request.requesterId);
        if (requester == null || !requester.isOnline()) {
            plugin.messages().send(target, "tpa.requester-offline", "player", request.requesterName);
            return;
        }

        // Snapshot dest on the target's thread, then hop to the requester (Folia-safe).
        final Location dest = target.getLocation().clone();
        final String targetName = target.getName();
        final String requesterName = requester.getName();

        plugin.messages().send(target, "tpa.accepted-target", "player", requesterName);

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        var helper = plugin.getTeleportHelper();
        plugin.getScheduler().runAtEntity(requester, () -> {
            if (!requester.isOnline()) {
                return;
            }
            plugin.messages().send(requester, "tpa.accepted-requester", "player", targetName);
            if (tpCountdown > 0) {
                helper.scheduleTeleport(requester, dest, tpCountdown, targetName);
            } else {
                helper.teleportExact(requester, dest, ok -> {
                    if (!requester.isOnline()) {
                        return;
                    }
                    if (Boolean.TRUE.equals(ok)) {
                        plugin.messages().send(requester, "tpa.arrived-requester", "player", targetName);
                        Player stillTarget = plugin.getServer().getPlayer(target.getUniqueId());
                        if (stillTarget != null && stillTarget.isOnline()) {
                            plugin.getScheduler().runAtEntity(stillTarget, () ->
                                    plugin.messages().send(stillTarget, "tpa.arrived-target", "player", requesterName));
                        }
                    } else {
                        plugin.messages().send(requester, "tpa.failed-to", "player", targetName);
                    }
                });
            }
        });
    }

    /**
     * Deny: notify both players (when online), then clean up.
     */
    public void deny(Player target) {
        UUID targetId = target.getUniqueId();
        PendingRequest request = pendingRequests.remove(targetId);
        if (request == null) {
            return;
        }

        cancelTask(targetId);

        Player requester = plugin.getServer().getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            plugin.messages().send(requester, "tpa.denied-requester", "player", target.getName());
        }
        plugin.messages().send(target, "tpa.denied-target", "player", request.requesterName);
    }

    /**
     * Cancel all outgoing TPA requests from this player (requester side).
     * Notifies online targets. Does not affect teleport countdowns.
     *
     * @return number of requests cancelled
     */
    public int cancelOutgoing(Player requester) {
        if (requester == null) {
            return 0;
        }
        UUID requesterId = requester.getUniqueId();
        String requesterName = requester.getName();
        int cancelled = 0;

        for (Map.Entry<UUID, PendingRequest> entry : pendingRequests.entrySet()) {
            if (!entry.getValue().requesterId.equals(requesterId)
                    || !pendingRequests.remove(entry.getKey(), entry.getValue())) {
                continue;
            }
            UUID targetId = entry.getKey();
            cancelTask(targetId);
            cancelled++;

            Player target = plugin.getServer().getPlayer(targetId);
            if (target != null && target.isOnline()) {
                plugin.messages().send(target, "tpa.cancelled-target", "player", requesterName);
            }
        }
        return cancelled;
    }

    /**
     * Called when a player quits. Cleans up any request where they are the target
     * or the requester, and notifies the other party when possible.
     */
    public void handleQuit(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();

        // Case 1: quitting player is the target of a pending request
        PendingRequest incoming = pendingRequests.remove(playerId);
        if (incoming != null) {
            cancelTask(playerId);

            Player requester = plugin.getServer().getPlayer(incoming.requesterId);
            if (requester != null && requester.isOnline()) {
                plugin.messages().send(requester, "tpa.quit-requester", "player", playerName);
            }
        }

        // Case 2: quitting player is the requester of one or more pending requests
        for (Map.Entry<UUID, PendingRequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().requesterId.equals(playerId)
                    && pendingRequests.remove(entry.getKey(), entry.getValue())) {
                UUID targetId = entry.getKey();
                cancelTask(targetId);

                Player target = plugin.getServer().getPlayer(targetId);
                if (target != null && target.isOnline()) {
                    plugin.messages().send(target, "tpa.quit-target", "player", playerName);
                }
            }
        }
    }

    private void expire(UUID targetId, PendingRequest request) {
        // Only expire if this request is still the active one
        if (!pendingRequests.remove(targetId, request)) {
            return;
        }

        // Task is already firing; remove the reference so cancelAll stays clean
        expiryTasks.remove(targetId);

        Player requester = plugin.getServer().getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            plugin.messages().send(requester, "tpa.expired-requester", "player", request.targetName);
        }

        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null && target.isOnline()) {
            plugin.messages().send(target, "tpa.expired-target", "player", request.requesterName);
        }
    }

    private void cancelTask(UUID id) {
        WrappedTask task = expiryTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private void clear(UUID targetId) {
        pendingRequests.remove(targetId);
        expiryTasks.remove(targetId);
    }

    /** Cancel all pending tasks on shutdown. Never schedules anything. */
    public void cancelAll() {
        for (WrappedTask task : expiryTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        expiryTasks.clear();
        pendingRequests.clear();
    }

    /** Immutable snapshot of a pending request. */
    private static final class PendingRequest {
        final UUID requesterId;
        final String requesterName;
        final String targetName;

        PendingRequest(UUID requesterId, String requesterName, String targetName) {
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.targetName = targetName;
        }
    }
}
