# Tasks

Zweig für die Umsetzung: `fix/radio-track-octaves`, angelegt vom Vorschlags-Zweig und nach dem Merge der Vorschlags-PR auf `origin/main` gerebased. Vor dem Abhaken einer Aufgabe läuft `./gradlew :features:radio:test`; vor dem Abschluss `./gradlew :features:radio:check` und `./gradlew :apps:cloudnet:smokeTest :apps:local:smokeTest`. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, keine Dateien außerhalb von Temp-Verzeichnissen.

## 1. Tests zuerst (rot)

- [ ] 1.1 Unit (`NoteMapperTest`): Korobeiniki-ähnliche Klavier-Spur auf den Tasten 72 bis 88 spielt alle Noten auf der Harfe, klingt eine Oktave tiefer (Taste − 12). Nachweis: rot vor 2.1.
- [ ] 1.2 Unit (`NoteMapperTest`): Reine Bass-Spur auf Taste 42 bleibt unverschoben, Bass mit Note 12. Schutztest: ist schon vor 2.1 grün und darf durch 2.1 nicht rot werden.
- [ ] 1.3 Unit (`NoteMapperTest`): Drei Oktaven, Tasten 48, 60, 72, 84: Verschiebung 0, Harfe für 60 und 72, Gitarre (Note 6) für 48, Flöte (Note 18) für 84. Nachweis: rot vor 2.1.
- [ ] 1.4 Unit (`NoteMapperTest`): Gleichstand Plus/Minus. Tasten 20, 43, 85 ergeben +12 (Taste 43 klingt wie 55), und zwei Aufrufe mit denselben Eingaben liefern dasselbe Ergebnis. Nachweis: rot vor 2.1.
- [ ] 1.5 Unit (`NoteMapperTest`): Die Erwartung für die Note 79 auf der Flöte (`melodyAboveTheHarpStaysInItsOctave`) wird durch den Spur-Fall ersetzt: Harfe auf 66 und 79 ergibt Verschiebung −12. Nachweis: rot vor 2.1.

## 2. Umsetzung (grün)

- [ ] 2.1 `NoteMapper`: melodische Noten nach (Kanal, Programm) gruppieren, je Spur die Verschiebung nach D2 und D3 aus `design.md` wählen (kleine private Helfer, keine neue Konfiguration), jede Note um die Verschiebung der Spur verschieben, danach `Pitch.foldIntoSpan` und `RadioInstrument.playing` wie bisher. Schlagzeug unverändert. Prüfung: 1.1 bis 1.5 grün.
- [ ] 2.2 Refactor: Klassen-Javadoc von `NoteMapper` auf die Spur-Regel anpassen, keine toten Pfade. Alle Radio-Tests grün.

## 3. Verifikation und Abnahme

- [ ] 3.1 `./gradlew :features:radio:spotlessApply :features:radio:check :apps:cloudnet:smokeTest :apps:local:smokeTest` grün.
- [ ] 3.2 Manuelle Abnahme auf einem lokalen Testserver mit einer Kopie des Jars (nicht im laufenden Worktree gebaut): Eine Klavier-Melodie über F#5 bleibt auf der Harfe, eine Bass-Spur klingt in ihrer Oktave.
- [ ] 3.3 Commit `fix(radio)`: nur Dateien unter `features/radio/`. Prüfung: der Code-Commit berührt kein Verzeichnis unter `openspec/`.

## 4. Pull Request öffnen

- [ ] 4.1 Code-Pull-Request mit dem Titel `fix(radio): shift each track by octaves into its instrument's register` von einem Zweig aus dem aktuellen `origin/main` öffnen, der die gemergte Vorschlags-PR enthält. Die Änderung ist nicht brechend.
