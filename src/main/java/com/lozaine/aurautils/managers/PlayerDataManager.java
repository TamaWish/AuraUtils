package com.lozaine.aurautils.managers;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent store for all per-player toggles and settings.
 * Toggle changes (god/fly/nofall/nohunger) force a synchronous save so state
 * is never lost on crash. Other changes may use deferred async save.
 * Shutdown always flushes synchronously and never schedules new tasks.
 *
 * <p>Trusted TPA lists are also stored here (one-way: owner trusts target →
 * target may TPA to owner without confirmation). Uses concurrent collections
 * so reads/writes remain safe across Folia region threads.
 */
public class PlayerDataManager {

    private final AuraUtils plugin;
    private final File dataFile;

    private final Map<UUID, Boolean> godMode = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> flyMode = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> noFall = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> noHunger = new ConcurrentHashMap<>();

    /**
     * owner UUID → set of trusted player UUIDs.
     * When A trusts B, B can /tpa A and the request auto-accepts.
     */
    private final Map<UUID, Set<UUID>> trustedPlayers = new ConcurrentHashMap<>();

    /** Last known names for trusted UUIDs (display when offline). */
    private final Map<UUID, String> knownNames = new ConcurrentHashMap<>();

    /** True once onDisable has started — no new tasks may be scheduled. */
    private volatile boolean shuttingDown = false;

    /** Pending deferred save task (cancelled / flushed on shutdown). */
    private WrappedTask pendingSaveTask = null;

    /** Delay before writing to disk after a non-toggle change (ticks). */
    private static final long SAVE_DELAY_TICKS = 40L; // 2 seconds

    public PlayerDataManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player-states.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        godMode.clear();
        flyMode.clear();
        noFall.clear();
        noHunger.clear();
        trustedPlayers.clear();
        knownNames.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String playerKey : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.getConfigurationSection(playerKey);
            if (playerSection == null) {
                continue;
            }

            if (playerSection.contains("god")) {
                godMode.put(playerId, playerSection.getBoolean("god"));
            }
            if (playerSection.contains("fly")) {
                flyMode.put(playerId, playerSection.getBoolean("fly"));
            }
            if (playerSection.contains("nofall")) {
                noFall.put(playerId, playerSection.getBoolean("nofall"));
            }
            if (playerSection.contains("nohunger")) {
                noHunger.put(playerId, playerSection.getBoolean("nohunger"));
            }

            List<String> trustedList = playerSection.getStringList("trusted");
            if (!trustedList.isEmpty()) {
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String entry : trustedList) {
                    // Format: uuid or uuid:Name
                    String uuidPart = entry;
                    String namePart = null;
                    int colon = entry.indexOf(':');
                    if (colon > 0) {
                        uuidPart = entry.substring(0, colon);
                        namePart = entry.substring(colon + 1);
                    }
                    try {
                        UUID trustedId = UUID.fromString(uuidPart);
                        set.add(trustedId);
                        if (namePart != null && !namePart.isBlank()) {
                            knownNames.put(trustedId, namePart);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // skip bad entry
                    }
                }
                if (!set.isEmpty()) {
                    trustedPlayers.put(playerId, set);
                }
            }

            String lastName = playerSection.getString("name");
            if (lastName != null && !lastName.isBlank()) {
                knownNames.put(playerId, lastName);
            }
        }
    }

    /**
     * Request a deferred save (for non-critical bulk changes).
     * During / after shutdown it writes synchronously and never schedules a task.
     */
    public void save() {
        if (shuttingDown || !plugin.isEnabled()) {
            flushSave();
            return;
        }
        scheduleDeferredSave();
    }

    /**
     * Force an immediate synchronous write. Used by all toggle setters so a
     * crash cannot lose the latest god/fly/nofall/nohunger state.
     */
    public void saveSync() {
        flushSave();
    }

    /**
     * Mark that the plugin is shutting down. Cancels any pending deferred save
     * and forces an immediate synchronous write. Safe to call from onDisable.
     */
    public void prepareShutdown() {
        shuttingDown = true;
        cancelPendingSave();
        flushSave();
    }

    /**
     * Synchronous disk write. Never touches the scheduler.
     * This is the only path used during disable and on toggles.
     */
    public void flushSave() {
        cancelPendingSave();
        writeToDisk();
    }

    private void scheduleDeferredSave() {
        // Cancel previous pending save so we only write once after a burst of changes
        cancelPendingSave();
        if (shuttingDown || !plugin.isEnabled()) {
            writeToDisk();
            return;
        }
        pendingSaveTask = plugin.getScheduler().runAsyncLater(() -> {
            // If we were disabled while waiting, still write (but do not re-schedule)
            writeToDisk();
            pendingSaveTask = null;
        }, SAVE_DELAY_TICKS);
    }

    private void cancelPendingSave() {
        if (pendingSaveTask != null) {
            try {
                pendingSaveTask.cancel();
            } catch (Exception ignored) {
                // Task may already be complete
            }
            pendingSaveTask = null;
        }
    }

    private void writeToDisk() {
        plugin.getDataFolder().mkdirs();

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection playersSection = config.createSection("players");
        Set<UUID> playerIds = new HashSet<>();
        playerIds.addAll(godMode.keySet());
        playerIds.addAll(flyMode.keySet());
        playerIds.addAll(noFall.keySet());
        playerIds.addAll(noHunger.keySet());
        playerIds.addAll(trustedPlayers.keySet());

        for (UUID playerId : playerIds) {
            boolean god = godMode.getOrDefault(playerId, false);
            boolean fly = flyMode.getOrDefault(playerId, false);
            boolean noFallEnabled = noFall.getOrDefault(playerId, false);
            boolean noHungerEnabled = noHunger.getOrDefault(playerId, false);
            Set<UUID> trusted = trustedPlayers.get(playerId);
            boolean hasTrusted = trusted != null && !trusted.isEmpty();

            if (!god && !fly && !noFallEnabled && !noHungerEnabled && !hasTrusted) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.createSection(playerId.toString());
            playerSection.set("god", god);
            playerSection.set("fly", fly);
            playerSection.set("nofall", noFallEnabled);
            playerSection.set("nohunger", noHungerEnabled);

            String name = knownNames.get(playerId);
            if (name != null) {
                playerSection.set("name", name);
            }

            if (hasTrusted) {
                List<String> entries = new ArrayList<>();
                for (UUID tid : trusted) {
                    String tName = knownNames.get(tid);
                    if (tName != null && !tName.isBlank()) {
                        entries.add(tid.toString() + ":" + tName);
                    } else {
                        entries.add(tid.toString());
                    }
                }
                Collections.sort(entries);
                playerSection.set("trusted", entries);
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-states.yml: " + exception.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Trusted TPA list
    // ------------------------------------------------------------------

    /** True if {@code owner} has {@code trusted} on their trusted list. */
    public boolean isTrusted(UUID owner, UUID trusted) {
        if (owner == null || trusted == null) {
            return false;
        }
        Set<UUID> set = trustedPlayers.get(owner);
        return set != null && set.contains(trusted);
    }

    /**
     * Add {@code trusted} to {@code owner}'s trusted list.
     * @return true if newly added, false if already present or limit reached
     */
    public boolean addTrusted(UUID owner, UUID trusted, String trustedName) {
        if (owner == null || trusted == null || owner.equals(trusted)) {
            return false;
        }
        int max = Math.max(0, plugin.getConfig().getInt("tpa.trusted-max", 50));
        Set<UUID> set = trustedPlayers.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet());
        if (set.contains(trusted)) {
            if (trustedName != null && !trustedName.isBlank()) {
                knownNames.put(trusted, trustedName);
            }
            return false;
        }
        if (max > 0 && set.size() >= max) {
            return false;
        }
        set.add(trusted);
        if (trustedName != null && !trustedName.isBlank()) {
            knownNames.put(trusted, trustedName);
        }
        save();
        return true;
    }

    /**
     * Remove {@code trusted} from {@code owner}'s list.
     * @return true if removed
     */
    public boolean removeTrusted(UUID owner, UUID trusted) {
        if (owner == null || trusted == null) {
            return false;
        }
        Set<UUID> set = trustedPlayers.get(owner);
        if (set == null) {
            return false;
        }
        boolean removed = set.remove(trusted);
        if (set.isEmpty()) {
            trustedPlayers.remove(owner, set);
        }
        if (removed) {
            save();
        }
        return removed;
    }

    /** Unmodifiable snapshot of trusted UUIDs for {@code owner}. */
    public Set<UUID> getTrusted(UUID owner) {
        Set<UUID> set = trustedPlayers.get(owner);
        if (set == null || set.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(set));
    }

    public int getTrustedCount(UUID owner) {
        Set<UUID> set = trustedPlayers.get(owner);
        return set == null ? 0 : set.size();
    }

    /** Best-effort display name for a UUID (online name → knownNames → short UUID). */
    public String getDisplayName(UUID id) {
        if (id == null) {
            return "?";
        }
        Player online = plugin.getServer().getPlayer(id);
        if (online != null) {
            knownNames.put(id, online.getName());
            return online.getName();
        }
        String known = knownNames.get(id);
        if (known != null && !known.isBlank()) {
            return known;
        }
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(id);
        String name = off.getName();
        if (name != null && !name.isBlank()) {
            knownNames.put(id, name);
            return name;
        }
        String shortId = id.toString().substring(0, 8);
        return shortId;
    }

    /** Remember a player's current name (join / trust add). */
    public void rememberName(UUID id, String name) {
        if (id != null && name != null && !name.isBlank()) {
            knownNames.put(id, name);
        }
    }

    // ------------------------------------------------------------------
    // Toggles
    // ------------------------------------------------------------------

    public boolean isGod(UUID id) {
        return godMode.getOrDefault(id, false);
    }

    public boolean toggleGod(UUID id) {
        boolean value = !isGod(id);
        setGod(id, value);
        return value;
    }

    public void setGod(UUID id, boolean value) {
        updateBoolean(godMode, id, value);
        saveSync();
    }

    public boolean isFly(UUID id) {
        return flyMode.getOrDefault(id, false);
    }

    public boolean toggleFly(UUID id) {
        boolean value = !isFly(id);
        setFly(id, value);
        return value;
    }

    public void setFly(UUID id, boolean value) {
        updateBoolean(flyMode, id, value);
        saveSync();
    }

    public boolean isNoFall(UUID id) {
        return noFall.getOrDefault(id, false);
    }

    public boolean toggleNoFall(UUID id) {
        boolean value = !isNoFall(id);
        setNoFall(id, value);
        return value;
    }

    public void setNoFall(UUID id, boolean value) {
        updateBoolean(noFall, id, value);
        saveSync();
    }

    public boolean isNoHunger(UUID id) {
        return noHunger.getOrDefault(id, false);
    }

    public boolean toggleNoHunger(UUID id) {
        boolean value = !isNoHunger(id);
        setNoHunger(id, value);
        return value;
    }

    public void setNoHunger(UUID id, boolean value) {
        updateBoolean(noHunger, id, value);
        saveSync();
    }

    /**
     * Apply stored states to an online player (join, enable, world/respawn restore).
     * Non-fly effects run 1 tick later. Fly is re-applied at multiple delays because
     * Spigot 26.2 resets player abilities after world change / login more aggressively
     * than Paper — a single early setAllowFlight is often overwritten.
     */
    public void applyTo(Player player) {
        if (!plugin.isEnabled() || shuttingDown) {
            return;
        }

        rememberName(player.getUniqueId(), player.getName());

        // Fly: multi-tick reapply (Spigot-safe)
        scheduleFlyReapply(player);

        // God / nohunger: single delayed apply is enough (event-based / soft state)
        plugin.getScheduler().runAtEntityLater(player, () -> {
            if (!player.isOnline() || !plugin.isEnabled() || shuttingDown) {
                return;
            }
            UUID playerId = player.getUniqueId();
            if (isNoHunger(playerId)) {
                applyNoHungerEffects(player);
            }
            if (isGod(playerId)) {
                applyGodEffects(player);
            }
            // Ensure fly flag matches stored state once more after other systems settle
            reapplyFly(player);
        }, 1L);
    }

    /**
     * Immediately push stored fly state onto the player.
     * Does NOT force setFlying(true) — only restores the ability to fly.
     */
    public void reapplyFly(Player player) {
        if (!player.isOnline()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (isFly(playerId)) {
            player.setAllowFlight(true);
        } else if (player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    /**
     * Re-apply allowFlight at 1, 5, and 20 ticks. Needed on Spigot where abilities
     * are cleared again after PlayerChangedWorldEvent / join processing.
     * Safe no-op when fly is not enabled for the player.
     */
    public void scheduleFlyReapply(Player player) {
        if (!plugin.isEnabled() || shuttingDown) {
            return;
        }
        if (!isFly(player.getUniqueId())) {
            // Still push a correct "off" state once in case abilities were left on
            plugin.getScheduler().runAtEntityLater(player, () -> {
                if (player.isOnline() && plugin.isEnabled() && !shuttingDown) {
                    reapplyFly(player);
                }
            }, 1L);
            return;
        }
        for (long delay : new long[]{1L, 5L, 20L}) {
            plugin.getScheduler().runAtEntityLater(player, () -> {
                if (!plugin.isEnabled() || shuttingDown || !player.isOnline()) {
                    return;
                }
                if (isFly(player.getUniqueId())) {
                    player.setAllowFlight(true);
                }
            }, delay);
        }
    }

    /** Full heal, clear fire, used when god is toggled on or re-applied. */
    public void applyGodEffects(Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.setFireTicks(0);
        try {
            player.setHealth(player.getMaxHealth());
        } catch (Exception ignored) {
            // Attribute missing or health locked by another plugin
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
    }

    /** Keep food/saturation full and exhaustion zero while no-hunger is active. */
    public void applyNoHungerEffects(Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
    }

    private void updateBoolean(Map<UUID, Boolean> state, UUID id, boolean value) {
        if (value) {
            state.put(id, true);
        } else {
            state.remove(id);
        }
    }
}
