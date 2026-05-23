package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaDenyCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaDenyCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!player.hasPermission("aura.tpdeny")) {
            plugin.send(player, "general.no-permission-short");
            return true;
        }
        if (!plugin.getTpaManager().hasPending(player.getUniqueId())) {
            plugin.send(player, "tpa.no-pending");
            return true;
        }
        plugin.getTpaManager().deny(player);
        return true;
    }
}
