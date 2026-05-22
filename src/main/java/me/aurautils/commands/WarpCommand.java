package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TeleportHelper;
import me.aurautils.util.CommandUtil;
import me.aurautils.util.MessagePlaceholders;
import me.aurautils.util.WarpPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarpCommand implements CommandExecutor, TabCompleter {

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
        if (!WarpPermissions.canUseAny(player, plugin.getWarpManager().getWarpNames())) {
            plugin.send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openWarpsMenu(player, 0);
            return true;
        }

        if (!WarpPermissions.canUse(player, args[0])) {
            plugin.send(player, "warp.no-permission", MessagePlaceholders.of("name", args[0]));
            return true;
        }

        var location = plugin.getWarpManager().getWarp(args[0]);
        if (location == null) {
            plugin.send(player, "warp.not-found", MessagePlaceholders.of("name", args[0]));
            return true;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = plugin.getTeleportHelper();
        MessagePlaceholders placeholders = MessagePlaceholders.of("name", args[0]);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, location, tpCountdown, false, "teleport.success-warp", placeholders);
        } else {
            plugin.getBackManager().skipNextRecord(player.getUniqueId());
            player.teleport(location);
            plugin.send(player, "teleport.success-warp", placeholders);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        List<String> allowed = new ArrayList<>();
        for (String name : plugin.getWarpManager().getWarpNames()) {
            if (WarpPermissions.canUse(player, name)) {
                allowed.add(name);
            }
        }
        return CommandUtil.filterPrefix(args[0], allowed);
    }
}
