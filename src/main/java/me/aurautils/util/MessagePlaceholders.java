package me.aurautils.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessagePlaceholders {

    private static final MessagePlaceholders EMPTY = new MessagePlaceholders(Map.of(), Map.of());

    private final Map<String, String> values;
    private final Map<String, Component> components;

    private MessagePlaceholders(Map<String, String> values, Map<String, Component> components) {
        this.values = Map.copyOf(values);
        this.components = Map.copyOf(components);
    }

    public static MessagePlaceholders empty() {
        return EMPTY;
    }

    public static MessagePlaceholders of(String key, String value) {
        return new MessagePlaceholders(Map.of(key, value), Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public MessagePlaceholders with(String key, String value) {
        Map<String, String> merged = new LinkedHashMap<>(values);
        merged.put(key, value);
        return new MessagePlaceholders(merged, components);
    }

    public TagResolver toTagResolver(TagResolver prefixResolver) {
        TagResolver.Builder builder = TagResolver.builder().resolver(prefixResolver);
        for (Map.Entry<String, Component> entry : components.entrySet()) {
            builder.resolver(Placeholder.component(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    public static final class Builder {
        private final Map<String, String> values = new LinkedHashMap<>();
        private final Map<String, Component> components = new LinkedHashMap<>();

        public Builder add(String key, String value) {
            values.put(key, value);
            return this;
        }

        public Builder component(String key, Component component) {
            components.put(key, component);
            return this;
        }

        public MessagePlaceholders build() {
            if (values.isEmpty() && components.isEmpty()) {
                return empty();
            }
            return new MessagePlaceholders(values, components);
        }
    }
}
