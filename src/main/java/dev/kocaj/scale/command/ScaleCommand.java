package dev.kocaj.scale.command;

import dev.kocaj.scale.CombatTracker;
import dev.kocaj.scale.ScalePlugin;
import dev.kocaj.scale.config.ScaleConfig;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ScaleCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§6Scale§8] §r";

    // Practical client rendering cap — custom input is clamped to this even if max-scale is higher.
    private static final double RENDER_CAP = 16.0;

    private final ScalePlugin plugin;
    private final ScaleConfig config;
    private final CombatTracker combatTracker;

    public ScaleCommand(ScalePlugin plugin, ScaleConfig config, CombatTracker combatTracker) {
        this.plugin = plugin;
        this.config = config;
        this.combatTracker = combatTracker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "tiny"   -> handlePreset(sender, "tiny",   config.getTinyScale(),   "scale.tiny");
            case "normal" -> handlePreset(sender, "normal", config.getNormalScale(), "scale.normal");
            case "giant"  -> handlePreset(sender, "giant",  config.getGiantScale(),  "scale.giant");
            case "set"    -> handleSet(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                try {
                    double value = Double.parseDouble(args[0]);
                    handleCustom(sender, value);
                } catch (NumberFormatException e) {
                    sendUsage(sender);
                }
            }
        }

        return true;
    }

    // --- Preset handler (self) ---

    private void handlePreset(CommandSender sender, String presetName, double scale, String permission) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§cThis command can only be used by players.");
            return;
        }

        if (!player.hasPermission(permission)) {
            player.sendMessage(PREFIX + "§cYou don't have permission to use §e/scale " + presetName + "§c.");
            return;
        }

        applyScale(player, scale, presetName, null);
    }

    // --- Admin set handler ---

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scale.admin")) {
            sender.sendMessage(PREFIX + "§cYou don't have permission to use §e/scale set§c.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cUsage: §e/scale set <player> <tiny|normal|giant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "§cPlayer §e" + args[1] + " §cis not online.");
            return;
        }

        String presetName = args[2].toLowerCase();
        double scale = switch (presetName) {
            case "tiny"   -> config.getTinyScale();
            case "normal" -> config.getNormalScale();
            case "giant"  -> config.getGiantScale();
            default -> {
                sender.sendMessage(PREFIX + "§cInvalid preset §e" + args[2] + "§c. Use: §etiny§c, §enormal§c, §egiant§c.");
                yield -1;
            }
        };

        if (scale < 0) return;

        applyScale(target, scale, presetName, sender.equals(target) ? null : sender);
    }

    // --- Reload handler ---

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("scale.admin")) {
            sender.sendMessage(PREFIX + "§cYou don't have permission to use §e/scale reload§c.");
            return;
        }

        config.reload();
        sender.sendMessage(PREFIX + "§aConfiguration reloaded.");
    }

    // --- Custom numeric scale handler ---

    private void handleCustom(CommandSender sender, double scale) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§cThis command can only be used by players.");
            return;
        }

        if (!player.hasPermission("scale.custom")) {
            player.sendMessage(PREFIX + "§cYou don't have permission to use §e/scale <number>§c.");
            return;
        }

        if (!Double.isFinite(scale)) {
            player.sendMessage(PREFIX + "§cNot a valid scale value.");
            return;
        }

        if (isBlocked(player, null)) return;

        double min = config.getMinScale();
        double effectiveMax = Math.min(config.getMaxScale(), RENDER_CAP);

        if (scale < min) {
            player.sendMessage(PREFIX + "§c" + fmt(scale) + " §cis below the minimum allowed scale §e(" + fmt(min) + ")§c.");
            return;
        }

        if (scale > effectiveMax) {
            if (config.getMaxScale() > RENDER_CAP) {
                player.sendMessage(PREFIX + "§c" + fmt(scale) + " §cexceeds the maximum for custom input §e(" + fmt(RENDER_CAP) + ")§c."
                        + " §7Custom values are capped at §e16 §7due to client rendering limits.");
            } else {
                player.sendMessage(PREFIX + "§c" + fmt(scale) + " §cexceeds the maximum allowed scale §e(" + fmt(effectiveMax) + ")§c.");
            }
            return;
        }

        if (!setAttribute(player, scale)) return;

        player.sendMessage(PREFIX + "§aYour scale is now §e" + fmt(scale) + "§a.");
    }

    // --- Core preset scale application ---

    private void applyScale(Player target, double scale, String presetName, CommandSender notifyAdmin) {
        if (isBlocked(target, notifyAdmin)) return;

        double min = config.getMinScale();
        double max = config.getMaxScale();

        if (scale < min) {
            String msg = PREFIX + "§cThe §e" + presetName + " §cpreset value §e(" + fmt(scale) + ") §c"
                    + "is below the server minimum §e(" + fmt(min) + ")§c. Update §econfig.yml §cto fix this.";
            target.sendMessage(msg);
            if (notifyAdmin != null) notifyAdmin.sendMessage(msg);
            return;
        }

        if (scale > max) {
            String msg = PREFIX + "§cThe §e" + presetName + " §cpreset value §e(" + fmt(scale) + ") §c"
                    + "exceeds the server maximum §e(" + fmt(max) + ")§c. Update §econfig.yml §cto fix this.";
            target.sendMessage(msg);
            if (notifyAdmin != null) notifyAdmin.sendMessage(msg);
            return;
        }

        if (!setAttribute(target, scale)) return;

        target.sendMessage(PREFIX + "§aYour scale is now §e" + presetName + " §7(" + fmt(scale) + ")§a.");
        if (notifyAdmin != null) {
            notifyAdmin.sendMessage(PREFIX + "§aSet §e" + target.getName() + "'s §ascale to §e"
                    + presetName + " §7(" + fmt(scale) + ")§a.");
        }
    }

    // --- Shared guards ---

    /** Returns true and sends appropriate messages if the player is blocked from scaling. */
    private boolean isBlocked(Player target, CommandSender notifyAdmin) {
        String worldName = target.getWorld().getName();
        if (config.getDisabledWorlds().contains(worldName.toLowerCase())) {
            target.sendMessage(PREFIX + "§cScale commands are disabled in world §e" + worldName + "§c.");
            if (notifyAdmin != null) {
                notifyAdmin.sendMessage(PREFIX + "§cCannot scale §e" + target.getName()
                        + " §c— they are in a disabled world §e(" + worldName + ")§c.");
            }
            return true;
        }

        if (config.isDisableInCombat() && combatTracker.isInCombat(target, config.getCombatSeconds())) {
            target.sendMessage(PREFIX + "§cYou cannot change your scale while in combat.");
            if (notifyAdmin != null) {
                notifyAdmin.sendMessage(PREFIX + "§cCannot scale §e" + target.getName()
                        + " §c— they are currently in combat.");
            }
            return true;
        }

        return false;
    }

    /** Applies the scale attribute. Returns false and sends an error message on failure. */
    private boolean setAttribute(Player target, double scale) {
        AttributeInstance scaleAttr = target.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr == null) {
            target.sendMessage(PREFIX + "§cFailed to apply scale: attribute unavailable.");
            plugin.getLogger().warning("GENERIC_SCALE attribute is null for " + target.getName()
                    + " — is this a non-player entity?");
            return false;
        }

        try {
            scaleAttr.setBaseValue(scale);
            return true;
        } catch (IllegalArgumentException e) {
            target.sendMessage(PREFIX + "§cThe server rejected the scale value §e" + fmt(scale)
                    + "§c. The server limit may be lower than your configured §emax-scale§c.");
            plugin.getLogger().warning("Failed to set scale " + scale + " for " + target.getName()
                    + ": " + e.getMessage());
            return false;
        }
    }

    // --- Usage ---

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(PREFIX + "§6Scale Commands:");
        sender.sendMessage("  §e/scale tiny              §7— Set your size to tiny");
        sender.sendMessage("  §e/scale normal            §7— Set your size to normal");
        sender.sendMessage("  §e/scale giant             §7— Set your size to giant");
        if (sender.hasPermission("scale.custom")) {
            sender.sendMessage("  §e/scale <number>          §7— Set a custom scale (e.g. §e/scale 2§7)");
        }
        if (sender.hasPermission("scale.admin")) {
            sender.sendMessage("  §e/scale set <player> <tiny|normal|giant>  §7— Set another player's size");
            sender.sendMessage("  §e/scale reload            §7— Reload config.yml");
        }
    }

    // --- Tab completion ---

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("tiny", "normal", "giant"));
            if (sender.hasPermission("scale.custom")) {
                options.addAll(List.of("0.5", "1", "2", "5", "10", "16"));
            }
            if (sender.hasPermission("scale.admin")) {
                options.add("set");
                options.add("reload");
            }
            return filterStartsWith(options, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("scale.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("scale.admin")) {
            return filterStartsWith(List.of("tiny", "normal", "giant"), args[2]);
        }

        return List.of();
    }

    // --- Helpers ---

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.startsWith(lower)).toList();
    }

    /** Formats a scale value cleanly: whole numbers drop the decimal, others display as-is. */
    private String fmt(double value) {
        return (value % 1 == 0) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
