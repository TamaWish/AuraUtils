package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class DelWarpCommand implements CommandExecutor, TabCompleter {

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

        boolean removed = plugin.getWarpManager().deleteWarp(args[0]);
        if (!removed) {
            sender.sendMessage(plugin.prefix("&cWarp &e" + args[0] + " &cwas not found."));
            return true;
        }

        plugin.getWarpManager().save();
        sender.sendMessage(plugin.prefix("&aDeleted warp &e" + args[0] + "&a."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !sender.hasPermission("aura.warp.delete")) {
            return Collections.emptyList();
        }
        return CommandUtil.filterPrefix(args[0], plugin.getWarpManager().getWarpNames());
    }
}
