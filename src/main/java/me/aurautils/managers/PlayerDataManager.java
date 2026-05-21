package me.aurautils.managers;

import me.aurautils.AuraUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent store for all per-player toggles and settings.
 */
public class PlayerDataManager {

    private final AuraUtils plugin;
    private final File dataFile;

    private final Map<UUID, Boolean> godMode = new HashMap<>();
    private final Map<UUID, Boolean> flyMode = new HashMap<>();
    private final Map<UUID, Boolean> noFall = new HashMap<>();
    private final Map<UUID, Boolean> noHunger = new HashMap<>();
    private final Map<UUID, Double> dmgMult = new HashMap<>();

    public PlayerDataManager(AuraUtils plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player-states.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        godMode.clear();
        flyMode.clear();
        noFall.clear();
        noHunger.clear();
        dmgMult.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String playerKey : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.getConfigurationSection(playerKey);
            if (playerSection == null) {
                continue;
            }

            if (playerSection.contains("god")) {
                godMode.put(playerId, playerSection.getBoolean("god"));
            }
            if (playerSection.contains("fly")) {
                flyMode.put(playerId, playerSection.getBoolean("fly"));
            }
            if (playerSection.contains("nofall")) {
                noFall.put(playerId, playerSection.getBoolean("nofall"));
            }
            if (playerSection.contains("nohunger")) {
                noHunger.put(playerId, playerSection.getBoolean("nohunger"));
            }
            if (playerSection.contains("damage-multiplier")) {
                dmgMult.put(playerId, playerSection.getDouble("damage-multiplier", 1.0));
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection playersSection = config.createSection("players");
        Set<UUID> playerIds = new HashSet<>();
        playerIds.addAll(godMode.keySet());
        playerIds.addAll(flyMode.keySet());
        playerIds.addAll(noFall.keySet());
        playerIds.addAll(noHunger.keySet());
        playerIds.addAll(dmgMult.keySet());

        for (UUID playerId : playerIds) {
            boolean god = godMode.getOrDefault(playerId, false);
            boolean fly = flyMode.getOrDefault(playerId, false);
            boolean noFallEnabled = noFall.getOrDefault(playerId, false);
            boolean noHungerEnabled = noHunger.getOrDefault(playerId, false);
            double damageMultiplier = dmgMult.getOrDefault(playerId, 1.0);

            if (!god && !fly && !noFallEnabled && !noHungerEnabled && damageMultiplier == 1.0) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.createSection(playerId.toString());
            playerSection.set("god", god);
            playerSection.set("fly", fly);
            playerSection.set("nofall", noFallEnabled);
            playerSection.set("nohunger", noHungerEnabled);
            playerSection.set("damage-multiplier", damageMultiplier);
        }

        try {
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-states.yml: " + exception.getMessage());
        }
    }

    public boolean isGod(UUID id) {
        return godMode.getOrDefault(id, false);
    }

    public boolean toggleGod(UUID id) {
        boolean value = !isGod(id);
        setGod(id, value);
        return value;
    }

    public void setGod(UUID id, boolean value) {
        updateBoolean(godMode, id, value);
        save();
    }

    public boolean isFly(UUID id) {
        return flyMode.getOrDefault(id, false);
    }

    public boolean toggleFly(UUID id) {
        boolean value = !isFly(id);
        setFly(id, value);
        return value;
    }

    public void setFly(UUID id, boolean value) {
        updateBoolean(flyMode, id, value);
        save();
    }

    public boolean isNoFall(UUID id) {
        return noFall.getOrDefault(id, false);
    }

    public boolean toggleNoFall(UUID id) {
        boolean value = !isNoFall(id);
        setNoFall(id, value);
        return value;
    }

    public void setNoFall(UUID id, boolean value) {
        updateBoolean(noFall, id, value);
        save();
    }

    public boolean isNoHunger(UUID id) {
        return noHunger.getOrDefault(id, false);
    }

    public boolean toggleNoHunger(UUID id) {
        boolean value = !isNoHunger(id);
        setNoHunger(id, value);
        return value;
    }

    public void setNoHunger(UUID id, boolean value) {
        updateBoolean(noHunger, id, value);
        save();
    }

    public double getDamageMultiplier(UUID id) {
        return dmgMult.getOrDefault(id, plugin.getConfig().getDouble("damage-multiplier-default", 1.0));
    }

    public boolean hasCustomDamageMultiplier(UUID id) {
        return dmgMult.containsKey(id);
    }

    public void setDamageMultiplier(UUID id, double mult) {
        if (mult == 1.0) {
            dmgMult.remove(id);
        } else {
            dmgMult.put(id, mult);
        }
        save();
    }

    public void applyTo(Player player) {
        if (isFly(player.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.setAllowFlight(true), 1L);
        }
    }

    private void updateBoolean(Map<UUID, Boolean> state, UUID id, boolean value) {
        if (value) {
            state.put(id, true);
        } else {
            state.remove(id);
        }
    }
}
