package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.util.InventoryLimits;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /inv [number|list]} — open a personal extra inventory.
 */
public class InvCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public InvCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/" + label);
            return true;
        }
        if (plugin.getPlayerInventoryManager() == null || !plugin.getPlayerInventoryManager().isEnabled()) {
            msg.send(player, "inventories.disabled");
            return true;
        }
        if (!player.hasPermission(InventoryLimits.USE_PERMISSION)) {
            msg.send(player, "common.no-permission");
            return true;
        }

        int limit = plugin.getPlayerInventoryManager().resolveLimit(player);
        if (limit <= 0) {
            msg.send(player, "inventories.none");
            return true;
        }

        if (args.length == 0) {
            if (limit == 1) {
                plugin.getPlayerInventoryManager().open(player, 1);
            } else {
                plugin.getMenuManager().openInventoriesMenu(player, 0);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
            plugin.getMenuManager().openInventoriesMenu(player, 0);
            return true;
        }

        int number;
        try {
            number = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            msg.send(player, "inventories.usage", "limit", String.valueOf(limit));
            return true;
        }
        if (!plugin.getPlayerInventoryManager().canOpen(player, number)) {
            msg.send(player, "inventories.denied",
                    "number", String.valueOf(number),
                    "limit", String.valueOf(limit));
            return true;
        }
        plugin.getPlayerInventoryManager().open(player, number);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return Collections.emptyList();
        }
        if (plugin.getPlayerInventoryManager() == null || !plugin.getPlayerInventoryManager().isEnabled()) {
            return Collections.emptyList();
        }
        if (!player.hasPermission(InventoryLimits.USE_PERMISSION)) {
            return Collections.emptyList();
        }
        int limit = plugin.getPlayerInventoryManager().resolveLimit(player);
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        if ("list".startsWith(prefix)) {
            suggestions.add("list");
        }
        for (int number = 1; number <= limit; number++) {
            String token = String.valueOf(number);
            if (token.startsWith(prefix)) {
                suggestions.add(token);
            }
        }
        return suggestions;
    }
}
