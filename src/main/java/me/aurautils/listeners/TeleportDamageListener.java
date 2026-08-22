package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Optionally cancels a pending teleport countdown when the player takes damage.
 * Controlled by config: teleport.cancel-on-damage (default false).
 */
public class TeleportDamageListener implements Listener {

    private final AuraUtils plugin;

    public TeleportDamageListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.getTeleportHelper() == null) {
            return;
        }
        plugin.getTeleportHelper().handleDamage(player);
    }
}
