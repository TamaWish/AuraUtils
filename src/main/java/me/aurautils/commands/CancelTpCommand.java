package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelTpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public CancelTpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (plugin.getTeleportService().cancelPendingTeleport(player)) {
            plugin.send(player, "teleport.cancelled-manual");
        }
        return true;
    }
}
