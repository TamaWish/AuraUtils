package com.lozaine.aurautils.util;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Resolves how many homes a player may set. {@code 0} means unlimited.
 *
 * <p>Matching {@code max: 0} is unlimited immediately. Otherwise the highest
 * matching positive max wins against {@code defaultLimit}. If the default is
 * unlimited ({@code 0}), a matching positive rank limit will cap that player
 * — set a positive default before adding VIP caps.
 */
public final class HomeLimits {

    private HomeLimits() {
    }

    public static int resolve(int defaultLimit, List<Map<?, ?>> entries, Predicate<String> hasPermission) {
        return PermissionLimits.resolve(defaultLimit, entries, hasPermission);
    }
}
