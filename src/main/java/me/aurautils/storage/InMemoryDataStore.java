package me.aurautils.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory {@link DataStore} for unit tests. */
public final class InMemoryDataStore implements DataStore {

    private final Map<String, YamlConfiguration> files = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String fileName) {
        return files.containsKey(fileName);
    }

    @Override
    public YamlConfiguration load(String fileName) {
        YamlConfiguration stored = files.get(fileName);
        if (stored == null) {
            return new YamlConfiguration();
        }
        YamlConfiguration copy = new YamlConfiguration();
        copy.setDefaults(stored);
        copy.options().copyDefaults(true);
        for (String key : stored.getKeys(true)) {
            copy.set(key, stored.get(key));
        }
        return copy;
    }

    @Override
    public void save(String fileName, YamlConfiguration configuration) {
        YamlConfiguration stored = new YamlConfiguration();
        for (String key : configuration.getKeys(true)) {
            stored.set(key, configuration.get(key));
        }
        files.put(fileName, stored);
    }
}
