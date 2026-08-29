# Changelog

All notable changes for AuraUtils.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2026-08-29

### Added
- **Translatable messages** — chat, GUI titles/lore, countdown title/action bar, and clickable confirm labels load from `lang/en.yml`. Set `language:` in `config.yml` and add `plugins/AuraUtils/lang/<code>.yml` for other locales. Missing keys fall back to jar English. New keys are merged into disk `lang/en.yml` on upgrade.
- **`/aura reload`** — reloads `config.yml` and the active language file (`aura.admin`). Warps and homes are live-managed and are not re-read from disk.
- **Home limits** — `homes.default-limit` (0 = unlimited) plus optional `homes.limits` entries keyed by Bukkit permission (LuckPerms groups work with no extra dependency). Highest matching positive max wins; `max: 0` is unlimited. Set a positive `default-limit` before adding VIP caps — if the default is unlimited, a matching positive max caps that player.
- **Overwrite confirmation** — `/sethome` and `/setwarp` prompt with clickable **[CONFIRM]** / **[CANCEL]** when the name already exists (30 second expiry).
- **Safe destination names** — home and warp names must be 1–32 letters, numbers, `_`, or `-`.
- **GitHub update checker** — optional (`update-checker.enabled`, default true). Checks [TamaWish/AuraUtils](https://github.com/TamaWish/AuraUtils) for a newer release. Console is informed; players with `aura.admin` get a chat notice with a clickable releases link.

### Fixed
- Concurrent TPA request lifecycle (requester cancel and player-quit cleanup).
- Pending teleport and `/back` state under Folia region parallelism.
- Paper async RTP chunk-load detection inspects the runtime world implementation.
- Teleport success messages wait for the asynchronous teleport result instead of reporting success when a request is only submitted.
- Trusted instant TPA and countdown-0 `/home`, `/warp`, and `/back` (commands and GUI) report failure when the teleport future does not succeed.
- `/aura` status showed `common.on` / `common.off` because YAML 1.1 treats unquoted `on`/`off` as booleans. Those keys are now quoted so players see ON/OFF.
- `/aura` status leaked ON/OFF color onto the next label (`Fly:`, `NoHunger:`). ON/OFF/ENABLED/DISABLED now reset (`&r`) so labels stay gray.

### Changed
- Java package and Maven groupId moved from `me.aurautils` to `com.lozaine.aurautils`. The plugin main class is `com.lozaine.aurautils.AuraUtils`. Shaded bStats and FoliaLib relocate to `com.lozaine.aurautils.libs.*`.
- Release builds copy one shaded payload to two marketplace filenames: `AuraUtils-1.3.0-spigot.jar` and `AuraUtils-1.3.0-paper-folia.jar`. Bytecode is identical; FoliaLib scheduling and Paper async chunk loading are detected at runtime. Use the filename that matches the platform you are listing.

## [1.2.2] - 2026-08-27

### Fixed
- **Spigot watchdog on `/rtp`** — `getHighestBlockAt` no longer generates dozens of distant chunks on the main thread.
  Paper/Folia use `World#getChunkAtAsync` (detected at runtime). Spigot prefers loaded chunks and caps sync generation (`rtp.max-sync-generations`, default 3) plus a hard search timeout (`rtp.max-search-ticks`, default 200).
- **Folia TPA accept** — `/tpaccept` now snapshots the destination on the target thread and schedules the teleport on the requester's entity thread.
- **Teleport callbacks** — FoliaLib `teleportAsync` results hop back to the entity scheduler before sounds/messages.
- **False success** — "Teleported to ..." is sent only after the teleport future succeeds.

### Changed
- RTP search is one candidate at a time (no parallel `attemptsPerTick` burst).
- RTP keeps the player's yaw/pitch.
- Repeat `/rtp` cancels the previous search.

## [1.2.0] - 2026-08-26

### Added
- **Trusted / instant TPA** — add friends to a personal trusted list so they can `/tpa` you without confirmation.
  - `/tpatrust <player>` — add (one-way: they can TPA to you free of accept/deny)
  - `/tpatrust list` — show your list
  - `/tpatrust remove <player>` or `/tpauntrust <player>` — remove
  - Tab completion for online players and existing trusted names
  - Permission `aura.tpa.trust` (default: true)
- Config: `tpa.trusted-max` (default 50) and `tpa.trusted-instant` (default false — still uses shared countdown unless true or `aura.teleport.bypass`)

### Changed
- `TpaManager.sendRequest` now returns a `SendResult` (`PENDING` / `TRUSTED_INSTANT` / `BUSY` / `FAILED`)
- Trusted teleports schedule on the requester's entity thread (Folia-safe)
- Player trusted lists persist in `player-states.yml` under each player as `trusted: [uuid:Name, ...]`

### Technical
- `PlayerDataManager` uses `ConcurrentHashMap` / concurrent key-sets for toggles and trusted lists (safer under Folia region parallelism)
- `TpaManager` pending maps switched to `ConcurrentHashMap`
- Version **1.2.0**

## [1.1.1] - 2026-08-23

### Fixed
- **Folia RTP** — `getHighestBlockAt` was called from the player's entity region thread for distant chunks, causing `IllegalStateException: Cannot retrieve chunk asynchronously`. Candidates are now evaluated with `runAtLocation` on the owning region so height lookups are legal on Folia.
- **Folia `/back`** — On Folia, `PlayerTeleportEvent` does not fire for most teleports (including `PLUGIN` / async). `/back` always reported no previous location. Recording now happens in `TeleportHelper.teleportExact` before the teleport; the event listener remains a fallback. Back positions are stored as world-name + coordinates so World references cannot go null across regions.

### Technical
- RTP search: entity-timer generator + per-candidate region-scheduled safety check
- Atomic found/finished flags to stop the search cleanly when a safe spot is located
- `BackManager` durable snapshot; pre-teleport record in `TeleportHelper` (Spigot/Paper/Folia safe)

## [1.1.0] - 2026-08-23

### Added
- **Folia support** — full region-aware scheduling via FoliaLib.
  - `folia-supported: true` in plugin.yml
  - Entity schedulers for teleport countdowns, fly re-apply, and RTP search
  - Async teleport when the platform supports it (Paper / Folia)
  - Same JAR runs on Spigot, Paper, Purpur, and Folia
- Compatible with Geyser/Floodgate (Bedrock) out of the box via standard Bukkit APIs

### Changed
- All BukkitScheduler / BukkitRunnable usage replaced with FoliaLib wrappers
- TeleportHelper countdowns now follow the player across regions on Folia
- Version bumped to 1.1.0

### Technical
- Shaded & relocated FoliaLib (`me.aurautils.libs.folialib`)
- New `SchedulerHelper` facade for clean cross-platform scheduling

## [1.0.0] - 2026-08-22

### Added
- Core utility commands:
  - `/back`, `/home`, `/sethome`, `/delhome`
  - `/warp`, `/setwarp`, `/delwarp`
  - `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel`
  - `/fly`, `/god`, `/nofall`, `/nohunger`
  - `/rtp`, `/menu`, `/aura`
- Random safe teleport (`/rtp`) with bounded retries, minimum distance, and surface safety checks.
- **Shared teleport countdown system** used by home, warp, back, TPA, RTP, and menu clicks:
  - Display modes: `chat` | `actionbar` | `both` | `none`
  - Configurable chat reminders via `teleport.chat-at` (default `[3, 2, 1]` — start is always announced)
  - Optional **title / subtitle** countdown (`teleport.title`)
  - Cancel-on-move and cancel-on-damage
  - Feedback sounds with optional **rising pitch** as the countdown nears zero
- Permission **`aura.teleport.bypass`** (default: op) — skip countdown for instant teleports
- Granular permission nodes (see `plugin.yml`) including the `aura.admin` parent node
- Per-player persistence for god / fly / nofall / nohunger (load on join, save on quit, forced sync write on toggle)
- Simple utility GUI (`/menu`)
- Configuration-driven defaults in `config.yml` with detailed comments
- **bStats metrics** (plugin id 33574) with custom charts for config choices and feature usage
  - Opt-out via the shared `plugins/bStats/config.yml` (`enabled: false`)

### Fixed (pre-release)
- **TPA pending**: `/tpacancel` now also cancels outgoing TPA requests (not only teleport countdowns).
- **TPA accept**: requester and target both receive a clear accept message before the countdown starts.
- **TPA accept/deny**: permission check for `aura.tpa` enforced on the commands (menu already checked).
- **TPA timeout**: minimum timeout clamped to 1 second to avoid zero/negative scheduler delays.
- **Fly (Spigot)**: `allowFlight` is re-applied at 1, 5, and 20 ticks after join, world change, respawn, and cross-world teleport. Spigot 26.x clears player abilities more aggressively than Paper; a single early call was often overwritten.
- **Fly**: world-change / respawn no longer force the player into a flying state; only the ability to fly is restored.
- **Fly**: disabling fly leaves Creative / Spectator flight alone; game-mode changes re-sync the stored fly state.
- **God**: enabling god fully heals, clears fire, and fills food/saturation; combust events are cancelled while god is active.
- **NoHunger**: saturation and exhaustion are kept full / zero; loss is blocked and effects re-applied on food-change events.
- **Persistence**: god / fly / nofall / nohunger toggles force a synchronous disk write so a crash cannot lose the latest state.

### Compatibility
- **Minecraft**: 1.21.x (tested through 1.21.11) **and** 26.1 / 26.2 (year-based numbering)
- **Server software**: Spigot and Paper
- **Java**: 21+ (plugin bytecode); 26.1+ servers themselves require Java 25
- Single JAR works on both version lines — no separate builds required

### Build
- Maven project targeting Java 21
- Spigot API 1.21.4 (provided)
- bStats 3.2.1 shaded and relocated to `me.aurautils.libs.bstats`
- Artifact: `AuraUtils-1.0.0.jar`

### Notes
- Install by dropping the JAR into `plugins/` and restarting the server.
- See `README.md` for usage, configuration, and permissions.
- Metrics disclosure and the standard bStats opt-out satisfy SpigotMC resource guidelines.
- `/tpacancel` (aliases: `/tpcancel`, `/auracancel`) cancels any pending countdown from home, warp, back, TPA, or RTP.
