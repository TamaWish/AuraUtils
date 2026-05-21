package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public SetWarpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot set warps.");
            return true;
        }
        if (!player.hasPermission("aura.warp.set")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.prefix("&eUsage: /setwarp <name>"));
            return true;
        }

        plugin.getWarpManager().setWarp(args[0], player.getLocation());
        plugin.getWarpManager().save();
        player.sendMessage(plugin.prefix("&aSet warp &e" + args[0] + "&a."));
        return true;
    }
}
