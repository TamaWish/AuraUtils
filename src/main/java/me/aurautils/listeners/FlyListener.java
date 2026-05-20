package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class FlyListener implements Listener {

    private final AuraUtils plugin;
    public FlyListener(AuraUtils plugin) { this.plugin = plugin; }

    /** Restore fly after changing worlds (flight gets reset by Bukkit). */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (plugin.getPlayerDataManager().isFly(p.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                p.setAllowFlight(true);
                p.setFlying(true);
            }, 1L);
        }
    }

    /** Restore fly after respawn. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        if (plugin.getPlayerDataManager().isFly(p.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                p.setAllowFlight(true);
            }, 1L);
        }
    }
}
