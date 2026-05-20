package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.aurautils.managers.TeleportHelper;

public class HomeCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public HomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /home.");
            return true;
        }
        if (!player.hasPermission("aura.home")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openHomesMenu(player, 0);
            return true;
        }

        var location = plugin.getTeleportStoreManager().getHome(player.getUniqueId(), args[0]);
        if (location == null) {
            player.sendMessage(plugin.prefix("&cHome &e" + args[0] + " &cwas not found."));
            return true;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown);
        } else {
            player.teleport(location);
            player.sendMessage(plugin.prefix("&aTeleported to home &e" + args[0] + "&a."));
        }
        return true;
    }
}