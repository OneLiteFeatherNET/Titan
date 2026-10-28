# Proposal

## Why

Im Radio-Menü hat jeder MIDI-Titel im Tooltip drei Zeilen: unser Name, die Beschreibung der Vanilla-Schallplatte („C418 - cat“, aus dem Bauteil `jukebox_playable`) und unsere Lore („DJ Bjra ft. Julia (fan transcription) · Own song“). Die mittlere Zeile stammt nicht von uns und passt zu einem eigenen Stück nicht. Die Lore wiederum mischt den Original-Künstler und die Herkunft der MIDI-Datei in eine Zeile. Wer die Noten gebaut hat, sieht der Spieler nicht.

## What Changes

- **Plattenbeschreibung entfällt:** Song-Gegenstände und „Läuft jetzt“ verlieren das Bauteil `jukebox_playable`. Der Tooltip zeigt nur Name und Lore.
- **Zwei Herkunftszeilen für MIDI-Songs:** „Original: <artist>“ und, wenn in `songs.yaml` ein Feld `midi` steht, „MIDI: <midi>“. Ohne `midi` fehlt die MIDI-Zeile.
- **Zusatz „Eigener Song“ entfällt:** Die MIDI-Zeile nennt die Herkunft. Ein Song ohne `midi` zeigt keinen Zusatz mehr; seine Kategorie im Menü bleibt sichtbar.
- **Vanilla-Platten unverändert in der Herkunft:** Die Lore zeigt den Original-Künstler („C418 · Schallplatte“), keine MIDI-Zeile.
- **Neues optionales Feld `midi` in `songs.yaml`:** Wer das MIDI gemacht hat oder woher es stammt. Das Feld `artist` bleibt der Original-Künstler.
- **Texte:** neuer Schlüssel `titan.radio.disc.lore.original`; `titan.radio.disc.lore.midi` zeigt nun „MIDI: <arg:0>“. Beide in `messages_de.properties` und `messages_en.properties`.
- **Dokumentation:** `docs/lobby-modules.md`, Abschnitt Radio, mit dem Feld `midi`.

Unverändert bleiben: Laden und Prüfen der Songs, Kategorien, Stoppgründe, Telemetrie, Lautstärke und die private Wiedergabe.

Nicht Teil dieser Änderung:

- Die Zeile „minecraft:music_disc_cat / N component(s)“ im erweiterten Tooltip des Clients (F3+H) ist clientseitig und bleibt.
- Keine Pflicht für `midi`; bestehende `songs.yaml` werden nicht geändert.
- Kein Resource Pack und keine Änderung an den Vanilla-Plattentexten.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-radio`: Drei neue Anforderungen zu Lore und Tooltip der Song-Gegenstände und zum Feld `midi`. Das Delta steht in `specs/lobby-radio/spec.md` und ist gegen den Stand nach `radio-track-octaves` geschrieben. Die Archivreihenfolge ist `lobby-radio`, `radio-categories`, `radio-instrument-registers`, `radio-track-octaves`, dann diese Änderung.

## Impact

- **Code:** `features/radio`: `MidiSong` (neues Feld), `SongCatalog` (liest `midi`), `RadioMenuBuilder` (Lore und Bauteil), `RadioMessages` und die Textdateien.
- **Tests:** `SongCatalogTest` und ein Test auf den gebauten Gegenstand (`ItemStack`), beide F.I.R.S.T.
- **Konfiguration:** neues optionales Feld `midi`. Ohne das Feld ändert sich nichts außer dem Tooltip.
- **Nicht brechend:** Bestehende `songs.yaml` bleiben gültig und zeigen nur die Original-Zeile.
- **Nutzertexte:** neue und geänderte Schlüssel in de und en.
- **Dokumentation:** `docs/lobby-modules.md`, Abschnitt Radio.
- **Pull-Request-Vorlage:** `.github/pull_request_template.md` existiert nicht. Die Beschreibung folgt dem Schema Problem/Lösung.

## Type and Scope

Die Umsetzung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): show original artist and midi source instead of the disc description`. Die Änderung ist nicht brechend. Der Code-Pull-Request folgt erst nach dem Merge dieser Vorschlags-PR (`docs(openspec): propose radio-disc-lore`).
