package me.aurautils.commands;

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
import org.bukkit.scheduler.BukkitRunnable;
import me.aurautils.managers.TeleportHelper;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
        int attempts = Math.max(1, plugin.getConfig().getInt("rtp.attempts", 30));
        int attemptsPerTick = Math.max(1, plugin.getConfig().getInt("rtp.attemptsPerTick", 5));

        player.sendMessage(plugin.prefix("&eSearching for a safe location..."));

        int rtpCountdown = Math.max(0, plugin.getConfig().getInt("rtp.countdown", plugin.getConfig().getInt("teleport.countdown", 5)));

        TeleportHelper teleportHelper = new TeleportHelper(plugin);

        new BukkitRunnable() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            final WorldBorder border = world.getWorldBorder();
            int tried = 0;

            @Override
            public void run() {
                for (int k = 0; k < attemptsPerTick && tried < attempts; k++, tried++) {
                    int x = world.getSpawnLocation().getBlockX() + random.nextInt(-radius, radius + 1);
                    int z = world.getSpawnLocation().getBlockZ() + random.nextInt(-radius, radius + 1);

                    Location borderCheck = new Location(world, x + 0.5, world.getSpawnLocation().getY(), z + 0.5);
                    if (border != null && !border.isInside(borderCheck)) {
                        continue;
                    }

                    Block surface = world.getHighestBlockAt(x, z);
                    Location teleportLocation = buildSafeLocation(surface);
                    if (teleportLocation != null) {
                        if (player.isOnline()) {
                            if (rtpCountdown > 0) {
                                teleportHelper.scheduleTeleport(player, teleportLocation, rtpCountdown);
                            } else {
                                player.teleport(teleportLocation);
                                player.sendMessage(plugin.prefix("&aTeleported to a random safe location."));
                            }
                        }
                        this.cancel();
                        return;
                    }
                }

                if (tried >= attempts) {
                    if (player.isOnline()) {
                        player.sendMessage(plugin.prefix("&cCould not find a safe teleport location. Try again."));
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Location findSafeLocation(World world, Location center, int radius, int attempts) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        WorldBorder border = world.getWorldBorder();

        for (int i = 0; i < attempts; i++) {
            int x = center.getBlockX() + random.nextInt(-radius, radius + 1);
            int z = center.getBlockZ() + random.nextInt(-radius, radius + 1);

            Location borderCheck = new Location(world, x + 0.5, world.getSpawnLocation().getY(), z + 0.5);
            if (border != null && !border.isInside(borderCheck)) {
                continue;
            }

            Block surface = world.getHighestBlockAt(x, z);
            Location teleportLocation = buildSafeLocation(surface);
            if (teleportLocation != null) {
                return teleportLocation;
            }
        }

        return null;
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