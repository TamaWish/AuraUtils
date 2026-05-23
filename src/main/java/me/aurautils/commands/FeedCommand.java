package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.CommandUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FeedCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public FeedCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.requireFeature(sender, "feed")) {
            return true;
        }
        if (!sender.hasPermission("aura.feed")) {
            plugin.send(sender, "general.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.feed.others")) {
                plugin.send(sender, "feed.others-denied");
                return true;
            }
            target = CommandUtil.resolveVisiblePlayer(plugin, sender, args[0]);
            if (target == null) {
                plugin.send(sender, "general.player-not-found");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                plugin.send(sender, "console.feed-only-players");
                return true;
            }
            target = player;
        }

        restoreFood(target);

        if (target.equals(sender)) {
            plugin.send(target, "feed.fed-self");
        } else {
            plugin.send(target, "feed.fed-by", TagResolver.builder()
                    .resolver(Placeholder.parsed("healer", sender.getName()))
                    .build());
            plugin.send(sender, "feed.fed-other", TagResolver.builder()
                    .resolver(Placeholder.parsed("player", target.getName()))
                    .build());
        }
        return true;
    }

    private void restoreFood(Player target) {
        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        target.setExhaustion(0.0f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return CommandUtil.onlinePlayerNames(plugin, sender, args[0], "aura.feed.others");
    }
}