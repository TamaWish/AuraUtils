# AuraUtils

Lightweight utility plugin for **Spigot**, **Paper**, **Purpur**, and **Folia**.

**Homes · Warps · TPA (trusted/instant) · Back · RTP · God · Fly · NoFall · NoHunger · GUI menu · Translations**

Two JARs · Minecraft **1.21.x** and **26.1 / 26.2** · Java 21+

- **Spigot / CraftBukkit:** `AuraUtils-1.3.0-spigot.jar`
- **Paper / Purpur / Folia:** `AuraUtils-1.3.0-paper-folia.jar`

---

## Features

| Feature | Commands | Notes |
|---------|----------|--------|
| **Homes** | `/home`, `/sethome`, `/delhome` | Per-player homes, optional GUI, limits, overwrite confirm |
| **Warps** | `/warp`, `/setwarp`, `/delwarp` | Server warps, optional GUI, overwrite confirm |
| **TPA** | `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel` | Timed requests + cancelable countdown |
| **Trusted TPA** | `/tpatrust`, `/tpauntrust` | Friends on your list can TPA without confirmation |
| **Back** | `/back` | Return to last teleport location |
| **RTP** | `/rtp` | Safe random teleport (bounded attempts) |
| **God** | `/god [player]` | Invincibility + heal / clear fire |
| **Fly** | `/fly [player]` | Persistent flight (Spigot & Paper safe) |
| **NoFall** | `/nofall [player]` | Toggle fall damage |
| **NoHunger** | `/nohunger [player]` | Toggle hunger depletion |
| **Menu** | `/menu` | Simple utility GUI |
| **Info** | `/aura`, `/aura reload` | Command list; reload config + language |
| **Languages** | `lang/en.yml` | Translate every player-facing message |

### Shared teleport countdown

Home, warp, back, TPA, RTP, and menu teleports share one system:

- Display: **chat** / **actionbar** / **both** / **none**
- Optional **title + subtitle**
- Configurable chat reminders (`chat-at`)
- Cancel on move / damage
- Sounds with optional **rising pitch**
- Permission **`aura.teleport.bypass`** for instant teleports

Cancel any pending countdown **or outgoing TPA request** with `/tpacancel` (aliases: `/tpcancel`, `/auracancel`).

### Trusted / instant TPA

1. `/tpatrust <player>` — add them to your list  
2. They run `/tpa You` → auto-accept (no `/tpaccept`)  
3. `/tpatrust list` / `/tpauntrust <player>` to manage  

Config: `tpa.trusted-max` (default 50), `tpa.trusted-instant` (skip countdown when true).

---

## Compatibility

| | |
|---|---|
| **Server** | Spigot, Paper, Purpur, **Folia** |
| **Minecraft** | **1.21.x** (tested to 1.21.11) **and** **26.1 / 26.2** |
| **Java** | 21+ (plugin). 26.1+ servers need Java 25; both plugin JARs run on Java 21 and 25. |

- **Folia**: Entity/region schedulers for countdowns, teleports, RTP, and fly re-apply  
- **Geyser / Floodgate**: Works via standard Bukkit APIs  

---

## Installation

1. Download the JAR from this page  
2. Place it in `plugins/`  
3. Restart the server  
4. (Optional) Edit `plugins/AuraUtils/config.yml` and `plugins/AuraUtils/lang/en.yml`  
5. `/aura reload` after edits (`aura.admin`)  

---

## Configuration highlights

```yaml
tpa:
  timeout: 60
  trusted-max: 50
  trusted-instant: false

teleport:
  countdown: 5
  countdown-display: both     # chat | actionbar | both | none
  chat-at: [3, 2, 1]
  title: true
  cancel-on-move: true
  cancel-on-damage: false
  sound: true
  sound-rising-pitch: true

homes:
  default-limit: 0            # 0 = unlimited

language: en                  # plugins/AuraUtils/lang/<code>.yml

update-checker:
  enabled: true               # GitHub notice for aura.admin
```

Player-facing strings are in `plugins/AuraUtils/lang/en.yml`. Copy it, translate, and set `language:`. Missing keys fall back to English. `/aura reload` reloads config and messages.

---

## Metrics

Uses [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574) (anonymous).  
Disable in `plugins/bStats/config.yml` → `enabled: false`.

---

## Version history

### 1.3.0 — Translations, home limits, safer set
- `lang/en.yml` for all player-facing messages; `language:` in config; `/aura reload`
- Home limits via `homes.default-limit` / `homes.limits` (permission-based)
- `/sethome` and `/setwarp` name rules + clickable overwrite confirm
- Folia-safe TPA/back/teleport state; teleport success waits for the async result
- Separate Spigot and Paper/Folia JARs (same commands and config)
- Package `com.lozaine.aurautils` (`plugin.yml` main: `com.lozaine.aurautils.AuraUtils`)
- Optional GitHub update checker (`update-checker.enabled`); `aura.admin` gets a clickable releases link

### 1.2.2 — RTP watchdog / Folia TPA
- Spigot `/rtp` no longer generates unloaded chunks on the tick thread
- Folia `/tpaccept` hops to the requester’s entity thread
- Teleport success only after the teleport future completes

### 1.2.0 — Trusted / instant TPA
- `/tpatrust`, `/tpauntrust` — personal trusted list; trusted players auto-accept TPA
- Config: `tpa.trusted-max`, `tpa.trusted-instant`
- Permission `aura.tpa.trust` (default: true)
- Concurrent maps for Folia-safe trusted lists and pending TPA state
- Trusted teleports scheduled on the requester’s entity thread

### 1.1.1 — Folia fixes
- **RTP**: height/surface checks run on the **target location’s region** (fixes `IllegalStateException: Cannot retrieve chunk asynchronously`)
- **`/back`**: record location in `TeleportHelper` before teleport (Folia often does not fire `PlayerTeleportEvent` for plugin teleports)
- Durable world-name + coordinates snapshot for back positions

### 1.1.0 — Folia support
- Full region-aware scheduling via FoliaLib (`folia-supported: true`)
- Entity schedulers for teleport countdowns, fly re-apply, RTP search
- Async teleport on Paper / Folia
- Same JAR on Spigot, Paper, Purpur, and Folia
- Geyser/Floodgate compatible out of the box

### 1.0.0 — Initial release
- Homes, warps, TPA, back, RTP, god, fly, nofall, nohunger, menu, `/aura`
- Shared teleport countdown (chat / actionbar / title, cancel-on-move/damage, rising pitch)
- `aura.teleport.bypass` for instant teleports
- Per-player persistence for toggles
- bStats metrics (plugin id 33574)
- Minecraft 1.21.x and 26.1 / 26.2; Java 21+
