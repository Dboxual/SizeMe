package dev.kocaj.scale.listener;

import dev.kocaj.scale.CombatTracker;
import dev.kocaj.scale.ScalePlugin;
import dev.kocaj.scale.config.ScaleConfig;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ScaleListener implements Listener {

    private static final String PREFIX = "§8[§6Scale§8] §r";

    private final ScalePlugin plugin;
    private final ScaleConfig config;
    private final CombatTracker combatTracker;

    public ScaleListener(ScalePlugin plugin, ScaleConfig config, CombatTracker combatTracker) {
        this.plugin = plugin;
        this.config = config;
        this.combatTracker = combatTracker;
    }

    // Reset scale to normal when a player enters a disabled world.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String newWorld = player.getWorld().getName();

        if (!config.getDisabledWorlds().contains(newWorld.toLowerCase())) return;

        AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr == null) return;

        double normalScale = config.getNormalScale();
        if (scaleAttr.getBaseValue() == normalScale) return;

        try {
            scaleAttr.setBaseValue(normalScale);
            player.sendMessage(PREFIX + "§7Your scale was reset to normal upon entering §e" + newWorld + "§7.");
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to reset scale for " + player.getName()
                    + " on world change to " + newWorld + ": " + e.getMessage());
        }
    }

    // Handles PvP hits: combat tagging and/or immediate scale reset depending on config.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (config.isDisableInCombat()) {
            combatTracker.markInCombat(victim);
            combatTracker.markInCombat(attacker);
        }

        if (config.isResetOnPvp()) {
            resetScaleForPvp(attacker);
            resetScaleForPvp(victim);
        }
    }

    private void resetScaleForPvp(Player player) {
        AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr == null) return;

        double normalScale = config.getNormalScale();
        if (scaleAttr.getBaseValue() == normalScale) return;

        try {
            scaleAttr.setBaseValue(normalScale);
            player.sendMessage(PREFIX + "§7You were returned to normal size because you entered PvP.");
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to reset scale for " + player.getName()
                    + " on PvP hit: " + e.getMessage());
        }
    }

    // Clean up combat state on logout so it doesn't linger after reconnect.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        combatTracker.remove(event.getPlayer());
    }
}
