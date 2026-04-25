package de.minecraftgilde.farmwelt.listener;

import de.minecraftgilde.farmwelt.gui.FarmweltMenuHolder;
import de.minecraftgilde.farmwelt.gui.FarmweltMenuItem;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class FarmweltGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof FarmweltMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            return;
        }

        FarmweltMenuItem menuItem = holder.getMenuItem(event.getSlot());
        if (menuItem == null) {
            holder.handleClick(event.getSlot(), event.getWhoClicked());
            return;
        }

        event.getWhoClicked().sendMessage("Du hast die Farmwelt \"" + menuItem.displayName()
                + "\" ausgewählt. Der Teleport wird später implementiert.");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof FarmweltMenuHolder)) {
            return;
        }

        Set<Integer> rawSlots = event.getRawSlots();
        for (int rawSlot : rawSlots) {
            if (rawSlot < topInventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
