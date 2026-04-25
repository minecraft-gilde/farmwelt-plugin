package de.minecraftgilde.farmwelt.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class FarmweltMenuHolder implements InventoryHolder {

    private final Map<Integer, FarmweltMenuItem> menuItemsBySlot = new HashMap<>();
    private final Map<Integer, Consumer<HumanEntity>> clickActionsBySlot = new HashMap<>();
    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void addMenuItem(int slot, FarmweltMenuItem menuItem) {
        menuItemsBySlot.put(slot, menuItem);
    }

    public FarmweltMenuItem getMenuItem(int slot) {
        return menuItemsBySlot.get(slot);
    }

    public void addClickAction(int slot, Consumer<HumanEntity> action) {
        clickActionsBySlot.put(slot, action);
    }

    public boolean handleClick(int slot, HumanEntity clicker) {
        Consumer<HumanEntity> action = clickActionsBySlot.get(slot);
        if (action == null) {
            return false;
        }

        action.accept(clicker);
        return true;
    }
}
