package me.aurautils.managers;

import me.aurautils.storage.DirectTaskExecutor;
import me.aurautils.storage.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataManagerTest {

    private static final UUID PLAYER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private InMemoryDataStore store;
    private DirectTaskExecutor tasks;
    private PlayerDataManager manager;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        tasks = new DirectTaskExecutor();
        manager = new PlayerDataManager(store, tasks, null, Logger.getLogger("test"));
    }

    @Test
    void persistsGodToggleAcrossReload() {
        manager.setGod(PLAYER_ID, true);
        manager.flushSave();

        PlayerDataManager reloaded = new PlayerDataManager(store, tasks, null, Logger.getLogger("test"));
        reloaded.load();

        assertTrue(reloaded.isGod(PLAYER_ID));
    }

    @Test
    void clearingToggleRemovesPersistedFlag() {
        manager.setGod(PLAYER_ID, true);
        manager.setGod(PLAYER_ID, false);
        manager.flushSave();

        PlayerDataManager reloaded = new PlayerDataManager(store, tasks, null, Logger.getLogger("test"));
        reloaded.load();

        assertFalse(reloaded.isGod(PLAYER_ID));
    }

    @Test
    void localeOverrideRoundTrips() {
        manager.setLocaleOverride(PLAYER_ID, "es");
        manager.flushSave();

        PlayerDataManager reloaded = new PlayerDataManager(store, tasks, null, Logger.getLogger("test"));
        reloaded.load();

        assertEquals("es", reloaded.getLocaleOverride(PLAYER_ID));
    }
}
