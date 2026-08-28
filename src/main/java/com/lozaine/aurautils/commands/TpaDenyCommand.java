package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaDenyCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaDenyCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player p)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "TPA");
            return true;
        }
        if (!p.hasPermission("aura.tpa")) {
            msg.send(p, "common.no-permission");
            return true;
        }
        if (!plugin.getTpaManager().hasPending(p.getUniqueId())) {
            msg.send(p, "tpa.no-pending");
            return true;
        }
        plugin.getTpaManager().deny(p);
        return true;
    }
}
