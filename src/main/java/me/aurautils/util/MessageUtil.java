package me.aurautils.util;

import me.aurautils.AuraUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

/**
 * Legacy helpers for menus and other APIs that still expect {@code §} strings.
 * Player-facing chat should use {@link me.aurautils.managers.MessagesManager} instead.
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private MessageUtil() {
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
        return LEGACY.serialize(MINI_MESSAGE.deserialize(miniMessage));
    }

    public static String prefix(String prefixRaw, String message) {
        return colorize(prefixRaw + message);
    }
}
