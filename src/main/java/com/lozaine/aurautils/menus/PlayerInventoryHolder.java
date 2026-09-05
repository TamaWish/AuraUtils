package com.lozaine.aurautils.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Holder for a player's stored extra inventory ({@code /inv <n>}).
 * Distinct from {@link UtilityMenuHolder} so menu click-cancel does not apply.
 */
public final class PlayerInventoryHolder implements InventoryHolder {

    private final UUID owner;
    private final int number;
    private Inventory inventory;

    public PlayerInventoryHolder(UUID owner, int number) {
        this.owner = owner;
        this.number = number;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getNumber() {
        return number;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
