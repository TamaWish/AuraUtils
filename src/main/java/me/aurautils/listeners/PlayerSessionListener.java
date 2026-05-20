package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {

    private final AuraUtils plugin;
    public PlayerSessionListener(AuraUtils plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().applyTo(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();

        // Make sure fly is revoked so they don't reconnect flying if data is wiped
        if (plugin.getPlayerDataManager().isFly(p.getUniqueId())) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }
}
