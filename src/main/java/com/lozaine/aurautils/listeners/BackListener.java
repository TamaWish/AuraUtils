package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Fallback recorder for teleports that do fire {@link PlayerTeleportEvent}
 * (chorus fruit, bed exit, dismount, etc.).
 *
 * <p>On Folia most teleports (including plugin/async) do <strong>not</strong> fire
 * this event. Primary recording is done in {@code TeleportHelper.teleportExact}
 * before the teleport runs, so {@code /back} works on Spigot, Paper, and Folia.
 */
public class BackListener implements Listener {

    private final AuraUtils plugin;

    public BackListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // Only record successful, non-cancelled teleports from causes that still
        // fire this event. Plugin teleports are recorded in TeleportHelper.
        if (event.getFrom() == null || event.getFrom().getWorld() == null) {
            return;
        }
        plugin.getBackManager().record(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBackManager().remove(event.getPlayer().getUniqueId());
    }
}
