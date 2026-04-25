package de.minecraftgilde.farmwelt.model;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;

public record ViolationRecord(
        UUID playerId,
        int currentCount,
        Instant windowStart,
        Instant lastViolationTime,
        String latestWorld,
        int latestX,
        int latestY,
        int latestZ,
        Material latestBlock,
        String latestCategory,
        Instant lastWarningTime,
        Instant lastStaffNotifyTime,
        Instant lastCancelBreakTime
) {
    public ViolationSnapshot toSnapshot() {
        return new ViolationSnapshot(
                playerId,
                currentCount,
                windowStart,
                lastViolationTime,
                latestWorld,
                latestX,
                latestY,
                latestZ,
                latestBlock,
                latestCategory
        );
    }
}
