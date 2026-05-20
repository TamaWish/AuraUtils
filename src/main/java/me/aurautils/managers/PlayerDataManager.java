package me.aurautils.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory store for all per-player toggles and settings.
 * Data resets on server restart (no persistence needed for utility toggles).
 */
public class PlayerDataManager {

    private final Map<UUID, Boolean> godMode     = new HashMap<>();
    private final Map<UUID, Boolean> flyMode     = new HashMap<>();
    private final Map<UUID, Boolean> noFall      = new HashMap<>();
    private final Map<UUID, Boolean> noHunger    = new HashMap<>();
    private final Map<UUID, Double>  dmgMult     = new HashMap<>();

    // ── God ─────────────────────────────────────────────────────────────────
    public boolean isGod(UUID id)        { return godMode.getOrDefault(id, false); }
    public boolean toggleGod(UUID id)    { boolean v = !isGod(id); godMode.put(id, v); return v; }
    public void setGod(UUID id, boolean v) { godMode.put(id, v); }

    // ── Fly ─────────────────────────────────────────────────────────────────
    public boolean isFly(UUID id)        { return flyMode.getOrDefault(id, false); }
    public boolean toggleFly(UUID id)    { boolean v = !isFly(id); flyMode.put(id, v); return v; }
    public void setFly(UUID id, boolean v) { flyMode.put(id, v); }

    // ── No Fall ──────────────────────────────────────────────────────────────
    public boolean isNoFall(UUID id)     { return noFall.getOrDefault(id, false); }
    public boolean toggleNoFall(UUID id) { boolean v = !isNoFall(id); noFall.put(id, v); return v; }
    public void setNoFall(UUID id, boolean v) { noFall.put(id, v); }

    // ── No Hunger ────────────────────────────────────────────────────────────
    public boolean isNoHunger(UUID id)     { return noHunger.getOrDefault(id, false); }
    public boolean toggleNoHunger(UUID id) { boolean v = !isNoHunger(id); noHunger.put(id, v); return v; }
    public void setNoHunger(UUID id, boolean v) { noHunger.put(id, v); }

    // ── Damage Multiplier ────────────────────────────────────────────────────
    public double getDamageMultiplier(UUID id) { return dmgMult.getOrDefault(id, 1.0); }
    public void setDamageMultiplier(UUID id, double mult) { dmgMult.put(id, mult); }

    // ── Cleanup on quit ──────────────────────────────────────────────────────
    public void remove(UUID id) {
        godMode.remove(id);
        flyMode.remove(id);
        noFall.remove(id);
        noHunger.remove(id);
        dmgMult.remove(id);
    }
}
