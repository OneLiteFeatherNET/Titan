# Exploration Lighting (per-player region reveal)

## Idea

Regions of the lobby/world should start **unlit (dark)** for a player and
**light up as that player explores them** — a "fog of war" / map-reveal effect
like in many exploration games. The reveal is **per player**: what one player
has discovered does not affect what another player sees. Each player builds up
their own lit area as they move through the world.

## Two modes

There are two distinct behaviours, and they must not be confused:

1. **Lobby = complete relight (now).** The whole lobby is lit. Maps are large
   (~18–20k populated chunks each), so we do **not** pre-load and relight every
   chunk at boot. Instead `MapProvider` schedules the light of each chunk as it
   is loaded, so everything is lit within a tick or two of becoming visible — no
   dark areas, no visible reveal.
2. **Special maps = deliberate darkening + per-player reveal (later).** Some
   maps should be **deliberately dark** and only light up per player as they
   explore (fog-of-war). This reuses the "anvil chunk stays dark until relit"
   behaviour on purpose, gated per map, and is layered per player.

## Current state (lobby, instance-level)

`MapProvider` lights the world through Falco's `ChunkLightScheduler`:

- The world instance uses `LightingChunk` (`instance.setChunkSupplier(LightingChunk::new)`).
- An `InstanceChunkLoadEvent` listener **marks** every chunk that arrives
  (`ChunkLightScheduler#markChanged`), together with the eight chunks around it.
  It computes nothing itself. Anvil chunks otherwise stay dark until a block
  update triggers a relight.
- An `InstanceTickEvent` listener runs the pass (`ChunkLightScheduler#onTick`).
  The scheduler collects the marks of a tick, groups them into areas that do not
  overlap, and computes each area on a virtual thread. Both halves of that
  matter:
  - **The marks reach the neighbours**, so the chunk that loaded first — lit
    with nothing beside it — is lit again when its neighbour turns up. Lighting
    a chunk once, inline, cannot do that: the write goes through `Light#set`,
    which clears the update flag of the section, so nothing would ever look at
    that chunk again and the border would stay dark permanently.
  - **The areas do not overlap**, so no two threads light chunks against each
    other. `FalcoAnvilLoader#supportsParallelLoading()` is `true`, which makes
    several adjacent chunks arriving at once the normal case rather than a
    corner one, and Falco's own javadoc is explicit that lighting overlapping
    neighbourhoods concurrently produces a seam — never an error, and permanent
    for the same reason as above.
- **The scheduler owns the sky pass too** (`SkyLight.FROM_DIMENSION`, so it runs
  in a dimension that has sky light and not in one that does not). This is not
  optional: a fresh Minestom `Light` reports itself as *valid*
  (`isValidBorders = true`), and `LightingChunk` only relights a section whose
  `requiresUpdate()` is true — so a section whose region file carries no
  `SkyLight` array would stay at level 0 for the lifetime of the server.
  Reading `SkyLight` out of the NBT sets the same flag, so the region file is
  not a fallback either.
- `LightingChunk` stays the chunk type for the one thing it does here: sending
  the light to the players. The scheduler drops the cached packets of every
  chunk it wrote, which covers whoever receives the chunk next; the resend timer
  for players who already hold it is armed from the completion callback of the
  pass.
- Lobby time is frozen at midday (`setTime(6000)` + `defaultClock().rate(0)`) so
  lit chunks render bright.

This makes the lobby fully lit (mode 1). The lighting is **shared**: a chunk is
lit once for everyone. It is also the foundation for mode 2 — the per-player
reveal just chooses whether to send the real light or a dark override per
player.

## Goal: per-player reveal

Each player has their own set of **discovered chunks**. A chunk a player has
not discovered yet is shown **dark** to them; once they discover it, it is
shown with its real (relit) light — and only for that player.

### Discovery trigger (to decide)

A chunk becomes "discovered" for a player when, e.g.:

- the player enters the chunk, **or**
- the player comes within `N` blocks / a configurable reveal radius, **or**
- the chunk enters line-of-sight.

Reveal radius and trigger should be configurable (likely via `AppConfig` /
a Togglz feature flag, consistent with `TitanFeatures`).

### Technical approach in Minestom

Minestom stores light per chunk in the instance (shared). To make it
per-player we override the light **on the wire**, per player:

1. **Track discovered chunks per player** — e.g. a `Set<Long>` of packed chunk
   keys on the player (custom `TitanPlayer` already exists), persisted per
   session (and optionally to storage for permanent reveal).
2. **Send dark light for undiscovered chunks.** When a chunk is sent to a
   player who has not discovered it, send the chunk with zeroed sky/block light
   (a "dark" `UpdateLightPacket` / chunk-data light section) instead of the
   real light.
3. **Reveal on discovery.** When the discovery trigger fires for a player +
   chunk, mark it discovered and send that player an `UpdateLightPacket` with
   the chunk's real (relit) light. Optionally animate the reveal by revealing
   neighbouring chunks outward.
4. **Real light source.** The instance still computes correct light through the
   Falco scheduler (see above); the per-player layer only chooses whether to
   send the real light or a dark override to each player.

### Open questions / challenges

- **Packet interception:** Minestom sends chunk data + light when a chunk
  enters a player's view. We need a hook to substitute dark light for
  undiscovered chunks (custom chunk-send path, or post-send dark
  `UpdateLightPacket`, or a per-player light override layer).
- **Border seams:** skylight propagates across chunk borders; revealing a
  single chunk while neighbours are dark may show hard edges. Reveal in small
  batches / with a radius to soften.
- **Performance:** per-player light packets scale with players × chunks; cache
  the "dark" light payload and only compute real light once (shared) + resend.
- **Persistence:** decide whether discovered regions persist across sessions
  (per-player save) or reset on rejoin.
- **Cross-version time API:** the rate is already frozen through the world
  `Clock` API (`instance.defaultClock().rate(0f)`); the time itself is still set
  through the legacy `Instance#setTime`.

## Next steps

1. Add a **per-map "dark/reveal" flag** (e.g. in the map metadata / `AppConfig`)
   so the lobby stays fully lit (mode 1) while special maps opt into deliberate
   darkening (mode 2). Lit maps skip the dark override entirely.
2. Settle the discovery trigger + reveal radius and where it is configured.
3. Add per-player discovered-chunk tracking on `TitanPlayer`.
4. Implement the dark-light override on chunk send + reveal `UpdateLightPacket`
   on discovery (only for maps with the dark/reveal flag set).
5. Keep the instance-level lighting (Falco block **and** sky light,
   `LightingChunk` for sending) as the real-light source.
