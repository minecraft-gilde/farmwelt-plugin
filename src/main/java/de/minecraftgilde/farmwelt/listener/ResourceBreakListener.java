package de.minecraftgilde.farmwelt.listener;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.model.ResourceMatch;
import de.minecraftgilde.farmwelt.model.ViolationAction;
import de.minecraftgilde.farmwelt.model.ViolationResult;
import de.minecraftgilde.farmwelt.service.ClaimProtectionService;
import de.minecraftgilde.farmwelt.service.JailActionService;
import de.minecraftgilde.farmwelt.service.MessageService;
import de.minecraftgilde.farmwelt.service.ResourceDetectionService;
import de.minecraftgilde.farmwelt.service.ViolationService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class ResourceBreakListener implements Listener {

    private final ConfigManager configManager;
    private final ClaimProtectionService claimProtectionService;
    private final ResourceDetectionService resourceDetectionService;
    private final MessageService messageService;
    private final ViolationService violationService;
    private final JailActionService jailActionService;
    private final ConcurrentMap<AuditCooldownKey, Long> lastAuditLogTimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<AuditCooldownKey, Long> lastProtectedLootCancelMessageTimes = new ConcurrentHashMap<>();

    public ResourceBreakListener(
            ConfigManager configManager,
            ClaimProtectionService claimProtectionService,
            ResourceDetectionService resourceDetectionService,
            MessageService messageService,
            ViolationService violationService,
            JailActionService jailActionService
    ) {
        this.configManager = configManager;
        this.claimProtectionService = claimProtectionService;
        this.resourceDetectionService = resourceDetectionService;
        this.messageService = messageService;
        this.violationService = violationService;
        this.jailActionService = jailActionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        World world = block.getWorld();
        ResourceMatch match = resourceDetectionService.detect(world, block.getType()).orElse(null);
        if (match == null) {
            return;
        }

        handleResourceAttempt(player, block, match, event, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame)) {
            return;
        }

        Player player = getResponsiblePlayer(event.getDamager());
        if (player == null) {
            return;
        }

        handleProtectedItemFrameAttempt(player, itemFrame, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame) || !shouldProtectHangingItemFrame(itemFrame)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protectExplosionBlockList(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protectExplosionBlockList(event.blockList());
    }

    private boolean shouldCancelBreak(ViolationResult violationResult) {
        return configManager.isViolationActionEnabled(ViolationAction.CANCEL_BREAK)
                && violationResult.snapshot().currentCount()
                >= configManager.getViolationActionAfterBlocks(ViolationAction.CANCEL_BREAK);
    }

    private void handleProtectedItemFrameAttempt(Player player, ItemFrame itemFrame, Cancellable event) {
        ItemStack itemStack = itemFrame.getItem();
        if (itemStack == null || itemStack.getType().isAir()) {
            return;
        }

        Block block = itemFrame.getLocation().getBlock();
        ResourceMatch match = resourceDetectionService.detectProtectedItem(block.getWorld(), itemStack.getType()).orElse(null);
        if (match == null) {
            return;
        }

        handleResourceAttempt(player, block, match, event, true);
    }

    private void handleResourceAttempt(
            Player player,
            Block block,
            ResourceMatch match,
            Cancellable event,
            boolean cancelImmediatelyInEnforce
    ) {
        if (!configManager.isResourceMonitorEnabled()) {
            return;
        }

        boolean auditMode = configManager.isResourceMonitorAuditMode();
        boolean warnMode = configManager.isResourceMonitorWarnMode();
        boolean enforceMode = configManager.isResourceMonitorEnforceMode();
        if (!auditMode && !warnMode && !enforceMode) {
            return;
        }

        if (claimProtectionService.wouldDisableResourceMonitor()) {
            return;
        }

        String bypassPermission = configManager.getBypassPermission();
        if (!bypassPermission.isBlank() && player.hasPermission(bypassPermission)) {
            return;
        }

        World world = block.getWorld();
        String worldName = world.getName();
        if (!configManager.isMonitoredWorld(worldName)
                || configManager.isIgnoredWorld(worldName)
                || !configManager.hasResourceWorldRule(worldName)) {
            return;
        }

        if (claimProtectionService.shouldSkipInsideClaims() && claimProtectionService.isInsideClaim(block.getLocation())) {
            return;
        }

        boolean warnActionsEnabled = warnMode || enforceMode;
        ViolationResult violationResult = violationService.registerViolation(
                player,
                block,
                match,
                warnActionsEnabled,
                enforceMode
        );

        if (auditMode) {
            if (shouldEmitAudit(player.getUniqueId(), match.material(), match.category())) {
                messageService.sendResourceAudit(player, block, match);
            }
            return;
        }

        if (!warnActionsEnabled) {
            return;
        }

        if (shouldEmitAudit(player.getUniqueId(), match.material(), match.category())) {
            messageService.logResourceAudit(player, block, match);
        }
        if (violationResult.shouldRun(ViolationAction.WARNING)) {
            messageService.sendViolationWarning(
                    player,
                    violationResult.snapshot(),
                    configManager.getViolationActionContent(ViolationAction.WARNING),
                    violationService.getWindowSeconds()
            );
        }

        if (violationResult.shouldRun(ViolationAction.NOTIFY_STAFF)) {
            messageService.sendViolationStaffNotification(
                    player,
                    violationResult.snapshot(),
                    configManager.getViolationActionContent(ViolationAction.NOTIFY_STAFF),
                    violationService.getWindowSeconds()
            );
        }

        if (enforceMode
                && configManager.isViolationActionEnabled(ViolationAction.CANCEL_BREAK)
                && (cancelImmediatelyInEnforce || shouldCancelBreak(violationResult))) {
            event.setCancelled(true);
            if (cancelImmediatelyInEnforce
                    ? shouldEmitProtectedLootCancelMessage(player.getUniqueId(), match.material(), match.category())
                    : violationResult.shouldRun(ViolationAction.CANCEL_BREAK)) {
                messageService.sendViolationCancelBreak(
                        player,
                        violationResult.snapshot(),
                        configManager.getViolationActionContent(ViolationAction.CANCEL_BREAK),
                        configManager.getViolationActionActionbarContent(ViolationAction.CANCEL_BREAK),
                        violationService.getWindowSeconds()
                );
            }

            ViolationResult blockedAttemptResult = violationService.registerBlockedAttempt(player, block, match);
            if (blockedAttemptResult.shouldRun(ViolationAction.JAIL)) {
                jailActionService.execute(player, blockedAttemptResult.snapshot());
            }
        }
    }

    private Player getResponsiblePlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }

        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }

    private boolean shouldProtectHangingItemFrame(ItemFrame itemFrame) {
        if (!shouldProtectExplosionResources()) {
            return false;
        }

        ItemStack itemStack = itemFrame.getItem();
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        Location location = itemFrame.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        String worldName = world.getName();
        if (!configManager.isMonitoredWorld(worldName)
                || configManager.isIgnoredWorld(worldName)
                || !configManager.hasResourceWorldRule(worldName)
                || resourceDetectionService.detectProtectedItem(world, itemStack.getType()).isEmpty()) {
            return false;
        }

        return !claimProtectionService.shouldSkipInsideClaims()
                || !claimProtectionService.isInsideClaim(location);
    }

    private void protectExplosionBlockList(List<Block> blocks) {
        if (!shouldProtectExplosionResources()) {
            return;
        }

        blocks.removeIf(this::shouldProtectExplosionBlock);
    }

    private boolean shouldProtectExplosionResources() {
        return configManager.isResourceMonitorEnabled()
                && configManager.isResourceMonitorEnforceMode()
                && configManager.isViolationActionEnabled(ViolationAction.CANCEL_BREAK)
                && !claimProtectionService.wouldDisableResourceMonitor();
    }

    private boolean shouldProtectExplosionBlock(Block block) {
        World world = block.getWorld();
        String worldName = world.getName();
        if (!configManager.isMonitoredWorld(worldName)
                || configManager.isIgnoredWorld(worldName)
                || !configManager.hasResourceWorldRule(worldName)) {
            return false;
        }

        if (resourceDetectionService.detect(world, block.getType()).isEmpty()) {
            return false;
        }

        return !claimProtectionService.shouldSkipInsideClaims()
                || !claimProtectionService.isInsideClaim(block.getLocation());
    }

    private boolean shouldEmitAudit(UUID playerId, Material material, String category) {
        return shouldEmitCooldown(lastAuditLogTimes, playerId, material, category, configManager.getAuditLogCooldownSeconds());
    }

    private boolean shouldEmitProtectedLootCancelMessage(UUID playerId, Material material, String category) {
        return shouldEmitCooldown(
                lastProtectedLootCancelMessageTimes,
                playerId,
                material,
                category,
                configManager.getViolationActionCooldownSeconds(ViolationAction.CANCEL_BREAK)
        );
    }

    private boolean shouldEmitCooldown(
            ConcurrentMap<AuditCooldownKey, Long> cooldowns,
            UUID playerId,
            Material material,
            String category,
            int cooldownSeconds
    ) {
        long cooldownMillis = cooldownSeconds * 1000L;
        if (cooldownMillis <= 0L) {
            return true;
        }

        long now = System.currentTimeMillis();
        AuditCooldownKey key = new AuditCooldownKey(playerId, material, category);
        AtomicBoolean allowed = new AtomicBoolean(false);
        cooldowns.compute(key, (ignored, lastLogTime) -> {
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
