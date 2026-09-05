package com.lozaine.aurautils.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoredDestinationTest {

    @Test
    void yamlRoundTripKeepsWorldNameWithoutALoadedWorld() {
        UUID setter = UUID.fromString("11111111-1111-1111-1111-111111111111");
        StoredDestination original = new StoredDestination(
                "spawn",
                "Spawn",
                "world_nether",
                10.5,
                64.0,
                -3.25,
                90.0F,
                12.5F,
                setter,
                "Steve"
        );

        YamlConfiguration yaml = new YamlConfiguration();
        original.writeTo(yaml.createSection("warps.spawn"));

        StoredDestination loaded = StoredDestination.fromSection(
                yaml.getConfigurationSection("warps.spawn"),
                "spawn"
        );

        assertNotNull(loaded);
        assertEquals("spawn", loaded.getKey());
        assertEquals("Spawn", loaded.getDisplayName());
        assertEquals("world_nether", loaded.getWorldName());
        assertEquals(10.5, loaded.getX());
        assertEquals(64.0, loaded.getY());
        assertEquals(-3.25, loaded.getZ());
        assertEquals(90.0F, loaded.getYaw());
        assertEquals(12.5F, loaded.getPitch());
        assertEquals(setter, loaded.getSetBy());
        assertEquals("Steve", loaded.getSetByName());
        assertEquals("world_nether", yaml.getString("warps.spawn.world"));
    }

    @Test
    void fromSectionRejectsMissingWorldName() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("x", 1.0);
        yaml.set("y", 2.0);
        yaml.set("z", 3.0);

        assertNull(StoredDestination.fromSection(yaml, "home"));
    }
}
