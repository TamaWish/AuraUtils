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

        plugin.getTeleportStoreManager().setHome(player.getUniqueId(), args[0], player.getLocation());
        plugin.getTeleportStoreManager().save();
        player.sendMessage(plugin.prefix("&aSet home &e" + args[0] + "&a."));
        return true;
    }
}