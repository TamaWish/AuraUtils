package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.util.DestinationName;
import com.lozaine.aurautils.economy.EconomyAction;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SetWarpCommand implements CommandExecutor {

    private final AuraUtils plugin;
    private final Map<UUID, PendingWarp> pendingOverwrites = new ConcurrentHashMap<>();
    private static final long CONFIRMATION_MILLIS = 30_000L;

    private record PendingWarp(String name, Location location, long expiresAt) { }

    public SetWarpCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/setwarp");
            return true;
        }
        if (!player.hasPermission("aura.warp.set")) {
            msg.send(player, "common.no-permission");
            return true;
        }
        if (args.length < 1) {
            msg.send(player, "warp.usage-set");
            return true;
        }

        var name = DestinationName.parse(args[0]);
        if (name.isEmpty()) {
            msg.send(player, "warp.invalid-name");
            return true;
        }

        String warpName = name.get();
        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            PendingWarp pending = pendingOverwrites.get(player.getUniqueId());
            if (pending == null || pending.expiresAt() < System.currentTimeMillis()
                    || !DestinationName.normalizedKey(pending.name()).equals(DestinationName.normalizedKey(warpName))
                    || !pendingOverwrites.remove(player.getUniqueId(), pending)) {
                msg.send(player, "warp.overwrite-expired");
                return true;
            }
            setWarp(player, pending.name(), pending.location());
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
            PendingWarp pending = pendingOverwrites.get(player.getUniqueId());
            if (pending == null || pending.expiresAt() < System.currentTimeMillis()
                    || !DestinationName.normalizedKey(pending.name()).equals(DestinationName.normalizedKey(warpName))
                    || !pendingOverwrites.remove(player.getUniqueId(), pending)) {
                msg.send(player, "warp.overwrite-stale");
                return true;
            }
            msg.send(player, "warp.overwrite-cancelled");
            return true;
        }
        if (plugin.getTeleportStoreManager().getWarpDestination(warpName) != null) {
            if (!plugin.economy().ensureCanPay(player, EconomyAction.SET_WARP)) {
                return true;
            }
            pendingOverwrites.put(player.getUniqueId(), new PendingWarp(
                    warpName, player.getLocation().clone(), System.currentTimeMillis() + CONFIRMATION_MILLIS));
            msg.send(player, "warp.overwrite-prompt",
                    "name", warpName,
                    "seconds", String.valueOf(CONFIRMATION_MILLIS / 1000L));
            sendConfirmation(player, "/setwarp " + warpName);
            return true;
        }
        setWarp(player, warpName, player.getLocation());
        return true;
    }

    private void setWarp(Player player, String name, Location loc) {
        boolean charged = !plugin.economy().isFree(player, EconomyAction.SET_WARP);
        if (!plugin.economy().tryBeginCharge(player, EconomyAction.SET_WARP)) {
            return;
        }
        if (!plugin.getTeleportStoreManager().setWarp(name, loc, player)) {
            if (charged) {
                plugin.economy().abortCharge(player, EconomyAction.SET_WARP);
            }
            plugin.messages().send(player, "teleport.destination-world");
            return;
        }
        plugin.getTeleportStoreManager().save();
        plugin.messages().send(player, "warp.set", "name", name, "coords", formatPos(loc));
        if (charged) {
            plugin.economy().announceCharge(player, EconomyAction.SET_WARP);
        }
    }

    private void sendConfirmation(Player player, String commandPrefix) {
        var msg = plugin.messages();
        TextComponent confirm = new TextComponent(msg.get("common.confirm-button"));
        confirm.setColor(ChatColor.GREEN);
        confirm.setBold(true);
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandPrefix + " confirm"));
        TextComponent cancel = new TextComponent(msg.get("common.cancel-button"));
        cancel.setColor(ChatColor.RED);
        cancel.setBold(true);
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandPrefix + " cancel"));
        player.spigot().sendMessage(confirm, new TextComponent(" "), cancel);
    }

    private static String formatPos(org.bukkit.Location loc) {
        return String.format(java.util.Locale.US, "%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
    }
}
