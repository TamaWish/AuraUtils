package me.aurautils.managers;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtpCoordinateSamplerTest {

    @Test
    void chunkCentricSamplesStayInsideChunkBounds() {
        RtpSessionCache cache = new RtpSessionCache(4);
        RtpCoordinateSampler sampler = new RtpCoordinateSampler(
                0, 0, 100, 500, 4, false, true, false, 64, 4, cache, ThreadLocalRandom.current());

        for (int i = 0; i < 50; i++) {
            RtpCoordinateSampler.Sample sample = sampler.next();
            assertTrue((sample.x() & 15) >= 0);
            assertTrue((sample.z() & 15) >= 0);
            assertTrue(Math.hypot(sample.x(), sample.z()) >= 50);
        }
    }

    @Test
    void stratifiedRingsPreferLowFailureBands() {
        RtpSessionCache cache = new RtpSessionCache(3);
        cache.recordBandFailure(0);
        cache.recordBandFailure(0);
        cache.recordBandFailure(1);

        int picks = 0;
        for (int i = 0; i < 100; i++) {
            if (cache.pickBandWithFewestFailures(ThreadLocalRandom.current()) == 2) {
                picks++;
            }
        }
        assertTrue(picks > 80, "band 2 should be picked most often, was " + picks);
    }
}
