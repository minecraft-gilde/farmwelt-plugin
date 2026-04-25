package de.minecraftgilde.farmwelt.gui;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class FarmweltMenu {

    private static final String TITLE = "Farmwelten";

    private final ConfigManager configManager;

    public FarmweltMenu(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void open(Player player) {
        FarmweltMenuHolder holder = new FarmweltMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, ConfigManager.FARMWELT_MENU_SIZE, Component.text(TITLE));
        holder.setInventory(inventory);

        for (FarmweltMenuItem menuItem : configManager.getFarmweltMenuItems()) {
            inventory.setItem(menuItem.slot(), createItemStack(menuItem));
            holder.addMenuItem(menuItem);
        }

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
}
