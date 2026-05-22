package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldSettingsListener implements Listener {

    private final AuraUtils plugin;

    public WorldSettingsListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /** Re-apply spawn and keep-inventory when a world loads (e.g. after RWR recreates a resource world). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getServerSettingsManager().applyToWorld(event.getWorld());
    }
}
