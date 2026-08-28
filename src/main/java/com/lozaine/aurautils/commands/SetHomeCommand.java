package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.util.DestinationName;
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

public class SetHomeCommand implements CommandExecutor {

    private final AuraUtils plugin;
    private final Map<UUID, PendingHome> pendingOverwrites = new ConcurrentHashMap<>();
    private static final long CONFIRMATION_MILLIS = 30_000L;

    private record PendingHome(String name, Location location, long expiresAt) { }

    public SetHomeCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/sethome");
            return true;
        }
        if (!player.hasPermission("aura.home.set")) {
            msg.send(player, "common.no-permission");
            return true;
        }
        if (args.length < 1) {
            msg.send(player, "home.usage-set");
            return true;
        }

        var name = DestinationName.parse(args[0]);
        if (name.isEmpty()) {
            msg.send(player, "home.invalid-name");
            return true;
        }

        String homeName = name.get();
        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            PendingHome pending = pendingOverwrites.get(player.getUniqueId());
            if (pending == null || pending.expiresAt() < System.currentTimeMillis()
                    || !DestinationName.normalizedKey(pending.name()).equals(DestinationName.normalizedKey(homeName))
                    || !pendingOverwrites.remove(player.getUniqueId(), pending)) {
                msg.send(player, "home.overwrite-expired");
                return true;
            }
            setHome(player, pending.name(), pending.location());
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
            PendingHome pending = pendingOverwrites.get(player.getUniqueId());
            if (pending == null || pending.expiresAt() < System.currentTimeMillis()
                    || !DestinationName.normalizedKey(pending.name()).equals(DestinationName.normalizedKey(homeName))
                    || !pendingOverwrites.remove(player.getUniqueId(), pending)) {
                msg.send(player, "home.overwrite-stale");
                return true;
            }
            msg.send(player, "home.overwrite-cancelled");
            return true;
        }
        if (plugin.getTeleportStoreManager().getHomeDestination(player.getUniqueId(), homeName) != null) {
            pendingOverwrites.put(player.getUniqueId(), new PendingHome(
                    homeName, player.getLocation().clone(), System.currentTimeMillis() + CONFIRMATION_MILLIS));
            msg.send(player, "home.overwrite-prompt",
                    "name", homeName,
                    "seconds", String.valueOf(CONFIRMATION_MILLIS / 1000L));
            sendConfirmation(player, "/sethome " + homeName);
            return true;
        }
        int limit = homeLimit(player);
        int currentHomes = plugin.getTeleportStoreManager().getHomes(player.getUniqueId()).size();
        if (limit > 0 && currentHomes >= limit) {
            msg.send(player, "home.limit", "limit", String.valueOf(limit));
            return true;
        }
        setHome(player, homeName, player.getLocation());
        return true;
    }

    /**
     * Returns zero for unlimited. Permission entries are intentionally generic
     * so LuckPerms (or Bukkit permissions) can assign them to any rank.
     */
    private int homeLimit(Player player) {
        int limit = Math.max(0, plugin.getConfig().getInt("homes.default-limit", 0));
        for (Map<?, ?> entry : plugin.getConfig().getMapList("homes.limits")) {
            Object permission = entry.get("permission");
            Object maximum = entry.get("max");
            if (!(permission instanceof String node) || node.isBlank() || maximum == null
                    || !player.hasPermission(node)) {
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(String.valueOf(maximum));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (value <= 0) {
                return 0;
            }
            limit = Math.max(limit, value);
        }
        return limit;
    }

    private void setHome(Player player, String name, Location loc) {
        plugin.getTeleportStoreManager().setHome(player.getUniqueId(), name, loc, player);
        plugin.getTeleportStoreManager().save();
        plugin.messages().send(player, "home.set", "name", name, "coords", formatPos(loc));
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
