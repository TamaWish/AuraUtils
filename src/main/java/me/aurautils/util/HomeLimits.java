package me.aurautils.util;

public final class HomeLimits {

    private HomeLimits() {
    }

    /** Parses the numeric suffix of a permission node (e.g. {@code aura.homes.5} → 5). Returns -1 if invalid. */
    public static int parseLimit(String permissionNode) {
        if (permissionNode == null) {
            return -1;
        }
        String trimmed = permissionNode.trim();
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot < 0 || lastDot == trimmed.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(trimmed.substring(lastDot + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
