package me.aurautils.managers;

import me.aurautils.storage.InMemoryDataStore;
import me.aurautils.test.StorageTestSupport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeManagerTest {

    private static final UUID PLAYER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private InMemoryDataStore store;
    private HomeManager manager;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        manager = newManager();
    }

    private HomeManager newManager() {
        return new HomeManager(
                store,
                StorageTestSupport.worldResolver("world"),
                StorageTestSupport.playerLookup(PLAYER_ID, "Tester"),
                StorageTestSupport.homeLimit(2),
                Logger.getLogger("test")
        );
    }

    @Test
    void roundTripsHomeThroughStore() {
        manager.setHome(PLAYER_ID, "base", StorageTestSupport.location("world", 1, 64, 2));
        manager.save();

        HomeManager reloaded = newManager();
        reloaded.load();

        Location home = reloaded.getHome(PLAYER_ID, "base");
        assertNotNull(home);
        assertEquals(1.0, home.getX(), 0.01);
    }

    @Test
    void enforcesHomeLimitForNewHomes() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);

        manager.setHome(PLAYER_ID, "one", StorageTestSupport.location("world", 0, 64, 0));
        manager.setHome(PLAYER_ID, "two", StorageTestSupport.location("world", 1, 64, 1));

        assertFalse(manager.canSetHome(player, "three"));
        assertTrue(manager.canSetHome(player, "one"));
    }

    @Test
    void deleteHomeRemovesEntry() {
        manager.setHome(PLAYER_ID, "base", StorageTestSupport.location("world", 0, 64, 0));
        assertTrue(manager.deleteHome(PLAYER_ID, "base"));
        assertEquals(0, manager.getHomeCount(PLAYER_ID));
    }
}
