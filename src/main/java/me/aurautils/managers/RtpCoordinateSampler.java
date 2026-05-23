package me.aurautils.managers;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Phase B coordinate generation: stratified rings, chunk-centric probes, and grid jitter.
 */
public final class RtpCoordinateSampler {

    public record Sample(int x, int z, int bandIndex, long chunkKey) {
    }

    private final int centerX;
    private final int centerZ;
    private final int minDist;
    private final int searchRadius;
    private final int ringBandCount;
    private final boolean stratifiedRings;
    private final boolean chunkCentric;
    private final boolean gridJitter;
    private final int gridCellSize;
    private final int chunkRetryLimit;
    private final RtpSessionCache session;
    private final ThreadLocalRandom random;

    public RtpCoordinateSampler(
            int centerX,
            int centerZ,
            int minDist,
            int searchRadius,
            int ringBandCount,
            boolean stratifiedRings,
            boolean chunkCentric,
            boolean gridJitter,
            int gridCellSize,
            int chunkRetryLimit,
            RtpSessionCache session,
            ThreadLocalRandom random
    ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minDist = Math.max(0, minDist);
        this.searchRadius = Math.max(1, searchRadius);
        this.ringBandCount = Math.max(1, ringBandCount);
        this.stratifiedRings = stratifiedRings;
        this.chunkCentric = chunkCentric;
        this.gridJitter = gridJitter;
        this.gridCellSize = Math.max(8, gridCellSize);
        this.chunkRetryLimit = Math.max(1, chunkRetryLimit);
        this.session = session;
        this.random = random;
    }

    public Sample next() {
        for (int attempt = 0; attempt < chunkRetryLimit; attempt++) {
            int bandIndex = stratifiedRings
                    ? session.pickBandWithFewestFailures(random)
                    : random.nextInt(ringBandCount);
            Sample sample = chunkCentric
                    ? sampleChunkCentric(bandIndex)
                    : (gridJitter ? sampleGridJitter(bandIndex) : samplePolar(bandIndex));
            if (!session.hasTriedChunk(sample.chunkKey()) || session.markChunkTried(sample.chunkKey())) {
                if (!gridJitter || chunkCentric || session.markGridCell(gridCellKey(sample.x(), sample.z()))) {
                    return sample;
                }
            }
        }
        return samplePolar(stratifiedRings ? session.pickBandWithFewestFailures(random) : random.nextInt(ringBandCount));
    }

    private Sample samplePolar(int bandIndex) {
        double inner = bandInner(bandIndex);
        double outer = bandOuter(bandIndex);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = inner + random.nextDouble() * Math.max(0.0, outer - inner);
        int x = centerX + (int) Math.round(Math.cos(angle) * distance);
        int z = centerZ + (int) Math.round(Math.sin(angle) * distance);
        return new Sample(x, z, bandIndex, chunkKey(x, z));
    }

    private Sample sampleChunkCentric(int bandIndex) {
        double inner = bandInner(bandIndex);
        double outer = bandOuter(bandIndex);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = inner + random.nextDouble() * Math.max(0.0, outer - inner);
        int chunkX = (centerX + (int) Math.round(Math.cos(angle) * distance)) >> 4;
        int chunkZ = (centerZ + (int) Math.round(Math.sin(angle) * distance)) >> 4;
        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        return new Sample(x, z, bandIndex, chunkKey(chunkX << 4, chunkZ << 4));
    }

    private Sample sampleGridJitter(int bandIndex) {
        double inner = bandInner(bandIndex);
        double outer = bandOuter(bandIndex);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = inner + random.nextDouble() * Math.max(0.0, outer - inner);
        int baseX = centerX + (int) Math.round(Math.cos(angle) * distance);
        int baseZ = centerZ + (int) Math.round(Math.sin(angle) * distance);
        int cellX = Math.floorDiv(baseX, gridCellSize);
        int cellZ = Math.floorDiv(baseZ, gridCellSize);
        int x = cellX * gridCellSize + random.nextInt(gridCellSize);
        int z = cellZ * gridCellSize + random.nextInt(gridCellSize);
        return new Sample(x, z, bandIndex, chunkKey(x, z));
    }

    private double bandInner(int bandIndex) {
        if (searchRadius <= minDist || ringBandCount <= 1) {
            return minDist;
        }
        double span = searchRadius - minDist;
        return minDist + (span * bandIndex) / ringBandCount;
    }

    private double bandOuter(int bandIndex) {
        if (searchRadius <= minDist || ringBandCount <= 1) {
            return searchRadius;
        }
        double span = searchRadius - minDist;
        return minDist + (span * (bandIndex + 1)) / ringBandCount;
    }

    private static long chunkKey(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private long gridCellKey(int x, int z) {
        long gx = Math.floorDiv(x, gridCellSize);
        long gz = Math.floorDiv(z, gridCellSize);
        return (gx << 32) ^ gz;
    }
}
