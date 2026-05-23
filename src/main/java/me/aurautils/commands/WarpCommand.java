package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.WarpService;
import me.aurautils.util.CommandUtil;
import me.aurautils.util.WarpPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public WarpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot use /warp.");
            return true;
        }

        if (!plugin.requireFeature(player, "warps")) {
            return true;
        }

        if (!WarpPermissions.canUseAny(player, plugin.getWarpManager().getWarpNames())) {
            plugin.send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            String categoryFilter = null;
            if (args.length >= 2 && !args[1].equalsIgnoreCase("list")) {
                categoryFilter = resolveCategoryFilter(args[1]);
            }
            boolean skipCategoryPicker = args.length >= 2;
            plugin.getMenuManager().openWarpsMenu(player, 0, categoryFilter, skipCategoryPicker);
            return true;
        }

        return WarpService.teleport(plugin, player, args[0]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("list");
            for (String resolvable : plugin.getWarpManager().getAllResolvableNames()) {
                String canonical = plugin.getWarpManager().resolveWarpName(resolvable);
                if (canonical != null && WarpPermissions.canUse(player, canonical)) {
                    suggestions.add(resolvable);
                }
            }
            return CommandUtil.filterPrefix(args[0], suggestions);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            List<String> categories = new ArrayList<>();
            categories.add("all");
            categories.addAll(plugin.getWarpManager().getCategories());
            if (plugin.getWarpManager().hasUncategorizedWarps()) {
                categories.add("other");
            }
            return CommandUtil.filterPrefix(args[1], categories);
        }

        return Collections.emptyList();
    }

    private static String resolveCategoryFilter(String input) {
        if (input.equalsIgnoreCase("all")) {
            return null;
        }
        if (input.equalsIgnoreCase("other")) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT).trim();
    }
}
