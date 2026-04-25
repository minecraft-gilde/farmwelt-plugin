package de.minecraftgilde.farmwelt.model;

import java.util.Locale;
import java.util.Optional;

public enum ResourceWorldType {
    OVERWORLD("overworld"),
    NETHER("nether"),
    END("end");

    private final String configValue;

    ResourceWorldType(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public static Optional<ResourceWorldType> fromConfigValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        for (ResourceWorldType type : values()) {
            if (type.configValue.equals(normalized)) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }
}
