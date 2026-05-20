package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DelWarpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public DelWarpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aura.warp.delete")) {
            sender.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.prefix("&eUsage: /delwarp <name>"));
            return true;
        }

        boolean removed = plugin.getTeleportStoreManager().deleteWarp(args[0]);
        if (!removed) {
            sender.sendMessage(plugin.prefix("&cWarp &e" + args[0] + " &cwas not found."));
            return true;
        }

        plugin.getTeleportStoreManager().save();
        sender.sendMessage(plugin.prefix("&aDeleted warp &e" + args[0] + "&a."));
        return true;
    }
}