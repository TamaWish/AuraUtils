package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Shared home teleport flow for commands and GUI. */
public final class HomeService {

    private HomeService() {
    }

    public static boolean teleport(AuraUtils plugin, Player player, String homeName) {
        Location location = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
        if (location == null) {
            plugin.send(player, "home.not-found", MessagePlaceholders.of("name", homeName));
            return false;
        }

        plugin.getTeleportService().teleport(player, location, plugin.getTeleportService().standardOptions()
                .successMessageKey("teleport.success-home")
                .successPlaceholders(MessagePlaceholders.of("name", homeName))
                .build());
        return true;
    }
}
