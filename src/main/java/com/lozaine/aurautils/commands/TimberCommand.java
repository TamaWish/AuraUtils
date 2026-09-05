package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimberCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TimberCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        var msg = plugin.messages();
        if (!sender.hasPermission("aura.timber")) {
            msg.send(sender, "common.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.timber.others")) {
                msg.send(sender, "common.cannot-toggle-others", "feature", msg.get("toggles.feature-timber"));
                return true;
            }
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                msg.send(sender, "common.player-not-found");
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                msg.sendPlain(sender, "common.console-specify-player");
                return true;
            }
            target = p;
        }

        boolean now = plugin.getPlayerDataManager().toggleTimber(target.getUniqueId());
        String state = msg.state(now);
        msg.send(target, "toggles.timber-self", "state", state);
        if (!target.equals(sender)) {
            msg.send(sender, "toggles.timber-other", "state", state, "player", target.getName());
        }
        if (now && !plugin.getConfig().getBoolean("timber.enabled", true)) {
            msg.send(sender, "toggles.timber-server-off");
        }
        return true;
    }
}
