

![AuraUtils](https://files.catbox.moe/0a2rns.png)

![Release](https://img.shields.io/github/v/release/TamaWish/AuraUtils?style=flat-square&label=Release)
![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20%2F%2026.x-blue?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

![Spigot downloads](https://img.shields.io/spiget/downloads/138193?style=flat-square&label=SpigotMC%20&color=yellow)
![Modrinth downloads](https://img.shields.io/badge/dynamic/json?style=flat-square&color=1bd96a&label=Modrinth&query=downloads&url=https%3A%2F%2Fapi.modrinth.com%2Fv2%2Fproject%2FW2WxC84B&suffix=%20downloads)
![GitHub stars](https://img.shields.io/github/stars/TamaWish/AuraUtils?style=flat-square&logo=github)

![Hangar](https://img.shields.io/hangar/dt/AuraUtils?style=flat-square)
![CurseForge](https://img.shields.io/curseforge/dt/1669497?style=flat-square&label=CurseForge%20)





Lightweight homes, warps, TPA, back, RTP, timber, player inventories, and player toggles for **Spigot**, **CraftBukkit**, **Paper**, **Purpur**, and **Folia**.

Players get a shared teleport countdown, a simple GUI, and translatable messages. Operators get warps, home and inventory limits, reload, and an optional GitHub update notice.

AuraUtils does **not** include an economy. Pair it with [PureEconomy](https://github.com/TamaWish/PureEconomy) when you want paid teleports without pulling in a full Essentials-style suite.

Version history lives in [CHANGELOG.md](CHANGELOG.md).

## Lightweight stack (with PureEconomy)

Two small plugins, one job each. Neither requires the other.


| Need                              | Plugin                                                     |
| --------------------------------- | ---------------------------------------------------------- |
| Homes, warps, TPA, RTP, fly, god  | **AuraUtils**                                              |
| Economy, pay, bank, baltop, Vault | **[PureEconomy](https://github.com/TamaWish/PureEconomy)** |


They connect through [Vault](https://www.spigotmc.org/resources/vault.4536/):

1. PureEconomy registers its **default currency** (usually `coins`) as the Vault economy.
2. AuraUtils charges that currency for `/home`, `/warp`, `/tpa`, `/rtp`, `/back`, `/sethome`, and `/setwarp` when you set `economy.costs` above `0`.
3. Extra PureEconomy currencies (`gems`, `tokens`, …) stay inside PureEconomy. Vault and AuraUtils only see the default.

Install **Vault + PureEconomy + AuraUtils**, restart, then set prices in `plugins/AuraUtils/config.yml`. `/aura` shows the hooked provider for `aura.admin` — with this stack it should read **PureEconomy**.

AuraUtils also works with EssentialsX, CMI, or any other Vault economy. Without Vault (or without a provider), every action stays free.

## Install

Install **exactly one** JAR. Both filenames are copies of the same bytecode; Paper, Folia, and Spigot behavior is detected at runtime.


| Server                        | Install                               |
| ----------------------------- | ------------------------------------- |
| Spigot / CraftBukkit / Bukkit | `AuraUtils-<version>-spigot.jar`      |
| Paper / Purpur / Folia        | `AuraUtils-<version>-paper-folia.jar` |


1. Place the matching JAR in `plugins/`.
2. Restart the server.
3. Edit `plugins/AuraUtils/config.yml` and `permissions.yml` if you want.
4. On first start the plugin copies `lang/en.yml` and `permissions.yml`. Translate the language file or add another locale and set `language:`.
5. Run `/aura reload` (`aura.admin`) after config, permission-default, or language edits.

**Requires** Java **21+** (Minecraft **26.1+** servers need Java 25), Minecraft **1.21.x** or **26.1 / 26.2**, and Spigot, CraftBukkit, Paper, Purpur, or Folia.

Optional: [Vault](https://www.spigotmc.org/resources/vault.4536/) plus an economy plugin if you want paid teleports, set-home, or set-warp. **[PureEconomy](https://github.com/TamaWish/PureEconomy)** is the matching lightweight companion (balances, pay, bank, baltop). Any Vault economy also works.

## Features

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
- `permissions.yml` for default who-gets-what (`true` / `false` / `op`) without LuckPerms

Trusted TPA: `/tpatrust <player>` adds them to your list. They can `/tpa` you without `/tpaccept`. Manage with `/tpatrust list` and `/tpauntrust <player>`. Cancel a countdown or an outgoing TPA with `/tpacancel` (`/tpcancel`, `/auracancel`).

Timber: with `timber.enabled: true` (default), chopping a log with an axe fells the connected tree. `/timber` turns it off for you. Sneak to chop a single log.

Player inventories: `/inv 1` is a personal double chest. Normal ranks get that one inventory. Grant more with LuckPerms (or the same nodes in any permission plugin) — see [Player inventories](docs/PLAYER_INVENTORIES.md).

## Commands


| Command                     | Permission                | Description                              |
| --------------------------- | ------------------------- | ---------------------------------------- |
| `/home [name|list]`         | `aura.home`               | Teleport to a home or open the home GUI  |
| `/sethome <name>`           | `aura.home.set`           | Create or update a home                  |
| `/delhome <name>`           | `aura.home.delete`        | Delete a home                            |
| `/warp [name|list]`         | `aura.warp`               | Teleport to a warp or open the warp GUI  |
| `/setwarp <name>`           | `aura.warp.set`           | Create or update a warp                  |
| `/delwarp <name>`           | `aura.warp.delete`        | Delete a warp                            |
| `/tpa <player>|list`        | `aura.tpa`                | Send a TPA request or open the TPA GUI   |
| `/tpaccept` / `/tpadeny`    | `aura.tpa`                | Accept or deny a pending TPA             |
| `/tpacancel`                | `aura.use`                | Cancel a countdown or outgoing TPA       |
| `/tpatrust` / `/tpauntrust` | `aura.tpa.trust`          | Manage your trusted TPA list             |
| `/back`                     | `aura.back`               | Return to the last teleport location     |
| `/rtp`                      | `aura.rtp`                | Random safe teleport                     |
| `/god [player]`             | `aura.god`                | Toggle invincibility                     |
| `/fly [player]`             | `aura.fly`                | Toggle flight                            |
| `/nofall [player]`          | `aura.nofall`             | Toggle fall damage                       |
| `/nohunger [player]`        | `aura.nohunger`           | Toggle hunger depletion                  |
| `/timber [player]`          | `aura.timber`             | Toggle chopping a whole tree from one log |
| `/inv [number\|list]`       | `aura.inv`                | Open a personal extra inventory           |
| `/menu`                     | `aura.menu`               | Open the utility GUI                     |
| `/aura [reload]`            | `aura.use` / `aura.admin` | Command list; reload config, permissions, and language |




## Config

`plugins/AuraUtils/config.yml`

```yaml
tpa:
  timeout: 60
  trusted-max: 50
  trusted-instant: false

homes:
  default-limit: 0
  limits: []      # optional: map a node you own to a home count

inventories:
  enabled: true
  rows: 6
  max: 10
  default-limit: 1
  limits: []      # optional: most servers just grant aura.inv.<n>

rtp:
  radius: 2000
  minDistance: 250
  attempts: 30
  generate-unloaded: true     # Spigot / CraftBukkit
  max-sync-generations: 3     # Spigot / CraftBukkit cap

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

`chat-at: [3, 2, 1]` always announces the start (for example 5), then only those remaining seconds. Action bar and title still update every second. Use `chat-at: [5, 4, 3, 2, 1]` or `[]` if you want a chat line every second.

When `update-checker.enabled` is true, operators with `aura.admin` get a clickable chat link if a newer GitHub release exists.

Economy is optional. Costs of `0` are free. Without Vault (or without an economy provider), every action stays free. Money is taken when the action succeeds; a cancelled countdown is not charged; a failed teleport is refunded. `aura.economy.bypass` skips costs. With `economy.notify: true` (default), chat says they spent (or were refunded) that amount on `/home`, `/sethome`, `/warp`, `/setwarp`, `/tpa`, `/rtp`, or `/back`.

With [PureEconomy](https://github.com/TamaWish/PureEconomy), charges use its **default currency**. Set prices here; players earn and spend through PureEconomy's `/balance`, `/pay`, `/bank`, and `/baltop`.

`timber.enabled: true` (default) lets an axe chop fell a whole tree. Set it `false` to keep vanilla chopping. Players can `/timber` to opt out. Sneak chops a single log.

`/inv` extra chests are separate from the Vault economy. Default players get **inventory 1**. To give VIP three chests with LuckPerms:

```
/lp group vip permission set aura.inv.3 true
```

That allows `/inv 1`, `/inv 2`, and `/inv 3` — no config edit. `aura.inv.1` … `aura.inv.<max>` are registered on startup, so `aura.inv.*` and `/lp` tab-completion work too. Same nodes in any other permission plugin. Full examples: [docs/PLAYER_INVENTORIES.md](docs/PLAYER_INVENTORIES.md).

**Back up `plugins/AuraUtils/` first**, then add `language:`, `homes.default-limit`, `inventories`, `update-checker.enabled`, `economy`, and `timber` to an existing `config.yml` if those keys are missing. The jar does not rewrite `config.yml` on upgrade. See [CHANGELOG.md](CHANGELOG.md) for 1.4.0 snippets.

## Languages

1. Copy `plugins/AuraUtils/lang/en.yml` to `lang/<code>.yml` (letters, numbers, `_`, `-` only — e.g. `zh`).
2. Translate the **values**. Keep the keys and `%placeholders%` (`%name%`, `%player%`, `%seconds%`, …). Color codes use `&`.
3. Set `language: zh` (or your code) in `config.yml`.
4. `/aura reload`.

Missing keys fall back to the jar English defaults. Plugin updates merge new keys into disk `lang/en.yml` without overwriting your edits.

## Permissions

Who gets each node **by default** is set in `plugins/AuraUtils/permissions.yml`:

- `true` — everyone
- `false` — nobody unless LuckPerms (or similar) grants it
- `op` — operators only

`/aura reload` applies changes. LuckPerms still overrides these per player. Example — let everyone fly, keep timber as ops only:

```yaml
defaults:
  aura.fly: true
  aura.timber: op
```

Shipped defaults:


| Permission                               | Default | Description                               |
| ---------------------------------------- | ------- | ----------------------------------------- |
| `aura.use`                               | true    | Basic access / `/tpacancel`               |
| `aura.menu`                              | true    | Open the utility menu                     |
| `aura.back`                              | true    | Use `/back`                               |
| `aura.warp`                              | true    | Use warp commands                         |
| `aura.warp.set`                          | op      | Create/update warps                       |
| `aura.warp.delete`                       | op      | Delete warps                              |
| `aura.home`                              | true    | Use home commands                         |
| `aura.home.set`                          | true    | Set homes                                 |
| `aura.home.delete`                       | true    | Delete homes                              |
| `aura.tpa`                               | true    | Use tpa / tpaccept / tpadeny              |
| `aura.tpa.trust`                         | true    | Manage trusted TPA list                   |
| `aura.god` / `aura.god.others`           | op      | God mode for self / others                |
| `aura.fly` / `aura.fly.others`           | op      | Fly for self / others                     |
| `aura.nofall` / `aura.nofall.others`     | op      | Fall damage for self / others             |
| `aura.nohunger` / `aura.nohunger.others` | op      | Hunger for self / others                  |
| `aura.timber` / `aura.timber.others`     | true / op | Easy tree chopping for self / others    |
| `aura.inv`                               | true    | Open extra inventories (`/inv`)           |
| `aura.inv.<n>`                           | false   | Open inventories 1 through `n`            |
| `aura.rtp`                               | true    | Random safe teleport                      |
| `aura.teleport.bypass`                   | op      | Skip teleport countdown                   |
| `aura.economy.bypass`                    | op      | Skip Vault economy costs                  |
| `aura.admin`                             | op      | All permissions, including `/aura reload` |




## Build

For contributors. Requires **Maven** and **Java 21+**.

```bash
mvn clean package
```

Release JARs land in `target/`:

- `AuraUtils-<version>-spigot.jar` — Spigot / CraftBukkit / Bukkit
- `AuraUtils-<version>-paper-folia.jar` — Paper / Purpur / Folia

The two files are copies of the same shaded JAR. Scheduling and RTP chunk loading adapt at runtime.

```bash
mvn test
```



## Metrics

AuraUtils sends **anonymous** usage stats through [bStats](https://bstats.org/plugin/bukkit/AuraUtils/33574) (plugin id **33574**). The charts are public. Nothing identifies a player or a server: no names, UUIDs, IPs, chat, or inventory contents.

**Every bStats plugin reports** player count, online-mode, Minecraft version, server software, Java version, OS, CPU cores, country, and plugin version.

**AuraUtils also reports** (config and usage only):

- Teleport countdown length and display mode (`chat` / `actionbar` / `both` / `none`)
- Cancel-on-move, cancel-on-damage, teleport sound
- RTP countdown and TPA timeout
- Whether timber, player inventories, and Vault economy are enabled / hooked
- How many *currently online* players have god, fly, nofall, or nohunger on
- How many server warps exist

**Opt out** for every bStats plugin on the server:

```yaml
# plugins/bStats/config.yml
enabled: false
```

Restart after editing. There is no separate AuraUtils metrics switch; bStats is shared.

[Downloads](https://github.com/TamaWish/AuraUtils/releases) · [Changelog](CHANGELOG.md) · [Support](https://discord.gg/kbKZzxDETU)

Copyright: **Lozaine@Tamawish** · MIT