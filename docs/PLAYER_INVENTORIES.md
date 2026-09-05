# Player inventories (`/inv`)

AuraUtils gives each player extra chest-sized storage. This is **not** the [Vault](https://www.spigotmc.org/resources/vault.4536/) economy plugin. Economy still uses Vault; `/inv` is personal item storage.

## Commands

| Command | Who | What |
|---------|-----|------|
| `/inv` | anyone with `aura.inv` | Open inventory **1** if they only have one; otherwise open a picker |
| `/inv 1` | same | Open inventory 1 (default for every rank) |
| `/inv 2`, `/inv 3`, … | ranks / extra nodes | Open that numbered inventory |
| `/inv list` | same | Always open the picker |

Aliases: `/vault`, `/playervault`, `/pv`.

Contents save when the chest closes, on quit, and when the plugin disables. Files live in `plugins/AuraUtils/inventories/<uuid>.yml`.

`/menu` also has an **Inventories** button when the feature is on.

## Defaults (no LuckPerms required)

Out of the box:

- `inventories.enabled: true`
- `inventories.rows: 6` (54 slots, a double chest)
- `inventories.max: 10` (highest number you will ever grant)
- `inventories.default-limit: 1`
- Permission `aura.inv` default **true**

Every normal player can run **`/inv 1`**. That is the minimum. You do not need LuckPerms for this.

Turn the feature off with `inventories.enabled: false`, or set `aura.inv: false` in `permissions.yml` so nobody gets it unless a permission plugin grants the node.

## How many inventories a player gets

AuraUtils takes the **highest** of these, then caps it at `inventories.max`:

1. **Numbered nodes** `aura.inv.<n>` — having `aura.inv.5` allows inventories **1 through 5**
2. **`inventories.default-limit`** — everyone with `aura.inv`
3. **`inventories.limits`** — optional, see [below](#optional-keep-the-number-in-configyml)
4. **`aura.admin`** — all inventories up to `max`

`0` in `default-limit` or a matching `limits` `max` means “all `max` slots”, not infinite numbered chests.

AuraUtils defines `aura.inv` and the numbered nodes `aura.inv.1` … `aura.inv.<max>`. It does not define rank-specific permission names.

## Permission plugins

Any permission plugin works. AuraUtils never talks to [LuckPerms](https://luckperms.net/); it just asks Bukkit whether the player has a node, so whatever LuckPerms (or UltraPermissions, GroupManager, PermissionsEx, …) grants is what AuraUtils sees.

### Give a rank more inventories

Grant the highest inventory number that rank should reach ([`/lp ... permission set`](https://luckperms.net/wiki/Permission-Commands)):

```
/lp group vip permission set aura.inv.3 true
/lp group mvp permission set aura.inv.5 true
```

`aura.inv.3` means that group can open `/inv 1`, `/inv 2`, and `/inv 3`. You do **not** need to set `aura.inv.1` and `aura.inv.2` as well. Changes apply immediately — no `/aura reload` needed for permission edits.

Other plugins, same node:

- UltraPermissions: add `aura.inv.3` to the rank
- GroupManager: `/manuaddp <player> aura.inv.3` or put the node in the group’s `permissions:` list
- PermissionsEx: `/pex group vip add aura.inv.3`

### Per-player extra chest

```
/lp user Steve permission set aura.inv.2 true
```

Steve keeps whatever their group already has if that number is higher.

### Wildcards

```
/lp group admin permission set aura.inv.* true
```

That group gets every inventory up to `inventories.max`. AuraUtils registers `aura.inv.1` … `aura.inv.<max>` on startup, which is what makes the wildcard resolve — LuckPerms only expands wildcards over permissions a plugin has **registered** (its `apply-wildcards` option, on by default). Registering them also makes the nodes tab-complete in `/lp`.

Raising `inventories.max` and running `/aura reload` registers the new numbers, so the wildcard covers them too. `aura.admin` already grants every inventory without a wildcard.

### Check what a player has

```
/lp user Steve info
/lp user Steve permission check aura.inv.3
```

In-game `/aura` shows **Inventories: N** for that player’s current limit.

### Optional — keep the number in `config.yml`

`inventories.limits` maps an existing permission from your server to a chest count, so you can change the count without editing your permission plugin. Use one of your own permission names, then run `/aura reload`:

```yaml
inventories:
  default-limit: 1
  limits:
    - permission: your.existing.rank.permission
      max: 3
```

Most servers do not need this. Numbered nodes cover the same ground in one command.

Sources are combined. If a mapped permission grants 3 inventories while `aura.inv.5` is also granted, the player gets **5** (highest wins).

## `permissions.yml`

`plugins/AuraUtils/permissions.yml` only sets the **default** for AuraUtils’ own nodes (`true` / `false` / `op`). It cannot express “VIP gets 3 chests” — grant `aura.inv.3` in your permission plugin for that.

```yaml
defaults:
  aura.inv: true    # everyone may use /inv (count still comes from config / permissions)
```

Set `aura.inv: false` if you want inventories to be a paid / rank perk only, then grant `aura.inv` plus a numbered node to those ranks.

## Changing size later

`inventories.rows` is 1–6. Shrinking the chest hides extra slots but **does not delete** items stored in them; they come back if you raise `rows` again. `inventories.max` is how many numbered inventories exist (1–54), not how many slots are in each chest.

## Upgrade

Add this block to an existing `config.yml` if it is missing, then `/aura reload`:

```yaml
inventories:
  enabled: true
  rows: 6
  max: 10
  default-limit: 1
  limits: []
```
