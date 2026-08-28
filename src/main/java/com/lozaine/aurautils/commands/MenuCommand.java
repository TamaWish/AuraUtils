package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
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
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/menu");
            return true;
        }
        // Prefer aura.menu; fall back to aura.use for older permission setups
        if (!player.hasPermission("aura.menu") && !player.hasPermission("aura.use")) {
            msg.send(player, "common.no-permission");
            return true;
        }

        plugin.getMenuManager().openMainMenu(player);
        return true;
    }
}
