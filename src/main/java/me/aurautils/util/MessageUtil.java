package me.aurautils.util;

import me.aurautils.AuraUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

/**
 * Formatting bridge for GUI code paths that still use Bukkit's legacy {@code String} item APIs.
 * <p>
 * Player-facing chat should use {@link me.aurautils.managers.MessagesManager} (MiniMessage end-to-end).
 * Menu inventory titles go through {@link #toComponent(String)} and
 * {@link me.aurautils.platform.PlatformAdapter#createInventory}; on Paper that uses native Adventure
 * titles (gradients/RGB work), on Spigot the title is flattened to legacy section codes (gradients are lost).
 * Item display names and lore in {@link me.aurautils.menus.MenuManager} still call {@link #colorize(String)}
 * and remain gradient-free on all platforms.
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (text.indexOf('<') >= 0) {
            return MINI_MESSAGE.deserialize(text);
        }
        return LEGACY_AMPERSAND.deserialize(text);
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.indexOf('<') >= 0) {
            return fromMiniMessage(text);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String fromMiniMessage(String miniMessage) {
        AuraUtils plugin = AuraUtils.getInstance();
        if (plugin != null && plugin.getMessages() != null) {
            return plugin.getMessages().toLegacyString(MINI_MESSAGE.deserialize(miniMessage));
        }
        return LEGACY_SECTION.serialize(MINI_MESSAGE.deserialize(miniMessage));
    }

    public static String prefix(String prefixRaw, String message) {
        return colorize(prefixRaw + message);
    }
}
