# AuraUtils 1.2.0 — Trusted / instant TPA

### Added
- **Trusted TPA list** — friends you trust can teleport to you without `/tpaccept`.
  - `/tpatrust <player>` — add to list
  - `/tpatrust list` — view list
  - `/tpauntrust <player>` (or `/tpatrust remove <player>`) — remove
  - When someone on your list runs `/tpa You`, the request auto-accepts (same countdown as normal TPA unless configured otherwise).
- Config options:
  - `tpa.trusted-max: 50` — max trusted players (0 = unlimited)
  - `tpa.trusted-instant: false` — set `true` to skip the teleport countdown for trusted TPAs
- Permission `aura.tpa.trust` (default: true)

### Folia
- Trusted teleports are scheduled on the **requester entity** scheduler.
- Trusted lists and TPA pending maps use concurrent collections for cross-region safety.

### Upgrade
Drop the new jar over 1.1.x. Existing `player-states.yml` and `config.yml` are compatible; new keys appear with defaults on first load if you regenerate config, or add them manually:

```yaml
tpa:
  timeout: 60
  trusted-max: 50
  trusted-instant: false
```
