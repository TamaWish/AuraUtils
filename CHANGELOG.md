# Changelog

All notable changes to AuraUtils are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Released]

Nothing yet.

## [1.0.0] - Released

Initial release. Not yet published to GitHub.

### Added

- **Warps** — `/warp`, `/setwarp`, `/delwarp` with GUI list, tab completion, and per-warp permissions (`aura.warp.<name>`, `aura.warp.*`, or `aura.warp` for all).
- **Homes** — `/home`, `/sethome`, `/delhome` with per-player limits, GUI list, and tab completion; `/home` with no args teleports directly when the player has exactly one home.
- **TPA** — `/tpa`, `/tpaccept`, `/tpadeny` with configurable request timeout and teleport countdown.
- **TPA Here** — `/tpahere` to request another player teleport to you; same accept/deny flow as TPA.
- **Back** — `/back` to the last teleport location, integrated with other teleports.
- **Random teleport** — `/rtp` with spawn-radius search, minimum distance, safe-surface checks, per-tick attempt batching, countdown, and post-success cooldown.
- **Player toggles** — `/fly`, `/god`, `/nofall`, `/nohunger` with optional target player via `.others` permissions.
- **Utility menu** — `/menu` GUI for warps, homes, pending TPA (shows TPA vs TPA Here), and back.
- **Info & reload** — `/aura` for command list and toggle status; `/aura reload` for config reload (`aura.admin`).
- **Persistence** — `warps.yml`, `homes.yml`, and per-player toggle data; state applied on join.
- **Listeners** — Back tracking, menu clicks, god/fly/nofall/nohunger, session load/save.
- **Configuration** — `config.yml` for TPA timeout, RTP settings, global teleport countdown, home limits, and message prefix.
- **Permissions** — Granular nodes in `plugin.yml` plus `aura.admin` umbrella.

### Build

- Maven artifact `AuraUtils-1.0.0.jar` (and shaded build when enabled).

### Compatibility

- Java 21+
- Spigot / Paper API 1.21+

### Notes

- Install by placing the built JAR in `plugins/` and restarting the server.
- See [README.md](README.md) for usage and [plugin.yml](src/main/resources/plugin.yml) for all permission nodes.
