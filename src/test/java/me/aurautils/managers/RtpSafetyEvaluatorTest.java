package me.aurautils.managers;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtpSafetyEvaluatorTest {

    @Test
    void normalizeBiomeKey_lowercasesKeys() {
        assertEquals("plains", RtpSafetyEvaluator.normalizeBiomeKey("Plains"));
        assertEquals("minecraft:deep_ocean", RtpSafetyEvaluator.normalizeBiomeKey("minecraft:deep_ocean"));
    }

    @Test
    void biomeAllowed_respectsDeniedList() {
        assertFalse(RtpSafetyEvaluator.biomeAllowed(
                "minecraft:plains", Set.of(), Set.of("minecraft:plains")));
        assertTrue(RtpSafetyEvaluator.biomeAllowed(
                "minecraft:plains", Set.of(), Set.of("minecraft:ocean")));
    }

    @Test
    void biomeAllowed_respectsAllowedList() {
        assertTrue(RtpSafetyEvaluator.biomeAllowed(
                "minecraft:plains", Set.of("minecraft:plains"), Set.of()));
        assertFalse(RtpSafetyEvaluator.biomeAllowed(
                "minecraft:plains", Set.of("minecraft:forest"), Set.of()));
    }
}
