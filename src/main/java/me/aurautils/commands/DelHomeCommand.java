package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public DelHomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot delete homes.");
            return true;
        }
        if (!player.hasPermission("aura.home.delete")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.prefix("&eUsage: /delhome <name>"));
            return true;
        }

        boolean removed = plugin.getTeleportStoreManager().deleteHome(player.getUniqueId(), args[0]);
        if (!removed) {
            player.sendMessage(plugin.prefix("&cHome &e" + args[0] + " &cwas not found."));
            return true;
        }

        plugin.getTeleportStoreManager().save();
        player.sendMessage(plugin.prefix("&aDeleted home &e" + args[0] + "&a."));
        return true;
    }
}