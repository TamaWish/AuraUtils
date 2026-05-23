package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.AdminTeleportService;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpAllCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpAllCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /tpall.");
            return true;
        }
        if (!plugin.requireFeature(player, "tpa")) {
            return true;
        }
        if (!player.hasPermission("aura.tpall")) {
            plugin.send(player, "general.no-permission");
            return true;
        }

        int count = 0;
        for (Player target : plugin.getServer().getOnlinePlayers()) {
            if (target.equals(player)) {
                continue;
            }
            AdminTeleportService.teleportTo(target, player, plugin);
            count++;
        }

        if (count == 0) {
            plugin.send(player, "tpall.none");
        } else {
            plugin.send(player, "tpall.summary", MessagePlaceholders.of("count", String.valueOf(count)));
        }
        return true;
    }
}
