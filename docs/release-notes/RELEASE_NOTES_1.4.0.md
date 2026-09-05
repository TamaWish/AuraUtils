# AuraUtils 1.4.0

## Player-facing

- **Player inventories** — `/inv 1`, `/inv 2`, … open extra double-chest storage (aliases `/vault`, `/pv`). Everyone with `aura.inv` gets inventory **1**. Grant a rank more with one node: `aura.inv.3` allows `/inv 1`–`3`. `aura.inv.*` and `/lp` tab-completion work because the numbered nodes are registered with the server. `aura.admin` gets every inventory up to `inventories.max`. `/menu` has an Inventories button. Step-by-step setup: [PLAYER_INVENTORIES.md](../PLAYER_INVENTORIES.md).
- **Timber** — chop one log with an axe to fell the connected tree. Leaves of that tree are cleared. Sneak to chop a single log. Turn the whole feature off with `timber.enabled: false`. Players can `/timber` to opt out (default on).
- **permissions.yml** — edit `plugins/AuraUtils/permissions.yml` to set who gets each node by default (`true` / `false` / `op`). `/aura reload` applies it. LuckPerms still overrides per player.
- **Vault economy** — optional. Install [Vault](https://www.spigotmc.org/resources/vault.4536/) plus an economy plugin. **[PureEconomy](https://github.com/TamaWish/PureEconomy)** is the matching lightweight companion (balances, pay, bank, baltop). EssentialsX, CMI, and other Vault providers also work. AuraUtils soft-depends on Vault; without it, every action stays free.
- Per-action prices in `config.yml` under `economy.costs` (`home`, `sethome`, `warp`, `setwarp`, `tpa`, `rtp`, `back`). Defaults are `0`.
- Charged when the action succeeds. Cancelling a countdown does not take money. A failed teleport is refunded.
- `aura.economy.bypass` (op / `aura.admin`) skips costs.
- Home, warp, and back GUI entries show the cost when you would be charged.
- `/aura` shows the hooked economy provider for operators with `aura.admin`.

## Fixed

- `aura.inv.*` grants every inventory now. The numbered nodes were not registered with the server, and permission plugins only expand wildcards over registered nodes.
- Docs no longer imply AuraUtils ships rank-specific permission names. Only `aura.inv` and `aura.inv.<n>` are defined; `inventories.limits` maps an existing server permission.
- Paper 1.21+ / 26.x no longer prints `IllegalArgumentException: World unloaded` when AuraUtils disables. Worlds unload before plugins, so warp/home save writes stored world name + coordinates instead of calling `Location.getWorld()`.
- `/home` and `/warp` (commands and GUI) say the destination world is not loaded instead of “not found”.
- `/sethome` and `/setwarp` do not charge if the home or warp cannot be stored.

## Upgrade

**Back up `plugins/AuraUtils/` first** (at least `config.yml`, `lang/`, `warps.yml`, `homes.yml`, `player-states.yml`).

**`config.yml` does not auto-update.** The jar only copies it when the file is missing (first install). An existing 1.3.0 file is left as-is so your values and comments stay. New 1.4.0 keys are not inserted. Missing keys still use built-in defaults (inventories / timber / economy on, costs `0`).

Replace the jar, then add these blocks to `config.yml` if they are missing. **Or**, after the backup, delete `config.yml` so the jar copies a full 1.4.0 file, then re-apply your old settings from the backup:

```yaml
inventories:
  enabled: true
  rows: 6
  max: 10
  default-limit: 1
  limits: []

timber:
  enabled: true
  require-axe: true
  sneak-chops-single: true
  break-leaves: true
  max-logs: 128
  max-leaves: 256

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
```

New English message keys merge into `plugins/AuraUtils/lang/en.yml` automatically. First start also copies `permissions.yml` if it is missing.

## Distribution

- `AuraUtils-1.4.0-spigot.jar` — Spigot / CraftBukkit listings
- `AuraUtils-1.4.0-paper-folia.jar` — Paper / Purpur / Folia listings

Both files are the same shaded bytecode. Minecraft **1.21.x** and **26.1 / 26.2**.

Per-version notes live in `docs/release-notes/`.
