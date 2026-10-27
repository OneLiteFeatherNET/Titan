# Tasks

Zweig für die Umsetzung: `fix/radio-instrument-registers`, angelegt vom Vorschlags-Zweig und nach dem Merge der Vorschlags-PR auf `origin/main` gerebased. Vor dem Abhaken einer Aufgabe läuft `./gradlew :features:radio:test`; vor dem Abschluss `./gradlew :features:radio:check`, `./gradlew :apps:cloudnet:smokeTest :apps:local:smokeTest`. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, keine Dateien außerhalb von Temp-Verzeichnissen.

## 1. Tests zuerst (rot)

- [ ] 1.1 Unit (`RadioInstrumentTest`, neu): Grundtasten je Instrument nach `design.md` D1, Trompeten (`trumpet`, `trumpet_exposed`, `trumpet_weathered`, `trumpet_oxidized`) mit 54. Nachweis: rot vor 2.1.
- [ ] 1.2 Unit (`RadioInstrumentTest`): Registergrenzen (Bass 30 und 54 drin, 29 und 55 nicht). Erweiterung nach D2: Harfe 50 ergibt Gitarre (nicht Bass), Harfe 35 ergibt Bass, Harfe 79 ergibt Flöte, Harfe 96 ergibt Glocke, Glocke 60 ergibt Gitarre, Harfe 66 bleibt Harfe. Nachweis: rot vor 2.1.
- [ ] 1.3 Unit (`PitchTest`): `foldIntoSpan` faltet 20 auf 32, 110 auf 98, lässt 30, 66 und 102 stehen; die Transposition bleibt bestehen. Nachweis: rot vor 2.2.
- [ ] 1.4 Unit (`NoteMapperTest`): Melodie-Note 79 (Klavier, Median in der Mitte) spielt Flöte mit Note 13 und klingt als 79; Note 96 spielt Glocke mit 18; Bass-Stimme (Programm 32) auf 42 spielt Bass mit 12; Note 20 wird als 32 auf dem Bass gespielt; Note 110 wird als 98 auf der Glocke gespielt; ein Song mit Median 54 wird um eine Oktave nach oben verschoben; Schlagzeug auf Kanal 10 bleibt Note 12. Nachweis: rot vor 2.4.
- [ ] 1.5 Unit (`InstrumentMapperTest`): Programme 56, 57, 58, 61, 62, 63 spielen `trumpet`; 59 `trumpet_oxidized`; 60 `trumpet_weathered`; Programm 40 bleibt Harfe. Die Erwartung für Programm 56 (bisher Didgeridoo) wird angepasst. Nachweis: rot vor 2.3.

## 2. Umsetzung (grün)

- [ ] 2.1 `RadioInstrument`: die vier Trompeten, Grundtaste je Instrument, `reaches(key)`, `semitoneOf(key)`, Erweiterungswahl `playing(key)` nach D2 mit der Erweiterungsgruppe Bass, Gitarre, Flöte, Glocke. Javadoc auf 20 Instrumente anpassen. Prüfung: 1.1 und 1.2 grün.
- [ ] 2.2 `Pitch`: `fold` wird `foldIntoSpan(key)` und faltet nur in 30 bis 102. Prüfung: 1.3 grün.
- [ ] 2.3 `InstrumentMapper`: Blechbläser-Gruppen 56 bis 63 nach D5; Didgeridoo ist aus den Programmen entfernt. Prüfung: 1.5 grün.
- [ ] 2.4 `NoteMapper`: Verschiebung unverändert, je Melodie-Note die Taste verschieben, in den Gesamtbereich falten (nur wenn nötig), Instrument und Note über `RadioInstrument.playing` bestimmen. Schlagzeug unverändert. Prüfung: 1.4 grün.
- [ ] 2.5 Refactor: die alte Harfen-Faltung (`Pitch.fold` mit 0 bis 24) und die Erwartungen der alten Tests entfernen. Alle Radio-Tests grün.

## 3. Verifikation und Abnahme

- [ ] 3.1 `./gradlew :features:radio:check` grün, `./gradlew :apps:cloudnet:smokeTest :apps:local:smokeTest` grün.
- [ ] 3.2 Manuelle Abnahme auf einem lokalen Testserver mit einer Kopie des Jars (nicht im laufenden Worktree gebaut): Eine Melodie über F#5 bleibt in der Oktave, eine Bass-Stimme klingt in ihrer Oktave, Trompeten klingen in der Oktave der Grundtaste 54 (Register per Ohr bestätigen, sonst Grundtaste in `RadioInstrument` korrigieren).
- [ ] 3.3 Vor dem Merge: Owner-Entscheidung zur Verschiebung tiefer Songs (`design.md`, Offen) dokumentiert.
- [ ] 3.4 Commit `fix(radio)`: nur Dateien unter `features/radio/`. Prüfung: `git diff origin/main...HEAD -- . ':!openspec'` zeigt nur den Code-Commit; kein Commit des Codes berührt `openspec/`.

## 4. Pull Request öffnen

- [ ] 4.1 Code-Pull-Request mit dem Titel `fix(radio): keep notes in their octave by using the instrument registers` von einem Zweig aus dem aktuellen `origin/main` öffnen, der die gemergte Vorschlags-PR enthält. Die Änderung ist nicht brechend.
