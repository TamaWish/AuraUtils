# Changelog

All notable changes to AuraUtils are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- **Async-safe RTP** — Headline `/rtp` implementation: Paper loads chunks with `getChunkAtAsync` (reflection); Spigot searches only already-loaded chunks when `rtp.only-loaded-chunks` is enabled; attempts spread across ticks with a pending-load cap on Paper.
- **`ChunkLoadPolicy`** and **`PlatformAdapter.whenChunkReady()`** — Centralized chunk readiness (`LOADED_ONLY`, `ASYNC`, `SYNC_FALLBACK`) replacing ad-hoc `chunk.load()` calls.
- **RTP config** — `rtp.generate-chunks`, `rtp.async-urgent`, `rtp.max-pending-chunk-loads`, optional `rtp.only-loaded-chunks`; `teleport.async-chunk-load`, `teleport.sync-chunk-fallback`.
- **Message** — `teleport.chunk-unavailable` when a destination chunk cannot be loaded before teleport.
- **Platform abstraction** — Single JAR with runtime Spigot/Paper detection (`PlatformAdapter`, `PlatformFactory`).
- **Paper optimizations** — `PaperMoveListener` uses `hasChangedPosition()` for teleport countdown cancel.
- **AsyncRtpEngine** — RTP location search batched per tick instead of one tight loop.
- **MiniMessage messaging** — `MessagesManager` loads `messages/<locale>.yml` with gradients, hex, hover, and click actions.
- **Localization** — Bundled `en` and sample `es` locales; Paper client locale; per-player override via `/aura locale`.
- **`/auracanceltp`** — Cancel pending teleport countdown (linked from countdown chat).
- **`/aura locale <code|clear>`** — Per-player language override stored in `player-states.yml`.
- **Server settings** — `/setspawn`, `/keepinventory`, spawn persistence across world reloads (RWR-friendly).

### Changed

- **TeleportHelper** — Destination chunks are no longer loaded synchronously when a countdown starts; load runs once when the countdown completes (async on Paper).
- **AsyncRtpEngine** — Removed synchronous `world.getChunkAt(...).load(true)` from the RTP search path; safety checks run only after chunks are available.
- **Chat prefix** moved from `config.yml` to `messages/<locale>.yml` → `meta.prefix` (MiniMessage).
- **`/aura reload`** now reloads `config.yml` and all message locale files.
- **Teleport countdown** messages use message keys; clickable cancel on supported clients.
- **Build** — Shades Adventure (MiniMessage, Gson, legacy serializers) into the plugin JAR.
- **Java** — Requires Java 25+ (see `pom.xml`).

### Documentation

- README: async-safe RTP section, platform comparison, and expanded `config.yml` reference.
- Release notes and changelog updated for chunk-loading behavior and RTP tuning.

## [1.0.0] - 2026-05-20

Initial release. Not yet published to GitHub.

### Added

- **Warps** — `/warp`, `/setwarp`, `/delwarp` with GUI list, tab completion, and per-warp permissions (`aura.warp.<name>`, `aura.warp.*`, or `aura.warp` for all).
- **Homes** — `/home`, `/sethome`, `/delhome` with per-player limits, GUI list, and tab completion; `/home` with no args teleports directly when the player has exactly one home.
- **TPA** — `/tpa`, `/tpaccept`, `/tpadeny` with configurable request timeout and teleport countdown.
- **TPA Here** — `/tpahere` to request another player teleport to you; same accept/deny flow as TPA.
- **Back** — `/back` to the last teleport location, integrated with other teleports.
- **Random teleport** — `/rtp` with spawn-radius search, minimum distance, safe-surface checks, per-tick attempt batching, countdown, and post-success cooldown.
- **Player toggles** — `/fly`, `/god`, `/nofall`, `/nohunger` with optional target player via `.others` permissions.
- **Utility menu** — `/menu` GUI for warps, homes, pending TPA (shows TPA vs TPA Here), and back.
- **Info & reload** — `/aura` for command list and toggle status; `/aura reload` for config reload (`aura.admin`).
- **Persistence** — `warps.yml`, `homes.yml`, and per-player toggle data; state applied on join.
- **Listeners** — Back tracking, menu clicks, god/fly/nofall/nohunger, session load/save.
- **Configuration** — `config.yml` for TPA timeout, RTP settings, global teleport countdown, and home limits.
- **Permissions** — Granular nodes in `plugin.yml` plus `aura.admin` umbrella.

### Build

- Maven artifact `AuraUtils-1.0.0.jar` (shaded build includes MiniMessage dependencies).

### Compatibility

- Java 25+
- Spigot / Paper API 1.21+

### Notes

- Install by placing the built JAR in `plugins/` and restarting the server.
- See [README.md](README.md) for usage and [plugin.yml](src/main/resources/plugin.yml) for all permission nodes.
