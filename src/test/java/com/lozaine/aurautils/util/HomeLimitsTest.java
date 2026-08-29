package com.lozaine.aurautils.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeLimitsTest {

    @Test
    void defaultLimitAppliesWhenNoPermissionsMatch() {
        assertEquals(3, HomeLimits.resolve(3, List.of(vip(5)), node -> false));
        assertEquals(0, HomeLimits.resolve(0, List.of(), node -> true));
    }

    @Test
    void matchingPositiveLimitRaisesAPositiveDefault() {
        assertEquals(5, HomeLimits.resolve(3, List.of(vip(5)), Set.of("aura.homes.vip")::contains));
    }

    @Test
    void matchingZeroIsUnlimited() {
        assertEquals(0, HomeLimits.resolve(3, List.of(unlimited()), Set.of("aura.homes.unlimited")::contains));
    }

    @Test
    void matchingPositiveLimitCapsAnUnlimitedDefault() {
        assertEquals(5, HomeLimits.resolve(0, List.of(vip(5)), Set.of("aura.homes.vip")::contains));
    }

    @Test
    void skipsBlankPermissionAndUnparseableMax() {
        List<Map<?, ?>> entries = List.of(
                Map.of("permission", "  ", "max", 9),
                Map.of("permission", "aura.homes.vip", "max", "nope"),
                vip(5)
        );
        assertEquals(5, HomeLimits.resolve(1, entries, Set.of("aura.homes.vip")::contains));
    }

    private static Map<String, Object> vip(int max) {
        return Map.of("permission", "aura.homes.vip", "max", max);
    }

    private static Map<String, Object> unlimited() {
        return Map.of("permission", "aura.homes.unlimited", "max", 0);
    }
}
