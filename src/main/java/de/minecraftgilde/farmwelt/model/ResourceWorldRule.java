package de.minecraftgilde.farmwelt.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;

public final class ResourceWorldRule {

    private final String worldName;
    private final ResourceWorldType type;
    private final Set<Material> resources;
    private final Set<Material> protectedItems;

    public ResourceWorldRule(
            String worldName,
            ResourceWorldType type,
            Set<Material> resources
    ) {
        this(worldName, type, resources, Set.of());
    }

    public ResourceWorldRule(
            String worldName,
            ResourceWorldType type,
            Set<Material> resources,
            Set<Material> protectedItems
    ) {
        this.worldName = worldName;
        this.type = type;
        this.resources = copyMaterials(resources);
        this.protectedItems = copyMaterials(protectedItems);
    }

    public String getWorldName() {
        return worldName;
    }

    public ResourceWorldType getType() {
        return type;
    }

    public Set<Material> getResources() {
        return resources;
    }

    public Set<Material> getProtectedItems() {
        return protectedItems;
    }

    private Set<Material> copyMaterials(Set<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(EnumSet.copyOf(materials));
    }
}
