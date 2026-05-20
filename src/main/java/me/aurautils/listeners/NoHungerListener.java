package me.aurautils.listeners;

import me.aurautils.AuraUtils;
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

        // Only cancel hunger LOSS (allow eating to fill if below 20)
        if (event.getFoodLevel() < p.getFoodLevel()) {
            event.setCancelled(true);
        }
    }
}
