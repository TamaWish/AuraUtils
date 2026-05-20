package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamicLightManager {

    private final AuraUtils plugin;
    private final Map<UUID, LightPlacement> placements = new HashMap<>();

    public DynamicLightManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(Player player) {
        UUID playerId = player.getUniqueId();
        if (isEnabled(playerId)) {
            clear(playerId);
            return false;
        }

        update(player);
        return true;
    }

    public boolean isEnabled(UUID playerId) {
        return placements.containsKey(playerId);
    }

    public void update(Player player) {
        UUID playerId = player.getUniqueId();
        Location target = player.getLocation().getBlock().getLocation();
        LightPlacement active = placements.get(playerId);

        if (active != null && sameBlock(active.location, target)) {
            return;
        }

        if (active != null) {
            restore(active);
        }

        Block block = target.getBlock();
        if (!isReplaceable(block.getType())) {
            return;
        }

        LightPlacement placement = new LightPlacement(target.clone(), block.getBlockData().clone());
        block.setBlockData(Bukkit.createBlockData(Material.LIGHT), false);
        placements.put(playerId, placement);
    }

    public void clear(UUID playerId) {
        LightPlacement placement = placements.remove(playerId);
        if (placement != null) {
            restore(placement);
        }
    }

    public void clearAll() {
        for (LightPlacement placement : placements.values()) {
            restore(placement);
        }
        placements.clear();
    }

    private void restore(LightPlacement placement) {
        Block block = placement.location.getBlock();
        if (block.getType() == Material.LIGHT) {
            block.setBlockData(placement.originalData, false);
        }
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private boolean isReplaceable(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private static final class LightPlacement {
        private final Location location;
        private final BlockData originalData;

        private LightPlacement(Location location, BlockData originalData) {
            this.location = location;
            this.originalData = originalData;
        }
    }
}