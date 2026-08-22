package me.aurautils.commands;

import me.aurautils.AuraUtils;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /warp.");
            return true;
        }
        if (!player.hasPermission("aura.warp")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openWarpsMenu(player, 0);
            return true;
        }

        var dest = plugin.getTeleportStoreManager().getWarpDestination(args[0]);
        if (dest == null || dest.getLocation() == null) {
            player.sendMessage(plugin.prefix("&cWarp &e" + args[0] + " &cwas not found."));
            return true;
        }
        String displayName = dest.getDisplayName();
        var location = dest.getLocation();

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        var helper = plugin.getTeleportHelper();
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown, "warp " + displayName);
        } else if (helper.teleportExact(player, location)) {
            player.sendMessage(plugin.prefix("&aTeleported to &bwarp " + displayName + "&a."));
        }
        return true;
    }
}