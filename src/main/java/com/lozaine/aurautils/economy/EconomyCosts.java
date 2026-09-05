package com.lozaine.aurautils.economy;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Predicate;

/**
 * Reads {@code economy.*} from config without touching Vault.
 */
public final class EconomyCosts {

    public static final String BYPASS_PERMISSION = "aura.economy.bypass";

    private EconomyCosts() {
    }

    public static boolean enabled(FileConfiguration config) {
        return config != null && config.getBoolean("economy.enabled", true);
    }

    public static boolean notify(FileConfiguration config) {
        return config == null || config.getBoolean("economy.notify", true);
    }

    /**
     * Configured price for {@code action}. {@code 0} when economy is disabled,
     * the action is missing, or the value is not positive.
     */
    public static double cost(FileConfiguration config, EconomyAction action) {
        if (!enabled(config) || action == null) {
            return 0;
        }
        double value = config.getDouble("economy.costs." + action.configKey(), 0);
        return value > 0 ? value : 0;
    }

    public static boolean isFree(FileConfiguration config, EconomyAction action, Predicate<String> hasPermission) {
        if (cost(config, action) <= 0) {
            return true;
        }
        return hasPermission != null && hasPermission.test(BYPASS_PERMISSION);
    }
}
