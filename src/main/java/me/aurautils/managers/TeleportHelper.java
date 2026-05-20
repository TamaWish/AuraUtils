package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.Location;
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
                    player.teleport(destination);
                    player.sendMessage(plugin.prefix("&aTeleported."));
                    cancel();
                    return;
                }

                // cancel if player moved
                if (player.getLocation().getWorld() != null && start.getWorld() != null
                        && !player.getLocation().getWorld().equals(start.getWorld())) {
                    player.sendMessage(plugin.prefix("&cTeleport cancelled."));
                    cancel();
                    return;
                }

                if (player.getLocation().distanceSquared(start) > 0.01) {
                    player.sendMessage(plugin.prefix("&cTeleport cancelled due to movement."));
                    cancel();
                    return;
                }

                player.sendMessage(plugin.prefix("&eTeleporting in &6" + remaining + "&e... Do not move."));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
