<!--
AuraUtils marketplace copy — Hangar (Paper, Purpur, Folia).
Paste into https://hangar.papermc.io/Lozaine/AuraUtils
-->

<div align="center">

![AuraUtils](https://files.catbox.moe/0a2rns.png)

[![Release](https://img.shields.io/github/v/release/TamaWish/AuraUtils?style=flat-square&label=Release)](https://github.com/TamaWish/AuraUtils/releases)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)
[![Paper](https://img.shields.io/badge/Paper%20%2F%20Purpur%20%2F%20Folia-1.21.x%2B%20%2F%2026.x%2B-blue?style=flat-square)](https://hangar.papermc.io/Lozaine/AuraUtils)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](https://github.com/TamaWish/AuraUtils/blob/main/LICENSE)

[![Spigot downloads](https://img.shields.io/spiget/downloads/138193?style=flat-square&label=Spigot%20downloads&color=yellow)](https://www.spigotmc.org/resources/aurautils-spigot-paper-folia.138193/)
[![Modrinth downloads](https://img.shields.io/badge/dynamic/json?style=flat-square&color=1bd96a&label=Modrinth&query=downloads&url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FW2WxC84B&suffix=%20downloads)](https://modrinth.com/project/W2WxC84B)
[![GitHub stars](https://img.shields.io/github/stars/TamaWish/AuraUtils?style=flat-square&logo=github)](https://github.com/TamaWish/AuraUtils)

[![Hangar](https://img.shields.io/hangar/dt/AuraUtils?style=flat-square)](https://hangar.papermc.io/Lozaine/AuraUtils)
[![BukkitDev downloads](https://img.shields.io/curseforge/dt/1669497?style=flat-square&label=BukkitDev%20downloads)](https://dev.bukkit.org/projects/1669497)

<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/fBpYOXKZQlc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

</div>

Lightweight homes, warps, TPA, back, RTP, timber, player inventories, and player toggles for **Paper**, **Purpur**, and **Folia**.

Players get a shared teleport countdown, a simple GUI, and translatable messages. Operators get warps, home limits, reload, and an optional GitHub update notice.

AuraUtils does **not** include an economy. Pair it with [PureEconomy](https://github.com/TamaWish/PureEconomy) for a lightweight utilities + economy stack. They talk through [Vault](https://www.spigotmc.org/resources/vault.4536/); AuraUtils charges PureEconomy's **default currency**. Neither plugin requires the other.

![INSTALLATION](https://files.catbox.moe/foxbd8.png)

Install **`AuraUtils-<version>-paper-folia.jar`** in `plugins/`. The paper-folia and spigot filenames are copies of the same shaded bytecode; Paper and Folia behavior is detected at runtime.

| Server | Install |
|--------|---------|
| Paper / Purpur / Folia | `AuraUtils-<version>-paper-folia.jar` |

1. Place the JAR in `plugins/`.
2. Restart the server.
3. Edit `plugins/AuraUtils/config.yml` if you want.
4. On first start the plugin copies `lang/en.yml`. Translate it or add another locale and set `language:`.
5. Run `/aura reload` (`aura.admin`) after config or language edits.

![FEATURES](https://files.catbox.moe/p0ee9p.png)

- Homes with optional GUI, name rules, overwrite confirm, and optional limits
- Server warps with optional GUI and overwrite confirm
- Timed TPA plus a trusted list that auto-accepts requests
- `/back`, safe `/rtp`, god, fly, nofall, nohunger, timber
- Extra player inventories (`/inv 1`, `/inv 2`, …) with rank limits
- `/menu` utility GUI
- Shared countdown: chat, action bar, title, cancel on move/damage, rising-pitch sounds
- `aura.teleport.bypass` for instant teleports
- Optional [Vault](https://github.com/MilkBowl/VaultAPI) economy costs for home, warp, TPA, RTP, and back — pair with [PureEconomy](https://github.com/TamaWish/PureEconomy) or any Vault economy
- All player-facing text in `lang/en.yml`

Trusted TPA: `/tpatrust <player>` adds them to your list. They can `/tpa` you without `/tpaccept`. Manage with `/tpatrust list` and `/tpauntrust <player>`. Cancel a countdown or an outgoing TPA with `/tpacancel` (`/tpcancel`, `/auracancel`).

Timber: with `timber.enabled: true` (default), chopping a log with an axe fells the connected tree. `/timber` turns it off for you. Sneak to chop a single log.

Player inventories: `/inv 1` is a personal double chest. Normal ranks get that one inventory. Give a rank more with a single node — `aura.inv.3` allows `/inv 1`–`3`. Works with LuckPerms or any other permission plugin.

![COMMANDS](https://files.catbox.moe/pwsw8n.png)

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
| `/timber [player]` | `aura.timber` | Toggle chopping a whole tree from one log |
| `/inv [number\|list]` | `aura.inv` | Open a personal extra inventory |
| `/menu` | `aura.menu` | Open the utility GUI |
| `/aura [reload]` | `aura.use` / `aura.admin` | Command list; reload config and language |

![REQUIREMENTS](https://files.catbox.moe/joazzp.png)

- Java **21+** (Minecraft **26.1+** servers need Java 25)
- Minecraft **1.21.x** and **26.1 / 26.2**
- Paper, Purpur, or Folia
- Optional: [Vault](https://www.spigotmc.org/resources/vault.4536/) + an economy plugin if you want paid teleports / set-home / set-warp. **[PureEconomy](https://github.com/TamaWish/PureEconomy)** is the matching lightweight companion.

![CONFIGURATION](https://files.catbox.moe/pwsw8n.png)

```yaml
tpa:
  timeout: 60
  trusted-max: 50
  trusted-instant: false

homes:
  default-limit: 3
  limits: []

inventories:
  enabled: true
  rows: 6
  max: 10
  default-limit: 1
  limits: []

rtp:
  radius: 2000
  minDistance: 250
  attempts: 30

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

economy:
  enabled: true
  notify: true
  costs:
    home: 0.0
    sethome: 0.0
    warp: 0.0
    setwarp: 0.0
    tpa: 0.0
    rtp: 0.0
    back: 0.0

timber:
  enabled: true
  require-axe: true
  sneak-chops-single: true
  break-leaves: true
  max-logs: 128
  max-leaves: 256
```

Home and warp names are 1–32 letters, numbers, `_`, or `-`. Overwriting an existing name asks for clickable **[CONFIRM]** / **[CANCEL]** (30 seconds).

`chat-at: [3, 2, 1]` always announces the start (for example 5), then only those remaining seconds. Action bar and title still update every second.

When `update-checker.enabled` is true, operators with `aura.admin` get a clickable chat link if a newer GitHub release exists.

Economy is optional. Costs of `0` are free. Without Vault (or without an economy provider), every action stays free. Money is taken when the action succeeds; a cancelled countdown is not charged; a failed teleport is refunded. `aura.economy.bypass` skips costs.

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
| `aura.timber` / `aura.timber.others` | true / op | Easy tree chopping for self / others |
| `aura.inv` | true | Open extra inventories (`/inv`) |
| `aura.inv.<n>` | false | Open inventories 1 through `n` |
| `aura.rtp` | true | Random safe teleport |
| `aura.teleport.bypass` | op | Skip teleport countdown |
| `aura.economy.bypass` | op | Skip Vault economy costs |
| `aura.admin` | op | All permissions, including `/aura reload` |

![NOTE](https://files.catbox.moe/22kdgh.png)

Replace the jar with `AuraUtils-<version>-paper-folia.jar`. The paper-folia and spigot filenames are the same bytecode; the server type is detected at runtime. After first start, translate `plugins/AuraUtils/lang/en.yml` or add another `lang/<code>.yml`. Add `language:`, `homes.default-limit`, `inventories`, `update-checker.enabled`, `economy`, and `timber` to an existing `config.yml` if those keys are missing.

![METRICS](https://files.catbox.moe/qlbzjk.png)

AuraUtils sends **anonymous** usage stats through [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574) (plugin id **33574**). Charts are public. No player names, UUIDs, IPs, chat, or inventory contents.

**Every bStats plugin reports** player count, online-mode, Minecraft version, server software, Java version, OS, CPU cores, country, and plugin version.

**AuraUtils also reports** (config and usage only): teleport countdown length and display mode; cancel-on-move / cancel-on-damage / teleport sound; RTP countdown and TPA timeout; whether timber, player inventories, and Vault economy are enabled or hooked; how many currently online players have god, fly, nofall, or nohunger on; how many server warps exist.

**Opt out** for every bStats plugin on the server: `plugins/bStats/config.yml` → `enabled: false`, then restart. There is no separate AuraUtils metrics switch.

[Downloads](https://github.com/TamaWish/AuraUtils/releases) · [Changelog](https://github.com/TamaWish/AuraUtils/blob/main/CHANGELOG.md) · [Support](https://discord.gg/kbKZzxDETU)

Copyright: **Lozaine@Tamawish** · MIT

## Changelog

### 1.4.0

**Added**
- **Player inventories** — `/inv 1`, `/inv 2`, … extra double-chest storage. Default players get inventory 1. Grant `aura.inv.<n>` with any permission plugin (`aura.inv.3` = inventories 1–3).
- **Timber** — chop one log with an axe to fell the connected tree (`timber.enabled` true/false). `/timber` to opt out. Sneak chops a single log.
- **permissions.yml** — set each node’s default to `true`, `false`, or `op`. `/aura reload` applies it. LuckPerms still overrides per player.
- Optional Vault economy costs (`economy.costs` for home, sethome, warp, setwarp, tpa, rtp, back). Defaults are free. Missing Vault keeps every action free. Pair with [PureEconomy](https://github.com/TamaWish/PureEconomy) or any Vault economy.
- Charge on success; cancelled countdown is not charged; failed teleport is refunded
- `aura.economy.bypass` skips costs; GUI lore shows the price when you would be charged

**Fixed**
- Paper shutdown no longer throws `World unloaded` while saving homes and warps. Destinations store world name + coordinates instead of calling `Location.getWorld()` after worlds are gone.
- `/home` and `/warp` report an unloaded world instead of “not found”
- `/sethome` and `/setwarp` do not charge if the destination cannot be stored

### 1.3.0

**Added**
- Translatable messages in `lang/en.yml`; set `language:` and add `lang/<code>.yml` for extra locales
- `/aura reload` reloads config and language (`aura.admin`)
- Home limits: `homes.default-limit` and permission-based `homes.limits` (set a positive default before VIP caps)
- Safer `/sethome` / `/setwarp`: 1–32 letter/number/`_`/`-` names and clickable overwrite confirm
- Optional GitHub update checker (`update-checker.enabled`); `aura.admin` gets a clickable releases link

**Fixed**
- Concurrent TPA request lifecycle (cancel / player quit)
- Folia-safe pending teleport and `/back` state
- Paper async RTP chunk-load detection
- Teleport success waits for the async result (trusted instant TPA and countdown-0 home/warp/back included)
- `/aura` status shows ON/OFF instead of raw keys, and color no longer leaks onto Fly / NoHunger labels

**Changed**
- Package `com.lozaine.aurautils` (main class `com.lozaine.aurautils.AuraUtils`)
- Marketplace filename: `AuraUtils-<version>-paper-folia.jar` (same bytecode as `AuraUtils-<version>-spigot.jar`; platform is detected at runtime)

### 1.2.2 — RTP watchdog / Folia TPA
- Spigot `/rtp` no longer generates unloaded chunks on the tick thread
- Folia `/tpaccept` hops to the requester’s entity thread
- Teleport success only after the teleport future completes

### 1.2.0 — Trusted / instant TPA
- `/tpatrust`, `/tpauntrust` — personal trusted list; trusted players auto-accept TPA
- Config: `tpa.trusted-max`, `tpa.trusted-instant`
- Permission `aura.tpa.trust` (default: true)

### 1.1.1 — Folia fixes
- RTP height/surface checks on the target location’s region
- `/back` records location before teleport (Folia often skips `PlayerTeleportEvent`)

### 1.1.0 — Folia support
- Region-aware scheduling (`folia-supported: true`)
- Async teleport on Paper / Folia

### 1.0.0 — Initial release
- Homes, warps, TPA, back, RTP, god, fly, nofall, nohunger, menu, `/aura`
- Shared teleport countdown · Minecraft 1.21.x and 26.1 / 26.2 · Java 21+
