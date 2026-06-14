package dev.kocaj.scale;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last time each player was involved in PvP combat.
 * isInCombat() derives tag status from elapsed time so no cleanup task is needed.
 * savedScales stores each player's pre-combat scale so it can be restored on expiry.
 */
public class CombatTracker {

    private final Map<UUID, Long>   lastCombatTime = new ConcurrentHashMap<>();
    private final Map<UUID, Double> savedScales    = new ConcurrentHashMap<>();

    public void markInCombat(Player player) {
        lastCombatTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isInCombat(Player player, int combatSeconds) {
        Long last = lastCombatTime.get(player.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last < (long) combatSeconds * 1000L;
    }

    public void saveScale(Player player, double scale) {
        savedScales.put(player.getUniqueId(), scale);
    }

    public boolean hasSavedScale(Player player) {
        return savedScales.containsKey(player.getUniqueId());
    }

    public Double getSavedScale(Player player) {
        return savedScales.get(player.getUniqueId());
    }

    public void clearSavedScale(Player player) {
        savedScales.remove(player.getUniqueId());
    }

    public void remove(Player player) {
        lastCombatTime.remove(player.getUniqueId());
    }
}
