package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
        if (!plugin.requireFeature(sender, "keepinventory")) {
            return true;
        }
        if (!sender.hasPermission("aura.keepinventory")) {
            plugin.send(sender, "general.no-permission");
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

        plugin.send(sender, "keepinventory.usage");
        return true;
    }

    private void sendStatus(CommandSender sender, boolean enabled) {
        plugin.send(sender, "keepinventory.toggled", TagResolver.builder()
                .resolver(Placeholder.component("state", plugin.getMessages().stateComponent(sender, enabled)))
                .build());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !sender.hasPermission("aura.keepinventory")) {
            return List.of();
        }
        return CommandUtil.filterPrefix(args[0], List.of("on", "off", "status"));
    }
}
