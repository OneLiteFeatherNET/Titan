# Tasks

Zweig für die Umsetzung: `feat/radio-instrument-table`, angelegt vom Vorschlags-Zweig und nach dem Merge der Vorschlags-PR auf `origin/main` gerebased. Vor dem Abhaken einer Aufgabe läuft `./gradlew :features:radio:test`; vor dem Abschluss `./gradlew :features:radio:check` und `./gradlew :apps:cloudnet:smokeTest :apps:local:smokeTest`. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, keine Dateien außerhalb von Temp-Verzeichnissen.

## 1. Tests zuerst (rot)

- [ ] 1.1 Unit (`InstrumentMapperTest`): je Programmfamilie eine Stichprobe liefert das Instrument aus der Spezifikation (Klavier 0 harp, E-Piano 4 pling, Gitarre 29 bit, Cello 42 bass, Trompete 56 trumpet, Sitar 104 banjo, Glocke 112 bell, Effekt 120 hat). Nachweis: rot vor 2.1.
- [ ] 1.2 Unit (`InstrumentMapperTest`): alle 128 Programme liefern ein Instrument, ohne Ausnahme. Nachweis: rot vor 2.1.
- [ ] 1.3 Unit (`InstrumentMapperTest`): Schlagzeugtasten liefern Instrument und Note aus der Tabelle (36 basedrum 6, 38 snare 12, 42 hat 16, 46 hat 10, 49 snare 20, 56 cow_bell 12, 60 basedrum 16, 80 chime 18), unbekannte Tasten (84) hat 12. Nachweis: rot vor 2.2.
- [ ] 1.4 Unit (`NoteMapperTest`): Schlagzeugnote 36 spielt basedrum auf Note 6 und wird nicht verschoben. Die Erwartung 12 wird ersetzt. Nachweis: rot vor 2.2.
- [ ] 1.5 Unit (`MidiSongParserTest`): CC 7 halbiert den Anschlag; CC 11 ebenso; beide multiplizieren sich; CC auf einem anderen Kanal wirkt nicht; Anschlag nach Skalierung 0 wird übersprungen. Nachweis: rot vor 2.3.

## 2. Umsetzung (grün)

- [ ] 2.1 `InstrumentMapper`: 128-Programm-Tabelle als `switch` nach D1. Prüfung: 1.1 und 1.2 grün.
- [ ] 2.2 `InstrumentMapper` und `NoteMapper`: Schlagzeugtabelle nach D2, `NoteMapper` nutzt die feste Note ohne Registerlogik. Prüfung: 1.3 und 1.4 grün.
- [ ] 2.3 `MidiSongParser`: CC 7 und CC 11 je Kanal nach D4. Prüfung: 1.5 grün.
- [ ] 2.4 Refactor: Javadoc von `InstrumentMapper` und `NoteMapper` auf die neue Zuordnung anpassen, keine toten Pfade. Alle Radio-Tests grün.

## 3. Verifikation und Abnahme

- [ ] 3.1 `./gradlew :features:radio:spotlessApply :features:radio:check :apps:cloudnet:smokeTest :apps:local:smokeTest` grün.
- [ ] 3.2 Manuelle Abnahme auf einem lokalen Testserver mit einer Kopie des Jars (nicht im laufenden Worktree gebaut): eine Schlagzeugspur mit Toms und Becken klingt unterscheidbar, ein E-Piano klingt nicht wie eine Harfe.
- [ ] 3.3 Commit `feat(radio)`: nur Dateien unter `features/radio/`. Prüfung: der Code-Commit berührt kein Verzeichnis unter `openspec/`.

## 4. Pull Request öffnen

- [ ] 4.1 Code-Pull-Request mit dem Titel `feat(radio): map every midi program and drum key to a fitting note block sound` von einem Zweig aus dem aktuellen `origin/main` öffnen, der die gemergte Vorschlags-PR enthält. Die Änderung ist nicht brechend.
