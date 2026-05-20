package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DamageCommand implements CommandExecutor {

    private final AuraUtils plugin;
    public DamageCommand(AuraUtils plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Console cannot use this."); return true; }
        if (!p.hasPermission("aura.damage")) {
            p.sendMessage(plugin.prefix("&cNo permission.")); return true;
        }
        if (args.length < 1) {
            double current = plugin.getPlayerDataManager().getDamageMultiplier(p.getUniqueId());
            p.sendMessage(plugin.prefix("&eCurrent damage multiplier: &b" + current + "x"));
            p.sendMessage(plugin.prefix("&eUsage: /damage <multiplier>  (e.g. &b/damage 2.5&e)"));
            return true;
        }

        double maxMult = plugin.getConfig().getDouble("damage-multiplier-max", 10.0);
        double value;
        try {
            value = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            p.sendMessage(plugin.prefix("&cInvalid number: &e" + args[0])); return true;
        }

        if (value <= 0) {
            p.sendMessage(plugin.prefix("&cMultiplier must be greater than 0.")); return true;
        }
        if (value > maxMult) {
            p.sendMessage(plugin.prefix("&cMax multiplier is &e" + maxMult + "x&c.")); return true;
        }

        plugin.getPlayerDataManager().setDamageMultiplier(p.getUniqueId(), value);
        p.sendMessage(plugin.prefix("&aWeapon damage multiplier set to &b" + value + "x&a."));
        return true;
    }
}
