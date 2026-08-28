package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.managers.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaCommand(AuraUtils plugin) { this.plugin = plugin; }

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
        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openTpaMenu(p);
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            msg.send(p, "common.player-not-online", "name", args[0]);
            return true;
        }
        if (target.equals(p)) {
            msg.send(p, "tpa.self");
            return true;
        }

        TpaManager.SendResult result = plugin.getTpaManager().sendRequest(p, target);
        switch (result) {
            case TRUSTED_INSTANT -> {
                // Messages already sent inside TpaManager
            }
            case PENDING -> {
                int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
                msg.send(p, "tpa.sent", "player", target.getName(), "seconds", String.valueOf(timeout));
                msg.send(target, "tpa.incoming", "player", p.getName());
            }
            case BUSY -> msg.send(p, "tpa.busy");
            case FAILED -> msg.send(p, "tpa.failed");
        }
        return true;
    }
}
