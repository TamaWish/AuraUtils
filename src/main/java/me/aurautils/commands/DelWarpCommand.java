package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelWarpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public DelWarpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot delete warps.");
            return true;
        }
        if (!plugin.requireFeature(player, "warps")) {
            return true;
        }
        if (!player.hasPermission("aura.warp.delete")) {
            plugin.send(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.send(player, "warp.usage-del");
            return true;
        }

        if (!plugin.getWarpManager().deleteWarp(args[0])) {
            plugin.send(player, "warp.not-found", MessagePlaceholders.of("name", args[0]));
            return true;
        }

        plugin.getWarpManager().save();
        plugin.send(player, "warp.deleted", MessagePlaceholders.of("name", args[0]));
        return true;
    }
}
