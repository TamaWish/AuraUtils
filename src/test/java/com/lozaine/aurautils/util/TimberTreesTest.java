package com.lozaine.aurautils.util;

import com.lozaine.aurautils.util.TimberTrees.BlockPos;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimberTreesTest {

    @Test
    void logSpeciesGroupsStrippedAndWoodWithLogs() {
        assertEquals("OAK", TimberTrees.logSpecies(Material.OAK_LOG));
        assertEquals("OAK", TimberTrees.logSpecies(Material.OAK_WOOD));
        assertEquals("OAK", TimberTrees.logSpecies(Material.STRIPPED_OAK_LOG));
        assertEquals("BIRCH", TimberTrees.logSpecies(Material.BIRCH_LOG));
        assertEquals("CRIMSON", TimberTrees.logSpecies(Material.CRIMSON_STEM));
        assertEquals("WARPED", TimberTrees.logSpecies(Material.STRIPPED_WARPED_HYPHAE));
        assertNull(TimberTrees.logSpecies(Material.OAK_LEAVES));
        assertNull(TimberTrees.logSpecies(Material.STONE));
    }

    @Test
    void isAxeDoesNotTreatPickaxesAsAxes() {
        assertTrue(TimberTrees.isAxe(Material.WOODEN_AXE));
        assertTrue(TimberTrees.isAxe(Material.NETHERITE_AXE));
        assertFalse(TimberTrees.isAxe(Material.WOODEN_PICKAXE));
        assertFalse(TimberTrees.isAxe(Material.DIAMOND_PICKAXE));
        assertFalse(TimberTrees.isAxe(Material.STICK));
        assertFalse(TimberTrees.isAxe(Material.AIR));
    }

    @Test
    void matchingLeavesIncludeAzaleaOnOakAndNetherCaps() {
        assertTrue(TimberTrees.isMatchingLeaf(Material.OAK_LEAVES, "OAK"));
        assertTrue(TimberTrees.isMatchingLeaf(Material.AZALEA_LEAVES, "OAK"));
        assertTrue(TimberTrees.isMatchingLeaf(Material.FLOWERING_AZALEA_LEAVES, "OAK"));
        assertFalse(TimberTrees.isMatchingLeaf(Material.BIRCH_LEAVES, "OAK"));
        assertTrue(TimberTrees.isMatchingLeaf(Material.NETHER_WART_BLOCK, "CRIMSON"));
        assertTrue(TimberTrees.isMatchingLeaf(Material.WARPED_WART_BLOCK, "WARPED"));
        assertFalse(TimberTrees.isMatchingLeaf(Material.NETHER_WART_BLOCK, "WARPED"));
    }

    @Test
    void collectLogsWalksDiagonalBranchesAndRespectsCap() {
        Map<BlockPos, Material> world = new HashMap<>();
        world.put(new BlockPos(0, 0, 0), Material.OAK_LOG);
        world.put(new BlockPos(0, 1, 0), Material.OAK_LOG);
        world.put(new BlockPos(1, 2, 1), Material.OAK_LOG);
        world.put(new BlockPos(5, 0, 0), Material.OAK_LOG);
        world.put(new BlockPos(0, 1, 1), Material.BIRCH_LOG);

        Function<BlockPos, Material> typeAt = pos -> world.getOrDefault(pos, Material.AIR);
        List<BlockPos> logs = TimberTrees.collectLogs(new BlockPos(0, 0, 0), "OAK", 64, typeAt);

        assertEquals(3, logs.size());
        assertTrue(logs.contains(new BlockPos(1, 2, 1)));
        assertFalse(logs.contains(new BlockPos(5, 0, 0)));
        assertFalse(logs.contains(new BlockPos(0, 1, 1)));

        List<BlockPos> capped = TimberTrees.collectLogs(new BlockPos(0, 0, 0), "OAK", 2, typeAt);
        assertEquals(2, capped.size());
    }

    @Test
    void leafCheckAndCollectStayOnTheSameTree() {
        Map<BlockPos, Material> world = new HashMap<>();
        world.put(new BlockPos(0, 0, 0), Material.OAK_LOG);
        world.put(new BlockPos(0, 1, 0), Material.OAK_LOG);
        world.put(new BlockPos(0, 2, 0), Material.OAK_LEAVES);
        world.put(new BlockPos(1, 2, 0), Material.OAK_LEAVES);
        world.put(new BlockPos(4, 4, 4), Material.OAK_LEAVES);

        Function<BlockPos, Material> typeAt = pos -> world.getOrDefault(pos, Material.AIR);
        List<BlockPos> logs = List.of(new BlockPos(0, 0, 0), new BlockPos(0, 1, 0));

        assertTrue(TimberTrees.hasMatchingLeaf(logs, "OAK", typeAt));
        assertFalse(TimberTrees.hasMatchingLeaf(logs, "BIRCH", typeAt));

        List<BlockPos> leaves = TimberTrees.collectLeaves(logs, "OAK", 64, typeAt);
        assertEquals(2, leaves.size());
        assertFalse(leaves.contains(new BlockPos(4, 4, 4)));
    }

    @Test
    void logHouseWithoutLeavesIsNotATree() {
        Map<BlockPos, Material> world = new HashMap<>();
        world.put(new BlockPos(0, 0, 0), Material.OAK_LOG);
        world.put(new BlockPos(1, 0, 0), Material.OAK_LOG);
        world.put(new BlockPos(0, 1, 0), Material.OAK_LOG);

        Function<BlockPos, Material> typeAt = pos -> world.getOrDefault(pos, Material.AIR);
        List<BlockPos> logs = TimberTrees.collectLogs(new BlockPos(0, 0, 0), "OAK", 64, typeAt);
        assertEquals(3, logs.size());
        assertFalse(TimberTrees.hasMatchingLeaf(logs, "OAK", typeAt));
    }
}
