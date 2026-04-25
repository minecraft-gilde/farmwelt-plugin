package de.minecraftgilde.farmwelt.service;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.model.ViolationSnapshot;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class JailActionService {

    private static final String MODE_DISABLED = "disabled";
    private static final String MODE_NOTIFY_ONLY = "notify-only";
    private static final String MODE_EXECUTE_COMMAND = "execute-command";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageService messageService;

    public JailActionService(JavaPlugin plugin, ConfigManager configManager, MessageService messageService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageService = messageService;
    }

    public void execute(Player player, ViolationSnapshot snapshot) {
        if (!configManager.isJailActionEnabled()) {
            return;
        }

        String mode = configManager.getJailMode().toLowerCase(Locale.ROOT);
        if (MODE_DISABLED.equals(mode)) {
            return;
        }

        if (configManager.isJailNotifyStaff()) {
            messageService.sendJailStaffNotification(
                    player,
                    snapshot,
                    configManager.getJailStaffMessage(),
                    configManager.getViolationWindowSeconds()
            );
        }

        if (MODE_NOTIFY_ONLY.equals(mode)) {
            return;
        }

        if (!MODE_EXECUTE_COMMAND.equals(mode)) {
            plugin.getLogger().warning("Unbekannter Jail-Modus beim Ausführen: " + configManager.getJailMode());
            return;
        }

        String command = normalizeCommand(messageService.replaceViolationPlaceholders(
                configManager.getJailCommand(),
                player,
                snapshot,
                configManager.getViolationWindowSeconds()
        ));
        if (command.isBlank()) {
            plugin.getLogger().warning("Jail-Befehl ist nach Platzhalter-Ersetzung leer.");
            return;
        }

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> dispatchConsoleCommand(player, snapshot, command));
    }

    private void dispatchConsoleCommand(Player player, ViolationSnapshot snapshot, String command) {
        try {
            Server server = plugin.getServer();
            boolean executed = server.dispatchCommand(server.getConsoleSender(), command);
            if (!executed) {
                plugin.getLogger().warning("Jail-Befehl konnte nicht ausgeführt werden: " + command);
                return;
            }

            boolean scheduled = player.getScheduler().execute(plugin, () -> messageService.sendJailPlayerMessage(
                    player,
                    snapshot,
                    configManager.getJailPlayerMessage(),
                    configManager.getViolationWindowSeconds()
            ), null, 1L);
            if (!scheduled) {
                plugin.getLogger().warning("Jail-Spielernachricht konnte nicht im Spieler-Kontext geplant werden.");
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Ausführen des Jail-Befehls: " + command, exception);
        }
    }

    private String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }

        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }
}
