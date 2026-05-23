package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.AsyncRtpEngine;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RtpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public RtpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "console.rtp-only-players");
            return true;
        }
        if (!player.hasPermission("aura.rtp")) {
            plugin.send(player, "general.no-permission");
            return true;
        }

        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt("rtp.cooldown", 0));
        boolean bypassCooldown = player.hasPermission("aura.rtp.cooldown.bypass");
        long remaining = bypassCooldown ? 0 : plugin.getRtpCooldownManager().remainingSeconds(player.getUniqueId(), cooldownSeconds);
        if (remaining > 0) {
            plugin.send(player, "rtp.cooldown", MessagePlaceholders.of("seconds", String.valueOf(remaining)));
            return true;
        }

        plugin.send(player, "rtp.searching");

        int rtpCountdown = Math.max(0, plugin.getConfig().getInt("rtp.countdown", 0));
        AsyncRtpEngine engine = plugin.getAsyncRtpEngine();

        engine.search(player, bypassCooldown, new AsyncRtpEngine.ResultHandler() {
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

        return true;
    }
}
