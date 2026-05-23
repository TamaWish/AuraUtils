package me.aurautils.menus;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TpaManager;
import me.aurautils.util.WarpPermissions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

/** GUI menus: titles use {@link me.aurautils.util.MessageUtil#toComponent} (gradients on Paper); item text uses legacy {@code &} codes. */
public class MenuManager {

    private static final int MENU_SIZE_SMALL = 27;
    private static final int MENU_SIZE_LARGE = 54;
    private static final int PAGE_SIZE = 45;

    private static final int SLOT_EMPTY_CENTER = 22;
    private static final int SLOT_PAGE_PREV = 45;
    private static final int SLOT_WARP_CATEGORIES = 46;
    private static final int SLOT_PAGE_INFO = 50;
    private static final int SLOT_MAIN_MENU = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_PAGE_NEXT = 53;

    private static final int MAIN_SLOT_WARPS = 10;
    private static final int MAIN_SLOT_HOMES = 11;
    private static final int MAIN_SLOT_TPA = 12;
    private static final int MAIN_SLOT_BACK = 14;
    private static final int MAIN_SLOT_REFRESH = 15;

    private static final int TPA_SLOT_ACCEPT = 11;
    private static final int TPA_SLOT_REQUESTER = 13;
    private static final int TPA_SLOT_DENY = 15;
    private static final int TPA_SLOT_MAIN_MENU = 26;

    private final AuraUtils plugin;

    public MenuManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        UtilityMenuHolder holder = new UtilityMenuHolder(MenuType.MAIN, 0);
        Inventory inventory = createMenu(holder, MENU_SIZE_SMALL, "&8Aura &7| &bMenu");
        holder.bind(inventory);

        inventory.setItem(MAIN_SLOT_WARPS, menuItem(Material.ENDER_PEARL, "&bWarps", "open_warps", "Open the warp list."));
        inventory.setItem(MAIN_SLOT_HOMES, menuItem(Material.RED_BED, "&dHomes", "open_homes", "Open your homes."));
        inventory.setItem(MAIN_SLOT_TPA, menuItem(Material.PAPER, "&eTPA", "open_tpa", "Review pending TPA requests."));
        inventory.setItem(13, menuItem(Material.COMPASS, "&aSpawn", "open_spawn", "Teleport to the world spawn."));
        inventory.setItem(16, menuItem(Material.GOLDEN_APPLE, "&cHeal", "open_heal", "Restore your health."));
        inventory.setItem(17, menuItem(Material.COOKED_BEEF, "&6Feed", "open_feed", "Restore your hunger."));
        inventory.setItem(MAIN_SLOT_BACK, menuItem(Material.ENDER_EYE, "&6Back", "open_back", "Return to your last teleport location."));
        inventory.setItem(MAIN_SLOT_REFRESH, menuItem(Material.COMPASS, "&aRefresh", "refresh_menu", "Rebuild this menu."));

        player.openInventory(inventory);
    }

    public void openWarpsMenu(Player player, int page) {
        openWarpsMenu(player, page, null, false);
    }

    public void openWarpsMenu(Player player, int page, String categoryFilter) {
        openWarpsMenu(player, page, categoryFilter, categoryFilter != null);
    }

    public void openWarpsMenu(Player player, int page, String categoryFilter, boolean skipCategoryPicker) {
        if (!skipCategoryPicker && categoryFilter == null && shouldShowWarpCategoryPicker(player)) {
            openWarpCategoriesMenu(player);
            return;
        }

        List<String> warps = new ArrayList<>();
        for (String name : plugin.getWarpManager().getWarpNamesSorted(categoryFilter)) {
            if (WarpPermissions.canUse(player, name)) {
                warps.add(name);
            }
        }

        String title = categoryFilter == null
                ? "&8Aura &7| &bWarps"
                : "&8Aura &7| &b" + formatCategoryTitle(categoryFilter);
        openPagedList(player, MenuType.WARPS, page, categoryFilter, warps, title, Material.ENDER_PEARL);
    }

    public void openWarpCategoriesMenu(Player player) {
        UtilityMenuHolder holder = new UtilityMenuHolder(MenuType.WARP_CATEGORIES, 0);
        Inventory inventory = createMenu(holder, MENU_SIZE_SMALL, "&8Aura &7| &bWarp Categories");
        holder.bind(inventory);

        int slot = 10;
        inventory.setItem(slot++, menuItem(Material.CHEST, "&fAll Warps", "open_warps_all", "Show every warp you can use."));
        for (String category : plugin.getWarpManager().getCategories()) {
            if (!playerHasWarpInCategory(player, category)) {
                continue;
            }
            inventory.setItem(slot++, menuItem(
                    Material.ENDER_CHEST,
                    "&b" + formatCategoryTitle(category),
                    "open_warp_category",
                    "Warps in category: " + category,
                    category));
        }
        if (plugin.getWarpManager().hasUncategorizedWarps() && playerHasWarpInCategory(player, "")) {
            inventory.setItem(slot, menuItem(Material.ENDER_PEARL, "&7Other", "open_warp_category", "Warps without a category.", ""));
        }

        inventory.setItem(SLOT_MAIN_MENU, menuItem(Material.BOOK, "&bMain Menu", "open_main", "Return to the main menu."));
        inventory.setItem(SLOT_CLOSE, menuItem(Material.BARRIER, "&cClose", "close_menu", "Close this menu."));
        player.openInventory(inventory);
    }

    private boolean shouldShowWarpCategoryPicker(Player player) {
        int categoryCount = 0;
        for (String category : plugin.getWarpManager().getCategories()) {
            if (playerHasWarpInCategory(player, category)) {
                categoryCount++;
            }
        }
        if (plugin.getWarpManager().hasUncategorizedWarps() && playerHasWarpInCategory(player, "")) {
            categoryCount++;
        }
        return categoryCount > 1;
    }

    private boolean playerHasWarpInCategory(Player player, String categoryFilter) {
        for (String name : plugin.getWarpManager().getWarpNamesSorted(categoryFilter)) {
            if (WarpPermissions.canUse(player, name)) {
                return true;
            }
        }
        return false;
    }

    private static String formatCategoryTitle(String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isEmpty()) {
            return "Other";
        }
        if (categoryFilter.length() == 1) {
            return categoryFilter.toUpperCase(Locale.ROOT);
        }
        return categoryFilter.substring(0, 1).toUpperCase(Locale.ROOT) + categoryFilter.substring(1);
    }

    public void openHomesMenu(Player player, int page) {
        List<String> homes = plugin.getHomeManager().getHomeNames(player.getUniqueId());
        openPagedList(player, MenuType.HOMES, page, null, homes, "&8Aura &7| &dHomes", Material.RED_BED);
    }

    public void openTpaMenu(Player player) {
        UtilityMenuHolder holder = new UtilityMenuHolder(MenuType.TPA, 0);
        Inventory inventory = createMenu(holder, MENU_SIZE_SMALL, "&8Aura &7| &eTPA");
        holder.bind(inventory);

        UUID requesterId = plugin.getTpaManager().getPendingRequester(player.getUniqueId());
        if (requesterId == null) {
            inventory.setItem(TPA_SLOT_REQUESTER, menuItem(Material.BARRIER, "&cNo pending TPA requests", "noop", "No requests are waiting for you."));
            inventory.setItem(TPA_SLOT_MAIN_MENU, menuItem(Material.BOOK, "&bMain Menu", "open_main", "Return to the main menu."));
            player.openInventory(inventory);
            return;
        }

        String requesterName = plugin.getServer().getPlayer(requesterId) != null
                ? plugin.getServer().getPlayer(requesterId).getName()
                : requesterId.toString();

        TpaManager.TpaType type = plugin.getTpaManager().getPendingType(player.getUniqueId());
        String typeHint = type == TpaManager.TpaType.TO_REQUESTER
                ? "&7Wants you to teleport to them."
                : "&7Wants to teleport to you.";

        inventory.setItem(TPA_SLOT_ACCEPT, menuItem(Material.LIME_WOOL, "&aAccept", "tpa_accept", "Accept the pending request."));
        inventory.setItem(TPA_SLOT_DENY, menuItem(Material.RED_WOOL, "&cDeny", "tpa_deny", "Deny the pending request."));
        inventory.setItem(TPA_SLOT_REQUESTER, playerHead(requesterName, requesterId, "&e" + requesterName, typeHint));
        inventory.setItem(TPA_SLOT_MAIN_MENU, menuItem(Material.BOOK, "&bMain Menu", "open_main", "Return to the main menu."));
        player.openInventory(inventory);
    }

    public void openMenuForType(Player player, UtilityMenuHolder holder) {
        openMenuForType(player, holder.getType(), holder.getPage(), holder.getWarpCategoryFilter());
    }

    public void openMenuForType(Player player, MenuType type, int page) {
        openMenuForType(player, type, page, null);
    }

    public void openMenuForType(Player player, MenuType type, int page, String warpCategoryFilter) {
        switch (type) {
            case MAIN -> openMainMenu(player);
            case WARP_CATEGORIES -> openWarpCategoriesMenu(player);
            case WARPS -> openWarpsMenu(player, page, warpCategoryFilter, true);
            case HOMES -> openHomesMenu(player, page);
            case TPA -> openTpaMenu(player);
        }
    }

    private void openPagedList(Player player, MenuType type, int page, String warpCategoryFilter,
                               List<String> entries, String title, Material icon) {
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        UtilityMenuHolder holder = new UtilityMenuHolder(type, safePage, warpCategoryFilter);
        Inventory inventory = createMenu(holder, MENU_SIZE_LARGE, title);
        holder.bind(inventory);

        if (entries.isEmpty()) {
            inventory.setItem(SLOT_EMPTY_CENTER, menuItem(Material.BARRIER, "&cNo entries", "noop", "Nothing is configured yet."));
        }

        int start = safePage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int index = start; index < end; index++) {
            String name = entries.get(index);
            Location location = type == MenuType.WARPS
                    ? plugin.getWarpManager().getWarp(name)
                    : plugin.getHomeManager().getHome(player.getUniqueId(), name);
            String action = type == MenuType.WARPS ? "warp" : "home";
            List<String> lore = locationLore(location);
            if (type == MenuType.WARPS) {
                var warpData = plugin.getWarpManager().getWarpData(name);
                if (warpData != null && warpData.getCategory() != null) {
                    lore.add(0, "&8Category: &f" + formatCategoryTitle(warpData.getCategory()));
                }
            }
            inventory.setItem(slot++, taggedItem(icon, name, action, name, lore));
        }

        fillPagedNavigation(inventory, player, type, warpCategoryFilter, safePage, totalPages);
        player.openInventory(inventory);
    }

    /** Bottom-row controls for warps/homes lists: prev/next (or placeholders), main menu, optional warp categories. */
    private void fillPagedNavigation(Inventory inventory, Player player, MenuType type, String warpCategoryFilter,
                                       int safePage, int totalPages) {
        if (totalPages > 1) {
            if (safePage > 0) {
                inventory.setItem(SLOT_PAGE_PREV, menuItem(Material.ARROW, "&ePrevious Page", "page_prev", "Go to the previous page."));
            } else {
                inventory.setItem(SLOT_PAGE_PREV, menuItem(Material.GRAY_STAINED_GLASS_PANE, "&7Previous Page", "noop",
                        "You are on the first page."));
            }
            if (safePage < totalPages - 1) {
                inventory.setItem(SLOT_PAGE_NEXT, menuItem(Material.ARROW, "&eNext Page", "page_next", "Go to the next page."));
            } else {
                inventory.setItem(SLOT_PAGE_NEXT, menuItem(Material.GRAY_STAINED_GLASS_PANE, "&7Next Page", "noop",
                        "You are on the last page."));
            }
            inventory.setItem(SLOT_PAGE_INFO, menuItem(Material.PAPER, "&8Page &f" + (safePage + 1) + "/" + totalPages,
                    "noop", "Current page."));
        }

        if (type == MenuType.WARPS && shouldShowWarpCategoryPicker(player)) {
            inventory.setItem(SLOT_WARP_CATEGORIES, menuItem(Material.CHEST, "&eCategories", "open_warp_categories",
                    "Back to warp categories."));
        }

        inventory.setItem(SLOT_MAIN_MENU, menuItem(Material.BOOK, "&bMain Menu", "open_main", "Return to the main menu."));
        inventory.setItem(SLOT_CLOSE, menuItem(Material.BARRIER, "&cClose", "close_menu", "Close this menu."));
    }

    public ItemStack menuItem(Material material, String displayName, String action, String loreLine) {
        return menuItem(material, displayName, action, loreLine, null);
    }

    public ItemStack menuItem(Material material, String displayName, String action, String loreLine, String id) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(displayName));
        meta.setLore(List.of(color("&7" + loreLine)));
        meta.getPersistentDataContainer().set(pluginKey("action"), PersistentDataType.STRING, action);
        if (id != null) {
            meta.getPersistentDataContainer().set(pluginKey("id"), PersistentDataType.STRING, id);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack taggedItem(Material material, String displayName, String action, String id, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&f" + displayName));
        meta.setLore(loreLines.stream().map(this::color).toList());
        meta.getPersistentDataContainer().set(pluginKey("action"), PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(pluginKey("id"), PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> locationLore(Location location) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Click to teleport.");
        if (location == null || location.getWorld() == null) {
            lore.add("&cLocation unavailable.");
            return lore;
        }
        lore.add("&8X: &f" + (int) location.getBlockX()
                + " &8Y: &f" + (int) location.getBlockY()
                + " &8Z: &f" + (int) location.getBlockZ());
        lore.add("&8World: &f" + location.getWorld().getName());
        lore.add("&8Dimension: &f" + formatEnvironment(location.getWorld().getEnvironment()));
        return lore;
    }

    private static String formatEnvironment(World.Environment environment) {
        return switch (environment) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "The End";
            default -> environment.name();
        };
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

    private Inventory createMenu(UtilityMenuHolder holder, int size, String title) {
        return plugin.getPlatform().createInventory(holder, size, me.aurautils.util.MessageUtil.toComponent(title));
    }

    private String color(String text) {
        return me.aurautils.util.MessageUtil.colorize(text);
    }
}
