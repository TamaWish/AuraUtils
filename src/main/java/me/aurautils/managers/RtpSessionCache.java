package me.aurautils.managers;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-search cache: ring-band failure counts, tried chunks, and used grid cells for jitter.
 */
public final class RtpSessionCache {

    private final int ringBandCount;
    private final int[] bandFailures;
    private final Set<Long> triedChunks = new HashSet<>();
    private final Set<Long> usedGridCells = new HashSet<>();

    public RtpSessionCache(int ringBandCount) {
        this.ringBandCount = Math.max(1, ringBandCount);
        this.bandFailures = new int[this.ringBandCount];
    }

    public int ringBandCount() {
        return ringBandCount;
    }

    public void recordBandFailure(int bandIndex) {
        if (bandIndex >= 0 && bandIndex < ringBandCount) {
            bandFailures[bandIndex]++;
        }
    }

    public int pickBandWithFewestFailures(java.util.random.RandomGenerator random) {
        int best = 0;
        int bestFailures = bandFailures[0];
        int tied = 1;
        for (int i = 1; i < ringBandCount; i++) {
            if (bandFailures[i] < bestFailures) {
                bestFailures = bandFailures[i];
                best = i;
                tied = 1;
            } else if (bandFailures[i] == bestFailures) {
                tied++;
                if (random.nextInt(tied) == 0) {
                    best = i;
                }
            }
        }
        return best;
    }

    public boolean markChunkTried(long chunkKey) {
        return triedChunks.add(chunkKey);
    }

    public boolean hasTriedChunk(long chunkKey) {
        return triedChunks.contains(chunkKey);
    }

    public boolean markGridCell(long cellKey) {
        return usedGridCells.add(cellKey);
    }
}
