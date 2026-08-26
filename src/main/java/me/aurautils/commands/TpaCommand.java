package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public TpaCommand(AuraUtils plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Console cannot use TPA."); return true;
        }
        if (!p.hasPermission("aura.tpa")) {
            p.sendMessage(plugin.prefix("&cYou don't have permission.")); return true;
        }
        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            plugin.getMenuManager().openTpaMenu(p);
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage(plugin.prefix("&cPlayer &e" + args[0] + " &cis not online.")); return true;
        }
        if (target.equals(p)) {
            p.sendMessage(plugin.prefix("&cYou can't TPA to yourself.")); return true;
        }

        TpaManager.SendResult result = plugin.getTpaManager().sendRequest(p, target);
        switch (result) {
            case TRUSTED_INSTANT -> {
                // Messages already sent inside TpaManager
            }
            case PENDING -> {
                int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
                p.sendMessage(plugin.prefix("&aTPA request sent to &e" + target.getName()
                        + "&a. Expires in &b" + timeout + "s&a."));
                target.sendMessage(plugin.prefix("&e" + p.getName()
                        + " &awants to teleport to you. Use &b/tpaccept &aor &c/tpadeny&a."));
            }
            case BUSY -> p.sendMessage(plugin.prefix(
                    "&cThat player already has a pending request. Wait for it to expire."));
            case FAILED -> p.sendMessage(plugin.prefix("&cCould not send TPA request."));
        }
        return true;
    }
}
