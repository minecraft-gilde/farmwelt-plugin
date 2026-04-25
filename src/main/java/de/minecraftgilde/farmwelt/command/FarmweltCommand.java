package de.minecraftgilde.farmwelt.command;

import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class FarmweltCommand implements CommandExecutor {

    private final FarmweltMenu farmweltMenu;

    public FarmweltCommand(FarmweltMenu farmweltMenu) {
        this.farmweltMenu = farmweltMenu;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgefuehrt werden.");
            return true;
        }

        if (!player.hasPermission("farmwelt.use")) {
            player.sendMessage("Du hast keine Berechtigung, die Farmwelt-GUI zu oeffnen.");
            return true;
        }

        farmweltMenu.open(player);
        return true;
    }
}
