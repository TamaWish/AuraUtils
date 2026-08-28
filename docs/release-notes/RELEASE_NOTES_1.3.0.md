# AuraUtils 1.3.0

## Player-facing

- **Translations** — all chat, GUI, titles, action bar, and clickable confirm labels live in `plugins/AuraUtils/lang/en.yml`. Set `language:` in `config.yml` and add another `lang/<code>.yml` for extra locales. `/aura reload` (`aura.admin`) reloads config and messages.
- **Home limits** — `homes.default-limit` (0 = unlimited) and optional permission-based `homes.limits` (LuckPerms groups work with no extra dependency).
- **Safer set** — `/sethome` and `/setwarp` names are 1–32 letters, numbers, `_`, or `-`. Overwriting an existing name asks for clickable **[CONFIRM]** / **[CANCEL]** (30 seconds).
- **Update checker** — `update-checker.enabled` (default true) checks GitHub for a newer release. Admins (`aura.admin`) get a chat notice with a clickable link to [TamaWish/AuraUtils](https://github.com/TamaWish/AuraUtils).

## Reliability

- Fixed concurrent TPA request lifecycle handling, including requester cancellation and player quit cleanup.
- Made pending teleport and `/back` state safe for concurrent Folia scheduler access.
- Fixed Paper async RTP chunk-load capability detection to inspect the runtime world implementation.
- Teleport success messages now wait for the asynchronous teleport result instead of reporting success when a request is merely submitted.

## Distribution

Two shaded artifacts (same commands and config):

- `AuraUtils-1.3.0-spigot.jar` — Spigot / CraftBukkit
- `AuraUtils-1.3.0-paper-folia.jar` — Paper / Purpur / Folia

Both cover Minecraft **1.21.x** and **26.1 / 26.2**. Plugin main class is `com.lozaine.aurautils.AuraUtils`. Platform validation remains required before publication.

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
