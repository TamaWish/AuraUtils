package com.lozaine.aurautils.listeners;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.util.TimberTrees;
import com.lozaine.aurautils.util.TimberTrees.BlockPos;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * When timber is enabled, breaking one log with an axe fells the connected tree.
 * Extra logs go through {@link Player#breakBlock(Block)} so protection plugins,
 * drops, and durability still apply. Leaves are broken without extra axe wear.
 */
public class TimberListener implements Listener {

    private final AuraUtils plugin;
    private final Set<UUID> felling = ConcurrentHashMap.newKeySet();

    public TimberListener(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (felling.contains(player.getUniqueId())) {
            return;
        }
        if (TimberTrees.logSpecies(event.getBlock().getType()) == null) {
            return;
        }
        if (!felling.add(player.getUniqueId())) {
            return;
        }
        try {
            fellConnectedTree(event.getBlock(), player);
        } finally {
            felling.remove(player.getUniqueId());
        }
    }

    private void fellConnectedTree(Block originBlock, Player player) {
        if (!plugin.getConfig().getBoolean("timber.enabled", true)) {
            return;
        }
        if (!player.hasPermission("aura.timber")) {
            return;
        }
        if (!plugin.getPlayerDataManager().isTimber(player.getUniqueId())) {
            return;
        }
        if (plugin.getConfig().getBoolean("timber.sneak-chops-single", true) && player.isSneaking()) {
            return;
        }

        Material originType = originBlock.getType();
        String species = TimberTrees.logSpecies(originType);
        if (species == null) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (plugin.getConfig().getBoolean("timber.require-axe", true)
                && !TimberTrees.isAxe(tool.getType())) {
            return;
        }

        World world = originBlock.getWorld();
        Function<BlockPos, Material> typeAt = pos -> typeAt(world, pos);
        BlockPos origin = new BlockPos(originBlock.getX(), originBlock.getY(), originBlock.getZ());
        int maxLogs = Math.max(1, plugin.getConfig().getInt("timber.max-logs", 128));

        List<BlockPos> logs = TimberTrees.collectLogs(origin, species, maxLogs, typeAt);
        if (!TimberTrees.hasMatchingLeaf(logs, species, typeAt)) {
            return;
        }

        List<BlockPos> extraLogs = new ArrayList<>(logs.size());
        for (BlockPos pos : logs) {
            if (!pos.equals(origin)) {
                extraLogs.add(pos);
            }
        }
        extraLogs.sort(Comparator.comparingInt(BlockPos::y).reversed());

        boolean requireAxe = plugin.getConfig().getBoolean("timber.require-axe", true);
        for (BlockPos pos : extraLogs) {
            if (requireAxe && !TimberTrees.isAxe(player.getInventory().getItemInMainHand().getType())) {
                break;
            }
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (!species.equals(TimberTrees.logSpecies(block.getType()))) {
                continue;
            }
            player.breakBlock(block);
        }

        if (!plugin.getConfig().getBoolean("timber.break-leaves", true)) {
            return;
        }

        int maxLeaves = Math.max(0, plugin.getConfig().getInt("timber.max-leaves", 256));
        List<BlockPos> leaves = TimberTrees.collectLeaves(logs, species, maxLeaves, typeAt);
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        for (BlockPos pos : leaves) {
            Block leaf = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (!TimberTrees.isMatchingLeaf(leaf.getType(), species)) {
                continue;
            }
            BlockBreakEvent leafEvent = new BlockBreakEvent(leaf, player);
            plugin.getServer().getPluginManager().callEvent(leafEvent);
            if (leafEvent.isCancelled()) {
                continue;
            }
            if (creative) {
                leaf.setType(Material.AIR);
            } else {
                leaf.breakNaturally();
            }
        }
    }

    private static Material typeAt(World world, BlockPos pos) {
        if (pos.y() < world.getMinHeight() || pos.y() >= world.getMaxHeight()) {
            return Material.AIR;
        }
        return world.getBlockAt(pos.x(), pos.y(), pos.z()).getType();
    }
}
