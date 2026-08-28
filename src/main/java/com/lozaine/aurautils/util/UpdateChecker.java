package com.lozaine.aurautils.util;

import com.lozaine.aurautils.AuraUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional GitHub release check. HTTP runs off the tick thread; player notices
 * hop to the entity scheduler so they stay legal on Folia.
 */
public final class UpdateChecker {

    public static final String GITHUB_PAGE = "https://github.com/TamaWish/AuraUtils";
    public static final String RELEASES_PAGE = "https://github.com/TamaWish/AuraUtils/releases";
    private static final String API_LATEST =
            "https://api.github.com/repos/TamaWish/AuraUtils/releases/latest";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SAFE_URL = Pattern.compile("^https://github\\.com/TamaWish/AuraUtils(/.*)?$");

    private final AuraUtils plugin;
    private volatile String latestTag;
    private volatile String latestUrl = RELEASES_PAGE;
    private volatile boolean updateAvailable;

    public UpdateChecker(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("update-checker.enabled", true);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String latestTag() {
        return latestTag;
    }

    public String latestUrl() {
        return latestUrl != null ? latestUrl : RELEASES_PAGE;
    }

    public void checkAsync() {
        if (!isEnabled()) {
            updateAvailable = false;
            return;
        }
        plugin.getScheduler().runAsync(this::fetchLatest);
    }

    public void notifyPlayerIfNeeded(Player player) {
        if (player == null || !player.isOnline() || !isEnabled() || !updateAvailable) {
            return;
        }
        if (!player.hasPermission("aura.admin")) {
            return;
        }
        plugin.getScheduler().runAtEntityLater(player, () -> sendNotice(player), 40L);
    }

    public void notifyOnlineAdmins() {
        if (!plugin.isEnabled() || !isEnabled() || !updateAvailable) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("aura.admin")) {
                plugin.getScheduler().runAtEntity(player, () -> sendNotice(player));
            }
        }
    }

    static int compareVersions(String current, String remote) {
        int[] left = versionParts(current);
        int[] right = versionParts(remote);
        int n = Math.max(left.length, right.length);
        for (int i = 0; i < n; i++) {
            int l = i < left.length ? left[i] : 0;
            int r = i < right.length ? right[i] : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static int[] versionParts(String raw) {
        if (raw == null || raw.isBlank()) {
            return new int[0];
        }
        String value = raw.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        int dash = value.indexOf('-');
        if (dash >= 0) {
            value = value.substring(0, dash);
        }
        String[] bits = value.split("\\.");
        int[] parts = new int[bits.length];
        for (int i = 0; i < bits.length; i++) {
            String digits = bits[i].replaceAll("[^0-9].*$", "");
            if (digits.isEmpty()) {
                parts[i] = 0;
                continue;
            }
            try {
                parts[i] = Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    static String firstJsonString(String json, Pattern pattern) {
        if (json == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void fetchLatest() {
        if (!plugin.isEnabled()) {
            return;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_LATEST))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "AuraUtils")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                plugin.getLogger().fine("GitHub update check returned HTTP " + response.statusCode());
                return;
            }
            applyLatest(response.body());
        } catch (Exception exception) {
            if (plugin.isEnabled()) {
                plugin.getLogger().log(Level.FINE, "GitHub update check failed", exception);
            }
        }
    }

    private void applyLatest(String body) {
        String tag = firstJsonString(body, TAG_NAME);
        if (tag == null || tag.isBlank()) {
            return;
        }
        String current = plugin.getDescription().getVersion();
        boolean newer = compareVersions(current, tag) < 0;
        latestTag = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        String url = firstJsonString(body, HTML_URL);
        latestUrl = (url != null && SAFE_URL.matcher(url).matches()) ? url : RELEASES_PAGE;
        updateAvailable = newer;
        if (!newer || !plugin.isEnabled()) {
            return;
        }
        plugin.getLogger().info("A new AuraUtils release is available: " + latestTag
                + " (running " + current + "). " + latestUrl());
        plugin.getScheduler().runNextTick(this::notifyOnlineAdmins);
    }

    private void sendNotice(Player player) {
        if (!plugin.isEnabled() || !player.isOnline() || !isEnabled() || !updateAvailable) {
            return;
        }
        var msg = plugin.messages();
        String current = plugin.getDescription().getVersion();
        String latest = latestTag != null ? latestTag : "";
        String url = latestUrl();
        player.sendMessage(plugin.prefix(msg.get("update.available",
                "latest", latest, "current", current)));

        TextComponent prefix = new TextComponent(plugin.prefix(""));
        TextComponent click = new TextComponent(AuraUtils.colorize(msg.get("update.click")));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        click.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(AuraUtils.colorize(msg.get("update.hover", "url", GITHUB_PAGE)))));
        player.spigot().sendMessage(prefix, click);
    }
}
