package me.aurautils.storage;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Persistence boundary for YAML data files under the plugin data folder.
 * Use {@link InMemoryDataStore} in unit tests.
 */
public interface DataStore {

    boolean exists(String fileName);

    YamlConfiguration load(String fileName);

    void save(String fileName, YamlConfiguration configuration);
}
