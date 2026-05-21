# AuraUtils

AuraUtils is a lightweight, configurable Spigot/Paper plugin that provides a curated set of essential player utilities and admin tools for Minecraft servers (recommended for 1.21+).

Designed for stability and ease-of-use, AuraUtils adds familiar quality-of-life features — warps and homes, teleport requests, random safe teleport (RTP), flight and god toggles, and a simple in-game menu — all driven by `config.yml` and permissions.

## Quick Start

- Drop the built `.jar` into your server's `plugins/` folder and start the server.
- Adjust `plugins/AuraUtils/config.yml` to tune behaviour (RT P radius, TPA timeout, prefixes, etc.).
- In-game, run `/aura` or `/menu` to explore available utilities.

## Features

The following features are implemented and ready to use:

- **Player utility commands:** `/back`, `/home`, `/sethome`, `/delhome`, `/warp`, `/setwarp`, `/delwarp`, `/tpa`, `/tpaccept`, `/tpadeny`, `/fly`, `/god`, `/nofall`, `/nohunger`, `/menu`, `/rtp`, `/aura`
- **Permissions:** Granular permissions are available (e.g. `aura.fly.others`, `aura.god.others`). See `plugin.yml` for exact nodes.
- **Player persistence:** Basic per-player data storage and load on join/quit.
- **Listeners:** Various listeners (back location tracking, menu interactions, and more) are registered and active.
- **Config-driven:** Many defaults and behaviours configurable via `config.yml`.

### Commands (at-a-glance)

| Command | Purpose |
|---|---|
| `/back` | Return to your last location |
| `/home` | Teleport to your home |
| `/sethome` | Set your current location as home |
| `/delhome` | Delete your home |
| `/warp <name>` | Teleport to a named warp |
| `/setwarp <name>` | Create or update a warp at your location |
| `/delwarp <name>` | Remove a named warp |
| `/tpa <player>` | Request to teleport to a player |
| `/tpaccept` | Accept a TPA request |
| `/tpadeny` | Deny a TPA request |
| `/fly [player]` | Toggle flight for yourself or another player |
| `/god [player]` | Toggle invincibility for yourself or another player |
| `/nofall [player]` | Toggle fall damage for yourself or another player |
| `/nohunger [player]` | Toggle hunger depletion for yourself or another player |
| `/menu` | Open the Aura utilities menu |
| `/rtp` | Randomly teleport to a safe location |
| `/aura` | Show plugin info and your current status |

## Requirements

- Java 21+
- Spigot / Paper **1.21+**

## Building

```bash
# Requires Maven and Java 21+
mvn clean package
```

The compiled `.jar` will be in `target/` (artifact name depends on your Maven coordinates).

## Installation

1. Drop the built `.jar` into your server's `plugins/` folder.
2. Start or restart the server.
3. Configure `plugins/AuraUtils/config.yml` as needed.

## Configuration (`config.yml`)

Key configuration options (see `resources/config.yml` for full details):

```yaml
tpa:
  timeout: 60               # Seconds before a TPA request expires
rtp:
  radius: 2000                    # Search radius around world spawn
  attempts: 30                    # Number of random location attempts

prefix: "&8[&bAura&8] &r"        # Chat prefix (supports & color codes)
```

## Permissions

Common permission nodes used by AuraUtils (defined in `plugin.yml`):

| Permission | Default | Description |
|---|---:|---|
| `aura.use` | `true` | Basic access to plugin info |
| `aura.menu` | `true` | Open the utility menu |
| `aura.back` | `true` | Use `/back` |
| `aura.warp` | `true` | Use warp commands |
| `aura.warp.set` | `op` | Create/update warps |
| `aura.warp.delete` | `op` | Delete warps |
| `aura.home` | `true` | Use home commands |
| `aura.home.set` | `true` | Set homes |
| `aura.home.delete` | `true` | Delete homes |
| `aura.tpa` | `true` | Use tpa/tpaccept/tpadeny |
| `aura.god` | `op` | Toggle god mode for self |
| `aura.god.others` | `op` | Toggle god mode on others |
| `aura.fly` | `op` | Toggle fly for self |
| `aura.fly.others` | `op` | Toggle fly for others |
| `aura.nofall` | `op` | Toggle fall damage for self |
| `aura.nofall.others` | `op` | Toggle fall damage for others |
| `aura.nohunger` | `op` | Toggle hunger for self |
| `aura.nohunger.others` | `op` | Toggle hunger for others |
| `aura.rtp` | `true` | Use random safe teleport |
| `aura.admin` | `op` | All AuraUtils permissions (children in `plugin.yml`) |

For the complete and authoritative list, see [plugin.yml](src/main/resources/plugin.yml).

For questions or help, open an issue in the project repository.
