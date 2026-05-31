# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.3.0] — 2026-05-31

### Added
- `reset-on-pvp` config key (default: `true`) — when enabled, both players involved in a PvP hit are immediately reset to `normal-scale`
- Only player-vs-player damage triggers the reset; PvE is ignored
- Players already at `normal-scale` are skipped silently; only scaled players receive the reset message
- Disabled-world enforcement is respected automatically: players in a disabled world are already at `normal-scale` from the world-change listener, so the reset is a no-op with no message

### Changed
- Restructured `onEntityDamage` in `ScaleListener` — removed the early `isDisableInCombat()` guard so combat tagging and PvP reset are evaluated independently on the same event

---

## [1.2.0] — 2026-05-31

### Added
- `/scale <number>` — set an arbitrary numeric scale value (e.g. `/scale 2`); requires `scale.custom`
- `scale.custom` permission
- Custom scale input capped at `16.0` (practical client rendering limit) even when `max-scale` is higher; `min-scale` and `max-scale` still apply as the lower bound and secondary upper bound respectively
- Tab completion includes common numeric suggestions (`0.5`, `1`, `2`, `5`, `10`, `16`) for players with `scale.custom`

### Changed
- Refactored `ScaleCommand` to extract shared `isBlocked()` and `setAttribute()` helpers used by both preset and custom scale paths

---

## [1.1.0] — 2026-05-31

### Added
- `disabled-worlds` config key — players cannot use scale commands in listed worlds; players who enter a disabled world while scaled are automatically reset to `normal-scale`
- `disable-in-combat` config key — when `true`, blocks scale changes during active PvP (off by default)
- `combat-seconds` config key — duration in seconds a player remains tagged as in combat after a PvP hit (default: `15`)
- `/scale reload` — reloads `config.yml` at runtime; requires `scale.admin`
- `ScaleListener` — handles `PlayerChangedWorldEvent` (auto-reset), `EntityDamageByEntityEvent` (combat tagging), `PlayerQuitEvent` (combat state cleanup)
- `CombatTracker` — lightweight timestamp map for PvP combat state; no scheduler required

---

## [1.0.0] — 2026-05-31

### Added
- `/scale tiny` — applies the `tiny-scale` preset to the executing player; requires `scale.tiny`
- `/scale normal` — applies the `normal-scale` preset to the executing player; requires `scale.normal`
- `/scale giant` — applies the `giant-scale` preset to the executing player; requires `scale.giant`
- `/scale set <player> <tiny|normal|giant>` — admin command to set another player's scale; requires `scale.admin`
- Permissions: `scale.tiny`, `scale.normal`, `scale.giant`, `scale.admin`
- `config.yml` with `min-scale`, `max-scale`, `tiny-scale`, `normal-scale`, `giant-scale`
- Validation: preset values outside `min-scale`/`max-scale` bounds are rejected with a clear error message
- Tab completion for all subcommands and online player names
- Standard Bukkit permission API — fully compatible with LuckPerms for future permission-based integrations
