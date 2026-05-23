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
import java.util.Optional;
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

        Location from = player.getLocation().clone();
        Location center = resolveSearchCenter(player, searchWorld, config.rtpCenterOnPlayer());
        UUID playerId = player.getUniqueId();

        new SearchTask(
                playerId,
                searchWorld,
                from,
                center.getBlockX(),
                center.getBlockZ(),
                radius,
                minDistance,
                config,
                bypassCooldown ? 0 : config.rtpCooldown(),
                teleportService.countdownFor(TeleportService.TeleportKind.RTP),
                platform.supportsAsyncChunkLoading()
                        && !(config.rtpOnlyLoadedChunksExplicitlySet() && config.rtpOnlyLoadedChunks()),
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
        private final int cooldownSeconds;
        private final int rtpCountdown;
        private final boolean useAsyncSearch;
        private final ResultHandler handler;

        private final int maxAttempts;
        private final int attemptsPerTick;
        private final boolean onlyLoadedChunks;
        private final boolean generateChunks;
        private final boolean asyncUrgent;
        private final int maxPendingLoads;
        private final int maxCandidates;
        private final RtpMode rtpMode;
        private final int solidBlocksBelow;
        private final int ceilingClearance;
        private final int caveSurfaceBuffer;
        private final int caveMinY;
        private final int caveMaxY;
        private final Set<String> allowedBiomes;
        private final Set<String> deniedBiomes;
        private final boolean preloadNeighbors;
        private final int preloadRadius;

        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        private final WorldBorder border;
        private final RtpSessionCache sessionCache;
        private final RtpCoordinateSampler coordinateSampler;
        private final List<RtpCandidate> candidates = new ArrayList<>();

        private int tried;
        private int pendingLoads;
        private int lastBandIndex = -1;
        private boolean finished;
        private BukkitTask timerTask;

        private SearchTask(UUID playerId, World world, Location from, int centerX, int centerZ,
                           int searchRadius, int minDist, AuraConfig config, int cooldownSeconds,
                           int rtpCountdown, boolean useAsyncSearch, ResultHandler handler) {
            this.playerId = playerId;
            this.world = world;
            this.from = from;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.searchRadius = searchRadius;
            this.minDist = minDist;
            this.cooldownSeconds = cooldownSeconds;
            this.rtpCountdown = rtpCountdown;
            this.useAsyncSearch = useAsyncSearch;
            this.handler = handler;

            this.maxAttempts = config.rtpAttempts();
            this.attemptsPerTick = config.rtpAttemptsPerTick();
            this.onlyLoadedChunks = config.rtpOnlyLoadedChunksExplicitlySet()
                    && config.rtpOnlyLoadedChunks();
            this.generateChunks = config.rtpGenerateChunks();
            this.asyncUrgent = config.rtpAsyncUrgent();
            this.maxPendingLoads = config.rtpMaxPendingChunkLoads();
            this.maxCandidates = config.rtpMaxCandidates();
            this.rtpMode = config.rtpMode();
            this.solidBlocksBelow = config.rtpSolidBlocksBelow();
            this.ceilingClearance = config.rtpCeilingClearance();
            this.caveSurfaceBuffer = config.rtpCaveSurfaceBuffer();
            this.caveMinY = config.rtpCaveMinY();
            this.caveMaxY = config.rtpCaveMaxY();
            this.allowedBiomes = config.rtpAllowedBiomes();
            this.deniedBiomes = config.rtpDeniedBiomes();
            this.preloadNeighbors = config.rtpPreloadNeighbors();
            this.preloadRadius = config.rtpPreloadRadius();

            this.border = world.getWorldBorder();
            this.sessionCache = new RtpSessionCache(config.rtpRingBands());
            this.coordinateSampler = new RtpCoordinateSampler(
                    centerX,
                    centerZ,
                    minDist,
                    searchRadius,
                    config.rtpRingBands(),
                    config.rtpStratifiedRings(),
                    config.rtpChunkCentric(),
                    config.rtpGridJitter(),
                    config.rtpGridCellSize(),
                    config.rtpChunkRetryLimit(),
                    sessionCache,
                    random
            );
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
                RtpCoordinateSampler.Sample sample = coordinateSampler.next();
                lastBandIndex = sample.bandIndex();
                int x = sample.x();
                int z = sample.z();

                tried++;
                startedThisTick++;

                if (!isInsideBorder(border, world, x, z, from.getY())) {
                    recordFailedProbe();
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
                recordFailedProbe();
                return;
            }

            evaluateAt(x, z).ifPresentOrElse(this::offerCandidate, this::recordFailedProbe);

            if (tried >= maxAttempts && pendingLoads == 0) {
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
                        evaluateAt(x, z).ifPresentOrElse(this::offerCandidate, this::recordFailedProbe);
                        onProbeFinished.run();
                        maybeFinishSearch();
                    },
                    onProbeFinished,
                    ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY
            );
        }

        private Optional<RtpCandidate> evaluateAt(int x, int z) {
            return RtpSafetyEvaluator.evaluate(
                    world, x, z, from.getYaw(), from.getPitch(),
                    rtpMode, solidBlocksBelow, ceilingClearance,
                    caveSurfaceBuffer, caveMinY, caveMaxY,
                    allowedBiomes, deniedBiomes
            );
        }

        private void recordFailedProbe() {
            if (lastBandIndex >= 0) {
                sessionCache.recordBandFailure(lastBandIndex);
            }
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

            Runnable complete = () -> finishSuccess(player, from, destination, cooldownSeconds, rtpCountdown, handler);
            if (preloadNeighbors && preloadRadius >= 0) {
                chunkLoads.preloadNeighbors(
                        playerId,
                        destination,
                        preloadRadius,
                        generateChunks,
                        asyncUrgent,
                        complete
                );
            } else {
                complete.run();
            }
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
