package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.Iterator;
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
        if (pendingRequests.containsKey(to.getUniqueId())) {
            return SendResult.BUSY;
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
        pendingRequests.put(targetId, request);

        int timeout = Math.max(1, plugin.getConfig().getInt("tpa.timeout", 60));
        WrappedTask task = plugin.getScheduler().runLater(() -> {
            expire(targetId, request);
        }, timeout * 20L);

        expiryTasks.put(targetId, task);
        return SendResult.PENDING;
    }

    /**
     * Auto-accept path when the target trusts the requester.
     * Uses the same countdown system as a normal accept (unless countdown is 0
     * or config forces trusted-instant, or the requester has aura.teleport.bypass).
     * Runs teleport scheduling on the requester's entity thread for Folia safety.
     */
    private void executeTrustedTeleport(Player requester, Player target) {
        requester.sendMessage(plugin.prefix("&aTrusted TPA: teleported to &e" + target.getName()
                + " &a(you are on their trusted list)."));
        target.sendMessage(plugin.prefix("&e" + requester.getName()
                + " &ateleported to you via trusted TPA."));

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        if (plugin.getConfig().getBoolean("tpa.trusted-instant", false)) {
            tpCountdown = 0;
        }

        var helper = plugin.getTeleportHelper();
        final int countdown = tpCountdown;
        plugin.getScheduler().runAtEntity(requester, () -> {
            if (!requester.isOnline() || !target.isOnline()) {
                return;
            }
            if (countdown > 0) {
                helper.scheduleTeleport(requester, target.getLocation(), countdown, target.getName());
            } else if (helper.teleportExact(requester, target.getLocation())) {
                requester.sendMessage(plugin.prefix("&aTeleported to &b" + target.getName() + "&a!"));
                if (target.isOnline()) {
                    target.sendMessage(plugin.prefix("&e" + requester.getName() + " &ahas teleported to you."));
                }
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
        PendingRequest request = pendingRequests.get(targetId);
        if (request == null) {
            return;
        }

        cancelTask(targetId);
        clear(targetId);

        Player requester = plugin.getServer().getPlayer(request.requesterId);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(plugin.prefix("&cThe requester (&e" + request.requesterName + "&c) is no longer online."));
            return;
        }

        // Explicit accept feedback (countdown messages alone are not always obvious)
        requester.sendMessage(plugin.prefix("&a" + target.getName() + " &aaccepted your TPA request."));
        target.sendMessage(plugin.prefix("&aYou accepted &e" + requester.getName() + "&a's TPA request."));

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        var helper = plugin.getTeleportHelper();
        if (tpCountdown > 0) {
            helper.scheduleTeleport(requester, target.getLocation(), tpCountdown, target.getName());
        } else if (helper.teleportExact(requester, target.getLocation())) {
            requester.sendMessage(plugin.prefix("&aTeleported to &b" + target.getName() + "&a!"));
            target.sendMessage(plugin.prefix("&e" + requester.getName() + " &ahas teleported to you."));
        }
    }

    /**
     * Deny: notify both players (when online), then clean up.
     */
    public void deny(Player target) {
        UUID targetId = target.getUniqueId();
        PendingRequest request = pendingRequests.get(targetId);
        if (request == null) {
            return;
        }

        cancelTask(targetId);
        clear(targetId);

        Player requester = plugin.getServer().getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.prefix("&c" + target.getName() + " &cdeclined your TPA request."));
        }
        target.sendMessage(plugin.prefix("&cYou denied &e" + request.requesterName + "&c's TPA request."));
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

        Iterator<Map.Entry<UUID, PendingRequest>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingRequest> entry = it.next();
            if (!entry.getValue().requesterId.equals(requesterId)) {
                continue;
            }
            UUID targetId = entry.getKey();
            it.remove();
            cancelTask(targetId);
            cancelled++;

            Player target = plugin.getServer().getPlayer(targetId);
            if (target != null && target.isOnline()) {
                target.sendMessage(plugin.prefix("&eTPA request from &b" + requesterName + " &ewas cancelled."));
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
        if (pendingRequests.containsKey(playerId)) {
            PendingRequest request = pendingRequests.get(playerId);
            cancelTask(playerId);
            clear(playerId);

            Player requester = plugin.getServer().getPlayer(request.requesterId);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(plugin.prefix("&eYour TPA request to &b" + playerName + " &ewas cancelled (player left)."));
            }
        }

        // Case 2: quitting player is the requester of one or more pending requests
        Iterator<Map.Entry<UUID, PendingRequest>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingRequest> entry = it.next();
            if (entry.getValue().requesterId.equals(playerId)) {
                UUID targetId = entry.getKey();
                it.remove();
                cancelTask(targetId);

                Player target = plugin.getServer().getPlayer(targetId);
                if (target != null && target.isOnline()) {
                    target.sendMessage(plugin.prefix("&eTPA request from &b" + playerName + " &ewas cancelled (player left)."));
                }
            }
        }
    }

    private void expire(UUID targetId, PendingRequest request) {
        // Only expire if this request is still the active one
        PendingRequest current = pendingRequests.get(targetId);
        if (current == null || current != request) {
            return;
        }

        clear(targetId);
        // Task is already firing; remove the reference so cancelAll stays clean
        expiryTasks.remove(targetId);

        Player requester = plugin.getServer().getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.prefix("&eYour TPA request to &b" + request.targetName + " &eexpired."));
        }

        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null && target.isOnline()) {
            target.sendMessage(plugin.prefix("&eTPA request from &b" + request.requesterName + " &eexpired."));
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
