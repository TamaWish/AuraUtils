package me.aurautils.util;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Lightweight vanish detection via Bukkit metadata keys (no hard dependency on vanish plugins).
 */
public final class VanishSupport {

    private final AuraUtils plugin;
    private boolean enabled;
    private String seePermission;
    private List<String> metadataKeys;

    public VanishSupport(AuraUtils plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        AuraConfig config = plugin.getAuraConfig();
        enabled = config.vanishEnabled();
        seePermission = config.vanishSeePermission();
        metadataKeys = config.vanishMetadataKeys();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVanished(Player player) {
        if (!enabled) {
            return false;
        }
        for (String key : metadataKeys) {
            if (player.hasMetadata(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean canSee(CommandSender viewer, Player target) {
        if (!enabled || !isVanished(target)) {
            return true;
        }
        if (viewer instanceof Player player && player.equals(target)) {
            return true;
        }
        return viewer.hasPermission(seePermission);
    }
}
