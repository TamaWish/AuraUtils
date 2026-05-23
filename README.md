# AuraUtils

**Version 1.0.0** · Spigot/Paper **1.21+** · **Java 25+**

AuraUtils is a lightweight utility plugin for Minecraft servers: warps, homes, TPA, async-safe random teleport, player toggles, heal/feed, `/back`, spawn management, and a `/menu` GUI — configured through `config.yml`, permission nodes, and **MiniMessage** locale files.

### Why AuraUtils?

AuraUtils targets servers that want common player utilities in one small, readable codebase — not a full [EssentialsX](https://github.com/EssentialsX/Essentials) replacement. Player-facing text lives in `messages/<locale>.yml` with [MiniMessage](https://docs.advntr.dev/minimessage/index.html) (gradients, hex, hover/click) instead of hard-coded `&` codes in Java.

## Quick start

1. **Build** the plugin (see [Building](#building) or [docs/BUILD.md](docs/BUILD.md)) or download a release JAR.
2. Place **`AuraUtils-1.0.0.jar`** in the server `plugins/` folder (only one copy).
3. **Restart** the server (full restart recommended after install or upgrades).
4. Edit `plugins/AuraUtils/config.yml` — TPA, RTP, teleport countdown, home limits, feature toggles, locales.
5. Customize chat in `plugins/AuraUtils/messages/en.yml` (prefix, colors, countdown cancel link).
6. On **Spigot**, set `rtp.only-loaded-chunks: true` for non-blocking RTP; on **Paper**, use the defaults for async search.
7. In-game: `/aura` for help and toggle status, `/menu` for the utility GUI.

On first run, bundled `messages/en.yml` and `messages/es.yml` are copied into `plugins/AuraUtils/messages/`. Existing locale files on disk are **merged** with bundled defaults so new keys appear without wiping your edits.

## Features

| Area | What you get |
|------|----------------|
| **Warps & homes** | Set, delete, teleport via command or paginated GUI; tab completion |
| **TPA & TPA Here** | `/tpa`, `/tpahere`, `/tpaccept`, `/tpadeny` with timeout and teleport countdown |
| **Async-safe RTP** | `/rtp` with Paper async chunk loads or Spigot loaded-chunk-only search |
| **Back** | `/back` to the last AuraUtils teleport destination |
| **Spawn** | `/spawn` to saved or vanilla world spawn; `/setspawn` persists per world |
| **Toggles** | `/fly`, `/god`, `/nofall`, `/nohunger` (self or others with `.others`) |
| **Heal & feed** | `/heal` and `/feed` (self or others with `.others`) |
| **Utility menu** | `/menu` — warps, homes, TPA, spawn, heal, feed, back; paged warp/home lists |
| **Per-warp permissions** | `aura.warp.<name>` or `aura.warp` for all; `aura.warp.admin` for operators |
| **Teleport countdown** | Shared delay for warp, home, back, TPA, spawn; separate RTP countdown; clickable `/auracanceltp` |
| **Feature toggles** | Disable commands and related behavior per feature in `config.yml` |
| **Locales** | MiniMessage `messages/*.yml`; Paper client locale; `/aura locale` override |
| **One JAR** | Runtime Spigot/Paper detection — no separate Paper build |

## Utility menu (`/menu`)

The main menu (27 slots) opens sub-menus and shortcuts:

- **Warps / Homes** — 54-slot paginated lists (45 entries per page); previous/next, main menu, close
- **TPA** — Accept/deny pending request with requester head and type hint (TPA vs TPA Here)
- **Spawn, Heal, Feed** — Runs `/spawn`, `/heal`, `/feed` (respects permissions and feature toggles)
- **Back** — Same behavior as `/back`
- **Refresh** — Rebuilds the current menu page

Menu item labels use legacy color codes via `MessageUtil`; chat messages use MiniMessage from locale files.

## Random teleport (async-safe)

`/rtp` searches for a safe surface (solid floor, two air blocks above) within a configurable radius and minimum distance. Search work is spread across ticks so slow disks are less likely to freeze the main thread.

| | **Paper** | **Spigot** |
|---|-----------|------------|
| Chunk loading during search | `getChunkAtAsync` (reflection); no sync `chunk.load()` in the search loop | Only **already-loaded** chunks when `rtp.only-loaded-chunks: true` (recommended) |
| Attempt pacing | `rtp.attemptsPerTick`; cap concurrent async loads (`rtp.max-pending-chunk-loads`) | Same tick batching; skips unloaded chunks |
| Terrain generation | Off by default (`rtp.generate-chunks: false`) | Pre-generate worlds for large radii |
| Final teleport | Main thread after chunk is ready | Chunk should already be loaded from search |

**Other teleports** (warp, home, back, TPA, spawn, RTP after countdown) do **not** load destination chunks when the countdown **starts**. The chunk loads when the countdown **finishes** — async on Paper, or optional sync fallback on Spigot (`teleport.sync-chunk-fallback`).

### Recommended settings

**Paper:**

```yaml
rtp:
  generate-chunks: false
  async-urgent: true
  max-pending-chunk-loads: 4
teleport:
  async-chunk-load: true
```

**Spigot:**

```yaml
rtp:
  only-loaded-chunks: true
  attemptsPerTick: 10
teleport:
  sync-chunk-fallback: true
```

If RTP fails often on Spigot, pre-generate the world (e.g. Chunky), lower `rtp.minDistance`, or raise `rtp.attempts`. Message keys: `rtp.searching`, `rtp.failed`, `teleport.chunk-unavailable`.

## Feature toggles

Under `features:` in `config.yml`, set any key to `false` to disable that feature’s commands (and `requireFeature` checks). Disabled features respond with `feature.disabled` from your locale file.

| Key | Affects |
|-----|---------|
| `menu` | `/menu` |
| `homes` | `/home`, `/sethome`, `/delhome` |
| `warps` | `/warp`, `/setwarp`, `/delwarp` |
| `spawn` | `/spawn`, `/setspawn` |
| `tpa` | `/tpa`, `/tpahere` (accept/deny still work for pending requests) |
| `rtp` | `/rtp` |
| `fly`, `god`, `nofall`, `nohunger` | Matching toggle commands |
| `heal`, `feed` | `/heal`, `/feed` |
| `keepinventory` | `/keepinventory` |

Reload with `/aura reload` after changes.

## Architecture (single JAR)

Compiles against the Spigot API; detects Paper at runtime. No separate Paper artifact.

```
me.aurautils/
├── platform/          PlatformAdapter, ChunkLoadPolicy, Spigot/Paper adapters
├── managers/          MessagesManager, TeleportHelper, AsyncRtpEngine, …
├── listeners/paper/   PaperMoveListener (movement during teleport countdown)
├── menus/             MenuManager, UtilityMenuHolder, MenuType
├── commands/
└── util/              MessageUtil, CommandUtil, WarpPermissions, …
```

- **Chunk loading:** `PlatformAdapter.whenChunkReady()` with `ChunkLoadPolicy` (`LOADED_ONLY`, `ASYNC`, `SYNC_FALLBACK`).
- **RTP:** `AsyncRtpEngine` — Spigot conservative path vs Paper async chunk queue.
- **Adventure / MiniMessage** shaded into the JAR (`me.aurautils.lib.kyori`); Paper uses native components when available.

## Messages & localization

| File | Purpose |
|------|---------|
| `plugins/AuraUtils/messages/en.yml` | Default English (MiniMessage) |
| `plugins/AuraUtils/messages/<locale>.yml` | Additional languages (e.g. `es.yml`) |
| `config.yml` → `messages.*` | Default locale, fallback, Paper client locale |

Every message key can use `<prefix>` from `meta.prefix` in the locale file. Example:

```yaml
meta:
  prefix: "<dark_gray>[<gradient:#00d2ff:#3a7bd5>Aura</gradient>]</dark_gray> "

teleport:
  countdown: "<prefix><yellow>Teleporting in <gold><seconds></gold>… <hover:show_text:'<gray>Click to cancel'><click:run_command:/auracanceltp><red><bold>✕</bold></red></click></hover>"
```

**Locale resolution (per player):**

1. `/aura locale <code>` override (`player-states.yml`)
2. Paper client locale (if `messages.use-client-locale: true` and file exists)
3. `messages.default-locale`
4. Missing keys → `messages.fallback-locale`

Reload: `/aura reload` (`aura.admin`).

## Compatibility with ResourceWorldResetter

Works alongside [**ResourceWorldResetter (RWR)**](https://github.com/TamaWish/ResourceWorldResetter):

| Plugin | Role |
|--------|------|
| **RWR** | Scheduled resets, admin GUI, **`/rwr tp`** into the resource world |
| **AuraUtils** | **`/rtp`**, **`/back`**, homes, warps, TPA on any world |

No overlapping commands. Use **`/setspawn`** so spawn coordinates in `server-spawns.yml` survive world recreation; **`/keepinventory`** applies the gamerule when worlds load after a reset.

During a reset, players moved out of the resource world may cancel an in-progress AuraUtils countdown — expected.

## Commands

| Command | Usage | Permission (base) |
|---------|-------|-------------------|
| `/back` | — | `aura.back` |
| `/home` | `[name\|list]` | `aura.home` |
| `/sethome` | `<name>` | `aura.home.set` |
| `/delhome` | `<name>` | `aura.home.delete` |
| `/warp` | `[name\|list]` | `aura.warp` |
| `/setwarp` | `<name>` | `aura.warp.set` |
| `/delwarp` | `<name>` | `aura.warp.delete` |
| `/tpa` | `<player>\|list` | `aura.tpa` |
| `/tpahere` | `<player>` | `aura.tpahere` |
| `/tpaccept` | — | `aura.tpaccept` |
| `/tpadeny` | — | `aura.tpdeny` |
| `/rtp` | — | `aura.rtp` |
| `/spawn` | — | `aura.spawn` |
| `/setspawn` | — | `aura.setspawn` |
| `/fly` | `[player]` | `aura.fly` (+ `aura.fly.others`) |
| `/god` | `[player]` | `aura.god` (+ `aura.god.others`) |
| `/nofall` | `[player]` | `aura.nofall` (+ `.others`) |
| `/nohunger` | `[player]` | `aura.nohunger` (+ `.others`) |
| `/heal` | `[player]` | `aura.heal` (+ `aura.heal.others`) |
| `/feed` | `[player]` | `aura.feed` (+ `aura.feed.others`) |
| `/menu` | — | `aura.use` / `aura.menu` |
| `/keepinventory` | `[on\|off\|status]` | `aura.keepinventory` |
| `/auracanceltp` | — | `aura.use` |
| `/aura` | `[reload\|locale …]` | `aura.use` (`reload` → `aura.admin`) |

**Aliases:** `homelist` → `/home list`, `warplist` → `/warp list`, `tplist` → `/tpa list`

### `/aura` subcommands

| Subcommand | Permission | Description |
|------------|------------|-------------|
| `reload` | `aura.admin` | Reload `config.yml` and all `messages/*.yml`; re-apply keep inventory |
| `locale <code>` | `aura.use` | Set message language (e.g. `es`) |
| `locale clear` | `aura.use` | Clear override; use client/default again |

With no args, `/aura` prints command help and (for players) god/fly/nofall/nohunger status.

## Requirements

- **Java 25+**
- **Spigot or Paper 1.21+** (Paper recommended for async RTP, client locale, `hasChangedPosition()` countdown cancel)

## Building

Use **Maven** so the JAR is compiled with `javac`. Full details, troubleshooting, and CI notes: **[docs/BUILD.md](docs/BUILD.md)**.

```powershell
.\.maven\apache-maven-3.9.16\bin\mvn.cmd clean package
```

Deploy **`target/AuraUtils-1.0.0.jar`** to `plugins/` and restart. Do not ship IDE-built JARs while the workspace has compile errors.

## Configuration

After first run: `plugins/AuraUtils/config.yml`

```yaml
tpa:
  timeout: 60

rtp:
  center-on-player: true
  radius: 2000
  minDistance: 100
  attempts: 80
  attemptsPerTick: 10
  # only-loaded-chunks: true   # Spigot: set true
  generate-chunks: false
  async-urgent: true
  max-pending-chunk-loads: 4
  countdown: 0
  cooldown: 60

teleport:
  countdown: 5
  async-chunk-load: true
  sync-chunk-fallback: true

homes:
  default-limit: 3
  permission-limits:
    - aura.home.limit.5
    - aura.home.limit.10
    - aura.home.limit.25

server:
  keep-inventory: false

messages:
  default-locale: en
  fallback-locale: en
  use-client-locale: true

features:
  menu: true
  homes: true
  warps: true
  spawn: true
  tpa: true
  rtp: true
  fly: true
  god: true
  nofall: true
  nohunger: true
  heal: true
  feed: true
  keepinventory: true
```

**Chat text** is in `messages/*.yml`, not `config.yml`. See [Messages & localization](#messages--localization).

### Data files

| Path | Contents |
|------|----------|
| `config.yml` | Gameplay, RTP, teleports, features, locale defaults |
| `messages/*.yml` | MiniMessage strings per locale |
| `warps.yml` | Warp locations |
| `homes.yml` | Player homes |
| `player-states.yml` | Toggles, optional `locale` override |
| `server-spawns.yml` | Per-world spawn from `/setspawn` |

## Permissions

| Permission | Default | Description |
|------------|---------:|-------------|
| `aura.use` | `true` | `/aura`, `/menu`, `/auracanceltp` |
| `aura.menu` | `true` | Open utility menu |
| `aura.back` | `true` | `/back` |
| `aura.warp` | `true` | All warps (or use per-warp nodes) |
| `aura.warp.<name>` | — | Specific warp (lowercase) |
| `aura.warp.admin` | `op` | Admin warp access |
| `aura.warp.set` / `aura.warp.delete` | `op` | Manage warps |
| `aura.home` / `.set` / `.delete` | `true` | Home commands |
| `aura.home.limit.<n>` | varies | Home cap (`3`, `5`, `10`, `25` in `plugin.yml`) |
| `aura.tpa` / `tpahere` / `tpaccept` / `tpdeny` | `true` | TPA flow |
| `aura.spawn` | `true` | `/spawn` |
| `aura.setspawn` | `op` | `/setspawn` |
| `aura.rtp` | `true` | `/rtp` |
| `aura.rtp.cooldown.bypass` | `op` | Skip RTP cooldown |
| `aura.god` / `fly` / `nofall` / `nohunger` | `op` | Self toggles |
| `aura.*.others` | `op` | Toggle/heal/feed other players |
| `aura.heal` / `aura.feed` | `op` | Self heal/feed |
| `aura.heal.others` / `aura.feed.others` | `op` | Heal/feed others |
| `aura.keepinventory` | `op` | `/keepinventory` |
| `aura.admin` | `op` | All AuraUtils permissions |

### Per-warp access

A player may use warp `spawn` if they have any of: `aura.admin`, `aura.warp.admin`, `aura.warp.spawn`, or `aura.warp` (all warps). For restricted servers, omit `aura.warp` and grant only `aura.warp.<name>` nodes.

Authoritative definitions: [`plugin.yml`](src/main/resources/plugin.yml).

## Extending messages

1. Copy `messages/en.yml` to `messages/<code>.yml`.
2. Translate values; **keep keys identical**.
3. `/aura reload` or restart.
4. Optional: `/aura locale de` per player.

Placeholders use angle brackets: `<player>`, `<seconds>`, `<name>`, etc.

## Changelog & releases

- [CHANGELOG.md](CHANGELOG.md) — version history
- [releases/v1.0.0.md](releases/v1.0.0.md) — v1.0.0 release notes
- [docs/BUILD.md](docs/BUILD.md) — build and deploy guide

## License

[MIT License](LICENSE) — Copyright (c) 2026 TamaWish and AuraUtils contributors.

## Support

Open an issue in the project repository for bugs or feature requests.
