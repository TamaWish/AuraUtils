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
        int limit = Math.max(0, defaultLimit);
        if (entries == null || hasPermission == null) {
            return limit;
        }
        for (Map<?, ?> entry : entries) {
            if (entry == null) {
                continue;
            }
            Object permission = entry.get("permission");
            Object maximum = entry.get("max");
            if (!(permission instanceof String node) || node.isBlank() || maximum == null
                    || !hasPermission.test(node)) {
                continue;
            }
            int value;
            if (maximum instanceof Number number) {
                value = number.intValue();
            } else {
                try {
                    value = Integer.parseInt(String.valueOf(maximum).trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            if (value <= 0) {
                return 0;
            }
            limit = Math.max(limit, value);
        }
        return limit;
    }
}
