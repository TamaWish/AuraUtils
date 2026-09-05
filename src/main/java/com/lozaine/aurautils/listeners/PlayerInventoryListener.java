package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.menus.PlayerInventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Persists extra inventories when the view closes or the player leaves.
 */
public class PlayerInventoryListener implements Listener {

    private final AuraUtils plugin;

    public PlayerInventoryListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PlayerInventoryHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!holder.getOwner().equals(player.getUniqueId())) {
            return;
        }
        if (plugin.getPlayerInventoryManager() == null) {
            return;
        }
        plugin.getPlayerInventoryManager().captureAndSave(holder, event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getPlayerInventoryManager() == null) {
            return;
        }
        plugin.getPlayerInventoryManager().captureIfOpen(event.getPlayer());
        plugin.getPlayerInventoryManager().save(event.getPlayer().getUniqueId());
    }
}
