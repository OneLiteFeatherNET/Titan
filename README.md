# Titan

Titan is a complete Minestom-based Minecraft lobby server that provides various quality-of-life features to enhance player experience. It contains everything needed to run a fully functional Minestom server.

## Features

- **Sitting System**: Allows players to sit on specific blocks like stairs
- **Tickle Mechanic**: Players can tickle each other using feathers, with cooldown periods
- **Elytra Boost**: Provides boost functionality for players using elytra
- **Height Teleportation**: Automatically teleports players when they exceed certain height limits

## Requirements

- Java 24 or higher

## Installation

1. Download the latest release from the releases page
2. Run the server using: `java -jar titan-x.x.x.jar`
3. The server will generate default configuration files on first run

## Running the Server

Once installed, you can:

- Start the server with additional memory: `java -Xmx2G -jar titan-x.x.x.jar`
- Use the console to manage the server while it's running
- Stop the server safely by typing `stop` in the console

### Server Properties

You can configure server properties like port, MOTD, and more in the generated configuration files.

## Configuration

Configuration is done through the `app.json` file, one named section per lobby feature module
(`configVersion: 2`). A module reads only its own section; a missing section or a missing field
falls back to the documented default below, and `app.json` is created with every module's defaults
on first start if it does not exist yet. A value that fails validation (e.g. a negative cooldown, or
`minHeight` not less than `maxHeight`) aborts startup with a message naming the section, field and
reason - the lobby never starts with a silently replaced value.

An `app.json` from before this format (a flat document with no `configVersion`) is migrated
automatically on the next start: the old file is kept alongside as `app.json.v1.bak`, and any key
that no longer has a home (`updateRateAgones`, `fireworkBoostSlot`) is dropped and named in the log.

```json
{
  "configVersion": 2,
  "spawn": {
    "minHeight": -64,
    "maxHeight": 310,
    "simulationDistance": 2
  },
  "sit": {
    "offset": {
      "x": 0.5,
      "y": 0.25,
      "z": 0.5
    },
    "allowedBlocks": [
      "minecraft:spruce_stairs"
    ]
  },
  "tickle": {
    "cooldownMillis": 4000
  },
  "elytra": {
    "boostMultiplier": 35.0
  },
  "navigator": {
    "title": "<yellow>Navigator",
    "entries": [
      {
        "slot": 0,
        "icon": "minecraft:elytra",
        "displayName": "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>",
        "destination": "ElytraRace"
      },
      {
        "slot": 4,
        "icon": "minecraft:grass_block",
        "displayName": "<!i><green>Survival",
        "destination": "Survival"
      },
      {
        "slot": 5,
        "icon": "minecraft:enderman_spawn_egg",
        "displayName": "<!i><gradient:#616161:#e80000c>Slender</gradient>",
        "destination": "cygnus"
      },
      {
        "slot": 8,
        "icon": "minecraft:wooden_axe",
        "displayName": "<!i><rainbow>Creative</rainbow>",
        "destination": "MemberBuild"
      }
    ]
  }
}
```

### Configuration Options Explained

- `spawn.minHeight` / `spawn.maxHeight`: height bounds a player is teleported back to spawn outside
  of
- `spawn.simulationDistance`: simulation distance sent to a player on spawn
- `sit.offset`: offset from the clicked block's position to the seat (x, y, z)
- `sit.allowedBlocks`: block keys players may sit down on, e.g. `minecraft:spruce_stairs`
- `tickle.cooldownMillis`: duration of the tickle cooldown in milliseconds
- `elytra.boostMultiplier`: multiplier applied to the vanilla-equivalent firework boost
- `navigator.title`: the shared navigator inventory's title, as a MiniMessage string
- `navigator.entries`: the navigator's destinations, each with a hotbar-chest slot (`0`-`8`), an
  icon material key, a MiniMessage display name and the CloudNet task name a click delivers the
  player to

## Development

### Building from Source

1. Clone the repository
2. Build using Gradle:
   ```
   ./gradlew clean build
   ```

### Testing

Run tests using:
```
./gradlew test
```

Code coverage reports are generated using JaCoCo and can be found in `build/reports/jacoco/`.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Credits

Developed by OneLiteFeather Network.
