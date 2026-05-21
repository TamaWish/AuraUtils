package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class BackListener implements Listener {

    private final AuraUtils plugin;

    public BackListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (plugin.getBackManager().consumeSkip(playerId)) {
            return;
        }
        plugin.getBackManager().record(playerId, event.getFrom());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBackManager().remove(event.getPlayer().getUniqueId());
    }
}