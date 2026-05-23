package me.aurautils.config;

import me.aurautils.AuraUtils;
import me.aurautils.managers.MessagesManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static AuraConfig validate(AuraUtils plugin) {
        FileConfiguration raw = plugin.getConfig();
        List<String> warnings = new ArrayList<>();

        int tpaTimeout = atLeast(raw, "tpa.timeout", 60, 1, "tpa.timeout", warnings);

        int teleportCountdown = nonNegative(raw, "teleport.countdown", 5, "teleport.countdown", warnings);
        boolean teleportAsyncChunkLoad = raw.getBoolean("teleport.async-chunk-load", true);
        boolean teleportSyncChunkFallback = raw.getBoolean("teleport.sync-chunk-fallback", true);
        int teleportChunkLoadTimeoutSeconds = nonNegative(raw, "teleport.chunk-load-timeout-seconds", 10,
                "teleport.chunk-load-timeout-seconds", warnings);
        TeleportFeatureOverride adminTeleportOverride = parseTeleportOverride(raw, "teleport.overrides.admin");
        TeleportFeatureOverride rtpTeleportOverride = parseTeleportOverride(raw, "teleport.overrides.rtp");

        int rtpRadius = atLeast(raw, "rtp.radius", 2000, 1, "rtp.radius", warnings);
        int rtpMinDistance = nonNegative(raw, "rtp.minDistance", 100, "rtp.minDistance", warnings);
        if (rtpMinDistance > rtpRadius) {
            warnings.add("rtp.minDistance (" + rtpMinDistance + ") exceeds rtp.radius (" + rtpRadius
                    + "); minDistance set to " + rtpRadius);
            rtpMinDistance = rtpRadius;
        }

        int rtpAttempts = atLeast(raw, "rtp.attempts", 80, 1, "rtp.attempts", warnings);
        int rtpAttemptsPerTick = atLeast(raw, "rtp.attemptsPerTick", 10, 1, "rtp.attemptsPerTick", warnings);
        if (rtpAttemptsPerTick > rtpAttempts) {
            warnings.add("rtp.attemptsPerTick (" + rtpAttemptsPerTick + ") exceeds rtp.attempts (" + rtpAttempts
                    + "); attemptsPerTick set to " + rtpAttempts);
            rtpAttemptsPerTick = rtpAttempts;
        }

        boolean rtpCenterOnPlayer = raw.getBoolean("rtp.center-on-player", true);
        boolean rtpOnlyLoadedChunksExplicitlySet = raw.contains("rtp.only-loaded-chunks");
        boolean rtpOnlyLoadedChunks = rtpOnlyLoadedChunksExplicitlySet
                && raw.getBoolean("rtp.only-loaded-chunks");
        boolean rtpGenerateChunks = raw.getBoolean("rtp.generate-chunks", false);
        boolean rtpAsyncUrgent = raw.getBoolean("rtp.async-urgent", true);
        int rtpMaxPendingChunkLoads = atLeast(raw, "rtp.max-pending-chunk-loads", 4, 1,
                "rtp.max-pending-chunk-loads", warnings);
        int chunkLoadMaxInFlightGlobal = atLeast(raw, "chunk-load.max-in-flight-global", 32, 1,
                "chunk-load.max-in-flight-global", warnings);
        int chunkLoadMaxPerPlayer = atLeast(raw, "chunk-load.max-in-flight-per-player", rtpMaxPendingChunkLoads, 1,
                "chunk-load.max-in-flight-per-player", warnings);
        int chunkLoadMaxQueueSize = atLeast(raw, "chunk-load.max-queue-size", 256, 0,
                "chunk-load.max-queue-size", warnings);
        int rtpCountdown = nonNegative(raw, "rtp.countdown", 0, "rtp.countdown", warnings);
        int rtpCooldown = nonNegative(raw, "rtp.cooldown", 60, "rtp.cooldown", warnings);

        int homesDefaultLimit = raw.getInt("homes.default-limit", raw.getInt("homes.max-per-player", 3));
        if (homesDefaultLimit < 0) {
            warnings.add("homes.default-limit was " + homesDefaultLimit + ", set to 0");
            homesDefaultLimit = 0;
        }
        validateHomePermissionLimits(raw, warnings);

        boolean vanishEnabled = raw.getBoolean("vanish.enabled", true);
        String vanishSeePermission = raw.getString("vanish.see-permission", "aura.vanish.see");
        if (vanishSeePermission == null || vanishSeePermission.isBlank()) {
            warnings.add("vanish.see-permission was empty, using aura.vanish.see");
            vanishSeePermission = "aura.vanish.see";
        }
        List<String> vanishMetadataKeys = new ArrayList<>(raw.getStringList("vanish.metadata-keys"));
        if (vanishMetadataKeys.isEmpty()) {
            warnings.add("vanish.metadata-keys was empty, using [aura.vanish]");
            vanishMetadataKeys = List.of("aura.vanish");
        } else {
            vanishMetadataKeys = List.copyOf(vanishMetadataKeys);
        }

        String messagesDefaultLocale = normalizeLocale(raw.getString("messages.default-locale", "en"));
        String messagesFallbackLocale = normalizeLocale(raw.getString("messages.fallback-locale", "en"));
        boolean messagesUseClientLocale = raw.getBoolean("messages.use-client-locale", true);
        validateMessageLocales(plugin, messagesDefaultLocale, messagesFallbackLocale, warnings);

        logWarnings(plugin, warnings);

        return new AuraConfig(
                tpaTimeout,
                teleportCountdown,
                teleportAsyncChunkLoad,
                teleportSyncChunkFallback,
                teleportChunkLoadTimeoutSeconds,
                adminTeleportOverride,
                rtpTeleportOverride,
                rtpRadius,
                rtpMinDistance,
                rtpAttempts,
                rtpAttemptsPerTick,
                rtpCenterOnPlayer,
                rtpOnlyLoadedChunksExplicitlySet,
                rtpOnlyLoadedChunks,
                rtpGenerateChunks,
                rtpAsyncUrgent,
                rtpMaxPendingChunkLoads,
                chunkLoadMaxInFlightGlobal,
                chunkLoadMaxPerPlayer,
                chunkLoadMaxQueueSize,
                rtpCountdown,
                rtpCooldown,
                homesDefaultLimit,
                vanishEnabled,
                vanishSeePermission,
                vanishMetadataKeys,
                messagesDefaultLocale,
                messagesFallbackLocale,
                messagesUseClientLocale
        );
    }

    private static void validateHomePermissionLimits(FileConfiguration raw, List<String> warnings) {
        for (String node : raw.getStringList("homes.permission-limits")) {
            if (parseHomeLimit(node) < 0) {
                warnings.add("homes.permission-limits entry '" + node
                        + "' does not end with a numeric limit and will be ignored");
            }
        }
    }

    private static int parseHomeLimit(String permissionNode) {
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

    private static void validateMessageLocales(AuraUtils plugin, String defaultLocale, String fallbackLocale,
                                               List<String> warnings) {
        MessagesManager messages = plugin.getMessages();
        if (messages == null) {
            return;
        }
        if (!messages.hasLocale(defaultLocale)) {
            warnings.add("messages.default-locale '" + defaultLocale + "' has no messages file on disk");
        }
        if (!messages.hasLocale(fallbackLocale)) {
            warnings.add("messages.fallback-locale '" + fallbackLocale + "' has no messages file on disk");
        }
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        return locale.trim().toLowerCase(Locale.ROOT);
    }

    private static TeleportFeatureOverride parseTeleportOverride(FileConfiguration raw, String path) {
        if (!raw.isConfigurationSection(path)) {
            return TeleportFeatureOverride.EMPTY;
        }
        Integer countdown = raw.contains(path + ".countdown")
                ? raw.getInt(path + ".countdown")
                : null;
        Boolean asyncChunkLoad = raw.contains(path + ".async-chunk-load")
                ? raw.getBoolean(path + ".async-chunk-load")
                : null;
        return new TeleportFeatureOverride(countdown, asyncChunkLoad);
    }

    private static int nonNegative(FileConfiguration raw, String path, int defaultValue, String label,
                                   List<String> warnings) {
        int value = raw.getInt(path, defaultValue);
        if (value < 0) {
            warnings.add(label + " was " + value + ", set to 0");
            return 0;
        }
        return value;
    }

    private static int atLeast(FileConfiguration raw, String path, int defaultValue, int minimum, String label,
                               List<String> warnings) {
        int value = raw.getInt(path, defaultValue);
        if (value < minimum) {
            warnings.add(label + " was " + value + ", set to " + minimum);
            return minimum;
        }
        return value;
    }

    private static void logWarnings(AuraUtils plugin, List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        for (String warning : warnings) {
            plugin.getLogger().warning("[Config] " + warning);
        }
        plugin.getLogger().warning("[Config] " + warnings.size()
                + " issue(s) corrected at runtime — fix config.yml and run /aura reload.");
    }
}
