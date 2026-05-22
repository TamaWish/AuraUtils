package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.platform.ChunkLoadPolicy;
import me.aurautils.platform.PlatformAdapter;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportHelper {

    public static final class PendingSession {
        private final Location start;
        private final Location destination;
        private final boolean horizontalOnly;
        private final String successMessageKey;
        private final MessagePlaceholders successPlaceholders;
        private volatile BukkitTask countdownTask;

        PendingSession(Location start, Location destination, boolean horizontalMovementOnly,
                       String successMessageKey, MessagePlaceholders successPlaceholders) {
            this.start = start.clone();
            this.destination = destination.clone();
            this.horizontalOnly = horizontalMovementOnly;
            this.successMessageKey = successMessageKey;
            this.successPlaceholders = successPlaceholders;
        }

        public Location getStart() {
            return start;
        }

        public Location getDestination() {
            return destination;
        }

        public boolean isHorizontalOnly() {
            return horizontalOnly;
        }

        public String getSuccessMessageKey() {
            return successMessageKey;
        }

        public MessagePlaceholders getSuccessPlaceholders() {
            return successPlaceholders;
        }

        void setCountdownTask(BukkitTask countdownTask) {
            this.countdownTask = countdownTask;
        }

        void cancelTask() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }
    }

    private final AuraUtils plugin;
    private final PlatformAdapter platform;
    private final Map<UUID, PendingSession> activeSessions = new ConcurrentHashMap<>();

    public TeleportHelper(AuraUtils plugin) {
        this.plugin = plugin;
        this.platform = plugin.getPlatform();
    }

    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public PendingSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    public void onPlayerMoved(Player player, org.bukkit.event.player.PlayerMoveEvent event) {
        PendingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (platform.hasPlayerMoved(event, session.isHorizontalOnly())) {
            cancelDueToMovement(player);
        }
    }

    public boolean cancelPendingTeleport(Player player) {
        PendingSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }
        session.cancelTask();
        return true;
    }

    public void cancelDueToMovement(Player player) {
        if (!cancelPendingTeleport(player)) {
            return;
        }
        if (player.isOnline()) {
            plugin.send(player, "teleport.cancelled-movement");
        }
    }

    public void scheduleTeleport(Player player, Location destination, int seconds) {
        scheduleTeleport(player, destination, seconds, false, "teleport.success-default", MessagePlaceholders.empty());
    }

    public void scheduleTeleport(Player player, Location destination, int seconds,
                                 boolean horizontalMovementOnly, String successMessageKey) {
        scheduleTeleport(player, destination, seconds, horizontalMovementOnly, successMessageKey, MessagePlaceholders.empty());
    }

    public void scheduleTeleport(Player player, Location destination, int seconds,
                                 boolean horizontalMovementOnly, String successMessageKey,
                                 MessagePlaceholders successPlaceholders) {
        final Location dest = destination.clone();
        UUID playerId = player.getUniqueId();
        endSession(playerId);

        PendingSession session = new PendingSession(
                player.getLocation(), dest, horizontalMovementOnly, successMessageKey, successPlaceholders);
        activeSessions.put(playerId, session);

        BukkitRunnable countdown = new BukkitRunnable() {
            int remaining = Math.max(0, seconds);
            final Location start = session.getStart();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    endSession(playerId);
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    endSession(playerId);
                    executeTeleport(player, session);
                    cancel();
                    return;
                }

                if (player.getLocation().getWorld() != null && start.getWorld() != null
                        && !player.getLocation().getWorld().equals(start.getWorld())) {
                    endSession(playerId);
                    plugin.send(player, "teleport.cancelled");
                    cancel();
                    return;
                }

                if (!platform.usesEventBasedMovementDetection()
                        && platform.hasPlayerMoved(
                        syntheticMoveEvent(player, start, player.getLocation()),
                        horizontalMovementOnly)) {
                    cancelDueToMovement(player);
                    cancel();
                    return;
                }

                plugin.send(player, "teleport.countdown",
                        MessagePlaceholders.of("seconds", String.valueOf(remaining)));
                remaining--;
            }
        };

        BukkitTask task = countdown.runTaskTimer(plugin, 20L, 20L);
        session.setCountdownTask(task);
    }

    private void executeTeleport(Player player, PendingSession session) {
        Location dest = session.getDestination();
        boolean useAsync = plugin.getConfig().getBoolean("teleport.async-chunk-load", true)
                && platform.supportsAsyncChunkLoading();
        boolean syncFallback = plugin.getConfig().getBoolean("teleport.sync-chunk-fallback", true);

        ChunkLoadPolicy policy;
        if (useAsync) {
            policy = ChunkLoadPolicy.ASYNC;
        } else if (syncFallback) {
            policy = ChunkLoadPolicy.SYNC_FALLBACK;
        } else {
            policy = ChunkLoadPolicy.LOADED_ONLY;
        }

        platform.whenChunkReady(
                dest,
                policy,
                true,
                true,
                () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    UUID playerId = player.getUniqueId();
                    plugin.getBackManager().skipNextRecord(playerId);
                    player.teleport(dest, PlayerTeleportEvent.TeleportCause.COMMAND);
                    plugin.send(player, session.getSuccessMessageKey(), session.getSuccessPlaceholders());
                },
                () -> {
                    if (player.isOnline()) {
                        plugin.send(player, "teleport.chunk-unavailable");
                    }
                }
        );
    }

    private void endSession(UUID playerId) {
        PendingSession existing = activeSessions.remove(playerId);
        if (existing != null) {
            existing.cancelTask();
        }
    }

    public void clearSession(UUID playerId) {
        endSession(playerId);
    }

    private static org.bukkit.event.player.PlayerMoveEvent syntheticMoveEvent(Player player, Location from, Location to) {
        return new org.bukkit.event.player.PlayerMoveEvent(player, from, to);
    }
}
