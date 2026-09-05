package com.lozaine.aurautils.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionDefaultsTest {

    @Test
    void parseAcceptsTrueFalseOpAndAliases() {
        assertEquals(PermissionDefault.TRUE, PermissionDefaults.parse(true));
        assertEquals(PermissionDefault.FALSE, PermissionDefaults.parse(false));
        assertEquals(PermissionDefault.TRUE, PermissionDefaults.parse("true"));
        assertEquals(PermissionDefault.FALSE, PermissionDefaults.parse("NO"));
        assertEquals(PermissionDefault.OP, PermissionDefaults.parse("op"));
        assertEquals(PermissionDefault.OP, PermissionDefaults.parse("operators"));
        assertEquals(PermissionDefault.NOT_OP, PermissionDefaults.parse("not op"));
        assertEquals(PermissionDefault.NOT_OP, PermissionDefaults.parse("player"));
        assertNull(PermissionDefaults.parse("maybe"));
        assertNull(PermissionDefaults.parse(""));
        assertNull(PermissionDefaults.parse(null));
    }

    @Test
    void resolvePrefersDiskAndFallsBackToJar() {
        YamlConfiguration disk = new YamlConfiguration();
        disk.set("aura.fly", "true");
        disk.set("aura.god", "nope");

        YamlConfiguration jar = new YamlConfiguration();
        jar.set("aura.fly", "op");
        jar.set("aura.god", "op");
        jar.set("aura.rtp", true);

        Map<String, PermissionDefault> resolved = PermissionDefaults.resolve(
                disk, Set.of("aura.fly", "aura.god", "aura.rtp"), jar);

        assertEquals(PermissionDefault.TRUE, resolved.get("aura.fly"));
        assertEquals(PermissionDefault.OP, resolved.get("aura.god"));
        assertEquals(PermissionDefault.TRUE, resolved.get("aura.rtp"));
    }

    @Test
    void unknownDiskKeysAreReported() {
        YamlConfiguration disk = PermissionDefaults.emptyLiteral();
        disk.set("aura.fly", "op");
        disk.set("aura.secret", true);
        Set<String> unknown = PermissionDefaults.unknownKeys(disk, Set.of("aura.fly"));
        assertEquals(Set.of("aura.secret"), unknown);
    }

    @Test
    void parentAndChildNodesDoNotCollide() {
        YamlConfiguration yaml = PermissionDefaults.loadLiteral(new java.io.StringReader("""
                defaults:
                  aura.warp: true
                  aura.warp.set: op
                  aura.fly: op
                """));
        ConfigurationSection defaults = yaml.getConfigurationSection("defaults");
        assertEquals(Boolean.TRUE, defaults.get("aura.warp"));
        assertEquals("op", defaults.get("aura.warp.set"));
        assertEquals("op", defaults.get("aura.fly"));

        Map<String, PermissionDefault> resolved = PermissionDefaults.resolve(
                defaults, Set.of("aura.warp", "aura.warp.set", "aura.fly"), null);
        assertEquals(PermissionDefault.TRUE, resolved.get("aura.warp"));
        assertEquals(PermissionDefault.OP, resolved.get("aura.warp.set"));
        assertEquals(PermissionDefault.OP, resolved.get("aura.fly"));
    }

    @Test
    void shippedPermissionsYmlCoversEveryPluginYmlNode() {
        YamlConfiguration pluginYml = load("plugin.yml");
        YamlConfiguration permissionsYml = PermissionDefaults.loadLiteral(
                new InputStreamReader(requireStream(PermissionDefaults.FILE_NAME), StandardCharsets.UTF_8));

        Set<String> pluginNodes = permissionNodesFromPluginYml(pluginYml);
        ConfigurationSection fileDefaults = permissionsYml.getConfigurationSection(PermissionDefaults.SECTION);
        Set<String> fileNodes = PermissionDefaults.leafKeys(fileDefaults);

        assertEquals(pluginNodes, fileNodes, "permissions.yml defaults must list every plugin.yml permission");
        assertEquals("op", fileDefaults.getString("aura.fly"));
        assertEquals(Boolean.TRUE, fileDefaults.get("aura.timber"));
        assertTrue(pluginNodes.contains("aura.admin"));
    }

    private static Set<String> permissionNodesFromPluginYml(YamlConfiguration pluginYml) {
        ConfigurationSection permissions = pluginYml.getConfigurationSection("permissions");
        Set<String> nodes = new java.util.LinkedHashSet<>();
        if (permissions == null) {
            return nodes;
        }
        for (String key : permissions.getKeys(true)) {
            if (key.endsWith(".default") && !key.contains(".children.")) {
                nodes.add(key.substring(0, key.length() - ".default".length()));
            }
        }
        return nodes;
    }

    private static InputStream requireStream(String resource) {
        InputStream stream = PermissionDefaults.class.getClassLoader().getResourceAsStream(resource);
        assertTrue(stream != null, resource + " must be on the test classpath");
        return stream;
    }

    private static YamlConfiguration load(String resource) {
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(requireStream(resource), StandardCharsets.UTF_8));
    }
}
