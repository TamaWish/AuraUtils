package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.config.TeleportFeatureOverride;
import me.aurautils.test.StorageTestSupport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeleportServiceTest {

    private AuraUtils plugin;
    private BackManager backManager;
    private TeleportService service;
    private TeleportHelper helper;
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(AuraUtils.class);
        backManager = mock(BackManager.class);
        when(plugin.getBackManager()).thenReturn(backManager);
        when(plugin.getPlatform()).thenReturn(StorageTestSupport.immediateChunkPlatform());

        AuraConfig config = new AuraConfig(
                60, 5, true, true, 0,
                new TeleportFeatureOverride(0, true), TeleportFeatureOverride.EMPTY,
                2000, 100, 80, 10, true, false, false, false, true, 4, 3, 60,
                3, true, "aura.vanish.see", List.of("aura.vanish"), "en", "en", true);
        when(plugin.getAuraConfig()).thenReturn(config);

        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        });

        helper = new TeleportHelper(plugin);
        service = new TeleportService(plugin, helper);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
    }

    @Test
    void validateDestination_rejectsUnloadedWorld() {
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("resource");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(null);
            Location dest = new Location(world, 1, 64, 1);
            assertFalse(service.validateDestination(player, dest));
        }

    }

    @Test
    void teleport_skipBackRecord_notifiesBackManager() {
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        Location dest = new Location(world, 10, 64, 10);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            service.teleport(player, dest, TeleportOptions.builder()
                    .countdownSeconds(0)
                    .skipBackRecord(true)
                    .noSuccessMessage()
                    .build());
        }

        verify(backManager).skipNextRecord(eq(player.getUniqueId()));
        verify(player).teleport(any(Location.class), any());
    }

    @Test
    void countdownFor_adminUsesOverride() {
        assertEquals(0, service.countdownFor(TeleportService.TeleportKind.ADMIN));
    }

    @Test
    void countdownFor_rtpUsesRtpSectionWhenOverrideUnset() {
        assertEquals(3, service.countdownFor(TeleportService.TeleportKind.RTP));
    }

    @Test
    void hasPendingTeleport_falseWhenIdle() {
        assertFalse(service.hasPendingTeleport(player));
    }
}
