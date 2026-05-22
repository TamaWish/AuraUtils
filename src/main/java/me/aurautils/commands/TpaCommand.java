package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use TPA.");
            return true;
        }
        if (!player.hasPermission("aura.tpa")) {
            plugin.send(player, "general.no-permission-short");
            return true;
        }
        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openTpaMenu(player);
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            plugin.send(player, "general.player-offline", MessagePlaceholders.of("name", args[0]));
            return true;
        }
        if (target.equals(player)) {
            plugin.send(player, "tpa.self");
            return true;
        }

        if (!plugin.getTpaManager().sendRequest(player, target)) {
            plugin.send(player, "tpa.pending-blocked");
            return true;
        }

        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        plugin.send(player, "tpa.sent", MessagePlaceholders.builder()
                .add("target", target.getName())
                .add("timeout", String.valueOf(timeout))
                .build());
        plugin.send(target, "tpa.received", MessagePlaceholders.of("requester", player.getName()));
        return true;
    }
}
