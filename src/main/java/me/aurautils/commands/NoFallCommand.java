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

public class NoFallCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public NoFallCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!plugin.requireFeature(sender, "nofall")) {
            return true;
        }
        if (!sender.hasPermission("aura.nofall")) {
            plugin.send(sender, "general.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.nofall.others")) {
                plugin.send(sender, "nofall.others-denied");
                return true;
            }
            target = CommandUtil.resolveVisiblePlayer(plugin, sender, args[0]);
            if (target == null) {
                plugin.send(sender, "general.player-not-found");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }
            target = player;
        }

        boolean enabled = plugin.getPlayerDataManager().toggleNoFall(target.getUniqueId());
        plugin.sendToggle(target, "nofall.toggled-self", enabled);
        if (!target.equals(sender)) {
            plugin.send(sender, "nofall.toggled-other", TagResolver.builder()
                    .resolver(Placeholder.parsed("player", target.getName()))
                    .resolver(Placeholder.component("state", plugin.getMessages().stateComponent(sender, enabled)))
                    .build());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return CommandUtil.onlinePlayerNames(plugin, sender, args[0], "aura.nofall.others");
    }
}
