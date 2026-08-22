package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {

    private final AuraUtils plugin;
    public GodCommand(AuraUtils plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aura.god")) {
            sender.sendMessage(plugin.prefix("&cNo permission.")); return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.god.others")) {
                sender.sendMessage(plugin.prefix("&cYou can't toggle god mode on others.")); return true;
            }
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) { sender.sendMessage(plugin.prefix("&cPlayer not found.")); return true; }
        } else {
            if (!(sender instanceof Player p)) { sender.sendMessage("Console must specify a player."); return true; }
            target = p;
        }

        boolean now = plugin.getPlayerDataManager().toggleGod(target.getUniqueId());
        if (now) {
            plugin.getPlayerDataManager().applyGodEffects(target);
        }
        String state = now ? "&aENABLED" : "&cDISABLED";

        target.sendMessage(plugin.prefix("God mode " + state + "&r."));
        if (!target.equals(sender)) {
            sender.sendMessage(plugin.prefix("God mode " + state + " &rfor &e" + target.getName() + "&r."));
        }
        return true;
    }
}
