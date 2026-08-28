package com.lozaine.aurautils.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Validates names used as YAML keys and in player-facing teleport labels. */
public final class DestinationName {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private DestinationName() {
    }

    /** Returns a trimmed, safe name, or empty when it cannot be stored safely. */
    public static Optional<String> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        return VALID.matcher(value).matches() ? Optional.of(value) : Optional.empty();
    }

    public static String normalizedKey(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
