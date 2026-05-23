package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.RtpService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RtpCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public RtpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "console.rtp-only-players");
            return true;
        }
        RtpService.startRtp(plugin, player);
        return true;
    }
}
