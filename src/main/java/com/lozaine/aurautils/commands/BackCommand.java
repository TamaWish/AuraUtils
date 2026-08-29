package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public BackCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/back");
            return true;
        }
        if (!player.hasPermission("aura.back")) {
            msg.send(player, "common.no-permission");
            return true;
        }

        var backLocation = plugin.getBackManager().get(player.getUniqueId());
        if (backLocation == null) {
            msg.send(player, "back.none");
            return true;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        var helper = plugin.getTeleportHelper();
        String destination = msg.get("back.label");
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, backLocation, tpCountdown, destination);
        } else {
            helper.teleportExact(player, backLocation, "back.success", destination);
        }
        return true;
    }
}
