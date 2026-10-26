# Tasks

Jede Aufgabe mit „Test zuerst“ wird erst rot gesehen, dann grün gemacht. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, Takt explizit (`tick()` bzw. `env.tick()`). Vor dem Abhaken einer Gruppe läuft der Modultest, vor dem Abhaken des letzten Punkts `./gradlew build`.

## 1. Vorlauf

- [ ] 1.1 OpenSpec-Artefakte (`proposal.md`, `design.md`, `specs/lobby-radio/spec.md`, `tasks.md`) schreiben und mit `openspec validate lobby-radio --strict` prüfen. Commit `docs(openspec): propose lobby-radio`.
- [ ] 1.2 Laufzeitimage: `java.desktop` (für `javax.sound.midi`) ist enthalten. Hotbar-Platz 5 und Priorität 680 sind frei (Konflikt-Test der Hotbar als Nachweis).
- [ ] 1.3 Offene Frage 1 entscheiden: Liest `avaje-config` ein YAML-Listenformat? Ja: kein SnakeYAML. Nein: SnakeYAML wird Laufzeitabhängigkeit von `features/radio`, Version aus dem Katalog.

## 2. Parser und Songs (`features/radio`, rein)

- [ ] 2.1 Test zuerst (Unit, `MidiSongParserTest`, Sequenzen im Speicher gebaut und mit `MidiSystem.write` geschrieben): Tempo 120 BPM ergibt die erwarteten Ticks; Tempowechsel wird berücksichtigt; Note-Off und Anschlagstärke 0 werden ignoriert; kaputte Bytes werfen `SongFormatException` mit Grund; zu viele Noten und zu lange Songs werden abgelehnt.
- [ ] 2.2 `MidiSongParser`, `Song`, `NoteEvent`, `SongFormatException` implementieren (`javax.sound.midi`, kein Synthesizer).
- [ ] 2.3 Test zuerst (Unit, `SongCatalogTest` mit `@TempDir`): fehlende Datei wird übersprungen; falscher Schallplattentyp wird übersprungen; doppelte `id` oder `disc` wird übersprungen; Überschuss über 18 Songs wird übersprungen; kein gültiger Song macht das Modul aus.
- [ ] 2.4 `SongCatalog` implementieren, Lesen aus `radio/songs.yaml` gemäß Entscheidung 1.3. Ablehnungen als WARN mit Dateiname.

## 3. Abbildung (rein)

- [ ] 3.1 Test zuerst (Unit, `InstrumentMapperTest`): je GM-Programmgruppe das Instrument aus `design.md` D4; Schlagzeug auf Kanal 10 nach Taste (Kick, Snare, Hi-Hat, Cowbell, Rest).
- [ ] 3.2 Test zuerst (Unit, `PitchTest`): `pitch(0) = 0,5`, `pitch(12) = 1,0`, `pitch(24) = 2,0`; Transponieren auf den Median in Vielfachen von 12, Gleichstand nach unten; Falten einzelner Noten hoch und runter, Tonhöhenklasse bleibt.
- [ ] 3.3 `NoteMapper` implementieren (Instrument, Note 0 bis 24, Lautstärke `velocity / 127`, Anschlagstärke 0 ausgenommen).

## 4. Wiedergabe (rein)

- [ ] 4.1 Test zuerst (Unit, `RadioPlaybackTest` mit `RecordingNoteSink` und explizitem `tick()`): Quantisierung auf 50 ms ohne Drift; Budget von 16 Noten je Tick nach Lautstärke; Noten mehr als einen Tick zu spät verfallen; Song-Ende liefert `END`; `stop` ist idempotent; nach dem Stopp kommen keine Töne mehr.
- [ ] 4.2 Test zuerst (Unit, `RadioSelectionTest`): „Weiter“ wechselt auf den nächsten Song und wrapt von der letzten Schallplatte auf die erste.
- [ ] 4.3 `RadioPlayback`, `NoteSink`, `StopReason` (`USER`, `NEXT`, `END`, `LOGOUT`, `BUSY`, `SHUTDOWN`) und `RadioSelection` implementieren.

## 5. Minestom-Verdrahtung (`features/radio`)

- [ ] 5.1 Test zuerst (Env, `RadioModuleIntegrationTest`, `env.tick()`): Item auf Hotbar-Platz 5; Klick auf eine Schallplatte startet den Song; zwei Spieler, nur der Startende bekommt Töne (zwei Sinks).
- [ ] 5.2 Test zuerst (Env): Abmelden stoppt mit `LOGOUT`; `LobbyPlayerBusyEvent(player, true)` stoppt mit `BUSY`; `LobbyPlayerBusyEvent(player, false)` tut nichts; Song-Ende stoppt mit `END`.
- [ ] 5.3 `RadioModule` (`@PostConstruct`: Songs laden, Menü registrieren, `FeatureNode` mit Priorität 680, ein Takt-Task für alle Wiedergaben; `@PreDestroy`: alle stoppen mit `SHUTDOWN`, Takt aufheben, Knoten schließen, Menü entfernen), `RadioPlaybacks` (nebenläufige Map), `RadioItems` (`@Factory`, leer ohne Songs), `RadioMenuBuilder` (einziger Builder, Aves-Inventar, geteilt), `package-info.java` mit `@InjectModule(name = "radioColumn", ...)`.
- [ ] 5.4 Test zuerst (Env): nach `close()` löst kein Ereignis mehr Code des Moduls aus (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`); `ColumnArchitectureTest` für `features/radio`.

## 6. Telemetrie und Meldungen

- [ ] 6.1 Test zuerst (Unit, `RadioTelemetryTest` mit `TestTelemetry`): `titan.radio.plays`, `titan.radio.stops{reason}`, `titan.radio.notes.dropped`, `titan.radio.songs{result}`; keine Spieler-Id in einem Attribut.
- [ ] 6.2 `RadioTelemetry` implementieren und in Start, Stopp, verworfene Noten und Ladeergebnis verdrahten.
- [ ] 6.3 Test zuerst (Unit, `RadioMessagesTest`): beide Sprachen haben dieselben Schlüssel, alle rendern. Dann `titan/radio/messages_de.properties` und `messages_en.properties`.

## 7. Dokumentation und Prüfung

- [ ] 7.1 `docs/lobby-modules.md`: Abschnitt „Radio (`radio`)“ mit Item, Menü, Stoppgründen und dem Verzeichnis `radio/` im Template. `README.md` nur, wenn dort Items oder Befehle gelistet sind.
- [ ] 7.2 `./gradlew spotlessApply` und `./gradlew build` grün; `openspec validate lobby-radio --strict` grün.
- [ ] 7.3 Abnahme auf einem Testserver, von einer Kopie des Jars gebaut und nicht im laufenden Worktree: Töne nur beim Startenden; Stopp, Weiter, Abmelden, Sprungtest; Lasttest mit 50 Spielern und Paketzahl aus der Telemetrie. Nicht ausgeführt, falls kein Testserver verfügbar; im PR als offen vermerken.
- [ ] 7.4 Spec-Delta mit der Implementierung abgleichen (Szenario-Titel, Grenzwerte, Priorität, Instrumententabelle nach Gehör).

## 8. Commits und Pull Request

- [ ] 8.1 Commits je Typ: `feat(radio): ...` für Code und Tests, `docs(radio): ...` für Dokumentation. Kein Commit mischt Typen.
- [ ] 8.2 Pull Request öffnen mit dem Titel `feat(radio): add a note block radio with personal discs`. Beschreibung auf Englisch, mit Verweis auf die Entscheidungen D1 bis D10 in `design.md`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer. Nach dem Merge archiviert der Maintainer die Änderung als `docs(openspec): archive lobby-radio`.
