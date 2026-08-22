# Changelog

All notable changes for AuraUtils.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-22

### Added
- Core utility commands:
  - `/back`, `/home`, `/sethome`, `/delhome`
  - `/warp`, `/setwarp`, `/delwarp`
  - `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel`
  - `/fly`, `/god`, `/nofall`, `/nohunger`
  - `/rtp`, `/menu`, `/aura`
- Random safe teleport (`/rtp`) with bounded retries, minimum distance, and surface safety checks.
- Shared teleport countdown system (chat / actionbar / both / none) with cancel-on-move, cancel-on-damage, and optional sounds.
- Granular permission nodes (see `plugin.yml`) including the `aura.admin` parent node.
- Per-player persistence for god / fly / nofall / nohunger (load on join, save on quit, forced sync write on toggle).
- Simple utility GUI (`/menu`).
- Configuration-driven defaults in `config.yml`.
- **bStats metrics** (plugin id 33574) with custom charts for config choices and feature usage.
  - Opt-out via the shared `plugins/bStats/config.yml` (`enabled: false`).

### Fixed (pre-release)
- **Fly (Spigot)**: `allowFlight` is re-applied at 1, 5, and 20 ticks after join, world change, respawn, and cross-world teleport. Spigot 26.x clears player abilities more aggressively than Paper; a single early call was often overwritten.
- **Fly**: world-change / respawn no longer force the player into a flying state; only the ability to fly is restored.
- **Fly**: disabling fly leaves Creative / Spectator flight alone; game-mode changes re-sync the stored fly state.
- **God**: enabling god fully heals, clears fire, and fills food/saturation; combust events are cancelled while god is active.
- **NoHunger**: saturation and exhaustion are kept full / zero; loss is blocked and effects re-applied on food-change events.
- **Persistence**: god / fly / nofall / nohunger toggles force a synchronous disk write so a crash cannot lose the latest state.

### Compatibility
- **Minecraft**: 1.21.x (tested through 1.21.11) **and** 26.1 / 26.2 (year-based numbering)
- **Server software**: Spigot and Paper
- **Java**: 21+ (plugin bytecode); 26.1+ servers themselves require Java 25
- Single JAR works on both version lines — no separate builds required

### Build
- Maven project targeting Java 21
- Spigot API 1.21.4 (provided)
- bStats 3.2.1 shaded and relocated to `me.aurautils.libs.bstats`
- Artifact: `AuraUtils-1.0.0.jar`

### Notes
- Install by dropping the JAR into `plugins/` and restarting the server.
- See `README.md` for usage, configuration, and permissions.
- Metrics disclosure and the standard bStats opt-out satisfy SpigotMC resource guidelines.
