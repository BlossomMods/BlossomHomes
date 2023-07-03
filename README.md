# BlossomHomes

BlossomHomes is a Minecraft Fabric mod in the Blossom-series mods that provides /home command and utilities

## Table of contents

- [Dependencies](#dependencies)
- [Config](#config)
- [Commands & their permissions](#commands--their-permissions)
- [Translation keys](#translation-keys)

## Dependencies

* [BlossomLib](https://github.com/BlossomMods/BlossomLib)
* [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) / [LuckPerms](https://luckperms.net/) /
  etc. (Optional)

## Config

This mod's config file can be found at `config/BlossomMods/BlossomHomes.json`, after running the server with
the mod at least once.

`teleportation`: [TeleportationConfig](https://github.com/BlossomMods/BlossomLib/blob/main/README.md#teleportationconfig)
-
teleportation settings  
`standStill`: int - (seconds), how long the player has to stand still before being teleported  
`cooldown`: int - (seconds), how long the player has to wait after teleporting using this command, before
being able to teleport again  
`defaultHome`: String - name of the default home  
`startHomes`: int - default max homes  
`dimensionBlacklist`: String[] - a list of dimension ids (like `minecraft:the_end`) in which a player can't set a home
`useBlacklistAsWhitelist`: boolean - invert blacklist to function as a whitelist

## Commands & their permissions

In all commands where `<name>` is optional, if it's not provided, config value `defaultHome` will be used.  
Do pay attention that only `/homes` subcommand permissions have the `homes` as a plural. This is done
to allow the server owners to hide some commands in case of command clutter.

- `/home [<name>]` - teleport self to home named `<name>`  
  Permission: `blossom.home` (default: true)
- `/sethome [<name>]` - creates a home named `<name>`  
  Permission: `blossom.home.set` (default: true)
- `/sethome <name> <position> <rotation>` - creates a home named `<name>` at `<position>` facing `<rotation>`  
  Permission: `blossom.home.set.pos` (default: false)
- `/sethome <name> <position> <rotation> <dimension>` - creates a home named `<name>` in `<dimension>` at `<position>`
  facing `<rotation>`  
  Permission: `blossom.home.set.dim` (default: false)
- `/delhome [<name>]` - delete a home named `<name>`  
  Permission: `blossom.home.remove` (default: true)
- `/listhomes` - lists all homes  
  Permission: `blossom.home.list` (default: true)
- `/homes` - alias of `/listhomes`  
  Permission: `blossom.homes.list` (default: true)
  - `list` - alias of `/listhomes`  
    Permission: `blossom.homes.list` (default: true)
  - `set` - alias of `/sethome`  
    Permission: `blossom.homes.set` (default: true)
  - `add` - alias of `/sethome`  
    Permission: `blossom.homes.add` (default: false)
  - `delete` - alias of `/delhome`  
    Permission: `blossom.homes.delete` (default: true)
  - `remove` - alias of `/delhome`  
    Permission: `blossom.homes.remove` (default: false)
  - `set-max <new-max> <players>` - set all `<players>`'s max homes to `<new-max>`   
    Permission: `blossom.homes.set-max` (default: OP level 2)
  - `load-legacy [<overwrite>]` - load legacy FabricHomes player homes, appending them to existing player homes, unless
    `<overwrite>` is set to `true`, then replace all (found) player homes. Players don't have to be online for their
    homes to be updated  
    Permission: `blossom.homes.load-legacy` (default: OP level 4)

Permission: `blossom.homes.set.in-blacklist` (default: OP level 2) - allow setting homes in blacklisted dimensions.
To set other players' homes, `/execute as <player> run sethome <name>` can be used.

## Translation keys

only keys with available arguments are shown, for full list, please see
[`src/main/resources/data/blossom/lang/en_us.json`](src/main/resources/data/blossom/lang/en_us.json)

- `blossom.homes.list.empty`: 1 argument - max home count
- `blossom.homes.list.header`: 2 arguments - total home count, max home count
- `blossom.homes.list.item`: 1 argument - home name
- `blossom.homes.list.item.description`: 7 arguments - home name, home dimension key, home x, home y, home z, home yaw,
  home pitch
- `blossom.homes.add`: 1 argument - home name
- `blossom.homes.add.failed.max`: 1 argument - max home count
- `blossom.homes.add.failed.name`: 1 argument - home name
- `blossom.homes.remove`: 1 argument - home name
- `blossom.homes.remove.failed`: 1 argument - home name
- `blossom.homes.setMax`: 2 arguments - new max, players
- `blossom.homes.load-legacy.done`: 2 arguments - home count, player count

`zh_cn` (Chinese, Simplified), `zh_tw` (Chinese, Traditional) - added by @BackWheel
