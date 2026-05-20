# Changelog

All notable changes for AuraUtils.

## [1.0.0] - 2026-05-20
### Added
- Player utility commands: `/back`, `/home`, `/sethome`, `/delhome`, `/warp`, `/setwarp`, `/delwarp`, `/tpa`, `/tpaccept`, `/tpadeny`, `/fly`, `/god`, `/nofall`, `/nohunger`, `/damage`, `/light`, `/menu`, `/rtp`, `/aura`
- Random safe teleport command `/rtp` with bounded retries and surface safety checks.
- Granular permission nodes (see `plugin.yml`) including `aura.admin` umbrella node.
- Basic per-player persistence (load on join, save on quit).
- Listeners for back-location tracking, damage multipliers, menu interactions, and more.
- Configuration-driven defaults in `config.yml` (TPA timeout, damage multiplier defaults and limits, chat prefix).

### Build
- Initial packaged artifacts: `target/AuraUtils-1.0.0.jar` and shaded artifact `target/AuraUtils-1.0.0-shaded.jar`.

### Compatibility
- Java 21+
- Spigot / Paper 1.21+

### Notes
- Install by dropping the built jar into the server `plugins/` folder and restarting.
- See `README.md` for usage and `plugin.yml` for full permission nodes.
