package de.minecraftgilde.farmwelt.service;

import de.minecraftgilde.farmwelt.config.ConfigManager;
import de.minecraftgilde.farmwelt.model.ResourceMatch;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public MessageService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void sendResourceAudit(Player player, Block block, ResourceMatch match) {
        if (configManager.isAuditLogToConsole()) {
            plugin.getLogger().info(createConsoleAuditMessage(player, block, match));
        }

        if (!configManager.isAuditNotifyStaff()) {
            return;
        }

        Server server = plugin.getServer();
        String notifyPermission = configManager.getNotifyPermission();
        String message = replaceAuditPlaceholders(configManager.getStaffMessage(), player, block, match);
        for (Player onlinePlayer : server.getOnlinePlayers()) {
            if (notifyPermission.isBlank() || onlinePlayer.hasPermission(notifyPermission)) {
                onlinePlayer.sendMessage(LEGACY_AMPERSAND.deserialize(message));
            }
        }
    }

    private String createConsoleAuditMessage(Player player, Block block, ResourceMatch match) {
        return "[Audit] Spieler " + player.getName()
                + " (" + player.getUniqueId() + ") hat "
                + match.material().name()
                + " in " + block.getWorld().getName()
                + " bei x=" + block.getX()
                + " y=" + block.getY()
                + " z=" + block.getZ()
                + " abgebaut. Kategorie: " + match.category();
    }

    private String replaceAuditPlaceholders(String message, Player player, Block block, ResourceMatch match) {
        return message
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{world}", block.getWorld().getName())
                .replace("{x}", Integer.toString(block.getX()))
                .replace("{y}", Integer.toString(block.getY()))
                .replace("{z}", Integer.toString(block.getZ()))
                .replace("{block}", match.material().name())
                .replace("{category}", match.category());
    }
}
