package me.aurautils.util;

import me.aurautils.AuraUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class CommandUtil {

    private CommandUtil() {
    }

    public static List<String> filterPrefix(String input, Collection<String> options) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }

    /**
     * Resolves an online player visible to the viewer (vanished players hidden unless bypass permission).
     */
    public static Player resolveVisiblePlayer(AuraUtils plugin, CommandSender viewer, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || !target.isOnline()) {
            return null;
        }
        if (!plugin.getVanishSupport().canSee(viewer, target)) {
            return null;
        }
        return target;
    }

    public static List<String> onlinePlayerNames(AuraUtils plugin, CommandSender sender, String input) {
        return onlinePlayerNames(plugin, sender, input, null);
    }

    public static List<String> onlinePlayerNames(AuraUtils plugin, CommandSender sender, String input, String othersPermission) {
        if (othersPermission != null && !sender.hasPermission(othersPermission)) {
            return List.of();
        }
        VanishSupport vanish = plugin.getVanishSupport();
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (vanish.canSee(sender, player)) {
                names.add(player.getName());
            }
        }
        return filterPrefix(input, names);
    }
}
