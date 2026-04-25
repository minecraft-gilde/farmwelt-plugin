package de.minecraftgilde.farmwelt.command;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import de.minecraftgilde.farmwelt.model.ViolationAction;
import de.minecraftgilde.farmwelt.model.ViolationSnapshot;
import de.minecraftgilde.farmwelt.service.ClaimProtectionService;
import de.minecraftgilde.farmwelt.service.ViolationService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FarmweltCommand implements BasicCommand {

    private final FarmweltMenu farmweltMenu;
    private final ClaimProtectionService claimProtectionService;
    private final ViolationService violationService;
    private final ConfigManager configManager;

    public FarmweltCommand(
            FarmweltMenu farmweltMenu,
            ClaimProtectionService claimProtectionService,
            ViolationService violationService,
            ConfigManager configManager
    ) {
        this.farmweltMenu = farmweltMenu;
        this.claimProtectionService = claimProtectionService;
        this.violationService = violationService;
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden.");
            return;
        }

        if (args.length > 0) {
            handleSubCommand(player, args);
            return;
        }

        if (!player.hasPermission("farmwelt.use")) {
            player.sendMessage("Dafuer hast du keine Berechtigung.");
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
        if (args.length == 1) {
            return List.of("debug");
        }

        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return List.of("claim", "violations");
        }

        if (args.length == 3 && "debug".equalsIgnoreCase(args[0]) && "violations".equalsIgnoreCase(args[1])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return List.of();
    }

    private void handleSubCommand(Player player, String[] args) {
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

        player.sendMessage("Unbekannter Farmwelt-Befehl.");
    }

    private void handleClaimDebug(Player player) {
        if (!player.hasPermission("farmwelt.admin")) {
            player.sendMessage("Dafuer hast du keine Berechtigung.");
            return;
        }

        boolean claimProtectionActive = claimProtectionService.isAvailable();
        boolean insideClaim = claimProtectionService.isInsideClaim(player.getLocation());

        player.sendMessage("Claim-Provider: " + claimProtectionService.getProviderName());
        player.sendMessage("Claim-Schutz aktiv: " + yesNo(claimProtectionActive));
        player.sendMessage("Position liegt in Claim: " + yesNo(insideClaim));
    }

    private void handleViolationsDebug(Player player, String[] args) {
        if (!player.hasPermission("farmwelt.admin")) {
            player.sendMessage("Dafuer hast du keine Berechtigung.");
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
            player.sendMessage("Violation-Status fuer: " + target.getName());
        }

        player.sendMessage("Verstoesse im aktuellen Zeitfenster: " + snapshot.currentCount());
        player.sendMessage("Zeitfenster: " + violationService.getWindowSeconds() + " Sekunden");
        player.sendMessage("Verbleibende Zeit: " + violationService.getRemainingWindowSeconds(snapshot) + " Sekunden");
        sendViolationThresholds(player, snapshot.currentCount());
        player.sendMessage("Letzter Block: " + snapshot.latestBlock().name());
        player.sendMessage("Kategorie: " + snapshot.latestCategory());
        player.sendMessage("Letzte Position: " + snapshot.latestWorld()
                + " " + snapshot.latestX()
                + " " + snapshot.latestY()
                + " " + snapshot.latestZ());
    }

    private void sendEmptyViolationSnapshot(Player player, Player target) {
        if (!player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("Violation-Status fuer: " + target.getName());
        }

        player.sendMessage("Verstoesse im aktuellen Zeitfenster: 0");
        player.sendMessage("Zeitfenster: " + violationService.getWindowSeconds() + " Sekunden");
        sendViolationThresholds(player, 0);
        player.sendMessage("Es wurde keine aktuelle Ressource erfasst.");
    }

    private void sendViolationThresholds(Player player, int currentCount) {
        player.sendMessage("Warn-Schwelle: " + configManager.getViolationActionAfterBlocks(ViolationAction.WARNING));
        player.sendMessage("Staff-Schwelle: " + configManager.getViolationActionAfterBlocks(ViolationAction.NOTIFY_STAFF));
        player.sendMessage("Blockier-Schwelle: " + configManager.getViolationActionAfterBlocks(ViolationAction.CANCEL_BREAK));
        player.sendMessage("Blockieren aktiv: " + yesNo(isCancelBreakActive(currentCount)));
        player.sendMessage("Modus: " + configManager.getResourceMonitorMode());
    }

    private boolean isCancelBreakActive(int currentCount) {
        return configManager.isResourceMonitorEnforceMode()
                && configManager.isViolationActionEnabled(ViolationAction.CANCEL_BREAK)
                && currentCount >= configManager.getViolationActionAfterBlocks(ViolationAction.CANCEL_BREAK);
    }

    private String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }
}
