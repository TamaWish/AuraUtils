package me.aurautils.storage;

import me.aurautils.AuraUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class YamlDataStore implements DataStore {

    private final File dataFolder;
    private final Logger logger;

    public YamlDataStore(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public YamlDataStore(AuraUtils plugin) {
        this(plugin.getDataFolder(), plugin.getLogger());
    }

    @Override
    public boolean exists(String fileName) {
        return resolve(fileName).exists();
    }

    @Override
    public YamlConfiguration load(String fileName) {
        File file = resolve(fileName);
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void save(String fileName, YamlConfiguration configuration) {
        ensureDataFolder();
        File file = resolve(fileName);
        try {
            configuration.save(file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Failed to save " + fileName + ": " + exception.getMessage());
        }
    }

    private File resolve(String fileName) {
        return new File(dataFolder, fileName);
    }

    private void ensureDataFolder() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
}
