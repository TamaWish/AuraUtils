package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
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
        var msg = plugin.messages();
        if (!sender.hasPermission("aura.warp.delete")) {
            msg.send(sender, "common.no-permission");
            return true;
        }
        if (args.length < 1) {
            msg.send(sender, "warp.usage-delete");
            return true;
        }

        boolean removed = plugin.getTeleportStoreManager().deleteWarp(args[0]);
        if (!removed) {
            msg.send(sender, "warp.not-found", "name", args[0]);
            return true;
        }

        plugin.getTeleportStoreManager().save();
        msg.send(sender, "warp.deleted", "name", args[0]);
        return true;
    }
}
