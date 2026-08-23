package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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

    /** target → pending request */
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();
    /** target → expiry task */
    private final Map<UUID, WrappedTask> expiryTasks = new HashMap<>();

    public TpaManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Send a TPA request from {@code from} to {@code to}.
     * Returns false if the target already has a pending request or the plugin is disabled.
     */
    public boolean sendRequest(Player from, Player to) {
        if (!plugin.isEnabled()) {
            return false;
        }
        if (pendingRequests.containsKey(to.getUniqueId())) {
            return false;
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
        return true;
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
