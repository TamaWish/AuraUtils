# AuraUtils

Lightweight utility plugin for Spigot and Paper servers.

**Homes · Warps · TPA · Back · RTP · God · Fly · NoFall · NoHunger · GUI menu**

[![bStats](https://img.shields.io/badge/bStats-AuraUtils-00AA00?logo=data:image/svg+xml;base64,)](https://bstats.org/plugin/bukkit/AuraUtils/33574)
[![Modrinth](https://img.shields.io/badge/Modrinth-AuraUtils-1BD96A?logo=modrinth)](https://modrinth.com/project/aurautils)
[![GitHub](https://img.shields.io/badge/GitHub-TamaWish%2FAuraUtils-181717?logo=github)](https://github.com/TamaWish/AuraUtils)

---

## Features

| Feature | Commands | Notes |
|---------|----------|--------|
| **Homes** | `/home`, `/sethome`, `/delhome` | Per-player homes with optional GUI |
| **Warps** | `/warp`, `/setwarp`, `/delwarp` | Server warps with optional GUI |
| **TPA** | `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel` | Timed requests, cancelable countdown |
| **Back** | `/back` | Return to last teleport location |
| **RTP** | `/rtp` | Safe random teleport (bounded attempts) |
| **God** | `/god [player]` | Invincibility + heal / clear fire |
| **Fly** | `/fly [player]` | Persistent flight (Spigot & Paper safe) |
| **NoFall** | `/nofall [player]` | Toggle fall damage |
| **NoHunger** | `/nohunger [player]` | Toggle hunger depletion |
| **Menu** | `/menu` | Simple utility GUI |
| **Info** | `/aura` | Plugin info and command list |

All teleport actions share a configurable countdown (chat / actionbar / both / none), optional cancel-on-move and cancel-on-damage, and short feedback sounds.

---

## Requirements

| | |
|---|---|
| **Server** | Spigot or Paper |
| **Minecraft** | **1.21.x** (tested to 1.21.11) **and** **26.1 / 26.2** |
| **Java** | 21+ (26.1+ servers require Java 25; the plugin itself is compiled for Java 21 and runs on both) |

One JAR covers both the classic `1.21.x` line and the new `26.x` year-based line. No separate builds required.

---

## Installation

1. Download the latest `AuraUtils-x.y.z.jar`.
2. Place it in your server’s `plugins/` folder.
3. Restart (or reload carefully).
4. Edit `plugins/AuraUtils/config.yml` if desired, then run `/aura` or restart.

---

## Configuration

Default `config.yml` (key options):

```yaml
# TPA
tpa:
  timeout: 60                 # Seconds before a TPA request expires

# Random teleport
rtp:
  radius: 2000                # Search radius around world spawn
  minDistance: 250            # Minimum horizontal distance from current position
  attempts: 30                # Max random location attempts
  attemptsPerTick: 5          # Attempts spread per tick (reduces lag)
  countdown: 5                # Seconds before RTP; 0 = immediate

# Shared teleport behaviour (warp / home / back / tpa)
teleport:
  countdown: 5                # Seconds before teleport; 0 = instant
  countdown-display: both     # chat | actionbar | both | none
  chat-at: [3, 2, 1]          # Extra chat reminders at these seconds
  cancel-on-move: true
  cancel-on-damage: false
  sound: true

# Chat prefix (& color codes supported)
prefix: "&8[&bAura&8] &r"
```

---

## Metrics (bStats)

AuraUtils uses [bStats](https://bstats.org) to collect **anonymous** usage statistics (server version, player count range, selected config options, and feature usage counts).

- **No personal data** is collected.
- Metrics are **enabled by default**.
- To disable metrics for this plugin (and any other bStats plugins on the server), edit `plugins/bStats/config.yml` and set `enabled: false`, then restart.
- Full public charts: [https://bstats.org/plugin/bukkit/AuraUtils/33574](https://bstats.org/plugin/bukkit/AuraUtils/33574)

This disclosure satisfies SpigotMC resource guidelines for optional metrics.

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `aura.use` | true | Basic access / `/aura` |
| `aura.menu` | true | Open the utility menu |
| `aura.back` | true | Use `/back` |
| `aura.warp` | true | Use warp commands |
| `aura.warp.set` | op | Create / update warps |
| `aura.warp.delete` | op | Delete warps |
| `aura.home` | true | Use home commands |
| `aura.home.set` | true | Set homes |
| `aura.home.delete` | true | Delete homes |
| `aura.tpa` | true | Use TPA commands |
| `aura.god` | op | Toggle god for self |
| `aura.god.others` | op | Toggle god for others |
| `aura.fly` | op | Toggle fly for self |
| `aura.fly.others` | op | Toggle fly for others |
| `aura.nofall` | op | Toggle no-fall for self |
| `aura.nofall.others` | op | Toggle no-fall for others |
| `aura.nohunger` | op | Toggle no-hunger for self |
| `aura.nohunger.others` | op | Toggle no-hunger for others |
| `aura.rtp` | true | Use `/rtp` |
| `aura.admin` | op | All AuraUtils permissions (parent node) |

Full authoritative list is in `plugin.yml`.

---

## Building from source

```bash
git clone https://github.com/TamaWish/AuraUtils.git
cd AuraUtils
mvn clean package
```

The shaded JAR is produced at `target/AuraUtils-1.0.0.jar` (or the current version).

- **Java 21** toolchain
- Dependency: Spigot API 1.21.4 (compatible with 1.21.x and 26.x)
- bStats is shaded and relocated

---

## Links

| | |
|---|---|
| **Source** | [github.com/TamaWish/AuraUtils](https://github.com/TamaWish/AuraUtils) |
| **Modrinth** | [modrinth.com/project/aurautils](https://modrinth.com/project/aurautils) |
| **bStats** | [bstats.org/plugin/bukkit/AuraUtils/33574](https://bstats.org/plugin/bukkit/AuraUtils/33574) |
| **Discord** | [discord.gg/6nD8qF9tKV](https://discord.gg/6nD8qF9tKV) |
| **Issues / Support** | GitHub Issues or Discord |

---

## License

See the repository for license information.
