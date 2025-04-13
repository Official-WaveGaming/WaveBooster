# WaveBooster Plugin

## Overview
WaveBooster is a Spigot/Bukkit plugin that implements an XP and item drop booster system for Minecraft servers with MythicMobs integration. The plugin allows players to activate personal boosters and admins to activate global boosters that multiply XP and item drops from mobs.

## Features
- **Two Booster Types**: XP boosters and Drop boosters
- **Personal & Global Boosters**: Personal boosters apply only to the player who activates them, while global boosters apply to all players on the server
- **Multiplier Stacking**: When multiple boosters are active, their effects multiply (e.g., 2x personal + 2x global = 4x total)
- **Duration-Based**: All boosters have configurable durations
- **SQL Storage**: All player boosters are stored in a MySQL database
- **MythicMobs Integration**: Works seamlessly with MythicMobs for XP and drops
- **Intuitive GUIs**: Player and admin interfaces for booster management
- **Tablist Display**: Shows active boosters in the tab list with remaining time
- **Comprehensive API**: For developers to integrate with other plugins

## Commands
### Player Commands
- `/boosters` - Opens the booster GUI
- `/boosters list` - Lists all your boosters
- `/boosters active` - Shows active boosters and multipliers
- `/boosters help` - Shows help menu

### Admin Commands
- `/adminbooster` - Opens the admin GUI
- `/adminbooster give <player> <type> <multiplier> <duration> [global]` - Gives a booster to a player
- `/adminbooster remove <player> <boosterId>` - Removes a booster from a player
- `/adminbooster global list` - Lists all active global boosters
- `/adminbooster global activate <type> <multiplier> <duration>` - Activates a global booster
- `/adminbooster global cancel <boosterId>` - Cancels a global booster
- `/adminbooster reload` - Reloads plugin configuration
- `/adminbooster help` - Shows admin help menu

## Permissions
- `wavebooster.use` - Allows access to player commands (default: true)
- `wavebooster.admin` - Allows access to admin commands (default: op)

## Configuration
The plugin includes two configuration files:
- `config.yml` - Main configuration file
- `messages.yml` - All plugin messages

## API
The plugin provides a BoosterAPI interface that can be used by other plugins to:
- Get player boosters
- Get active boosters for players
- Give/remove boosters
- Activate boosters
- Get multiplier values

## Database
The plugin uses MySQL to store:
- Player boosters
- Active boosters
- Booster properties (type, multiplier, duration)

## Installation
1. Place the plugin JAR in your server's `plugins` folder
2. Configure the database settings in `config.yml`
3. Restart your server
4. Configure permissions as needed

## Dependencies
- MythicMobs - Required for XP and drop integration

## Developer Information
For plugin developers looking to integrate with WaveBooster:

```java
// Get the API
BoosterAPI api = WaveBooster.getInstance().getBoosterAPI();

// Get player's active multiplier
UUID playerId = player.getUniqueId();
int xpMultiplier = api.getActiveMultiplier(playerId, BoosterType.XP);
int dropMultiplier = api.getActiveMultiplier(playerId, BoosterType.DROP);

// Give a player a booster
api.givePlayerBooster(playerId, BoosterType.XP, 2, 3600, false); // Personal 2x XP booster for 1 hour
```
