package me.aurautils.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class UtilityMenuHolder implements InventoryHolder {

    private final MenuType type;
    private final int page;
    /** {@code null} = all warps; empty string = uncategorized only. */
    private final String warpCategoryFilter;
    private Inventory inventory;

    public UtilityMenuHolder(MenuType type, int page) {
        this(type, page, null);
    }

    public UtilityMenuHolder(MenuType type, int page, String warpCategoryFilter) {
        this.type = type;
        this.page = page;
        this.warpCategoryFilter = warpCategoryFilter;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    public MenuType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public String getWarpCategoryFilter() {
        return warpCategoryFilter;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
