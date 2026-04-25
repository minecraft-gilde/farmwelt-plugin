package de.minecraftgilde.farmwelt.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;

public final class ResourceWorldRule {

    private final String worldName;
    private final ResourceWorldType type;
    private final int seaLevel;
    private final Set<Material> surfaceResources;
    private final Set<Material> undergroundResources;
    private final Set<Material> resources;

    public ResourceWorldRule(
            String worldName,
            ResourceWorldType type,
            int seaLevel,
            Set<Material> surfaceResources,
            Set<Material> undergroundResources,
            Set<Material> resources
    ) {
        this.worldName = worldName;
        this.type = type;
        this.seaLevel = seaLevel;
        this.surfaceResources = copyMaterials(surfaceResources);
        this.undergroundResources = copyMaterials(undergroundResources);
        this.resources = copyMaterials(resources);
    }

    public String getWorldName() {
        return worldName;
    }

    public ResourceWorldType getType() {
        return type;
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public Set<Material> getSurfaceResources() {
        return surfaceResources;
    }

    public Set<Material> getUndergroundResources() {
        return undergroundResources;
    }

    public Set<Material> getResources() {
        return resources;
    }

    private Set<Material> copyMaterials(Set<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(EnumSet.copyOf(materials));
    }
}
