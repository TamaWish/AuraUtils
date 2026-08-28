# AuraUtils

Lightweight utility plugin for **Spigot**, **Paper**, **Purpur**, and **Folia**.

**Homes · Warps · TPA (trusted/instant) · Back · RTP · God · Fly · NoFall · NoHunger · GUI menu · Translations**

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

All teleport actions share a configurable countdown (chat / actionbar / title / both), optional cancel-on-move and cancel-on-damage, rising-pitch sounds, and permission `aura.teleport.bypass` for instant teleports.

**Trusted TPA:** `/tpatrust <player>` adds them to your list — they can `/tpa` you without `/tpaccept`. Manage with `/tpatrust list` and `/tpauntrust <player>`.

---

## Compatibility

| | |
|---|---|
| **Server** | Spigot, Paper, Purpur, or **Folia** |
| **Minecraft** | **1.21.x** (tested to 1.21.11) **and** **26.1 / 26.2** (year-based numbering) |
| **Java** | 21+ (plugin). 26.1+ servers require Java 25; both plugin JARs run on Java 21 and 25. |

**Two JARs** (same commands and config): `-spigot` for Spigot/CraftBukkit, `-paper-folia` for Paper/Purpur/Folia. Each covers Minecraft **1.21.x** and **26.x**. Folia uses entity/region schedulers. Geyser/Floodgate works via standard Bukkit APIs.

---

## Installation

1. Download the JAR for your platform from this page (or from Hangar / SpigotMC / GitHub Releases).
2. Place it in your server’s `plugins/` folder.
3. Restart the server.
4. (Optional) Edit `plugins/AuraUtils/config.yml` and `plugins/AuraUtils/lang/en.yml`.
5. `/aura reload` after edits (`aura.admin`).

---

## Configuration highlights

```yaml
tpa:
  timeout: 60
  trusted-max: 50             # 0 = unlimited
  trusted-instant: false      # true = skip countdown for trusted TPAs

teleport:
  countdown: 5
  countdown-display: both     # chat | actionbar | both | none
  chat-at: [3, 2, 1]          # start always announced; extra reminders
  title: true                 # large title + subtitle
  cancel-on-move: true
  cancel-on-damage: false
  sound: true
  sound-rising-pitch: true

homes:
  default-limit: 0            # 0 = unlimited

language: en                  # plugins/AuraUtils/lang/<code>.yml

update-checker:
  enabled: true
```

Players with `aura.teleport.bypass` (default: op) skip the countdown entirely.

Translate `plugins/AuraUtils/lang/en.yml` (or copy it and set `language:`). `/aura reload` reloads config and messages.

---

## What's new in 1.3.0

- **Translations** — `lang/en.yml`, `language:` in config, `/aura reload`
- Home limits (`homes.default-limit` / `homes.limits`)
- Safer `/sethome` and `/setwarp` (name rules + clickable overwrite confirm)
- Folia-safe TPA/back/teleport; success messages wait for the async teleport
- Separate Spigot and Paper/Folia JARs
- Package `com.lozaine.aurautils`
- Optional GitHub update checker for admins (clickable release link)

Earlier: **1.2.2** RTP watchdog · **1.2.0** trusted TPA · **1.1.0** Folia · **1.0.0** core utilities

---

## Metrics

Uses [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574) (anonymous). Disable in `plugins/bStats/config.yml`.
