package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.BackService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        BackService.teleportBack(plugin, player);
        return true;
    }
}
