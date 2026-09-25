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
3. The server runs with every module's compiled-in defaults if no configuration file is present;
   copy [`app/src/dist/application.example.yaml`](app/src/dist/application.example.yaml) to
   `application.yaml` next to the jar to customize it (see Configuration below)

## Running the Server

Once installed, you can:

- Start the server with additional memory: `java -Xmx2G -jar titan-x.x.x.jar`
- Use the console to manage the server while it's running
- Stop the server safely by typing `stop` in the console

### Server Properties

You can configure server properties like port, MOTD, and more in the generated configuration files.

## Configuration

Configuration lives in `application.yaml` in the lobby's working directory (next to the jar), one
named section per lobby feature module, key names matching that module's config record fields. A
commented example with every section and its defaults ships as
[`app/src/dist/application.example.yaml`](app/src/dist/application.example.yaml) - copy it to
`application.yaml` and edit only what should differ. A module reads only its own section; a missing
file, a missing section or a missing key falls back to the module's own compiled-in default, listed
below. The lobby never creates or writes a configuration file itself, other than the one-time
`app.json` switch described further down.

### Profiles and overrides

A profile file `application-<profile>.yaml`, next to `application.yaml`, only needs to set the keys
that differ for that profile - everything else still comes from the base file. Activate one or more
profiles with the environment variable `AVAJE_PROFILES` (e.g. `AVAJE_PROFILES=dev`) or the system
property `-Davaje.profiles=dev`. Every start logs the active profiles at INFO:
`Active configuration profiles: [...]`.

An external file can be layered in via the environment variable `CONFIG_FILE` or the system
property `-Dconfig.file=...` (e.g. for a Kubernetes ConfigMap or a CloudNet template file outside
the working directory).

Every key can also be set directly via an environment variable or a system property. Rank order,
low to high:

1. the module's own compiled-in default,
2. `application.yaml`,
3. the active profile's `application-<profile>.yaml`,
4. the external file selected via `CONFIG_FILE`/`config.file`,
5. an environment variable,
6. a system property (`-D...`).

An environment variable's name is the dotted key, upper-cased, with `.` replaced by `_` and `-`
dropped - e.g. `spawn.simulationDistance` becomes `SPAWN_SIMULATIONDISTANCE`, and the matching
system property is `-Dspawn.simulationDistance=...`. A list value is set as a single
comma-separated value via an environment variable or system property, e.g.
`SIT_ALLOWEDBLOCKS=minecraft:oak_stairs,minecraft:spruce_stairs`.

A navigator entry is a named map entry rather than a plain record field, so its keys follow the
pattern `NAVIGATOR_ENTRIES_<NAME>_<FIELD>`, `<NAME>` being the entry's map key, upper-cased - e.g.
`navigator.entries.survival.destination` becomes `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION`.

An invalid value - from `application.yaml`, a profile or an override - aborts startup with a
message naming the module, the field and the reason (e.g. a negative cooldown, or `minHeight` not
less than `maxHeight`). A section with a key its module's config record does not declare produces
one warning per section at startup and is otherwise ignored - the lobby still starts.

### Sections and keys

```yaml
spawn:
  minHeight: -64
  maxHeight: 310
  simulationDistance: 2

sit:
  offset:
    x: 0.5
    y: 0.25
    z: 0.5
  allowedBlocks:
    - minecraft:spruce_stairs

tickle:
  cooldownMillis: 4000

elytra:
  burnDurationTicks: 30
  cooldownTicks: 40

navigator:
  title: "<yellow>Navigator"
  entries:
    elytrarace:
      slot: 0
      icon: minecraft:elytra
      displayName: "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>"
      destination: ElytraRace
    survival:
      slot: 4
      icon: minecraft:grass_block
      displayName: "<!i><green>Survival"
      destination: Survival
    slender:
      slot: 5
      icon: minecraft:enderman_spawn_egg
      displayName: "<!i><gradient:#616161:#e80000c>Slender</gradient>"
      destination: cygnus
      feature: NAVIGATOR_SLENDER
    creative:
      slot: 8
      icon: minecraft:wooden_axe
      displayName: "<!i><rainbow>Creative</rainbow>"
      destination: MemberBuild
```

### Configuration Options Explained

- `spawn.minHeight` / `spawn.maxHeight`: height bounds a player is teleported back to spawn outside
  of
- `spawn.simulationDistance`: simulation distance sent to a player on spawn - also the only key the
  setup server reads (see "Setup server" below)
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
- `navigator.entries`: a map of the navigator's destinations, keyed by a unique name (e.g.
  `survival`) so a profile or an override can change a single entry without repeating the others;
  each entry has a hotbar-chest slot (`0`-`8`), an icon material key, a MiniMessage display name
  and the CloudNet task name a click delivers the player to
- `navigator.entries.<name>.feature` (optional): the name of a `TitanFeatures` feature flag this
  destination is gated behind, e.g. `NAVIGATOR_SLENDER`. Omitted, the destination is always
  visible. A name Togglz does not recognize aborts startup with a message naming
  `navigator.entries` and the unknown name. A flag missing from `flags.properties` counts as
  **off** - Slender, for example, stays hidden until `NAVIGATOR_SLENDER` is explicitly turned on.
  Toggling a flag takes effect the next time a player opens the navigator, with no restart.

### Environment variable reference

| Key | Environment variable |
| --- | --- |
| `spawn.minHeight` | `SPAWN_MINHEIGHT` |
| `spawn.maxHeight` | `SPAWN_MAXHEIGHT` |
| `spawn.simulationDistance` | `SPAWN_SIMULATIONDISTANCE` |
| `sit.offset.x` | `SIT_OFFSET_X` |
| `sit.offset.y` | `SIT_OFFSET_Y` |
| `sit.offset.z` | `SIT_OFFSET_Z` |
| `sit.allowedBlocks` (comma-separated) | `SIT_ALLOWEDBLOCKS` |
| `tickle.cooldownMillis` | `TICKLE_COOLDOWNMILLIS` |
| `elytra.burnDurationTicks` | `ELYTRA_BURNDURATIONTICKS` |
| `elytra.cooldownTicks` | `ELYTRA_COOLDOWNTICKS` |
| `navigator.title` | `NAVIGATOR_TITLE` |
| `navigator.entries.<name>.slot` | `NAVIGATOR_ENTRIES_<NAME>_SLOT` |
| `navigator.entries.<name>.icon` | `NAVIGATOR_ENTRIES_<NAME>_ICON` |
| `navigator.entries.<name>.displayName` | `NAVIGATOR_ENTRIES_<NAME>_DISPLAYNAME` |
| `navigator.entries.<name>.destination` | `NAVIGATOR_ENTRIES_<NAME>_DESTINATION` |
| `navigator.entries.<name>.feature` | `NAVIGATOR_ENTRIES_<NAME>_FEATURE` |

`<NAME>` is the entry's map key, upper-cased - e.g. `navigator.entries.survival.destination`
becomes `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION`. The default entries are `elytrarace`,
`survival`, `slender` and `creative`.

### Migrating from app.json

An old `app.json` (flat or already sectioned) is switched over automatically, once, the next time
the lobby starts and finds no `application.yaml` yet:

- the values are written to a new `application.yaml`,
- `app.json` is renamed to `app.json.migrated`,
- keys with no home anymore (`updateRateAgones`, `fireworkBoostSlot`, `elytraBoostMultiplier`) are
  dropped and named in the log,
- `navigator.entries` (a list in `app.json`) becomes a map, each entry named after its
  `displayName` (MiniMessage tags stripped, lower-cased, only letters and digits kept; a name that
  would be empty or is already taken falls back to `slot<N>`/`name-<N>`, `N` being the entry's
  slot),
- a WARN line names the switch and the renamed file.

If both `app.json` and `application.yaml` already exist, the lobby only reads `application.yaml`
and warns that `app.json` is ignored - it is never touched in that case. If `app.json` is not
valid JSON, startup aborts and nothing is renamed or written.

**Rollback:** deploy the previous jar and rename `app.json.migrated` back to `app.json`.

### Setup server

The setup server no longer edits configuration - the `/setup app ...` commands have been removed.
It only reads `spawn.simulationDistance` (default `2`) from the same configuration, and logs a
warning if it finds an `app.json` but no `application.yaml` yet (the lobby migrates on its own next
start, not the setup server).

### Deployment

A CloudNet template, a Docker image or a Kubernetes deployment delivers `application.yaml` (or an
external file referenced via `CONFIG_FILE`) into the working directory and sets `AVAJE_PROFILES`
for the environment it runs in.

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
