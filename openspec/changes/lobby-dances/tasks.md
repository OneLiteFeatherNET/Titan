# Tasks

## 1. Vorlauf

- [x] 1.1 OpenSpec-Artefakte (`proposal.md`, `specs/lobby-dances/spec.md`, `design.md`, `tasks.md`) schreiben und mit `openspec validate --strict lobby-dances` prüfen. Commit `docs(openspec): propose lobby-dances`.
- [x] 1.2 Minestom-Schnittstellen per `javap` am Jar prüfen (Tag-Schreibweg, `CameraPacket`, `EntityTypes.MARKER`, `Dialog.Notice`); Ergebnis in `design.md` D2 festhalten.

## 2. Schnellaktion und Befehl (`features/dance`)

- [x] 2.1 Test zuerst (Unit, `QuickActionDialogTest`): Nach der Registrierung enthält `Registry#getTag(DialogTags.QUICK_ACTIONS)` den Schlüssel `titan:dances`; der Dialog ist ein `Dialog.Notice` mit genau einem Knopf, dessen Aktion `DialogAction.Custom` mit `titan:dances/open` ist. Verifikation: rot, dann grün.
- [x] 2.2 Dialog über Pica 0.1.5 (`DialogTagRegistry#addQuickAction`) registrieren; beim ersten Spawn schickt `DanceModule` jedem Spieler per `sendTags` das `TagsPacket` mit `minecraft:quick_actions`. Ohne Reflexion. Geprüft von `QuickActionTagsIntegrationTest` (Versand nur beim ersten Spawn).
- [x] 2.3 Test zuerst (Unit, `DanceMenuTest`): Dialog zeigt nur erlaubte Tänze (Berechtigung `spin` fehlt → kein Knopf), enthält immer Stopp, jeder Tanz-Knopf sendet `titan:dance` mit `{id}`; Texte in Deutsch und Englisch. Verifikation: rot, dann grün.
- [x] 2.4 `DanceMenu`, `DanceKeys`, `DanceMessages` mit `titan/dance/messages_de.properties` und `messages_en.properties`; Bundle-Test (gleiche Schlüssel in beiden Dateien, alle Schlüssel gerendert).
- [x] 2.5 Test zuerst (Unit, `DancePayloadTest`): gültige Id → Tanz; fehlende Id, unbekannte Id, falscher Typ, `null`-Payload → leer. Verifikation: rot, dann grün.
- [x] 2.6 `DanceCommand` (`tanz`, Alias `dance`) und `DanceCommands` (`@Singleton`, registriert und meldet ab). Test (Env): Spieler öffnet Dialog, Konsole erhält Meldung.

## 3. Tänze (`features/dance`, rein)

- [x] 3.1 Test zuerst (Unit, `DanceSequenceTest`): `every` und `alternating` liefern die Aktionen an den richtigen Ticks; Schritte liegen innerhalb der Länge. Verifikation: rot, dann grün.
- [x] 3.2 Test zuerst (Unit, `DanceTest`): Ids eindeutig; `spin` verlangt `titan.dance.spin`, die übrigen drei nicht; jede Sequenz hat Schritte innerhalb der Länge.
- [x] 3.3 `Dance`, `DanceSequence`, `DanceStep`, `DanceAction` mit den Definitionen aus `design.md` D5.
- [x] 3.4 Test zuerst (Unit, `DanceSessionTest`): Session liefert Aktionen pro Tick und meldet `finished` genau nach der Länge; `movedBeyond` zählt nur horizontale Bewegung über 0,25 Blöcke. Verifikation: rot, dann grün.
- [x] 3.5 `DanceSession` und `DanceMovement` (pure Funktion) implementieren.

## 4. Kamera (`features/dance`)

- [x] 4.1 Test zuerst (Unit, `CameraPlacementTest`): Kamera liegt 3,5 Blöcke vor dem Tanzenden auf Augenhöhe; ihr Blick zeigt auf den Tanzenden (Richtungsvektor und Blickrichtung stimmen überein). Verifikation: rot, dann grün.
- [x] 4.2 `CameraPlacement` und `DanceCamera` (Entität `EntityTypes.MARKER`, `setAutoViewable(false)`, `addViewer`, `spectate`; `close()` mit `stopSpectating` zuerst, dann `remove`).

## 5. Abklingzeit, Berechtigung, Telemetrie

- [x] 5.1 Test zuerst (Unit, `DanceCooldownTest` mit `AdjustableClock`): erster Start frei; innerhalb von drei Sekunden gesperrt; nach drei Sekunden frei; `forget` gibt den Spieler frei. Verifikation: rot, dann grün.
- [x] 5.2 Test zuerst (Unit, `DancePermissionsTest`): freier Tanz ohne Prüfung erlaubt; `spin` nur bei `ALLOWED`, nicht bei `DENIED` oder `NOT_SET`.
- [x] 5.3 `DanceCooldown`, `DancePermissions`, `DanceTelemetry` (Zähler `titan.dance.started{dance}`).

## 6. Start, Stopp und Lebenszyklus (`features/dance`)

- [x] 6.1 Test zuerst (Env, `DanceModuleIntegrationTest`, je ein Verhalten pro Test): Start setzt Kamera (`CameraPacket` mit der Entitäts-Id der Kameraentität) und die Tanzpose; Bewegung stoppt, stellt `STANDING` her, setzt die Kamera auf den Spieler und entfernt die Entität; Verbindungsabbruch räumt auf; Stopp-Knopf stoppt; Angriff stoppt; Bewegungseingabe stoppt; Ende der Dauer stoppt; Start innerhalb der Abklingzeit wird abgewiesen; gesperrter Tanz wird abgewiesen und verändert nichts; Fehler in einem Schritt stellt die Kamera wieder her. Verifikation: rot, dann grün.
- [x] 6.2 `DanceService` (Sitzungen je Spieler, Einzelstart, Abklingzeit, Berechtigung, Task über `Scheduler#submitTask`, Stopp mit verschachteltem `try`/`finally`) und `DanceEffects` (Aktionen am Spieler, Partikel, Töne).
- [x] 6.3 `DanceModule` (`@PostConstruct`: Schnellaktion registrieren, `FeatureNode` anlegen, Listener für `PlayerCustomClickEvent`, `ClientInputPacket`, `PlayerMoveEvent`, `EntityAttackEvent`, `PlayerHandAnimationEvent`, Interaktions-Events mit `onIncludingCancelled`, `RemoveEntityFromInstanceEvent`, `PlayerDisconnectEvent`; `@PreDestroy`: Knoten abhängen, alle Sitzungen beenden).
- [x] 6.4 Test zuerst (Env): Nach `stop()` löst kein Event mehr Code des Moduls aus (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`); `stop()` mit laufendem Tanz stellt die Kamera wieder her.
- [x] 6.5 `package-info.java` mit `@InjectModule(name = "danceColumn", ...)`, `ColumnArchitectureTest` (wie bei den übrigen Columns).

## 7. Dokumentation

- [x] 7.1 `docs/lobby-modules.md`: Abschnitt „Tanz (`dance`)“ mit Schnellaktion, Befehl, Stopp-Pfaden und Hinweis auf die Tag-Registry von Pica.
- [x] 7.2 `README.md`: Liste der Befehle um `/tanz` (Alias `/dance`) ergänzen, sofern dort Befehle aufgeführt sind.
- [x] 7.3 Spec-Delta mit der Implementierung abgleichen (Szenario-Titel, Ids, Abklingzeit, Schwelle, Priorität).

## 8. Prüfung

- [x] 8.1 `./gradlew spotlessApply` und `./gradlew build` grün; `openspec validate --strict lobby-dances` grün.
- [ ] 8.2 Sichtprüfung auf einem Testserver: Schnellaktion im Client mit 1.21.6+, Kamerabild, Pose, Töne, Stopp über Bewegung. Nicht ausgeführt, falls kein Testserver zur Verfügung steht; im PR als offen vermerken.

## 9. Commits und Pull Request

- [x] 9.1 Commit `docs(openspec): propose lobby-dances` (Artefakte und Aufgaben).
- [ ] 9.2 Commit `feat(dance): add lobby dances with quick-action dialog and camera view` (Code, Tests, Dokumentation, abgehakte Aufgaben).
- [ ] 9.3 Pull Request öffnen mit dem Titel `feat(dance): add lobby dances with quick-action dialog and camera view`. Beschreibung auf Englisch: Schnellaktion und Tag-Eintrag (Pica-Tag-Registry, TagsPacket je Spieler beim ersten Spawn, ohne Reflexion), Tänze, Stopp-Pfade, Kamera, Testnachweis, offene Sichtprüfung. Abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach nur auf grüne CI warten (`gh pr checks --watch`); Merge und Release macht der Nutzer.
