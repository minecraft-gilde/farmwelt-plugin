package de.minecraftgilde.farmwelt.listener;

import de.minecraftgilde.farmwelt.gui.FarmweltMenuHolder;
import de.minecraftgilde.farmwelt.gui.FarmweltMenuItem;
import de.minecraftgilde.farmwelt.service.FarmweltTeleportService;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class FarmweltGuiListener implements Listener {

    private final FarmweltTeleportService teleportService;

    public FarmweltGuiListener(FarmweltTeleportService teleportService) {
        this.teleportService = teleportService;
    }

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

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        teleportService.teleport(player, menuItem);
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
