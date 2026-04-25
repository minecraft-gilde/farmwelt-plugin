package de.minecraftgilde.farmwelt.model;

import java.util.Set;

public record ViolationResult(
        ViolationSnapshot snapshot,
        Set<ViolationAction> actionsToRun
) {
    public boolean shouldRun(ViolationAction action) {
        return actionsToRun.contains(action);
    }
}
