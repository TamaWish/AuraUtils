package me.aurautils.listeners;

import me.aurautils.AuraUtils;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Projectile;

public class DamageMultiplierListener implements Listener {

    private final AuraUtils plugin;
    public DamageMultiplierListener(AuraUtils plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Player attacker = null;
        boolean rangedAttack = false;

        if (damager instanceof Player player) {
            attacker = player;
        } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            attacker = player;
            rangedAttack = true;
        }

        if (attacker == null) return;

        ItemStack inHand = attacker.getInventory().getItemInMainHand();
        if (inHand == null) return;

        Material mat = inHand.getType();
        if (mat == Material.AIR || !plugin.isDamageWeapon(mat)) return;

        if (rangedAttack && mat != Material.BOW && mat != Material.CROSSBOW && mat != Material.TRIDENT) return;

        double mult = plugin.getPlayerDataManager().getDamageMultiplier(attacker.getUniqueId());
        if (mult == 1.0) return; // no change needed

        event.setDamage(event.getDamage() * mult);
    }
}
