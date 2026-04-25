package de.minecraftgilde.farmwelt.gui;

import java.util.List;
import org.bukkit.Material;

public record FarmweltMenuItem(
        String id,
        String displayName,
        Material icon,
        int slot,
        List<String> lore,
        TeleportAction teleportAction
) {
}
