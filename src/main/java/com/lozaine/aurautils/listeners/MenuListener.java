package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.economy.EconomyAction;
import com.lozaine.aurautils.menus.MenuType;
import com.lozaine.aurautils.menus.UtilityMenuHolder;
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

        // Cancel all clicks while our GUI is open (prevent item theft / shift-click)
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Only react to clicks in the top (GUI) inventory
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
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
            case "open_warps" -> {
                if (!player.hasPermission("aura.warp")) {
                    player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                    return;
                }
                plugin.getMenuManager().openWarpsMenu(player, 0);
            }
            case "open_homes" -> {
                if (!player.hasPermission("aura.home")) {
                    player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                    return;
                }
                plugin.getMenuManager().openHomesMenu(player, 0);
            }
            case "open_tpa" -> {
                if (!player.hasPermission("aura.tpa")) {
                    player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                    return;
                }
                plugin.getMenuManager().openTpaMenu(player);
            }
            case "open_inventories" -> openInventoriesPicker(player);
            case "open_inventory" -> openStoredInventory(player, id);
            case "open_back" -> handleBack(player);
            case "open_main" -> plugin.getMenuManager().openMainMenu(player);
            case "refresh_menu" -> plugin.getMenuManager().openMainMenu(player);
            case "page_next" -> openNextPage(holder, player);
            case "page_prev" -> openPreviousPage(holder, player);
            case "close_menu" -> player.closeInventory();
            case "warp" -> teleportToWarp(player, id);
            case "home" -> teleportToHome(player, id);
            case "tpa_accept" -> {
                if (!player.hasPermission("aura.tpa")) {
                    player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                    return;
                }
                player.closeInventory();
                if (!plugin.getTpaManager().hasPending(player.getUniqueId())) {
                    plugin.messages().send(player, "tpa.no-pending");
                    return;
                }
                plugin.getTpaManager().accept(player);
            }
            case "tpa_deny" -> {
                if (!player.hasPermission("aura.tpa")) {
                    player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                    return;
                }
                player.closeInventory();
                if (!plugin.getTpaManager().hasPending(player.getUniqueId())) {
                    plugin.messages().send(player, "tpa.no-pending");
                    return;
                }
                plugin.getTpaManager().deny(player);
            }
            case "noop", "tpa_info" -> {
                // informational only
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // no-op; menus are stateless and reopen on demand
    }

    private void openInventoriesPicker(Player player) {
        openInventoriesPicker(player, 0);
    }

    private void openInventoriesPicker(Player player, int page) {
        if (plugin.getPlayerInventoryManager() == null || !plugin.getPlayerInventoryManager().isEnabled()) {
            player.sendMessage(plugin.prefix(plugin.messages().get("inventories.disabled")));
            return;
        }
        if (!player.hasPermission("aura.inv")) {
            player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
            return;
        }
        plugin.getMenuManager().openInventoriesMenu(player, page);
    }

    private void openStoredInventory(Player player, String id) {
        if (plugin.getPlayerInventoryManager() == null || !plugin.getPlayerInventoryManager().isEnabled()) {
            player.sendMessage(plugin.prefix(plugin.messages().get("inventories.disabled")));
            return;
        }
        if (!player.hasPermission("aura.inv")) {
            player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
            return;
        }
        if (id == null) {
            return;
        }
        int number;
        try {
            number = Integer.parseInt(id);
        } catch (NumberFormatException ignored) {
            return;
        }
        if (!plugin.getPlayerInventoryManager().canOpen(player, number)) {
            plugin.messages().send(player, "inventories.denied",
                    "number", String.valueOf(number),
                    "limit", String.valueOf(plugin.getPlayerInventoryManager().resolveLimit(player)));
            return;
        }
        plugin.getMenuManager().openInventoryLater(player, number);
    }

    private void handleBack(Player player) {
        if (!player.hasPermission("aura.back")) {
            player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
            return;
        }
        player.closeInventory();

        var backLocation = plugin.getBackManager().get(player.getUniqueId());
        if (backLocation == null) {
            plugin.messages().send(player, "back.none");
            return;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        String label = plugin.messages().get("back.label");
        if (tpCountdown > 0) {
            plugin.getTeleportHelper().scheduleTeleport(player, backLocation, tpCountdown, label, EconomyAction.BACK);
        } else {
            plugin.getTeleportHelper().teleportExact(player, backLocation, "back.success", label, EconomyAction.BACK);
        }
    }

    private void openNextPage(UtilityMenuHolder holder, Player player) {
        int nextPage = holder.getPage() + 1;
        if (holder.getType() == MenuType.WARPS) {
            if (!player.hasPermission("aura.warp")) {
                player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                return;
            }
            plugin.getMenuManager().openWarpsMenu(player, nextPage);
        } else if (holder.getType() == MenuType.HOMES) {
            if (!player.hasPermission("aura.home")) {
                player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                return;
            }
            plugin.getMenuManager().openHomesMenu(player, nextPage);
        } else if (holder.getType() == MenuType.INVENTORIES) {
            openInventoriesPicker(player, nextPage);
        }
    }

    private void openPreviousPage(UtilityMenuHolder holder, Player player) {
        int previousPage = Math.max(0, holder.getPage() - 1);
        if (holder.getType() == MenuType.WARPS) {
            if (!player.hasPermission("aura.warp")) {
                player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                return;
            }
            plugin.getMenuManager().openWarpsMenu(player, previousPage);
        } else if (holder.getType() == MenuType.HOMES) {
            if (!player.hasPermission("aura.home")) {
                player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
                return;
            }
            plugin.getMenuManager().openHomesMenu(player, previousPage);
        } else if (holder.getType() == MenuType.INVENTORIES) {
            openInventoriesPicker(player, previousPage);
        }
    }

    private void teleportToWarp(Player player, String name) {
        if (name == null) {
            return;
        }
        if (!player.hasPermission("aura.warp")) {
            player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
            return;
        }
        var dest = plugin.getTeleportStoreManager().getWarpDestination(name);
        if (dest == null) {
            plugin.messages().send(player, "warp.not-found", "name", name);
            return;
        }
        var location = dest.getLocation();
        if (location == null) {
            plugin.messages().send(player, "teleport.destination-world");
            return;
        }
        String displayName = dest.getDisplayName();
        player.closeInventory();
        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        String label = plugin.messages().get("warp.destination-label", "name", displayName);
        if (tpCountdown > 0) {
            plugin.getTeleportHelper().scheduleTeleport(player, location, tpCountdown, label, EconomyAction.WARP);
        } else {
            plugin.getTeleportHelper().teleportExact(player, location,
                    "warp.teleported", label, EconomyAction.WARP, "name", displayName);
        }
    }

    private void teleportToHome(Player player, String name) {
        if (name == null) {
            return;
        }
        if (!player.hasPermission("aura.home")) {
            player.sendMessage(plugin.prefix(plugin.messages().get("common.no-permission")));
            return;
        }
        var dest = plugin.getTeleportStoreManager().getHomeDestination(player.getUniqueId(), name);
        if (dest == null) {
            plugin.messages().send(player, "home.not-found", "name", name);
            return;
        }
        var location = dest.getLocation();
        if (location == null) {
            plugin.messages().send(player, "teleport.destination-world");
            return;
        }
        String displayName = dest.getDisplayName();
        player.closeInventory();
        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        String label = plugin.messages().get("home.destination-label", "name", displayName);
        if (tpCountdown > 0) {
            plugin.getTeleportHelper().scheduleTeleport(player, location, tpCountdown, label, EconomyAction.HOME);
        } else {
            plugin.getTeleportHelper().teleportExact(player, location,
                    "home.teleported", label, EconomyAction.HOME, "name", displayName);
        }
    }
}
