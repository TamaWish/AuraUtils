package com.lozaine.aurautils.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionLimitsTest {

    @Test
    void shippedInventoryLimitsAreEmptyUntilUncommented() {
        var stream = PermissionLimitsTest.class.getClassLoader().getResourceAsStream("config.yml");
        assert stream != null;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertTrue(PermissionLimits.readEntries(yaml, "inventories.limits").isEmpty());
        assertEquals(1, InventoryLimits.resolve(
                yaml.getInt("inventories.default-limit"),
                yaml.getInt("inventories.max"),
                PermissionLimits.readEntries(yaml, "inventories.limits"),
                Set.of("myserver.vip")::contains));
    }

    @Test
    void wildcardGrantsEveryInventoryOnceNumberedNodesAreRegistered() {
        // A permission plugin expands aura.inv.* only over registered nodes,
        // so this mirrors what it reports after registerNumberedPermissions().
        Set<String> expanded = new java.util.HashSet<>();
        for (int number = 1; number <= 10; number++) {
            expanded.add(InventoryLimits.numberedNode(number));
        }
        assertEquals(10, InventoryLimits.resolve(1, 10, List.of(), expanded::contains));
    }

    @Test
    void listOfPermissionMaxMaps() {
        YamlConfiguration yaml = yaml("""
                inventories:
                  limits:
                    - permission: myserver.vip
                      max: 3
                    - permission: myserver.mvp
                      max: 5
                """);
        List<Map<?, ?>> entries = PermissionLimits.readEntries(yaml, "inventories.limits");
        assertEquals(2, entries.size());
        assertEquals(3, PermissionLimits.resolve(1, entries, Set.of("myserver.vip")::contains));
        assertEquals(5, PermissionLimits.resolve(1, entries, Set.of("myserver.mvp")::contains));
    }

    @Test
    void forgottenDashesStillMapASingleEntry() {
        YamlConfiguration yaml = yaml("""
                inventories:
                  limits:
                    permission: myserver.vip
                    max: 3
                """);
        assertEquals(3, PermissionLimits.resolve(
                1,
                PermissionLimits.readEntries(yaml, "inventories.limits"),
                Set.of("myserver.vip")::contains));
    }

    @Test
    void nodeToMaxMap() {
        YamlConfiguration yaml = yaml("""
                inventories:
                  limits:
                    myserver.vip: 3
                    myserver.mvp: 5
                """);
        List<Map<?, ?>> entries = PermissionLimits.readEntries(yaml, "inventories.limits");
        assertEquals(3, PermissionLimits.resolve(1, entries, Set.of("myserver.vip")::contains));
        assertEquals(5, PermissionLimits.resolve(1, entries, Set.of("myserver.mvp")::contains));
    }

    private static YamlConfiguration yaml(String text) {
        return YamlConfiguration.loadConfiguration(new StringReader(text));
    }
}
