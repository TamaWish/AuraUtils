package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TeleportHelper;
import org.bukkit.HeightMap;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RtpCommand implements CommandExecutor {

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

        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt("rtp.cooldown", 0));
        long remaining = plugin.getRtpCooldownManager().remainingSeconds(player.getUniqueId(), cooldownSeconds);
        if (remaining > 0) {
            player.sendMessage(plugin.prefix("&cRTP is on cooldown. Try again in &e" + remaining + "s&c."));
            return true;
        }

        World world = player.getWorld();
        int radius = Math.max(1, plugin.getConfig().getInt("rtp.radius", 2000));
        radius = clampRadiusToBorder(world, radius);
        int minDistance = Math.max(0, plugin.getConfig().getInt("rtp.minDistance", 100));
        if (minDistance > radius) {
            minDistance = radius;
        }
        final int searchRadius = radius;
        final int minDist = minDistance;
        int attempts = Math.max(1, plugin.getConfig().getInt("rtp.attempts", 80));
        int attemptsPerTick = Math.max(1, plugin.getConfig().getInt("rtp.attemptsPerTick", 10));
        boolean centerOnPlayer = plugin.getConfig().getBoolean("rtp.center-on-player", true);

        Location from = player.getLocation().clone();
        Location center = centerOnPlayer ? from : world.getSpawnLocation();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        player.sendMessage(plugin.prefix("&eSearching for a safe location..."));

        int rtpCountdown = Math.max(0, plugin.getConfig().getInt("rtp.countdown", 0));

        TeleportHelper teleportHelper = new TeleportHelper(plugin);

        new BukkitRunnable() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            final WorldBorder border = world.getWorldBorder();
            int tried = 0;

            @Override
            public void run() {
                for (int k = 0; k < attemptsPerTick && tried < attempts; k++, tried++) {
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

                    if (!isInsideBorder(border, world, x, z, from.getY())) {
                        continue;
                    }

                    Location teleportLocation = findSafeLocation(world, x, z, from.getYaw(), from.getPitch());
                    if (teleportLocation == null) {
                        continue;
                    }

                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }

                    if (cooldownSeconds > 0) {
                        plugin.getRtpCooldownManager().recordUse(player.getUniqueId());
                    }

                    int blocksAway = (int) Math.round(teleportLocation.distance(from));
                    if (rtpCountdown > 0) {
                        player.sendMessage(plugin.prefix(
                                "&aFound a spot &e" + blocksAway + " &ablocks away. Stand still!"));
                        teleportHelper.scheduleTeleport(
                                player,
                                teleportLocation,
                                rtpCountdown,
                                true,
                                "&aTeleported to a random safe location.");
                    } else {
                        executeTeleport(player, teleportLocation);
                        player.sendMessage(plugin.prefix(
                                "&aTeleported &e" + blocksAway + " &ablocks to a random safe location."));
                    }
                    cancel();
                    return;
                }

                if (tried >= attempts) {
                    if (player.isOnline()) {
                        player.sendMessage(plugin.prefix(
                                "&cCould not find a safe location. "
                                        + "Lower &ertp.minDistance&c or increase &ertp.attempts&c."));
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void executeTeleport(Player player, Location destination) {
        TeleportHelper.ensureChunkLoaded(destination);
        plugin.getBackManager().skipNextRecord(player.getUniqueId());
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.COMMAND);
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

    /**
     * Finds a standable spot at x/z by scanning down from the surface heightmap.
     */
    private static Location findSafeLocation(World world, int x, int z, float yaw, float pitch) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.getChunkAt(chunkX, chunkZ).load(true);
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

    /** Floor must be a solid block players stand on (not air, fluid, or pass-through plants). */
    private static boolean isStandableFloor(Block block) {
        if (block.isEmpty() || block.isLiquid()) {
            return false;
        }
        Material type = block.getType();
        if (UNSAFE_FLOOR.contains(type)) {
            return false;
        }
        // Passable blocks (flowers, tall grass, etc.) are not valid floors.
        return !block.isPassable();
    }

    private static boolean hasStandingSpace(Block feet, Block head) {
        return feet.isEmpty() && head.isEmpty();
    }
}
