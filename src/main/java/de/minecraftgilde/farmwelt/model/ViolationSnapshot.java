package de.minecraftgilde.farmwelt.model;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;

public record ViolationSnapshot(
        UUID playerId,
        int currentCount,
        Instant windowStart,
        Instant lastViolationTime,
        String latestWorld,
        int latestX,
        int latestY,
        int latestZ,
        Material latestBlock,
        String latestCategory
) {
}
