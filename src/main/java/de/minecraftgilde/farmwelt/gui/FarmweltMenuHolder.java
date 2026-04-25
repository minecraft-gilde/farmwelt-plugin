package de.minecraftgilde.farmwelt.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class FarmweltMenuHolder implements InventoryHolder {

    private final Map<Integer, FarmweltMenuItem> menuItemsBySlot = new HashMap<>();
    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void addMenuItem(FarmweltMenuItem menuItem) {
        menuItemsBySlot.put(menuItem.slot(), menuItem);
    }

    public FarmweltMenuItem getMenuItem(int slot) {
        return menuItemsBySlot.get(slot);
    }
}
