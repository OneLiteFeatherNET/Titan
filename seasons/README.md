# Seasons

One file per season, read at startup from this directory (next to the running
process, alongside `worlds/` and `app.json`). Adding a season is adding a file
and, if it wants its own lobby, a world directory under `worlds/`. It is never
adding Java — if it ever is, the design in `docs/spec-lobby-saison-events.md`
stage 4 has failed and the effect belongs in
`net.onelitefeather.titan.common.season.SeasonEffect` first.

`example-lantern-nights.json` is a fixture, not content. It ships with
`"enabled": false` so it never places anything in a real lobby, and exists to be
copied and to be exercised by `SeasonSmokeTest` — which is the point of US-4.08:
code that lies dark for eleven months is exercised by nothing unless something
exercises it deliberately.

## The fields

| Field | Meaning |
|---|---|
| `id` | Lowercase letters, digits, `-` and `_`. Also the name the release gate knows the season by. Must be unique across this directory. |
| `enabled` | The kill switch. Missing means `true`; `false` makes the season invisible whatever its window says. |
| `priority` | Which season wins where two overlap. Higher is applied later and therefore on top. Missing means `0`. |
| `stage` | `internal`, `lite` or `ga` — the audience the season's per-player content is released to. Missing means `internal`, the narrowest. |
| `world` | The directory under `worlds/` this season wants the lobby to load. Optional. |
| `window.from` | Inclusive start, `2026-12-01` or `2026-12-01T18:00`. Optional. |
| `window.to` | Exclusive end, same formats. Optional. |
| `window.zone` | Zone `from` and `to` are read in. Optional, defaults to `Europe/Berlin`. |
| `effects` | What the season does. |

A window may instead name a season — `{"named": "WINTER", "year": 2026}` — once
spec stage 2 installs a resolver for the astronomical boundaries. Until then a
file that does so fails to load with a message saying exactly that, rather than
running all year.

## The effects

| `type` | Fields | What it does | What ending the season undoes |
|---|---|---|---|
| `place_decoration` | `position`, `block` | Puts a block into the world. | Puts back whatever block was read at that position first. |
| `place_display` | `position`, `text` (MiniMessage) | Spawns a floating text display. | Removes the display. |
| `ambient_sound` | `position`, `sound`, `periodSeconds` | Plays a sound on a loop. | Cancels the scheduled task. |
| `replace_icon` | `destination`, `material` | Swaps a navigator icon. | Nothing to undo — computed per viewer, never written down. |
| `message_prefix` | `prefix` (MiniMessage) | Puts a prefix in front of lobby messages. | Nothing to undo — same reason. |

A `type` that is not in this table makes the lobby refuse to start, with the
unknown type and the file name in the message. That is deliberate: a season is
looked at once a year, and a typo that is tolerated at startup is a typo nobody
finds until the season is live.

## Preview

A holder of `titan.season.preview` sees the per-player half — icons and prefixes
— outside the window. Decoration is a block in a world everybody shares, so no
permission can show it to one player; previewing that means opening the window on
a lobby whose release stage keeps it to the team.

## Rollout

Stage changes belong in `docs/rollout-log.md`, like any other feature.
