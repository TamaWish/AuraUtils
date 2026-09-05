package com.lozaine.aurautils.economy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyCostsTest {

    @Test
    void missingSectionIsFree() {
        YamlConfiguration config = new YamlConfiguration();
        assertEquals(0, EconomyCosts.cost(config, EconomyAction.RTP));
        assertTrue(EconomyCosts.isFree(config, EconomyAction.RTP, node -> false));
    }

    @Test
    void disabledEconomyIsFreeEvenWithPositiveCosts() {
        YamlConfiguration config = configWithCosts(50);
        config.set("economy.enabled", false);
        assertEquals(0, EconomyCosts.cost(config, EconomyAction.HOME));
        assertTrue(EconomyCosts.isFree(config, EconomyAction.HOME, node -> false));
    }

    @Test
    void zeroAndNegativeCostsAreFree() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("economy.enabled", true);
        config.set("economy.costs.home", 0);
        config.set("economy.costs.warp", -10);
        assertEquals(0, EconomyCosts.cost(config, EconomyAction.HOME));
        assertEquals(0, EconomyCosts.cost(config, EconomyAction.WARP));
    }

    @Test
    void positiveCostAppliesUnlessBypassPermission() {
        YamlConfiguration config = configWithCosts(25);
        assertEquals(25, EconomyCosts.cost(config, EconomyAction.RTP));
        assertFalse(EconomyCosts.isFree(config, EconomyAction.RTP, node -> false));
        assertTrue(EconomyCosts.isFree(config, EconomyAction.RTP, EconomyCosts.BYPASS_PERMISSION::equals));
    }

    @Test
    void eachActionUsesItsOwnKey() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("economy.enabled", true);
        config.set("economy.costs.home", 1);
        config.set("economy.costs.sethome", 2);
        config.set("economy.costs.warp", 3);
        config.set("economy.costs.setwarp", 4);
        config.set("economy.costs.tpa", 5);
        config.set("economy.costs.rtp", 6);
        config.set("economy.costs.back", 7);

        assertEquals(1, EconomyCosts.cost(config, EconomyAction.HOME));
        assertEquals(2, EconomyCosts.cost(config, EconomyAction.SET_HOME));
        assertEquals(3, EconomyCosts.cost(config, EconomyAction.WARP));
        assertEquals(4, EconomyCosts.cost(config, EconomyAction.SET_WARP));
        assertEquals(5, EconomyCosts.cost(config, EconomyAction.TPA));
        assertEquals(6, EconomyCosts.cost(config, EconomyAction.RTP));
        assertEquals(7, EconomyCosts.cost(config, EconomyAction.BACK));
    }

    @Test
    void notifyDefaultsTrue() {
        assertTrue(EconomyCosts.notify(null));
        YamlConfiguration config = new YamlConfiguration();
        assertTrue(EconomyCosts.notify(config));
        config.set("economy.notify", false);
        assertFalse(EconomyCosts.notify(config));
    }

    @Test
    void bypassPermissionMatchesPluginYml() {
        assertEquals("aura.economy.bypass", EconomyCosts.BYPASS_PERMISSION);
        assertTrue(Set.of(EconomyAction.values()).contains(EconomyAction.TPA));
    }

    @Test
    void everyActionHasAConfigKey() {
        assertEquals("home", EconomyAction.HOME.configKey());
        assertEquals("sethome", EconomyAction.SET_HOME.configKey());
        assertEquals("warp", EconomyAction.WARP.configKey());
        assertEquals("setwarp", EconomyAction.SET_WARP.configKey());
        assertEquals("tpa", EconomyAction.TPA.configKey());
        assertEquals("rtp", EconomyAction.RTP.configKey());
        assertEquals("back", EconomyAction.BACK.configKey());
    }

    private static YamlConfiguration configWithCosts(double rtp) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("economy.enabled", true);
        config.set("economy.costs.home", rtp);
        config.set("economy.costs.rtp", rtp);
        return config;
    }
}
