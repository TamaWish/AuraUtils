package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import me.aurautils.managers.BackService;
import me.aurautils.managers.HomeService;
import me.aurautils.managers.RtpService;
import me.aurautils.managers.WarpService;
import me.aurautils.menus.MenuType;
import me.aurautils.menus.UtilityMenuHolder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    private final AuraUtils plugin;
    private final org.bukkit.NamespacedKey actionKey;
    private final org.bukkit.NamespacedKey idKey;

    public MenuListener(AuraUtils plugin) {
        this.plugin = plugin;
        this.actionKey = new org.bukkit.NamespacedKey(plugin, "action");
        this.idKey = new org.bukkit.NamespacedKey(plugin, "id");
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
            actionKey, PersistentDataType.STRING);
        String id = item.getItemMeta().getPersistentDataContainer().get(
            idKey, PersistentDataType.STRING);

        if (action == null) {
            return;
        }

        switch (action) {
            case "open_warps" -> plugin.getMenuManager().openWarpsMenu(player, 0);
            case "open_warps_all" -> plugin.getMenuManager().openWarpsMenu(player, 0, null, true);
            case "open_warp_category" -> plugin.getMenuManager().openWarpsMenu(player, 0, id, true);
            case "open_warp_categories" -> plugin.getMenuManager().openWarpCategoriesMenu(player);
            case "open_homes" -> plugin.getMenuManager().openHomesMenu(player, 0);
            case "open_tpa" -> plugin.getMenuManager().openTpaMenu(player);
            case "open_spawn" -> player.performCommand("spawn");
            case "open_heal" -> player.performCommand("heal");
            case "open_feed" -> player.performCommand("feed");
            case "open_back" -> {
                player.closeInventory();
                BackService.teleportBack(plugin, player);
            }
            case "open_main" -> plugin.getMenuManager().openMainMenu(player);
            
            case "refresh_menu" -> refreshMenu(holder, player);
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
            case "rtp_world" -> startRtpInWorld(player, id);
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // no-op; menus are stateless and reopen on demand
    }

    private void refreshMenu(UtilityMenuHolder holder, Player player) {
        plugin.getMenuManager().openMenuForType(player, holder);
    }

    private void openNextPage(UtilityMenuHolder holder, Player player) {
        int nextPage = holder.getPage() + 1;
        if (holder.getType() == MenuType.WARPS) {
            plugin.getMenuManager().openWarpsMenu(player, nextPage, holder.getWarpCategoryFilter(), true);
        } else if (holder.getType() == MenuType.HOMES) {
            plugin.getMenuManager().openHomesMenu(player, nextPage);
        }
    }

    private void openPreviousPage(UtilityMenuHolder holder, Player player) {
        int previousPage = Math.max(0, holder.getPage() - 1);
        if (holder.getType() == MenuType.WARPS) {
            plugin.getMenuManager().openWarpsMenu(player, previousPage, holder.getWarpCategoryFilter(), true);
        } else if (holder.getType() == MenuType.HOMES) {
            plugin.getMenuManager().openHomesMenu(player, previousPage);
        }
    }

    private void teleportToWarp(Player player, String name) {
        if (name == null) {
            return;
        }
        player.closeInventory();
        WarpService.teleport(plugin, player, name);
    }

    private void teleportToHome(Player player, String name) {
        if (name == null) {
            return;
        }
        player.closeInventory();
        HomeService.teleport(plugin, player, name);
    }

    private void startRtpInWorld(Player player, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            plugin.send(player, "rtp.world-invalid");
            return;
        }
        World bukkitWorld = plugin.getServer().getWorld(worldName);
        if (bukkitWorld == null) {
            plugin.send(player, "rtp.world-unloaded",
                    me.aurautils.util.MessagePlaceholders.of("world", worldName));
            return;
        }
        World world = RtpService.resolveNamedWorld(plugin, worldName);
        if (world == null) {
            plugin.send(player, "rtp.world-invalid");
            return;
        }
        player.closeInventory();
        RtpService.beginSearch(plugin, player, world);
    }
}