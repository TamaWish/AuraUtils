package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Persistent store for all per-player toggles and settings.
 * Disk writes are serialized through a single async queue and file lock.
 */
public class PlayerDataManager {

    private static final long SAVE_DEBOUNCE_TICKS = 40L;

    private final AuraUtils plugin;
    private final File dataFile;
    private final ReentrantLock fileLock = new ReentrantLock();

    private final Map<UUID, Boolean> godMode = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> flyMode = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> noFall = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> noHunger = new ConcurrentHashMap<>();

    private final Set<UUID> pendingPlayerSaves = ConcurrentHashMap.newKeySet();
    private volatile boolean pendingFullSave;
    private volatile boolean saveWorkerActive;

    private BukkitTask debounceTask;

    public PlayerDataManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player-states.yml");
    }

    public void load() {
        fileLock.lock();
        try {
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
        } finally {
            fileLock.unlock();
        }
    }

    public void scheduleSave() {
        pendingFullSave = true;
        if (debounceTask != null) {
            debounceTask.cancel();
        }
        debounceTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            debounceTask = null;
            requestSaveDrain();
        }, SAVE_DEBOUNCE_TICKS);
    }

    public void flushSave() {
        if (debounceTask != null) {
            debounceTask.cancel();
            debounceTask = null;
        }
        pendingFullSave = false;
        pendingPlayerSaves.clear();
        persistAllToDisk();
    }

    /** Queues a single-player persist (e.g. on quit). */
    public void savePlayer(UUID playerId) {
        pendingPlayerSaves.add(playerId);
        requestSaveDrain();
    }

    private void requestSaveDrain() {
        if (saveWorkerActive || !plugin.isEnabled()) {
            return;
        }
        saveWorkerActive = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::runSaveDrain);
    }

    private void runSaveDrain() {
        try {
            while (plugin.isEnabled() && hasPendingWrites()) {
                boolean fullSave = pendingFullSave;
                pendingFullSave = false;

                Set<UUID> playerIds = Set.copyOf(pendingPlayerSaves);
                pendingPlayerSaves.clear();

                if (fullSave) {
                    persistAllToDisk();
                    continue;
                }

                for (UUID playerId : playerIds) {
                    persistPlayerToDisk(playerId);
                }
            }

            if (!plugin.isEnabled() && hasPendingWrites()) {
                boolean fullSave = pendingFullSave;
                pendingFullSave = false;
                Set<UUID> playerIds = Set.copyOf(pendingPlayerSaves);
                pendingPlayerSaves.clear();

                if (fullSave) {
                    persistAllToDisk();
                } else {
                    for (UUID playerId : playerIds) {
                        persistPlayerToDisk(playerId);
                    }
                }
            }
        } finally {
            saveWorkerActive = false;
            if (hasPendingWrites()) {
                requestSaveDrain();
            }
        }
    }

    private boolean hasPendingWrites() {
        return pendingFullSave || !pendingPlayerSaves.isEmpty();
    }

    private void persistAllToDisk() {
        fileLock.lock();
        try {
            plugin.getDataFolder().mkdirs();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            if (playersSection == null) {
                playersSection = config.createSection("players");
            }

            for (UUID playerId : collectKnownPlayerIds()) {
                writePlayerSection(playersSection, playerId);
            }

            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-states.yml: " + exception.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private void persistPlayerToDisk(UUID playerId) {
        fileLock.lock();
        try {
            plugin.getDataFolder().mkdirs();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            if (playersSection == null) {
                playersSection = config.createSection("players");
            }

            writePlayerSection(playersSection, playerId);
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-states.yml for " + playerId + ": " + exception.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private void writePlayerSection(ConfigurationSection playersSection, UUID playerId) {
        String playerKey = playerId.toString();
        if (!hasStoredToggles(playerId)) {
            playersSection.set(playerKey, null);
            return;
        }

        ConfigurationSection playerSection = playersSection.createSection(playerKey);
        playerSection.set("god", isGod(playerId));
        playerSection.set("fly", isFly(playerId));
        playerSection.set("nofall", isNoFall(playerId));
        playerSection.set("nohunger", isNoHunger(playerId));
    }

    private Set<UUID> collectKnownPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();
        playerIds.addAll(godMode.keySet());
        playerIds.addAll(flyMode.keySet());
        playerIds.addAll(noFall.keySet());
        playerIds.addAll(noHunger.keySet());
        return playerIds;
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
        scheduleSave();
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
        scheduleSave();
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
        scheduleSave();
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
        scheduleSave();
    }

    public void applyTo(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            UUID playerId = player.getUniqueId();

            boolean flyEnabled = isFly(playerId);
            if (flyEnabled) {
                player.setAllowFlight(true);
            } else if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }

            if (isNoHunger(playerId)) {
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
            }

            if (isGod(playerId)) {
                player.setFireTicks(0);
            }
        }, 1L);
    }

    private boolean hasStoredToggles(UUID playerId) {
        return isGod(playerId) || isFly(playerId) || isNoFall(playerId) || isNoHunger(playerId);
    }

    private void updateBoolean(Map<UUID, Boolean> state, UUID id, boolean value) {
        if (value) {
            state.put(id, true);
        } else {
            state.remove(id);
        }
    }
}
