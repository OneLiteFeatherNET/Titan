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

A navigator entry is a named map entry rather than a plain record field, so its keys follow the
pattern `NAVIGATOR_ENTRIES_<NAME>_<FIELD>`, `<NAME>` being the entry's map key, upper-cased - e.g.
`navigator.entries.survival.destination` becomes `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION`.

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
- `navigator.title`: the shared navigator inventory's title, as a MiniMessage string
- `navigator.entries`: a map of the navigator's destinations, keyed by a unique name (e.g.
  `survival`) so a profile or an override can change a single entry without repeating the others;
  each entry has a hotbar-chest slot (`0`-`8`), an icon material key, a MiniMessage display name
  and the CloudNet task name a click delivers the player to
- `navigator.entries.<name>.feature` (optional): the name of a flag from the `features` section
  below this destination is gated behind, e.g. `NAVIGATOR_SLENDER`. Omitted, the destination is
  always visible. A name that is not one of the shipped `features` keys aborts startup with a
  message naming `navigator.entries` and the unknown name. A flag not set anywhere counts as
  **off** - Slender, for example, stays hidden until `NAVIGATOR_SLENDER` is explicitly turned on.
  Toggling a flag takes effect the next time a player opens the navigator, with no restart of the
  navigator or the lobby.
- `features`: plain booleans, one per feature flag, with the same sources and override order as
  every other key (see "Feature flags" below).

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
| `features.<NAME>` | `FEATURES_<NAME>` |

`<NAME>` is the entry's map key, upper-cased - e.g. `navigator.entries.survival.destination`
becomes `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION`. The default entries are `elytrarace`,
`survival`, `slender` and `creative`. The same rule applies to a feature flag's own name, e.g.
`features.NAVIGATOR_SLENDER` becomes `FEATURES_NAVIGATOR_SLENDER`.

## Runtime reloading

The lobby notices a change to its configuration files without a restart. It polls
`application.{yaml,yml,properties}` in the working directory, the same three names per active
profile (e.g. `application-dev.yaml`), and the file selected via `CONFIG_FILE`/`config.file` - none
of these need to exist yet - for a changed existence, modification time or size, at the interval
set by `titan.config.reload.intervalSeconds` (default `10` seconds, `0` turns polling off). The
interval is read once at startup; changing it in a running lobby has no effect until the next
restart. `config.watch.enabled`, avaje-config's own built-in file watcher, is deliberately left
`false` (see the comment next to it in `application.yaml`): on reload it does not re-apply
environment/system-property overrides, leaves a deleted key in place, never notices a newly
created file, and partially applies a change when one of several files is broken. The lobby's own
reloader rebuilds the whole configuration through the same pipeline used on startup instead, so
overrides, deleted keys and new files all behave exactly like a fresh start.

The command `/titanreload` reloads immediately instead of waiting for the next poll. It requires
the permission `titan.command.reload`; the console may always run it, and a player without the
permission cannot see or run it at all (it never appears in their tab-completion). Grant the
permission to the admin group in LuckPerms as part of rolling this out.

A reload only restarts the module whose own section (`<module-id>.*`) actually changed a key -
every other module keeps running untouched. A change under `features.*`, `titan.*` or `config.*`
restarts no module at all. Restarting a module briefly tears it down and brings it back up, so any
state that lives only in that module's memory is lost for players who were mid-interaction with
it - a player sitting down stands back up, and a player mid-elytra-boost loses the tracked boost
- while their hotbar items and navigator entries are put back automatically. Only modules whose
keys actually changed are restarted, so this only affects a module an operator is actively
reconfiguring.

If the new value for a key is invalid, the lobby discards it for that module only: the module
keeps running with its previous values, the log names the key and the reason at WARN, and the
`/titanreload` reply lists the rejected key. If the file itself is not valid YAML after a change,
the whole reload is discarded - no key changes at all - and the reply names the file and the
location of the error. An environment variable or system property override still wins after a
reload, exactly as it does on startup.

## Feature flags

Feature flags are plain booleans under the `features` section, one per flag name (e.g.
`features.NAVIGATOR_SLENDER: true`), with the same sources and override order as every other
configuration key - a profile's file, an external file, an environment variable
(`FEATURES_NAVIGATOR_SLENDER`), or a system property. The five flags the lobby ships with, all
`false` by default: `NAVIGATOR_CREATIVE`, `NAVIGATOR_SLENDER`, `NAVIGATOR_MANIS`,
`NAVIGATOR_SURVIVAL` and `NAVIGATOR_ELYTRA`. A flag not listed in the shipped defaults is unknown -
a navigator entry naming it aborts startup, and changing a navigator entry to name it while
running is discarded on reload instead. Changing a flag's value takes effect the next time the
navigator is opened, without restarting any module or the lobby.

### Migrating from `flags.properties`

`flags.properties` and Togglz are no longer read. Move every line over by hand:

| `flags.properties` | `application.yaml` | or environment variable |
| --- | --- | --- |
| `NAME=true` | `features.NAME: true` | `FEATURES_NAME=true` |

Remove `flags.properties` from the working directory once its values are migrated - a leftover
copy has no effect any more.

### Local testing with every flag on

For local testing, create an `application-local.yaml` next to `application.yaml` with every flag
turned on:

```yaml
features:
  NAVIGATOR_CREATIVE: true
  NAVIGATOR_SLENDER: true
  NAVIGATOR_MANIS: true
  NAVIGATOR_SURVIVAL: true
  NAVIGATOR_ELYTRA: true
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

Before rolling this change out to an existing deployment:

1. migrate every `flags.properties` line to `features.*` in `application.yaml` or to an
   environment variable, as described in "Migrating from `flags.properties`" above, then remove
   `flags.properties` from the template,
2. grant the `titan.command.reload` permission to the admin group in LuckPerms.

After rolling out, changing a key and either waiting for the next poll or running `/titanreload`
confirms the reload works; the start log's "Active configuration profiles" line confirms which
profiles are active. **Rollback:** deploy the previous jar and restore `flags.properties` - a
leftover `features.*` section in `application.yaml` does not affect the previous jar.

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
- A module needs nothing extra to support the runtime reload described under "Runtime reloading"
  above: as long as everything it registers goes through `ModuleContext` (`listen`, `items`,
  `navigator`, `commands`, `tasks`), a restart tears it down and brings it back up the same way
  startup and shutdown already do.

See [`docs/lobby-modules.md`](docs/lobby-modules.md) (German) for the full walkthrough - module
anatomy, `ModuleContext` dock points, tick-thread rules, test setup with `ModuleHarness`, the
ArchUnit rules, and a copyable template module with its tests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Credits

Developed by OneLiteFeather Network.
