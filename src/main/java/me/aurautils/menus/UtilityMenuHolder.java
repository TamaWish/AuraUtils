package me.aurautils.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class UtilityMenuHolder implements InventoryHolder {

    private final MenuType type;
    private final int page;

    public UtilityMenuHolder(MenuType type, int page) {
        this.type = type;
        this.page = page;
    }

    public MenuType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}