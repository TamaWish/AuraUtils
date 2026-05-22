package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;

public final class BackService {

    private BackService() {
    }

    public static boolean teleportBack(AuraUtils plugin, Player player) {
        if (!player.hasPermission("aura.back")) {
            plugin.send(player, "general.no-permission");
            return false;
        }

        var backLocation = plugin.getBackManager().get(player.getUniqueId());
        if (backLocation == null) {
            plugin.send(player, "teleport.no-back");
            return false;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = plugin.getTeleportHelper();
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, backLocation, tpCountdown, false, "teleport.success-back");
        } else {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
            player.teleport(backLocation);
            plugin.send(player, "teleport.success-back");
        }
        return true;
    }
}
