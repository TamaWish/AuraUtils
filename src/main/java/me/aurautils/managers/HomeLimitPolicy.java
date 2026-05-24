package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.util.HomeLimits;
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
                int limit = HomeLimits.parseLimit(limitNode);
                if (limit < 0) {
                    continue;
                }
                if (player.hasPermission(limitNode)) {
                    highestConfiguredLimit = Math.max(highestConfiguredLimit, limit);
                }
            }
            return highestConfiguredLimit;
        }
    }
}
