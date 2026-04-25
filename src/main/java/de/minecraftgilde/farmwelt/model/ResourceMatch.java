package de.minecraftgilde.farmwelt.model;

import org.bukkit.Material;

public record ResourceMatch(
        ResourceWorldType worldRuleType,
        String category,
        Material material
) {
}
