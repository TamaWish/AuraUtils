package me.aurautils.managers;



import me.aurautils.AuraUtils;

import me.aurautils.util.MessagePlaceholders;

import me.aurautils.util.WarpPermissions;

import org.bukkit.Location;

import org.bukkit.entity.Player;



/** Shared warp teleport flow for commands and GUI. */

public final class WarpService {



    private WarpService() {

    }



    /**

     * Resolves {@code nameOrAlias}, checks permission and cooldown, then teleports.

     *

     * @return {@code true} if the request was handled (including errors)

     */

    public static boolean teleport(AuraUtils plugin, Player player, String nameOrAlias) {

        WarpManager warps = plugin.getWarpManager();

        String canonical = warps.resolveWarpName(nameOrAlias);

        if (canonical == null) {

            plugin.send(player, "warp.not-found", MessagePlaceholders.of("name", nameOrAlias));

            return true;

        }



        if (!WarpPermissions.canUse(player, canonical)) {

            plugin.send(player, "warp.no-permission", MessagePlaceholders.of("name", canonical));

            return true;

        }



        WarpData data = warps.getWarpData(canonical);

        if (data == null) {

            plugin.send(player, "warp.not-found", MessagePlaceholders.of("name", nameOrAlias));

            return true;

        }



        int cooldownSeconds = data.getCooldownSeconds();

        boolean bypassCooldown = player.hasPermission("aura.warp.cooldown.bypass");

        if (!bypassCooldown && cooldownSeconds > 0) {

            long remaining = plugin.getWarpCooldownManager()

                    .remainingSeconds(player.getUniqueId(), canonical, cooldownSeconds);

            if (remaining > 0) {

                plugin.send(player, "warp.cooldown", MessagePlaceholders.builder()

                        .add("name", canonical)

                        .add("seconds", String.valueOf(remaining))

                        .build());

                return true;

            }

        }



        Location location = data.getLocation();

        MessagePlaceholders placeholders = MessagePlaceholders.of("name", canonical);

        Runnable recordCooldown = () -> {

            if (!bypassCooldown && cooldownSeconds > 0) {

                plugin.getWarpCooldownManager().recordUse(player.getUniqueId(), canonical);

            }

        };



        plugin.getTeleportService().teleport(player, location, plugin.getTeleportService().standardOptions()

                .successMessageKey("teleport.success-warp")

                .successPlaceholders(placeholders)

                .onSuccess(recordCooldown)

                .build());

        return true;

    }

}

