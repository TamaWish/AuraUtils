package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {

    private final AuraUtils plugin;

    public PlayerSessionListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().applyTo(event.getPlayer());
        if (plugin.updateChecker() != null) {
            plugin.updateChecker().notifyPlayerIfNeeded(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Clean up any pending TPA requests involving this player
        if (plugin.getTpaManager() != null) {
            plugin.getTpaManager().handleQuit(event.getPlayer());
        }
        // Drop any countdown teleport so tasks don't outlive the session
        if (plugin.getTeleportHelper() != null) {
            plugin.getTeleportHelper().cancelTeleport(event.getPlayer(), false);
        }
        plugin.getPlayerDataManager().save();
    }
}
