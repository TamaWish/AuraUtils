package me.aurautils.commands;

import me.aurautils.AuraUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;

public class WeaponDamageCommand implements CommandExecutor {

    private final AuraUtils plugin;
    public WeaponDamageCommand(AuraUtils plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aura.damage.weapon") && !sender.hasPermission("aura.admin")) {
            sender.sendMessage(plugin.prefix("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.prefix("&eUsage: /weapondamage <list|set|remove> ..."));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            Set<Material> weapons = plugin.getDamageWeapons();
            if (weapons.isEmpty()) {
                sender.sendMessage(plugin.prefix("&eNo configured damage weapons."));
                return true;
            }
            sender.sendMessage(plugin.prefix("&eConfigured weapon multipliers:"));
            for (Material m : weapons) {
                Double mult = plugin.getWeaponDamageMultiplier(m);
                if (mult == null) mult = plugin.getConfig().getDouble("damage-multiplier-default", 1.0);
                sender.sendMessage(plugin.prefix(" &b" + m.name() + ": &a" + mult + "x"));
            }
            return true;
        }

        if (sub.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.prefix("&eUsage: /weapondamage set <MATERIAL> <multiplier>"));
                return true;
            }
            String matName = args[1].toUpperCase(Locale.ROOT);
            Material mat;
            try {
                mat = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.prefix("&cUnknown material: &e" + matName));
                return true;
            }
            double val;
            try {
                val = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.prefix("&cInvalid number: &e" + args[2]));
                return true;
            }
            if (val <= 0) {
                sender.sendMessage(plugin.prefix("&cMultiplier must be greater than 0."));
                return true;
            }
            plugin.setWeaponDamageMultiplier(mat, val);
            sender.sendMessage(plugin.prefix("&aSet &b" + mat.name() + " &ato &b" + val + "x&a."));
            return true;
        }

        if (sub.equals("remove") || sub.equals("unset")) {
            if (args.length < 2) {
                sender.sendMessage(plugin.prefix("&eUsage: /weapondamage remove <MATERIAL>"));
                return true;
            }
            String matName = args[1].toUpperCase(Locale.ROOT);
            Material mat;
            try {
                mat = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.prefix("&cUnknown material: &e" + matName));
                return true;
            }
            plugin.setWeaponDamageMultiplier(mat, null);
            sender.sendMessage(plugin.prefix("&aRemoved configured multiplier for &b" + mat.name() + "&a."));
            return true;
        }

        sender.sendMessage(plugin.prefix("&cUnknown subcommand. Use list/set/remove."));
        return true;
    }
}
