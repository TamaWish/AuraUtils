package com.lozaine.aurautils.commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.economy.EconomyAction;
import com.lozaine.aurautils.util.Platform;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Random teleport that never floods the tick thread with chunk generation.
 * Paper/Folia: getChunkAtAsync. Spigot: loaded chunks first, capped sync gens.
 */
public class RtpCommand implements CommandExecutor {

    private static final Set<Material> UNSAFE_SURFACES = Set.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE
    );

    private final AuraUtils plugin;
    private final Map<UUID, WrappedTask> activeSearches = new ConcurrentHashMap<>();

    public RtpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/rtp");
            return true;
        }
        if (!player.hasPermission("aura.rtp")) {
            msg.send(player, "common.no-permission");
            return true;
        }
        if (!plugin.economy().ensureCanPay(player, EconomyAction.RTP)) {
            return true;
        }

        cancelSearch(player.getUniqueId());

        World world = player.getWorld();
        int radius = Math.max(1, plugin.getConfig().getInt("rtp.radius", 2000));
        int minDistance = Math.max(0, plugin.getConfig().getInt("rtp.minDistance", 250));
        if (minDistance > radius) {
            minDistance = radius;
        }
        int attempts = Math.max(1, plugin.getConfig().getInt("rtp.attempts", 30));
        int maxSearchTicks = Math.max(20, plugin.getConfig().getInt("rtp.max-search-ticks", 200));
        int maxSyncGens = Math.max(0, plugin.getConfig().getInt("rtp.max-sync-generations", 3));
        boolean allowSyncGen = plugin.getConfig().getBoolean("rtp.generate-unloaded", true);

        Location from = player.getLocation().clone();
        long minDistanceSquared = (long) minDistance * minDistance;
        int rtpCountdown = Math.max(0, plugin.getConfig().getInt("rtp.countdown",
                plugin.getConfig().getInt("teleport.countdown", 5)));

        player.sendMessage(plugin.prefix(plugin.messages().get("rtp.searching")));

        var helper = plugin.getTeleportHelper();
        var scheduler = plugin.getScheduler();
        Platform platform = scheduler.platform();

        AtomicInteger tried = new AtomicInteger(0);
        AtomicInteger ticks = new AtomicInteger(0);
        AtomicInteger syncGens = new AtomicInteger(0);
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicBoolean inFlight = new AtomicBoolean(false);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        WorldBorder border = world.getWorldBorder();
        int spawnX = world.getSpawnLocation().getBlockX();
        int spawnZ = world.getSpawnLocation().getBlockZ();
        float yaw = from.getYaw();
        float pitch = from.getPitch();

        final WrappedTask[] taskHolder = new WrappedTask[1];
        taskHolder[0] = scheduler.runAtEntityTimer(player, () -> {
            WrappedTask self = taskHolder[0];
            if (!plugin.isEnabled() || found.get() || finished.get() || !player.isOnline()) {
                stop(player.getUniqueId(), self);
                return;
            }

            int tick = ticks.incrementAndGet();
            if (tick > maxSearchTicks) {
                fail(player, found, finished, self, plugin.messages().get("rtp.timeout"));
                return;
            }

            if (inFlight.get()) {
                return;
            }

            if (tried.get() >= attempts) {
                fail(player, found, finished, self, plugin.messages().get("rtp.no-safe-spot"));
                return;
            }

            int x = spawnX + random.nextInt(-radius, radius + 1);
            int z = spawnZ + random.nextInt(-radius, radius + 1);
            tried.incrementAndGet();

            long dx = (long) x - from.getBlockX();
            long dz = (long) z - from.getBlockZ();
            if (minDistanceSquared > 0 && (dx * dx + dz * dz) < minDistanceSquared) {
                return;
            }

            Location probe = new Location(world, x + 0.5, world.getSpawnLocation().getY(), z + 0.5);
            if (border != null && !border.isInside(probe)) {
                return;
            }

            int cx = Platform.chunkCoord(x);
            int cz = Platform.chunkCoord(z);
            boolean loaded = world.isChunkLoaded(cx, cz);

            if (loaded) {
                evaluateLoaded(world, x, z, yaw, pitch, player, rtpCountdown, helper, scheduler,
                        found, finished, self);
                return;
            }

            var async = platform.loadChunkAsync(world, cx, cz);
            if (async != null) {
                inFlight.set(true);
                async.whenComplete((chunk, error) -> scheduler.runAtLocation(probe, () -> {
                    inFlight.set(false);
                    if (error != null || chunk == null || found.get() || finished.get()) {
                        return;
                    }
                    evaluateSurface(world, x, z, yaw, pitch, player, rtpCountdown, helper, scheduler,
                            found, finished, self);
                }));
                return;
            }

            // Spigot: generating here is what tripped the watchdog. Cap it hard.
            if (!allowSyncGen || syncGens.get() >= maxSyncGens) {
                return;
            }
            syncGens.incrementAndGet();
            inFlight.set(true);
            scheduler.runAtLocation(probe, () -> {
                try {
                    evaluateSurface(world, x, z, yaw, pitch, player, rtpCountdown, helper, scheduler,
                            found, finished, self);
                } finally {
                    inFlight.set(false);
                }
            });
        }, 0L, 1L);

        activeSearches.put(player.getUniqueId(), taskHolder[0]);
        return true;
    }

    private void evaluateLoaded(World world, int x, int z, float yaw, float pitch, Player player,
                                int countdown, com.lozaine.aurautils.managers.TeleportHelper helper,
                                com.lozaine.aurautils.util.SchedulerHelper scheduler,
                                AtomicBoolean found, AtomicBoolean finished, WrappedTask self) {
        Location regionLoc = new Location(world, x + 0.5, world.getSpawnLocation().getY(), z + 0.5);
        scheduler.runAtLocation(regionLoc, () ->
                evaluateSurface(world, x, z, yaw, pitch, player, countdown, helper, scheduler,
                        found, finished, self));
    }

    private void evaluateSurface(World world, int x, int z, float yaw, float pitch, Player player,
                                 int countdown, com.lozaine.aurautils.managers.TeleportHelper helper,
                                 com.lozaine.aurautils.util.SchedulerHelper scheduler,
                                 AtomicBoolean found, AtomicBoolean finished, WrappedTask self) {
        if (found.get() || finished.get() || !plugin.isEnabled()) {
            return;
        }

        Block surface;
        try {
            surface = world.getHighestBlockAt(x, z);
        } catch (IllegalStateException | ArrayIndexOutOfBoundsException ex) {
            return;
        }

        Location teleportLocation = buildSafeLocation(surface, yaw, pitch);
        if (teleportLocation == null) {
            return;
        }
        if (!found.compareAndSet(false, true)) {
            return;
        }
        finished.set(true);
        stop(player.getUniqueId(), self);

        scheduler.runAtEntity(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (countdown > 0) {
                helper.scheduleTeleport(player, teleportLocation, countdown, plugin.messages().get("rtp.label"),
                        EconomyAction.RTP);
            } else {
                helper.teleportExact(player, teleportLocation, EconomyAction.RTP, ok -> {
                    if (player.isOnline() && Boolean.TRUE.equals(ok)) {
                        plugin.messages().send(player, "teleport.success",
                                "label", plugin.messages().get("rtp.label"));
                    } else if (player.isOnline()) {
                        plugin.messages().send(player, "teleport.failed",
                                "label", plugin.messages().get("rtp.label"));
                    }
                });
            }
        });
    }

    private void fail(Player player, AtomicBoolean found, AtomicBoolean finished,
                      WrappedTask self, String message) {
        if (!found.compareAndSet(false, true)) {
            return;
        }
        finished.set(true);
        stop(player.getUniqueId(), self);
        if (player.isOnline()) {
            player.sendMessage(plugin.prefix(message));
        }
    }

    private void stop(UUID id, WrappedTask self) {
        activeSearches.remove(id);
        if (self != null) {
            try {
                self.cancel();
            } catch (Exception ignored) {
                // already cancelled
            }
        }
    }

    private void cancelSearch(UUID id) {
        WrappedTask existing = activeSearches.remove(id);
        if (existing != null) {
            try {
                existing.cancel();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private Location buildSafeLocation(Block surface, float yaw, float pitch) {
        if (surface == null) {
            return null;
        }

        Material surfaceType = surface.getType();
        if (surfaceType == Material.BEDROCK || UNSAFE_SURFACES.contains(surfaceType)) {
            return null;
        }
        if (surfaceType.isAir() || surface.isLiquid() || !surfaceType.isSolid()) {
            return null;
        }

        Block feet = surface.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        if (!feet.isPassable() || !head.isPassable()) {
            return null;
        }

        Location loc = feet.getLocation().add(0.5, 0.0, 0.5);
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        return loc;
    }
}
