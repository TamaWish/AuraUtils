package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportHelper {

    private final AuraUtils plugin;

    public TeleportHelper(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Schedule a teleport with a countdown in seconds. Cancels if player moves.
     */
    public void scheduleTeleport(Player player, Location destination, int seconds) {
        scheduleTeleport(player, destination, seconds, false, "&aTeleported.");
    }

    /**
     * @param horizontalMovementOnly when true, only X/Z movement cancels (useful for RTP countdown)
     * @param successMessage         message sent after a successful teleport
     */
    public void scheduleTeleport(Player player, Location destination, int seconds,
                                 boolean horizontalMovementOnly, String successMessage) {
        final Location dest = destination.clone();
        ensureChunkLoaded(dest);

        new BukkitRunnable() {
            int remaining = Math.max(0, seconds);
            final Location start = player.getLocation().clone();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    ensureChunkLoaded(dest);
                    plugin.getBackManager().skipNextRecord(player.getUniqueId());
                    player.teleport(dest);
                    player.sendMessage(plugin.prefix(successMessage));
                    cancel();
                    return;
                }

                if (player.getLocation().getWorld() != null && start.getWorld() != null
                        && !player.getLocation().getWorld().equals(start.getWorld())) {
                    player.sendMessage(plugin.prefix("&cTeleport cancelled."));
                    cancel();
                    return;
                }

                if (hasMoved(start, player.getLocation(), horizontalMovementOnly)) {
                    player.sendMessage(plugin.prefix("&cTeleport cancelled due to movement."));
                    cancel();
                    return;
                }

                player.sendMessage(plugin.prefix("&eTeleporting in &6" + remaining + "&e... Do not move."));
                remaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private static boolean hasMoved(Location from, Location to, boolean horizontalOnly) {
        if (horizontalOnly) {
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            return (dx * dx + dz * dz) > 0.01;
        }
        return to.distanceSquared(from) > 0.01;
    }

    public static void ensureChunkLoaded(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            if (!chunk.isLoaded()) {
                chunk.load();
            }
        }
    }
}
