<!--
AuraUtils marketplace copy — DevBukkit / CraftBukkit.
Paste into https://dev.bukkit.org/projects/1669497
Replace category image URLs when listing-specific banners are ready.
-->

![AuraUtils](https://files.catbox.moe/0a2rns.png)

[![Release v1.3.0](https://img.shields.io/badge/Release-v1.3.0-brightgreen?style=flat-square)](https://github.com/TamaWish/AuraUtils/releases)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)
[![CraftBukkit](https://img.shields.io/badge/CraftBukkit-1.21.x%2B%20%2F%2026.x%2B-blue?style=flat-square)](https://dev.bukkit.org/projects/1669497)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](https://github.com/TamaWish/AuraUtils/blob/main/LICENSE)

[![Spigot downloads](https://img.shields.io/spiget/downloads/138193?style=flat-square&label=Spigot%20downloads&color=yellow)](https://www.spigotmc.org/resources/aurautils-spigot-paper-folia.138193/)
[![Modrinth downloads](https://img.shields.io/badge/dynamic/json?style=flat-square&color=1bd96a&label=Modrinth&query=downloads&url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FW2WxC84B&suffix=%20downloads)](https://modrinth.com/project/W2WxC84B)
[![GitHub stars](https://img.shields.io/github/stars/TamaWish/AuraUtils?style=flat-square&logo=github)](https://github.com/TamaWish/AuraUtils)

[![Hangar](https://img.shields.io/hangar/dt/AuraUtils?style=flat-square)](https://hangar.papermc.io/Lozaine/AuraUtils)
[![BukkitDev downloads](https://img.shields.io/curseforge/dt/1669497?style=flat-square&label=BukkitDev%20downloads)](https://dev.bukkit.org/projects/1669497)

Lightweight homes, warps, TPA, back, RTP, and player toggles for **CraftBukkit**.

Players get a shared teleport countdown, a simple GUI, and translatable messages. Operators get warps, home limits, reload, and an optional GitHub update notice.

![INSTALLATION](https://file.garden/apESCVYBqnKcJ-mg/AU/INSTALLATION.png)

Install **exactly one** JAR:

| Server | Install |
|--------|---------|
| CraftBukkit | `AuraUtils-1.3.0-spigot.jar` |

1. Place the JAR in `plugins/`.
2. Restart the server.
3. Edit `plugins/AuraUtils/config.yml` if you want.
4. On first start the plugin copies `lang/en.yml`. Translate it or add another locale and set `language:`.
5. Run `/aura reload` (`aura.admin`) after config or language edits.

![FEATURES](https://file.garden/apESCVYBqnKcJ-mg/AU/FEATURES.png)

- Homes with optional GUI, name rules, overwrite confirm, and optional limits
- Server warps with optional GUI and overwrite confirm
- Timed TPA plus a trusted list that auto-accepts requests
- `/back`, safe `/rtp`, god, fly, nofall, nohunger
- `/menu` utility GUI
- Shared countdown: chat, action bar, title, cancel on move/damage, rising-pitch sounds
- `aura.teleport.bypass` for instant teleports
- All player-facing text in `lang/en.yml`

Trusted TPA: `/tpatrust <player>` adds them to your list. They can `/tpa` you without `/tpaccept`. Manage with `/tpatrust list` and `/tpauntrust <player>`. Cancel a countdown or an outgoing TPA with `/tpacancel` (`/tpcancel`, `/auracancel`).

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/home [name\|list]` | `aura.home` | Teleport to a home or open the home GUI |
| `/sethome <name>` | `aura.home.set` | Create or update a home |
| `/delhome <name>` | `aura.home.delete` | Delete a home |
| `/warp [name\|list]` | `aura.warp` | Teleport to a warp or open the warp GUI |
| `/setwarp <name>` | `aura.warp.set` | Create or update a warp |
| `/delwarp <name>` | `aura.warp.delete` | Delete a warp |
| `/tpa <player>\|list` | `aura.tpa` | Send a TPA request or open the TPA GUI |
| `/tpaccept` / `/tpadeny` | `aura.tpa` | Accept or deny a pending TPA |
| `/tpacancel` | `aura.use` | Cancel a countdown or outgoing TPA |
| `/tpatrust` / `/tpauntrust` | `aura.tpa.trust` | Manage your trusted TPA list |
| `/back` | `aura.back` | Return to the last teleport location |
| `/rtp` | `aura.rtp` | Random safe teleport |
| `/god [player]` | `aura.god` | Toggle invincibility |
| `/fly [player]` | `aura.fly` | Toggle flight |
| `/nofall [player]` | `aura.nofall` | Toggle fall damage |
| `/nohunger [player]` | `aura.nohunger` | Toggle hunger depletion |
| `/menu` | `aura.menu` | Open the utility GUI |
| `/aura [reload]` | `aura.use` / `aura.admin` | Command list; reload config and language |

![REQUIREMENTS](https://file.garden/apESCVYBqnKcJ-mg/AU/REQUIREMENTS.png)

- Java **21+** (Minecraft **26.1+** servers need Java 25)
- Minecraft **1.21.x** and **26.1 / 26.2**
- CraftBukkit

![CONFIGURATION](https://files.catbox.moe/pwsw8n.png)

```yaml
tpa:
  timeout: 60
  trusted-max: 50
  trusted-instant: false

homes:
  default-limit: 0
  limits:
    - permission: aura.homes.vip
      max: 5

rtp:
  radius: 2000
  minDistance: 250
  attempts: 30
  generate-unloaded: true
  max-sync-generations: 3

teleport:
  countdown: 5
  countdown-display: both
  chat-at: [3, 2, 1]
  title: true
  cancel-on-move: true
  sound: true
  sound-rising-pitch: true

language: en
prefix: "&8[&bAura&8] &r"

update-checker:
  enabled: true
```

Home and warp names are 1–32 letters, numbers, `_`, or `-`. Overwriting an existing name asks for clickable **[CONFIRM]** / **[CANCEL]** (30 seconds).

`chat-at: [3, 2, 1]` always announces the start (for example 5), then only those remaining seconds. Action bar and title still update every second.

When `update-checker.enabled` is true, operators with `aura.admin` get a clickable chat link if a newer GitHub release exists.

![PERMISSIONS](https://files.catbox.moe/rc5ojm.png)

| Permission | Default | Description |
|------------|---------|-------------|
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
| `aura.god` / `aura.god.others` | op | God mode for self / others |
| `aura.fly` / `aura.fly.others` | op | Fly for self / others |
| `aura.nofall` / `aura.nofall.others` | op | Fall damage for self / others |
| `aura.nohunger` / `aura.nohunger.others` | op | Hunger for self / others |
| `aura.rtp` | true | Random safe teleport |
| `aura.teleport.bypass` | op | Skip teleport countdown |
| `aura.admin` | op | All permissions, including `/aura reload` |

![NOTE](https://file.garden/apESCVYBqnKcJ-mg/AU/UPGRADE%20NOTE.png)

Replace the jar with `AuraUtils-1.3.0-spigot.jar`. After first start, translate `plugins/AuraUtils/lang/en.yml` or add another `lang/<code>.yml`. Add `language:`, `homes.default-limit`, and `update-checker.enabled` to an existing `config.yml` if those keys are missing.

![METRICS](https://files.catbox.moe/qlbzjk.png)

Anonymous [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574). Disable in `plugins/bStats/config.yml` → `enabled: false`.

[Downloads](https://github.com/TamaWish/AuraUtils/releases) · [Changelog](https://github.com/TamaWish/AuraUtils/blob/main/CHANGELOG.md) · [Support](https://discord.gg/kbKZzxDETU)

Author: **Lozaine@Tamawish** · Copyright: **TamaWish** · MIT
