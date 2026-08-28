# AuraUtils 1.1.1 — Folia fixes

### Fixed
- **Folia RTP crash** — `IllegalStateException: Cannot retrieve chunk asynchronously` when `/rtp` picked a location outside the player's current region.
  - Height / surface checks now run on the **target location's region** via `runAtLocation`.
  - Generator stays on the player entity scheduler for pacing only.
  - Same behaviour on Spigot/Paper; no config changes.
- **Folia `/back` empty** — `PlayerTeleportEvent` does not fire for most teleports on Folia (including plugin/async), so the old event-only recorder never stored a location.
  - Primary recording now happens in `TeleportHelper.teleportExact` **before** the teleport (Spigot / Paper / Folia).
  - `BackListener` remains as a fallback for causes that still fire the event (chorus fruit, bed exit, etc.).
  - Back locations are stored as a durable world-name + coordinates snapshot (avoids null World references across regions / reloads).

### Upgrade
Drop the new jar over 1.1.0. No config or data migration required.
