package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public HomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/home");
            return true;
        }
        if (!player.hasPermission("aura.home")) {
            msg.send(player, "common.no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openHomesMenu(player, 0);
            return true;
        }

        var dest = plugin.getTeleportStoreManager().getHomeDestination(player.getUniqueId(), args[0]);
        if (dest == null || dest.getLocation() == null) {
            msg.send(player, "home.not-found", "name", args[0]);
            return true;
        }
        String displayName = dest.getDisplayName();
        var location = dest.getLocation();

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        var helper = plugin.getTeleportHelper();
        String destination = msg.get("home.destination-label", "name", displayName);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown, destination);
        } else {
            helper.teleportExact(player, location, "home.teleported", destination, "name", displayName);
        }
        return true;
    }
}
