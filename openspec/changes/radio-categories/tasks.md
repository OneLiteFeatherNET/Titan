# Tasks

Jede Aufgabe mit „Test zuerst“ wird erst rot gesehen, dann grün gemacht. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, `@TempDir` für Dateien, Takt explizit (`env.tick()`), Zufall nur über `RandomGenerator` mit festem Seed, WARN-Zeilen über einen `ListAppender` geprüft. Vor dem Abhaken einer Gruppe läuft der Modultest, vor dem Abhaken des letzten Punkts `./gradlew build`.

Die Umsetzung läuft über Haiku-Agenten mit effort low, medium oder high je Aufgabe. Die Hauptsitzung prüft Diffs und Testergebnisse vor jedem Commit.

Vorbedingung fürs Archivieren: `lobby-radio` ist zuerst archiviert (`docs(openspec): archive lobby-radio`). Erst dann gibt es `openspec/specs/lobby-radio/spec.md`, auf das dieses Delta zeigt.

## 0. Vorschlag

- [x] 0.1 OpenSpec-Artefakte (`proposal.md`, `design.md`, `specs/lobby-radio/spec.md`, `tasks.md`) schreiben und mit `openspec validate radio-categories --strict` prüfen. Commit `docs(openspec): propose radio-categories`.

## 1. Vorlauf

- [ ] 1.1 Aves-Spike (Env, rot, dann grün): `PersonalTranslatedInventoryBuilder` mit Datenebenen-Funktion. Prüfen: Öffnen zeigt den Inhalt nach einem Tick; `invalidateDataLayout()` ändert den Inhalt eines offenen Fensters an Ort und Stelle (gleiche Inventar-Instanz); `unregister()` schließt nur dieses Fenster und ruft den Schließ-Listener nicht rekursiv auf. Ergebnis zusätzlich prüfen: ändert sich der Fenstertitel in place? Ergebnis in `design.md`, R6, und in `proposal.md`, Offen 5, festhalten. Die Aves-Version, die Titan auflöst, mit aufschreiben.
- [ ] 1.2 Materialprüfung: alle Symbole der Änderung (`OAK_DOOR`, `SPECTRAL_ARROW`, `BOOK`, `BARRIER`, `REPEATER`, `NOTE_BLOCK`, `ARROW`, `PAPER`, `MUSIC_DISC_13`, `GRAY_STAINED_GLASS_PANE`, `JUKEBOX`, `EMERALD`, `FIREWORK_ROCKET`, `CLOCK`, `JACK_O_LANTERN`) gegen `Material.values()` der Minestom-Version. Test: alle existieren.

## 2. Kategorien (rein)

- [ ] 2.1 Test zuerst (Unit, `RadioCategoryTest`): Id-Muster `[a-z0-9_-]{1,32}`; reservierte Ids `minecraft` und `other`; Anzeigename als Text oder als `{de, en}` mit Rückfall auf die andere Sprache; Symbol-Ersatz `PAPER`; „Minecraft“ ist eingebaut und enthält genau die 22 Vanilla-Platten.
- [ ] 2.2 `RadioCategory` (Record aus Id, Namen, Symbol und Reihenfolge) und `RadioCategories` (Sortierung: Minecraft vorn, deklarierte nach Reihenfolge und Id, Sonstige hinten; nur Kategorien mit Titeln) implementieren.
- [ ] 2.3 Test zuerst (Unit, `RadioSongTest` erweitern): `VanillaDisc` liefert `minecraft`; `MidiSong` liefert die Kategorie aus dem Konstruktor; bestehende Tests anpassen.
- [ ] 2.4 `RadioSong` um `category()` erweitern; `VanillaDisc` und `MidiSong` anpassen.

## 3. Laden (rein, `@TempDir`)

- [ ] 3.1 Test zuerst (Unit, `SongCatalogTest`, WARN mit `ListAppender`): der Abschnitt `categories` wird gelesen; ein Song ohne `category` landet in „Sonstige“ mit WARN; eine unbekannte Id landet in „Sonstige“ mit WARN und Id; eine reservierte Id wird übersprungen mit WARN; die 21. Kategorie wird übersprungen mit WARN; ein ungültiges Symbol wird durch `PAPER` ersetzt mit WARN.
- [ ] 3.2 Test zuerst (Unit): der 201. Song wird übersprungen mit WARN, die 200 davor stehen im Ergebnis; ein Song, der das Notenbudget von 1 000 000 sprengt, wird übersprungen und zählt nicht, die Songs davor bleiben.
- [ ] 3.3 Test zuerst (Unit): eine alte `songs.yaml` mit 18 Songs ohne `category` und ohne `categories` lädt alle 18 in „Sonstige“; unbekannte Felder werden ignoriert (Rückrollbarkeit, R11).
- [ ] 3.4 `SongCatalog` um den Kategorienabschnitt, die Zuordnung je Song und den Notenzähler erweitern. Konstanten in `RadioLimits`: `MAX_SONGS = 200`, `MAX_NOTES_TOTAL = 1_000_000`, `MAX_CATEGORIES = 20`.

## 4. Seiten (rein)

- [ ] 4.1 Test zuerst (Unit, `PageLayoutTest`): Seitenzahl für 0, 1, 45, 46 und 200 Titel (0 und 1 ergeben 1 Seite); Index 0 auf Seite 1 Platz 0, Index 44 auf Seite 1 Platz 44, Index 45 auf Seite 2 Platz 0, Index 199 auf Seite 5 Platz 44; Klammer einer Seite über dem Ende; Steuerzeile liegt auf 45 bis 53; Blättern an den Grenzen ändert nichts.
- [ ] 4.2 `PageLayout` implementieren (ohne Aves, ohne Minestom).

## 5. Auswahl im Kategorie-Scope (rein)

- [ ] 5.1 Test zuerst (Unit, `RadioSelectionTest`, Seed): Einmal ohne Nachfolger; Schleife derselbe Titel; Zufall nie der eben gespielte und nie ein Titel aus einer anderen Kategorie, über viele Ziehungen; eine Kategorie mit einem Titel wiederholt ihn; eine leere Kategorie liefert nichts.
- [ ] 5.2 Test zuerst (Unit): „Weiter“ in Einmal und Schleife geht in Reihenfolge mit Wrap innerhalb der Kategorie; „Weiter“ im Zufall nie derselbe Titel; ohne laufenden Titel gilt die zuletzt geöffnete Kategorie.
- [ ] 5.3 `RadioSelection` auf Kategorien umstellen: ein Katalog je Kategorie, Auswahl nach der Kategorie des laufenden Titels, Zufall über `RandomGenerator`.

## 6. Lautstärke und Wiedergabe (rein)

- [ ] 6.1 Test zuerst (Unit, `VolumeTest`): Schritte 10 bis 100; Standard 100; Grenzen bleiben stehen; Links- und Rechtsklick.
- [ ] 6.2 Test zuerst (Unit, `NoteMapperTest`, `RadioPlaybackTest`): Lautstärke jeder Note ist `velocity / 127 × v`; die Rangfolge für das Budget von 16 Noten bleibt nach Anschlag.
- [ ] 6.3 Test zuerst (Unit, `VanillaDiscPlaybackTest` mit `RecordingRadioSink`): Der Start sendet die Lautstärke `v / 100`; eine Änderung während des Laufs ruft den Sink nicht auf; der nächste Start nutzt die neue Lautstärke.
- [ ] 6.4 `Volume` (Wertobjekt) implementieren; `RadioPlayback`, `NoteMapper` und `VanillaDisc`-Start anpassen.

## 7. Sitzung (rein)

- [ ] 7.1 Test zuerst (Unit, `RadioSessionTest`): Standard ist Übersicht, Seite 1, Modus Einmal, Lautstärke 100; Blättern ändert nur die Seite; ein Kategoriewechsel setzt Seite 1; Modus und Lautstärke bleiben beim Kategoriewechsel; Abmelden vergisst alles.
- [ ] 7.2 `RadioSession` um Ansicht, Seite und Lautstärke erweitern.

## 8. Menü (Aves, Env)

- [ ] 8.1 Test zuerst (Env, `RadioMenuIntegrationTest`, `env.tick()`, Testverbindung): das Hotbar-Item öffnet die Übersicht mit 54 Plätzen und Steuerzeile auf 45 bis 53; ein Klick auf eine Kategorie öffnet ihre Seite; Blättern; Zurück zur Übersicht; der Modusklick ändert den Inhalt desselben Inventars; Links- und Rechtsklick auf die Lautstärke.
- [ ] 8.2 Test zuerst (Env): Schließen entfernt die Ansicht, ohne rekursives Schließen; `@PreDestroy` schließt alle offenen Fenster; nach `close()` löst kein Ereignis mehr Code des Moduls aus (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`); zwei Spieler sehen jeweils ihre eigene Seite.
- [ ] 8.3 `RadioMenuLayout` (Inhalt 0 bis 44, Steuerzeile 45 bis 53), `RadioMenuBuilder` (ein Builder je Fenster, einzige Stelle mit dem Layout), `RadioViews` (öffnen, aktualisieren, schließen) und `RadioModule` (Klick-, Schließ- und Tick-Aktualisierung) umbauen. `RadioMenu` und `Variant` entfallen.
- [ ] 8.4 Test zuerst (Env): Titelstart und Titelende aktualisieren „Läuft jetzt“ im offenen Fenster, ohne Neuöffnen; danach Implementierung.

## 9. Telemetrie und Meldungen

- [ ] 9.1 Test zuerst (Unit, `RadioTelemetryTest`): `plays` trägt `category` (`minecraft` für Vanilla, `other` für Sonstige, sonst die Id); `stops` trägt keine Kategorie; kein Attribut mit Spieler-Id.
- [ ] 9.2 `RadioTelemetry` erweitern. INFO beim Start mit Anzahl Kategorien und Titeln.
- [ ] 9.3 Test zuerst (Unit, `RadioMessagesTest`): Schlüssel für Kategorienamen, Sonstige, Seitenanzeige, Zurück, Seite zurück und weiter, „Läuft jetzt“, „Kein Titel läuft“, Lautstärke, den Hinweis zur Schallplatte und die Lore-Zeilen sind in beiden Sprachen gleich und rendern. Dann `titan/radio/messages_de.properties` und `messages_en.properties`.

## 10. Architektur, Dokumentation, Prüfung

- [ ] 10.1 `ColumnArchitectureTest` grün; `features/radio` hängt weiter nur von `core` ab.
- [ ] 10.2 `docs/lobby-modules.md`, Abschnitt Radio: Übersicht, Kategorien mit Beispiel `songs.yaml`, Grenzen 200 und 1 000 000, Lautstärke und ihre Grenze bei laufenden Schallplatten, Vergessen beim Abmelden, Hinweis zum Zurücksetzen der Seite. `README.md` nur, wenn dort Items oder Befehle gelistet sind.
- [ ] 10.3 `./gradlew spotlessApply` nur auf geänderte Dateien; `./gradlew :features:radio:test`; vor dem letzten Häkchen `./gradlew build`; `openspec validate radio-categories --strict` ohne Fehler (die Warnungen zu SHALL/MUST und zur Länge sind bei den bestehenden Changes ebenso).
- [ ] 10.4 Speichermessung mit 200 Titeln (Heap-Histogramm von `MidiNote`) gegen die Schätzung in `design.md`, R5, auf einer Kopie des Jars. Ergebnis im PR.
- [ ] 10.5 Abnahme auf einem Testserver, auf einer Kopie des Jars und nicht im laufenden Worktree: Übersicht, Blättern bei mehr als 45 Titeln, Modus, Lautstärke bei MIDI und Vanilla, Zufall bleibt in der Kategorie, Stopp, Weiter, Abmelden, Sprungtest. Nicht ausgeführt, falls kein Testserver verfügbar: im PR als offen vermerken.

## 11. Commits und Pull Request

- [ ] 11.1 Commits je Typ: `feat(radio): ...` für Code und Tests; `docs(radio): ...` für `docs/lobby-modules.md`. Kein Commit mischt Typen. Kein Trailer mit Session-Link.
- [ ] 11.2 Pull Request öffnen mit dem Titel `feat(radio): add categories, paging and volume to the radio menu`. Beschreibung auf Englisch im Schema Problem/Approach, mit Verweis auf R1 bis R11 in `design.md`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer. Nach dem Merge archiviert der Maintainer die Änderung als `docs(openspec): archive radio-categories`.
