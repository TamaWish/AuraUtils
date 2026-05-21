package me.aurautils.util;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Locale;

public final class WarpPermissions {

    private WarpPermissions() {
    }

    /** Permission node for a specific warp, e.g. {@code aura.warp.spawn}. */
    public static String nodeFor(String warpName) {
        return "aura.warp." + warpName.toLowerCase(Locale.ROOT);
    }

    /**
     * A player may use a warp if they have {@code aura.admin}, the per-warp node,
     * {@code aura.warp.*}, or the general {@code aura.warp} (all warps).
     */
    public static boolean canUse(Player player, String warpName) {
        if (player.hasPermission("aura.admin")) {
            return true;
        }
        String normalized = warpName.toLowerCase(Locale.ROOT);
        if (player.hasPermission(nodeFor(normalized))) {
            return true;
        }
        if (player.hasPermission("aura.warp.*")) {
            return true;
        }
        return player.hasPermission("aura.warp");
    }

    /** True if the player can use at least one configured warp. */
    public static boolean canUseAny(Player player, Collection<String> warpNames) {
        if (player.hasPermission("aura.admin")
                || player.hasPermission("aura.warp")
                || player.hasPermission("aura.warp.*")) {
            return true;
        }
        for (String name : warpNames) {
            if (player.hasPermission(nodeFor(name))) {
                return true;
            }
        }
        return false;
    }
}
