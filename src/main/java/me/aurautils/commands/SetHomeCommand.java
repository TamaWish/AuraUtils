package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public SetHomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot set homes.");
            return true;
        }
        if (!player.hasPermission("aura.home.set")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.prefix("&eUsage: /sethome <name>"));
            return true;
        }

        var loc = player.getLocation();
        plugin.getTeleportStoreManager().setHome(player.getUniqueId(), args[0], loc, player);
        plugin.getTeleportStoreManager().save();
        player.sendMessage(plugin.prefix("&aSet home &e" + args[0]
                + " &aat &f" + formatPos(loc) + "&a."));
        return true;
    }

    private static String formatPos(org.bukkit.Location loc) {
        return String.format(java.util.Locale.US, "%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
    }
}
