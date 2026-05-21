package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaHereCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaHereCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Console cannot use TPA Here.");
            return true;
        }
        if (!p.hasPermission("aura.tpa")) {
            p.sendMessage(plugin.prefix("&cYou don't have permission."));
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(plugin.prefix("&cUsage: &e/tpahere <player>"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage(plugin.prefix("&cPlayer &e" + args[0] + " &cis not online."));
            return true;
        }
        if (target.equals(p)) {
            p.sendMessage(plugin.prefix("&cYou can't send TPA Here to yourself."));
            return true;
        }

        boolean sent = plugin.getTpaManager().sendRequest(p, target, TpaManager.TpaType.TO_REQUESTER);
        if (!sent) {
            p.sendMessage(plugin.prefix("&cThat player already has a pending request. Wait for it to expire."));
            return true;
        }

        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        p.sendMessage(plugin.prefix("&aTPA Here request sent to &e" + target.getName() + "&a. Expires in &b" + timeout + "s&a."));
        target.sendMessage(plugin.prefix("&e" + p.getName() + " &awants you to teleport to them. Use &b/tpaccept &aor &c/tpadeny&a."));
        return true;
    }
}
