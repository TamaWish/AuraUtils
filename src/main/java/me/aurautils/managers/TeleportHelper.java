package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles delayed teleports and exact-coordinate teleports.
 * Destinations are always cloned and applied with full X/Y/Z/yaw/pitch fidelity.
 * At most one pending countdown teleport is tracked per player.
 */
public class TeleportHelper {

    private final AuraUtils plugin;

    /** player → active countdown task */
    private final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();
    /** player → label of the pending destination (for cancel messages) */
    private final Map<UUID, String> pendingLabels = new HashMap<>();

    public TeleportHelper(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Teleport the player to the exact stored coordinates (same X/Y/Z/yaw/pitch).
     * Resolves the world by name so stale World references cannot shift the landing spot.
     *
     * @return true if the teleport was issued successfully
     */
    public boolean teleportExact(Player player, Location destination) {
        if (player == null || !player.isOnline() || destination == null) {
            return false;
        }

        Location exact = resolveExact(destination);
        if (exact == null) {
            player.sendMessage(plugin.prefix("&cDestination world is not loaded."));
            return false;
        }

        boolean ok = player.teleport(exact, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (ok && soundsEnabled()) {
            play(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.1f);
        }
        return ok;
    }

    /**
     * Build a fresh Location with the same world name and exact double coordinates.
     */
    public static Location resolveExact(Location source) {
        if (source == null) {
            return null;
        }
        World world = source.getWorld();
        if (world == null) {
            return null;
        }
        World live = org.bukkit.Bukkit.getWorld(world.getName());
        if (live == null) {
            return null;
        }
        return new Location(
                live,
                source.getX(),
                source.getY(),
                source.getZ(),
                source.getYaw(),
                source.getPitch()
        );
    }

    /**
     * Countdown teleport with a destination label for clearer messages.
     *
     * @param destinationLabel short description shown in messages, e.g. "warp Spawn"
     */
    public void scheduleTeleport(Player player, Location destination, int seconds, String destinationLabel) {
        if (player == null || !player.isOnline()) {
            return;
        }

        final Location dest = resolveExact(destination);
        if (dest == null) {
            player.sendMessage(plugin.prefix("&cDestination world is not loaded."));
            return;
        }

        final String label = (destinationLabel == null || destinationLabel.isBlank())
                ? "your destination"
                : destinationLabel.trim();

        if (!plugin.isEnabled()) {
            if (teleportExact(player, dest)) {
                player.sendMessage(plugin.prefix("&aTeleported to &b" + label + "&a."));
            }
            return;
        }

        if (seconds <= 0) {
            if (teleportExact(player, dest)) {
                player.sendMessage(plugin.prefix("&aTeleported to &b" + label + "&a."));
            }
            return;
        }

        // Replace any existing pending teleport for this player
        if (cancelTeleport(player, false)) {
            player.sendMessage(plugin.prefix("&ePrevious teleport cancelled."));
        }

        final Location start = player.getLocation().clone();
        final UUID playerId = player.getUniqueId();
        final Set<Integer> chatAt = loadChatAtSeconds();
        final String displayMode = loadDisplayMode(); // chat | actionbar | both | none
        final boolean cancelOnMove = plugin.getConfig().getBoolean("teleport.cancel-on-move", true);

        pendingLabels.put(playerId, label);

        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;
            boolean announcedStart = false;

            @Override
            public void run() {
                if (!plugin.isEnabled()) {
                    cleanup(playerId);
                    cancel();
                    return;
                }

                Player p = plugin.getServer().getPlayer(playerId);
                if (p == null || !p.isOnline()) {
                    cleanup(playerId);
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    cleanup(playerId);
                    if (teleportExact(p, dest)) {
                        p.sendMessage(plugin.prefix("&aTeleported to &b" + label + "&a."));
                        clearActionBar(p);
                    }
                    cancel();
                    return;
                }

                if (cancelOnMove) {
                    Location now = p.getLocation();
                    if (now.getWorld() == null || start.getWorld() == null
                            || !now.getWorld().equals(start.getWorld())) {
                        cleanup(playerId);
                        p.sendMessage(plugin.prefix("&cTeleport to &b" + label + " &ccancelled."));
                        clearActionBar(p);
                        if (soundsEnabled()) {
                            play(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                        }
                        cancel();
                        return;
                    }
                    if (now.distanceSquared(start) > 0.01) {
                        cleanup(playerId);
                        p.sendMessage(plugin.prefix("&cTeleport to &b" + label + " &ccancelled (you moved)."));
                        clearActionBar(p);
                        if (soundsEnabled()) {
                            play(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                        }
                        cancel();
                        return;
                    }
                }

                // Messages / action bar
                boolean showChat = false;
                if ("chat".equals(displayMode) || "both".equals(displayMode)) {
                    if (!announcedStart) {
                        showChat = true;
                    } else if (chatAt.isEmpty() || chatAt.contains(remaining)) {
                        showChat = true;
                    }
                }
                if (showChat) {
                    p.sendMessage(plugin.prefix(
                            "&eTeleporting to &b" + label + " &ein &6" + remaining + "&e... Don't move. &7(/tpacancel)"));
                }

                if ("actionbar".equals(displayMode) || "both".equals(displayMode)) {
                    sendActionBar(p, plugin.colorize(
                            "&e→ &b" + label + " &e• &6" + remaining + "s &7• don't move"));
                }

                if (!announcedStart && soundsEnabled()) {
                    play(p, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f);
                }

                announcedStart = true;
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        pendingTasks.put(playerId, task);
    }

    /** Backwards-compatible overload without label. */
    public void scheduleTeleport(Player player, Location destination, int seconds) {
        scheduleTeleport(player, destination, seconds, "your destination");
    }

    /**
     * Cancel a pending countdown teleport for the player.
     *
     * @param notify if true, send a cancel message when something was cancelled
     * @return true if a pending teleport was cancelled
     */
    public boolean cancelTeleport(Player player, boolean notify) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUniqueId();
        BukkitTask task = pendingTasks.remove(id);
        String label = pendingLabels.remove(id);
        if (task == null) {
            return false;
        }
        task.cancel();
        clearActionBar(player);
        if (notify && player.isOnline()) {
            String shown = label != null ? label : "your destination";
            player.sendMessage(plugin.prefix("&cTeleport to &b" + shown + " &ccancelled."));
            if (soundsEnabled()) {
                play(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
        }
        return true;
    }

    public boolean cancelTeleport(Player player) {
        return cancelTeleport(player, true);
    }

    public boolean hasPending(UUID playerId) {
        return pendingTasks.containsKey(playerId);
    }

    public boolean hasPending(Player player) {
        return player != null && hasPending(player.getUniqueId());
    }

    /** Called when the player takes damage, if cancel-on-damage is enabled. */
    public void handleDamage(Player player) {
        if (player == null || !plugin.getConfig().getBoolean("teleport.cancel-on-damage", false)) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!pendingTasks.containsKey(id)) {
            return;
        }
        String label = pendingLabels.getOrDefault(id, "your destination");
        if (cancelTeleport(player, false) && player.isOnline()) {
            player.sendMessage(plugin.prefix("&cTeleport to &b" + label + " &ccancelled (you took damage)."));
            clearActionBar(player);
            if (soundsEnabled()) {
                play(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
        }
    }

    /** Cancel all pending teleports (plugin disable). */
    public void cancelAll() {
        for (BukkitTask task : pendingTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        pendingTasks.clear();
        pendingLabels.clear();
    }

    private void cleanup(UUID playerId) {
        pendingTasks.remove(playerId);
        pendingLabels.remove(playerId);
    }

    private boolean soundsEnabled() {
        return plugin.getConfig().getBoolean("teleport.sound", true);
    }

    private String loadDisplayMode() {
        String mode = plugin.getConfig().getString("teleport.countdown-display", "both");
        if (mode == null) {
            return "both";
        }
        mode = mode.trim().toLowerCase();
        return switch (mode) {
            case "chat", "actionbar", "both", "none" -> mode;
            default -> "both";
        };
    }

    private Set<Integer> loadChatAtSeconds() {
        Set<Integer> set = new HashSet<>();
        List<Integer> list = plugin.getConfig().getIntegerList("teleport.chat-at");
        if (list != null) {
            for (Integer n : list) {
                if (n != null && n > 0) {
                    set.add(n);
                }
            }
        }
        // Default: start is handled separately; also chat at 3,2,1
        if (set.isEmpty() && !plugin.getConfig().isSet("teleport.chat-at")) {
            set.add(3);
            set.add(2);
            set.add(1);
        }
        return set;
    }

    private void play(Player player, Sound sound, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {
            // sound may not exist on some forks
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
            );
        } catch (Exception ignored) {
            // action bar unavailable
        }
    }

    private void clearActionBar(Player player) {
        sendActionBar(player, " ");
    }
}
