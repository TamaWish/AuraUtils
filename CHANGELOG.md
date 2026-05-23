# Changelog

All notable changes to AuraUtils are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Documentation

- README, CHANGELOG, and v1.0.0 release notes updated for feature toggles, `/spawn`, heal/feed, menu GUI, bundled Maven path, and correct shaded JAR deployment.
- Added [docs/BUILD.md](docs/BUILD.md) (build/deploy guide) and [LICENSE](LICENSE) (MIT).

## [1.0.0] - 2026-05-23

### Added

- **Warps** — `/warp`, `/setwarp`, `/delwarp` with paginated GUI, tab completion, per-warp permissions (`aura.warp.<name>`, `aura.warp`, `aura.warp.admin`).
- **Homes** — `/home`, `/sethome`, `/delhome` with limits, GUI, tab completion; single-home players teleport directly with `/home`.
- **TPA** — `/tpa`, `/tpaccept`, `/tpadeny` with configurable timeout and teleport countdown.
- **TPA Here** — `/tpahere`; same accept/deny flow.
- **Back** — `/back` integrated with AuraUtils teleports.
- **Random teleport** — Async-safe `/rtp` (Paper async chunk loads; Spigot loaded-chunk-only mode); per-tick attempt batching, countdown, cooldown.
- **Spawn** — `/spawn` to saved or vanilla spawn; `/setspawn` with `server-spawns.yml` persistence (RWR-friendly).
- **Player toggles** — `/fly`, `/god`, `/nofall`, `/nohunger` with `.others` permissions.
- **Heal & feed** — `/heal`, `/feed` with `.others` permissions.
- **Utility menu** — `/menu` with warps, homes, TPA, spawn, heal, feed, back, refresh, and pagination.
- **Feature toggles** — `features.*` in `config.yml` to disable command groups.
- **Platform abstraction** — Single JAR; runtime Spigot/Paper detection (`PlatformAdapter`, `PlatformFactory`, `ChunkLoadPolicy`).
- **Paper optimizations** — `PaperMoveListener` with `hasChangedPosition()` for countdown cancel.
- **MiniMessage messaging** — `MessagesManager`, bundled `en` and `es`; merge bundled keys into existing locale files on disk.
- **Localization** — Paper client locale; `/aura locale <code|clear>` stored in `player-states.yml`.
- **`/auracanceltp`** — Cancel pending teleport countdown (clickable in chat).
- **`/keepinventory`** — Server-wide keep inventory gamerule; re-applied on world load.
- **`ChunkLoadPolicy` / `whenChunkReady()`** — Centralized chunk readiness for RTP and teleports.
- **Message** — `teleport.chunk-unavailable`, `feature.disabled`, heal/feed/spawn keys.

### Changed

- **TeleportHelper** — Destination chunks load when countdown completes, not when it starts (async on Paper).
- **Chat prefix** — Moved to `messages/<locale>.yml` → `meta.prefix` (MiniMessage).
- **`/aura reload`** — Reloads config, messages, and re-applies keep inventory.
- **Build** — Shades Adventure into the plugin JAR (relocated); requires **Java 25+**.

### Fixed

- Menu warp/home clicks use shared `TeleportHelper` and respect countdown.
- Bundled message files merge with on-disk overrides without dropping custom keys.
- Shaded JAR build on Java 25.

### Documentation

- README: async RTP, platform table, feature toggles, commands, permissions, building, deployment notes.
- Release notes: [releases/v1.0.0.md](releases/v1.0.0.md).

### Compatibility

- Java 25+
- Spigot / Paper API 1.21+
