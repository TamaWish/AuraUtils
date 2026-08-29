# AuraUtils 1.3.0

## Player-facing

- **Translations** — all chat, GUI, titles, action bar, and clickable confirm labels live in `plugins/AuraUtils/lang/en.yml`. Set `language:` in `config.yml` and add another `lang/<code>.yml` for extra locales. `/aura reload` (`aura.admin`) reloads config and messages.
- **Home limits** — `homes.default-limit` (0 = unlimited) and optional permission-based `homes.limits` (LuckPerms groups work with no extra dependency). Set a positive `default-limit` before adding VIP caps so ranks stack upward.
- **Safer set** — `/sethome` and `/setwarp` names are 1–32 letters, numbers, `_`, or `-`. Overwriting an existing name asks for clickable **[CONFIRM]** / **[CANCEL]** (30 seconds).
- **Update checker** — `update-checker.enabled` (default true) checks GitHub for a newer release. Admins (`aura.admin`) get a chat notice with a clickable link to [TamaWish/AuraUtils](https://github.com/TamaWish/AuraUtils).

## Reliability

- Fixed concurrent TPA request lifecycle handling, including requester cancellation and player quit cleanup.
- Made pending teleport and `/back` state safe for concurrent Folia scheduler access.
- Fixed Paper async RTP chunk-load capability detection to inspect the runtime world implementation.
- Teleport success messages now wait for the asynchronous teleport result instead of reporting success when a request is merely submitted. Trusted instant TPA and countdown-0 home/warp/back do the same, including failure chat.
- `/aura` status lines now show ON/OFF instead of the raw keys `common.on` / `common.off` (YAML 1.1 boolean trap on unquoted `on`/`off`). Green/red status color no longer leaks onto the next label.

## Distribution

Two marketplace filenames (same shaded bytecode; platform is detected at runtime):

- `AuraUtils-1.3.0-spigot.jar` — Spigot / CraftBukkit listings
- `AuraUtils-1.3.0-paper-folia.jar` — Paper / Purpur / Folia listings

Both cover Minecraft **1.21.x** and **26.1 / 26.2**. Plugin main class is `com.lozaine.aurautils.AuraUtils`. Plugin author is **Lozaine**. GitHub **1.3.0** is the latest stable release: [TamaWish/AuraUtils 1.3.0](https://github.com/TamaWish/AuraUtils/releases/tag/1.3.0) (`AuraUtils-1.3.0-spigot.jar` and `AuraUtils-1.3.0-paper-folia.jar`).

## Upgrade

Replace the jar, then either delete `plugins/AuraUtils/config.yml` or add:

```yaml
homes:
  default-limit: 0
  limits: []

language: en

update-checker:
  enabled: true
```

On first start the plugin copies `lang/en.yml` into `plugins/AuraUtils/lang/`. Translate that file or add another locale and set `language:`.

Per-version notes live in `docs/release-notes/`.
