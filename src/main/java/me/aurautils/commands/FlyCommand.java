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

public class FlyCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public FlyCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aura.fly")) {
            plugin.send(sender, "general.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.fly.others")) {
                plugin.send(sender, "fly.others-denied");
                return true;
            }
            target = plugin.getServer().getPlayer(args[0]);
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

        boolean enabled = plugin.getPlayerDataManager().toggleFly(target.getUniqueId());
        target.setAllowFlight(enabled);
        if (!enabled) {
            target.setFlying(false);
        }

        plugin.sendToggle(target, "fly.toggled-self", enabled);
        if (!target.equals(sender)) {
            plugin.send(sender, "fly.toggled-other", TagResolver.builder()
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
        return CommandUtil.onlinePlayerNames(sender, args[0], "aura.fly.others");
    }
}
