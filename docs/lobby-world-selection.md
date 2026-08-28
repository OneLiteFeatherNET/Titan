# Lobby worlds: layout and selection

How the lobby decides which world it serves, and what a world directory has to
look like for it to be servable. Covers US-1.01 to US-1.06 of
[`spec-lobby-saison-events.md`](spec-lobby-saison-events.md).

## One directory per world (US-1.06)

Every world is a directory below `worlds/`, next to the process:

```
worlds/
  world/            <- the default world, used when nothing else is selected
  halloween/
  winter/
```

The name of the directory is the name of the world. Nothing in the code knows
these names — adding a season means adding a directory and setting one property,
never a code change.

A directory only counts as a world if it carries the map data file
(`AppConfig.MAP_FILE_NAME`, `map.json`) — that is what `:app` filters on. The
`:setup` module accepts every directory, because that is where a new world gets
its map data in the first place.

Inside the directory, Falco's `FalcoAnvilLoader` looks for the region files in
this order:

1. `worlds/<name>/dimensions/<namespace>/<value>/region/` — the 26.1 layout,
   with the dimension of the instance filled in (`minecraft/overworld` for the
   lobby)
2. `worlds/<name>/region/` — the older layout, used when the directory above
   does not exist

The lobby worlds are in the older layout, so the fallback is the path that is
actually taken today. `level.dat` is not read; a directory holding only region
files is enough. Upgrading a world to a newer Minecraft version is a separate
job, described in [`world-conversion.md`](world-conversion.md).

## Selecting the active world (US-1.04, US-1.05)

The active world is named by the system property `TITAN_LOBBY_MAP`. It defaults
to `world`:

```bash
java -DTITAN_LOBBY_MAP=halloween -jar app-titan.jar
```

The `:setup` module wires `-DTITAN_LOBBY_MAP=halloween` into its
`applicationDefaultJvmArgs`, so a setup run edits the Halloween world unless it
is told otherwise.

The property is evaluated no matter how many worlds are present. That is worth
stating because it used to not be true: with exactly one world below `worlds/`,
the old code took that world and never looked at the property, which made a
machine with one world behave differently from a machine with three.

**A world that is named but not present is not fatal.** The lobby logs a warning
that carries both halves needed to spot a typo — the name that was searched for
and the names that were found — and starts with the default world `world`
instead:

```
The world 'halloewen' named by the system property TITAN_LOBBY_MAP does not
exist. Found worlds: world, winter. Falling back to the default world 'world'.
```

If the default world is missing as well, the first world that was found is used,
and the same warning says so instead — it names the world that was really taken:

```
The world 'halloween' named by the system property TITAN_LOBBY_MAP does not
exist. Found worlds: winter. Falling back to the world 'winter', because the
default world 'world' is not there either.
```

Only a `worlds/` directory without any world at all stops the start, because
there is then nothing left to serve.

Whether the requested world was the one that got selected is readable from
`MapPool#isRequestedMapSelected()`, so the fallback is observable from code and
not only from the log.

## Which engine serves the chunks (US-1.01, US-1.02, US-1.03)

`MapProvider` installs `net.onelitefeather.falco.anvil.FalcoAnvilLoader` as the
chunk loader of the lobby instance, not Minestom's `AnvilLoader`. The reason is
narrower than "it is ours": Minestom's loader reports a chunk it cannot read as
absent, the server then generates a fresh chunk in its place, and the next save
writes that over the built world. Falco's loader reports the failure instead.

The loader keeps region files open for as long as it lives, so it is created
once per world root and closed on shutdown through `MapProvider#close()` — in
`:app` and in `:setup`, which is the module that actually changes worlds and
therefore the one that must not drop a region handle unflushed. Closing also
takes the closed loader off the instance and removes the listeners of the
provider, so a player who is still moving during the shutdown gets an empty
chunk rather than the `IllegalStateException` of a loader that was closed
underneath them. A closed provider refuses `saveMap`; it does not build a fresh
loader and reopen what the shutdown has just closed.

Light — block **and** sky — is computed by `falco-light`, driven by a
`ChunkLightScheduler`: a chunk that arrives is marked together with the eight
around it, and the pass runs once per instance tick over areas that do not
overlap. That is what keeps the chunk which loaded first from staying dark along
the border once its neighbours arrive, and what keeps two parallel loads from
lighting each other's chunks. `LightingChunk` stays the chunk type of the
instance for the part Falco does not do: sending the light. See
[`exploration-lighting.md`](exploration-lighting.md) for the mechanics and for
what this is the foundation of.
