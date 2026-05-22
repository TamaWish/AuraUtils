package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetHomeCommand implements CommandExecutor, TabCompleter {

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
            plugin.send(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.send(player, "home.usage-set");
            return true;
        }

        if (!plugin.getHomeManager().canSetHome(player.getUniqueId(), args[0])) {
            int max = plugin.getHomeManager().getMaxHomesPerPlayer();
            plugin.send(player, "home.limit", MessagePlaceholders.of("max", String.valueOf(max)));
            return true;
        }

        plugin.getHomeManager().setHome(player.getUniqueId(), args[0], player.getLocation());
        plugin.getHomeManager().save();
        plugin.send(player, "home.set", MessagePlaceholders.of("name", args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player) || !sender.hasPermission("aura.home.set")) {
            return Collections.emptyList();
        }
        return CommandUtil.filterPrefix(args[0], plugin.getHomeManager().getHomeNames(player.getUniqueId()));
    }
}
