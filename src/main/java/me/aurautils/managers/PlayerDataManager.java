package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.storage.BukkitTaskExecutor;
import me.aurautils.storage.DataStore;
import me.aurautils.storage.StoragePaths;
import me.aurautils.storage.TaskExecutor;
import me.aurautils.storage.YamlDataStore;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import me.aurautils.storage.Cancellable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Persistent store for all per-player toggles and settings.
 * Disk writes are serialized through a single async queue and file lock.
 */
public class PlayerDataManager {

    private static final long SAVE_DEBOUNCE_TICKS = 40L;

    private final DataStore dataStore;
    private final TaskExecutor taskExecutor;
    private final Logger logger;
    private final AuraUtils plugin;
    private final ReentrantLock fileLock = new ReentrantLock();

    private final Map<UUID, Boolean> godMode = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> flyMode = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> noFall = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> noHunger = new ConcurrentHashMap<>();
    /** Optional per-player message locale override (e.g. en, es). */
    private final Map<UUID, String> localeOverrides = new ConcurrentHashMap<>();

    private final Set<UUID> pendingPlayerSaves = ConcurrentHashMap.newKeySet();
    private volatile boolean pendingFullSave;
    private volatile boolean saveWorkerActive;

    private Cancellable debounceTask;

    public PlayerDataManager(AuraUtils plugin) {
        this(new YamlDataStore(plugin), new BukkitTaskExecutor(plugin), plugin, plugin.getLogger());
    }

    public PlayerDataManager(DataStore dataStore, TaskExecutor taskExecutor, AuraUtils plugin, Logger logger) {
        this.dataStore = dataStore;
        this.taskExecutor = taskExecutor;
        this.plugin = plugin;
        this.logger = logger;
    }

    public void load() {
        fileLock.lock();
        try {
            godMode.clear();
            flyMode.clear();
            noFall.clear();
            noHunger.clear();
            localeOverrides.clear();

            YamlConfiguration config = dataStore.load(StoragePaths.PLAYER_STATES);
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
                if (playerSection.contains("locale")) {
                    String locale = MessagesManager.normalizeLocale(playerSection.getString("locale"));
                    if (!locale.isEmpty()) {
                        localeOverrides.put(playerId, locale);
                    }
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
        debounceTask = taskExecutor.runSyncLater(() -> {
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
        if (saveWorkerActive || !taskExecutor.isPluginEnabled()) {
            return;
        }
        saveWorkerActive = true;
        taskExecutor.runAsync(this::runSaveDrain);
    }

    private void runSaveDrain() {
        try {
            while (taskExecutor.isPluginEnabled() && hasPendingWrites()) {
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

            if (!taskExecutor.isPluginEnabled() && hasPendingWrites()) {
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
            YamlConfiguration config = dataStore.load(StoragePaths.PLAYER_STATES);
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            if (playersSection == null) {
                playersSection = config.createSection("players");
            }

            for (UUID playerId : collectPlayerIdsForPersist(playersSection)) {
                writePlayerSection(playersSection, playerId);
            }

            dataStore.save(StoragePaths.PLAYER_STATES, config);
        } finally {
            fileLock.unlock();
        }
    }

    private void persistPlayerToDisk(UUID playerId) {
        fileLock.lock();
        try {
            YamlConfiguration config = dataStore.load(StoragePaths.PLAYER_STATES);
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            if (playersSection == null) {
                playersSection = config.createSection("players");
            }

            writePlayerSection(playersSection, playerId);
            dataStore.save(StoragePaths.PLAYER_STATES, config);
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
        String locale = localeOverrides.get(playerId);
        if (locale != null) {
            playerSection.set("locale", locale);
        }
    }

    private Set<UUID> collectKnownPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();
        playerIds.addAll(godMode.keySet());
        playerIds.addAll(flyMode.keySet());
        playerIds.addAll(noFall.keySet());
        playerIds.addAll(noHunger.keySet());
        playerIds.addAll(localeOverrides.keySet());
        return playerIds;
    }

    /** In-memory toggles plus any player already present on disk (so cleared toggles are removed). */
    private Set<UUID> collectPlayerIdsForPersist(ConfigurationSection playersSection) {
        Set<UUID> playerIds = collectKnownPlayerIds();
        if (playersSection != null) {
            for (String playerKey : playersSection.getKeys(false)) {
                try {
                    playerIds.add(UUID.fromString(playerKey));
                } catch (IllegalArgumentException ignored) {
                    // skip non-UUID keys
                }
            }
        }
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

    public String getLocaleOverride(UUID playerId) {
        return localeOverrides.get(playerId);
    }

    public void setLocaleOverride(UUID playerId, String locale) {
        if (locale == null || locale.isBlank()) {
            localeOverrides.remove(playerId);
        } else {
            localeOverrides.put(playerId, MessagesManager.normalizeLocale(locale));
        }
        scheduleSave();
    }

    private boolean hasStoredToggles(UUID playerId) {
        return isGod(playerId) || isFly(playerId) || isNoFall(playerId) || isNoHunger(playerId)
                || localeOverrides.containsKey(playerId);
    }

    private void updateBoolean(Map<UUID, Boolean> state, UUID id, boolean value) {
        if (value) {
            state.put(id, true);
        } else {
            state.remove(id);
        }
    }
}
