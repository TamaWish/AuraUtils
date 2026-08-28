# Hangar — paste these as the changelog when uploading each version

Hangar’s **project description** = `HANGAR_DESCRIPTION.md` (full overview + version history).

When you upload a **version**, use the short changelog below for that version’s notes field.

Use **`AuraUtils-1.3.0-spigot.jar`** on Spigot/CraftBukkit and **`AuraUtils-1.3.0-paper-folia.jar`** on Paper/Purpur/Folia.

---

## Version 1.3.0 (current)

**Translations, home limits, safer set**

- All player-facing messages in `lang/en.yml`; set `language:` and add `lang/<code>.yml` for extra locales
- `/aura reload` reloads config + language (`aura.admin`)
- Home limits: `homes.default-limit` (0 = unlimited) and permission-based `homes.limits`
- `/sethome` / `/setwarp`: 1–32 letter/number/`_`/`-` names; clickable overwrite confirm
- Folia-safe TPA cancel/quit, `/back`, and teleport pending state
- Teleport success waits for the async result
- Separate Spigot and Paper/Folia artifacts (same commands and config)
- Package `com.lozaine.aurautils` (main class `com.lozaine.aurautils.AuraUtils`)
- Optional GitHub update checker (`update-checker.enabled`); `aura.admin` gets a clickable [releases](https://github.com/TamaWish/AuraUtils/releases) link

Also includes everything from 1.2.2 / 1.2.0 / 1.1.1 / 1.1.0 / 1.0.0.

---

## Version 1.2.2 (optional historical upload)

**RTP watchdog / Folia TPA**

- Spigot `/rtp` no longer generates unloaded chunks on the tick thread
- Folia `/tpaccept` hops to the requester’s entity thread
- Teleport success only after the teleport future completes

---

## Version 1.2.0 (optional historical upload)

**Trusted / instant TPA**

- `/tpatrust <player>` — add to your trusted list (they can `/tpa` you without confirmation)
- `/tpatrust list` — view list
- `/tpauntrust <player>` or `/tpatrust remove <player>` — remove
- Permission `aura.tpa.trust` (default: true)
- Config: `tpa.trusted-max` (default 50), `tpa.trusted-instant` (default false)
- Folia-safe: concurrent maps; trusted teleports on the requester entity scheduler

Also includes everything from 1.1.1 / 1.1.0 / 1.0.0 (Folia support, RTP & back fixes, core utilities).

---

## Version 1.1.1 (optional historical upload)

**Folia fixes**

- RTP: height/surface checks on the target location’s region (fixes async chunk crash)
- `/back`: record location before teleport (Folia often skips PlayerTeleportEvent for plugin teleports)
- Durable world-name + coordinates for back positions

---

## Version 1.1.0 (optional historical upload)

**Folia support**

- Full region-aware scheduling via FoliaLib (`folia-supported: true`)
- Entity schedulers for countdowns, fly re-apply, RTP
- Async teleport on Paper / Folia
- Same JAR: Spigot, Paper, Purpur, Folia
- Geyser/Floodgate compatible

---

## Version 1.0.0 (optional historical upload)

**Initial release**

- Homes, warps, TPA, back, RTP, god, fly, nofall, nohunger, menu, `/aura`
- Shared teleport countdown (chat / actionbar / title, cancel-on-move/damage, rising pitch)
- `aura.teleport.bypass`, persistent toggles, bStats
- Minecraft 1.21.x and 26.1 / 26.2 · Java 21+
