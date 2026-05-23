package me.aurautils.managers;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * RTP safety validation for surface and cave modes: standable floor, clearance, biome filters, hazard scoring.
 */
public final class RtpSafetyEvaluator {

    private static final int SURFACE_SCAN_DEPTH = 24;
    private static final int LIQUID_SCAN_RADIUS = 4;

    private static final Set<Material> UNSAFE_FLOOR = Set.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.BEDROCK,
            Material.POINTED_DRIPSTONE,
            Material.COBWEB,
            Material.SCAFFOLDING,
            Material.BAMBOO,
            Material.BAMBOO_SAPLING
    );

    private RtpSafetyEvaluator() {
    }

    public static Optional<RtpCandidate> evaluate(
            World world,
            int x,
            int z,
            float yaw,
            float pitch,
            RtpMode mode,
            int solidBlocksBelow,
            int ceilingClearance,
            int caveSurfaceBuffer,
            int caveMinY,
            int caveMaxY,
            Set<String> allowedBiomes,
            Set<String> deniedBiomes
    ) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return Optional.empty();
        }
        return mode == RtpMode.CAVE
                ? evaluateCave(world, x, z, yaw, pitch, solidBlocksBelow, ceilingClearance,
                caveSurfaceBuffer, caveMinY, caveMaxY, allowedBiomes, deniedBiomes)
                : evaluateSurface(world, x, z, yaw, pitch, solidBlocksBelow, ceilingClearance,
                allowedBiomes, deniedBiomes);
    }

    private static Optional<RtpCandidate> evaluateSurface(
            World world,
            int x,
            int z,
            float yaw,
            float pitch,
            int solidBlocksBelow,
            int ceilingClearance,
            Set<String> allowedBiomes,
            Set<String> deniedBiomes
    ) {
        int topY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int minY = world.getMinHeight();
        int scanMin = Math.max(minY, topY - SURFACE_SCAN_DEPTH);

        return scanVertical(world, x, z, yaw, pitch, topY, scanMin, solidBlocksBelow, ceilingClearance,
                allowedBiomes, deniedBiomes);
    }

    private static Optional<RtpCandidate> evaluateCave(
            World world,
            int x,
            int z,
            float yaw,
            float pitch,
            int solidBlocksBelow,
            int ceilingClearance,
            int caveSurfaceBuffer,
            int caveMinY,
            int caveMaxY,
            Set<String> allowedBiomes,
            Set<String> deniedBiomes
    ) {
        int surfaceY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int scanTop = Math.min(caveMaxY, surfaceY - caveSurfaceBuffer);
        int scanMin = Math.max(world.getMinHeight() + 1, caveMinY);
        if (scanTop <= scanMin) {
            return Optional.empty();
        }
        return scanVertical(world, x, z, yaw, pitch, scanTop, scanMin, solidBlocksBelow, ceilingClearance,
                allowedBiomes, deniedBiomes);
    }

    private static Optional<RtpCandidate> scanVertical(
            World world,
            int x,
            int z,
            float yaw,
            float pitch,
            int startY,
            int minY,
            int solidBlocksBelow,
            int ceilingClearance,
            Set<String> allowedBiomes,
            Set<String> deniedBiomes
    ) {
        for (int y = startY; y >= minY; y--) {
            Block floor = world.getBlockAt(x, y, z);
            if (!isStandableFloor(floor)) {
                continue;
            }

            Block feet = floor.getRelative(BlockFace.UP);
            Block head = feet.getRelative(BlockFace.UP);
            if (!hasStandingSpace(feet, head)) {
                continue;
            }
            if (!hasSolidDepth(floor, solidBlocksBelow)) {
                continue;
            }
            if (!hasCeilingClearance(head, ceilingClearance)) {
                continue;
            }
            if (!biomeAllowed(normalizeBiomeKey(world.getBiome(x, y, z).getKey().toString()),
                    allowedBiomes, deniedBiomes)) {
                continue;
            }

            Location location = new Location(world, x + 0.5, floor.getY() + 1.0, z + 0.5, yaw, pitch);
            int hazardScore = hazardScore(world, floor, feet);
            return Optional.of(new RtpCandidate(location, hazardScore));
        }
        return Optional.empty();
    }

    static boolean biomeAllowed(
            String biomeKey,
            Set<String> allowedBiomes,
            Set<String> deniedBiomes
    ) {
        if (!deniedBiomes.isEmpty() && deniedBiomes.contains(biomeKey)) {
            return false;
        }
        if (allowedBiomes.isEmpty()) {
            return true;
        }
        return allowedBiomes.contains(biomeKey);
    }

    static String normalizeBiomeKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        return key.toLowerCase(Locale.ROOT);
    }

    static int hazardScore(World world, Block floor, Block feet) {
        int score = liquidProximityPenalty(world, floor);
        score += lightPenalty(feet.getLightLevel());
        score += slopePenalty(world, floor);
        return score;
    }

    private static int liquidProximityPenalty(World world, Block floor) {
        int centerX = floor.getX();
        int centerY = floor.getY();
        int centerZ = floor.getZ();
        int nearest = LIQUID_SCAN_RADIUS + 1;

        for (int dx = -LIQUID_SCAN_RADIUS; dx <= LIQUID_SCAN_RADIUS; dx++) {
            for (int dz = -LIQUID_SCAN_RADIUS; dz <= LIQUID_SCAN_RADIUS; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Block block = world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz);
                    if (!block.isLiquid()) {
                        continue;
                    }
                    int distance = Math.max(Math.abs(dx), Math.abs(dz));
                    nearest = Math.min(nearest, distance);
                }
            }
        }

        if (nearest > LIQUID_SCAN_RADIUS) {
            return 0;
        }
        return (LIQUID_SCAN_RADIUS - nearest + 1) * 8;
    }

    private static int lightPenalty(int lightLevel) {
        return Math.max(0, 12 - lightLevel) * 2;
    }

    private static int slopePenalty(World world, Block floor) {
        int baseY = floor.getY();
        int maxDelta = 0;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block neighbor = floor.getRelative(face);
            int neighborSurfaceY = world.getHighestBlockYAt(
                    neighbor.getX(), neighbor.getZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
            maxDelta = Math.max(maxDelta, Math.abs(neighborSurfaceY - baseY));
        }
        return maxDelta * 5;
    }

    private static boolean hasSolidDepth(Block floor, int requiredSolidBlocks) {
        if (requiredSolidBlocks <= 0) {
            return true;
        }
        Block current = floor;
        for (int i = 0; i < requiredSolidBlocks; i++) {
            if (!isSolidSupport(current)) {
                return false;
            }
            current = current.getRelative(BlockFace.DOWN);
        }
        return true;
    }

    private static boolean hasCeilingClearance(Block head, int clearance) {
        Block above = head;
        for (int i = 0; i < clearance; i++) {
            above = above.getRelative(BlockFace.UP);
            if (!isPassableForHeadroom(above)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSolidSupport(Block block) {
        if (block.isEmpty() || block.isLiquid()) {
            return false;
        }
        Material type = block.getType();
        if (UNSAFE_FLOOR.contains(type)) {
            return false;
        }
        return !block.isPassable();
    }

    private static boolean isStandableFloor(Block block) {
        return isSolidSupport(block);
    }

    private static boolean hasStandingSpace(Block feet, Block head) {
        return isPassableForHeadroom(feet) && isPassableForHeadroom(head);
    }

    private static boolean isPassableForHeadroom(Block block) {
        if (block.isEmpty()) {
            return true;
        }
        Material type = block.getType();
        if (UNSAFE_FLOOR.contains(type)) {
            return false;
        }
        if (block.isLiquid()) {
            return false;
        }
        return block.isPassable();
    }
}
