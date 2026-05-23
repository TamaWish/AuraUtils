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

public class HealCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;

    public HealCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.requireFeature(sender, "heal")) {
            return true;
        }
        if (!sender.hasPermission("aura.heal")) {
            plugin.send(sender, "general.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("aura.heal.others")) {
                plugin.send(sender, "heal.others-denied");
                return true;
            }
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                plugin.send(sender, "general.player-not-found");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                plugin.send(sender, "console.heal-only-players");
                return true;
            }
            target = player;
        }

        restoreHealth(target);

        if (target.equals(sender)) {
            plugin.send(target, "heal.healed-self");
        } else {
            plugin.send(target, "heal.healed-by", TagResolver.builder()
                    .resolver(Placeholder.parsed("healer", sender.getName()))
                    .build());
            plugin.send(sender, "heal.healed-other", TagResolver.builder()
                    .resolver(Placeholder.parsed("player", target.getName()))
                    .build());
        }
        return true;
    }

    private void restoreHealth(Player target) {
        target.setFireTicks(0);
        target.setFreezeTicks(0);
        target.setAbsorptionAmount(0.0D);
        target.setHealth(Math.max(1.0D, target.getMaxHealth()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return CommandUtil.onlinePlayerNames(sender, args[0], "aura.heal.others");
    }
}