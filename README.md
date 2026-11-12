# Minecraft Paper Plugin: Echo Buffs

Echo Buffs is a Minecraft Paper plugin that creates a hostile Echo zombie when a player dies after being killed by another player or after recently receiving player damage. Defeating the Echo grants one random configurable potion-effect buff that lasts until the winner's next death.

## Install

1. Build the plugin with Java 21 and Maven:
   ```bash
   mvn package
   ```
2. Copy `target/plugin.jar` into the server's `plugins` directory.
3. Start or restart a Paper server running Minecraft 1.21.x.
4. Edit `plugins/Echo/config.yml` if you want to change buff amplifiers or disable individual buffs.
5. Restart the server or run:
   ```text
   /echo reload
   ```

## Use

When a player dies, an Echo is created if one of these conditions applies:

- Another player directly kills the player.
- The player dies within 15 seconds after being damaged by another player. The timer restarts whenever the player is hit again.
- Projectile damage counts when the projectile was fired by a player.
- Logging out does not create an Echo by itself.

Each Echo is a hostile zombie with 40 health and 4 attack damage. It remains for up to 10 minutes and is targeted at the player responsible for the death when that player is online and in the same world.

Defeating an Echo grants the player who dealt the final damage one random enabled buff. A player can only have one active buff; receiving another buff replaces the previous one. The active buff is removed when the player dies.

The available configurable buffs are:

- `speed`
- `strength`
- `haste`
- `jump_boost`
- `regeneration`
- `resistance`
- `fire_resistance`
- `water_breathing`
- `night_vision`
- `health_boost`
- `absorption`

All administrative commands require the `echo.admin` permission, which is granted to server operators by default.

List buffs and their current status:
```text
/echo buffs
```

Enable or disable a buff:
```text
/echo buff <id> <on|off>
```

Examples:
```text
/echo buff speed off
/echo buff resistance on
```

Reload the configuration:
```text
/echo reload
```

The plugin stores the active buff on the player, so it remains associated with the player while the server is running and is cleared on death.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/e9ad23be-5400-484d-bde0-9b8219b457af?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=e9ad23be-5400-484d-bde0-9b8219b457af
