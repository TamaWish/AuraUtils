package me.aurautils.platform;

/**
 * Controls how a chunk at a destination is made available before teleport or RTP validation.
 */
public enum ChunkLoadPolicy {

    /** Skip unloaded chunks (Spigot RTP search). */
    LOADED_ONLY,

    /** Request async chunk load; optional generation (Paper). */
    ASYNC,

    /** Allow a single synchronous load on the main thread when nothing else works (Spigot teleports). */
    SYNC_FALLBACK
}
