package de.minecraftgilde.farmwelt.command;

import de.minecraftgilde.farmwelt.FarmweltPlugin;
import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import de.minecraftgilde.farmwelt.model.ViolationAction;
import de.minecraftgilde.farmwelt.model.ViolationSnapshot;
import de.minecraftgilde.farmwelt.service.ClaimProtectionService;
import de.minecraftgilde.farmwelt.service.ViolationService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FarmweltCommand implements BasicCommand {

    private static final String USE_PERMISSION = "farmwelt.use";
    private static final String ADMIN_PERMISSION = "farmwelt.admin";

    private final FarmweltPlugin plugin;
    private final FarmweltMenu farmweltMenu;
    private final ClaimProtectionService claimProtectionService;
    private final ViolationService violationService;
    private final ConfigManager configManager;

    public FarmweltCommand(
            FarmweltPlugin plugin,
            FarmweltMenu farmweltMenu,
            ClaimProtectionService claimProtectionService,
            ViolationService violationService,
            ConfigManager configManager
    ) {
        this.plugin = plugin;
        this.farmweltMenu = farmweltMenu;
        this.claimProtectionService = claimProtectionService;
        this.violationService = violationService;
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length > 0) {
            handleSubCommand(sender, args);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden.");
            return;
        }

        if (!player.hasPermission(USE_PERMISSION)) {
            player.sendMessage("Dafür hast du keine Berechtigung.");
            return;
        }

        farmweltMenu.open(player);
    }

    @Override
    public @Nullable String permission() {
        return null;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 1) {
            if (!canUseAdminCommand(sender)) {
                return List.of();
            }

            List<String> suggestions = new ArrayList<>(List.of("reload", "info"));
            if (sender instanceof Player) {
                suggestions.add("debug");
            }
            return suggestions;
        }

        if (args.length == 2
                && "debug".equalsIgnoreCase(args[0])
                && sender instanceof Player
                && canUseAdminCommand(sender)) {
            return List.of("claim", "violations");
        }

        if (args.length == 3
                && "debug".equalsIgnoreCase(args[0])
                && "violations".equalsIgnoreCase(args[1])
                && sender instanceof Player
                && canUseAdminCommand(sender)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return List.of();
    }

    private void handleSubCommand(CommandSender sender, String[] args) {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            handleReload(sender);
            return;
        }

        if (args.length == 1 && "info".equalsIgnoreCase(args[0])) {
            handleInfo(sender);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden.");
            return;
        }

        if (args.length == 2 && "debug".equalsIgnoreCase(args[0]) && "claim".equalsIgnoreCase(args[1])) {
            handleClaimDebug(player);
            return;
        }

        if ((args.length == 2 || args.length == 3)
                && "debug".equalsIgnoreCase(args[0])
                && "violations".equalsIgnoreCase(args[1])) {
            handleViolationsDebug(player, args);
            return;
        }

        sender.sendMessage("Unbekannter Farmwelt-Befehl.");
    }

    private void handleReload(CommandSender sender) {
        if (!canUseAdminCommand(sender)) {
            sender.sendMessage("Dafür hast du keine Berechtigung.");
            return;
        }

        plugin.reloadFarmweltConfiguration();
        sender.sendMessage("Farmwelt-Konfiguration wurde neu geladen.");
    }

    private void handleInfo(CommandSender sender) {
        if (!canUseAdminCommand(sender)) {
            sender.sendMessage("Dafür hast du keine Berechtigung.");
            return;
        }

        sender.sendMessage("Farmwelt " + plugin.getPluginMeta().getVersion());
        sender.sendMessage("Geladene Farmwelten: " + configManager.getFarmweltMenuItems().size());
        sender.sendMessage("Ressourcenmonitor: " + yesNo(configManager.isResourceMonitorEnabled())
                + " (Modus: " + configManager.getResourceMonitorMode() + ")");
        sender.sendMessage("Claim-Provider: " + claimProtectionService.getProviderName());
        sender.sendMessage("Claim-Hook aktiv: " + yesNo(claimProtectionService.isAvailable()));
        sender.sendMessage("BetterRTP aktiv: " + yesNo(isPluginEnabled("BetterRTP")));
        sender.sendMessage("GriefPrevention aktiv: " + yesNo(isPluginEnabled("GriefPrevention")));
        sender.sendMessage("Jail-Modus: " + configManager.getJailMode());
    }

    private void handleClaimDebug(Player player) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage("Dafür hast du keine Berechtigung.");
            return;
        }

        boolean claimProtectionActive = claimProtectionService.isAvailable();
        boolean insideClaim = claimProtectionService.isInsideClaim(player.getLocation());

        player.sendMessage("Claim-Provider: " + claimProtectionService.getProviderName());
        player.sendMessage("Claim-Schutz aktiv: " + yesNo(claimProtectionActive));
        player.sendMessage("Position liegt in Claim: " + yesNo(insideClaim));
    }

    private void handleViolationsDebug(Player player, String[] args) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage("Dafür hast du keine Berechtigung.");
            return;
        }

        Player target = player;
        if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                player.sendMessage("Spieler ist nicht online: " + args[2]);
                return;
            }
        }

        Player finalTarget = target;
        violationService.getSnapshot(finalTarget.getUniqueId()).ifPresentOrElse(
                snapshot -> sendViolationSnapshot(player, finalTarget, snapshot),
                () -> sendEmptyViolationSnapshot(player, finalTarget)
        );
    }

    private void sendViolationSnapshot(Player player, Player target, ViolationSnapshot snapshot) {
        if (!player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("Violation-Status für: " + target.getName());
        }

        player.sendMessage("Verstöße im aktuellen Zeitfenster: " + snapshot.currentCount());
        player.sendMessage("Blockierte Versuche: " + snapshot.blockedCount());
        player.sendMessage("Zeitfenster: " + violationService.getWindowSeconds() + " Sekunden");
        player.sendMessage("Verbleibende Zeit: " + violationService.getRemainingWindowSeconds(snapshot) + " Sekunden");
        sendViolationThresholds(player, snapshot.currentCount());
        sendJailStatus(player, snapshot.jailActionExecutedInCurrentWindow());
        player.sendMessage("Letzter Block: " + snapshot.latestBlock().name());
        player.sendMessage("Kategorie: " + snapshot.latestCategory());
        player.sendMessage("Letzte Position: " + snapshot.latestWorld()
                + " " + snapshot.latestX()
                + " " + snapshot.latestY()
                + " " + snapshot.latestZ());
    }

    private void sendEmptyViolationSnapshot(Player player, Player target) {
        if (!player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("Violation-Status für: " + target.getName());
        }

        player.sendMessage("Verstöße im aktuellen Zeitfenster: 0");
        player.sendMessage("Blockierte Versuche: 0");
        player.sendMessage("Zeitfenster: " + violationService.getWindowSeconds() + " Sekunden");
        sendViolationThresholds(player, 0);
        sendJailStatus(player, false);
        player.sendMessage("Es wurde keine aktuelle Ressource erfasst.");
    }

    private void sendViolationThresholds(Player player, int currentCount) {
        player.sendMessage("Warn-Schwelle: " + configManager.getViolationActionAfterBlocks(ViolationAction.WARNING));
        player.sendMessage("Staff-Schwelle: " + configManager.getViolationActionAfterBlocks(ViolationAction.NOTIFY_STAFF));
        player.sendMessage("Blockier-Schwelle: " + configManager.getViolationActionAfterBlocks(ViolationAction.CANCEL_BREAK));
        player.sendMessage("Blockieren aktiv: " + yesNo(isCancelBreakActive(currentCount)));
        player.sendMessage("Modus: " + configManager.getResourceMonitorMode());
    }

    private void sendJailStatus(Player player, boolean jailActionExecutedInCurrentWindow) {
        player.sendMessage("Jail-Modus: " + configManager.getJailMode());
        player.sendMessage("Jail-Schwelle: " + configManager.getJailAfterBlockedAttempts());
        player.sendMessage("Jail-Cooldown: " + configManager.getJailCooldownMinutes() + " Minuten");
        player.sendMessage("Jail-Aktion in diesem Fenster bereits ausgelöst: " + yesNo(jailActionExecutedInCurrentWindow));
    }

    private boolean isCancelBreakActive(int currentCount) {
        return configManager.isResourceMonitorEnforceMode()
                && configManager.isViolationActionEnabled(ViolationAction.CANCEL_BREAK)
                && currentCount >= configManager.getViolationActionAfterBlocks(ViolationAction.CANCEL_BREAK);
    }

    private boolean canUseAdminCommand(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(ADMIN_PERMISSION);
    }

    private boolean isPluginEnabled(String pluginName) {
        return plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    private String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }
}
