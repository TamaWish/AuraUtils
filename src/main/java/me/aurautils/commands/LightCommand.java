package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LightCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public LightCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use dynamic light.");
            return true;
        }
        if (!player.hasPermission("aura.light")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        boolean nowEnabled = plugin.getLightManager().toggle(player);
        player.sendMessage(plugin.prefix(nowEnabled ? "&aDynamic light enabled." : "&cDynamic light disabled."));
        return true;
    }
}