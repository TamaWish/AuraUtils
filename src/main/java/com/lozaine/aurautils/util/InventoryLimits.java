package com.lozaine.aurautils.util;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * How many extra inventories ({@code /inv 1}, {@code /inv 2}, …) a player may open.
 *
 * <p>Sources (highest wins, then clamped to {@code maxInventories}):
 * <ul>
 *   <li>Numbered nodes {@code aura.inv.<n>} — having {@code aura.inv.5} allows inventories 1–5.
 *       These are registered with the server so wildcards expand over them.</li>
 *   <li>{@code inventories.default-limit} plus {@code inventories.limits}, which maps a
 *       server-defined node to a count ({@code 0} means all {@code maxInventories} slots)</li>
 *   <li>{@code aura.admin} — all {@code maxInventories} slots</li>
 * </ul>
 */
public final class InventoryLimits {

    public static final String USE_PERMISSION = "aura.inv";
    public static final String ADMIN_PERMISSION = "aura.admin";
    public static final String NUMBERED_PREFIX = "aura.inv.";
    public static final int MIN_INVENTORIES = 1;
    public static final int MAX_INVENTORIES = 54;
    public static final int DEFAULT_LIMIT = 1;
    public static final int DEFAULT_MAX = 10;

    private InventoryLimits() {
    }

    public static int clampMax(int maxInventories) {
        return Math.max(MIN_INVENTORIES, Math.min(MAX_INVENTORIES, maxInventories));
    }

    public static String numberedNode(int number) {
        return NUMBERED_PREFIX + number;
    }

    /**
     * @return 1..maxInventories inventories this player may open
     */
    public static int resolve(
            int defaultLimit,
            int maxInventories,
            List<Map<?, ?>> entries,
            Predicate<String> hasPermission
    ) {
        int max = clampMax(maxInventories);
        if (hasPermission != null && hasPermission.test(ADMIN_PERMISSION)) {
            return max;
        }
        int fromConfig = PermissionLimits.resolve(Math.max(0, defaultLimit), entries, hasPermission);
        if (fromConfig <= 0) {
            fromConfig = max;
        }
        int fromNodes = numberedLimit(max, hasPermission);
        int limit = Math.max(fromConfig, fromNodes);
        return Math.max(0, Math.min(max, limit));
    }

    /**
     * Highest {@code aura.inv.<n>} the player has, or {@code 0} if none.
     * Gaps are allowed: {@code aura.inv.5} without {@code aura.inv.2} still yields 5.
     */
    public static int numberedLimit(int maxInventories, Predicate<String> hasPermission) {
        int max = clampMax(maxInventories);
        if (hasPermission == null) {
            return 0;
        }
        int highest = 0;
        for (int number = 1; number <= max; number++) {
            if (hasPermission.test(numberedNode(number))) {
                highest = number;
            }
        }
        return highest;
    }

    public static boolean canOpen(int number, int limit) {
        return number >= MIN_INVENTORIES && limit > 0 && number <= limit;
    }
}
