# AuraUtils

Lightweight utility plugin for **Spigot**, **Paper**, **Purpur**, and **Folia**.

**Homes · Warps · TPA (trusted/instant) · Back · RTP · God · Fly · NoFall · NoHunger · GUI menu**

[![bStats](https://img.shields.io/badge/bStats-AuraUtils-00AA00)](https://bstats.org/plugin/bukkit/AuraUtils/33574)
[![Modrinth](https://img.shields.io/badge/Modrinth-AuraUtils-1BD96A?logo=modrinth)](https://modrinth.com/project/aurautils)
[![GitHub](https://img.shields.io/badge/GitHub-TamaWish%2FAuraUtils-181717?logo=github)](https://github.com/TamaWish/AuraUtils)

---

## Features

| Feature | Commands | Notes |
|---------|----------|--------|
| **Homes** | `/home`, `/sethome`, `/delhome` | Per-player homes with optional GUI |
| **Warps** | `/warp`, `/setwarp`, `/delwarp` | Server warps with optional GUI |
| **TPA** | `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel`, `/tpatrust`, `/tpauntrust` | Timed requests, **trusted/instant list**, cancelable countdown |
| **Back** | `/back` | Return to last teleport location |
| **RTP** | `/rtp` | Safe random teleport (bounded attempts) |
| **God** | `/god [player]` | Invincibility + heal / clear fire |
| **Fly** | `/fly [player]` | Persistent flight (Spigot & Paper safe) |
| **NoFall** | `/nofall [player]` | Toggle fall damage |
| **NoHunger** | `/nohunger [player]` | Toggle hunger depletion |
| **Menu** | `/menu` | Simple utility GUI |
| **Info** | `/aura` | Plugin info and command list |

All teleport actions (home, warp, back, TPA accept, RTP, menu clicks) share one countdown system:

- Display: **chat** / **actionbar** / **both** / **none**
- Optional **title + subtitle** (large remaining seconds)
- Configurable chat reminders (`chat-at`)
- Cancel on move / damage
- Sounds with optional **rising pitch**
- Permission **`aura.teleport.bypass`** for instant teleports

Cancel any pending countdown **or outgoing TPA request** with `/tpacancel` (aliases: `/tpcancel`, `/auracancel`).

---

## Requirements

| | |
|---|---|
| **Server** | Spigot, Paper, Purpur, or **Folia** |
| **Minecraft** | **1.21.x** (tested to 1.21.11) **and** **26.1 / 26.2** |
| **Java** | 21+ (26.1+ servers require Java 25; the plugin itself is compiled for Java 21 and runs on both) |

One JAR covers both the classic `1.21.x` line and the new `26.x` year-based line. No separate builds required.

---


### Platform notes

- **Folia**: Fully supported. Countdowns, teleports, and fly re-apply use entity/region schedulers so they remain correct across region boundaries.
- **Geyser / Bedrock**: Works via standard Bukkit APIs; no extra configuration required.

## Installation

1. Download the latest `AuraUtils-x.y.z.jar`.
2. Place it in your server’s `plugins/` folder.
3. Restart the server.
4. Edit `plugins/AuraUtils/config.yml` if desired.

---

## Configuration

Key options from `config.yml` (see the file for full comments):

```yaml
tpa:
  timeout: 60                 # Seconds before a TPA request expires
  trusted-max: 50             # Max players on trusted list (0 = unlimited)
  trusted-instant: false      # true = skip countdown for trusted TPAs

rtp:
  radius: 2000
  minDistance: 250
  attempts: 30
  attemptsPerTick: 5
  countdown: 5                # 0 = immediate

teleport:
  countdown: 5                # Shared delay for home/warp/back/tpa (0 = instant)
  countdown-display: both     # chat | actionbar | both | none
  chat-at: [3, 2, 1]          # Extra chat lines at these remaining seconds
                              # Start is always announced; empty list = every second
  title: true                 # Large title (seconds) + subtitle (destination)
  cancel-on-move: true
  cancel-on-damage: false
  sound: true
  sound-rising-pitch: true    # Pitch rises as countdown approaches 0

prefix: "&8[&bAura&8] &r"
```

### Why does the chat countdown skip 4?

By default `chat-at: [3, 2, 1]`. Chat always shows the **start** value (e.g. 5), then only the listed remaining seconds. This reduces chat spam. Action bar and title still update every second. Set `chat-at: [5, 4, 3, 2, 1]` (or `[]`) if you want a message every second.

---

## Permissions

| Permission | Default | Description |
|---|:---:|---|
| `aura.use` | true | Basic access / `/tpacancel` |
| `aura.menu` | true | Open the utility menu |
| `aura.back` | true | Use `/back` |
| `aura.warp` | true | Use warp commands |
| `aura.warp.set` | op | Create/update warps |
| `aura.warp.delete` | op | Delete warps |
| `aura.home` | true | Use home commands |
| `aura.home.set` | true | Set homes |
| `aura.home.delete` | true | Delete homes |
| `aura.tpa` | true | Use tpa / tpaccept / tpadeny |
| `aura.tpa.trust` | true | Manage trusted TPA list |
| `aura.god` | op | Toggle god mode for self |
| `aura.god.others` | op | Toggle god mode on others |
| `aura.fly` | op | Toggle fly for self |
| `aura.fly.others` | op | Toggle fly for others |
| `aura.nofall` | op | Toggle fall damage for self |
| `aura.nofall.others` | op | Toggle fall damage on others |
| `aura.nohunger` | op | Toggle hunger for self |
| `aura.nohunger.others` | op | Toggle hunger on others |
| `aura.rtp` | true | Use random safe teleport |
| `aura.teleport.bypass` | op | Skip teleport countdown (instant) |
| `aura.admin` | op | All AuraUtils permissions |

For the complete list see `plugin.yml`.

---

## Building

```bash
# Requires Maven and Java 21+
mvn clean package
```

The compiled JAR will be in `target/` (shaded artifact includes bStats).

---

## Metrics

AuraUtils uses [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574) (anonymous).  
Disable in `plugins/bStats/config.yml` → `enabled: false`.

---

## Development

Open the `AuraUtils` folder in your IDE — the Maven project is detected automatically.

Contributions welcome. When adding features:

- Register commands in `AuraUtils.java` and `plugin.yml`
- Prefer routing teleports through `TeleportHelper.scheduleTeleport` so countdown / bypass / display stay consistent
- Update `config.yml` comments and this README
#
