package me.aurautils.managers;

import me.aurautils.config.AuraConfig;
import me.aurautils.test.TestAuraConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtpAdaptiveStateTest {

    private RtpAdaptiveState state;
    private AuraConfig config;

    @BeforeEach
    void setUp() {
        state = new RtpAdaptiveState();
        config = TestAuraConfigs.defaults();
    }

    @Test
    void doesNotAdaptBeforeMinSamples() {
        for (int i = 0; i < config.rtpAdaptiveMinSamples() - 1; i++) {
            state.recordFailure();
        }
        assertFalse(state.shouldAdapt(config));
        assertEquals(config.rtpRadius(), state.adjustedRadius(config.rtpRadius(), config));
    }

    @Test
    void widensSearchWhenFailureRateHigh() {
        for (int i = 0; i < config.rtpAdaptiveMinSamples(); i++) {
            state.recordFailure();
        }
        assertTrue(state.shouldAdapt(config));
        assertEquals(
                config.rtpRadius() + config.rtpAdaptiveRadiusBonus(),
                state.adjustedRadius(config.rtpRadius(), config));
        assertEquals(
                config.rtpMinDistance() - config.rtpAdaptiveMinDistanceReduction(),
                state.adjustedMinDistance(config.rtpMinDistance(), config));
    }
}
