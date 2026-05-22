package me.aurautils.listeners.paper;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TeleportHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Paper-only listener: uses {@code PlayerMoveEvent.hasChangedPosition()} via the platform adapter
 * to cancel pending teleports without polling location every tick.
 */
public class PaperMoveListener implements Listener {

    private final AuraUtils plugin;
    private final TeleportHelper teleportHelper;

    public PaperMoveListener(AuraUtils plugin, TeleportHelper teleportHelper) {
        this.plugin = plugin;
        this.teleportHelper = teleportHelper;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getPlatform().usesEventBasedMovementDetection()) {
            return;
        }
        Player player = event.getPlayer();
        if (!teleportHelper.hasActiveSession(player.getUniqueId())) {
            return;
        }
        teleportHelper.onPlayerMoved(player, event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        teleportHelper.clearSession(event.getPlayer().getUniqueId());
    }
}
