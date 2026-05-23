package me.aurautils.managers;

import me.aurautils.storage.InMemoryDataStore;
import me.aurautils.storage.StoragePaths;
import me.aurautils.test.StorageTestSupport;
import me.aurautils.util.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpManagerTest {

    private InMemoryDataStore store;
    private WarpManager manager;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        manager = new WarpManager(store, StorageTestSupport.worldResolver("world"), Logger.getLogger("test"));
    }

    @Test
    void persistsAndResolvesAlias() {
        Location location = StorageTestSupport.location("world", 10, 64, 20);
        YamlConfiguration config = new YamlConfiguration();
        var warps = config.createSection("warps");
        var spawn = warps.createSection("spawn");
        LocationIO.write(spawn, location);
        spawn.set("aliases", List.of("shop"));
        store.save(StoragePaths.WARPS, config);

        manager.load();

        assertEquals("spawn", manager.resolveWarpName("shop"));
        assertNotNull(manager.getWarp("shop"));
    }

    @Test
    void deleteWarpClearsPersistedEntry() {
        manager.setWarp("arena", StorageTestSupport.location("world", 0, 70, 0));
        manager.save();
        assertTrue(manager.deleteWarp("arena"));
        manager.save();

        WarpManager reloaded = new WarpManager(store, StorageTestSupport.worldResolver("world"), Logger.getLogger("test"));
        reloaded.load();

        assertNull(reloaded.getWarp("arena"));
    }

    @Test
    void getWarpNamesSortedReturnsOnlyUncategorizedWhenFilterEmpty() {
        Location location = StorageTestSupport.location("world", 1, 64, 1);

        YamlConfiguration config = new YamlConfiguration();
        var warps = config.createSection("warps");
        var categorized = warps.createSection("mall");
        LocationIO.write(categorized, location);
        categorized.set("category", "shops");
        store.save(StoragePaths.WARPS, config);

        manager.load();
        manager.setWarp("plain", location);

        List<String> uncategorized = manager.getWarpNamesSorted("");
        assertTrue(uncategorized.contains("plain"));
        assertTrue(uncategorized.stream().noneMatch("mall"::equals));
    }
}
