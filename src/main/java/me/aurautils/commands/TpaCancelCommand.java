package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Cancels:
 * <ul>
 *   <li>a pending countdown teleport (home / warp / back / tpa / rtp), and/or</li>
 *   <li>any outgoing TPA request(s) this player sent that are still waiting.</li>
 * </ul>
 * Command: /tpacancel (aliases: tpcancel, auracancel).
 */
public class TpaCancelCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaCancelCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot cancel teleports.");
            return true;
        }

        boolean cancelledSomething = false;

        // 1) Countdown teleport (home/warp/back/tpa/rtp)
        if (plugin.getTeleportHelper().hasPending(player)) {
            plugin.getTeleportHelper().cancelTeleport(player, true);
            cancelledSomething = true;
        }

        // 2) Outgoing TPA request(s) waiting for accept/deny
        int outgoing = plugin.getTpaManager().cancelOutgoing(player);
        if (outgoing > 0) {
            if (outgoing == 1) {
                player.sendMessage(plugin.prefix("&cYour TPA request was cancelled."));
            } else {
                player.sendMessage(plugin.prefix("&cCancelled &e" + outgoing + " &cpending TPA requests."));
            }
            cancelledSomething = true;
        }

        if (!cancelledSomething) {
            player.sendMessage(plugin.prefix("&cYou have no pending teleport or TPA request."));
        }
        return true;
    }
}
