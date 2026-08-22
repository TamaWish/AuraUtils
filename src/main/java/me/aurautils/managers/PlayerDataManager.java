package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent store for all per-player toggles and settings.
 * Toggle changes (god/fly/nofall/nohunger) force a synchronous save so state
 * is never lost on crash. Other changes may use deferred async save.
 * Shutdown always flushes synchronously and never schedules new tasks.
 */
public class PlayerDataManager {

    private final AuraUtils plugin;
    private final File dataFile;

    private final Map<UUID, Boolean> godMode = new HashMap<>();
    private final Map<UUID, Boolean> flyMode = new HashMap<>();
    private final Map<UUID, Boolean> noFall = new HashMap<>();
    private final Map<UUID, Boolean> noHunger = new HashMap<>();

    /** True once onDisable has started — no new tasks may be scheduled. */
    private volatile boolean shuttingDown = false;

    /** Pending deferred save task (cancelled / flushed on shutdown). */
    private BukkitTask pendingSaveTask = null;

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
        pendingSaveTask = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
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

        for (UUID playerId : playerIds) {
            boolean god = godMode.getOrDefault(playerId, false);
            boolean fly = flyMode.getOrDefault(playerId, false);
            boolean noFallEnabled = noFall.getOrDefault(playerId, false);
            boolean noHungerEnabled = noHunger.getOrDefault(playerId, false);

            if (!god && !fly && !noFallEnabled && !noHungerEnabled) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.createSection(playerId.toString());
            playerSection.set("god", god);
            playerSection.set("fly", fly);
            playerSection.set("nofall", noFallEnabled);
            playerSection.set("nohunger", noHungerEnabled);
        }

        try {
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-states.yml: " + exception.getMessage());
        }
    }

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

        // Fly: multi-tick reapply (Spigot-safe)
        scheduleFlyReapply(player);

        // God / nohunger: single delayed apply is enough (event-based / soft state)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.isEnabled() && !shuttingDown) {
                    reapplyFly(player);
                }
            }, 1L);
            return;
        }
        for (long delay : new long[]{1L, 5L, 20L}) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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
