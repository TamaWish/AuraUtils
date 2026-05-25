package me.aurautils.managers;

import me.aurautils.AuraUtils;
import me.aurautils.config.AuraConfig;
import me.aurautils.util.MessagePlaceholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads per-locale {@code messages/<locale>.yml} bundles with MiniMessage syntax,
 * resolves the best locale per player, and delivers components (with click/hover on Paper).
 */
public class MessagesManager {

    private static final String[] BUNDLED_LOCALES = {"en", "es"};

    /** Always available even if YAML / JAR resources fail to load. */
    private static final Map<String, String> BUILTIN_TOGGLE = Map.of(
            "toggle.on", "<green>enabled",
            "toggle.off", "<red>disabled",
            "toggle.enabled", "<green>enabled",
            "toggle.disabled", "<red>disabled"
    );

    private final AuraUtils plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();

    private final Map<String, Map<String, String>> bundles = new HashMap<>();
    private final Map<String, String> metaPrefixByLocale = new HashMap<>();
    private String defaultLocale = "en";
    private String fallbackLocale = "en";
    private boolean useClientLocale = true;
    private String prefixTemplate = "";
    private TagResolver prefixResolver = TagResolver.empty();

    public MessagesManager(AuraUtils plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration raw) {
        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }

        for (String locale : BUNDLED_LOCALES) {
            File localeFile = new File(messagesDir, locale + ".yml");
            if (!localeFile.exists()) {
                plugin.saveResource("messages/" + locale + ".yml", false);
            }
        }

        bundles.clear();
        metaPrefixByLocale.clear();
        File[] files = messagesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String locale = file.getName().replace(".yml", "").toLowerCase(Locale.ROOT);
                loadBundle(locale, YamlConfiguration.loadConfiguration(file));
            }
        }

        for (String locale : BUNDLED_LOCALES) {
            if (!bundles.containsKey(locale)) {
                loadBundle(locale, null);
            }
        }

        defaultLocale = normalizeLocale(raw.getString("messages.default-locale", "en"));
        fallbackLocale = normalizeLocale(raw.getString("messages.fallback-locale", "en"));
        useClientLocale = raw.getBoolean("messages.use-client-locale", true);
        reconcileLocaleSettings();

        refreshPrefix();

        plugin.getLogger().info("Loaded message locales: " + String.join(", ", bundles.keySet())
                + " (default=" + defaultLocale + ", fallback=" + fallbackLocale + ")");
    }

    public void applyLocaleConfig(AuraConfig auraConfig) {
        defaultLocale = auraConfig.messagesDefaultLocale();
        fallbackLocale = auraConfig.messagesFallbackLocale();
        useClientLocale = auraConfig.messagesUseClientLocale();
        reconcileLocaleSettings();
        refreshPrefix();
    }

    private void reconcileLocaleSettings() {
        if (!bundles.containsKey(defaultLocale)) {
            defaultLocale = "en";
        }
        if (!bundles.containsKey(fallbackLocale)) {
            fallbackLocale = defaultLocale;
        }
    }

    private void refreshPrefix() {
        prefixTemplate = metaPrefixByLocale.get(defaultLocale);
        if (prefixTemplate == null || prefixTemplate.isEmpty()) {
            prefixTemplate = "<dark_gray>[<aqua>Aura</aqua>]</dark_gray> ";
        }
        prefixResolver = Placeholder.component("prefix", miniMessage.deserialize(prefixTemplate));
    }

    /**
     * Loads a locale: bundled defaults from the JAR first, then optional on-disk overrides.
     * Prevents stale {@code plugins/AuraUtils/messages/*.yml} from missing keys added in updates.
     */
    private void loadBundle(String locale, YamlConfiguration diskOverlay) {
        Map<String, String> flat = new HashMap<>();
        Map<String, String> jarFlat = new HashMap<>();
        String metaPrefix = null;
        YamlConfiguration bundled = readBundledConfig(locale);
        if (bundled != null) {
            flattenConfig(bundled, jarFlat);
            flat.putAll(jarFlat);
            metaPrefix = readMetaPrefix(bundled);
        }
        if (diskOverlay != null) {
            flattenConfig(diskOverlay, flat);
            String diskPrefix = readMetaPrefix(diskOverlay);
            if (diskPrefix != null && !diskPrefix.isEmpty()) {
                metaPrefix = diskPrefix;
            }
        }
        sanitizeStaleToggleTags(flat, jarFlat);
        for (Map.Entry<String, String> builtin : BUILTIN_TOGGLE.entrySet()) {
            flat.putIfAbsent(builtin.getKey(), builtin.getValue());
        }
        if (!flat.isEmpty()) {
            bundles.put(locale, flat);
        }
        if (metaPrefix != null && !metaPrefix.isEmpty()) {
            metaPrefixByLocale.put(locale, metaPrefix);
        }
    }

    /**
     * Older message files used {@code <toggle.on>} as a MiniMessage tag; unknown tags render as {@code [toggle.on]}.
     */
    private void sanitizeStaleToggleTags(Map<String, String> flat, Map<String, String> jarFlat) {
        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String value = entry.getValue();
            if (value == null || (!value.contains("<toggle.on>") && !value.contains("<toggle.off>"))) {
                continue;
            }
            String jarValue = jarFlat.get(entry.getKey());
            if (jarValue != null && !jarValue.equals(value)) {
                flat.put(entry.getKey(), jarValue);
                plugin.getLogger().warning(
                        "Updated stale MiniMessage tag in messages key '" + entry.getKey()
                                + "' — use <state>, <god>, <fly>, etc. instead of <toggle.on>.");
            }
        }
    }

    private YamlConfiguration readBundledConfig(String locale) {
        String resourcePath = "messages/" + locale + ".yml";
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled messages/" + locale + ".yml", exception);
            return null;
        }
    }

    private void flattenConfig(YamlConfiguration config, Map<String, String> flat) {
        for (String topKey : config.getKeys(false)) {
            if ("meta".equals(topKey)) {
                continue;
            }
            Object value = config.get(topKey);
            if (value instanceof ConfigurationSection section) {
                flattenKeys(topKey, section, flat);
            }
        }
    }

    private String readMetaPrefix(YamlConfiguration config) {
        return config.getString("meta.prefix", "");
    }

    private void flattenKeys(String prefix, ConfigurationSection section, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String path = prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                flattenKeys(path, section.getConfigurationSection(key), out);
            } else {
                String text = section.getString(key);
                if (text != null) {
                    out.put(path, text);
                }
            }
        }
    }

    public String resolveLocale(Player player) {
        String override = plugin.getPlayerDataManager().getLocaleOverride(player.getUniqueId());
        if (override != null && bundles.containsKey(override)) {
            return override;
        }
        if (useClientLocale) {
            String client = resolveClientLocale(player);
            if (client != null && bundles.containsKey(client)) {
                return client;
            }
        }
        return defaultLocale;
    }

    public String resolveLocale(CommandSender sender) {
        if (sender instanceof Player player) {
            return resolveLocale(player);
        }
        return defaultLocale;
    }

    private String resolveClientLocale(Player player) {
        try {
            Method localeMethod = player.getClass().getMethod("locale");
            Object result = localeMethod.invoke(player);
            if (result instanceof Locale locale) {
                return normalizeLocale(locale.toString());
            }
            if (result instanceof String localeString) {
                return normalizeLocale(localeString);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    public static String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            return "en";
        }
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        int dash = cleaned.indexOf('-');
        if (dash > 0) {
            cleaned = cleaned.substring(0, dash);
        }
        return cleaned;
    }

    public String raw(String key, CommandSender sender) {
        return resolveRaw(key, resolveLocale(sender));
    }

    public String raw(String key, String locale) {
        return resolveRaw(key, locale);
    }

    private String resolveRaw(String key, String locale) {
        String resolved = lookupRaw(key, locale);
        if (resolved != null) {
            return resolved;
        }
        if (!locale.equals(fallbackLocale)) {
            resolved = lookupRaw(key, fallbackLocale);
            if (resolved != null) {
                return resolved;
            }
        }
        String builtin = BUILTIN_TOGGLE.get(key);
        if (builtin != null) {
            return builtin;
        }
        plugin.getLogger().warning("Missing message key: " + key + " (locale=" + locale + ")");
        return "<red>[" + key + "]";
    }

    private String lookupRaw(String key, String locale) {
        Map<String, String> bundle = bundles.get(locale);
        if (bundle == null) {
            return null;
        }
        if (bundle.containsKey(key)) {
            return bundle.get(key);
        }
        if ("toggle.on".equals(key) && bundle.containsKey("toggle.enabled")) {
            return bundle.get("toggle.enabled");
        }
        if ("toggle.off".equals(key) && bundle.containsKey("toggle.disabled")) {
            return bundle.get("toggle.disabled");
        }
        return null;
    }

    public Component component(String key, CommandSender sender, MessagePlaceholders placeholders) {
        String template = resolveRaw(key, resolveLocale(sender));
        TagResolver resolver = placeholders.toTagResolver(prefixResolver);
        return miniMessage.deserialize(template, resolver);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, MessagePlaceholders.empty());
    }

    public void send(CommandSender sender, String key, MessagePlaceholders placeholders) {
        deliver(sender, component(key, sender, placeholders));
    }

    public void sendToggle(CommandSender sender, String key, boolean enabled) {
        String stateKey = enabled ? "toggle.on" : "toggle.off";
        String locale = resolveLocale(sender);
        Component stateComponent = miniMessage.deserialize(resolveRaw(stateKey, locale));
        String template = resolveRaw(key, locale);
        TagResolver resolver = TagResolver.builder()
                .resolver(prefixResolver)
                .resolver(Placeholder.component("state", stateComponent))
                .build();
        deliver(sender, miniMessage.deserialize(template, resolver));
    }

    public void send(CommandSender sender, String key, TagResolver additionalResolvers) {
        TagResolver combined = TagResolver.builder()
                .resolver(prefixResolver)
                .resolver(additionalResolvers)
                .build();
        deliver(sender, miniMessage.deserialize(resolveRaw(key, resolveLocale(sender)), combined));
    }

    public Component stateComponent(CommandSender sender, boolean enabled) {
        String stateKey = enabled ? "toggle.on" : "toggle.off";
        return miniMessage.deserialize(resolveRaw(stateKey, resolveLocale(sender)));
    }

    private void deliver(CommandSender sender, Component component) {
        if (sender instanceof Player player) {
            if (sendNative(player, component)) {
                return;
            }
        }
        sender.sendMessage(toLegacyString(component));
    }

    private boolean sendNative(Player player, Component component) {
        try {
            String json = gson.serialize(component);
            Object serverComponent = deserializeOnServer(json);
            if (serverComponent == null) {
                return false;
            }
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method sendMessage = player.getClass().getMethod("sendMessage", componentClass);
            sendMessage.invoke(player, serverComponent);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static Object deserializeOnServer(String json) throws ReflectiveOperationException {
        ClassLoader serverLoader = org.bukkit.Bukkit.getServer().getClass().getClassLoader();
        Class<?> gsonClass = Class.forName(
                "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer", true, serverLoader);
        Method gsonMethod = gsonClass.getMethod("gson");
        Object gsonSerializer = gsonMethod.invoke(null);
        return gsonClass.getMethod("deserialize", String.class).invoke(gsonSerializer, json);
    }

    public String toLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Converts a plugin-shaded Adventure component to the server's Adventure class for reflective API calls.
     */
    public Object toServerComponent(Component component) {
        try {
            return deserializeOnServer(gson.serialize(component));
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    /** For item display names / lore that still expect legacy section codes. */
    public String toLegacy(String miniMessageTemplate, CommandSender sender, MessagePlaceholders placeholders) {
        return toLegacyString(componentFromTemplate(miniMessageTemplate, sender, placeholders));
    }

    public Component componentFromTemplate(String template, CommandSender sender, MessagePlaceholders placeholders) {
        TagResolver resolver = placeholders.toTagResolver(prefixResolver);
        return miniMessage.deserialize(template, resolver);
    }

    public TagResolver prefixResolver() {
        return prefixResolver;
    }

    public boolean hasLocale(String locale) {
        return bundles.containsKey(normalizeLocale(locale));
    }

    public String availableLocales() {
        return String.join(", ", bundles.keySet());
    }
}
