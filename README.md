# Scale

A lightweight Paper plugin that lets players change their in-game scale via configurable presets.

## Requirements

- Paper 1.21.1+
- Java 21

## Installation

1. Drop `Scale-1.3.0.jar` into your server's `plugins/` folder.
2. Restart or reload the server.
3. Edit `plugins/Scale/config.yml` to adjust preset values and limits.
4. Grant permissions via LuckPerms (or your permission plugin of choice) — see below.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/scale tiny` | Set yourself to tiny size | `scale.tiny` |
| `/scale normal` | Set yourself to normal size | `scale.normal` |
| `/scale giant` | Set yourself to giant size | `scale.giant` |
| `/scale <number>` | Set an arbitrary scale (e.g. `/scale 2`) | `scale.custom` |
| `/scale set <player> <tiny\|normal\|giant>` | Set another player's scale | `scale.admin` |
| `/scale reload` | Reload config.yml | `scale.admin` |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `scale.tiny` | Use `/scale tiny` | op |
| `scale.normal` | Use `/scale normal` | op |
| `scale.giant` | Use `/scale giant` | op |
| `scale.custom` | Use `/scale <number>` for arbitrary values | op |
| `scale.admin` | Use `/scale set` and `/scale reload` | op |

All permissions default to `op`. Grant them to groups or players via LuckPerms:

```
/lp group <group> permission set scale.tiny true
/lp group <group> permission set scale.normal true
/lp group <group> permission set scale.giant true
/lp group <group> permission set scale.custom true
```

### Custom scale limits

`/scale <number>` respects `min-scale` and `max-scale` from `config.yml`. Additionally, even if `max-scale` is set above `16.0`, custom input is capped at **16.0** — the practical client rendering limit. Presets (tiny/normal/giant) are not subject to this cap, since their values are intentionally configured by an admin.

## Configuration

```yaml
# Safety limits — preset values outside these bounds are rejected.
min-scale: 0.1
max-scale: 20.0

# Preset values
tiny-scale:   0.5
normal-scale: 1.0
giant-scale:  20.0

# Disabled worlds — scale commands blocked; entering resets scale to normal.
disabled-worlds: []

# Combat restrictions
disable-in-combat: false   # block scale commands while in PvP
combat-seconds: 15         # how long the combat tag lasts
reset-on-pvp: true         # immediately reset both players to normal on any PvP hit
```

| Key | Default | Description |
|---|---|---|
| `min-scale` | `0.1` | Floor for all scale values |
| `max-scale` | `20.0` | Ceiling for preset values (`/scale <number>` caps at 16.0) |
| `tiny-scale` | `0.5` | Value applied by `/scale tiny` |
| `normal-scale` | `1.0` | Value applied by `/scale normal` |
| `giant-scale` | `20.0` | Value applied by `/scale giant` |
| `disabled-worlds` | `[]` | World names where scale commands are blocked |
| `disable-in-combat` | `false` | Block scale commands while in PvP combat |
| `combat-seconds` | `15` | Seconds the PvP combat tag lasts |
| `reset-on-pvp` | `true` | Instantly reset both players to `normal-scale` on any PvP hit |

### Vanilla rendering note

Minecraft's client renders scale reliably up to **16.0**. Values above 16.0 are applied server-side (and stored correctly in the attribute), but visual behaviour above that threshold depends on the client version. If visual accuracy matters, keep `giant-scale` at or below `16.0`.

## Building

```bash
mvn clean package
```

The compiled jar will be in `target/Scale-1.3.0.jar`.

## Future integrations

Permissions are managed entirely through the standard Bukkit permission API, making it straightforward to grant or revoke them programmatically via LuckPerms' API:

```java
// Example: grant scale.tiny to a player via LuckPerms API
LuckPerms lp = LuckPermsProvider.get();
User user = lp.getUserManager().getUser(playerUUID);
user.data().add(Node.builder("scale.tiny").build());
lp.getUserManager().saveUser(user);
```

This makes unlock systems, progression gates, and role-based grants trivial to wire up later.
