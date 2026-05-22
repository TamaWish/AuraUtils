package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import me.aurautils.managers.BackService;
import me.aurautils.managers.TeleportHelper;
import me.aurautils.util.WarpPermissions;
import me.aurautils.menus.MenuType;
import me.aurautils.menus.UtilityMenuHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    private final AuraUtils plugin;

    public MenuListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof UtilityMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        String action = item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "action"), PersistentDataType.STRING);
        String id = item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "id"), PersistentDataType.STRING);

        if (action == null) {
            return;
        }

        switch (action) {
            case "open_warps" -> plugin.getMenuManager().openWarpsMenu(player, 0);
            case "open_homes" -> plugin.getMenuManager().openHomesMenu(player, 0);
            case "open_tpa" -> plugin.getMenuManager().openTpaMenu(player);
            case "open_back" -> {
                player.closeInventory();
                BackService.teleportBack(plugin, player);
            }
            case "open_main" -> plugin.getMenuManager().openMainMenu(player);
            
            case "refresh_menu" -> plugin.getMenuManager().openMainMenu(player);
            case "page_next" -> openNextPage(holder, player);
            case "page_prev" -> openPreviousPage(holder, player);
            case "close_menu" -> player.closeInventory();
            case "warp" -> teleportToWarp(player, id);
            case "home" -> teleportToHome(player, id);
            case "tpa_accept" -> {
                player.closeInventory();
                plugin.getTpaManager().accept(player);
            }
            case "tpa_deny" -> {
                player.closeInventory();
                plugin.getTpaManager().deny(player);
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // no-op; menus are stateless and reopen on demand
    }

    private void openNextPage(UtilityMenuHolder holder, Player player) {
        int nextPage = holder.getPage() + 1;
        if (holder.getType() == MenuType.WARPS) {
            plugin.getMenuManager().openWarpsMenu(player, nextPage);
        } else if (holder.getType() == MenuType.HOMES) {
            plugin.getMenuManager().openHomesMenu(player, nextPage);
        }
    }

    private void openPreviousPage(UtilityMenuHolder holder, Player player) {
        int previousPage = Math.max(0, holder.getPage() - 1);
        if (holder.getType() == MenuType.WARPS) {
            plugin.getMenuManager().openWarpsMenu(player, previousPage);
        } else if (holder.getType() == MenuType.HOMES) {
            plugin.getMenuManager().openHomesMenu(player, previousPage);
        }
    }

    private void teleportToWarp(Player player, String name) {
        if (name == null) {
            return;
        }
        if (!WarpPermissions.canUse(player, name)) {
            player.sendMessage(plugin.prefix("&cYou don't have permission to use warp &e" + name + "&c."));
            return;
        }
        var location = plugin.getWarpManager().getWarp(name);
        if (location == null) {
            player.sendMessage(plugin.prefix("&cWarp &e" + name + " &cwas not found."));
            return;
        }
        player.closeInventory();
        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown);
        } else {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
            player.teleport(location);
            player.sendMessage(plugin.prefix("&aTeleported to warp &e" + name + "&a."));
        }
    }

    private void teleportToHome(Player player, String name) {
        if (name == null) {
            return;
        }
        var location = plugin.getHomeManager().getHome(player.getUniqueId(), name);
        if (location == null) {
            player.sendMessage(plugin.prefix("&cHome &e" + name + " &cwas not found."));
            return;
        }
        player.closeInventory();
        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown);
        } else {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
            player.teleport(location);
            player.sendMessage(plugin.prefix("&aTeleported to home &e" + name + "&a."));
        }
    }
}