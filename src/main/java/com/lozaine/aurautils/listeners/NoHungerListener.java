package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class NoHungerListener implements Listener {

    private final AuraUtils plugin;
    public NoHungerListener(AuraUtils plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!plugin.getPlayerDataManager().isNoHunger(p.getUniqueId())) return;

        // Block any hunger loss; keep the player fully fed.
        if (event.getFoodLevel() < p.getFoodLevel()) {
            event.setCancelled(true);
            plugin.getPlayerDataManager().applyNoHungerEffects(p);
        }
    }
}
