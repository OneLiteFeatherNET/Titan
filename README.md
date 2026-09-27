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
3. The server runs with every module's shipped defaults if no configuration file is present; copy
   `application.example.yaml` from the distribution (next to the jar) to `application.yaml` and
   edit it to customize (see Configuration below)

## Running the Server

Once installed, you can:

- Start the server with additional memory: `java -Xmx2G -jar titan-x.x.x.jar`
- Use the console to manage the server while it's running
- Stop the server safely by typing `stop` in the console

### Server Properties

You can configure server properties like port, MOTD, and more in the generated configuration files.

## Configuration

Configuration lives in `application.yaml` in the lobby's working directory (next to the jar), one
named section per lobby feature module, keys following the `<module-id>.<field>` schema. The
shipped defaults live inside the jar; a commented `application.example.yaml` listing every section
and key with its default also ships in the distribution, next to the jar - copy it to
`application.yaml` and edit only the values that should differ. A missing file, a missing section
or a missing key falls back to the shipped default, listed below. The lobby never creates or writes
a configuration file itself.

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

1. the shipped default,
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

An invalid value - from `application.yaml`, a profile or an override - aborts startup with a
message naming the full key (`<module-id>.<field>`) and the reason (e.g. a negative cooldown, or
`spawn.minHeight` not less than `spawn.maxHeight`). An unknown or misspelled key is no longer
reported - it is silently ignored, and the lobby starts using the shipped default for that key.

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

features:
  NAVIGATOR_CREATIVE: false
  NAVIGATOR_SLENDER: false
  NAVIGATOR_MANIS: false
  NAVIGATOR_SURVIVAL: false
  NAVIGATOR_ELYTRA: false
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
- `features`: plain booleans, one per feature flag, with the same sources and override order as
  every other key (see "Feature flags" below).

The navigator - a feather in hotbar slot 4 opening a shared inventory with ElytraRace, Survival,
Slender and Creative - has no configuration section: its title and destinations are fixed in code
(`NavigatorModule`/`Destination`), not read from `application.yaml`. A `navigator.*` key set here
or anywhere else has no effect. Only Slender is gated behind a flag, `features.NAVIGATOR_SLENDER`
(see "Feature flags" below) - toggling it takes effect the next time a player opens the navigator,
with no restart of the navigator or the lobby.

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
| `features.<NAME>` | `FEATURES_<NAME>` |

`<NAME>` is a feature flag's own name, upper-cased - e.g. `features.NAVIGATOR_SLENDER` becomes
`FEATURES_NAVIGATOR_SLENDER`. There is no environment variable for the navigator's title or
destinations - they are fixed in code, not configuration (see "Configuration Options Explained"
above).

## Runtime reloading

The lobby can pick up a change to its configuration files without a restart, using avaje-config's
own built-in file watcher. It is off by default; an operator turns it on in their own
working-directory `application.yaml`, an active profile's `application-<profile>.yaml`, or the
file selected via `CONFIG_FILE`/`config.file`, with:

```yaml
config.watch.enabled: true
```

Once enabled, it watches only the configuration files that already existed on disk at startup - a
file created afterwards is picked up only on the next restart. Every `config.watch.period` seconds
(default `10`, the first check happening `config.watch.delay` seconds after startup, also default
`10`) it re-reads every watched file and applies whatever changed behind the
`io.avaje.config.Config` facade.

**No module, and no part of the lobby, ever restarts to pick up a change.** Instead, every module
reads its settings live, at the moment it needs them, rather than once at startup: tickle reads
`tickle.cooldownMillis` on every attack, sit reads `sit.offset.*`/`sit.allowedBlocks` on every block
interaction, elytra reads `elytra.burnDurationTicks`/`elytra.cooldownTicks` on every boost, spawn
reads `spawn.minHeight`/`spawn.maxHeight` on every height check and `spawn.simulationDistance` on
every join, and the navigator evaluates `features.NAVIGATOR_SLENDER` every time it is opened - its
title and destinations are otherwise fixed in code, not read from configuration at all.
Once the watcher applies a change behind the facade, the very next such read sees the new value.
Because nothing restarts, no in-flight, per-player state is ever lost - a player who is already
sitting stays sitting even if `sit.offset.*` changes underneath them, and a player mid-elytra-boost
keeps that boost even if `elytra.burnDurationTicks`/`elytra.cooldownTicks` change; the new value
only applies the next time each is used.

Configuration is validated only once, at startup - an invalid value found there still aborts the
start, unchanged from before. A live read, at the point of use, is never re-validated and never
falls back to a shipped classpath default: an invalid value simply takes effect (e.g. a negative
`tickle.cooldownMillis`) until an operator corrects the file - validate a value before saving it.
If a watched file is not valid YAML after a change, avaje-config itself logs the
file and the location of the error at ERROR (over `java.util.logging`, not the lobby's own
SLF4J-backed logs) and applies no value from it; every other changed file is still applied.

**Accepted limits of this built-in watcher** (see `design.md`, decision 1, in
`openspec/changes/config-reload-feature-flags`):

- an environment variable or system property override for a key is displaced by a changed file's
  value for that same key, until the lobby is next restarted;
- a key deleted from a changed file stays active with its old value until the next restart;
- a file created after startup is only picked up on the next restart;
- there is no manual trigger - a change takes effect only once the watcher notices it, at most
  `config.watch.delay` plus `config.watch.period` after it was made;
- a syntactically broken file is logged by avaje-config itself over `java.util.logging`, not
  SLF4J, so that ERROR line may not appear alongside the rest of the lobby's own logs.

## Feature flags

Feature flags are plain booleans under the `features` section, one per flag name (e.g.
`features.NAVIGATOR_SLENDER: true`), with the same sources and override order as every other
configuration key - a profile's file, an external file, an environment variable
(`FEATURES_NAVIGATOR_SLENDER`), or a system property. The five flags the lobby ships with, all
`false` by default: `NAVIGATOR_CREATIVE`, `NAVIGATOR_SLENDER`, `NAVIGATOR_MANIS`,
`NAVIGATOR_SURVIVAL` and `NAVIGATOR_ELYTRA`. Only `NAVIGATOR_SLENDER` currently gates anything -
the navigator's Slender destination. Changing its value takes effect the next time a player opens
the navigator, without restarting any module or the lobby.

### Migrating from `flags.properties`

`flags.properties` and Togglz are no longer read. Move every line over by hand:

| `flags.properties` | `application.yaml` | or environment variable |
| --- | --- | --- |
| `NAME=true` | `features.NAME: true` | `FEATURES_NAME=true` |

Remove `flags.properties` from the working directory once its values are migrated - a leftover
copy has no effect any more.

### Local testing with every flag on

For local testing, create an `application-local.yaml` next to `application.yaml` with every flag
turned on. It also turns on the file watcher from "Runtime reloading" above, so a flag flipped
back off in the file takes effect without a restart while testing:

```yaml
features:
  NAVIGATOR_CREATIVE: true
  NAVIGATOR_SLENDER: true
  NAVIGATOR_MANIS: true
  NAVIGATOR_SURVIVAL: true
  NAVIGATOR_ELYTRA: true
config.watch.enabled: true
```

Activate the `local` profile with the environment variable `AVAJE_PROFILES=local` or the system
property `-Davaje.profiles=local` (see "Profiles and overrides" above).

### Upgrading from an app.json-based release

`app.json` is no longer read or converted. Before upgrading a server that still has one, either
start the previous release once - it switches `app.json` over to `application.yaml` on its own, as
described in that release's docs - or transfer the values by hand into a new `application.yaml`
(same sections and keys as before). A leftover `app.json` or `app.json.migrated` next to the jar is
ignored and does not affect startup; once `application.yaml` is in place, either file can be
deleted.

### Setup server

The setup server no longer edits configuration - the `/setup app ...` commands have been removed.
It only reads `spawn.simulationDistance` (default `2`) from the same configuration.

### Deployment

A CloudNet template, a Docker image or a Kubernetes deployment delivers `application.yaml` (or an
external file referenced via `CONFIG_FILE`) into the working directory and sets `AVAJE_PROFILES`
for the environment it runs in.

Before rolling this change out to an existing deployment, migrate every `flags.properties` line to
`features.*` in `application.yaml` or to an environment variable, as described in "Migrating from
`flags.properties`" above, then remove `flags.properties` from the template. Add
`config.watch.enabled: true` to the deployment's own `application.yaml`/profile file/`CONFIG_FILE`
if it should pick up configuration changes without a restart.

After rolling out, the start log's "Active configuration profiles" line confirms which profiles are
active. If the file watcher is enabled, changing a watched key and waiting up to
`config.watch.delay` plus `config.watch.period` confirms the reload works: exercising the affected
module afterwards (e.g. triggering a tickle attack after changing `tickle.cooldownMillis`) shows
the new value took effect, with no restart of any kind appearing in the log. **Rollback:** deploy
the previous jar and restore `flags.properties` - a leftover `features.*` section or
`config.watch.*` setting in `application.yaml` does not affect the previous jar.

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

### Adding a Lobby Feature

A lobby feature is a self-contained package under
`app/src/main/java/net/onelitefeather/titan/app/feature/<name>/`, discovered automatically by
Avaje Inject - there is no central feature list to edit:

- New package, copied from the template feature at
  `app/src/test/java/net/onelitefeather/titan/app/feature/example/` (`ExampleModule` and friends).
- The `<Name>Module` class is a plain `@jakarta.inject.Singleton` bean with a unique
  `static final int EVENT_PRIORITY` - it decides the order in which two features process the same
  event, not a start order; the seven existing features use gaps of 100 (protection 100, spawn 200,
  respawn 300, navigator 400, sit 500, tickle 600, elytra 700). A class with an `@PostConstruct`
  method that is missing `@Singleton`, or two features sharing an `EVENT_PRIORITY`, fails the build
  (ArchUnit), not just the running lobby. `@PostConstruct start()` attaches the feature's own
  `FeatureNode`; `@PreDestroy stop()` detaches it again.
- Dependencies (a platform service such as `Deliver`, an `Instance`, a `Clock`, the `Scheduler`,
  ...) are requested through the constructor; `@jakarta.inject.Inject` is only needed on a
  constructor when the class has more than one. A brand-new shared platform service is added as
  another `@Bean` in `app/src/main/java/net/onelitefeather/titan/app/bootstrap/PlatformBeans.java`,
  or, if it carries feature-spanning logic of its own rather than wrapping a platform type, as its
  own `@Singleton` class.
- A hotbar or equipment item is a `@Bean LobbyItem` from the feature's own, package-private
  `@Factory` class, collected by the platform-wide `LobbyItems` bean.
- Zero changed lines outside the new package - except a brand-new shared platform service, which
  necessarily touches `PlatformBeans`.
- A dependency nothing provides fails building the `BeanScope` (and with it, the lobby's start),
  naming the missing type, instead of the lobby quietly running without that feature.
- A feature that reads configuration reads it live, at the point it is used, not just once in
  `start()` - see [`docs/lobby-modules.md`](docs/lobby-modules.md) for the pattern (a direct
  `Config.<method>(key)` call at the use site, unvalidated - configuration is validated only once,
  at startup). That is what makes the runtime reload described under "Runtime reloading" above
  apply to a feature without it ever restarting.

See [`docs/lobby-modules.md`](docs/lobby-modules.md) (German) for the full walkthrough - feature
anatomy, `FeatureNode`, items and tasks as beans, tick-thread rules, test setup without a harness,
the ArchUnit rules, and a copyable template feature with its tests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Credits

Developed by OneLiteFeather Network.
