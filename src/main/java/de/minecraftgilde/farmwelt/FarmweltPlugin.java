package de.minecraftgilde.farmwelt;

import de.minecraftgilde.farmwelt.command.FarmweltCommand;
import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import de.minecraftgilde.farmwelt.listener.FarmweltGuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class FarmweltPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private FarmweltMenu farmweltMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadFarmweltMenuItems();

        farmweltMenu = new FarmweltMenu(configManager);
        registerCommand();
        getServer().getPluginManager().registerEvents(new FarmweltGuiListener(), this);

        getLogger().info("Farmwelt wurde gestartet.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Farmwelt wurde gestoppt.");
    }

    private void registerCommand() {
        registerCommand(
                "farmwelt",
                "Öffnet die Farmwelt-Auswahl.",
                new FarmweltCommand(farmweltMenu)
        );
    }
}
