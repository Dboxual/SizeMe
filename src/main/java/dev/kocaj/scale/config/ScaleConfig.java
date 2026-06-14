package dev.kocaj.scale.config;

import dev.kocaj.scale.ScalePlugin;

import java.util.List;

public class ScaleConfig {

    private final ScalePlugin plugin;

    private double minScale;
    private double maxScale;
    private double tinyScale;
    private double normalScale;
    private double giantScale;
    private List<String> disabledWorlds;
    private boolean disableInCombat;
    private int combatSeconds;
    private boolean resetOnPvp;
    private String pvpEnterMessage;
    private String pvpExitMessage;

    public ScaleConfig(ScalePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        minScale      = plugin.getConfig().getDouble("min-scale",      0.1);
        maxScale      = plugin.getConfig().getDouble("max-scale",     20.0);
        tinyScale     = plugin.getConfig().getDouble("tiny-scale",     0.5);
        normalScale   = plugin.getConfig().getDouble("normal-scale",   1.0);
        giantScale    = plugin.getConfig().getDouble("giant-scale",   20.0);
        disabledWorlds = plugin.getConfig().getStringList("disabled-worlds")
                .stream().map(String::toLowerCase).toList();
        disableInCombat  = plugin.getConfig().getBoolean("disable-in-combat", false);
        combatSeconds    = plugin.getConfig().getInt("combat-seconds", 15);
        resetOnPvp       = plugin.getConfig().getBoolean("reset-on-pvp", true);
        pvpEnterMessage  = plugin.getConfig().getString("pvp-enter-message", "&7Your size was normalized for PvP.");
        pvpExitMessage   = plugin.getConfig().getString("pvp-exit-message",  "&7Your size has been restored.");
    }

    public double getMinScale()           { return minScale; }
    public double getMaxScale()           { return maxScale; }
    public double getTinyScale()          { return tinyScale; }
    public double getNormalScale()        { return normalScale; }
    public double getGiantScale()         { return giantScale; }
    public List<String> getDisabledWorlds() { return disabledWorlds; }
    public boolean isDisableInCombat()    { return disableInCombat; }
    public int getCombatSeconds()         { return combatSeconds; }
    public boolean isResetOnPvp()         { return resetOnPvp; }
    public String getPvpEnterMessage()    { return pvpEnterMessage; }
    public String getPvpExitMessage()     { return pvpExitMessage; }
}
