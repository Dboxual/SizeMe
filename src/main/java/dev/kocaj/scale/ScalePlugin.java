package dev.kocaj.scale;

import dev.kocaj.scale.command.ScaleCommand;
import dev.kocaj.scale.config.ScaleConfig;
import dev.kocaj.scale.listener.ScaleListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ScalePlugin extends JavaPlugin {

    private ScaleConfig scaleConfig;
    private CombatTracker combatTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.scaleConfig    = new ScaleConfig(this);
        this.combatTracker  = new CombatTracker();

        ScaleCommand scaleCommand = new ScaleCommand(this, scaleConfig, combatTracker);
        getCommand("scale").setExecutor(scaleCommand);
        getCommand("scale").setTabCompleter(scaleCommand);

        getServer().getPluginManager().registerEvents(
                new ScaleListener(this, scaleConfig, combatTracker), this);

        getLogger().info("Scale v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Scale disabled.");
    }

    public ScaleConfig getScaleConfig() {
        return scaleConfig;
    }

    public CombatTracker getCombatTracker() {
        return combatTracker;
    }
}
