package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuraCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public AuraCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        var msg = plugin.messages();
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aura.admin")) {
                msg.send(sender, "common.no-permission");
                return true;
            }

            plugin.reloadPluginConfig();
            msg.send(sender, "aura.reload", "language", plugin.messages().language());
            msg.send(sender, "aura.reload-stores");
            return true;
        }

        msg.sendLines(sender, "aura.help", "version", plugin.getDescription().getVersion());

        if (sender instanceof Player p) {
            var mgr = plugin.getPlayerDataManager();
            UUID id = p.getUniqueId();
            msg.sendPlain(sender, "aura.status-header");
            msg.sendPlain(sender, "aura.status-toggles",
                    "god", msg.onOff(mgr.isGod(id)),
                    "fly", msg.onOff(mgr.isFly(id)));
            msg.sendPlain(sender, "aura.status-protection",
                    "nofall", msg.onOff(mgr.isNoFall(id)),
                    "nohunger", msg.onOff(mgr.isNoHunger(id)));
            msg.sendPlain(sender, "aura.status-timber",
                    "timber", msg.onOff(mgr.isTimber(id)));
            if (plugin.getPlayerInventoryManager() != null && plugin.getPlayerInventoryManager().isEnabled()) {
                msg.sendPlain(sender, "aura.status-inventories",
                        "limit", String.valueOf(plugin.getPlayerInventoryManager().resolveLimit(p)));
            }
            if (p.hasPermission("aura.admin")) {
                if (plugin.economy() != null && plugin.economy().isHooked()) {
                    msg.sendPlain(sender, "aura.economy-hooked", "provider", plugin.economy().providerName());
                } else {
                    msg.sendPlain(sender, "aura.economy-idle");
                }
            }
        }
        msg.sendPlain(sender, "aura.help-footer");
        return true;
    }
}
