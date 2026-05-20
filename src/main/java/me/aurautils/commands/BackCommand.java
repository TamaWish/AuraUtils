package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.aurautils.managers.TeleportHelper;

public class BackCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public BackCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /back.");
            return true;
        }
        if (!player.hasPermission("aura.back")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        var backLocation = plugin.getBackManager().get(player.getUniqueId());
        if (backLocation == null) {
            player.sendMessage(plugin.prefix("&cYou have no previous teleport location."));
            return true;
        }

        int tpCountdown = Math.max(0, plugin.getConfig().getInt("teleport.countdown", 5));
        TeleportHelper helper = new TeleportHelper(plugin);
        if (tpCountdown > 0) {
            helper.scheduleTeleport(player, backLocation, tpCountdown);
        } else {
            player.teleport(backLocation);
            player.sendMessage(plugin.prefix("&aReturned to your last teleport location."));
        }
        return true;
    }
}