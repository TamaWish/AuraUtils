package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TeleportHelper;
import me.aurautils.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class HomeCommand implements CommandExecutor, TabCompleter {

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

        if (args.length == 0) {
            List<String> homes = plugin.getHomeManager().getHomeNames(player.getUniqueId());
            if (homes.size() == 1) {
                teleportToHome(player, homes.get(0));
                return true;
            }
            plugin.getMenuManager().openHomesMenu(player, 0);
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openHomesMenu(player, 0);
            return true;
        }

        teleportToHome(player, args[0]);
        return true;
    }

    private void teleportToHome(Player player, String homeName) {
        var location = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
        if (location == null) {
            player.sendMessage(plugin.prefix("&cHome &e" + homeName + " &cwas not found."));
            return;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown);
        } else {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
            player.teleport(location);
            player.sendMessage(plugin.prefix("&aTeleported to home &e" + homeName + "&a."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player) || !sender.hasPermission("aura.home")) {
            return Collections.emptyList();
        }
        return CommandUtil.filterPrefix(args[0], plugin.getHomeManager().getHomeNames(player.getUniqueId()));
    }
}
