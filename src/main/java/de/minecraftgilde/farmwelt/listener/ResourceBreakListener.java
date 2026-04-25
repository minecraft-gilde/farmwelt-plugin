package de.minecraftgilde.farmwelt.listener;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.model.ResourceMatch;
import de.minecraftgilde.farmwelt.service.ClaimProtectionService;
import de.minecraftgilde.farmwelt.service.MessageService;
import de.minecraftgilde.farmwelt.service.ResourceDetectionService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class ResourceBreakListener implements Listener {

    private final ConfigManager configManager;
    private final ClaimProtectionService claimProtectionService;
    private final ResourceDetectionService resourceDetectionService;
    private final MessageService messageService;
    private final ConcurrentMap<AuditCooldownKey, Long> lastAuditLogTimes = new ConcurrentHashMap<>();

    public ResourceBreakListener(
            ConfigManager configManager,
            ClaimProtectionService claimProtectionService,
            ResourceDetectionService resourceDetectionService,
            MessageService messageService
    ) {
        this.configManager = configManager;
        this.claimProtectionService = claimProtectionService;
        this.resourceDetectionService = resourceDetectionService;
        this.messageService = messageService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!configManager.isResourceMonitorEnabled()) {
            return;
        }

        if (!configManager.isResourceMonitorAuditMode()) {
            return;
        }

        if (claimProtectionService.wouldDisableResourceMonitor()) {
            return;
        }

        Player player = event.getPlayer();
        String bypassPermission = configManager.getBypassPermission();
        if (!bypassPermission.isBlank() && player.hasPermission(bypassPermission)) {
            return;
        }

        Block block = event.getBlock();
        World world = block.getWorld();
        String worldName = world.getName();
        if (!configManager.isMonitoredWorld(worldName)) {
            return;
        }

        if (configManager.isIgnoredWorld(worldName)) {
            return;
        }

        if (!configManager.hasResourceWorldRule(worldName)) {
            return;
        }

        ResourceMatch match = resourceDetectionService.detect(world, block.getType(), block.getY()).orElse(null);
        if (match == null) {
            return;
        }

        if (claimProtectionService.shouldSkipInsideClaims() && claimProtectionService.isInsideClaim(block.getLocation())) {
            return;
        }

        if (!shouldEmitAudit(player.getUniqueId(), match.material(), match.category())) {
            return;
        }

        messageService.sendResourceAudit(player, block, match);
    }

    private boolean shouldEmitAudit(UUID playerId, Material material, String category) {
        long cooldownMillis = configManager.getAuditLogCooldownSeconds() * 1000L;
        if (cooldownMillis <= 0L) {
            return true;
        }

        long now = System.currentTimeMillis();
        AuditCooldownKey key = new AuditCooldownKey(playerId, material, category);
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastAuditLogTimes.compute(key, (ignored, lastLogTime) -> {
            if (lastLogTime == null || now - lastLogTime >= cooldownMillis) {
                allowed.set(true);
                return now;
            }

            return lastLogTime;
        });
        return allowed.get();
    }

    private record AuditCooldownKey(UUID playerId, Material material, String category) {
    }
}
