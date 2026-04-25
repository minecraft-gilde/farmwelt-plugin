package de.minecraftgilde.farmwelt.claim;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GriefPreventionClaimProtectionProvider implements ClaimProtectionProvider {

    private static final String PROVIDER_NAME = "GriefPrevention";

    private final JavaPlugin plugin;
    private final boolean ignoreHeight;
    private final Object dataStore;
    private final Method getClaimAtMethod;

    public GriefPreventionClaimProtectionProvider(JavaPlugin plugin, boolean ignoreHeight) {
        this.plugin = plugin;
        this.ignoreHeight = ignoreHeight;

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Plugin griefPrevention = pluginManager.getPlugin(PROVIDER_NAME);
        if (griefPrevention == null || !griefPrevention.isEnabled()) {
            dataStore = null;
            getClaimAtMethod = null;
            return;
        }

        Object resolvedDataStore = null;
        Method resolvedGetClaimAtMethod = null;
        try {
            Field dataStoreField = findDataStoreField(griefPrevention.getClass());
            resolvedDataStore = dataStoreField.get(griefPrevention);
            if (resolvedDataStore != null) {
                resolvedGetClaimAtMethod = findGetClaimAtMethod(resolvedDataStore.getClass());
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING, "GriefPrevention wurde gefunden, aber die Claim-API konnte nicht vorbereitet werden.", exception);
        }

        dataStore = resolvedDataStore;
        getClaimAtMethod = resolvedGetClaimAtMethod;
    }

    @Override
    public boolean isAvailable() {
        return dataStore != null && getClaimAtMethod != null;
    }

    @Override
    public boolean isInsideClaim(Location location) {
        if (!isAvailable()) {
            return false;
        }

        try {
            Object claim = getClaimAtMethod.invoke(dataStore, location, ignoreHeight, null);
            return claim != null;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING, "Fehler bei der GriefPrevention-Claim-Prüfung.", exception);
            return false;
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    private Method findGetClaimAtMethod(Class<?> dataStoreClass) throws NoSuchMethodException {
        for (Method method : dataStoreClass.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!method.getName().equals("getClaimAt") || parameterTypes.length != 3) {
                continue;
            }

            if (Location.class.isAssignableFrom(parameterTypes[0])
                    && (parameterTypes[1] == boolean.class || parameterTypes[1] == Boolean.class)) {
                return method;
            }
        }

        throw new NoSuchMethodException("getClaimAt(Location, boolean, Claim)");
    }

    private Field findDataStoreField(Class<?> griefPreventionClass) throws NoSuchFieldException {
        try {
            return griefPreventionClass.getField("dataStore");
        } catch (NoSuchFieldException exception) {
            Field field = griefPreventionClass.getDeclaredField("dataStore");
            field.setAccessible(true);
            return field;
        }
    }
}
