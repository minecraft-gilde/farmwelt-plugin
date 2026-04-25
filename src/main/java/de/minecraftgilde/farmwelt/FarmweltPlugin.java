package de.minecraftgilde.farmwelt;

import de.minecraftgilde.farmwelt.command.FarmweltCommand;
import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.gui.FarmweltMenu;
import de.minecraftgilde.farmwelt.listener.FarmweltGuiListener;
import de.minecraftgilde.farmwelt.listener.ResourceBreakListener;
import de.minecraftgilde.farmwelt.service.ClaimProtectionService;
import de.minecraftgilde.farmwelt.service.FarmweltTeleportService;
import de.minecraftgilde.farmwelt.service.MessageService;
import de.minecraftgilde.farmwelt.service.ResourceDetectionService;
import org.bukkit.plugin.java.JavaPlugin;

public final class FarmweltPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private FarmweltMenu farmweltMenu;
    private FarmweltTeleportService teleportService;
    private ClaimProtectionService claimProtectionService;
    private ResourceDetectionService resourceDetectionService;
    private MessageService messageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadFarmweltMenuItems();
        configManager.loadResourceMonitorConfig();

        farmweltMenu = new FarmweltMenu(configManager);
        teleportService = new FarmweltTeleportService(this);
        claimProtectionService = new ClaimProtectionService(this);
        resourceDetectionService = new ResourceDetectionService(configManager);
        messageService = new MessageService(this, configManager);
        registerCommand();
        getServer().getPluginManager().registerEvents(new FarmweltGuiListener(teleportService), this);
        getServer().getPluginManager().registerEvents(
                new ResourceBreakListener(configManager, claimProtectionService, resourceDetectionService, messageService),
                this
        );

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
                new FarmweltCommand(farmweltMenu, claimProtectionService)
        );
    }
}
