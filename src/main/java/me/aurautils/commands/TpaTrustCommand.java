package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.managers.PlayerDataManager;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot manage TPA trust lists.");
            return true;
        }
        if (!player.hasPermission("aura.tpa.trust")) {
            player.sendMessage(plugin.prefix("&cYou don't have permission."));
            return true;
        }

        PlayerDataManager data = plugin.getPlayerDataManager();
        UUID ownerId = player.getUniqueId();

        if (removeOnly) {
            if (args.length < 1) {
                player.sendMessage(plugin.prefix("&cUsage: /tpauntrust <player>"));
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
                player.sendMessage(plugin.prefix("&cUsage: /tpatrust remove <player>"));
                return true;
            }
            return removeTrusted(player, data, ownerId, args[1]);
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                player.sendMessage(plugin.prefix("&cUsage: /tpatrust add <player>"));
                return true;
            }
            return addTrusted(player, data, ownerId, args[1]);
        }

        // Default: /tpatrust <player> = add
        return addTrusted(player, data, ownerId, args[0]);
    }

    private boolean listTrusted(Player player, PlayerDataManager data, UUID ownerId) {
        Set<UUID> trusted = data.getTrusted(ownerId);
        int max = Math.max(0, plugin.getConfig().getInt("tpa.trusted-max", 50));
        if (trusted.isEmpty()) {
            player.sendMessage(plugin.prefix("&7Your trusted list is empty. Use &e/tpatrust <player>&7 to add someone."));
            return true;
        }
        List<String> names = trusted.stream()
                .map(data::getDisplayName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        String limit = max > 0 ? " &8(" + names.size() + "/" + max + ")" : " &8(" + names.size() + ")";
        player.sendMessage(plugin.prefix("&aTrusted players" + limit + "&a:"));
        player.sendMessage(plugin.prefix("&7" + String.join("&8, &7", names)));
        player.sendMessage(plugin.prefix("&8They can /tpa you without confirmation."));
        return true;
    }

    private boolean addTrusted(Player player, PlayerDataManager data, UUID ownerId, String nameArg) {
        Resolved resolved = resolvePlayer(nameArg);
        if (resolved == null) {
            player.sendMessage(plugin.prefix("&cPlayer &e" + nameArg + " &cnot found (must have joined before)."));
            return true;
        }
        if (resolved.id.equals(ownerId)) {
            player.sendMessage(plugin.prefix("&cYou can't trust yourself."));
            return true;
        }
        if (data.isTrusted(ownerId, resolved.id)) {
            player.sendMessage(plugin.prefix("&e" + resolved.name + " &cis already on your trusted list."));
            return true;
        }
        int max = Math.max(0, plugin.getConfig().getInt("tpa.trusted-max", 50));
        if (max > 0 && data.getTrustedCount(ownerId) >= max) {
            player.sendMessage(plugin.prefix("&cTrusted list is full (&e" + max + "&c). Remove someone first."));
            return true;
        }
        boolean added = data.addTrusted(ownerId, resolved.id, resolved.name);
        if (added) {
            player.sendMessage(plugin.prefix("&aAdded &e" + resolved.name
                    + " &ato your trusted list. They can /tpa you without confirmation."));
            Player online = plugin.getServer().getPlayer(resolved.id);
            if (online != null && online.isOnline()) {
                online.sendMessage(plugin.prefix("&e" + player.getName()
                        + " &aadded you to their trusted TPA list."));
            }
        } else {
            player.sendMessage(plugin.prefix("&cCould not add &e" + resolved.name + "&c."));
        }
        return true;
    }

    private boolean removeTrusted(Player player, PlayerDataManager data, UUID ownerId, String nameArg) {
        // Prefer match against known trusted names first
        UUID match = null;
        String matchName = null;
        String lower = nameArg.toLowerCase(Locale.ROOT);
        for (UUID tid : data.getTrusted(ownerId)) {
            String display = data.getDisplayName(tid);
            if (display.equalsIgnoreCase(nameArg) || display.toLowerCase(Locale.ROOT).startsWith(lower)) {
                match = tid;
                matchName = display;
                if (display.equalsIgnoreCase(nameArg)) {
                    break;
                }
            }
        }
        if (match == null) {
            Resolved resolved = resolvePlayer(nameArg);
            if (resolved != null && data.isTrusted(ownerId, resolved.id)) {
                match = resolved.id;
                matchName = resolved.name;
            }
        }
        if (match == null) {
            player.sendMessage(plugin.prefix("&c&e" + nameArg + " &cis not on your trusted list."));
            return true;
        }
        if (data.removeTrusted(ownerId, match)) {
            player.sendMessage(plugin.prefix("&aRemoved &e" + matchName + " &afrom your trusted list."));
        } else {
            player.sendMessage(plugin.prefix("&cCould not remove &e" + nameArg + "&c."));
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
