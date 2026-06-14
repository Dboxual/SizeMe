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
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScaleListener implements Listener {

    private static final String PREFIX = "§8[§6Scale§8] §r";

    private final ScalePlugin plugin;
    private final ScaleConfig config;
    private final CombatTracker combatTracker;

    private final Map<UUID, BukkitTask> pendingRestores = new ConcurrentHashMap<>();

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

    // Handles PvP hits: combat tagging, scale normalization, and restore scheduling.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (config.isDisableInCombat()) {
            combatTracker.markInCombat(victim);
            combatTracker.markInCombat(attacker);
        }

        if (config.isResetOnPvp()) {
            enterCombat(attacker);
            enterCombat(victim);
        }
    }

    // On quit: cancel the restore task and silently restore the player's scale so
    // player.dat is written with the correct value before the session ends.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        BukkitTask task = pendingRestores.remove(uuid);
        if (task != null) task.cancel();

        if (combatTracker.hasSavedScale(player)) {
            Double saved = combatTracker.getSavedScale(player);
            combatTracker.clearSavedScale(player);
            if (saved != null) {
                AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
                if (scaleAttr != null) {
                    try {
                        scaleAttr.setBaseValue(saved);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        combatTracker.remove(player);
    }

    // --- Internal helpers ---

    /**
     * Called on each PvP hit for a participant.
     * Saves the player's scale on first entry, normalizes it, and schedules (or
     * reschedules) the restore task for after combat-seconds.
     */
    private void enterCombat(Player player) {
        AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr == null) return;

        double normalScale = config.getNormalScale();
        double currentScale = scaleAttr.getBaseValue();
        boolean firstEntry = !combatTracker.hasSavedScale(player);

        if (firstEntry) {
            combatTracker.saveScale(player, currentScale);

            if (currentScale != normalScale) {
                try {
                    scaleAttr.setBaseValue(normalScale);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Failed to normalize scale for " + player.getName()
                            + " on PvP entry: " + e.getMessage());
                }
                String msg = config.getPvpEnterMessage();
                if (!msg.isEmpty()) {
                    player.sendMessage(PREFIX + colorize(msg));
                }
            }
        }

        combatTracker.markInCombat(player);

        BukkitTask existing = pendingRestores.remove(player.getUniqueId());
        if (existing != null) existing.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> restoreAfterCombat(player),
                (long) config.getCombatSeconds() * 20L);

        pendingRestores.put(player.getUniqueId(), task);
    }

    /**
     * Fired by the delayed task after combat expires.
     * Restores the player's saved scale and sends the exit message.
     */
    private void restoreAfterCombat(Player player) {
        pendingRestores.remove(player.getUniqueId());

        Double savedScale = combatTracker.getSavedScale(player);
        combatTracker.clearSavedScale(player);
        combatTracker.remove(player);

        if (savedScale == null) return;

        double normalScale = config.getNormalScale();
        if (savedScale == normalScale) return;

        AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr == null) return;

        try {
            scaleAttr.setBaseValue(savedScale);
            String msg = config.getPvpExitMessage();
            if (!msg.isEmpty()) {
                player.sendMessage(PREFIX + colorize(msg));
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to restore scale for " + player.getName()
                    + " after combat: " + e.getMessage());
        }
    }

    private String colorize(String s) {
        return s.replace('&', '§');
    }
}
