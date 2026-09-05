package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Restores allow-flight after events that clear player abilities.
 * Spigot 26.2 resets abilities on dimension change / login more aggressively
 * than Paper; multi-tick reapply is handled by PlayerDataManager.scheduleFlyReapply.
 */
public class FlyListener implements Listener {

    private final AuraUtils plugin;

    public FlyListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.getPlayerDataManager().scheduleFlyReapply(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.getPlayerDataManager().scheduleFlyReapply(event.getPlayer());
    }

    /**
     * Some cross-world teleports on Spigot clear abilities around teleport time
     * without a reliable ChangedWorld ordering — reapply when worlds differ.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (event.getTo() == null || !event.getFrom().isWorldLoaded() || !event.getTo().isWorldLoaded()) {
            return;
        }
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        plugin.getPlayerDataManager().scheduleFlyReapply(event.getPlayer());
    }

    /** Keep stored fly state in sync when leaving/entering Creative or Spectator. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        // Delay one tick so the new game mode is applied first
        plugin.getScheduler().runAtEntityLater(player, () -> {
            if (plugin.isEnabled() && player.isOnline()) {
                plugin.getPlayerDataManager().reapplyFly(player);
            }
        }, 1L);
    }
}
