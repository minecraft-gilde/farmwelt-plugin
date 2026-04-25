package de.minecraftgilde.farmwelt.service;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.model.ResourceMatch;
import de.minecraftgilde.farmwelt.model.ViolationAction;
import de.minecraftgilde.farmwelt.model.ViolationRecord;
import de.minecraftgilde.farmwelt.model.ViolationResult;
import de.minecraftgilde.farmwelt.model.ViolationSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class ViolationService {

    private final ConcurrentMap<UUID, ViolationRecord> records = new ConcurrentHashMap<>();
    private final int windowSeconds;
    private final long windowMillis;
    private final ActionConfig warningConfig;
    private final ActionConfig staffNotifyConfig;
    private final ActionConfig cancelBreakConfig;

    public ViolationService(ConfigManager configManager) {
        this.windowSeconds = configManager.getViolationWindowSeconds();
        this.windowMillis = windowSeconds * 1000L;
        this.warningConfig = new ActionConfig(
                configManager.isViolationActionEnabled(ViolationAction.WARNING),
                configManager.getViolationActionAfterBlocks(ViolationAction.WARNING),
                configManager.getViolationActionCooldownSeconds(ViolationAction.WARNING)
        );
        this.staffNotifyConfig = new ActionConfig(
                configManager.isViolationActionEnabled(ViolationAction.NOTIFY_STAFF),
                configManager.getViolationActionAfterBlocks(ViolationAction.NOTIFY_STAFF),
                configManager.getViolationActionCooldownSeconds(ViolationAction.NOTIFY_STAFF)
        );
        this.cancelBreakConfig = new ActionConfig(
                configManager.isViolationActionEnabled(ViolationAction.CANCEL_BREAK),
                configManager.getViolationActionAfterBlocks(ViolationAction.CANCEL_BREAK),
                configManager.getViolationActionCooldownSeconds(ViolationAction.CANCEL_BREAK)
        );
    }

    public ViolationResult registerViolation(
            Player player,
            Block block,
            ResourceMatch match,
            boolean runWarnActions,
            boolean runCancelActions
    ) {
        UUID playerId = player.getUniqueId();
        Instant now = Instant.now();
        String worldName = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        Material material = match.material();
        String category = match.category();
        AtomicReference<ViolationRecord> updatedRecord = new AtomicReference<>();
        AtomicReference<Set<ViolationAction>> actionsToRun = new AtomicReference<>(Set.of());

        records.compute(playerId, (ignored, existingRecord) -> {
            boolean startNewWindow = existingRecord == null || isExpired(existingRecord, now);
            int count = startNewWindow ? 1 : existingRecord.currentCount() + 1;
            Instant windowStart = startNewWindow ? now : existingRecord.windowStart();
            Instant lastWarningTime = existingRecord == null ? null : existingRecord.lastWarningTime();
            Instant lastStaffNotifyTime = existingRecord == null ? null : existingRecord.lastStaffNotifyTime();
            Instant lastCancelBreakTime = existingRecord == null ? null : existingRecord.lastCancelBreakTime();
            EnumSet<ViolationAction> actions = EnumSet.noneOf(ViolationAction.class);

            if (runWarnActions && shouldRunAction(warningConfig, count, lastWarningTime, now)) {
                actions.add(ViolationAction.WARNING);
                lastWarningTime = now;
            }

            if (runWarnActions && shouldRunAction(staffNotifyConfig, count, lastStaffNotifyTime, now)) {
                actions.add(ViolationAction.NOTIFY_STAFF);
                lastStaffNotifyTime = now;
            }

            if (runCancelActions && shouldRunAction(cancelBreakConfig, count, lastCancelBreakTime, now)) {
                actions.add(ViolationAction.CANCEL_BREAK);
                lastCancelBreakTime = now;
            }

            ViolationRecord newRecord = new ViolationRecord(
                    playerId,
                    count,
                    windowStart,
                    now,
                    worldName,
                    x,
                    y,
                    z,
                    material,
                    category,
                    lastWarningTime,
                    lastStaffNotifyTime,
                    lastCancelBreakTime
            );
            updatedRecord.set(newRecord);
            actionsToRun.set(actions.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(actions)));
            return newRecord;
        });

        return new ViolationResult(updatedRecord.get().toSnapshot(), actionsToRun.get());
    }

    public Optional<ViolationSnapshot> getSnapshot(UUID playerId) {
        ViolationRecord record = records.get(playerId);
        if (record == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (isExpired(record, now)) {
            records.remove(playerId, record);
            return Optional.empty();
        }

        return Optional.of(record.toSnapshot());
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public long getRemainingWindowSeconds(ViolationSnapshot snapshot) {
        long elapsedSeconds = Duration.between(snapshot.windowStart(), Instant.now()).toSeconds();
        return Math.max(0L, windowSeconds - elapsedSeconds);
    }

    private boolean isExpired(ViolationRecord record, Instant now) {
        return Duration.between(record.windowStart(), now).toMillis() >= windowMillis;
    }

    private boolean shouldRunAction(ActionConfig config, int count, Instant lastRunTime, Instant now) {
        if (!config.enabled() || count < config.afterBlocks()) {
            return false;
        }

        long cooldownMillis = config.cooldownSeconds() * 1000L;
        return cooldownMillis <= 0L
                || lastRunTime == null
                || Duration.between(lastRunTime, now).toMillis() >= cooldownMillis;
    }

    private record ActionConfig(
            boolean enabled,
            int afterBlocks,
            int cooldownSeconds
    ) {
    }
}
