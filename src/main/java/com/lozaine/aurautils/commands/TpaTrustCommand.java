package com.lozaine.aurautils.commands;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.managers.PlayerDataManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manage the trusted TPA list.
 * <ul>
 *   <li>{@code /tpatrust <player>} — add (they can TPA to you without confirmation)</li>
 *   <li>{@code /tpatrust list} — show your list</li>
 *   <li>{@code /tpatrust remove <player>} or {@code /tpauntrust <player>}</li>
 * </ul>
 */
public class TpaTrustCommand implements CommandExecutor, TabCompleter {

    private final AuraUtils plugin;
    private final boolean removeOnly;

    public TpaTrustCommand(AuraUtils plugin) {
        this(plugin, false);
    }

    /**
     * @param removeOnly when true, this executor is wired to /tpauntrust and only removes
     */
    public TpaTrustCommand(AuraUtils plugin, boolean removeOnly) {
        this.plugin = plugin;
        this.removeOnly = removeOnly;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        var msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.sendPlain(sender, "common.console-players-only", "command", "/" + label);
            return true;
        }
        if (!player.hasPermission("aura.tpa.trust")) {
            msg.send(player, "common.no-permission");
            return true;
        }

        PlayerDataManager data = plugin.getPlayerDataManager();
        UUID ownerId = player.getUniqueId();

        if (removeOnly) {
            if (args.length < 1) {
                plugin.messages().send(player, "trust.usage-untrust");
                return true;
            }
            return removeTrusted(player, data, ownerId, args[0]);
        }

        if (args.length < 1 || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
            return listTrusted(player, data, ownerId);
        }

        if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("untrust")
                || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                plugin.messages().send(player, "trust.usage-remove");
                return true;
            }
            return removeTrusted(player, data, ownerId, args[1]);
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                plugin.messages().send(player, "trust.usage-add");
                return true;
            }
            return addTrusted(player, data, ownerId, args[1]);
        }

        // Default: /tpatrust <player> = add
        return addTrusted(player, data, ownerId, args[0]);
    }

    private boolean listTrusted(Player player, PlayerDataManager data, UUID ownerId) {
        var msg = plugin.messages();
        Set<UUID> trusted = data.getTrusted(ownerId);
        int max = Math.max(0, plugin.getConfig().getInt("tpa.trusted-max", 50));
        if (trusted.isEmpty()) {
            msg.send(player, "trust.empty");
            return true;
        }
        List<String> names = trusted.stream()
                .map(data::getDisplayName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        String current = String.valueOf(names.size());
        if (max > 0) {
            msg.send(player, "trust.list-header", "current", current, "max", String.valueOf(max));
        } else {
            msg.send(player, "trust.list-header-unlimited", "current", current);
        }
        msg.send(player, "trust.list-names", "names", String.join(msg.get("trust.list-separator"), names));
        msg.send(player, "trust.list-footer");
        return true;
    }

    private boolean addTrusted(Player player, PlayerDataManager data, UUID ownerId, String nameArg) {
        var msg = plugin.messages();
        Resolved resolved = resolvePlayer(nameArg);
        if (resolved == null) {
            msg.send(player, "trust.not-found", "name", nameArg);
            return true;
        }
        if (resolved.id.equals(ownerId)) {
            msg.send(player, "trust.self");
            return true;
        }
        if (data.isTrusted(ownerId, resolved.id)) {
            msg.send(player, "trust.already", "player", resolved.name);
            return true;
        }
        int max = Math.max(0, plugin.getConfig().getInt("tpa.trusted-max", 50));
        if (max > 0 && data.getTrustedCount(ownerId) >= max) {
            msg.send(player, "trust.full", "max", String.valueOf(max));
            return true;
        }
        boolean added = data.addTrusted(ownerId, resolved.id, resolved.name);
        if (added) {
            msg.send(player, "trust.added", "player", resolved.name);
            Player online = plugin.getServer().getPlayer(resolved.id);
            if (online != null && online.isOnline()) {
                msg.send(online, "trust.added-notify", "player", player.getName());
            }
        } else {
            msg.send(player, "trust.add-failed", "player", resolved.name);
        }
        return true;
    }

    private boolean removeTrusted(Player player, PlayerDataManager data, UUID ownerId, String nameArg) {
        // Prefer exact match against known trusted names; allow unique prefix match
        UUID match = null;
        String matchName = null;
        String lower = nameArg.toLowerCase(Locale.ROOT);
        List<UUID> prefixMatches = new ArrayList<>();
        List<String> prefixNames = new ArrayList<>();
        for (UUID tid : data.getTrusted(ownerId)) {
            String display = data.getDisplayName(tid);
            if (display.equalsIgnoreCase(nameArg)) {
                match = tid;
                matchName = display;
                break;
            }
            if (display.toLowerCase(Locale.ROOT).startsWith(lower)) {
                prefixMatches.add(tid);
                prefixNames.add(display);
            }
        }
        if (match == null && prefixMatches.size() == 1) {
            match = prefixMatches.get(0);
            matchName = prefixNames.get(0);
        } else if (match == null && prefixMatches.size() > 1) {
            plugin.messages().send(player, "trust.ambiguous",
                    "name", nameArg,
                    "matches", String.join(plugin.messages().get("trust.list-separator"), prefixNames));
            return true;
        }
        if (match == null) {
            Resolved resolved = resolvePlayer(nameArg);
            if (resolved != null && data.isTrusted(ownerId, resolved.id)) {
                match = resolved.id;
                matchName = resolved.name;
            }
        }
        if (match == null) {
            plugin.messages().send(player, "trust.not-on-list", "name", nameArg);
            return true;
        }
        if (data.removeTrusted(ownerId, match)) {
            plugin.messages().send(player, "trust.removed", "player", matchName);
        } else {
            plugin.messages().send(player, "trust.remove-failed", "name", nameArg);
        }
        return true;
    }

    private static final class Resolved {
        final UUID id;
        final String name;

        Resolved(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private Resolved resolvePlayer(String name) {
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return new Resolved(online.getUniqueId(), online.getName());
        }
        // Case-insensitive online match
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return new Resolved(p.getUniqueId(), p.getName());
            }
        }
        // Offline (must have played before for a real UUID)
        @SuppressWarnings("deprecation")
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(name);
        if (off.hasPlayedBefore() || off.isOnline()) {
            String n = off.getName() != null ? off.getName() : name;
            return new Resolved(off.getUniqueId(), n);
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        if (!player.hasPermission("aura.tpa.trust")) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        if (removeOnly) {
            if (args.length == 1) {
                String prefix = args[0].toLowerCase(Locale.ROOT);
                for (UUID tid : plugin.getPlayerDataManager().getTrusted(player.getUniqueId())) {
                    String name = plugin.getPlayerDataManager().getDisplayName(tid);
                    if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(name);
                    }
                }
            }
            return out;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : List.of("list", "add", "remove")) {
                if (sub.startsWith(prefix)) {
                    out.add(sub);
                }
            }
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.equals(player)) {
                    continue;
                }
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("remove") || sub.equals("untrust") || sub.equals("del") || sub.equals("delete")) {
                for (UUID tid : plugin.getPlayerDataManager().getTrusted(player.getUniqueId())) {
                    String name = plugin.getPlayerDataManager().getDisplayName(tid);
                    if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(name);
                    }
                }
            } else if (sub.equals("add")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.equals(player)) {
                        continue;
                    }
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(p.getName());
                    }
                }
            }
        }
        return out;
    }
}
