package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageMultiplierListener implements Listener {

    private final AuraUtils plugin;
    public DamageMultiplierListener(AuraUtils plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        // Only apply to direct player weapon hits
        if (!(event.getDamager() instanceof Player attacker)) return;

        double mult = plugin.getPlayerDataManager().getDamageMultiplier(attacker.getUniqueId());
        if (mult == 1.0) return; // no change needed

        event.setDamage(event.getDamage() * mult);
    }
}
