package me.aurautils.commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.aurautils.AuraUtils;
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

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    public RtpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /rtp.");
            return true;
        }
        if (!player.hasPermission("aura.rtp")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        World world = player.getWorld();
        int radius = Math.max(1, plugin.getConfig().getInt("rtp.radius", 2000));
        int minDistance = Math.max(0, plugin.getConfig().getInt("rtp.minDistance", 250));
        if (minDistance > radius) {
            minDistance = radius;
        }
        int attempts = Math.max(1, plugin.getConfig().getInt("rtp.attempts", 30));
        int attemptsPerTick = Math.max(1, plugin.getConfig().getInt("rtp.attemptsPerTick", 5));
        Location from = player.getLocation().clone();
        long minDistanceSquared = (long) minDistance * minDistance;

        player.sendMessage(plugin.prefix("&eSearching for a safe location..."));

        int rtpCountdown = Math.max(0, plugin.getConfig().getInt("rtp.countdown", plugin.getConfig().getInt("teleport.countdown", 5)));

        var teleportHelper = plugin.getTeleportHelper();
        var scheduler = plugin.getScheduler();

        final AtomicInteger tried = new AtomicInteger(0);
        final AtomicBoolean found = new AtomicBoolean(false);
        final AtomicBoolean finished = new AtomicBoolean(false);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final WorldBorder border = world.getWorldBorder();
        final int spawnX = world.getSpawnLocation().getBlockX();
        final int spawnZ = world.getSpawnLocation().getBlockZ();
        final double spawnY = world.getSpawnLocation().getY();

        // Generator runs on the player entity thread (pacing only). Each candidate is then
        // evaluated on the region that owns that (x,z) via runAtLocation so getHighestBlockAt
        // is legal on Folia even when the target chunk is far from the player.
        final WrappedTask[] taskHolder = new WrappedTask[1];
        taskHolder[0] = scheduler.runAtEntityTimer(player, () -> {
            WrappedTask self = taskHolder[0];
            if (!plugin.isEnabled() || found.get() || finished.get()) {
                if (self != null) self.cancel();
                return;
            }

            for (int k = 0; k < attemptsPerTick; k++) {
                if (found.get() || finished.get()) {
                    break;
                }
                int current = tried.incrementAndGet();
                if (current > attempts) {
                    break;
                }

                int x = spawnX + random.nextInt(-radius, radius + 1);
                int z = spawnZ + random.nextInt(-radius, radius + 1);

                long dx = x - from.getBlockX();
                long dz = z - from.getBlockZ();
                if (minDistanceSquared > 0 && (dx * dx + dz * dz) < minDistanceSquared) {
                    continue;
                }

                Location borderCheck = new Location(world, x + 0.5, spawnY, z + 0.5);
                if (border != null && !border.isInside(borderCheck)) {
                    continue;
                }

                // Schedule height + safety check on the region that owns this chunk.
                // On Spigot/Paper this is simply the main thread; on Folia it is the
                // correct region thread so getHighestBlockAt is allowed.
                final int fx = x;
                final int fz = z;
                Location regionLoc = new Location(world, fx + 0.5, spawnY, fz + 0.5);
                scheduler.runAtLocation(regionLoc, () -> {
                    if (found.get() || finished.get() || !plugin.isEnabled()) {
                        return;
                    }

                    Block surface;
                    try {
                        surface = world.getHighestBlockAt(fx, fz);
                    } catch (IllegalStateException ex) {
                        // Extremely rare: region ownership race. Skip this candidate.
                        return;
                    }

                    Location teleportLocation = buildSafeLocation(surface);
                    if (teleportLocation == null) {
                        return;
                    }

                    if (!found.compareAndSet(false, true)) {
                        return; // another candidate already won
                    }

                    finished.set(true);
                    if (self != null) {
                        self.cancel();
                    }

                    // Teleport / countdown must run on the player's entity scheduler
                    scheduler.runAtEntity(player, () -> {
                        if (!player.isOnline()) {
                            return;
                        }
                        if (rtpCountdown > 0) {
                            teleportHelper.scheduleTeleport(player, teleportLocation, rtpCountdown, "a random location");
                        } else {
                            if (teleportHelper.teleportExact(player, teleportLocation)) {
                                player.sendMessage(plugin.prefix("&aTeleported to &ba random location&a."));
                            }
                        }
                    });
                });
            }

            // After the last batch has been dispatched, wait a short grace period for
            // in-flight region checks, then report failure if nothing succeeded.
            if (tried.get() >= attempts && !found.get() && !finished.get()) {
                scheduler.runAtEntityLater(player, () -> {
                    if (found.get() || finished.get()) {
                        return;
                    }
                    finished.set(true);
                    if (self != null) {
                        self.cancel();
                    }
                    if (player.isOnline()) {
                        player.sendMessage(plugin.prefix("&cCould not find a safe teleport location. Try again."));
                    }
                }, 40L); // 2 seconds for pending region tasks to finish
            }
        }, 0L, 1L);

        return true;
    }

    private Location buildSafeLocation(Block surface) {
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

        return feet.getLocation().add(0.5, 0.0, 0.5);
    }
}
