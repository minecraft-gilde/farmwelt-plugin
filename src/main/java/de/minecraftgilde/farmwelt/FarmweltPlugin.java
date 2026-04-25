package de.minecraftgilde.farmwelt;

import org.bukkit.plugin.java.JavaPlugin;

public final class FarmweltPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Farmwelt wurde gestartet.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Farmwelt wurde gestoppt.");
    }
}
