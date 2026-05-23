# AuraUtils

**Version 1.0.0** (released 2026-05-20)

AuraUtils is a lightweight, configurable Spigot/Paper plugin that bundles essential player utilities and admin tools for Minecraft servers (**1.21+**, **Java 25+**).

It provides warps and homes, teleport requests (including TPA Here), random safe teleport, flight and god toggles, `/back`, and a simple in-game menu — driven by `config.yml`, permission nodes, and **MiniMessage** locale files.

### Why AuraUtils?

AuraUtils is a good fit if you want a lean, modern, self-contained plugin you can read, configure, and extend without wading through a massive codebase. It is not trying to replace [EssentialsX](https://github.com/EssentialsX/Essentials) feature-for-feature — it is a focused alternative for servers that want the usual utilities without bundling everything under the sun.

**Compared to Essentials-style messaging:** player-facing text lives in `messages/<locale>.yml` with [MiniMessage](https://docs.advntr.dev/minimessage/index.html) from day one (gradients, hex, hover/click). Operators edit YAML instead of hunting `&` codes in Java.

## Quick Start

1. Build or download the plugin JAR and place it in your server's `plugins/` folder.
2. Start or restart the server.
3. Edit `plugins/AuraUtils/config.yml` (TPA, RTP, teleport countdown, home limits, locale defaults). On **Spigot**, set `rtp.only-loaded-chunks: true`; on **Paper**, use the defaults for async RTP.
4. Customize chat in `plugins/AuraUtils/messages/en.yml` (prefix, colors, teleport countdown hover text).
5. In-game, run `/aura` or `/menu` to explore utilities.

On first run, bundled `messages/en.yml` and `messages/es.yml` are copied into `plugins/AuraUtils/messages/`.

## Features

- **Warps & homes** — Set, delete, and teleport via command or GUI; tab completion for names.
- **TPA & TPA Here** — Request to teleport to a player (`/tpa`) or ask them to come to you (`/tpahere`); accept/deny with `/tpaccept` and `/tpadeny`.
- **Async-safe random teleport** — `/rtp` is a headline feature: Paper uses async chunk loading so search does not block the main thread; Spigot uses conservative search on already-loaded chunks (best with pre-generated worlds). All teleports load destination chunks only when needed, not at countdown start.
- **Back** — `/back` returns players to their last teleport destination.
- **Player toggles** — `/fly`, `/god`, `/nofall`, `/nohunger` (self or others with `.others` permissions).
- **Utility menu** — `/menu` GUI for warps, homes, pending TPA, and back.
- **Per-warp permissions** — Restrict individual warps with nodes like `aura.warp.spawn`, while keeping a clean admin node at `aura.warp.admin`.
- **Teleport countdown** — Shared delay for warp, home, back, and TPA; RTP has its own countdown. During countdown, players see a **clickable cancel** control (`/auracanceltp`).
- **Persistence** — Warps, homes, per-player toggles, optional locale override, and server spawn data saved to disk.
- **MiniMessage locales** — All player chat uses message keys; add `messages/de.yml`, etc., for translations.
- **Spigot + Paper, one JAR** — Runtime platform detection: Paper uses async chunk loads, `hasChangedPosition()`, and event-based teleport cancel; Spigot falls back safely without requiring a separate build.

## Random teleport (async-safe)

`/rtp` searches for a safe surface (solid floor, two air blocks above) within a configurable radius and minimum distance. The implementation is tuned so **slow disks do not freeze the server tick** for every player.

| | **Paper** | **Spigot** |
|---|-----------|------------|
| Chunk loading during search | `getChunkAtAsync` (reflection; no sync `chunk.load()` in the search loop) | Only evaluates **already-loaded** chunks when `rtp.only-loaded-chunks: true` (recommended) |
| Attempt pacing | Spread across ticks (`rtp.attemptsPerTick`); cap concurrent async loads (`rtp.max-pending-chunk-loads`) | Same tick batching; skips unloaded chunks instead of loading them |
| Terrain generation | Off by default (`rtp.generate-chunks: false`); enable only if you want new terrain during RTP | N/A during search — use a pre-generated world for reliable results |
| Final teleport | Main thread only, after chunk is ready | Chunk should already be loaded from search; optional async load on Paper |

**Other teleports** (warp, home, back, TPA, RTP countdown) no longer call synchronous chunk load when the countdown **starts**. The destination chunk is loaded when the countdown **finishes** — asynchronously on Paper, or with a single optional sync fallback on Spigot (`teleport.sync-chunk-fallback`).

### Recommended settings

**Paper (default-friendly):**

```yaml
rtp:
  # only-loaded-chunks: false   # omit or false — enables async search
  generate-chunks: false
  async-urgent: true
  max-pending-chunk-loads: 4
teleport:
  async-chunk-load: true
```

**Spigot:**

```yaml
rtp:
  only-loaded-chunks: true      # required for non-blocking search
  attemptsPerTick: 10           # keep moderate on busy servers
teleport:
  sync-chunk-fallback: true     # one sync load at teleport time if chunk unloaded
```

If RTP fails often on Spigot, pre-generate the world (e.g. Chunky) or lower `rtp.minDistance` / raise `rtp.attempts`. Message keys: `rtp.searching`, `rtp.failed`, `teleport.chunk-unavailable`.

## Architecture (single JAR)

AuraUtils compiles against the Spigot API and detects Paper at runtime. No separate Paper artifact is required.

```
me.aurautils/
├── platform/          PlatformAdapter, ChunkLoadPolicy, Spigot/Paper adapters, PlatformFactory
├── managers/          MessagesManager, TeleportHelper, AsyncRtpEngine, …
├── listeners/paper/   Paper-only listeners (e.g. movement during teleport countdown)
├── commands/
├── messages/          Bundled en.yml, es.yml (copied to data folder on first run)
└── util/              MessageUtil (legacy/menu), CommandUtil, …
```

- **Chunk loading** uses `PlatformAdapter.whenChunkReady()` with `ChunkLoadPolicy` (`LOADED_ONLY`, `ASYNC`, `SYNC_FALLBACK`) — never bulk `chunk.load()` during RTP search.
- **RTP search** runs in `AsyncRtpEngine`: Spigot conservative path vs Paper async chunk queue, both with per-tick attempt limits.
- **Teleport countdown** stores the destination up front but loads the chunk only when the timer completes.
- **Adventure / MiniMessage** is shaded into the JAR (relocated) so rich text works on both platforms; Paper delivers native components when available.

## Messages & localization

| File | Purpose |
|------|---------|
| `plugins/AuraUtils/messages/en.yml` | Default English strings (MiniMessage) |
| `plugins/AuraUtils/messages/<locale>.yml` | Additional languages (e.g. `es.yml`) |
| `config.yml` → `messages.*` | Default locale, fallback, Paper client locale |

Every message key can use `<prefix>` (from `meta.prefix` in the locale file). Example:

```yaml
meta:
  prefix: "<dark_gray>[<gradient:#00d2ff:#3a7bd5>Aura</gradient>]</dark_gray> "

teleport:
  countdown: "<prefix><yellow>Teleporting in <gold><seconds></gold>… <hover:show_text:'<gray>Click to cancel'><click:run_command:/auracanceltp><red><bold>✕</bold></red></click></hover>"
```

**Locale resolution (per player):**

1. Override from `/aura locale <code>` (stored in `player-states.yml`)
2. Paper client locale (if `messages.use-client-locale: true` and a matching file exists)
3. `messages.default-locale` from `config.yml`
4. Missing keys fall back to `messages.fallback-locale`

Reload messages with `/aura reload` (`aura.admin`).

## Compatibility with ResourceWorldResetter

AuraUtils is designed to work alongside [**ResourceWorldResetter (RWR)**](https://github.com/TamaWish/ResourceWorldResetter). RWR handles resource-world resets and world selection; AuraUtils supplies player teleport utilities (including RTP and `/back`).

| Plugin | Role |
|--------|------|
| **RWR** | Scheduled resets, admin GUI, **`/rwr tp`** — GUI to pick and enter the resource world |
| **AuraUtils** | **`/rtp`**, **`/back`**, homes, warps, TPA, and related teleports on any world (including the resource world) |

RWR does not provide `/rwr tp random` or `/rwr back`; use AuraUtils for those. There are no overlapping commands between the two plugins.

| AuraUtils command | With RWR |
|-------------------|----------|
| `/rtp` | Async-safe random teleport, including in the resource world |
| `/back` | Last teleport location (AuraUtils only) |
| `/home`, `/warp`, `/tpa` | Works in all worlds; safe alongside RWR |

**During a reset:** RWR moves players out of the resource world before deleting it. An AuraUtils teleport countdown in that world may cancel if the player is sent to another world first — that is expected.

Install both JARs in `plugins/`, configure RWR’s `worldName` and `teleport.defaultWorld`, and use **`/rwr tp`** when entering the resource world. No extra AuraUtils config is required.

**Spawn & keep inventory:** Use **`/setspawn`** before or after resets; spawn coordinates are stored in `server-spawns.yml` and reapplied when the world loads. **`/keepinventory`** sets the `keepInventory` gamerule on all worlds (including newly loaded resource worlds after a reset).

## Commands

| Command | Usage | Description |
|---------|-------|-------------|
| `/back` | — | Return to your last teleport location |
| `/home` | `[name\|list]` | Teleport to a home, open the home GUI, or go directly when you have only one home |
| `/sethome` | `<name>` | Set a home at your location |
| `/delhome` | `<name>` | Delete a home |
| `/warp` | `[name\|list]` | Teleport to a warp, open the warp GUI, or list warps |
| `/setwarp` | `<name>` | Create or update a warp |
| `/delwarp` | `<name>` | Delete a warp |
| `/tpa` | `<player>\|list` | Request to teleport to a player, or open the TPA GUI |
| `/tpahere` | `<player>` | Ask a player to teleport to you |
| `/tpaccept` | — | Accept a pending TPA / TPA Here request |
| `/tpadeny` | — | Deny a pending request |
| `/rtp` | — | Random safe teleport (respects cooldown) |
| `/fly` | `[player]` | Toggle flight |
| `/god` | `[player]` | Toggle invincibility |
| `/nofall` | `[player]` | Toggle fall damage |
| `/nohunger` | `[player]` | Toggle hunger depletion |
| `/menu` | — | Open the utility GUI |
| `/setspawn` | — | Set spawn for your current world (saved for RWR world reloads) |
| `/keepinventory` | `[on\|off\|status]` | Toggle or set server-wide keep inventory on death |
| `/auracanceltp` | — | Cancel a pending teleport countdown (also clickable in chat) |
| `/aura` | `[reload\|locale …]` | Plugin info, toggle status, reload config/messages, set language |

**Aliases:** `homelist` → `/home list`, `warplist` → `/warp list`, `tplist` → `/tpa list`

### `/aura` subcommands

| Subcommand | Permission | Description |
|------------|------------|-------------|
| `reload` | `aura.admin` | Reload `config.yml` and all `messages/*.yml` |
| `locale <code>` | `aura.use` | Set your message language (e.g. `es`) |
| `locale clear` | `aura.use` | Clear override; use client/default locale again |

## Requirements

- **Java 25+**
- **Spigot or Paper 1.21+** (Paper strongly recommended for async RTP, client locale, and smoother teleport movement detection)

## Building

```bash
mvn clean package
```

On this repository, the bundled Maven wrapper is also available:

```powershell
tools/apache-maven-3.9.6/bin/mvn.cmd clean package
```

Output in `target/`:

- `AuraUtils-1.0.0.jar` — shaded JAR (includes Adventure MiniMessage; use this on the server)
- `original-AuraUtils-1.0.0.jar` — unshaded build (if present)

## Configuration

After first run, see `plugins/AuraUtils/config.yml`:

```yaml
tpa:
  timeout: 60                    # Seconds before a request expires

rtp:
  center-on-player: true         # Search around player; false = world spawn
  radius: 2000
  minDistance: 100
  attempts: 80
  attemptsPerTick: 10            # Max new attempts started per tick
  # only-loaded-chunks: true     # Spigot: set true. Paper: omit/false for async search
  generate-chunks: false         # Paper: generate terrain during async RTP (usually false)
  async-urgent: true             # Paper: prioritize RTP chunk loads
  max-pending-chunk-loads: 4     # Paper: concurrent async loads per active /rtp
  countdown: 0                   # RTP-specific delay (0 = instant)
  cooldown: 60                   # Seconds between successful RTP (0 = off)

teleport:
  countdown: 5                   # Warp, home, back, TPA (0 = instant)
  async-chunk-load: true         # Paper: load destination chunk before teleport
  sync-chunk-fallback: true      # Spigot: allow one sync load at teleport time if needed

homes:
  default-limit: 3               # Base home limit when no rank-specific node matches
  permission-limits:
    - aura.home.limit.5
    - aura.home.limit.10
    - aura.home.limit.25

server:
  keep-inventory: false

messages:
  default-locale: en
  fallback-locale: en
  use-client-locale: true        # Paper: match messages/<locale>.yml to client language
```

**Chat appearance** is configured in `plugins/AuraUtils/messages/en.yml` (and other locale files), not in `config.yml`. See [Messages & localization](#messages--localization).

### Data files

| Path | Contents |
|------|----------|
| `config.yml` | Gameplay settings |
| `messages/*.yml` | MiniMessage strings per locale |
| `warps.yml` | Warp locations |
| `homes.yml` | Player homes |
| `player-states.yml` | Toggles, optional `locale` override |
| `server-spawns.yml` | Per-world spawn points from `/setspawn` |

## Permissions

| Permission | Default | Description |
|------------|---------:|-------------|
| `aura.use` | `true` | Basic access (`/aura`, `/menu`, `/auracanceltp`) |
| `aura.menu` | `true` | Open the utility menu |
| `aura.back` | `true` | Use `/back` |
| `aura.warp` | `true` | Use all warps (see per-warp nodes below) |
| `aura.warp.<name>` | — | Use a specific warp (e.g. `aura.warp.spawn`) |
| `aura.warp.admin` | `op` | Administrative warp access |
| `aura.warp.set` | `op` | Create/update warps |
| `aura.warp.delete` | `op` | Delete warps |
| `aura.home` | `true` | Use home commands |
| `aura.home.set` | `true` | Set homes |
| `aura.home.delete` | `true` | Delete homes |
| `aura.home.limit.<number>` | — | Home limit nodes (e.g. `aura.home.limit.10`) |
| `aura.tpa` | `true` | `/tpa` |
| `aura.tpahere` | `true` | `/tpahere` |
| `aura.tpaccept` | `true` | `/tpaccept` |
| `aura.tpdeny` | `true` | `/tpadeny` |
| `aura.god` | `op` | Toggle god mode (self) |
| `aura.god.others` | `op` | Toggle god mode on others |
| `aura.fly` | `op` | Toggle fly (self) |
| `aura.fly.others` | `op` | Toggle fly on others |
| `aura.nofall` | `op` | Toggle fall damage (self) |
| `aura.nofall.others` | `op` | Toggle fall damage on others |
| `aura.nohunger` | `op` | Toggle hunger (self) |
| `aura.nohunger.others` | `op` | Toggle hunger on others |
| `aura.rtp` | `true` | Use `/rtp` |
| `aura.rtp.cooldown.bypass` | `op` | Ignore RTP cooldowns |
| `aura.setspawn` | `op` | Set world spawn with `/setspawn` |
| `aura.keepinventory` | `op` | Toggle server-wide keep inventory |
| `aura.admin` | `op` | All AuraUtils permissions |

### Per-warp access

Players can use a warp if they have **any** of:

- `aura.admin`
- `aura.warp.admin`
- `aura.warp.<warpname>` (lowercase name, e.g. `aura.warp.spawn`)
- `aura.warp` (grants every warp)

For restricted servers, omit `aura.warp` and grant only the nodes you need.

The authoritative list is in [plugin.yml](src/main/resources/plugin.yml).

## Extending messages

1. Copy `messages/en.yml` to `messages/<code>.yml` (e.g. `de`, `fr`, `pt_br` → use `pt` after normalization).
2. Translate values; **keep keys identical** to English.
3. Run `/aura reload` or restart the server.
4. Optional: players run `/aura locale de` to force a language.

Keys use dot notation (`teleport.countdown`, `tpa.sent`, …). Placeholders are angle-bracket tags (`<player>`, `<seconds>`, …).

## License

See repository license file (if present).

## Support

Open an issue in the project repository for bugs or feature requests.
