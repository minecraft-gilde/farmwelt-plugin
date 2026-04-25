package de.minecraftgilde.farmwelt.service;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.model.ResourceMatch;
import de.minecraftgilde.farmwelt.model.ResourceWorldRule;
import de.minecraftgilde.farmwelt.model.ResourceWorldType;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.World;

public final class ResourceDetectionService {

    private final ConfigManager configManager;

    public ResourceDetectionService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public Optional<ResourceMatch> detect(World world, Material material, int blockY) {
        if (world == null || material == null) {
            return Optional.empty();
        }

        Optional<ResourceWorldRule> rule = configManager.getResourceWorldRule(world.getName());
        if (rule.isEmpty()) {
            return Optional.empty();
        }

        return detect(rule.get(), material, blockY);
    }

    private Optional<ResourceMatch> detect(ResourceWorldRule rule, Material material, int blockY) {
        ResourceWorldType type = rule.getType();
        return switch (type) {
            case OVERWORLD -> detectOverworld(rule, material, blockY);
            case NETHER -> rule.getResources().contains(material)
                    ? Optional.of(new ResourceMatch(type, "nether", material))
                    : Optional.empty();
            case END -> rule.getResources().contains(material)
                    ? Optional.of(new ResourceMatch(type, "end", material))
                    : Optional.empty();
        };
    }

    private Optional<ResourceMatch> detectOverworld(ResourceWorldRule rule, Material material, int blockY) {
        if (blockY >= rule.getSeaLevel()) {
            return rule.getSurfaceResources().contains(material)
                    ? Optional.of(new ResourceMatch(rule.getType(), "surface", material))
                    : Optional.empty();
        }

        return rule.getUndergroundResources().contains(material)
                ? Optional.of(new ResourceMatch(rule.getType(), "underground", material))
                : Optional.empty();
    }
}
