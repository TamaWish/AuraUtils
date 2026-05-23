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

        plugin.getTeleportService().teleport(player, backLocation, plugin.getTeleportService().standardOptions()
                .skipBackRecord(true)
                .successMessageKey("teleport.success-back")
                .build());
        return true;
    }
}
