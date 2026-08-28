![AU Banner](https://files.catbox.moe/0a2rns.png)
# AuraUtils 1.3.0

Lightweight utility plugin for **Spigot**, **Paper**, **Purpur**, and **Folia**.

**Homes · Warps · TPA (trusted/instant) · Back · RTP · God · Fly · NoFall · NoHunger · GUI menu · Translations**

[![bStats](https://img.shields.io/badge/bStats-AuraUtils-00AA00)](https://bstats.org/plugin/bukkit/AuraUtils/33574)
[![Modrinth](https://img.shields.io/badge/Modrinth-AuraUtils-1BD96A?logo=modrinth)](https://modrinth.com/project/aurautils)
[![GitHub](https://img.shields.io/badge/GitHub-TamaWish%2FAuraUtils-181717?logo=github)](https://github.com/TamaWish/AuraUtils)

---

![FEATURES](https://file.garden/apESCVYBqnKcJ-mg/AU/FEATURES.png)

| Feature | Commands | Notes |
|---------|----------|--------|
| **Homes** | `/home`, `/sethome`, `/delhome` | Per-player homes, optional GUI, name rules, overwrite confirm, optional limits |
| **Warps** | `/warp`, `/setwarp`, `/delwarp` | Server warps, optional GUI, name rules, overwrite confirm |
| **TPA** | `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel`, `/tpatrust`, `/tpauntrust` | Timed requests, **trusted/instant list**, cancelable countdown |
| **Back** | `/back` | Return to last teleport location |
| **RTP** | `/rtp` | Safe random teleport (bounded attempts) |
| **God** | `/god [player]` | Invincibility + heal / clear fire |
| **Fly** | `/fly [player]` | Persistent flight (Spigot & Paper safe) |
| **NoFall** | `/nofall [player]` | Toggle fall damage |
| **NoHunger** | `/nohunger [player]` | Toggle hunger depletion |
| **Menu** | `/menu` | Simple utility GUI |
| **Info** | `/aura`, `/aura reload` | Command list; reload config + language (`aura.admin`) |
| **Languages** | `lang/en.yml` | All player-facing messages; extra locales via `language:` |

All teleport actions (home, warp, back, TPA accept, RTP, menu clicks) share one countdown system:

- Display: **chat** / **actionbar** / **both** / **none**
- Optional **title + subtitle** (large remaining seconds)
- Configurable chat reminders (`chat-at`)
- Cancel on move / damage
- Sounds with optional **rising pitch**
- Permission **`aura.teleport.bypass`** for instant teleports

Cancel any pending countdown **or outgoing TPA request** with `/tpacancel` (aliases: `/tpcancel`, `/auracancel`).

---

![REQUIREMENTS](https://file.garden/apESCVYBqnKcJ-mg/AU/REQUIREMENTS.png)

| | |
|---|---|
| **Server** | Spigot, Paper, Purpur, or **Folia** |
| **Minecraft** | **1.21.x** (tested to 1.21.11) **and** **26.1 / 26.2** |
| **Java** | 21+ (26.1+ servers require Java 25; the plugin itself is compiled for Java 21 and runs on both) |

One plugin covers both the classic `1.21.x` line and the new `26.x` year-based line. Download the JAR that matches your **server software**.

---

![UPGRADE NOTE](https://file.garden/apESCVYBqnKcJ-mg/AU/UPGRADE%20NOTE.png)

- **Folia**: Fully supported.
- **1.3.0**: Use the Spigot artifact on CraftBukkit/Spigot and the Paper/Folia artifact on Paper, Purpur, or Folia. After first start, translate `plugins/AuraUtils/lang/en.yml` or add another `lang/<code>.yml`. Admins are notified of new GitHub releases unless `update-checker.enabled` is false.

![INSTALLATION](https://file.garden/apESCVYBqnKcJ-mg/AU/INSTALLATION.png)

1. Download the JAR for your platform (`-spigot` or `-paper-folia`).
2. Place it in your server’s `plugins/` folder.
3. Restart the server.
4. Edit `plugins/AuraUtils/config.yml` if desired.
5. (Optional) Edit `plugins/AuraUtils/lang/en.yml`, or copy it and set `language:` to another code.
6. Run `/aura reload` (`aura.admin`) after language or config edits.

---

![CONFIGURATION](https://files.catbox.moe/pwsw8n.png)

Key options from `config.yml` (see the file for full comments):

```yaml
tpa:
  timeout: 60                 # Seconds before a TPA request expires
  trusted-max: 50             # Max players on trusted list (0 = unlimited)
  trusted-instant: false      # true = skip countdown for trusted TPAs

homes:
  default-limit: 0            # 0 = unlimited
  limits:
    - permission: aura.homes.vip
      max: 5
    - permission: aura.homes.unlimited
      max: 0                  # unlimited; works with LuckPerms ranks

rtp:
  radius: 2000
  minDistance: 250
  attempts: 30
  max-search-ticks: 200
  generate-unloaded: true     # Spigot only; Paper/Folia use async chunks
  max-sync-generations: 3     # Spigot cap (0 = loaded chunks only)
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

language: en                  # plugins/AuraUtils/lang/<code>.yml (default: lang/en.yml)
prefix: "&8[&bAura&8] &r"

update-checker:
  enabled: true               # GitHub release notice for aura.admin (clickable link)
```

Chat, GUI, titles, action bar, and clickable **[CONFIRM]** / **[CANCEL]** labels live in `plugins/AuraUtils/lang/en.yml` (copied from the jar on first run). Home and warp names are 1–32 letters, numbers, `_`, or `-`. Overwriting an existing name asks for confirmation (expires in 30 seconds).

### Languages

1. Copy `plugins/AuraUtils/lang/en.yml` to `lang/<code>.yml` (letters, numbers, `_`, `-` only — e.g. `zh`).
2. Translate the **values**. Keep the keys and `%placeholders%` (`%name%`, `%player%`, `%seconds%`, …). Color codes use `&`.
3. Set `language: zh` (or your code) in `config.yml`.
4. `/aura reload`.

Missing keys fall back to the jar English defaults. Plugin updates merge new keys into disk `lang/en.yml` without overwriting your edits.

When `update-checker.enabled` is true (default), AuraUtils checks GitHub for a newer release. The console is told if one exists. Players with `aura.admin` also get a chat notice with a **clickable** link to [GitHub releases](https://github.com/TamaWish/AuraUtils/releases). Set `enabled: false` to turn this off.

### Why does the chat countdown skip 4?

By default `chat-at: [3, 2, 1]`. Chat always shows the **start** value (e.g. 5), then only the listed remaining seconds. This reduces chat spam. Action bar and title still update every second. Set `chat-at: [5, 4, 3, 2, 1]` (or `[]`) if you want a message every second.

---

![PERMISSIONS](https://files.catbox.moe/rc5ojm.png)

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
| `aura.admin` | op | All AuraUtils permissions, including `/aura reload` |

---

![BUILDING](https://files.catbox.moe/jmhwqm.png)

```bash
# Requires Maven and Java 21+
mvn clean package
```

The compiled release JARs will be in `target/`:

- `AuraUtils-1.3.0-spigot.jar` for Spigot/CraftBukkit.
- `AuraUtils-1.3.0-paper-folia.jar` for Paper, Purpur, and Folia.

Both artifacts support Minecraft **1.21.x** and **26.x**. They share the same commands and config.

---

![METRICS](https://files.catbox.moe/qlbzjk.png)

AuraUtils uses [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574) (anonymous).  
Disable in `plugins/bStats/config.yml` → `enabled: false`.

