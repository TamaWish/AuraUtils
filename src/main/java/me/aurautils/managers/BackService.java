package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;

public final class BackService {

    private BackService() {
    }

    public static boolean teleportBack(AuraUtils plugin, Player player) {
        if (!player.hasPermission("aura.back")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return false;
        }

        var backLocation = plugin.getBackManager().get(player.getUniqueId());
        if (backLocation == null) {
            player.sendMessage(plugin.prefix("&cYou have no previous teleport location."));
            return false;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, backLocation, tpCountdown);
        } else {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
            player.teleport(backLocation);
            player.sendMessage(plugin.prefix("&aReturned to your last teleport location."));
        }
        return true;
    }
}
