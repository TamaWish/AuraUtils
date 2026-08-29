package com.lozaine.aurautils.util;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {

    @Test
    void applyReplacesPlaceholders() {
        String result = MessageService.apply("&cPlayer &e%name% &cis not online.", "name", "Steve");
        assertEquals("&cPlayer &eSteve &cis not online.", result);
    }

    @Test
    void applyTreatsNullValueAsEmpty() {
        assertEquals("Hello .", MessageService.apply("Hello %name%.", "name", null));
    }

    @Test
    void missingKeyFallsBackToEnglishThenToKey() {
        YamlConfiguration lang = new YamlConfiguration();
        lang.set("tpa.self", "&cCustom self message.");

        YamlConfiguration fallback = new YamlConfiguration();
        fallback.set("tpa.self", "&cYou can't TPA to yourself.");
        fallback.set("common.no-permission", "&cNo permission.");

        MessageService messages = new MessageService(lang, fallback);

        assertEquals("&cCustom self message.", messages.get("tpa.self"));
        assertEquals("&cNo permission.", messages.get("common.no-permission"));
        assertEquals("missing.key", messages.get("missing.key"));
    }

    @Test
    void listAppliesPlaceholders() {
        YamlConfiguration lang = new YamlConfiguration();
        lang.set("aura.help", List.of("  &bAura&fUtils &7v%version%", "  &e/rtp"));

        MessageService messages = new MessageService(lang, new YamlConfiguration());
        List<String> lines = messages.getList("aura.help", "version", "1.3.0");

        assertEquals(2, lines.size());
        assertEquals("  &bAura&fUtils &7v1.3.0", lines.get(0));
        assertEquals("  &e/rtp", lines.get(1));
    }

    @Test
    void mergeMissingDoesNotOverwriteCustomValues() {
        YamlConfiguration target = new YamlConfiguration();
        target.set("common.no-permission", "custom");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("common.no-permission", "default");
        defaults.set("common.player-not-found", "not found");

        int added = MessageService.mergeMissing(target, defaults);

        assertEquals(1, added);
        assertEquals("custom", target.getString("common.no-permission"));
        assertEquals("not found", target.getString("common.player-not-found"));
    }

    @Test
    void sanitizeLanguageRejectsPathTraversal() {
        assertEquals("en", MessageService.sanitizeLanguage(null));
        assertEquals("en", MessageService.sanitizeLanguage("../etc"));
        assertEquals("en", MessageService.sanitizeLanguage("zh/cn"));
        assertEquals("zh_cn", MessageService.sanitizeLanguage("zh_cn"));
        assertEquals("en", MessageService.sanitizeLanguage("  en  "));
    }

    @Test
    void colorizeIsAppliedByCallerNotByGet() {
        YamlConfiguration fallback = new YamlConfiguration();
        fallback.set("common.no-permission", "&cNo permission.");
        MessageService messages = new MessageService(new YamlConfiguration(), fallback);

        String raw = messages.get("common.no-permission");
        assertEquals("&cNo permission.", raw);
        assertEquals("\u00A7cNo permission.", AuraUtils.colorize(raw));
    }

    @Test
    void shippedEnglishCatalogParsesAndHasCoreKeys() {
        InputStream stream = MessageService.class.getClassLoader().getResourceAsStream("lang/en.yml");
        assertTrue(stream != null, "lang/en.yml must be on the test classpath");
        YamlConfiguration catalog = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));

        assertEquals("&cNo permission.", catalog.getString("common.no-permission"));
        assertEquals("your destination", catalog.getString("teleport.default-label"));
        assertEquals("a random location", catalog.getString("rtp.label"));
        assertTrue(catalog.isList("aura.help"));
        assertTrue(catalog.getStringList("aura.help").size() > 10);
        assertTrue(catalog.getStringList("aura.help").stream().anyMatch(line -> line.contains("/delhome")));
        assertTrue(catalog.getStringList("aura.help").stream().anyMatch(line -> line.contains("/delwarp")));
        assertEquals("&8Aura &7| &bMenu", catalog.getString("menu.main-title"));
        assertEquals("&eA new AuraUtils release is available: &b%latest% &7(you have &f%current%&7).",
                catalog.getString("update.available"));
        assertTrue(catalog.isString("common.on"), "common.on must stay a string (YAML 1.1 boolean trap)");
        assertTrue(catalog.isString("common.off"), "common.off must stay a string (YAML 1.1 boolean trap)");
        assertEquals("&aON&r", catalog.getString("common.on"));
        assertEquals("&cOFF&r", catalog.getString("common.off"));
        assertEquals("&aENABLED&r", catalog.getString("common.enabled"));
        assertEquals("&cDISABLED&r", catalog.getString("common.disabled"));
        assertTrue(catalog.getString("aura.status-toggles").contains("&7Fly:"));
        assertTrue(catalog.getString("aura.status-protection").contains("&7NoHunger:"));
    }

    @Test
    void unquotedOnOffKeysAreSwallowedByYaml11() {
        YamlConfiguration broken = YamlConfiguration.loadConfiguration(new StringReader("""
                common:
                  on: "&aON"
                  off: "&cOFF"
                """));
        assertTrue(!broken.isString("common.on") || !"&aON".equals(broken.getString("common.on")));

        YamlConfiguration quoted = YamlConfiguration.loadConfiguration(new StringReader("""
                common:
                  "on": "&aON"
                  "off": "&cOFF"
                """));
        assertEquals("&aON", quoted.getString("common.on"));
        assertEquals("&cOFF", quoted.getString("common.off"));
    }

    @Test
    void onOffFallsBackWhenDiskLangLostTheYamlBooleanKeys() {
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(new StringReader("""
                common:
                  on: "&aON"
                  off: "&cOFF"
                """));
        YamlConfiguration fallback = YamlConfiguration.loadConfiguration(new StringReader("""
                common:
                  "on": "&aON"
                  "off": "&cOFF"
                """));
        MessageService messages = new MessageService(disk, fallback);
        assertEquals("&aON&r", messages.onOff(true));
        assertEquals("&cOFF&r", messages.onOff(false));
    }

    @Test
    void onOffAndStateAlwaysResetSoFollowingLabelsStayUncolored() {
        YamlConfiguration fallback = new YamlConfiguration();
        fallback.set("common.on", "&aON");
        fallback.set("common.off", "&cOFF");
        fallback.set("common.enabled", "&aENABLED");
        fallback.set("common.disabled", "&cDISABLED");
        fallback.set("aura.status-toggles", "  God: %god%  Fly: %fly%");
        fallback.set("aura.status-protection", "  NoFall: %nofall%  NoHunger: %nohunger%");
        MessageService messages = new MessageService(new YamlConfiguration(), fallback);

        assertEquals("&aON&r", messages.onOff(true));
        assertEquals("&cOFF&r", messages.onOff(false));
        assertEquals("&aENABLED&r", messages.state(true));
        assertEquals("&cDISABLED&r", messages.state(false));
        assertEquals("&aON&r", MessageService.withReset("&aON&r"));

        String toggles = messages.get("aura.status-toggles",
                "god", messages.onOff(true),
                "fly", messages.onOff(false));
        assertEquals("  God: &aON&r  Fly: &cOFF&r", toggles);

        String protection = messages.get("aura.status-protection",
                "nofall", messages.onOff(true),
                "nohunger", messages.onOff(false));
        assertEquals("  NoFall: &aON&r  NoHunger: &cOFF&r", protection);
    }
}
