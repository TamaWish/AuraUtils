package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public FlyCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aura.fly")) {
            sender.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.fly.others")) {
                sender.sendMessage(plugin.prefix("&cYou can't toggle fly on others."));
                return true;
            }
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.prefix("&cPlayer not found."));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }
            target = player;
        }

        boolean now = plugin.getPlayerDataManager().toggleFly(target.getUniqueId());
        target.setAllowFlight(now);
        if (!now) {
            target.setFlying(false);
        }

        String state = now ? "&aENABLED" : "&cDISABLED";
        target.sendMessage(plugin.prefix("Fly " + state + "&r."));
        if (!target.equals(sender)) {
            sender.sendMessage(plugin.prefix("Fly " + state + " &rfor &e" + target.getName() + "&r."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return CommandUtil.onlinePlayerNames(sender, args[0], "aura.fly.others");
    }
}
