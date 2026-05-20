package me.aurautils.commands;

import me.aurautils.AuraUtils;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuraCommand implements CommandExecutor {

    private final AuraUtils plugin;
    public AuraCommand(AuraUtils plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aura.admin")) {
                sender.sendMessage(AuraUtils.colorize("&cNo permission."));
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(AuraUtils.colorize("&aAuraUtils configuration reloaded, including damage weapons."));
            return true;
        }

        sender.sendMessage(AuraUtils.colorize("&8&m------------------------------------"));
        sender.sendMessage(AuraUtils.colorize("  &bAura&fUtils &7v" + plugin.getDescription().getVersion()));
        sender.sendMessage(AuraUtils.colorize("&7  Your utility bundle for Minecraft."));
        sender.sendMessage(AuraUtils.colorize(""));
        sender.sendMessage(AuraUtils.colorize("  &e/tpa &8<player>    &7- Request teleport"));
        sender.sendMessage(AuraUtils.colorize("  &e/tpaccept        &7- Accept TPA"));
        sender.sendMessage(AuraUtils.colorize("  &e/tpadeny         &7- Deny TPA"));
        sender.sendMessage(AuraUtils.colorize("  &e/warp &8[<name>|list] &7- Teleport to a warp or open GUI"));
        sender.sendMessage(AuraUtils.colorize("  &e/setwarp &8<name> &7- Set a warp"));
        sender.sendMessage(AuraUtils.colorize("  &e/home &8[<name>|list] &7- Teleport to a home or open GUI"));
        sender.sendMessage(AuraUtils.colorize("  &e/sethome &8<name> &7- Set a home"));
        sender.sendMessage(AuraUtils.colorize("  &e/back &7- Return to your last teleport"));
        
        sender.sendMessage(AuraUtils.colorize("  &e/menu &7- Open the utility GUI"));
        sender.sendMessage(AuraUtils.colorize("  &e/god &8[player]   &7- Toggle god mode"));
        sender.sendMessage(AuraUtils.colorize("  &e/fly &8[player]   &7- Toggle fly"));
        sender.sendMessage(AuraUtils.colorize("  &e/nofall &8[p]     &7- Toggle fall damage"));
        sender.sendMessage(AuraUtils.colorize("  &e/nohunger &8[p]   &7- Toggle hunger"));
        sender.sendMessage(AuraUtils.colorize("  &e/damage &8<mult>  &7- Set damage multiplier"));
        sender.sendMessage(AuraUtils.colorize("  &e/aura reload    &7- Reload config and weapon list"));
        sender.sendMessage(AuraUtils.colorize("  &e/rtp &7- Random safe teleport"));

        if (sender instanceof Player p) {
            var mgr = plugin.getPlayerDataManager();
            UUID id = p.getUniqueId();
            sender.sendMessage(AuraUtils.colorize(""));
            sender.sendMessage(AuraUtils.colorize("  &7Your status:"));
            sender.sendMessage(AuraUtils.colorize("  God: "   + toggle(mgr.isGod(id))     + "  Fly: "      + toggle(mgr.isFly(id))));
            sender.sendMessage(AuraUtils.colorize("  NoFall: "+ toggle(mgr.isNoFall(id))  + "  NoHunger: " + toggle(mgr.isNoHunger(id))));
            sender.sendMessage(AuraUtils.colorize("  Damage mult: &b" + mgr.getDamageMultiplier(id) + "x"));
        }
        sender.sendMessage(AuraUtils.colorize("&8&m------------------------------------"));
        return true;
    }

    private String toggle(boolean on) { return on ? "&aON" : "&cOFF"; }
}
