package me.aurautils.test;

import me.aurautils.config.AuraConfig;
import me.aurautils.config.TeleportFeatureOverride;
import me.aurautils.managers.RtpMode;

import java.util.List;
import java.util.Set;

/** Shared {@link AuraConfig} instances for unit tests. */
public final class TestAuraConfigs {

    private TestAuraConfigs() {
    }

    public static AuraConfig defaults() {
        return new AuraConfig(
                60, 5, true, true, 0,
                TeleportFeatureOverride.EMPTY, TeleportFeatureOverride.EMPTY,
                2000, 100, 80, 10, true, false, false, false, true, 4,
                2, 3, 3, true, 60, 8, 500, 50, 0,
                Set.of(), Set.of(), List.of(),
                RtpMode.SURFACE, true, 4, true, true, 64, 8, true, 1, 10, -32, 64,
                32, 4, 256, 0, 60,
                3, true, "aura.vanish.see", List.of("aura.vanish"), "en", "en", true);
    }
}
