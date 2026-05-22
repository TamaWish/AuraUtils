# AuraUtils

**Version 1.0.0** (released)

AuraUtils is a lightweight, configurable Spigot/Paper plugin that bundles essential player utilities and admin tools for Minecraft servers (recommended for **1.21+**).

It provides warps and homes, teleport requests (including TPA Here), random safe teleport, flight and god toggles, `/back`, and a simple in-game menu — all driven by `config.yml` and permissions.

### Why AuraUtils?

AuraUtils is a good fit if you want a lean, modern, self-contained plugin you can read, configure, and extend without wading through a massive codebase. It is not trying to replace [EssentialsX](https://github.com/EssentialsX/Essentials) feature-for-feature — it is a focused alternative for servers that want the usual utilities without bundling everything under the sun.

## Quick Start

1. Build or download the plugin JAR and place it in your server's `plugins/` folder.
2. Start or restart the server.
3. Edit `plugins/AuraUtils/config.yml` (TPA timeout, RTP radius/cooldown, teleport countdown, home limits, prefix).
4. In-game, run `/aura` or `/menu` to explore utilities.

## Features

- **Warps & homes** — Set, delete, and teleport via command or GUI; tab completion for names.
- **TPA & TPA Here** — Request to teleport to a player (`/tpa`) or ask them to come to you (`/tpahere`); accept/deny with `/tpaccept` and `/tpadeny`.
- **Random teleport** — `/rtp` finds a safe surface near world spawn with configurable radius, distance, retries, countdown, and cooldown.
- **Back** — `/back` returns players to their last teleport destination.
- **Player toggles** — `/fly`, `/god`, `/nofall`, `/nohunger` (self or others with `.others` permissions).
- **Utility menu** — `/menu` GUI for warps, homes, pending TPA, and back.
- **Per-warp permissions** — Restrict individual warps with nodes like `aura.warp.spawn`.
- **Teleport countdown** — Shared delay for warp, home, back, and TPA (configurable; RTP has its own countdown).
- **Persistence** — Warps, homes, and per-player toggle state saved to disk.

## Compatibility with ResourceWorldResetter

AuraUtils is designed to work alongside [**ResourceWorldResetter (RWR)**](https://github.com/TamaWish/ResourceWorldResetter). RWR handles resource-world resets and world selection; AuraUtils supplies player teleport utilities (including RTP and `/back`).

| Plugin | Role |
|---|---|
| **RWR** | Scheduled resets, admin GUI, **`/rwr tp`** — GUI to pick and enter the resource world |
| **AuraUtils** | **`/rtp`**, **`/back`**, homes, warps, TPA, and related teleports on any world (including the resource world) |

RWR does not provide `/rwr tp random` or `/rwr back`; use AuraUtils (or another RTP/back plugin) for those. There are no overlapping commands between the two plugins.

| AuraUtils command | With RWR |
|---|---|
| `/rtp` | Use for random teleport, including in the resource world |
| `/back` | Last teleport location (AuraUtils only) |
| `/home`, `/warp`, `/tpa` | Works in all worlds; safe alongside RWR |

**During a reset:** RWR moves players out of the resource world before deleting it. An AuraUtils teleport countdown in that world may cancel if the player is sent to another world first — that is expected.

Install both JARs in `plugins/`, configure RWR’s `worldName` and `teleport.defaultWorld`, and use **`/rwr tp`** to open the world GUI when entering the resource world. No extra AuraUtils config is required.

## Commands

| Command | Usage | Description |
|---|---|---|
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
| `/aura` | `[reload]` | Plugin info, your toggle status, reload config (`aura.admin`) |

**Aliases:** `homelist` → `/home list`, `warplist` → `/warp list`, `tplist` → `/tpa list`

## Requirements

- **Java 25+**
- **Spigot / Paper 1.21+**

## Building

```bash
mvn clean package
```

The compiled JAR is written to `target/` (e.g. `AuraUtils-1.0.0.jar`).

## Configuration

See `plugins/AuraUtils/config.yml` after first run. Main options:

```yaml
tpa:
  timeout: 60                    # Seconds before a request expires

rtp:
  radius: 2000                   # Search radius around world spawn (blocks)
  minDistance: 250               # Minimum horizontal distance from current position
  attempts: 30                   # Random location attempts before giving up
  attemptsPerTick: 5             # Attempts per tick (spreads server load)
  countdown: 5                   # Delay before RTP executes (0 = instant)
  cooldown: 60                   # Seconds between successful RTP uses (0 = disabled)

teleport:
  countdown: 5                   # Delay for warp, home, back, and TPA (0 = instant)

homes:
  max-per-player: 5              # Home limit per player (-1 = unlimited)

prefix: "&8[&bAura&8] &r"        # Chat prefix (& color codes)
```

## Permissions

| Permission | Default | Description |
|---|---:|---|
| `aura.use` | `true` | Basic access (`/aura`, `/menu`) |
| `aura.menu` | `true` | Open the utility menu |
| `aura.back` | `true` | Use `/back` |
| `aura.warp` | `true` | Use all warps (see per-warp nodes below) |
| `aura.warp.<name>` | — | Use a specific warp (e.g. `aura.warp.spawn`) |
| `aura.warp.*` | `op` | All per-warp nodes |
| `aura.warp.set` | `op` | Create/update warps |
| `aura.warp.delete` | `op` | Delete warps |
| `aura.home` | `true` | Use home commands |
| `aura.home.set` | `true` | Set homes |
| `aura.home.delete` | `true` | Delete homes |
| `aura.tpa` | `true` | `/tpa`, `/tpahere`, `/tpaccept`, `/tpadeny` |
| `aura.god` | `op` | Toggle god mode (self) |
| `aura.god.others` | `op` | Toggle god mode on others |
| `aura.fly` | `op` | Toggle fly (self) |
| `aura.fly.others` | `op` | Toggle fly on others |
| `aura.nofall` | `op` | Toggle fall damage (self) |
| `aura.nofall.others` | `op` | Toggle fall damage on others |
| `aura.nohunger` | `op` | Toggle hunger (self) |
| `aura.nohunger.others` | `op` | Toggle hunger on others |
| `aura.rtp` | `true` | Use `/rtp` |
| `aura.admin` | `op` | All AuraUtils permissions |

### Per-warp access

Players can use a warp if they have **any** of:

- `aura.admin`
- `aura.warp.<warpname>` (lowercase name, e.g. `aura.warp.spawn`)
- `aura.warp.*`
- `aura.warp` (grants every warp)

For restricted servers, omit `aura.warp` and grant only the nodes you need, for example `aura.warp.spawn` and `aura.warp.shop`.

The authoritative list is in [plugin.yml](src/main/resources/plugin.yml).

## License

See repository license file (if present).

## Support

Open an issue in the project repository for bugs or feature requests.
