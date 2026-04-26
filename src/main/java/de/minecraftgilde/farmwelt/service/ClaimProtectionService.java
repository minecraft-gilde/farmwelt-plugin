package de.minecraftgilde.farmwelt.service;

import de.minecraftgilde.farmwelt.claim.ClaimProtectionProvider;
import de.minecraftgilde.farmwelt.claim.GriefPreventionClaimProtectionProvider;
import de.minecraftgilde.farmwelt.claim.NoopClaimProtectionProvider;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClaimProtectionService {

    private static final String PROVIDER_GRIEF_PREVENTION = "GriefPrevention";
    private static final String FAIL_MODE_DISABLE_MONITOR = "disable-monitor";

    private final JavaPlugin plugin;
    private boolean enabled;
    private boolean skipInsideClaims;
    private String configuredProviderName;
    private String failMode;
    private ClaimProtectionProvider provider;
    private boolean resourceMonitorWouldBeDisabled;

    public ClaimProtectionService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("resource-monitor.claim-protection");
        enabled = section != null && section.getBoolean("enabled", false);
        skipInsideClaims = section == null || section.getBoolean("skip-inside-claims", true);
        configuredProviderName = section == null ? PROVIDER_GRIEF_PREVENTION : section.getString("provider", PROVIDER_GRIEF_PREVENTION);
        failMode = section == null ? FAIL_MODE_DISABLE_MONITOR : section.getString("fail-mode", FAIL_MODE_DISABLE_MONITOR);
        boolean ignoreHeight = section == null || section.getBoolean("ignore-height", true);

        provider = createProvider(ignoreHeight);
        resourceMonitorWouldBeDisabled = enabled
                && !provider.isAvailable()
                && FAIL_MODE_DISABLE_MONITOR.equalsIgnoreCase(failMode);

        logStartupState();
    }

    public boolean isAvailable() {
        return enabled && provider.isAvailable();
    }

    public boolean isInsideClaim(Location location) {
        if (!isAvailable() || location == null) {
            return false;
        }

        try {
            return provider.isInsideClaim(location);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Fehler bei der Claim-Prüfung.", exception);
            return false;
        }
    }

    public boolean shouldSkipInsideClaims() {
        return enabled && skipInsideClaims;
    }

    public String getProviderName() {
        return provider.getName();
    }

    public boolean wouldDisableResourceMonitor() {
        return resourceMonitorWouldBeDisabled;
    }

    private ClaimProtectionProvider createProvider(boolean ignoreHeight) {
        if (!enabled) {
            return new NoopClaimProtectionProvider();
        }

        if (!PROVIDER_GRIEF_PREVENTION.equalsIgnoreCase(configuredProviderName)) {
            plugin.getLogger().warning("Unbekannter Claim-Provider konfiguriert: " + configuredProviderName);
            return new NoopClaimProtectionProvider();
        }

        GriefPreventionClaimProtectionProvider griefPreventionProvider =
                new GriefPreventionClaimProtectionProvider(plugin, ignoreHeight);
        if (!griefPreventionProvider.isAvailable()) {
            plugin.getLogger().warning("Claim-Schutz ist aktiviert, aber GriefPrevention ist nicht verfügbar.");
            return new NoopClaimProtectionProvider();
        }

        return griefPreventionProvider;
    }

    private void logStartupState() {
        boolean griefPreventionFound = plugin.getServer().getPluginManager().getPlugin(PROVIDER_GRIEF_PREVENTION) != null;
        plugin.getLogger().info("Claim-Schutz aktiviert: " + yesNo(enabled));
        plugin.getLogger().info("Konfigurierter Claim-Provider: " + configuredProviderName);
        plugin.getLogger().info("GriefPrevention gefunden: " + yesNo(griefPreventionFound));
        plugin.getLogger().info("Claim-Hook aktiv: " + yesNo(isAvailable()));
        plugin.getLogger().info("Claims werden vom Ressourcenmonitor übersprungen: " + yesNo(enabled && skipInsideClaims));

        if (resourceMonitorWouldBeDisabled) {
            plugin.getLogger().warning("Der Ressourcenmonitor wird wegen fehlendem Claim-Provider deaktiviert.");
        }
    }

    private String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }
}
