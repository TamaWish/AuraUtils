package com.lozaine.aurautils.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryLimitsTest {

    @Test
    void defaultPlayersGetOneInventory() {
        assertEquals(1, InventoryLimits.resolve(1, 10, List.of(), node -> false));
        assertEquals(1, InventoryLimits.resolve(1, 10, List.of(vip(3)), node -> false));
    }

    @Test
    void namedNodeDoesNothingWithoutAConfigMapping() {
        assertEquals(1, InventoryLimits.resolve(1, 10, List.of(), Set.of("myserver.vip")::contains));
    }

    @Test
    void rankLimitRaisesTheDefault() {
        assertEquals(3, InventoryLimits.resolve(1, 10, List.of(vip(3)), Set.of("myserver.vip")::contains));
    }

    @Test
    void numberedNodeGrantsInventoriesOneThroughN() {
        assertEquals(5, InventoryLimits.resolve(1, 10, List.of(), Set.of("aura.inv.5")::contains));
        assertEquals(5, InventoryLimits.resolve(1, 10, List.of(), Set.of("aura.inv.2", "aura.inv.5")::contains));
    }

    @Test
    void highestSourceWinsThenClampsToMax() {
        assertEquals(5, InventoryLimits.resolve(1, 10, List.of(vip(5)), Set.of("myserver.vip", "aura.inv.3")::contains));
        assertEquals(3, InventoryLimits.resolve(1, 10, List.of(vip(2)), Set.of("myserver.vip", "aura.inv.3")::contains));
        assertEquals(4, InventoryLimits.resolve(1, 4, List.of(vip(9)), Set.of("myserver.vip")::contains));
    }

    @Test
    void unlimitedConfigBecomesMax() {
        assertEquals(10, InventoryLimits.resolve(0, 10, List.of(), node -> false));
        assertEquals(10, InventoryLimits.resolve(1, 10, List.of(unlimited()), Set.of("aura.inv.unlimited")::contains));
    }

    @Test
    void adminGetsTheHardCap() {
        assertEquals(10, InventoryLimits.resolve(1, 10, List.of(), Set.of("aura.admin")::contains));
    }

    @Test
    void canOpenOnlyWithinLimit() {
        assertTrue(InventoryLimits.canOpen(1, 1));
        assertTrue(InventoryLimits.canOpen(3, 5));
        assertFalse(InventoryLimits.canOpen(2, 1));
        assertFalse(InventoryLimits.canOpen(0, 5));
        assertFalse(InventoryLimits.canOpen(1, 0));
    }

    @Test
    void numberedNodeHelper() {
        assertEquals("aura.inv.3", InventoryLimits.numberedNode(3));
        assertEquals(10, InventoryLimits.clampMax(10));
        assertEquals(1, InventoryLimits.clampMax(0));
        assertEquals(54, InventoryLimits.clampMax(99));
    }

    private static Map<String, Object> vip(int max) {
        return Map.of("permission", "myserver.vip", "max", max);
    }

    private static Map<String, Object> unlimited() {
        return Map.of("permission", "aura.inv.unlimited", "max", 0);
    }
}
