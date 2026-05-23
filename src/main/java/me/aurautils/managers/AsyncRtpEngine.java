package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.platform.ChunkLoadCoordinator;
import me.aurautils.platform.ChunkLoadPolicy;
import me.aurautils.platform.ChunkLoadService;
import me.aurautils.platform.PlatformAdapter;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Async-friendly RTP: Spigot uses conservative search on already-loaded chunks only;
 * Paper queues chunk loads off the hot path and validates safety on the main thread.
 */
public class AsyncRtpEngine {

    private static final Set<Material> UNSAFE_FLOOR = Set.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.BEDROCK
    );

    private static final int SURFACE_SCAN_DEPTH = 24;

    public interface ResultHandler {
        void onFound(Location destination, int blocksAway);

        void onFailed();
    }

    private final AuraUtils plugin;
    private final PlatformAdapter platform;
    private final ChunkLoadService chunkLoads;
    private final TeleportService teleportService;

    public AsyncRtpEngine(AuraUtils plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.platform = plugin.getPlatform();
        this.chunkLoads = plugin.getChunkLoadService();
        this.teleportService = teleportService;
    }

    public void search(Player player, boolean bypassCooldown, ResultHandler handler) {
        World world = player.getWorld();
        AuraConfig config = plugin.getAuraConfig();
        int radius = clampRadiusToBorder(world, config.rtpRadius());
        int minDistance = config.rtpMinDistance();
        int attempts = config.rtpAttempts();
        int attemptsPerTick = config.rtpAttemptsPerTick();
        boolean centerOnPlayer = config.rtpCenterOnPlayer();
        boolean onlyLoadedChunks = config.rtpOnlyLoadedChunksExplicitlySet()
                ? config.rtpOnlyLoadedChunks()
                : !platform.supportsAsyncChunkLoading();
        boolean generateChunks = config.rtpGenerateChunks();
        boolean asyncUrgent = config.rtpAsyncUrgent();
        int maxPendingLoads = config.rtpMaxPendingChunkLoads();

        Location from = player.getLocation().clone();
        Location center = centerOnPlayer ? from : world.getSpawnLocation();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        int cooldownSeconds = bypassCooldown ? 0 : config.rtpCooldown();
        int rtpCountdown = teleportService.countdownFor(TeleportService.TeleportKind.RTP);
        UUID playerId = player.getUniqueId();

        boolean useAsyncSearch = platform.supportsAsyncChunkLoading() && !onlyLoadedChunks;

        new SearchTask(
                playerId,
                world,
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
                useAsyncSearch,
                handler
        ).start();
    }

    public void teleportWithCountdown(Player player, Location destination, int countdownSeconds) {
        teleportService.teleport(player, destination, TeleportService.rtpWithCountdown(countdownSeconds));
    }

    private void finishSuccess(Player player, Location from, Location destination, int cooldownSeconds,
                               int rtpCountdown, ResultHandler handler) {
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

    private Location findSafeLocation(World world, int x, int z, float yaw, float pitch) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }

        int topY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int minY = world.getMinHeight();

        for (int y = topY; y >= Math.max(minY, topY - SURFACE_SCAN_DEPTH); y--) {
            Block floor = world.getBlockAt(x, y, z);
            if (!isStandableFloor(floor)) {
                continue;
            }

            Block feet = floor.getRelative(BlockFace.UP);
            Block head = feet.getRelative(BlockFace.UP);
            if (!hasStandingSpace(feet, head)) {
                continue;
            }

            return new Location(
                    world,
                    x + 0.5,
                    floor.getY() + 1.0,
                    z + 0.5,
                    yaw,
                    pitch
            );
        }
        return null;
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
        private final boolean useAsyncSearch;
        private final ResultHandler handler;
        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        private final WorldBorder border;

        private int tried;
        private int pendingLoads;
        private boolean finished;
        private BukkitTask timerTask;

        private SearchTask(UUID playerId, World world, Location from, int centerX, int centerZ,
                           int searchRadius, int minDist, int maxAttempts, int attemptsPerTick,
                           int cooldownSeconds, int rtpCountdown, boolean onlyLoadedChunks,
                           boolean generateChunks, boolean asyncUrgent, int maxPendingLoads,
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
                fail();
            }
        }

        private void tryConservativeAttempt(Player player, int x, int z) {
            if (onlyLoadedChunks && !world.isChunkLoaded(x >> 4, z >> 4)) {
                return;
            }

            Location candidate = findSafeLocation(world, x, z, from.getYaw(), from.getPitch());
            if (candidate != null) {
                succeed(player, candidate);
            }
        }

        private void tryAsyncAttempt(Player player, int x, int z) {
            Location probe = new Location(world, x + 0.5, from.getY(), z + 0.5);

            Runnable onProbeFinished = () -> {
                pendingLoads--;
                maybeFinishFailed();
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
                        Location candidate = findSafeLocation(world, x, z, from.getYaw(), from.getPitch());
                        if (candidate != null) {
                            pendingLoads--;
                            succeed(current, candidate);
                        } else {
                            onProbeFinished.run();
                        }
                    },
                    onProbeFinished,
                    ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY
            );
        }

        private void succeed(Player player, Location destination) {
            if (finished) {
                return;
            }
            finished = true;
            stop();
            finishSuccess(player, from, destination, cooldownSeconds, rtpCountdown, handler);
        }

        private void maybeFinishFailed() {
            if (!finished && tried >= maxAttempts && pendingLoads == 0) {
                fail();
            }
        }

        private void fail() {
            if (finished) {
                return;
            }
            finished = true;
            stop();
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

    private static boolean isStandableFloor(Block block) {
        if (block.isEmpty() || block.isLiquid()) {
            return false;
        }
        Material type = block.getType();
        if (UNSAFE_FLOOR.contains(type)) {
            return false;
        }
        return !block.isPassable();
    }

    private static boolean hasStandingSpace(Block feet, Block head) {
        return feet.isEmpty() && head.isEmpty();
    }
}
