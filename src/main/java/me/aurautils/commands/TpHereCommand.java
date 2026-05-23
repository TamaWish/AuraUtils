package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.AdminTeleportService;
import me.aurautils.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TpHereCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public TpHereCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /tphere.");
            return true;
        }
        if (!plugin.requireFeature(player, "tpa")) {
            return true;
        }
        if (!player.hasPermission("aura.tphere.others")) {
            plugin.send(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.send(player, "tphere.usage");
            return true;
        }

        Player target = CommandUtil.resolveVisiblePlayer(plugin, player, args[0]);
        if (target == null) {
            plugin.send(player, "general.player-not-found");
            return true;
        }
        if (target.equals(player)) {
            plugin.send(player, "tphere.self");
            return true;
        }

        AdminTeleportService.teleportTo(target, player, plugin);
        plugin.send(player, "tphere.teleported-sender", me.aurautils.util.MessagePlaceholders.of("player", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return CommandUtil.onlinePlayerNames(plugin, sender, args[0], "aura.tphere.others");
    }
}
