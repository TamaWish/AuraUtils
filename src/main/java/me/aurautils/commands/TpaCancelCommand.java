package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Cancels a pending countdown teleport (warp/home/back/tpa/rtp).
 * Command name is /tpacancel (aliases: tpcancel, auracancel).
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

        if (!plugin.getTeleportHelper().hasPending(player)) {
            player.sendMessage(plugin.prefix("&cYou have no pending teleport."));
            return true;
        }

        plugin.getTeleportHelper().cancelTeleport(player, true);
        return true;
    }
}
