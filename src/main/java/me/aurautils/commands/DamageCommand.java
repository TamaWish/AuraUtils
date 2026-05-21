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
        // Support two forms:
        // 1) /damage <multiplier>  -> set player-specific multiplier
        // 2) /damage <MATERIAL|hand> <multiplier> -> set global weapon multiplier (requires aura.damage.weapon)

        double maxMult = plugin.getConfig().getDouble("damage-multiplier-max", 10.0);

        if (args.length == 0) {
            double current = plugin.getPlayerDataManager().getDamageMultiplier(p.getUniqueId());
            p.sendMessage(plugin.prefix("&eCurrent damage multiplier: &b" + current + "x"));
            p.sendMessage(plugin.prefix("&eUsage: /damage <multiplier>  OR  /damage <MATERIAL|hand> <multiplier>"));
            return true;
        }

        if (args.length == 1) {
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

        // args.length >= 2 -> attempt to set global per-material multiplier
        if (!p.hasPermission("aura.damage.weapon") && !p.hasPermission("aura.admin")) {
            p.sendMessage(plugin.prefix("&cNo permission to set weapon multipliers.")); return true;
        }

        String target = args[0];
        String multArg = args[1];
        double value;
        try {
            value = Double.parseDouble(multArg);
        } catch (NumberFormatException e) {
            p.sendMessage(plugin.prefix("&cInvalid number: &e" + multArg)); return true;
        }

        if (value <= 0) {
            p.sendMessage(plugin.prefix("&cMultiplier must be greater than 0.")); return true;
        }
        if (value > maxMult) {
            p.sendMessage(plugin.prefix("&cMax multiplier is &e" + maxMult + "x&c.")); return true;
        }

        org.bukkit.Material mat = null;
        if (target.equalsIgnoreCase("hand")) {
            if (p.getInventory().getItemInMainHand() == null) {
                p.sendMessage(plugin.prefix("&cYou have nothing in hand.")); return true;
            }
            mat = p.getInventory().getItemInMainHand().getType();
        } else {
            try {
                mat = org.bukkit.Material.valueOf(target.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                p.sendMessage(plugin.prefix("&cUnknown material: &e" + target)); return true;
            }
        }

        plugin.setWeaponDamageMultiplier(mat, value);
        p.sendMessage(plugin.prefix("&aSet weapon multiplier for &b" + mat.name() + " &ato &b" + value + "x&a."));
        return true;
    }
}
