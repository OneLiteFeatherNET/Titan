# Tasks

Zweig für die Umsetzung: `feat/radio-disc-lore`, angelegt vom Vorschlags-Zweig und nach dem Merge der Vorschlags-PR auf `origin/main` gerebased. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, keine Dateien außerhalb von Temp-Verzeichnissen.

## 1. Tests zuerst (rot)

- [ ] 1.1 Unit (`SongCatalogTest`, `@TempDir`): ein Eintrag mit `midi: BitMidi-Upload (unbekannt)` liefert dieses Feld am Song; ein Eintrag ohne `midi` liefert `null`. Nachweis: rot vor 2.1.
- [ ] 1.2 Unit (`SongItemTest`): der Schallplatten-Gegenstand eines Vanilla-Titels und eines MIDI-Titels hat kein Bauteil `jukebox_playable`. Nachweis: rot vor 2.2.
- [ ] 1.3 Unit (`SongItemTest`): die Lore eines MIDI-Songs mit `midi` ist genau `Original: Beispiel` und `MIDI: BitMidi-Upload (unbekannt)`; ohne `midi` nur die Original-Zeile. Nachweis: rot vor 2.1.
- [ ] 1.4 Unit (`SongItemTest`): die Lore einer Vanilla-Platte ist `C418 · Music disc` und hat keine MIDI-Zeile. Nachweis: grün (Schutztest).

## 2. Umsetzung (grün)

- [ ] 2.1 `MidiSong` um das Feld `midi` (nullable) erweitern, `SongCatalog` liest es mit `text(fields, "midi")`. Schlüssel `titan.radio.disc.lore.original` in `RadioMessages`, `KEYS` und beiden Bundles; `titan.radio.disc.lore.midi` auf „MIDI: <arg:0>“ umstellen. Prüfung: 1.1 und 1.3 grün.
- [ ] 2.2 `RadioMenuBuilder`: Lore über ein `switch` auf `RadioSong` bauen; Song-Gegenstände und „Läuft jetzt“ mit `without(DataComponents.JUKEBOX_PLAYABLE)` bauen. Prüfung: 1.2 und 1.4 grün.
- [ ] 2.3 Refactor: keine toten Pfade, keine Dopplung zwischen Titel- und „Läuft jetzt“-Gegenstand. Alle Radio-Tests grün.

## 3. Dokumentation

- [ ] 3.1 `docs/lobby-modules.md`, Abschnitt Radio: das Feld `midi` im Beispiel und in den Regeln, die Lore-Zeilen für MIDI und Vanilla.

## 4. Verifikation und Abnahme

- [ ] 4.1 `./gradlew :features:radio:spotlessApply :features:radio:check :apps:cloudnet:smokeTest :apps:local:smokeTest` grün.
- [ ] 4.2 Manuelle Abnahme im Spiel: der Tooltip eines MIDI-Songs zeigt Name und zwei Herkunftszeilen, der Tooltip einer Vanilla-Platte zeigt Name und „C418 · Schallplatte“ ohne die Plattenbeschreibung.
- [ ] 4.3 Code-Commits: der Code-Zweig berührt kein Verzeichnis unter `openspec/`.

## 5. Pull Request öffnen

- [ ] 5.1 Code-Pull-Request mit dem Titel `feat(radio): show original artist and midi source instead of the disc description` von einem Zweig aus dem `origin/main` öffnen, der die gemergte Vorschlags-PR enthält. Die Änderung ist nicht brechend.
