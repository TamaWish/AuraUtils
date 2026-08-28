package com.lozaine.aurautils.util;

import com.lozaine.aurautils.AuraUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Loads {@code lang/{language}.yml}, falls back to jar {@code lang/en.yml},
 * and resolves {@code %placeholder%} tokens. Color codes stay as {@code &}
 * until {@link AuraUtils#prefix(String)} or {@link AuraUtils#colorize(String)}.
 */
public final class MessageService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final Pattern SAFE_LANGUAGE = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final String JAR_ENGLISH = "lang/en.yml";

    private final AuraUtils plugin;
    private FileConfiguration lang = new YamlConfiguration();
    private FileConfiguration fallback = new YamlConfiguration();
    private String language = DEFAULT_LANGUAGE;

    public MessageService(AuraUtils plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Test constructor — no disk I/O. */
    public MessageService(FileConfiguration lang, FileConfiguration fallback) {
        this.plugin = null;
        this.lang = lang != null ? lang : new YamlConfiguration();
        this.fallback = fallback != null ? fallback : new YamlConfiguration();
        this.language = DEFAULT_LANGUAGE;
    }

    public void reload() {
        if (plugin == null) {
            return;
        }

        fallback = loadJarEnglish();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Could not create lang directory: " + langDir.getAbsolutePath());
        }

        File englishFile = new File(langDir, "en.yml");
        if (!englishFile.exists()) {
            plugin.saveResource(JAR_ENGLISH, false);
        }

        language = sanitizeLanguage(plugin.getConfig().getString("language", DEFAULT_LANGUAGE));
        File langFile = new File(langDir, language + ".yml");

        if (langFile.exists()) {
            lang = YamlConfiguration.loadConfiguration(langFile);
        } else {
            if (!DEFAULT_LANGUAGE.equals(language)) {
                plugin.getLogger().warning("Language file lang/" + language + ".yml not found; using English.");
            }
            lang = new YamlConfiguration();
        }

        int added = mergeMissing(lang, fallback);
        if (added > 0 && DEFAULT_LANGUAGE.equals(language) && englishFile.exists()) {
            try {
                lang.save(englishFile);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not write new message keys to lang/en.yml", exception);
            }
        }
    }

    public String language() {
        return language;
    }

    /**
     * Raw template with placeholders applied. Missing keys fall back to jar English,
     * then to the key itself.
     */
    public String get(String key, String... placeholders) {
        String template = firstString(key);
        if (template == null) {
            template = key;
        }
        return apply(template, placeholders);
    }

    public List<String> getList(String key, String... placeholders) {
        List<String> lines = firstList(key);
        if (lines.isEmpty()) {
            return List.of(get(key, placeholders));
        }
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(apply(line, placeholders));
        }
        return Collections.unmodifiableList(out);
    }

    public String colored(String key, String... placeholders) {
        return AuraUtils.colorize(get(key, placeholders));
    }

    public String state(boolean enabled) {
        return get(enabled ? "common.enabled" : "common.disabled");
    }

    public String onOff(boolean on) {
        return get(on ? "common.on" : "common.off");
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        if (plugin == null) {
            sender.sendMessage(get(key, placeholders));
            return;
        }
        sender.sendMessage(plugin.prefix(get(key, placeholders)));
    }

    public void sendPlain(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(AuraUtils.colorize(get(key, placeholders)));
    }

    public void sendLines(CommandSender sender, String key, String... placeholders) {
        for (String line : getList(key, placeholders)) {
            sender.sendMessage(AuraUtils.colorize(line));
        }
    }

    public static String apply(String template, String... placeholders) {
        if (template == null) {
            return "";
        }
        String result = template;
        if (placeholders == null) {
            return result;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String token = placeholders[i];
            String value = placeholders[i + 1];
            if (token == null) {
                continue;
            }
            result = result.replace("%" + token + "%", value != null ? value : "");
        }
        return result;
    }

    static int mergeMissing(FileConfiguration target, FileConfiguration defaults) {
        if (target == null || defaults == null) {
            return 0;
        }
        int added = 0;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    static String sanitizeLanguage(String raw) {
        if (raw == null) {
            return DEFAULT_LANGUAGE;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || !SAFE_LANGUAGE.matcher(trimmed).matches()) {
            return DEFAULT_LANGUAGE;
        }
        return trimmed;
    }

    private String firstString(String key) {
        String fromLang = stringFrom(lang, key);
        if (fromLang != null) {
            return fromLang;
        }
        return stringFrom(fallback, key);
    }

    private List<String> firstList(String key) {
        List<String> fromLang = listFrom(lang, key);
        if (!fromLang.isEmpty()) {
            return fromLang;
        }
        return listFrom(fallback, key);
    }

    private static String stringFrom(FileConfiguration config, String key) {
        if (config == null || key == null) {
            return null;
        }
        if (config.isString(key)) {
            return config.getString(key);
        }
        return null;
    }

    private static List<String> listFrom(FileConfiguration config, String key) {
        if (config == null || key == null || !config.isList(key)) {
            return List.of();
        }
        List<String> list = config.getStringList(key);
        return list == null ? List.of() : list;
    }

    private FileConfiguration loadJarEnglish() {
        InputStream stream = plugin.getResource(JAR_ENGLISH);
        if (stream == null) {
            plugin.getLogger().severe("Missing jar resource " + JAR_ENGLISH);
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + JAR_ENGLISH, exception);
            return new YamlConfiguration();
        }
    }
}
