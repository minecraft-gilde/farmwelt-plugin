package de.minecraftgilde.farmwelt.gui;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class FarmweltMenu {

    private static final String TITLE = "Farmwelten";
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 40;

    private final ConfigManager configManager;

    public FarmweltMenu(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void open(Player player) {
        FarmweltMenuHolder holder = new FarmweltMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, ConfigManager.FARMWELT_MENU_SIZE, Component.text(TITLE));
        holder.setInventory(inventory);

        for (FarmweltMenuItem menuItem : configManager.getFarmweltMenuItems()) {
            int displaySlot = ConfigManager.FARMWELT_MENU_CONTENT_OFFSET + menuItem.slot();
            inventory.setItem(displaySlot, createItemStack(menuItem));
            holder.addMenuItem(displaySlot, menuItem);
        }

        addStaticItems(inventory, holder);
        player.openInventory(inventory);
    }

    private ItemStack createItemStack(FarmweltMenuItem menuItem) {
        ItemStack itemStack = new ItemStack(menuItem.icon());
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        itemMeta.displayName(Component.text(menuItem.displayName()));
        List<Component> lore = menuItem.lore().stream()
                .map(line -> (Component) Component.text(line))
                .toList();
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void addStaticItems(Inventory inventory, FarmweltMenuHolder holder) {
        if (inventory.getItem(INFO_SLOT) == null) {
            inventory.setItem(INFO_SLOT, createInfoItem());
        }

        if (inventory.getItem(CLOSE_SLOT) == null) {
            inventory.setItem(CLOSE_SLOT, createCloseItem());
            holder.addClickAction(CLOSE_SLOT, clicker -> clicker.closeInventory());
        }
    }

    private ItemStack createInfoItem() {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.displayName(Component.text("Farmwelt-Auswahl", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Wähle eine Farmwelt für den", NamedTextColor.GRAY),
                Component.text("Ressourcenabbau aus.", NamedTextColor.GRAY)
        ));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createCloseItem() {
        ItemStack itemStack = new ItemStack(Material.BARRIER);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.displayName(Component.text("Menü schließen", NamedTextColor.RED));
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
