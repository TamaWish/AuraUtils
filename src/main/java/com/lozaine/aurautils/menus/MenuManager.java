package com.lozaine.aurautils.menus;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.economy.EconomyAction;
import com.lozaine.aurautils.managers.StoredDestination;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MenuManager {

    private static final int PAGE_SIZE = 45;

    private final AuraUtils plugin;

    public MenuManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        var msg = plugin.messages();
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(MenuType.MAIN, 0), 27,
                color(msg.get("menu.main-title")));

        inventory.setItem(10, menuItem(Material.ENDER_PEARL, msg.get("menu.warps-name"), "open_warps", msg.get("menu.warps-lore")));
        inventory.setItem(11, menuItem(Material.RED_BED, msg.get("menu.homes-name"), "open_homes", msg.get("menu.homes-lore")));
        inventory.setItem(12, menuItem(Material.PAPER, msg.get("menu.tpa-name"), "open_tpa", msg.get("menu.tpa-lore")));
        if (plugin.getPlayerInventoryManager() != null
                && plugin.getPlayerInventoryManager().isEnabled()
                && player.hasPermission("aura.inv")) {
            inventory.setItem(13, menuItem(Material.CHEST, msg.get("menu.inventories-name"), "open_inventories",
                    msg.get("menu.inventories-lore")));
        }
        inventory.setItem(14, menuItem(Material.ENDER_EYE, msg.get("menu.back-name"), "open_back",
                msg.get("menu.back-lore"),
                plugin.economy().costLine(player, EconomyAction.BACK)));
        inventory.setItem(15, menuItem(Material.COMPASS, msg.get("menu.refresh-name"), "refresh_menu", msg.get("menu.refresh-lore")));

        player.openInventory(inventory);
    }

    public void openWarpsMenu(Player player, int page) {
        List<StoredDestination> warps = plugin.getTeleportStoreManager().getWarps();
        openPagedDestinations(player, MenuType.WARPS, page, warps, plugin.messages().get("menu.warps-title"), Material.ENDER_PEARL, "warp");
    }

    public void openHomesMenu(Player player, int page) {
        List<StoredDestination> homes = plugin.getTeleportStoreManager().getHomes(player.getUniqueId());
        openPagedDestinations(player, MenuType.HOMES, page, homes, plugin.messages().get("menu.homes-title"), Material.RED_BED, "home");
    }

    public void openTpaMenu(Player player) {
        var msg = plugin.messages();
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(MenuType.TPA, 0), 27,
                color(msg.get("menu.tpa-title")));

        UUID requesterId = plugin.getTpaManager().getPendingRequester(player.getUniqueId());
        if (requesterId == null) {
            inventory.setItem(13, menuItem(Material.BARRIER, msg.get("menu.tpa-empty-name"), "noop", msg.get("menu.tpa-empty-lore")));
            player.openInventory(inventory);
            return;
        }

        String storedName = plugin.getTpaManager().getPendingRequesterName(player.getUniqueId());
        String requesterName = plugin.getServer().getPlayer(requesterId) != null
                ? plugin.getServer().getPlayer(requesterId).getName()
                : (storedName != null ? storedName : requesterId.toString());

        inventory.setItem(11, menuItem(Material.LIME_WOOL, msg.get("menu.tpa-accept-name"), "tpa_accept", msg.get("menu.tpa-accept-lore")));
        inventory.setItem(15, menuItem(Material.RED_WOOL, msg.get("menu.tpa-deny-name"), "tpa_deny", msg.get("menu.tpa-deny-lore")));
        inventory.setItem(13, playerHead(requesterName, requesterId,
                msg.get("menu.tpa-requester-name", "player", requesterName),
                msg.get("menu.tpa-requester-lore")));
        inventory.setItem(26, menuItem(Material.BOOK, msg.get("menu.main-menu-name"), "open_main", msg.get("menu.main-menu-lore")));
        player.openInventory(inventory);
    }

    public void openInventoriesMenu(Player player, int page) {
        var msg = plugin.messages();
        var inventories = plugin.getPlayerInventoryManager();
        if (inventories == null || !inventories.isEnabled()) {
            msg.send(player, "inventories.disabled");
            return;
        }
        int limit = inventories.resolveLimit(player);
        if (limit <= 0) {
            msg.send(player, "inventories.none");
            return;
        }
        int totalPages = Math.max(1, (int) Math.ceil(limit / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(MenuType.INVENTORIES, safePage), 54,
                color(msg.get("menu.inventories-title")));

        int start = safePage * PAGE_SIZE;
        int end = Math.min(limit, start + PAGE_SIZE);
        int slot = 0;
        for (int number = start + 1; number <= end; number++) {
            inventory.setItem(slot++, inventoryIcon(number, limit));
        }

        if (totalPages > 1) {
            if (safePage > 0) {
                inventory.setItem(45, menuItem(Material.ARROW, msg.get("menu.prev-name"), "page_prev", msg.get("menu.prev-lore")));
            }
            if (safePage < totalPages - 1) {
                inventory.setItem(53, menuItem(Material.ARROW, msg.get("menu.next-name"), "page_next", msg.get("menu.next-lore")));
            }
        }

        inventory.setItem(48, menuItem(Material.BOOK, msg.get("menu.main-menu-name"), "open_main", msg.get("menu.main-menu-lore")));
        inventory.setItem(49, menuItem(Material.BARRIER, msg.get("menu.close-name"), "close_menu", msg.get("menu.close-lore")));
        player.openInventory(inventory);
    }

    public void openInventoryLater(Player player, int number) {
        if (plugin.getPlayerInventoryManager() == null) {
            return;
        }
        plugin.getScheduler().runAtEntityLater(player, () -> {
            if (player.isOnline() && plugin.isEnabled() && plugin.getPlayerInventoryManager().canOpen(player, number)) {
                plugin.getPlayerInventoryManager().open(player, number);
            }
        }, 1L);
    }

    public void openMenuForType(Player player, MenuType type, int page) {
        switch (type) {
            case MAIN -> openMainMenu(player);
            case WARPS -> openWarpsMenu(player, page);
            case HOMES -> openHomesMenu(player, page);
            case TPA -> openTpaMenu(player);
            case INVENTORIES -> openInventoriesMenu(player, page);
        }
    }

    private ItemStack inventoryIcon(int number, int limit) {
        var msg = plugin.messages();
        ItemStack item = menuItem(
                Material.CHEST,
                msg.get("menu.inventory-name", "number", String.valueOf(number)),
                "open_inventory",
                msg.get("menu.inventory-lore", "number", String.valueOf(number), "limit", String.valueOf(limit)));
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(pluginKey("id"), PersistentDataType.STRING, String.valueOf(number));
        item.setItemMeta(meta);
        return item;
    }

    private void openPagedDestinations(
            Player player,
            MenuType type,
            int page,
            List<StoredDestination> entries,
            String title,
            Material icon,
            String clickAction
    ) {
        var msg = plugin.messages();
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(type, safePage), 54, color(title));

        if (entries.isEmpty()) {
            inventory.setItem(22, menuItem(Material.BARRIER, msg.get("menu.empty-name"), "noop", msg.get("menu.empty-lore")));
        }

        int start = safePage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int index = start; index < end; index++) {
            StoredDestination dest = entries.get(index);
            inventory.setItem(slot++, destinationItem(icon, dest, clickAction, player));
        }

        if (totalPages > 1) {
            if (safePage > 0) {
                inventory.setItem(45, menuItem(Material.ARROW, msg.get("menu.prev-name"), "page_prev", msg.get("menu.prev-lore")));
            }
            if (safePage < totalPages - 1) {
                inventory.setItem(53, menuItem(Material.ARROW, msg.get("menu.next-name"), "page_next", msg.get("menu.next-lore")));
            }
        }

        inventory.setItem(48, menuItem(Material.BOOK, msg.get("menu.main-menu-name"), "open_main", msg.get("menu.main-menu-lore")));
        inventory.setItem(49, menuItem(Material.BARRIER, msg.get("menu.close-name"), "close_menu", msg.get("menu.close-lore")));
        player.openInventory(inventory);
    }

    /**
     * Home/warp icon: display name keeps original casing.
     * Lore colors: name (aqua), coords (gray/yellow numbers), setter (green).
     * PDC id stays normalized key so lookup still works.
     */
    private ItemStack destinationItem(Material material, StoredDestination dest, String action, Player viewer) {
        var msg = plugin.messages();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Title: original casing
        meta.setDisplayName(color("&f" + dest.getDisplayName()));

        String storedWorld = dest.getWorldName();
        String worldName = (storedWorld == null || storedWorld.isBlank())
                ? msg.get("common.unknown-world")
                : storedWorld;
        String coords = (storedWorld == null || storedWorld.isBlank())
                ? msg.get("common.coords-unknown")
                : String.format(Locale.US, "%.2f, %.2f, %.2f", dest.getX(), dest.getY(), dest.getZ());

        List<String> lore = new ArrayList<>();
        lore.add(color("&b" + dest.getDisplayName()));
        lore.add(color(msg.get("menu.dest-coords", "coords", coords)));
        lore.add(color(msg.get("menu.dest-world", "world", worldName)));
        String setter = dest.getSetByName();
        if (setter == null || setter.isBlank() || "Unknown".equalsIgnoreCase(setter)) {
            setter = msg.get("common.unknown");
        }
        lore.add(color(msg.get("menu.dest-setter", "setter", setter)));
        lore.add(color(msg.get("menu.dest-click")));
        EconomyAction costAction = "warp".equals(action) ? EconomyAction.WARP
                : "home".equals(action) ? EconomyAction.HOME : null;
        String costLine = costAction == null || plugin.economy() == null
                ? null
                : plugin.economy().costLine(viewer, costAction);
        if (costLine != null) {
            lore.add(color(costLine));
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(pluginKey("action"), PersistentDataType.STRING, action);
        // Use normalized key for reliable lookup; display name is only visual
        meta.getPersistentDataContainer().set(pluginKey("id"), PersistentDataType.STRING, dest.getKey());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack menuItem(Material material, String displayName, String action, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(displayName));
        List<String> lore = new ArrayList<>();
        if (loreLines != null) {
            for (String line : loreLines) {
                if (line != null && !line.isBlank()) {
                    lore.add(color(line));
                }
            }
        }
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        meta.getPersistentDataContainer().set(pluginKey("action"), PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack playerHead(String displayName, UUID ownerId, String title, String loreLine) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(color(title));
        meta.setLore(List.of(color(loreLine)));
        meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(ownerId));
        meta.getPersistentDataContainer().set(pluginKey("action"), PersistentDataType.STRING, "tpa_info");
        meta.getPersistentDataContainer().set(pluginKey("id"), PersistentDataType.STRING, ownerId.toString());
        item.setItemMeta(meta);
        return item;
    }

    private org.bukkit.NamespacedKey pluginKey(String key) {
        return new org.bukkit.NamespacedKey(plugin, key);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
