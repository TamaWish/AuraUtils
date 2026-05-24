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

import java.util.concurrent.atomic.AtomicBoolean;



public class TeleportHelper {



    public static final class PendingSession {

        private final Location start;

        private final Location destination;

        private final boolean horizontalOnly;

        private final String successMessageKey;

        private final MessagePlaceholders successPlaceholders;

        private final Runnable onSuccess;

        private volatile BukkitTask countdownTask;



        PendingSession(Location start, Location destination, boolean horizontalMovementOnly,

                       String successMessageKey, MessagePlaceholders successPlaceholders,

                       Runnable onSuccess) {

            this.start = start.clone();

            this.destination = destination.clone();

            this.horizontalOnly = horizontalMovementOnly;

            this.successMessageKey = successMessageKey;

            this.successPlaceholders = successPlaceholders;

            this.onSuccess = onSuccess;

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

        Runnable getOnSuccess() {
            return onSuccess;
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

        scheduleTeleport(player, destination, seconds, horizontalMovementOnly,

                successMessageKey, successPlaceholders, null);

    }



    public void scheduleTeleport(Player player, Location destination, int seconds,

                                 boolean horizontalMovementOnly, String successMessageKey,

                                 MessagePlaceholders successPlaceholders, Runnable onSuccess) {

        final Location dest = destination.clone();

        UUID playerId = player.getUniqueId();

        endSession(playerId);



        PendingSession session = new PendingSession(

                player.getLocation(), dest, horizontalMovementOnly, successMessageKey, successPlaceholders,

                onSuccess);

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

                    executeCountdownFinish(player, session);

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



    /**
     * Loads the destination chunk when needed, then teleports on the main thread.
     * Used for instant teleports and when a countdown finishes.
     */
    public void executeTeleport(Player player, Location destination, TeleportOptions options) {
        runChunkTeleport(player, destination.clone(), options);
    }



    private void executeCountdownFinish(Player player, PendingSession session) {

        TeleportOptions options = TeleportOptions.builder()

                .countdownSeconds(0)

                .successMessageKey(session.getSuccessMessageKey())

                .successPlaceholders(session.getSuccessPlaceholders())

                .onSuccess(session.getOnSuccess())

                .build();

        runChunkTeleport(player, session.getDestination(), options);

    }



    private void runChunkTeleport(Player player, Location dest, TeleportOptions options) {

        ChunkLoadPolicy policy = options.chunkPolicyOverride() != null

                ? options.chunkPolicyOverride()

                : resolveDefaultChunkPolicy();

        boolean generate = options.generateChunksOverride() != null

                ? options.generateChunksOverride()

                : true;

        boolean urgent = options.asyncUrgentOverride() != null

                ? options.asyncUrgentOverride()

                : true;



        // Only one of timeout, onReady, or onFailed may complete; late callbacks are ignored.
        AtomicBoolean completed = new AtomicBoolean(false);

        int timeoutSeconds = plugin.getAuraConfig().teleportChunkLoadTimeoutSeconds();

        BukkitTask timeoutTask = null;

        if (timeoutSeconds > 0) {

            timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                if (!completed.compareAndSet(false, true)) {

                    return;

                }

                if (player.isOnline() && options.sendChunkFailureMessage()) {

                    plugin.send(player, "teleport.chunk-timeout");

                }

            }, timeoutSeconds * 20L);

        }

        final BukkitTask timeoutRef = timeoutTask;



        Runnable onReady = () -> {

            if (!completed.compareAndSet(false, true)) {

                return;

            }

            if (timeoutRef != null) {

                timeoutRef.cancel();

            }

            if (!player.isOnline()) {

                return;

            }

            player.teleport(dest, options.cause());

            if (options.successMessageKey() != null) {

                plugin.send(player, options.successMessageKey(), options.successPlaceholders());

            }

            Runnable hook = options.onSuccess();

            if (hook != null) {

                hook.run();

            }

        };



        Runnable onFailed = () -> {

            // Timeout may have already notified the player; ignore this late failure.
            if (!completed.compareAndSet(false, true)) {

                return;

            }

            if (timeoutRef != null) {

                timeoutRef.cancel();

            }

            if (player.isOnline() && options.sendChunkFailureMessage()) {

                plugin.send(player, "teleport.chunk-unavailable");

            }

        };



        plugin.getChunkLoadService().whenChunkReady(
                player.getUniqueId(), dest, policy, generate, urgent, onReady, onFailed);

    }



    private ChunkLoadPolicy resolveDefaultChunkPolicy() {

        boolean useAsync = plugin.getAuraConfig().teleportAsyncChunkLoad()

                && platform.supportsAsyncChunkLoading();

        if (useAsync) {

            return ChunkLoadPolicy.ASYNC;

        }

        if (plugin.getAuraConfig().teleportSyncChunkFallback()) {

            return ChunkLoadPolicy.SYNC_FALLBACK;

        }

        return ChunkLoadPolicy.LOADED_ONLY;

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

