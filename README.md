# Minecraft Player Renaming plugin 0.1.2+1.21.8
Simple Fabric-based mod that adds a command for server operators to handle player renaming with saving all player's progress on a new nickname.

### NOTE. This works only for Minecraft Server that is launched in offline mode!

Due to Minecraft server offline mode mechanism, every single time when nickname is changed, new UUID is computed based off it.

This mod adds an ability to preserve player data whenever someone requests to change their nickname.

Since everything in the server logic is based on UUID, this mod preserves an original UUID and changes nickname for it. 

## File system usage
Whenever server is started in offline mode, new file is created under the server's root path, called `player-nickname-migrations/migrations.json`. So if you want to make amends when server is turned off, just add migrations in this manner:

```json
{
  "praise_the_sun": "praise_the_moon"
}
```
And start the server. Once it has started, the data is parsed once, and then is never read unless new restart comes up 
## In-game usage
`/transferplayer old_nickname new_nickname`

After every successful command execution (and server shutdown), `player-nickname-migrations/migrations.json` will be updated.

## Permissions
By default, level 4 operator is required, but due to [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) you can set `player-migration.command.transferplayer` permission.