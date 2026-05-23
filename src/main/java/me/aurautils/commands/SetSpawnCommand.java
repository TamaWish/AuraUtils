package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public SetSpawnCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot set world spawn. Use an in-game operator.");
            return true;
        }
        if (!plugin.requireFeature(player, "spawn")) {
            return true;
        }
        if (!player.hasPermission("aura.setspawn")) {
            plugin.send(player, "general.no-permission");
            return true;
        }

        var world = player.getWorld();
        plugin.getServerSettingsManager().setWorldSpawn(player.getLocation());
        String coords = formatBlock(player.getLocation().getBlockX())
                + ", " + formatBlock(player.getLocation().getBlockY())
                + ", " + formatBlock(player.getLocation().getBlockZ());
        plugin.send(player, "spawn.set", MessagePlaceholders.builder()
                .add("world", world.getName())
                .add("coords", coords)
                .build());
        return true;
    }

    private static int formatBlock(double coord) {
        return (int) Math.floor(coord);
    }
}
