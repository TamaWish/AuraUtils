package com.lozaine.aurautils.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses {@code permissions.yml} default values ({@code true} / {@code false} / {@code op}).
 *
 * <p>Bukkit’s default path separator is {@code .}, which would collapse
 * {@code aura.warp} and {@code aura.warp.set}. Files are loaded with a
 * non-dot separator so each node name stays a single key.
 */
public final class PermissionDefaults {

    public static final String FILE_NAME = "permissions.yml";
    public static final String SECTION = "defaults";
    /** Must not appear in permission node names. */
    static final char PATH_SEPARATOR = '\u001f';

    private PermissionDefaults() {}

    public static YamlConfiguration loadLiteral(File file) {
        YamlConfiguration yaml = emptyLiteral();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException ignored) {
            return emptyLiteral();
        }
        return yaml;
    }

    public static YamlConfiguration loadLiteral(Reader reader) {
        YamlConfiguration yaml = emptyLiteral();
        try {
            yaml.load(reader);
        } catch (IOException | InvalidConfigurationException ignored) {
            return emptyLiteral();
        }
        return yaml;
    }

    static YamlConfiguration emptyLiteral() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator(PATH_SEPARATOR);
        return yaml;
    }

    /**
     * @return {@code true}/{@code false}/{@code op}/{@code not op}, or {@code null} if the value is not recognized
     */
    public static PermissionDefault parse(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool ? PermissionDefault.TRUE : PermissionDefault.FALSE;
        }
        String value = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        return switch (value) {
            case "true", "yes", "everyone", "all" -> PermissionDefault.TRUE;
            case "false", "no", "nobody", "none" -> PermissionDefault.FALSE;
            case "op", "ops", "operator", "operators" -> PermissionDefault.OP;
            case "not op", "not_op", "not-op", "player", "players" -> PermissionDefault.NOT_OP;
            default -> null;
        };
    }

    /**
     * Resolves defaults for known plugin nodes. Disk values win; missing or invalid
     * disk values fall back to {@code jarDefaults}. Unknown disk keys are skipped.
     */
    public static Map<String, PermissionDefault> resolve(
            ConfigurationSection diskDefaults,
            Set<String> knownNodes,
            ConfigurationSection jarDefaults
    ) {
        Map<String, PermissionDefault> resolved = new LinkedHashMap<>();
        if (knownNodes == null) {
            return resolved;
        }
        for (String node : knownNodes) {
            if (node == null || node.isBlank()) {
                continue;
            }
            PermissionDefault value = parse(valueOf(diskDefaults, node));
            if (value == null) {
                value = parse(valueOf(jarDefaults, node));
            }
            if (value != null) {
                resolved.put(node, value);
            }
        }
        return resolved;
    }

    public static Set<String> unknownKeys(ConfigurationSection diskDefaults, Set<String> knownNodes) {
        Set<String> unknown = new LinkedHashSet<>();
        for (String key : leafKeys(diskDefaults)) {
            if (knownNodes == null || !knownNodes.contains(key)) {
                unknown.add(key);
            }
        }
        return unknown;
    }

    public static Set<String> leafKeys(ConfigurationSection section) {
        if (section == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(section.getKeys(false));
    }

    private static Object valueOf(ConfigurationSection section, String node) {
        if (section == null || !section.contains(node)) {
            return null;
        }
        return section.get(node);
    }
}
