package de.minecraftgilde.farmwelt.config;

import de.minecraftgilde.farmwelt.gui.FarmweltMenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    public static final int FARMWELT_MENU_SIZE = 27;

    private final JavaPlugin plugin;
    private List<FarmweltMenuItem> farmweltMenuItems = List.of();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFarmweltMenuItems() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("farmworlds");
        if (section == null) {
            plugin.getLogger().warning("Config-Bereich 'farmworlds' fehlt. Es werden keine Farmwelten angezeigt.");
            farmweltMenuItems = List.of();
            return;
        }

        List<FarmweltMenuItem> loadedItems = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection farmworldSection = section.getConfigurationSection(key);
            if (farmworldSection == null) {
                plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' ist kein gueltiger Config-Bereich.");
                continue;
            }

            FarmweltMenuItem item = loadFarmweltMenuItem(key, farmworldSection);
            if (item != null) {
                loadedItems.add(item);
            }
        }

        farmweltMenuItems = Collections.unmodifiableList(loadedItems);
    }

    public List<FarmweltMenuItem> getFarmweltMenuItems() {
        return farmweltMenuItems;
    }

    private FarmweltMenuItem loadFarmweltMenuItem(String key, ConfigurationSection section) {
        if (!section.getBoolean("enabled", true)) {
            return null;
        }

        String displayName = section.getString("display-name");
        if (displayName == null || displayName.isBlank()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keinen Anzeigenamen und wird uebersprungen.");
            return null;
        }

        String iconName = section.getString("icon");
        Material icon = iconName == null ? null : Material.matchMaterial(iconName.toUpperCase(Locale.ROOT));
        if (icon == null || !icon.isItem()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat ein ungueltiges Icon und wird uebersprungen: " + iconName);
            return null;
        }

        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= FARMWELT_MENU_SIZE) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat einen Slot ausserhalb der GUI und wird uebersprungen: " + slot);
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keine Zielwelt und wird uebersprungen.");
            return null;
        }

        return new FarmweltMenuItem(
                key,
                displayName,
                icon,
                slot,
                section.getStringList("lore"),
                worldName,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }
}
