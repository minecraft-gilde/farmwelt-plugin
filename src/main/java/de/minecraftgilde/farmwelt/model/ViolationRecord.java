package de.minecraftgilde.farmwelt.model;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;

public record ViolationRecord(
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
        Instant lastWarningTime,
        Instant lastStaffNotifyTime,
        Instant lastCancelBreakTime,
        boolean jailActionExecutedInCurrentWindow,
        Instant lastJailActionTime
) {
    public ViolationSnapshot toSnapshot() {
        return new ViolationSnapshot(
                playerId,
                currentCount,
                blockedCount,
                windowStart,
                lastViolationTime,
                lastBlockedAttemptTime,
                latestWorld,
                latestX,
                latestY,
                latestZ,
                latestBlock,
                latestCategory,
                jailActionExecutedInCurrentWindow,
                lastJailActionTime
        );
    }
}
