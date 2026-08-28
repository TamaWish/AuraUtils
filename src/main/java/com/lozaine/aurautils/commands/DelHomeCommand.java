package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public DelHomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/delhome");
            return true;
        }
        if (!player.hasPermission("aura.home.delete")) {
            msg.send(player, "common.no-permission");
            return true;
        }
        if (args.length < 1) {
            msg.send(player, "home.usage-delete");
            return true;
        }

        boolean removed = plugin.getTeleportStoreManager().deleteHome(player.getUniqueId(), args[0]);
        if (!removed) {
            msg.send(player, "home.not-found", "name", args[0]);
            return true;
        }

        plugin.getTeleportStoreManager().save();
        msg.send(player, "home.deleted", "name", args[0]);
        return true;
    }
}
