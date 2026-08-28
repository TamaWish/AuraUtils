package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
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
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/tpacancel");
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
                msg.send(player, "tpa.cancel-self");
            } else {
                msg.send(player, "tpa.cancel-many", "count", String.valueOf(outgoing));
            }
            cancelledSomething = true;
        }

        if (!cancelledSomething) {
            msg.send(player, "tpa.cancel-none");
        }
        return true;
    }
}
