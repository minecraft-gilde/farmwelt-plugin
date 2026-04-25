package de.minecraftgilde.farmwelt.command;

import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FarmweltCommand implements BasicCommand {

    private final FarmweltMenu farmweltMenu;

    public FarmweltCommand(FarmweltMenu farmweltMenu) {
        this.farmweltMenu = farmweltMenu;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden.");
            return;
        }

        farmweltMenu.open(player);
    }

    @Override
    public @Nullable String permission() {
        return "farmwelt.use";
    }
}
