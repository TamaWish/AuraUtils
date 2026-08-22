package me.aurautils.menus;

import me.aurautils.AuraUtils;
import me.aurautils.managers.StoredDestination;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(MenuType.MAIN, 0), 27, color("&8Aura &7| &bMenu"));

        inventory.setItem(10, menuItem(Material.ENDER_PEARL, "&bWarps", "open_warps", "Open the warp list."));
        inventory.setItem(11, menuItem(Material.RED_BED, "&dHomes", "open_homes", "Open your homes."));
        inventory.setItem(12, menuItem(Material.PAPER, "&eTPA", "open_tpa", "Review pending TPA requests."));
        inventory.setItem(14, menuItem(Material.ENDER_EYE, "&6Back", "open_back", "Return to your last teleport location."));
        inventory.setItem(15, menuItem(Material.COMPASS, "&aRefresh", "refresh_menu", "Refresh the menu."));

        player.openInventory(inventory);
    }

    public void openWarpsMenu(Player player, int page) {
        List<StoredDestination> warps = plugin.getTeleportStoreManager().getWarps();
        openPagedDestinations(player, MenuType.WARPS, page, warps, "&8Aura &7| &bWarps", Material.ENDER_PEARL, "warp");
    }

    public void openHomesMenu(Player player, int page) {
        List<StoredDestination> homes = plugin.getTeleportStoreManager().getHomes(player.getUniqueId());
        openPagedDestinations(player, MenuType.HOMES, page, homes, "&8Aura &7| &dHomes", Material.RED_BED, "home");
    }

    public void openTpaMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(MenuType.TPA, 0), 27, color("&8Aura &7| &eTPA"));

        UUID requesterId = plugin.getTpaManager().getPendingRequester(player.getUniqueId());
        if (requesterId == null) {
            inventory.setItem(13, menuItem(Material.BARRIER, "&cNo pending TPA requests", "noop", "No requests are waiting for you."));
            player.openInventory(inventory);
            return;
        }

        String storedName = plugin.getTpaManager().getPendingRequesterName(player.getUniqueId());
        String requesterName = plugin.getServer().getPlayer(requesterId) != null
                ? plugin.getServer().getPlayer(requesterId).getName()
                : (storedName != null ? storedName : requesterId.toString());

        inventory.setItem(11, menuItem(Material.LIME_WOOL, "&aAccept", "tpa_accept", "Accept the pending request."));
        inventory.setItem(15, menuItem(Material.RED_WOOL, "&cDeny", "tpa_deny", "Deny the pending request."));
        inventory.setItem(13, playerHead(requesterName, requesterId, "&e" + requesterName, "&7Pending TPA requester."));
        inventory.setItem(26, menuItem(Material.BOOK, "&bMain Menu", "open_main", "Return to the main menu."));
        player.openInventory(inventory);
    }

    public void openMenuForType(Player player, MenuType type, int page) {
        switch (type) {
            case MAIN -> openMainMenu(player);
            case WARPS -> openWarpsMenu(player, page);
            case HOMES -> openHomesMenu(player, page);
            case TPA -> openTpaMenu(player);
        }
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
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        Inventory inventory = Bukkit.createInventory(new UtilityMenuHolder(type, safePage), 54, color(title));

        if (entries.isEmpty()) {
            inventory.setItem(22, menuItem(Material.BARRIER, "&cNo entries", "noop", "Nothing is configured yet."));
        }

        int start = safePage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int index = start; index < end; index++) {
            StoredDestination dest = entries.get(index);
            inventory.setItem(slot++, destinationItem(icon, dest, clickAction));
        }

        if (totalPages > 1) {
            if (safePage > 0) {
                inventory.setItem(45, menuItem(Material.ARROW, "&ePrevious Page", "page_prev", "Go to the previous page."));
            }
            if (safePage < totalPages - 1) {
                inventory.setItem(53, menuItem(Material.ARROW, "&eNext Page", "page_next", "Go to the next page."));
            }
        }

        inventory.setItem(48, menuItem(Material.BOOK, "&bMain Menu", "open_main", "Return to the main menu."));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", "close_menu", "Close this menu."));
        player.openInventory(inventory);
    }

    /**
     * Home/warp icon: display name keeps original casing.
     * Lore colors: name (aqua), coords (gray/yellow numbers), setter (green).
     * PDC id stays normalized key so lookup still works.
     */
    private ItemStack destinationItem(Material material, StoredDestination dest, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Title: original casing
        meta.setDisplayName(color("&f" + dest.getDisplayName()));

        Location loc = dest.getLocation();
        String worldName = (loc != null && loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
        String coords = loc == null
                ? "?, ?, ?"
                : String.format(Locale.US, "%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());

        List<String> lore = new ArrayList<>();
        lore.add(color("&b" + dest.getDisplayName()));           // name color
        lore.add(color("&7Coords: &e" + coords));              // coords different color
        lore.add(color("&7World: &f" + worldName));
        lore.add(color("&7Set by: &a" + dest.getSetByName())); // who set it
        lore.add(color("&8Click to teleport."));

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(pluginKey("action"), PersistentDataType.STRING, action);
        // Use normalized key for reliable lookup; display name is only visual
        meta.getPersistentDataContainer().set(pluginKey("id"), PersistentDataType.STRING, dest.getKey());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack menuItem(Material material, String displayName, String action, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(displayName));
        meta.setLore(List.of(color("&7" + loreLine)));
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
