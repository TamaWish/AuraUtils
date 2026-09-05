package com.lozaine.aurautils.managers;

import com.lozaine.aurautils.AuraUtils;
import com.lozaine.aurautils.menus.PlayerInventoryHolder;
import com.lozaine.aurautils.util.InventoryLimits;
import com.lozaine.aurautils.util.PermissionLimits;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persistent extra inventories ({@code /inv 1}, {@code /inv 2}, …).
 * Contents live in {@code plugins/AuraUtils/inventories/<uuid>.yml}.
 * Always stores up to 54 slots so shrinking {@code inventories.rows} does not drop items.
 */
public class PlayerInventoryManager {

    public static final int STORAGE_SIZE = 54;

    private final AuraUtils plugin;
    private final File folder;
    private final Map<UUID, Map<Integer, ItemStack[]>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private volatile boolean shuttingDown;

    public PlayerInventoryManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "inventories");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("inventories.enabled", true);
    }

    public int rows() {
        return Math.max(1, Math.min(6, plugin.getConfig().getInt("inventories.rows", 6)));
    }

    public int viewSize() {
        return rows() * 9;
    }

    public int maxInventories() {
        return InventoryLimits.clampMax(plugin.getConfig().getInt("inventories.max", InventoryLimits.DEFAULT_MAX));
    }

    /**
     * Registers {@code aura.inv.1} … {@code aura.inv.<max>} with the server.
     * Permission plugins only expand wildcards over <em>registered</em> nodes, so
     * without this {@code aura.inv.*} grants nothing and the nodes never tab-complete.
     * Safe to call again after a reload; nodes above {@code max} are dropped.
     */
    public void registerNumberedPermissions() {
        PluginManager manager = plugin.getServer().getPluginManager();
        int max = maxInventories();
        for (int number = InventoryLimits.MIN_INVENTORIES; number <= InventoryLimits.MAX_INVENTORIES; number++) {
            String node = InventoryLimits.numberedNode(number);
            Permission registered = manager.getPermission(node);
            if (number <= max) {
                if (registered == null) {
                    manager.addPermission(new Permission(node,
                            "Open extra inventories 1 through " + number,
                            PermissionDefault.FALSE));
                }
            } else if (registered != null) {
                manager.removePermission(registered);
            }
        }
    }

    public int resolveLimit(Player player) {
        if (player == null || !isEnabled()) {
            return 0;
        }
        return InventoryLimits.resolve(
                plugin.getConfig().getInt("inventories.default-limit", InventoryLimits.DEFAULT_LIMIT),
                maxInventories(),
                PermissionLimits.readEntries(plugin.getConfig(), "inventories.limits"),
                player::hasPermission);
    }

    public boolean canOpen(Player player, int number) {
        return InventoryLimits.canOpen(number, resolveLimit(player));
    }

    public void open(Player player, int number) {
        if (player == null || !player.isOnline() || shuttingDown) {
            return;
        }
        int size = viewSize();
        ItemStack[] stored = contents(player.getUniqueId(), number);
        PlayerInventoryHolder holder = new PlayerInventoryHolder(player.getUniqueId(), number);
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.messages().get("inventories.title", "number", String.valueOf(number)));
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);
        for (int slot = 0; slot < size && slot < stored.length; slot++) {
            ItemStack item = stored[slot];
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(slot, item.clone());
            }
        }
        player.openInventory(inventory);
    }

    /**
     * Copy the open view back into the 54-slot store and mark dirty.
     */
    public void capture(PlayerInventoryHolder holder, Inventory inventory) {
        if (holder == null || inventory == null || holder.getOwner() == null) {
            return;
        }
        ItemStack[] stored = contents(holder.getOwner(), holder.getNumber());
        ItemStack[] view = inventory.getContents();
        int size = Math.min(view.length, stored.length);
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = view[slot];
            stored[slot] = (item == null || item.getType() == Material.AIR) ? null : item.clone();
        }
        playerVaults(holder.getOwner()).put(holder.getNumber(), stored);
        dirty.add(holder.getOwner());
        rememberName(holder.getOwner());
    }

    public void captureAndSave(PlayerInventoryHolder holder, Inventory inventory) {
        capture(holder, inventory);
        save(holder.getOwner());
    }

    public void captureIfOpen(Player player) {
        if (player == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof PlayerInventoryHolder holder
                && holder.getOwner().equals(player.getUniqueId())) {
            capture(holder, top);
        }
    }

    public void save(UUID playerId) {
        if (playerId == null || !dirty.contains(playerId)) {
            return;
        }
        flushPlayer(playerId);
    }

    public void prepareShutdown() {
        shuttingDown = true;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            captureIfOpen(player);
        }
        for (UUID playerId : Set.copyOf(dirty)) {
            flushPlayer(playerId);
        }
    }

    private void flushPlayer(UUID playerId) {
        if (!dirty.remove(playerId)) {
            return;
        }
        folder.mkdirs();
        File file = new File(folder, playerId + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        String name = names.get(playerId);
        if (name != null && !name.isBlank()) {
            yaml.set("name", name);
        }
        Map<Integer, ItemStack[]> vaults = playerVaults(playerId);
        for (Map.Entry<Integer, ItemStack[]> entry : vaults.entrySet()) {
            ItemStack[] stored = entry.getValue();
            if (stored == null) {
                continue;
            }
            String path = "inventories." + entry.getKey();
            boolean any = false;
            for (int slot = 0; slot < stored.length; slot++) {
                ItemStack item = stored[slot];
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                yaml.set(path + "." + slot, item);
                any = true;
            }
            if (!any) {
                yaml.set(path, null);
            }
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            dirty.add(playerId);
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to save inventory for " + playerId + ": " + exception.getMessage(), exception);
        }
    }

    private ItemStack[] contents(UUID playerId, int number) {
        return playerVaults(playerId).computeIfAbsent(number, key -> new ItemStack[STORAGE_SIZE]);
    }

    private Map<Integer, ItemStack[]> playerVaults(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::loadAllFromDisk);
    }

    private Map<Integer, ItemStack[]> loadAllFromDisk(UUID playerId) {
        Map<Integer, ItemStack[]> vaults = new ConcurrentHashMap<>();
        File file = new File(folder, playerId + ".yml");
        if (!file.isFile()) {
            return vaults;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String storedName = yaml.getString("name");
        if (storedName != null && !storedName.isBlank()) {
            names.put(playerId, storedName);
        }
        ConfigurationSection root = yaml.getConfigurationSection("inventories");
        if (root == null) {
            return vaults;
        }
        for (String key : root.getKeys(false)) {
            int number;
            try {
                number = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (number < InventoryLimits.MIN_INVENTORIES || number > InventoryLimits.MAX_INVENTORIES) {
                continue;
            }
            ItemStack[] stored = new ItemStack[STORAGE_SIZE];
            ConfigurationSection slots = root.getConfigurationSection(key);
            if (slots != null) {
                for (String slotKey : slots.getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotKey);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                    if (slot < 0 || slot >= STORAGE_SIZE) {
                        continue;
                    }
                    ItemStack item = slots.getItemStack(slotKey);
                    if (item != null && item.getType() != Material.AIR) {
                        stored[slot] = item.clone();
                    }
                }
            }
            vaults.put(number, stored);
        }
        return vaults;
    }

    private void rememberName(UUID playerId) {
        Player online = plugin.getServer().getPlayer(playerId);
        if (online != null) {
            names.put(playerId, online.getName());
            if (plugin.getPlayerDataManager() != null) {
                plugin.getPlayerDataManager().rememberName(playerId, online.getName());
            }
        }
    }
}
