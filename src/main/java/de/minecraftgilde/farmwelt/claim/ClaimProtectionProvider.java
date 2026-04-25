package de.minecraftgilde.farmwelt.claim;

import org.bukkit.Location;

public interface ClaimProtectionProvider {

    boolean isAvailable();

    boolean isInsideClaim(Location location);

    String getName();
}
