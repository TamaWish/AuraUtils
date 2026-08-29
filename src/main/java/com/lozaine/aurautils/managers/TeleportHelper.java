package com.lozaine.aurautils.managers;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.lozaine.aurautils.AuraUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Delayed and exact teleports. Destinations keep full X/Y/Z/yaw/pitch.
 * All entry points should use {@link #scheduleTeleport} or {@link #teleportExact}.
 */
public class TeleportHelper {

    private final AuraUtils plugin;
    /** May be read by entity, region, and shutdown threads on Folia. */
    private final Map<UUID, WrappedTask> pendingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingLabels = new ConcurrentHashMap<>();

    public TeleportHelper(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public boolean teleportExact(Player player, Location destination) {
        return teleportExact(player, destination, null);
    }

    public boolean teleportExact(Player player, Location destination, Consumer<Boolean> after) {
        CompletableFuture<Boolean> issued = startTeleport(player, destination, after);
        return issued != null;
    }

    /**
     * Instant teleport that reports success or {@code teleport.failed} after the
     * platform future completes — never before the request is submitted.
     */
    public boolean teleportExact(Player player, Location destination,
                                 String successKey, String failLabel, String... successPlaceholders) {
        return teleportExact(player, destination, success -> {
            if (player == null || !player.isOnline()) {
                return;
            }
            if (Boolean.TRUE.equals(success)) {
                plugin.messages().send(player, successKey, successPlaceholders);
            } else {
                plugin.messages().send(player, "teleport.failed", "label", failLabel);
            }
        });
    }

    private CompletableFuture<Boolean> startTeleport(Player player, Location destination, Consumer<Boolean> after) {
        if (player == null || !player.isOnline() || destination == null) {
            if (after != null) {
                after.accept(false);
            }
            return null;
        }

        Location exact = resolveExact(destination);
        if (exact == null) {
            plugin.messages().send(player, "teleport.destination-world");
            if (after != null) {
                after.accept(false);
            }
            return null;
        }

        Location from = player.getLocation();
        if (from.getWorld() != null) {
            boolean sameSpot = exact.getWorld() != null
                    && from.getWorld().equals(exact.getWorld())
                    && from.distanceSquared(exact) < 0.01D;
            if (!sameSpot) {
                plugin.getBackManager().record(player.getUniqueId(), from);
            }
        }

        return plugin.getScheduler().teleportAsync(player, exact, PlayerTeleportEvent.TeleportCause.PLUGIN, success -> {
            if (Boolean.TRUE.equals(success) && player.isOnline() && soundsEnabled()) {
                play(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.1f);
            }
            if (after != null) {
                after.accept(Boolean.TRUE.equals(success));
            }
        });
    }

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

    public void scheduleTeleport(Player player, Location destination, int seconds, String destinationLabel) {
        if (player == null || !player.isOnline()) {
            return;
        }

        final Location dest = resolveExact(destination);
        if (dest == null) {
            plugin.messages().send(player, "teleport.destination-world");
            return;
        }

        final String label = (destinationLabel == null || destinationLabel.isBlank())
                ? plugin.messages().get("teleport.default-label")
                : destinationLabel.trim();

        if (player.hasPermission("aura.teleport.bypass") || !plugin.isEnabled() || seconds <= 0) {
            finishTeleport(player, dest, label);
            return;
        }

        if (cancelTeleport(player, false)) {
            player.sendMessage(plugin.prefix(plugin.messages().get("teleport.previous-cancelled")));
        }

        final Location start = player.getLocation().clone();
        final UUID playerId = player.getUniqueId();
        final int totalSeconds = seconds;
        final Set<Integer> chatAt = loadChatAtSeconds();
        final String displayMode = loadDisplayMode();
        final boolean showTitle = plugin.getConfig().getBoolean("teleport.title", true);
        final boolean cancelOnMove = plugin.getConfig().getBoolean("teleport.cancel-on-move", true);
        final boolean risingPitch = plugin.getConfig().getBoolean("teleport.sound-rising-pitch", true);

        pendingLabels.put(playerId, label);

        final int[] remaining = {totalSeconds};
        final boolean[] announcedStart = {false};
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
                finishTeleport(p, dest, label);
                if (self != null) self.cancel();
                return;
            }

            if (cancelOnMove) {
                Location now = p.getLocation();
                if (now.getWorld() == null || start.getWorld() == null
                        || !now.getWorld().equals(start.getWorld())) {
                    abortMove(p, playerId, label, self, false);
                    return;
                }
                if (now.distanceSquared(start) > 0.01) {
                    abortMove(p, playerId, label, self, true);
                    return;
                }
            }

            boolean showChat = false;
            if ("chat".equals(displayMode) || "both".equals(displayMode)) {
                if (!announcedStart[0]) {
                    showChat = true;
                } else if (chatAt.isEmpty() || chatAt.contains(remaining[0])) {
                    showChat = true;
                }
            }
            if (showChat) {
                var msg = plugin.messages();
                String secondsText = String.valueOf(remaining[0]);
                if (!announcedStart[0]) {
                    p.sendMessage(plugin.prefix(msg.get("teleport.countdown-start",
                            "label", label, "seconds", secondsText)));
                } else {
                    p.sendMessage(plugin.prefix(msg.get("teleport.countdown-tick",
                            "label", label, "seconds", secondsText)));
                }
            }

            if ("actionbar".equals(displayMode) || "both".equals(displayMode)) {
                sendActionBar(p, plugin.colorize(plugin.messages().get("teleport.actionbar",
                        "label", label, "seconds", String.valueOf(remaining[0]))));
            }

            if (showTitle) {
                sendTitle(p,
                        plugin.colorize(plugin.messages().get("teleport.title", "seconds", String.valueOf(remaining[0]))),
                        plugin.colorize(plugin.messages().get("teleport.subtitle", "label", label)),
                        0, 25, 5);
            }

            if (soundsEnabled()) {
                float pitch;
                if (risingPitch && totalSeconds > 1) {
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

    public void scheduleTeleport(Player player, Location destination, int seconds) {
        scheduleTeleport(player, destination, seconds, plugin.messages().get("teleport.default-label"));
    }

    private void finishTeleport(Player player, Location dest, String label) {
        teleportExact(player, dest, success -> {
            if (!player.isOnline()) {
                return;
            }
            if (Boolean.TRUE.equals(success)) {
                plugin.messages().send(player, "teleport.success", "label", label);
            } else {
                plugin.messages().send(player, "teleport.failed", "label", label);
            }
        });
    }

    private void abortMove(Player p, UUID playerId, String label, WrappedTask self, boolean moved) {
        cleanup(playerId);
        clearDisplays(p);
        plugin.messages().send(p, moved ? "teleport.cancelled-moved" : "teleport.cancelled", "label", label);
        if (soundsEnabled()) {
            play(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
        }
        if (self != null) self.cancel();
    }

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
            String shown = label != null ? label : plugin.messages().get("teleport.default-label");
            plugin.messages().send(player, "teleport.cancelled", "label", shown);
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

    public void handleDamage(Player player) {
        if (player == null || !plugin.getConfig().getBoolean("teleport.cancel-on-damage", false)) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!pendingTasks.containsKey(id)) {
            return;
        }
        String label = pendingLabels.getOrDefault(id, plugin.messages().get("teleport.default-label"));
        if (cancelTeleport(player, false) && player.isOnline()) {
            plugin.messages().send(player, "teleport.cancelled-damage", "label", label);
            clearDisplays(player);
            if (soundsEnabled()) {
                play(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
        }
    }

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

    private void clearDisplays(Player player) {
        sendActionBar(player, " ");
        try {
            player.resetTitle();
        } catch (Exception ignored) {
            // ignore
        }
    }
}
