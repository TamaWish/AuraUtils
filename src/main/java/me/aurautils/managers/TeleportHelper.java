package me.aurautils.managers;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

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
 *
 * <p>All teleport entry points (home, warp, back, RTP, TPA accept, menu clicks)
 * should call {@link #scheduleTeleport} so behaviour stays consistent.
 * Players with {@code aura.teleport.bypass} skip the countdown entirely.
 *
 * <p>Uses FoliaLib entity schedulers so countdowns follow the player across
 * regions on Folia while remaining fully compatible with Spigot and Paper.
 */
public class TeleportHelper {

    private final AuraUtils plugin;

    /** player → active countdown task */
    private final Map<UUID, WrappedTask> pendingTasks = new HashMap<>();
    /** player → label of the pending destination (for cancel messages) */
    private final Map<UUID, String> pendingLabels = new HashMap<>();

    public TeleportHelper(AuraUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Teleport the player to the exact stored coordinates (same X/Y/Z/yaw/pitch).
     * Resolves the world by name so stale World references cannot shift the landing spot.
     * Uses async teleport when the platform supports it (Paper / Folia).
     *
     * @return true if the teleport was issued successfully (or scheduled)
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

        // Prefer platform-aware async teleport (safe on Spigot/Paper/Folia)
        plugin.getScheduler().teleportAsync(player, exact, PlayerTeleportEvent.TeleportCause.PLUGIN, success -> {
            if (Boolean.TRUE.equals(success) && player.isOnline() && soundsEnabled()) {
                play(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.1f);
            }
        });
        return true;
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
     * Players with {@code aura.teleport.bypass} are teleported immediately.
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

        // Bypass permission → instant teleport
        if (player.hasPermission("aura.teleport.bypass")) {
            if (teleportExact(player, dest)) {
                player.sendMessage(plugin.prefix("&aTeleported to &b" + label + "&a."));
            }
            return;
        }

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
        final int totalSeconds = seconds;
        final Set<Integer> chatAt = loadChatAtSeconds();
        final String displayMode = loadDisplayMode(); // chat | actionbar | both | none
        final boolean showTitle = plugin.getConfig().getBoolean("teleport.title", true);
        final boolean cancelOnMove = plugin.getConfig().getBoolean("teleport.cancel-on-move", true);
        final boolean risingPitch = plugin.getConfig().getBoolean("teleport.sound-rising-pitch", true);

        pendingLabels.put(playerId, label);

        // Entity scheduler: follows the player across regions on Folia
        final int[] remaining = { totalSeconds };
        final boolean[] announcedStart = { false };
        final WrappedTask[] taskHolder = new WrappedTask[1];

        taskHolder[0] = plugin.getScheduler().runAtEntityTimer(player, () -> {
            WrappedTask self = taskHolder[0];
            if (!plugin.isEnabled()) {
                cleanup(playerId);
                if (self != null) self.cancel();
                return;
            }

            Player p = plugin.getServer().getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                cleanup(playerId);
                if (self != null) self.cancel();
                return;
            }

            if (remaining[0] <= 0) {
                cleanup(playerId);
                clearDisplays(p);
                if (teleportExact(p, dest)) {
                    p.sendMessage(plugin.prefix("&aTeleported to &b" + label + "&a."));
                }
                if (self != null) self.cancel();
                return;
            }

            if (cancelOnMove) {
                Location now = p.getLocation();
                if (now.getWorld() == null || start.getWorld() == null
                        || !now.getWorld().equals(start.getWorld())) {
                    cleanup(playerId);
                    clearDisplays(p);
                    p.sendMessage(plugin.prefix("&cTeleport to &b" + label + " &ccancelled."));
                    if (soundsEnabled()) {
                        play(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                    }
                    if (self != null) self.cancel();
                    return;
                }
                if (now.distanceSquared(start) > 0.01) {
                    cleanup(playerId);
                    clearDisplays(p);
                    p.sendMessage(plugin.prefix("&cTeleport to &b" + label + " &ccancelled (you moved)."));
                    if (soundsEnabled()) {
                        play(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                    }
                    if (self != null) self.cancel();
                    return;
                }
            }

            // --- Chat ---
            boolean showChat = false;
            if ("chat".equals(displayMode) || "both".equals(displayMode)) {
                if (!announcedStart[0]) {
                    showChat = true;
                } else if (chatAt.isEmpty() || chatAt.contains(remaining[0])) {
                    showChat = true;
                }
            }
            if (showChat) {
                if (!announcedStart[0]) {
                    p.sendMessage(plugin.prefix(
                            "&eTeleporting to &b" + label + " &ein &6" + remaining[0]
                                    + " &eseconds... Don't move. &7(/tpacancel)"));
                } else {
                    p.sendMessage(plugin.prefix(
                            "&eTeleporting to &b" + label + " &ein &6" + remaining[0] + "&e... &7(/tpacancel)"));
                }
            }

            // --- Action bar ---
            if ("actionbar".equals(displayMode) || "both".equals(displayMode)) {
                sendActionBar(p, plugin.colorize(
                        "&e→ &b" + label + " &e• &6" + remaining[0] + "s &7• don't move"));
            }

            // --- Title / subtitle ---
            if (showTitle) {
                sendTitle(p,
                        plugin.colorize("&6" + remaining[0]),
                        plugin.colorize("&e→ &b" + label),
                        0, 25, 5);
            }

            // --- Sound (optional rising pitch as countdown progresses) ---
            if (soundsEnabled()) {
                float pitch;
                if (risingPitch && totalSeconds > 1) {
                    // 0.9 at start → ~1.5 near the end
                    float progress = 1.0f - ((float) remaining[0] / (float) totalSeconds);
                    pitch = 0.9f + (progress * 0.6f);
                } else {
                    pitch = announcedStart[0] ? 1.2f : 1.4f;
                }
                play(p, Sound.BLOCK_NOTE_BLOCK_PLING, 0.45f, pitch);
            }

            announcedStart[0] = true;
            remaining[0]--;
        }, 0L, 20L);

        pendingTasks.put(playerId, taskHolder[0]);
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
        WrappedTask task = pendingTasks.remove(id);
        String label = pendingLabels.remove(id);
        if (task == null) {
            return false;
        }
        try {
            task.cancel();
        } catch (Exception ignored) {
            // already cancelled
        }
        clearDisplays(player);
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
            clearDisplays(player);
            if (soundsEnabled()) {
                play(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
        }
    }

    /** Cancel all pending teleports (plugin disable). */
    public void cancelAll() {
        for (WrappedTask task : pendingTasks.values()) {
            if (task != null) {
                try {
                    task.cancel();
                } catch (Exception ignored) {
                    // ignore
                }
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
        // Default: start is handled separately; also chat at 3, 2, 1
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

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        } catch (Exception ignored) {
            // title API unavailable
        }
    }

    private void clearActionBar(Player player) {
        sendActionBar(player, " ");
    }

    private void clearTitle(Player player) {
        try {
            player.resetTitle();
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void clearDisplays(Player player) {
        clearActionBar(player);
        clearTitle(player);
    }
}
