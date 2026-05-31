# Scale — CLAUDE.md

## Project overview

Scale is a standalone Paper 1.21.1 plugin that exposes `/scale tiny|normal|giant` and `/scale set <player> <preset>` commands. It applies Minecraft's `minecraft:generic.scale` attribute to players via the Bukkit attribute API.

## Build

```bash
mvn clean package
# Output: target/Scale-1.0.0.jar
```

Requires Java 21 and Maven 3.8+. All dependencies are `provided` (no shading needed).

## Project structure

```
src/main/java/dev/kocaj/scale/
  ScalePlugin.java          — plugin main class, wires up config and command
  config/ScaleConfig.java   — thin wrapper around config.yml values
  command/ScaleCommand.java — all command and tab-complete logic

src/main/resources/
  plugin.yml   — command and permission declarations
  config.yml   — default configuration (filtered by Maven for version injection)
```

## Key design decisions

- **Single command, four subcommands.** All scale logic lives in `ScaleCommand`. No separate handlers per preset — the switch dispatches to shared `applyScale()`.
- **Config validation at command time.** `ScaleConfig` holds parsed doubles. Range checks happen in `applyScale()` so the error messages are consistent regardless of which path triggered the scale change.
- **Standard Bukkit permissions.** `player.hasPermission("scale.xyz")` is used throughout. LuckPerms hooks into this layer automatically — no LuckPerms dependency required.
- **Attribute API.** Scale is set via `player.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(value)`. `IllegalArgumentException` is caught and surfaced as a user-friendly message in case the server rejects an out-of-range value.

## How to add a new preset

1. Add a new config key to `config.yml` (e.g. `huge-scale: 5.0`).
2. Add a getter in `ScaleConfig` and read it in `reload()`.
3. Add a new `case` in `ScaleCommand.onCommand` pointing to the new config value and a new permission string.
4. Declare the permission in `plugin.yml`.
5. Update `onTabComplete` to include the new subcommand name.

## Future integration points

- **LuckPerms unlock system**: call `user.data().add(Node.builder("scale.tiny").build())` to grant a preset.
- **Bits / economy**: check balance before calling the existing `handlePreset` logic; no structural changes needed.
- **GUI**: read `ScaleConfig` values to populate buttons; trigger `applyScale` on click.

## Do NOT add

- Economy, GUI, or unlock systems (planned for future plugins).
- A `/scale reload` command unless explicitly requested.
- Any soft-depend on LuckPerms — permission checks must remain vanilla Bukkit.
