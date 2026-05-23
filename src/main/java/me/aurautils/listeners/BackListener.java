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

    /**
     * Records {@code event.getFrom()} for {@code /back} on every {@link PlayerTeleportEvent}, including
     * teleports from other plugins (RTP, warps, etc.). AuraUtils-initiated teleports call
     * {@link BackManager#skipNextRecord} first so the destination is not stored as the back target.
     * <p>
     * Skip is single-use: if multiple teleports fire for the same player in one tick, only the first
     * event consumes the skip; a later event in that tick may record an intermediate location as back.
     */
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