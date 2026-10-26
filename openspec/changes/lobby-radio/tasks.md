# Tasks

Jede Aufgabe mit „Test zuerst“ wird erst rot gesehen, dann grün gemacht. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, Takt explizit (`tick()` bzw. `env.tick()`). Vanilla-Wiedergabe wird über eine Testverbindung geprüft, die gesendete Pakete erfasst. Vor dem Abhaken einer Gruppe läuft der Modultest, vor dem Abhaken des letzten Punkts `./gradlew build`.

## 1. Vorlauf

- [x] 1.1 OpenSpec-Artefakte (`proposal.md`, `design.md`, `specs/lobby-radio/spec.md`, `tasks.md`) schreiben und mit `openspec validate lobby-radio --strict` prüfen. Commit `docs(openspec): propose lobby-radio`.
- [ ] 1.2 Laufzeitimage: `java.desktop` (für `javax.sound.midi`) ist enthalten. Hotbar-Platz 5 und Priorität 680 sind frei (Konflikt-Test der Hotbar als Nachweis).
- [ ] 1.3 Offene Frage aus `design.md` D7 entscheiden: Liest `avaje-config` ein YAML-Listenformat? Ja: kein SnakeYAML. Nein: SnakeYAML wird Laufzeitabhängigkeit von `features/radio`, Version aus dem Katalog.
- [ ] 1.4 Die Plattenliste gegen `Material.values()` und `SoundEvents` der Minestom-Version `2026.10.07-26.2` prüfen (22 Platten, inklusive `bounce`). Nachweis: Test 3.2.

## 2. Songmodell und MIDI-Parser (`features/radio`, rein)

- [ ] 2.1 Test zuerst (Unit, `RadioSongTest`): `VanillaDisc` und `MidiSong` liefern `kind()` `VANILLA` bzw. `MIDI`, und beide liefern Titel, Künstler und Schallplatte; die Art ist ein Attribut für Telemetrie.
- [ ] 2.2 `RadioSong` (sealed interface), `RadioKind`, `VanillaDisc` und `MidiSong` als Records implementieren.
- [ ] 2.3 Test zuerst (Unit, `MidiSongParserTest`, Sequenzen im Speicher gebaut und mit `MidiSystem.write` geschrieben): Tempo 120 BPM ergibt die erwarteten Ticks; Tempowechsel wird berücksichtigt; Note-Off und Anschlagstärke 0 werden ignoriert; kaputte Bytes werfen `SongFormatException` mit Grund; zu viele Noten und zu lange Songs werden abgelehnt.
- [ ] 2.4 `MidiSongParser`, `SongFormatException` und die Notenliste im `MidiSong` implementieren (`javax.sound.midi`, kein Synthesizer).
- [ ] 2.5 Test zuerst (Unit, `SongCatalogTest` mit `@TempDir`): fehlende Datei wird übersprungen; falscher Schallplattentyp wird übersprungen; Platte schon vergeben (Vanilla oder MIDI) wird übersprungen; doppelte `id` oder `disc` wird übersprungen; Überschuss über 18 Songs wird übersprungen; ohne `radio/` bleibt die MIDI-Liste leer.
- [ ] 2.6 `SongCatalog` implementieren, Lesen aus `radio/songs.yaml` gemäß Entscheidung 1.3. Ablehnungen als WARN mit Dateiname.

## 3. Vanilla-Schallplatten (`features/radio`, rein)

- [ ] 3.1 Längen messen: die 22 Plattenlängen gegen die Plattendateien der Client-Assets prüfen, die Platzhalter `tears`, `lava_chicken` und `bounce` ersetzen. Abweichungen von `design.md` D11 im PR nennen.
- [ ] 3.2 Test zuerst (Unit, `VanillaDiscsTest`): die Tabelle enthält genau die `Material.MUSIC_DISC_*` aus `Material.values()`, je einen Plattenton aus `SoundEvents`, keine Duplikate, jede Länge zwischen 0 und 15 Minuten. Ein Test, der die Liste zählt (22), schlägt fehl, wenn eine Platte fehlt.
- [ ] 3.3 `VanillaDiscs` (feste Tabelle mit Schallplatte, Plattenton und Länge in Ticks) implementieren.

## 4. Notenabbildung (MIDI, rein)

- [ ] 4.1 Test zuerst (Unit, `InstrumentMapperTest`): je GM-Programmgruppe das Instrument aus `design.md` D4; Schlagzeug auf Kanal 10 nach Taste (Kick, Snare, Hi-Hat, Cowbell, Rest).
- [ ] 4.2 Test zuerst (Unit, `PitchTest`): `pitch(0) = 0,5`, `pitch(12) = 1,0`, `pitch(24) = 2,0`; Transponieren auf den Median in Vielfachen von 12, Gleichstand nach unten; Falten einzelner Noten hoch und runter, Tonhöhenklasse bleibt.
- [ ] 4.3 `NoteMapper` implementieren (Instrument, Note 0 bis 24, Lautstärke `velocity / 127`, Anschlagstärke 0 ausgenommen).

## 5. Wiedergabe (rein)

- [ ] 5.1 Test zuerst (Unit, `RadioPlaybackTest` mit `RecordingRadioSink` und explizitem `tick()`, MIDI): Quantisierung auf 50 ms ohne Drift; Budget von 16 Noten je Tick nach Lautstärke; Noten mehr als einen Tick zu spät verfallen; Ende liefert `END`; `stop` ist idempotent; nach dem Stopp kommen keine Töne mehr.
- [ ] 5.2 Test zuerst (Unit, `VanillaDiscPlaybackTest` mit `RecordingRadioSink`): Start ruft `playRecord` mit dem Plattenton, Quelle RECORD, Emitter `self`, Lautstärke 1,0, Tonhöhe 1,0, genau einmal; Stopp ruft `stopRecord` mit demselben Schlüssel, genau einmal; nach Länge plus 5 Sekunden Puffer kommt `END` und ein benannter Stopp; Stopp nach Ende tut nichts.
- [ ] 5.3 Test zuerst (Unit, `RadioSelectionTest`): „Weiter“ wechselt auf den nächsten Titel und wrapt von der letzten Platte des Menüs auf die erste; „Weiter“ während eines Vanilla-Titels sendet erst den Stopp, dann den Start des nächsten.
- [ ] 5.4 `RadioPlayback` (gemeinsame Lebensdauer, Inhalt je Art), `RadioSink` (`note`, `playRecord`, `stopRecord`), `StopReason` (`USER`, `NEXT`, `END`, `LOGOUT`, `BUSY`, `SHUTDOWN`) und `RadioSelection` implementieren.

## 6. Minestom-Verdrahtung (`features/radio`)

- [ ] 6.1 Test zuerst (Env, `RadioModuleIntegrationTest`, `env.tick()`, je eigene Testverbindung): Item auf Hotbar-Platz 5 ist immer da, auch ohne MIDI-Dateien; Klick auf eine Vanilla-Platte startet den Titel; Klick auf einen MIDI-Song startet ihn; nur der Startende erhält Pakete, der andere Spieler erhält keine.
- [ ] 6.2 Test zuerst (Env): Vanilla-Platte wird auf der Testverbindung des Spielers erfasst: Plattenton-Paket mit Quelle RECORD und Emitter `self`, danach beim Stopp das benannte Stopp-Paket mit demselben Schlüssel; Ende ohne Stopp durch den Spieler sendet ebenfalls den benannten Stopp.
- [ ] 6.3 Test zuerst (Env): Abmelden stoppt mit `LOGOUT`; `LobbyPlayerBusyEvent(player, true)` stoppt mit `BUSY` und sendet für Vanilla den benannten Stopp; `LobbyPlayerBusyEvent(player, false)` tut nichts.
- [ ] 6.4 `RadioModule` (`@PostConstruct`: MIDI-Songs laden, Menüs bauen, `FeatureNode` mit Priorität 680, ein Takt-Task für alle Wiedergaben; `@PreDestroy`: alle stoppen mit `SHUTDOWN`, Takt aufheben, Knoten schließen, Menüs entfernen), `RadioPlaybacks` (nebenläufige Map), `PlayerRadioSink` (Produktion von `RadioSink` für einen Spieler), `RadioItems` (`@Factory`, immer da), `RadioMenuBuilder` (einziger Builder, zwei geteilte Aves-Inventare nach D12: Vanilla-Platten und Eigene Songs, Umschalter nur mit MIDI-Songs), `package-info.java` mit `@InjectModule(name = "radioColumn", ...)`.
- [ ] 6.5 Test zuerst (Env): nach `close()` löst kein Ereignis mehr Code des Moduls aus (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`); `ColumnArchitectureTest` für `features/radio`.

## 7. Telemetrie und Meldungen

- [ ] 7.1 Test zuerst (Unit, `RadioTelemetryTest` mit `TestTelemetry`): `titan.radio.plays{kind}`, `titan.radio.stops{reason, kind}`, `titan.radio.notes.dropped` (nur MIDI), `titan.radio.songs{result}` (nur MIDI-Ladeergebnis); keine Spieler-Id in einem Attribut.
- [ ] 7.2 `RadioTelemetry` implementieren und in Start, Stopp, verworfene Noten und Ladeergebnis verdrahten.
- [ ] 7.3 Test zuerst (Unit, `RadioMessagesTest`): beide Sprachen haben dieselben Schlüssel, alle rendern, auch die Menütitel und der Umschalter. Dann `titan/radio/messages_de.properties` und `messages_en.properties`.

## 8. Dokumentation und Prüfung

- [ ] 8.1 `docs/lobby-modules.md`: Abschnitt „Radio (`radio`)“ mit Item, zwei Menüs, Vanilla-Platten, optionalem Verzeichnis `radio/` im Template, Stoppgründen. `README.md` nur, wenn dort Items oder Befehle gelistet sind.
- [ ] 8.2 `./gradlew spotlessApply` und `./gradlew build` grün; `openspec validate lobby-radio --strict` grün.
- [ ] 8.3 Abnahme auf einem Testserver, von einer Kopie des Jars gebaut und nicht im laufenden Worktree: Vanilla-Platte ohne MIDI-Dateien spielt nur beim Startenden; Stopp, Weiter, Abmelden, Sprungtest; MIDI-Song mit Notenblöcken; Lasttest mit 50 Spielern und Paketzahl aus der Telemetrie. Nicht ausgeführt, falls kein Testserver verfügbar; im PR als offen vermerken.
- [ ] 8.4 Spec-Delta mit der Implementierung abgleichen (Szenario-Titel, Grenzwerte, Priorität, Instrumententabelle nach Gehör, Längen aus 3.1).

## 9. Commits und Pull Request

- [ ] 9.1 Commits je Typ: `feat(radio): ...` für Code und Tests, `docs(radio): ...` für Dokumentation. Kein Commit mischt Typen.
- [ ] 9.2 Pull Request öffnen mit dem Titel `feat(radio): add a note block radio with personal discs`. Beschreibung auf Englisch, mit Verweis auf die Entscheidungen D1 bis D12 in `design.md`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer. Nach dem Merge archiviert der Maintainer die Änderung als `docs(openspec): archive lobby-radio`.
