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
    "burnDurationTicks": 30,
    "cooldownTicks": 40
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
        "destination": "cygnus",
        "feature": "NAVIGATOR_SLENDER"
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
- `elytra.burnDurationTicks`: how many ticks a lit firework rocket boosts a flying player for -
  the boost itself is Vanilla's own client-side firework impulse (ported from
  [Voyager](https://github.com/onelitefeather/Voyager)'s `FireworkBoostTracker`/`Rockets`), not a
  server-applied velocity, so there is no multiplier to configure
- `elytra.cooldownTicks`: how many ticks after a boost starts before the player may use another
  rocket; must be strictly greater than `elytra.burnDurationTicks`, since it is measured from the
  burn's start
- `navigator.title`: the shared navigator inventory's title, as a MiniMessage string
- `navigator.entries`: the navigator's destinations, each with a hotbar-chest slot (`0`-`8`), an
  icon material key, a MiniMessage display name and the CloudNet task name a click delivers the
  player to
- `navigator.entries[].feature` (optional): the name of a `TitanFeatures` feature flag this
  destination is gated behind, e.g. `"NAVIGATOR_SLENDER"`. Omitted, the destination is always
  visible. A name Togglz does not recognize aborts startup with a message naming
  `navigator.entries` and the unknown name. A flag missing from `flags.properties` counts as
  **off** - Slender, for example, stays hidden until `NAVIGATOR_SLENDER` is explicitly turned on.
  Toggling a flag takes effect the next time a player opens the navigator, with no restart.

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

### Adding a Lobby Feature Module

A lobby feature is a self-contained package under
`app/src/main/java/net/onelitefeather/titan/app/feature/<name>/`, discovered automatically by
Avaje Inject - there is no central module list to edit:

- New package, copied from the template module at
  `app/src/test/java/net/onelitefeather/titan/app/feature/example/` (`ExampleModule` and friends).
- The `<Name>Module` class implements `LobbyModule` and carries `@jakarta.inject.Singleton` plus a
  unique `@io.avaje.inject.Priority(n)` - ascending priority is start order, the seven existing
  modules use gaps of 100 (protection 100, spawn 200, respawn 300, navigator 400, sit 500,
  tickle 600, elytra 700). Missing either annotation fails the build (ArchUnit), not just the
  running lobby.
- Dependencies (a platform service such as `Deliver`, an `Instance`, a `Clock`, ...) are requested
  through the constructor; `@jakarta.inject.Inject` is only needed on a constructor when the class
  has more than one. A brand-new shared platform service is added as another `@Bean` in
  `app/src/main/java/net/onelitefeather/titan/app/bootstrap/PlatformBeans.java`, or, if it carries
  feature-spanning logic of its own rather than wrapping a platform type, as its own
  `@Singleton` class.
- Zero changed lines outside the new package - except a brand-new shared platform service, which
  necessarily touches `PlatformBeans`.
- A dependency nothing provides fails the build or the start, naming the missing type, instead of
  the lobby quietly running without that module.
- The actual start order is visible at runtime in one INFO log line:
  `Lobby modules enabled in order: {}`.

See [`docs/lobby-modules.md`](docs/lobby-modules.md) (German) for the full walkthrough - module
anatomy, `ModuleContext` dock points, tick-thread rules, test setup with `ModuleHarness`, the
ArchUnit rules, and a copyable template module with its tests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Credits

Developed by OneLiteFeather Network.
