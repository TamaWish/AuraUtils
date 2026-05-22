package me.aurautils.commands;

import me.aurautils.AuraUtils;
import me.aurautils.util.MessagePlaceholders;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AuraCommand implements CommandExecutor {

    private final AuraUtils plugin;

    public AuraCommand(AuraUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aura.admin")) {
                plugin.send(sender, "aura.reload-denied");
                return true;
            }
            plugin.reloadPluginConfig();
            plugin.send(sender, "aura.reloaded");
            boolean keepInv = plugin.getServerSettingsManager().isKeepInventoryEnabled();
            plugin.send(sender, "aura.keepinventory-status", TagResolver.builder()
                    .resolver(Placeholder.component("state", plugin.getMessages().stateComponent(sender, keepInv)))
                    .build());
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("locale")) {
            if (!(sender instanceof Player player)) {
                return true;
            }
            if (args.length < 2) {
                plugin.send(player, "aura.locale-usage");
                return true;
            }
            if (args[1].equalsIgnoreCase("clear")) {
                plugin.getPlayerDataManager().setLocaleOverride(player.getUniqueId(), null);
                plugin.send(player, "aura.locale-cleared");
                return true;
            }
            String locale = args[1].toLowerCase();
            if (!plugin.getMessages().hasLocale(locale)) {
                plugin.send(player, "aura.locale-invalid", MessagePlaceholders.builder()
                        .add("locale", locale)
                        .add("list", plugin.getMessages().availableLocales())
                        .build());
                return true;
            }
            plugin.getPlayerDataManager().setLocaleOverride(player.getUniqueId(), locale);
            plugin.send(player, "aura.locale-set", MessagePlaceholders.of("locale", locale));
            return true;
        }

        plugin.send(sender, "aura.header");
        plugin.send(sender, "aura.title", MessagePlaceholders.of("version", plugin.getDescription().getVersion()));
        plugin.send(sender, "aura.tagline");
        plugin.send(sender, "aura.help-tpa");
        plugin.send(sender, "aura.help-tpahere");
        plugin.send(sender, "aura.help-tpaccept");
        plugin.send(sender, "aura.help-tpadeny");
        plugin.send(sender, "aura.help-warp");
        plugin.send(sender, "aura.help-setwarp");
        plugin.send(sender, "aura.help-home");
        plugin.send(sender, "aura.help-sethome");
        plugin.send(sender, "aura.help-back");
        plugin.send(sender, "aura.help-menu");
        plugin.send(sender, "aura.help-god");
        plugin.send(sender, "aura.help-fly");
        plugin.send(sender, "aura.help-nofall");
        plugin.send(sender, "aura.help-nohunger");
        plugin.send(sender, "aura.help-reload");
        plugin.send(sender, "aura.help-locale");
        plugin.send(sender, "aura.help-rtp");
        plugin.send(sender, "aura.help-setspawn");
        plugin.send(sender, "aura.help-keepinventory");

        if (sender instanceof Player player) {
            var mgr = plugin.getPlayerDataManager();
            UUID id = player.getUniqueId();
            plugin.send(player, "aura.status-header");
            plugin.send(player, "aura.status-line", MessagePlaceholders.builder()
                    .add("god", toggleState(mgr.isGod(id), player))
                    .add("fly", toggleState(mgr.isFly(id), player))
                    .add("nofall", toggleState(mgr.isNoFall(id), player))
                    .add("nohunger", toggleState(mgr.isNoHunger(id), player))
                    .build());
        }
        plugin.send(sender, "aura.header");
        return true;
    }

    private String toggleState(boolean on, Player player) {
        return plugin.getMessages().raw(on ? "toggle.on" : "toggle.off", player);
    }
}
