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
   chunk at boot. Instead `MapProvider` relights each chunk as it is loaded, so
   everything is lit by the time it is visible — no dark areas, no visible
   reveal.
2. **Special maps = deliberate darkening + per-player reveal (later).** Some
   maps should be **deliberately dark** and only light up per player as they
   explore (fog-of-war). This reuses the "anvil chunk stays dark until relit"
   behaviour on purpose, gated per map, and is layered per player.

## Current state (lobby, instance-level)

`MapProvider` does an **instance-wide** relight:

- The world instance uses `LightingChunk` (`instance.setChunkSupplier(LightingChunk::new)`).
- An `InstanceChunkLoadEvent` listener relights every chunk as it is loaded
  (`LightingChunk.relight(instance, List.of(chunk))`), because anvil chunks
  otherwise stay dark until a block update triggers a relight.
- Lobby time is frozen at midday (`setTime(6000)` + `setTimeRate(0)`) so lit
  chunks render bright.

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
4. **Real light source.** The instance still computes correct light via
   `LightingChunk` (the global relight above); the per-player layer only chooses
   whether to send the real light or a dark override to each player.

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
- **Cross-version time API:** time is frozen with the legacy
  `setTime`/`setTimeRate`; newer Minestom replaces this with the world
  `Clock` API (`instance.defaultClock().rate(0f)`) — migrate when Minestom is
  bumped.

## Next steps

1. Add a **per-map "dark/reveal" flag** (e.g. in the map metadata / `AppConfig`)
   so the lobby stays fully lit (mode 1) while special maps opt into deliberate
   darkening (mode 2). Lit maps skip the dark override entirely.
2. Settle the discovery trigger + reveal radius and where it is configured.
3. Add per-player discovered-chunk tracking on `TitanPlayer`.
4. Implement the dark-light override on chunk send + reveal `UpdateLightPacket`
   on discovery (only for maps with the dark/reveal flag set).
5. Keep the instance-level `LightingChunk` relight as the real-light source.
