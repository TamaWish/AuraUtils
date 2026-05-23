package me.aurautils.storage;

import me.aurautils.AuraUtils;
import org.bukkit.World;

public final class BukkitWorldResolver implements WorldResolver {

    private final AuraUtils plugin;

    public BukkitWorldResolver(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public World getWorld(String name) {
        return name == null ? null : plugin.getServer().getWorld(name);
    }
}
