package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public WarpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/warp");
            return true;
        }
        if (!player.hasPermission("aura.warp")) {
            msg.send(player, "common.no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openWarpsMenu(player, 0);
            return true;
        }

        var dest = plugin.getTeleportStoreManager().getWarpDestination(args[0]);
        if (dest == null || dest.getLocation() == null) {
            msg.send(player, "warp.not-found", "name", args[0]);
            return true;
        }
        String displayName = dest.getDisplayName();
        var location = dest.getLocation();

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        var helper = plugin.getTeleportHelper();
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown,
                    msg.get("warp.destination-label", "name", displayName));
        } else {
            helper.teleportExact(player, location, success -> {
                if (success && player.isOnline()) {
                    msg.send(player, "warp.teleported", "name", displayName);
                }
            });
        }
        return true;
    }
}
