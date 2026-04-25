package de.minecraftgilde.farmwelt.config;

import de.minecraftgilde.farmwelt.gui.FarmweltMenuItem;
import de.minecraftgilde.farmwelt.gui.TeleportAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    public static final int FARMWELT_MENU_SIZE = 45;
    public static final int FARMWELT_MENU_CONTENT_SIZE = 27;
    public static final int FARMWELT_MENU_CONTENT_OFFSET = 9;

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
                plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' ist kein gültiger Config-Bereich.");
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
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keinen Anzeigenamen und wird übersprungen.");
            return null;
        }

        String iconName = section.getString("icon");
        Material icon = iconName == null ? null : Material.matchMaterial(iconName.toUpperCase(Locale.ROOT));
        if (icon == null || !icon.isItem()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat ein ungültiges Icon und wird übersprungen: " + iconName);
            return null;
        }

        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= FARMWELT_MENU_CONTENT_SIZE) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat einen Slot außerhalb der GUI und wird übersprungen: " + slot);
            return null;
        }

        TeleportAction teleportAction = loadTeleportAction(key, section);
        if (teleportAction == null) {
            return null;
        }

        return new FarmweltMenuItem(
                key,
                displayName,
                icon,
                slot,
                section.getStringList("lore"),
                teleportAction
        );
    }

    private TeleportAction loadTeleportAction(String key, ConfigurationSection section) {
        ConfigurationSection teleportSection = section.getConfigurationSection("teleport");
        if (teleportSection == null) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keine Teleport-Konfiguration und wird übersprungen.");
            return null;
        }

        String type = teleportSection.getString("type");
        if (type == null || type.isBlank()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keinen Teleport-Typ und wird übersprungen.");
            return null;
        }

        if (!"command".equalsIgnoreCase(type)) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' nutzt einen nicht unterstützten Teleport-Typ und wird übersprungen: " + type);
            return null;
        }

        String sender = teleportSection.getString("sender", "player");
        if (sender == null || sender.isBlank()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keinen Teleport-Absender. Es wird 'player' verwendet.");
            sender = "player";
        }

        String normalizedSender = sender.toLowerCase(Locale.ROOT);
        if (!"player".equals(normalizedSender) && !"console".equals(normalizedSender)) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' nutzt einen unbekannten Teleport-Absender. Es wird 'player' verwendet: " + sender);
            normalizedSender = "player";
        }

        String command = teleportSection.getString("command");
        if (command == null || command.isBlank()) {
            plugin.getLogger().warning("Farmwelt-Eintrag '" + key + "' hat keinen Teleport-Befehl und wird übersprungen.");
            return null;
        }

        return new TeleportAction(
                type.toLowerCase(Locale.ROOT),
                normalizedSender,
                command
        );
    }
}
