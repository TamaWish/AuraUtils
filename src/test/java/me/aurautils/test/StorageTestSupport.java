package me.aurautils.test;

import me.aurautils.managers.HomeLimitPolicy;
import me.aurautils.platform.ChunkLoadPolicy;
import me.aurautils.platform.PlatformAdapter;
import me.aurautils.storage.PlayerLookup;
import me.aurautils.storage.WorldResolver;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class StorageTestSupport {

    private StorageTestSupport() {
    }

    public static WorldResolver worldResolver(String worldName) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        return name -> worldName.equals(name) ? world : null;
    }

    public static Location location(String worldName, double x, double y, double z) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        return new Location(world, x, y, z);
    }

    public static WorldResolver mapWorldResolver(Map<String, World> worlds) {
        return worlds::get;
    }

    public static PlayerLookup playerLookup(UUID playerId, String displayName) {
        return new PlayerLookup() {
            @Override
            public UUID resolveToken(String token, Set<UUID> knownHomeOwners) {
                if (playerId.toString().equals(token) || displayName.equalsIgnoreCase(token)) {
                    return playerId;
                }
                return null;
            }

            @Override
            public String displayName(UUID id) {
                return displayName;
            }

            @Override
            public List<String> tabCompleteTokens(Set<UUID> knownHomeOwners) {
                return List.of(displayName);
            }
        };
    }

    /**
     * Runs {@code onReady} synchronously on the calling thread (tests use DirectTaskExecutor semantics).
     */
    public static PlatformAdapter immediateChunkPlatform() {
        return new PlatformAdapter() {
            @Override
            public String getPlatformName() {
                return "Test";
            }

            @Override
            public boolean isPaper() {
                return false;
            }

            @Override
            public boolean supportsAsyncChunkLoading() {
                return true;
            }

            @Override
            public org.bukkit.inventory.Inventory createInventory(
                    org.bukkit.inventory.InventoryHolder owner, int size,
                    net.kyori.adventure.text.Component title) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isChunkLoaded(Location location) {
                return true;
            }

            @Override
            public void whenChunkReady(Location location, ChunkLoadPolicy policy, boolean generate, boolean urgent,
                                       Runnable onReady, Runnable onFailed) {
                onReady.run();
            }

            @Override
            public boolean hasPlayerMoved(PlayerMoveEvent event, boolean horizontalOnly) {
                return false;
            }

            @Override
            public boolean usesEventBasedMovementDetection() {
                return false;
            }
        };
    }

    public static HomeLimitPolicy homeLimit(int maxHomes) {
        return new HomeLimitPolicy() {
            @Override
            public int getMaxHomes(Player player) {
                return maxHomes;
            }
        };
    }
}
