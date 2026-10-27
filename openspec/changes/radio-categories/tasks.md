# Tasks

Jede Aufgabe mit „Test zuerst“ wird erst rot gesehen, dann grün gemacht. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, `@TempDir` für Dateien, Takt explizit (`env.tick()`), Zufall nur über `RandomGenerator` mit festem Seed, IO- und Tick-Executors im Test inline oder als geleerte Warteschlange, WARN-Zeilen über einen `ListAppender` geprüft. Vor dem Abhaken einer Gruppe läuft der Modultest, vor dem Abhaken des letzten Punkts `./gradlew build`.

Die Umsetzung läuft über Haiku-Agenten mit effort low, medium oder high je Aufgabe. Die Hauptsitzung prüft Diffs und Testergebnisse vor jedem Commit.

Vorbedingung fürs Archivieren: `lobby-radio` ist zuerst archiviert (`docs(openspec): archive lobby-radio`). Erst dann gibt es `openspec/specs/lobby-radio/spec.md`, auf das dieses Delta zeigt.

## 0. Vorschlag und Entscheidungen

- [x] 0.1 OpenSpec-Artefakte schreiben und mit `openspec validate radio-categories --strict` prüfen. Commit `docs(openspec): propose radio-categories` (PR #511).
- [x] 0.2 Entscheidungen des Owners in `proposal.md` (Entschieden) und `design.md`, `specs/lobby-radio/spec.md` und `tasks.md` übernehmen. Commit `docs(openspec): record owner decisions for radio-categories`.

## 1. Vorlauf

- [ ] 1.1 Aves- und Minestom-Spike (Env, rot, dann grün): `PersonalTranslatedInventoryBuilder` mit Datenebenen-Funktion. Prüfen: Öffnen zeigt den Inhalt nach einem Tick; `invalidateDataLayout()` ändert den Inhalt eines offenen Fensters an Ort und Stelle (gleiche Inventar-Instanz); `unregister()` schließt nur dieses Fenster ohne rekursives Schließen; der Fenstertitel ändert sich in place oder nur durch Neuöffnen (Ergebnis in `design.md`, R6, und `proposal.md`, Offen 4); `ItemStack.builder(disc).glowing()` bleibt nach `TranslatedItem.of(builder).get(locale)` erhalten. Die Aves-Version, die Titan auflöst, notieren.
- [ ] 1.2 Materialprüfung: alle Symbole der Änderung (`OAK_DOOR`, `SPECTRAL_ARROW`, `BOOK`, `BARRIER`, `REPEATER`, `NOTE_BLOCK`, `ARROW`, `PAPER`, `MUSIC_DISC_13`, `GRAY_STAINED_GLASS_PANE`, `JUKEBOX`, `EMERALD`, `FIREWORK_ROCKET`, `CLOCK`, `JACK_O_LANTERN`) gegen `Material.values()` der Minestom-Version. Test: alle existieren.

## 2. Spielereinstellung mit Stufen (`core`, `features/settings`, `platform/otis`; Commit `feat(settings)`)

Ziel: eine Einstellung, die einen Wert mit fester Stufenliste speichert, nicht nur einen Schalter. Otis nimmt den Wert bereits als JSON an (`origin/main`, PR #181); die Änderung liegt in der Titan-Seite. Schalter bleiben unverändert im Verhalten.

- [ ] 2.1 Test zuerst (Unit, `SettingValueTest`): `Flag` und `Level` sind unterscheidbar; gleiche Werte sind gleich; `Level` nimmt nur ganze Zahlen.
- [ ] 2.2 Test zuerst (Unit, `LevelSettingTest`): der Standard steht in der Liste; die Liste ist nicht leer und ohne Duplikate; eine Liste, die nicht sortiert ist, wird sortiert; ein Standard außerhalb der Liste wirft.
- [ ] 2.3 Test zuerst (Unit, `SettingsRegistryTest`): `registerLevel` legt eine Stufen-Einstellung an; eine Schlüsselkollision mit einem Schalter oder einer Stufe wird abgelehnt; `definitions()` enthält keine Stufen, und `MAX_SETTINGS` gilt für Schalter wie bisher.
- [ ] 2.4 Test zuerst (Unit, `PlayerSettingsServiceTest`): `level` liefert den Standard, solange nichts geladen ist; `setLevel` speichert und ruft keine Schalter-Listener auf; eine geladene Zahl in der Liste wird übernommen; eine Zahl außerhalb der Liste bleibt beim Standard und wird mit einer WARN-Zeile nur für den Schlüssel protokolliert; eine lokale Änderung vor dem Laden gewinnt (`touched`); ein Wert außerhalb der Liste wirft.
- [ ] 2.5 Test zuerst (Unit, `WriteBehindTest` und bestehende Tests): Schreibverzögerung und Wiederholung gelten für `SettingValue` wie bisher; die bestehenden Schalter-Tests laufen unverändert gegen `SettingValue.Flag`.
- [ ] 2.6 Test zuerst (Unit, `OtisJsonTest`): eine JSON-Zahl wird als `Level` gelesen, Text, Gleitkomma und Wahrheitswerte an einer Stufe werden verworfen; `valueBody` für `Level(75)` ergibt `{"value":75}`; `valueBody` für `Flag(true)` ergibt wie bisher `{"value":true}`.
- [ ] 2.7 Test zuerst (Integration, `OtisSettingsStoreIntegrationTest` mit `FakeOtis`): `PUT` sendet den Körper `{"value":75}`; `GET` liefert die Zahl zurück; ein Ausfall von Otis verhält sich wie bei Schaltern (Standard, Wiederholung, Breaker).
- [ ] 2.8 Implementierung: `SettingValue` (sealed, `Flag` und `Level`), `LevelSetting` (Record mit Standard-Prüfung), `PlayerSettings` um `level` und `setLevel` erweitern, `SettingsStore` und `WriteBehind` auf `SettingValue` umstellen, `InMemorySettingsStore` und `OtisSettingsStore` anpassen, `OtisJson` um Zahlen erweitern, `SettingsRegistry.registerLevel`, `SettingsModule` (Laden übernimmt Zahlen in der Stufenliste).
- [ ] 2.9 Architektur: `ColumnArchitectureTest` und Modul-Abhängigkeiten grün; `core` hängt nicht von `features` oder `platform` ab.
- [ ] 2.10 Spec-Delta `specs/lobby-settings/spec.md` (ADDED: Stufenwert speichern) mit der Umsetzung abgleichen.

## 3. Kategorien (rein)

- [ ] 3.1 Test zuerst (Unit, `RadioCategoryTest`): Id-Muster `[a-z0-9_-]{1,32}`; reservierte Ids `minecraft` und `other`; Anzeigename als Text oder als `{de, en}` mit Rückfall auf die andere Sprache; Symbol-Ersatz `PAPER`; „Minecraft“ ist eingebaut und enthält genau die 22 Vanilla-Platten.
- [ ] 3.2 `RadioCategory` (Record aus Id, Namen, Symbol und Reihenfolge) und `RadioCategories` (Sortierung: Minecraft vorn, deklarierte nach Reihenfolge und Id, Sonstige hinten; nur Kategorien mit Titeln) implementieren.
- [ ] 3.3 Test zuerst (Unit, `RadioSongTest`): `VanillaDisc` liefert `minecraft`; `MidiSong` liefert die Kategorie aus dem Konstruktor. Bestehende Tests anpassen.
- [ ] 3.4 `RadioSong` um `category()` erweitern; `VanillaDisc` und `MidiSong` anpassen. `MidiSong` hält den Pfad der Datei und keine Noten mehr (Noten kommen aus dem Cache, Gruppe 6).

## 4. Laden beim Start: Metadaten und Kopf (rein, `@TempDir`)

- [ ] 4.1 Test zuerst (Unit, `SongCatalogTest`, WARN mit `ListAppender`): ein Song ohne `category` landet in „Sonstige“ mit WARN; eine unbekannte Id landet in „Sonstige“ mit WARN und Id; eine reservierte Id wird übersprungen mit WARN; die 21. Kategorie wird übersprungen mit WARN; ein ungültiges Symbol wird durch `PAPER` ersetzt mit WARN.
- [ ] 4.2 Test zuerst (Unit): eine Datei mit fehlendem `MThd`-Kopf wird übersprungen mit WARN; eine fehlende Datei wird übersprungen mit WARN; eine Datei außerhalb von `radio/` (Pfad mit `..`) wird übersprungen; eine gültige Kopfdatei mit kaputtem Rumpf wird geladen (der Rumpf wird erst beim Abspielen gelesen).
- [ ] 4.3 Test zuerst (Unit): keine Grenze für die Titelzahl (500 Einträge werden geladen); keine Grenze für die Dateigröße (eine Kopfdatei von 50 MB wird geladen, ohne dass sie gelesen wird, außer den ersten vier Bytes).
- [ ] 4.4 Test zuerst (Unit): eine alte `songs.yaml` mit 18 Songs ohne `category` und ohne `categories` lädt alle 18 in „Sonstige“; unbekannte Felder werden ignoriert (Rückrollbarkeit, R11).
- [ ] 4.5 `SongCatalog` umbauen: Metadaten und Kopfprüfung, keine Sequenz, keine Notenzähler. Die Konstanten `MAX_SONGS`, `MAX_FILE_BYTES`, `MAX_NOTES_PER_SONG`, `MAX_TICKS_PER_SONG` und `MAX_NOTES_TOTAL` werden aus `RadioLimits` entfernt. `RadioLimits` behält `TICKS_PER_SECOND`, `MAX_NOTES_PER_TICK` und `DISC_END_BUFFER_TICKS`.

## 5. Parser ohne Grenzen (rein)

- [ ] 5.1 Test zuerst (Unit, `MidiSongParserTest`): eine synthetische Sequenz mit 60 000 Noten und 40 Minuten Länge wird ohne Fehler geparst; kaputte Bytes werfen weiterhin `SongFormatException`; Note-Off und Anschlag 0 bleiben ignoriert.
- [ ] 5.2 `MidiSongParser` ohne Größen-, Noten- und Längenprüfung. `SongFormatException` bleibt für ungültige Bytes.
- [ ] 5.3 `VanillaDisc`: die Längenprüfung gegen `MAX_TICKS_PER_SONG` entfällt; sie prüft nur, dass die Länge größer als null ist. Test zuerst (`VanillaDiscsTest` bleibt grün).

## 6. Cache und Laden (rein, injizierte Executors)

- [ ] 6.1 Test zuerst (Unit, `SongNotesCacheTest`, IO-Executor inline): ein Halt lädt die Noten und liefert sie; ein zweiter Halt desselben Titels löst keinen Parse aus (Dedup); ein laufender Titel wird bei neun anderen Titeln nie entfernt; der LRU behält die letzten acht freigegebenen Titel; die Freigabe ist idempotent; ein Parse-Fehler wird als Leerlauf-Eintrag gemerkt und beim zweiten Halt nicht erneut geparst.
- [ ] 6.2 Test zuerst (Unit, `RadioLoadTest`, Warteschlange für den Tick-Übergang): ein Ergebnis mit abweichender Generation wird verworfen und gibt den Halt frei; ein Abmelden während des Ladens verwirft das Ergebnis; ein Stopp während des Ladens verwirft es ebenfalls; ein Treffer im Cache startet ohne Warteschlange.
- [ ] 6.3 Test zuerst (Unit): ein Parse-Fehler sendet die Meldung, zählt `rejected` einmal und protokolliert eine WARN-Zeile mit Dateiname; ein fehlgeschlagener Nachfolger startet nichts.
- [ ] 6.4 `SongNotesCache` (Lock über Map, LRU mit acht Plätzen für Leerlauf-Einträge, Halte, Fehler-Einträge), `SongLoader` (Parse auf dem IO-Executor, Ergebnis über den Tick-Übergang) und `RadioLoad` (Generation, Halt-Freigabe) implementieren. Produktion: virtuelle Threads für IO, `Scheduler.scheduleNextTick` für den Tick-Übergang; `@PreDestroy` fährt den Executor herunter und gibt alle Halte frei.

## 7. Seiten (rein)

- [ ] 7.1 Test zuerst (Unit, `PageLayoutTest`): Seitenzahl für 0, 1, 45, 46 und 200 Titel (0 und 1 ergeben 1 Seite); Index 0 auf Seite 1 Platz 0, Index 44 auf Seite 1 Platz 44, Index 45 auf Seite 2 Platz 0; Klammer einer Seite über dem Ende; Steuerzeile liegt auf 45 bis 53; Blättern an den Grenzen ändert nichts.
- [ ] 7.2 `PageLayout` implementieren (ohne Aves, ohne Minestom).

## 8. Auswahl im Kategorie-Scope (rein)

- [ ] 8.1 Test zuerst (Unit, `RadioSelectionTest`, Seed): Einmal ohne Nachfolger; Schleife derselbe Titel; Zufall nie der eben gespielte und nie ein Titel aus einer anderen Kategorie, über viele Ziehungen; eine Kategorie mit einem Titel wiederholt ihn; eine leere Kategorie liefert nichts; ein Zufall über alle Kategorien ist nicht vorhanden (Entschieden 4).
- [ ] 8.2 Test zuerst (Unit): „Weiter“ in Einmal und Schleife geht in Reihenfolge mit Wrap innerhalb der Kategorie; „Weiter“ im Zufall nie derselbe Titel; ohne laufenden Titel gilt die zuletzt geöffnete Kategorie.
- [ ] 8.3 `RadioSelection` auf Kategorien umstellen: ein Katalog je Kategorie, Auswahl nach der Kategorie des laufenden Titels oder der zuletzt geöffneten, Zufall über `RandomGenerator`.

## 9. Lautstärke und Wiedergabe (rein)

- [ ] 9.1 Test zuerst (Unit, `VolumeTest`): die Stufen sind 25, 50, 75, 100; Linksklick geht eine Stufe hoch, Rechtsklick eine herunter; an den Rändern bleibt der Wert; der Standard ist 100; Prozent werden in den Faktor 0,25 bis 1,0 umgerechnet.
- [ ] 9.2 Test zuerst (Unit, `NoteMapperTest`, `RadioPlaybackTest`): die Lautstärke jeder Note ist `velocity / 127 × Faktor`; die Rangfolge für das Budget von 16 Noten bleibt nach Anschlag.
- [ ] 9.3 Test zuerst (Unit, `VanillaDiscPlaybackTest` mit `RecordingRadioSink`): der Start sendet die Lautstärke der Stufe; eine Änderung während des Laufs ruft den Sink nicht auf; der nächste Start nutzt die neue Lautstärke.
- [ ] 9.4 `Volume` als Hilfsklasse über `LevelSetting` implementieren; `RadioPlayback`, `NoteMapper` und den Start der Schallplatte anpassen.

## 10. Sitzung (rein)

- [ ] 10.1 Test zuerst (Unit, `RadioSessionTest`): Standard ist Übersicht, Seite 1, Modus Einmal; Blättern ändert nur die Seite; ein Kategoriewechsel setzt Seite 1; der Modus bleibt beim Kategoriewechsel; Abmelden vergisst Modus, Kategorie und Seite, aber nicht die gespeicherte Lautstärke (sie liegt in der Einstellung).
- [ ] 10.2 Test zuerst (Unit): die Generation zählt jeden Start, Stopp, Weiter und Abmelden; ein Halt des Ladens wird bei Stopp freigegeben.
- [ ] 10.3 `RadioSession` um Ansicht, Seite und Generation erweitern. Die Lautstärke liest und schreibt die Sitzung nicht; sie geht über den Port.

## 11. Glanz (Unit und Env)

- [ ] 11.1 Test zuerst (Unit, `RadioGlintTest`): das Item des laufenden Titels hat `DataComponents.ENCHANTMENT_GLINT_OVERRIDE`; nach `TranslatedItem.get(locale)` ist der Glanz noch da; ein anderes Item hat keinen Glanz.
- [ ] 11.2 Umsetzung: der Glanz wird beim Bauen der Datenebene für das laufende Item gesetzt (`ItemStack.builder(disc).glowing()`), und die Ansicht aktualisiert sich beim Titelstart und -ende im Tick.

## 12. Menü (Aves, Env)

- [ ] 12.1 Test zuerst (Env, `RadioMenuIntegrationTest`, `env.tick()`, Testverbindung): das Hotbar-Item öffnet die Übersicht mit 54 Plätzen und Steuerzeile auf 45 bis 53; ein Klick auf eine Kategorie öffnet ihre Seite; Blättern; Zurück; der Modusklick ändert den Inhalt desselben Inventars; Links- und Rechtsklick auf die Lautstärke schreiben über den Port.
- [ ] 12.2 Test zuerst (Env): Schließen entfernt die Ansicht, ohne rekursives Schließen; `@PreDestroy` schließt alle offenen Fenster; nach `close()` löst kein Ereignis mehr Code des Moduls aus (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`); zwei Spieler sehen jeweils ihre eigene Seite und ihren eigenen Glanz.
- [ ] 12.3 Test zuerst (Env): Lautstärke überlebt einen Logout (der Wert liegt im Port); ein Parse-Fehler im Abspielen zeigt die Meldung, zählt `rejected` und startet nichts; ein Titel, der während des Ladens gestoppt wird, startet nicht.
- [ ] 12.4 Test zuerst (Env): „Läuft jetzt“ und der Glanz aktualisieren sich bei Titelstart und -ende im offenen Fenster, ohne Neuöffnen.
- [ ] 12.5 `RadioMenuLayout` (Inhalt 0 bis 44, Steuerzeile 45 bis 53), `RadioMenuBuilder` (ein Builder je Fenster, einzige Stelle mit dem Layout), `RadioViews` (öffnen, aktualisieren, schließen) und `RadioModule` (Klick-, Schließ- und Tick-Aktualisierung) umbauen. `RadioMenu` und `Variant` entfallen.

## 13. Telemetrie und Meldungen

- [ ] 13.1 Test zuerst (Unit, `RadioTelemetryTest`): `plays` trägt `category` (`minecraft` für Vanilla, `other` für Sonstige, sonst die Id); `stops` trägt keine Kategorie; `songs{result=rejected}` zählt beim Start-Check und beim Parse-Fehler; kein Attribut mit Spieler-Id.
- [ ] 13.2 `RadioTelemetry` erweitern. INFO beim Start mit Anzahl Kategorien und Titeln.
- [ ] 13.3 Test zuerst (Unit, `RadioMessagesTest`): Schlüssel für Kategorienamen, Sonstige, Seitenanzeige, Zurück, Seite zurück und weiter, „Läuft jetzt“, „Kein Titel läuft“, „Lädt …“, die Meldung für einen nicht lesbaren Titel, Lautstärke und die Lore-Zeilen sind in beiden Sprachen gleich und rendern. Dann `titan/radio/messages_de.properties` und `messages_en.properties`.

## 14. Architektur, Dokumentation, Prüfung

- [ ] 14.1 `ColumnArchitectureTest` grün; `features/radio` hängt weiter nur von `core` ab.
- [ ] 14.2 `docs/lobby-modules.md`, Abschnitt Radio: Übersicht, Kategorien mit Beispiel `songs.yaml`, keine Titelgrenzen, Laden beim Abspielen und der Cache, Lautstärke in Otis mit Stufen und Grenze bei laufenden Schallplatten, Glanz, Hinweis zur Seite. `README.md` nur, wenn dort Items oder Befehle gelistet sind.
- [ ] 14.3 `./gradlew spotlessApply` nur auf geänderte Dateien; `./gradlew :core:test :features:settings:test :platform:otis:test :features:radio:test`; vor dem letzten Häkchen `./gradlew build`; `openspec validate radio-categories --strict` ohne Fehler (die Warnungen zu SHALL/MUST und zur Länge sind bei den bestehenden Changes ebenso).

## 15. Abnahme (Testserver)

- [ ] 15.1 Abnahme auf einem Testserver, auf einer Kopie des Jars und nicht im laufenden Worktree: Übersicht, Blättern bei mehr als 45 Titeln, Modus, Lautstärke bei MIDI und Vanilla, Lautstärke nach Logout (Otis), Zufall bleibt in der Kategorie, Stopp, Weiter, Abmelden, Sprungtest, Glanz, Meldung bei einer kaputten Datei. Nicht ausgeführt, falls kein Testserver verfügbar: im PR als offen vermerken.
- [ ] 15.2 Speichermessung bei 256 MB Heap: 200 Titel mit realistischen Dateien (2 000 bis 5 000 Noten), Cache im Betrieb mit neun Titeln, Heap-Histogramm von `MidiNote`. Ergebnis gegen `design.md`, R5 (Schätzung 32 Byte je Note) im PR.
- [ ] 15.3 Prüfung des Otis-Stands: die produktive Otis-Instanz liefert die Einstellungs-API (`PUT` und `GET` auf `titan:radio.volume`). Ergebnis im PR.

## 16. Commits und Pull Request

- [ ] 16.1 Commits je Typ: `feat(settings): ...` für Gruppe 2; `feat(radio): ...` für die übrigen Gruppen mit Code und Tests; `docs(radio): ...` für `docs/lobby-modules.md`. Kein Commit mischt Typen. Kein Trailer mit Session-Link.
- [ ] 16.2 Pull Request öffnen mit dem Titel `feat(radio): add categories, paging and volume to the radio menu`. Beschreibung auf Englisch im Schema Problem/Approach, mit Verweis auf die Entscheidungen R1 bis R12 in `design.md` und der Angabe, dass die Settings-Änderung in `feat(settings)` steckt. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer. Nach dem Merge archiviert der Maintainer die Änderung als `docs(openspec): archive radio-categories`.
