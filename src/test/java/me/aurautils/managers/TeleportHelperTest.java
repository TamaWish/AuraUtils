package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.test.TestAuraConfigs;
import me.aurautils.platform.ChunkLoadService;
import me.aurautils.test.StorageTestSupport;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeleportHelperTest {

    private AuraUtils plugin;
    private TeleportHelper helper;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        plugin = mock(AuraUtils.class);
        when(plugin.getPlatform()).thenReturn(StorageTestSupport.immediateChunkPlatform());

        AuraConfig config = TestAuraConfigs.defaults();
        when(plugin.getAuraConfig()).thenReturn(config);
        when(plugin.getChunkLoadService()).thenReturn(new ChunkLoadService(plugin, StorageTestSupport.immediateChunkPlatform()));

        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return mock(BukkitTask.class);
        });

        helper = new TeleportHelper(plugin);

        world = mock(World.class);
        when(world.getName()).thenReturn("world");
        player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
    }

    @Test
    void executeTeleport_runsChunkReadyThenTeleports() {
        Location dest = new Location(world, 100, 70, 200);

        helper.executeTeleport(player, dest, TeleportOptions.builder()
                .countdownSeconds(0)
                .noSuccessMessage()
                .build());

        verify(player).teleport(dest, PlayerTeleportEvent.TeleportCause.COMMAND);
    }
}
