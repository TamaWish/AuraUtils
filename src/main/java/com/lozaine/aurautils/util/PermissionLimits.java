package com.lozaine.aurautils.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Resolves a numeric cap from {@code defaultLimit} plus permission-keyed
 * {@code max} entries. {@code 0} means unlimited.
 *
 * <p>Matching {@code max: 0} is unlimited immediately. Otherwise the highest
 * matching positive max wins against {@code defaultLimit}. If the default is
 * unlimited ({@code 0}), a matching positive rank limit will cap that player
 * — set a positive default before adding VIP caps.
 */
public final class PermissionLimits {

    private PermissionLimits() {
    }

    /**
     * Reads {@code path} as a list of {@code permission}/{@code max} maps, a
     * single such mapping (forgotten {@code - } dashes), or {@code node: max}.
     */
    public static List<Map<?, ?>> readEntries(ConfigurationSection config, String path) {
        if (config == null || path == null || path.isBlank()) {
            return List.of();
        }
        List<?> list = config.getList(path);
        if (list != null) {
            List<Map<?, ?>> entries = new ArrayList<>();
            for (Object object : list) {
                Map<?, ?> map = asEntryMap(object);
                if (map != null && !map.isEmpty()) {
                    entries.add(map);
                }
            }
            if (!entries.isEmpty()) {
                return List.copyOf(entries);
            }
        }
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null) {
            return fromMapping(section.getValues(false));
        }
        Object raw = config.get(path);
        if (raw instanceof Map<?, ?> map && !map.isEmpty()) {
            return fromMapping(map);
        }
        return List.of();
    }

    public static int resolve(int defaultLimit, List<Map<?, ?>> entries, Predicate<String> hasPermission) {
        int limit = Math.max(0, defaultLimit);
        if (entries == null || hasPermission == null) {
            return limit;
        }
        for (Map<?, ?> entry : entries) {
            if (entry == null) {
                continue;
            }
            Object permission = entry.get("permission");
            Object maximum = entry.get("max");
            if (!(permission instanceof String node) || node.isBlank() || maximum == null
                    || !hasPermission.test(node)) {
                continue;
            }
            int value;
            if (maximum instanceof Number number) {
                value = number.intValue();
            } else {
                try {
                    value = Integer.parseInt(String.valueOf(maximum).trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            if (value <= 0) {
                return 0;
            }
            limit = Math.max(limit, value);
        }
        return limit;
    }

    private static List<Map<?, ?>> fromMapping(Map<?, ?> values) {
        Object permission = values.get("permission");
        Object maximum = values.get("max");
        if (permission instanceof String node && !node.isBlank() && maximum != null
                && isSinglePermissionMaxEntry(values)) {
            return List.of(Map.of("permission", node, "max", maximum));
        }
        List<Map<?, ?>> entries = new ArrayList<>();
        collectNodeMax(values, "", entries);
        return List.copyOf(entries);
    }

    /**
     * Bukkit treats {@code .} as a path separator, so {@code myserver.vip: 3}
     * loads as nested {@code myserver → vip}. Flatten it back to the node name.
     */
    private static void collectNodeMax(Map<?, ?> values, String prefix, List<Map<?, ?>> entries) {
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String key = String.valueOf(entry.getKey()).trim();
            if (key.isEmpty() || key.equals("permission") || key.equals("max")) {
                continue;
            }
            String node = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof ConfigurationSection section) {
                collectNodeMax(section.getValues(false), node, entries);
            } else if (value instanceof Map<?, ?> nested) {
                collectNodeMax(nested, node, entries);
            } else {
                entries.add(Map.of("permission", node, "max", value));
            }
        }
    }

    private static boolean isSinglePermissionMaxEntry(Map<?, ?> values) {
        for (Object key : values.keySet()) {
            if (key == null) {
                continue;
            }
            String name = String.valueOf(key);
            if (!name.equals("permission") && !name.equals("max")) {
                return false;
            }
        }
        return true;
    }

    private static Map<?, ?> asEntryMap(Object object) {
        if (object instanceof Map<?, ?> map) {
            return map;
        }
        if (object instanceof ConfigurationSection section) {
            return section.getValues(false);
        }
        return null;
    }
}
