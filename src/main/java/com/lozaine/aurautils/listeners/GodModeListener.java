package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class GodModeListener implements Listener {

    private final AuraUtils plugin;
    public GodModeListener(AuraUtils plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (plugin.getPlayerDataManager().isGod(p.getUniqueId())) {
            event.setCancelled(true);
            p.setFireTicks(0);
        }
    }

    /** Prevent fire / lava combust while god is active. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (plugin.getPlayerDataManager().isGod(p.getUniqueId())) {
            event.setCancelled(true);
            p.setFireTicks(0);
        }
    }
}
