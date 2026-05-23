package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TpaManager;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaHereCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaHereCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use TPA Here.");
            return true;
        }
        if (!player.hasPermission("aura.tpahere")) {
            plugin.send(player, "general.no-permission-short");
            return true;
        }
        if (args.length < 1) {
            plugin.send(player, "tpa.usage-tpahere");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            plugin.send(player, "general.player-offline", MessagePlaceholders.of("name", args[0]));
            return true;
        }
        if (target.equals(player)) {
            plugin.send(player, "tpa.self-here");
            return true;
        }

        if (!plugin.getTpaManager().sendRequest(player, target, TpaManager.TpaType.TO_REQUESTER)) {
            plugin.send(player, "tpa.pending-blocked");
            return true;
        }

        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        plugin.send(player, "tpa.sent-here", MessagePlaceholders.builder()
                .add("target", target.getName())
                .add("timeout", String.valueOf(timeout))
                .build());
        plugin.send(target, "tpa.received-here", MessagePlaceholders.of("requester", player.getName()));
        return true;
    }
}
