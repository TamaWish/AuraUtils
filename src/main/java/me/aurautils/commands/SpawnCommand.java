package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public SpawnCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "console.spawn-only-players");
            return true;
        }
        if (!plugin.requireFeature(player, "spawn")) {
            return true;
        }
        if (!player.hasPermission("aura.spawn")) {
            plugin.send(player, "general.no-permission");
            return true;
        }

        Location spawn = plugin.getServerSettingsManager().getSavedSpawn(player.getWorld().getName());
        if (spawn == null) {
            spawn = player.getWorld().getSpawnLocation();
        }

        plugin.getTeleportService().teleport(player, spawn, plugin.getTeleportService().standardOptions()
                .successMessageKey("teleport.success-spawn")
                .build());
        return true;
    }
}
