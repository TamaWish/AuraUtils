package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Resolves per-player home limits from config and permissions.
 */
public interface HomeLimitPolicy {

    int getMaxHomes(Player player);

    final class AuraHomeLimitPolicy implements HomeLimitPolicy {

        private final AuraUtils plugin;

        public AuraHomeLimitPolicy(AuraUtils plugin) {
            this.plugin = plugin;
        }

        @Override
        public int getMaxHomes(Player player) {
            if (player.hasPermission("aura.admin")) {
                return -1;
            }

            int defaultLimit = plugin.getAuraConfig().homesDefaultLimit();
            int highestConfiguredLimit = defaultLimit;

            List<String> limitNodes = plugin.getConfig().getStringList("homes.permission-limits");
            if (limitNodes.isEmpty()) {
                return defaultLimit;
            }

            for (String limitNode : limitNodes) {
                int limit = parseHomeLimit(limitNode);
                if (limit < 0) {
                    continue;
                }
                if (player.hasPermission(limitNode)) {
                    highestConfiguredLimit = Math.max(highestConfiguredLimit, limit);
                }
            }
            return highestConfiguredLimit;
        }

        private static int parseHomeLimit(String permissionNode) {
            if (permissionNode == null) {
                return -1;
            }
            String trimmed = permissionNode.trim();
            int lastDot = trimmed.lastIndexOf('.');
            if (lastDot < 0 || lastDot == trimmed.length() - 1) {
                return -1;
            }
            try {
                return Integer.parseInt(trimmed.substring(lastDot + 1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
    }
}
