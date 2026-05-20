package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public MenuCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot open the menu.");
            return true;
        }
        if (!player.hasPermission("aura.use")) {
            player.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        plugin.getMenuManager().openMainMenu(player);
        return true;
    }
}