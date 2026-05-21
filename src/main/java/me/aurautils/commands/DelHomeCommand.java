package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

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

        boolean removed = plugin.getHomeManager().deleteHome(player.getUniqueId(), args[0]);
        if (!removed) {
            player.sendMessage(plugin.prefix("&cHome &e" + args[0] + " &cwas not found."));
            return true;
        }

        plugin.getHomeManager().save();
        player.sendMessage(plugin.prefix("&aDeleted home &e" + args[0] + "&a."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player) || !sender.hasPermission("aura.home.delete")) {
            return Collections.emptyList();
        }
        return CommandUtil.filterPrefix(args[0], plugin.getHomeManager().getHomeNames(player.getUniqueId()));
    }
}
