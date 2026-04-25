package de.minecraftgilde.farmwelt.claim;

import org.bukkit.Location;

public final class NoopClaimProtectionProvider implements ClaimProtectionProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isInsideClaim(Location location) {
        return false;
    }

    @Override
    public String getName() {
        return "none";
    }
}
