package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.entity.Player;

/** Force teleports for admin commands (no TPA handshake). */
public final class AdminTeleportService {

    private AdminTeleportService() {
    }

    public static void teleportTo(Player traveler, Player destinationHolder, AuraUtils plugin) {
        MessagePlaceholders travelerPlaceholders = MessagePlaceholders.of("destination", destinationHolder.getName());
        MessagePlaceholders destinationPlaceholders = MessagePlaceholders.of("traveler", traveler.getName());

        TeleportService teleports = plugin.getTeleportService();
        int tpCountdown = teleports.countdownFor(TeleportService.TeleportKind.ADMIN);

        if (tpCountdown > 0) {
            teleports.teleport(traveler, destinationHolder.getLocation(), teleports.adminOptions()
                    .countdownSeconds(tpCountdown)
                    .successMessageKey("tphere.teleported-traveler")
                    .successPlaceholders(travelerPlaceholders)
                    .build());
            plugin.send(traveler, "tphere.countdown-traveler", travelerPlaceholders
                    .with("seconds", String.valueOf(tpCountdown)));
            if (!traveler.equals(destinationHolder)) {
                plugin.send(destinationHolder, "tphere.countdown-destination", destinationPlaceholders
                        .with("seconds", String.valueOf(tpCountdown)));
            }
            return;
        }

        teleports.teleport(traveler, destinationHolder.getLocation(), teleports.adminOptions()
                .successMessageKey("tphere.teleported-traveler")
                .successPlaceholders(travelerPlaceholders)
                .onSuccess(() -> {
                    if (!traveler.equals(destinationHolder)) {
                        plugin.send(destinationHolder, "tphere.teleported-destination", destinationPlaceholders);
                    }
                })
                .build());
    }
}
