package com.lozaine.aurautils.util;

import org.bukkit.Material;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Pure helpers for timber: wood species matching, axes, and connected-log search.
 * Isolated from Bukkit worlds so unit tests can feed a fake block map.
 */
public final class TimberTrees {

    public record BlockPos(int x, int y, int z) {
        public BlockPos offset(int dx, int dy, int dz) {
            return new BlockPos(x + dx, y + dy, z + dz);
        }
    }

    private TimberTrees() {}

    /**
     * Species key for a log/stem/hyphae/wood block, or {@code null} if not a tree trunk.
     * Stripped variants share the unstripped species (STRIPPED_OAK_LOG → OAK).
     */
    public static String logSpecies(Material material) {
        if (material == null) {
            return null;
        }
        String name = material.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        if (name.endsWith("_LOG")) {
            return name.substring(0, name.length() - 4);
        }
        if (name.endsWith("_WOOD")) {
            return name.substring(0, name.length() - 5);
        }
        if (name.endsWith("_STEM")) {
            return name.substring(0, name.length() - 5);
        }
        if (name.endsWith("_HYPHAE")) {
            return name.substring(0, name.length() - 7);
        }
        return null;
    }

    public static boolean isAxe(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_AXE") && !name.endsWith("_PICKAXE");
    }

    public static boolean isMatchingLeaf(Material material, String species) {
        if (material == null || species == null) {
            return false;
        }
        String name = material.name();
        if (name.endsWith("_LEAVES")) {
            String leafSpecies = name.substring(0, name.length() - 7);
            if (leafSpecies.equals(species)) {
                return true;
            }
            return "OAK".equals(species)
                    && (leafSpecies.equals("AZALEA") || leafSpecies.equals("FLOWERING_AZALEA"));
        }
        if ("CRIMSON".equals(species)
                && (name.equals("NETHER_WART_BLOCK") || name.equals("SHROOMLIGHT"))) {
            return true;
        }
        return "WARPED".equals(species)
                && (name.equals("WARPED_WART_BLOCK") || name.equals("SHROOMLIGHT"));
    }

    /**
     * 26-connected logs of the same species, including {@code origin}, capped at {@code max}.
     */
    public static List<BlockPos> collectLogs(
            BlockPos origin,
            String species,
            int max,
            Function<BlockPos, Material> typeAt
    ) {
        int cap = Math.max(1, max);
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        found.add(origin);

        while (!queue.isEmpty() && found.size() < cap) {
            BlockPos current = queue.removeFirst();
            for (BlockPos neighbor : neighbors26(current)) {
                if (found.contains(neighbor)) {
                    continue;
                }
                if (!species.equals(logSpecies(typeAt.apply(neighbor)))) {
                    continue;
                }
                found.add(neighbor);
                queue.addLast(neighbor);
                if (found.size() >= cap) {
                    break;
                }
            }
        }
        return new ArrayList<>(found);
    }

    public static boolean hasMatchingLeaf(
            Collection<BlockPos> logs,
            String species,
            Function<BlockPos, Material> typeAt
    ) {
        for (BlockPos log : logs) {
            for (BlockPos neighbor : neighbors26(log)) {
                if (isMatchingLeaf(typeAt.apply(neighbor), species)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Leaves of {@code species} connected to the felled logs (and then to each other).
     */
    public static List<BlockPos> collectLeaves(
            Collection<BlockPos> logs,
            String species,
            int max,
            Function<BlockPos, Material> typeAt
    ) {
        int cap = Math.max(0, max);
        if (cap == 0 || logs.isEmpty()) {
            return List.of();
        }
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : logs) {
            for (BlockPos neighbor : neighbors26(log)) {
                if (found.contains(neighbor)) {
                    continue;
                }
                if (!isMatchingLeaf(typeAt.apply(neighbor), species)) {
                    continue;
                }
                found.add(neighbor);
                queue.addLast(neighbor);
                if (found.size() >= cap) {
                    return new ArrayList<>(found);
                }
            }
        }
        while (!queue.isEmpty() && found.size() < cap) {
            BlockPos current = queue.removeFirst();
            for (BlockPos neighbor : neighbors26(current)) {
                if (found.contains(neighbor)) {
                    continue;
                }
                if (!isMatchingLeaf(typeAt.apply(neighbor), species)) {
                    continue;
                }
                found.add(neighbor);
                queue.addLast(neighbor);
                if (found.size() >= cap) {
                    break;
                }
            }
        }
        return new ArrayList<>(found);
    }

    static List<BlockPos> neighbors26(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    neighbors.add(pos.offset(dx, dy, dz));
                }
            }
        }
        return neighbors;
    }
}
