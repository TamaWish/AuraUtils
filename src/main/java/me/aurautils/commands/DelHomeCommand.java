package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
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
        if (!plugin.requireFeature(player, "homes")) {
            return true;
        }
        if (!player.hasPermission("aura.home.delete")) {
            plugin.send(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.send(player, "home.usage-del");
            return true;
        }

        if (!plugin.getHomeManager().deleteHome(player.getUniqueId(), args[0])) {
            plugin.send(player, "home.not-found", MessagePlaceholders.of("name", args[0]));
            return true;
        }

        plugin.getHomeManager().save();
        plugin.send(player, "home.deleted", MessagePlaceholders.of("name", args[0]));
        return true;
    }
}
