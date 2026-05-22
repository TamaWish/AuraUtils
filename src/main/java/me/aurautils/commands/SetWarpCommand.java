package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
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
            plugin.send(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.send(player, "warp.usage-set");
            return true;
        }

        plugin.getWarpManager().setWarp(args[0], player.getLocation());
        plugin.getWarpManager().save();
        plugin.send(player, "warp.set", MessagePlaceholders.of("name", args[0]));
        return true;
    }
}
