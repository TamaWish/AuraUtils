package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.platform.ChunkLoadCoordinator;
import me.aurautils.platform.ChunkLoadPolicy;
import me.aurautils.platform.ChunkLoadService;
import me.aurautils.platform.PlatformAdapter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Async-friendly RTP: Spigot uses conservative search on already-loaded chunks only;
 * Paper queues chunk loads off the hot path and validates safety on the main thread.
 */
public class AsyncRtpEngine {

    public interface ResultHandler {
        void onFound(Location destination, int blocksAway);

        void onFailed();
    }

    private final AuraUtils plugin;
    private final PlatformAdapter platform;
    private final ChunkLoadService chunkLoads;
    private final TeleportService teleportService;
    private final RtpAdaptiveState adaptiveState = new RtpAdaptiveState();

    public AsyncRtpEngine(AuraUtils plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.platform = plugin.getPlatform();
        this.chunkLoads = plugin.getChunkLoadService();
        this.teleportService = teleportService;
    }

    public void search(Player player, World searchWorld, boolean bypassCooldown, ResultHandler handler) {
        AuraConfig config = plugin.getAuraConfig();
        int radius = adaptiveState.adjustedRadius(config.rtpRadius(), config);
        if (config.rtpAdaptiveMaxRadius() > 0) {
            radius = Math.min(radius, config.rtpAdaptiveMaxRadius());
        }
        radius = clampRadiusToBorder(searchWorld, radius);

        int minDistance = adaptiveState.adjustedMinDistance(config.rtpMinDistance(), config);
        if (minDistance > radius) {
            minDistance = radius;
        }

        int attempts = config.rtpAttempts();
        int attemptsPerTick = config.rtpAttemptsPerTick();
        boolean centerOnPlayer = config.rtpCenterOnPlayer();
        boolean onlyLoadedChunks = config.rtpOnlyLoadedChunksExplicitlySet()
                ? config.rtpOnlyLoadedChunks()
                : !platform.supportsAsyncChunkLoading();
        boolean generateChunks = config.rtpGenerateChunks();
        boolean asyncUrgent = config.rtpAsyncUrgent();
        int maxPendingLoads = config.rtpMaxPendingChunkLoads();
        int maxCandidates = config.rtpMaxCandidates();
        int solidBlocksBelow = config.rtpSolidBlocksBelow();
        int ceilingClearance = config.rtpCeilingClearance();
        Set<String> allowedBiomes = config.rtpAllowedBiomes();
        Set<String> deniedBiomes = config.rtpDeniedBiomes();

        Location from = player.getLocation().clone();
        Location center = resolveSearchCenter(player, searchWorld, centerOnPlayer);
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        int cooldownSeconds = bypassCooldown ? 0 : config.rtpCooldown();
        int rtpCountdown = teleportService.countdownFor(TeleportService.TeleportKind.RTP);
        UUID playerId = player.getUniqueId();

        boolean useAsyncSearch = platform.supportsAsyncChunkLoading() && !onlyLoadedChunks;

        new SearchTask(
                playerId,
                searchWorld,
                from,
                centerX,
                centerZ,
                radius,
                minDistance,
                attempts,
                attemptsPerTick,
                cooldownSeconds,
                rtpCountdown,
                onlyLoadedChunks,
                generateChunks,
                asyncUrgent,
                maxPendingLoads,
                maxCandidates,
                solidBlocksBelow,
                ceilingClearance,
                allowedBiomes,
                deniedBiomes,
                useAsyncSearch,
                handler
        ).start();
    }

    public void teleportWithCountdown(Player player, Location destination, int countdownSeconds) {
        teleportService.teleport(player, destination, TeleportService.rtpWithCountdown(countdownSeconds));
    }

    private static Location resolveSearchCenter(Player player, World searchWorld, boolean centerOnPlayer) {
        if (centerOnPlayer && player.getWorld().equals(searchWorld)) {
            return player.getLocation();
        }
        return searchWorld.getSpawnLocation();
    }

    private void finishSuccess(Player player, Location from, Location destination, int cooldownSeconds,
                               int rtpCountdown, ResultHandler handler) {
        adaptiveState.recordSuccess();
        if (cooldownSeconds > 0) {
            plugin.getRtpCooldownManager().recordUse(player.getUniqueId());
        }

        int blocksAway = (int) Math.round(destination.distance(from));
        if (rtpCountdown > 0) {
            handler.onFound(destination, blocksAway);
        } else {
            teleportService.teleport(player, destination, teleportService.rtpInstant(plugin));
            handler.onFound(destination, blocksAway);
        }
    }

    private final class SearchTask extends BukkitRunnable {

        private final UUID playerId;
        private final World world;
        private final Location from;
        private final int centerX;
        private final int centerZ;
        private final int searchRadius;
        private final int minDist;
        private final int maxAttempts;
        private final int attemptsPerTick;
        private final int cooldownSeconds;
        private final int rtpCountdown;
        private final boolean onlyLoadedChunks;
        private final boolean generateChunks;
        private final boolean asyncUrgent;
        private final int maxPendingLoads;
        private final int maxCandidates;
        private final int solidBlocksBelow;
        private final int ceilingClearance;
        private final Set<String> allowedBiomes;
        private final Set<String> deniedBiomes;
        private final boolean useAsyncSearch;
        private final ResultHandler handler;
        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        private final WorldBorder border;
        private final List<RtpCandidate> candidates = new ArrayList<>();

        private int tried;
        private int pendingLoads;
        private boolean finished;
        private BukkitTask timerTask;

        private SearchTask(UUID playerId, World world, Location from, int centerX, int centerZ,
                           int searchRadius, int minDist, int maxAttempts, int attemptsPerTick,
                           int cooldownSeconds, int rtpCountdown, boolean onlyLoadedChunks,
                           boolean generateChunks, boolean asyncUrgent, int maxPendingLoads,
                           int maxCandidates, int solidBlocksBelow, int ceilingClearance,
                           Set<String> allowedBiomes, Set<String> deniedBiomes,
                           boolean useAsyncSearch, ResultHandler handler) {
            this.playerId = playerId;
            this.world = world;
            this.from = from;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.searchRadius = searchRadius;
            this.minDist = minDist;
            this.maxAttempts = maxAttempts;
            this.attemptsPerTick = attemptsPerTick;
            this.cooldownSeconds = cooldownSeconds;
            this.rtpCountdown = rtpCountdown;
            this.onlyLoadedChunks = onlyLoadedChunks;
            this.generateChunks = generateChunks;
            this.asyncUrgent = asyncUrgent;
            this.maxPendingLoads = maxPendingLoads;
            this.maxCandidates = maxCandidates;
            this.solidBlocksBelow = solidBlocksBelow;
            this.ceilingClearance = ceilingClearance;
            this.allowedBiomes = allowedBiomes;
            this.deniedBiomes = deniedBiomes;
            this.useAsyncSearch = useAsyncSearch;
            this.handler = handler;
            this.border = world.getWorldBorder();
        }

        void start() {
            timerTask = runTaskTimer(plugin, 0L, 1L);
        }

        @Override
        public void run() {
            if (finished) {
                return;
            }

            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                stop();
                return;
            }

            int startedThisTick = 0;
            while (startedThisTick < attemptsPerTick
                    && tried < maxAttempts
                    && pendingLoads < maxPendingLoads
                    && candidates.size() < maxCandidates
                    && chunkLoads.hasImmediateCapacity(playerId)) {
                int x;
                int z;
                if (minDist > 0 && minDist < searchRadius) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double distance = minDist + random.nextDouble() * (searchRadius - minDist);
                    x = centerX + (int) Math.round(Math.cos(angle) * distance);
                    z = centerZ + (int) Math.round(Math.sin(angle) * distance);
                } else {
                    x = centerX + random.nextInt(-searchRadius, searchRadius + 1);
                    z = centerZ + random.nextInt(-searchRadius, searchRadius + 1);
                }

                tried++;
                startedThisTick++;

                if (!isInsideBorder(border, world, x, z, from.getY())) {
                    continue;
                }

                if (useAsyncSearch) {
                    tryAsyncAttempt(player, x, z);
                } else {
                    tryConservativeAttempt(player, x, z);
                }
            }

            if (tried >= maxAttempts && pendingLoads == 0) {
                finishSearch(player);
                return;
            }

            if (candidates.size() >= maxCandidates && pendingLoads == 0) {
                finishSearch(player);
            }
        }

        private void tryConservativeAttempt(Player player, int x, int z) {
            if (onlyLoadedChunks && !world.isChunkLoaded(x >> 4, z >> 4)) {
                return;
            }

            RtpSafetyEvaluator.evaluate(
                    world, x, z, from.getYaw(), from.getPitch(),
                    solidBlocksBelow, ceilingClearance, allowedBiomes, deniedBiomes
            ).ifPresent(this::offerCandidate);

            if (tried >= maxAttempts) {
                finishSearch(player);
            }
        }

        private void tryAsyncAttempt(Player player, int x, int z) {
            Location probe = new Location(world, x + 0.5, from.getY(), z + 0.5);

            Runnable onProbeFinished = () -> {
                pendingLoads--;
                maybeFinishSearch();
            };

            chunkLoads.whenChunkReady(
                    playerId,
                    probe,
                    ChunkLoadPolicy.ASYNC,
                    generateChunks,
                    asyncUrgent,
                    () -> pendingLoads++,
                    () -> {
                        if (finished) {
                            pendingLoads--;
                            return;
                        }
                        Player current = plugin.getServer().getPlayer(playerId);
                        if (current == null || !current.isOnline()) {
                            pendingLoads--;
                            stop();
                            return;
                        }
                        RtpSafetyEvaluator.evaluate(
                                world, x, z, from.getYaw(), from.getPitch(),
                                solidBlocksBelow, ceilingClearance, allowedBiomes, deniedBiomes
                        ).ifPresent(this::offerCandidate);
                        onProbeFinished.run();
                        maybeFinishSearch();
                    },
                    onProbeFinished,
                    ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY
            );
        }

        private synchronized void offerCandidate(RtpCandidate candidate) {
            if (finished) {
                return;
            }
            if (candidates.size() < maxCandidates) {
                candidates.add(candidate);
                return;
            }
            RtpCandidate worst = candidates.stream()
                    .max(Comparator.comparingInt(RtpCandidate::hazardScore))
                    .orElse(null);
            if (worst != null && candidate.hazardScore() < worst.hazardScore()) {
                candidates.remove(worst);
                candidates.add(candidate);
            }
        }

        private void maybeFinishSearch() {
            if (!finished && tried >= maxAttempts && pendingLoads == 0) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    finishSearch(player);
                } else {
                    fail();
                }
            }
        }

        private void finishSearch(Player player) {
            if (finished) {
                return;
            }
            RtpCandidate best = candidates.stream()
                    .min(Comparator.comparingInt(RtpCandidate::hazardScore))
                    .orElse(null);
            if (best != null) {
                succeed(player, best.location());
            } else {
                fail();
            }
        }

        private void succeed(Player player, Location destination) {
            if (finished) {
                return;
            }
            finished = true;
            stop();
            finishSuccess(player, from, destination, cooldownSeconds, rtpCountdown, handler);
        }

        private void fail() {
            if (finished) {
                return;
            }
            finished = true;
            stop();
            adaptiveState.recordFailure();
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                handler.onFailed();
            }
        }

        private void stop() {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
            cancel();
        }
    }

    private static int clampRadiusToBorder(World world, int radius) {
        WorldBorder border = world.getWorldBorder();
        if (border == null) {
            return radius;
        }
        double halfSize = border.getSize() / 2.0;
        if (halfSize <= 0) {
            return radius;
        }
        int borderLimit = (int) Math.max(1, Math.floor(halfSize) - 2);
        return Math.min(radius, borderLimit);
    }

    private static boolean isInsideBorder(WorldBorder border, World world, int x, int z, double y) {
        if (border == null) {
            return true;
        }
        return border.isInside(new Location(world, x + 0.5, y, z + 0.5));
    }
}
