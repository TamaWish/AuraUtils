package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AdminHomeCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = List.of("list", "del");

    private final AuraUtils plugin;

    public AdminHomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aura.home.admin")) {
            plugin.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.send(sender, "adminhome.usage");
            return true;
        }

        UUID playerId = plugin.getHomeManager().resolvePlayerId(args[0]);
        if (playerId == null) {
            plugin.send(sender, "adminhome.player-not-found", MessagePlaceholders.of("target", args[0]));
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "list" -> listHomes(sender, playerId);
            case "del" -> deleteHome(sender, playerId, args);
            default -> {
                plugin.send(sender, "adminhome.usage");
                yield true;
            }
        };
    }

    private boolean listHomes(CommandSender sender, UUID playerId) {
        List<String> homeNames = plugin.getHomeManager().getHomeNames(playerId);
        String playerLabel = plugin.getHomeManager().getPlayerDisplayName(playerId);

        if (homeNames.isEmpty()) {
            plugin.send(sender, "adminhome.list-empty", MessagePlaceholders.of("player", playerLabel));
            return true;
        }

        plugin.send(sender, "adminhome.list-header", MessagePlaceholders.builder()
                .add("player", playerLabel)
                .add("count", String.valueOf(homeNames.size()))
                .build());

        for (String homeName : homeNames) {
            Location location = plugin.getHomeManager().getHome(playerId, homeName);
            if (location == null) {
                continue;
            }
            plugin.send(sender, "adminhome.list-entry", MessagePlaceholders.builder()
                    .add("name", homeName)
                    .add("world", location.getWorld() != null ? location.getWorld().getName() : "?")
                    .add("coords", formatCoords(location))
                    .build());
        }
        return true;
    }

    private boolean deleteHome(CommandSender sender, UUID playerId, String[] args) {
        if (args.length < 3) {
            plugin.send(sender, "adminhome.usage");
            return true;
        }

        String homeName = args[2];
        if (!plugin.getHomeManager().deleteHome(playerId, homeName)) {
            plugin.send(sender, "home.not-found", MessagePlaceholders.of("name", homeName));
            return true;
        }

        plugin.getHomeManager().save();
        plugin.send(sender, "adminhome.deleted", MessagePlaceholders.builder()
                .add("name", homeName)
                .add("player", plugin.getHomeManager().getPlayerDisplayName(playerId))
                .build());
        return true;
    }

    private static String formatCoords(Location location) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f",
                location.getX(), location.getY(), location.getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aura.home.admin")) {
            return Collections.emptyList();
        }
        return switch (args.length) {
            case 1 -> CommandUtil.filterPrefix(args[0], plugin.getHomeManager().knownPlayerTokens());
            case 2 -> CommandUtil.filterPrefix(args[1], ACTIONS);
            case 3 -> {
                if (!"del".equalsIgnoreCase(args[1])) {
                    yield Collections.emptyList();
                }
                UUID playerId = plugin.getHomeManager().resolvePlayerId(args[0]);
                if (playerId == null) {
                    yield Collections.emptyList();
                }
                yield CommandUtil.filterPrefix(args[2], plugin.getHomeManager().getHomeNames(playerId));
            }
            default -> Collections.emptyList();
        };
    }
}
