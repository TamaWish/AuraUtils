package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.managers.TeleportService.TeleportKind;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/** Shared /rtp entry point for commands and menus. */
public final class RtpService {

    private RtpService() {
    }

    public static void startRtp(AuraUtils plugin, Player player) {
        if (!plugin.requireFeature(player, "rtp")) {
            return;
        }
        if (!player.hasPermission("aura.rtp")) {
            plugin.send(player, "general.no-permission");
            return;
        }

        AuraConfig config = plugin.getAuraConfig();
        List<String> worlds = config.rtpWorlds();
        if (worlds.size() > 1) {
            plugin.getMenuManager().openRtpWorldsMenu(player);
            return;
        }

        World targetWorld = resolveTargetWorld(plugin, player, worlds);
        if (targetWorld == null) {
            plugin.send(player, "rtp.world-invalid");
            return;
        }

        beginSearch(plugin, player, targetWorld);
    }

    public static void beginSearch(AuraUtils plugin, Player player, World searchWorld) {
        AuraConfig config = plugin.getAuraConfig();
        boolean bypassCooldown = player.hasPermission("aura.rtp.cooldown.bypass");
        long remaining = bypassCooldown ? 0
                : plugin.getRtpCooldownManager().remainingSeconds(player.getUniqueId(), config.rtpCooldown());
        if (remaining > 0) {
            plugin.send(player, "rtp.cooldown", MessagePlaceholders.of("seconds", String.valueOf(remaining)));
            return;
        }

        plugin.send(player, "rtp.searching");

        int rtpCountdown = plugin.getTeleportService().countdownFor(TeleportKind.RTP);
        AsyncRtpEngine engine = plugin.getAsyncRtpEngine();

        engine.search(player, searchWorld, bypassCooldown, new AsyncRtpEngine.ResultHandler() {
            @Override
            public void onFound(org.bukkit.Location destination, int blocksAway) {
                if (!player.isOnline()) {
                    return;
                }
                if (rtpCountdown > 0) {
                    plugin.send(player, "rtp.found-countdown",
                            MessagePlaceholders.of("blocks", String.valueOf(blocksAway)));
                    engine.teleportWithCountdown(player, destination, rtpCountdown);
                } else {
                    plugin.send(player, "rtp.success-instant",
                            MessagePlaceholders.of("blocks", String.valueOf(blocksAway)));
                }
            }

            @Override
            public void onFailed() {
                if (player.isOnline()) {
                    plugin.send(player, "rtp.failed");
                }
            }
        });
    }

    public static World resolveTargetWorld(AuraUtils plugin, Player player, List<String> configuredWorlds) {
        if (configuredWorlds.isEmpty()) {
            return player.getWorld();
        }
        if (configuredWorlds.size() == 1) {
            return plugin.getServer().getWorld(configuredWorlds.getFirst());
        }
        return null;
    }

    public static World resolveNamedWorld(AuraUtils plugin, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        List<String> allowed = plugin.getAuraConfig().rtpWorlds();
        if (!allowed.isEmpty() && !allowed.contains(world.getName())) {
            return null;
        }
        return world;
    }
}
