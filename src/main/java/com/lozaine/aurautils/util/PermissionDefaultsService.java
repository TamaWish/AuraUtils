package com.lozaine.aurautils.util;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Loads {@code plugins/AuraUtils/permissions.yml} and applies {@code defaults:}
 * onto the nodes registered from plugin.yml. LuckPerms still overrides per player.
 */
public final class PermissionDefaultsService {

    private final AuraUtils plugin;
    private final File file;

    public PermissionDefaultsService(AuraUtils plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), PermissionDefaults.FILE_NAME);
    }

    public void loadAndApply() {
        plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            plugin.saveResource(PermissionDefaults.FILE_NAME, false);
        }

        YamlConfiguration disk = PermissionDefaults.loadLiteral(file);
        YamlConfiguration jar = loadJar();
        ConfigurationSection diskSection = disk.getConfigurationSection(PermissionDefaults.SECTION);
        ConfigurationSection jarSection = jar.getConfigurationSection(PermissionDefaults.SECTION);

        Set<String> known = knownNodes();
        for (String unknown : PermissionDefaults.unknownKeys(diskSection, known)) {
            plugin.getLogger().warning("permissions.yml ignores unknown node: " + unknown);
        }

        Map<String, PermissionDefault> resolved = PermissionDefaults.resolve(diskSection, known, jarSection);
        apply(resolved);
    }

    private void apply(Map<String, PermissionDefault> resolved) {
        PluginManager manager = plugin.getServer().getPluginManager();
        int changed = 0;
        for (Map.Entry<String, PermissionDefault> entry : resolved.entrySet()) {
            Permission permission = manager.getPermission(entry.getKey());
            if (permission == null) {
                continue;
            }
            if (permission.getDefault() == entry.getValue()) {
                continue;
            }
            permission.setDefault(entry.getValue());
            manager.recalculatePermissionDefaults(permission);
            changed++;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getScheduler().runAtEntity(player, player::recalculatePermissions);
        }

        if (changed > 0) {
            plugin.getLogger().info("Applied " + changed + " permission default(s) from permissions.yml");
        }
    }

    private Set<String> knownNodes() {
        Set<String> nodes = new LinkedHashSet<>();
        for (Permission permission : plugin.getDescription().getPermissions()) {
            nodes.add(permission.getName());
        }
        return nodes;
    }

    private YamlConfiguration loadJar() {
        InputStream stream = plugin.getResource(PermissionDefaults.FILE_NAME);
        if (stream == null) {
            plugin.getLogger().severe("Missing jar resource " + PermissionDefaults.FILE_NAME);
            return PermissionDefaults.emptyLiteral();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return PermissionDefaults.loadLiteral(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + PermissionDefaults.FILE_NAME, exception);
            return PermissionDefaults.emptyLiteral();
        }
    }
}
