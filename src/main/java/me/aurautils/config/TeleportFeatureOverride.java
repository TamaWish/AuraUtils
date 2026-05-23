package me.aurautils.config;

/**
 * Optional per-feature overrides under {@code teleport.overrides.<feature>} in config.yml.
 */
public record TeleportFeatureOverride(Integer countdown, Boolean asyncChunkLoad) {
    public static final TeleportFeatureOverride EMPTY = new TeleportFeatureOverride(null, null);
}
