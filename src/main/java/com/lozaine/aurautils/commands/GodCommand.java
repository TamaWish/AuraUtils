package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {

    private final AuraUtils plugin;
    public GodCommand(AuraUtils plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        var msg = plugin.messages();
        if (!sender.hasPermission("aura.god")) {
            msg.send(sender, "common.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.god.others")) {
                msg.send(sender, "common.cannot-toggle-others", "feature", msg.get("toggles.feature-god"));
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

        boolean now = plugin.getPlayerDataManager().toggleGod(target.getUniqueId());
        if (now) {
            plugin.getPlayerDataManager().applyGodEffects(target);
        }
        String state = msg.state(now);

        msg.send(target, "toggles.god-self", "state", state);
        if (!target.equals(sender)) {
            msg.send(sender, "toggles.god-other", "state", state, "player", target.getName());
        }
        return true;
    }
}
