package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class KeepInventoryCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public KeepInventoryCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aura.keepinventory")) {
            sender.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        var settings = plugin.getServerSettingsManager();
        if (args.length == 0) {
            boolean enabled = settings.isKeepInventoryEnabled();
            settings.setKeepInventoryEnabled(!enabled);
            sendStatus(sender, !enabled);
            return true;
        }

        String action = args[0].toLowerCase();
        if (action.equals("on") || action.equals("enable") || action.equals("true")) {
            settings.setKeepInventoryEnabled(true);
            sendStatus(sender, true);
            return true;
        }
        if (action.equals("off") || action.equals("disable") || action.equals("false")) {
            settings.setKeepInventoryEnabled(false);
            sendStatus(sender, false);
            return true;
        }
        if (action.equals("status")) {
            sendStatus(sender, settings.isKeepInventoryEnabled());
            return true;
        }

        sender.sendMessage(plugin.prefix("&eUsage: /keepinventory [on|off|status]"));
        return true;
    }

    private void sendStatus(CommandSender sender, boolean enabled) {
        String state = enabled ? "&aenabled" : "&cdisabled";
        sender.sendMessage(plugin.prefix("Keep inventory on death is now " + state + " &7for all worlds&r."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !sender.hasPermission("aura.keepinventory")) {
            return List.of();
        }
        return CommandUtil.filterPrefix(args[0], List.of("on", "off", "status"));
    }
}
