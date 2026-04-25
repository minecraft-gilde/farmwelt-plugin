package de.minecraftgilde.farmwelt.command;

import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import de.minecraftgilde.farmwelt.service.ClaimProtectionService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FarmweltCommand implements BasicCommand {

    private final FarmweltMenu farmweltMenu;
    private final ClaimProtectionService claimProtectionService;

    public FarmweltCommand(FarmweltMenu farmweltMenu, ClaimProtectionService claimProtectionService) {
        this.farmweltMenu = farmweltMenu;
        this.claimProtectionService = claimProtectionService;
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
            return List.of("claim");
        }

        return List.of();
    }

    private void handleSubCommand(Player player, String[] args) {
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0]) && "claim".equalsIgnoreCase(args[1])) {
            handleClaimDebug(player);
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

    private String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }
}
