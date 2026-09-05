package com.lozaine.aurautils.economy;

import org.bukkit.OfflinePlayer;

/**
 * Vault-backed operations. Implemented only by {@code VaultBridge}, which is
 * loaded after the Vault plugin is confirmed present.
 */
interface EconomyHandler {

    String name();

    boolean has(OfflinePlayer player, double amount);

    double balance(OfflinePlayer player);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    String format(double amount);
}
