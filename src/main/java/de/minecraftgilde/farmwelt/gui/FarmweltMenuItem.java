package de.minecraftgilde.farmwelt.gui;

import java.util.List;
import org.bukkit.Material;

public record FarmweltMenuItem(
        String key,
        String displayName,
        Material icon,
        int slot,
        List<String> lore,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
}
