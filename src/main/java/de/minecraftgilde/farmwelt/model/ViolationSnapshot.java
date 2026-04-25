package de.minecraftgilde.farmwelt.model;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;

public record ViolationSnapshot(
        UUID playerId,
        int currentCount,
        int blockedCount,
        Instant windowStart,
        Instant lastViolationTime,
        Instant lastBlockedAttemptTime,
        String latestWorld,
        int latestX,
        int latestY,
        int latestZ,
        Material latestBlock,
        String latestCategory,
        boolean jailActionExecutedInCurrentWindow,
        Instant lastJailActionTime
) {
}
