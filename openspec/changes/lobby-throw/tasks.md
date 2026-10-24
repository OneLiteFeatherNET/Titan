# Tasks

Zweig: `feat/lobby-throw` von `origin/main`, nach dem Merge von `lobby-settings`. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`. Die Tests folgen F.I.R.S.T.: keine Sleeps, kein Systemzeit-Zugriff (Clock und Ticks injiziert), keine geteilten Zustände, jeder Test baut seine Fixtures selbst, ein Verhalten pro Test.

## 0. Voraussetzung (Blocker)

- [x] 0.1 `lobby-settings` (Otis) ist vollständig umgesetzt und auf `main` gemergt, inklusive Änderungsbenachrichtigung am Settings-Port (Ergänzung, siehe `design.md` D9). Erst danach beginnt der Zweig `feat/lobby-throw`. Der Integrationstest gegen den echten Port folgt in 4.4.

## 1. Spike (Minestom-Verhalten prüfen)

- [x] 1.1 Ladeträger: Stock mit Komponente `consumable` (`consume_seconds` 3600, `animation` = `bow`, keine Partikel, kein Ton). Prüfen, dass `PlayerBeginItemUseEvent` die Animation `BOW` und die Dauer 72000 Ticks liefert und dass der Client die Zieh-Animation zeigt. Nachweis: Notiz im PR mit Screenshot oder Beschreibung.
- [x] 1.2 Loslassen: `PlayerCancelItemUseEvent.getUseDuration()` liefert die Ladezeit in Ticks. Prüfen, dass das Loslassen ein `UPDATE_ITEM_STATE` ist und kein Timeout.
- [x] 1.3 Hotbar-Platzwechsel während der Ladung: bestätigen, dass `PlayerHeldListener` die Nutzung still löscht (ohne Cancel-Event) und dass das Abbrechen über `PlayerChangeHeldSlotEvent` den Ladeträger im Platz hält.
- [x] 1.4 Geschwindigkeit: Einheit von `setVelocity` in Minestom 26.2 bestätigen (Blöcke pro Sekunde). Ergebnis fixiert `T_max` und die Vertikalgrenze aus `design.md` D2 und D3; die Geschwindigkeitsgrenzen wurden in 6.3 neu berechnet.
- [x] 1.5 Vorschau-Physik: Gravitation 0,08, Luftwiderstand 0,91 horizontal und 0,98 vertikal gegen die Minestom-Werte für `EntityType.PLAYER` geprüft (`ThrowRangeTest`); siehe 6.3. Die Vorschau bleibt eine Näherung, weil Kollisionen auf der Client-Seite liegen.
- [x] 1.6 Partikel: `ParticlePacket` mit `end_rod` und `dust` über `player.sendPacket` nur an den Werfer; bestätigen, dass andere Spieler nichts erhalten.
- [x] 1.7 Eventfolge für `PlayerInstanceChangeEvent` (oder den Instanzwechsel-Event der Version) und Tod bestätigen, damit die Hand vor dem Wechsel zurückgesetzt wird.
- [ ] 1.8 Fallback-Test: Falls 1.1 oder 1.2 unzuverlässig ist, Zwei-Klick-Aufladung (Linksklick startet, nächster Linksklick wirft, Ladezeit = Zeit zwischen Klicks) als Fallback prüfen und im PR begründen. Schleichen-Halten ist ausgeschlossen (siehe `design.md` D5).

## 2. Tests zuerst (rot)

- [x] 2.1 `ChargeTest` (rein, ohne Env): Stärke `p = min(t, 30) / 30` für Ticks 0, 1, 15, 30, 90; Tippen unter 2 Ticks ergibt `p = 0`; Geschwindigkeit `8 + 8 p`.
- [x] 2.2 `ThrowRulesTest` (rein): Aufnahme nur mit `ALLOWED`; Opt-out, Immunität, Sitzen, Tanzen, Elytra, Jump-and-Run, Verkettung blockieren mit dem richtigen `reason`. Jede Regel als eigener Test.
- [x] 2.3 `ThrowCooldownsTest` mit fixer `Clock`: Werfer 5 s, Paar 60 s nach Abbruch, Ziel 20 s, Auto-Drop nach 15 s. Keine Zeitabhängigkeit ohne Clock.
- [x] 2.4 `ThrowVelocityTest` (rein): Betrag im Bereich 18 bis 50 (nach 6.3), vertikal ≤ 10, bei Blick nach unten nicht negativ.
- [x] 2.5 `ThrowTrajectoryTest` (rein, deterministisch): gleiche Eingabe liefert gleiche Punkte; höchstens 40 Punkte; Ende bei festem Block; Landemarker am Auftreffpunkt; Gravitation und Luftwiderstand pro Tick nach Formel.
- [x] 2.6 `ThrowStateTest` (umgesetzt als `CarryRegistryTest`, rein): Übergänge Idle, Carrying, Charging, Thrown, Idle; ein Ziel in höchstens einem Zustand; ein Werfer trägt höchstens ein Ziel; Laden ohne Snapshot ist nicht erreichbar.
- [x] 2.7 `ChargeFeedbackTest` (rein): Tonhöhe steigt mit `p`; Ton nur alle 5 Ticks; Actionbar-Leiste wächst mit `p`.
- [x] 2.8 `ThrowMessagesTest`: de- und en-Bundle haben dieselben Schlüssel.
- [x] 2.9 `ThrowModuleChargeTest` (Env, `env.tick()`): Rechtsklick startet Ladung, Loslassen nach N Ticks wirft mit erwarteter Geschwindigkeit; Loslassen ohne Ladung wirft nicht; Klick direkt nach der Aufnahme innerhalb 500 ms ignoriert.
- [x] 2.10 `ThrowModuleHandRestoreTest` (Env): Hand wird auf jedem Pfad exakt zurückgesetzt: Wurf, Schleichen, Auto-Drop, Opt-out, Immunität, Disconnect, Instanzwechsel, Tod, Modulstopp. Ein Test pro Pfad.
- [x] 2.11 `ThrowModuleHotbarAbortTest` (Env): Hotbar-Platzwechsel während der Ladung bricht ab, hält den Ladeträger im Platz, wirft nicht und erhöht `charge_aborts`.
- [x] 2.12 `ThrowPreviewRecipientTest` (Env): Vorschau-Pakete gehen nur an den Werfer; andere Spieler erhalten keines.
- [x] 2.13 `ThrowModuleOptOutTest` (Env, Fake-Settings-Port mit Änderungsbenachrichtigung): Opt-out blockiert neue Aufnahmen; Einschalten während der Trage gibt das Ziel sofort frei, ohne Impuls, mit Hand-Wiederherstellung.
- [x] 2.14 `ThrowModuleLifecycleTest` (Env): Disconnect und Instanzwechsel lösen die Trage ohne Impuls.
- [x] 2.15 `ThrowPortalGuardTest`: Portal-Übergabe während Flug und 2 s nach Landung verweigert; danach wieder erlaubt; Flugende nach 5 s.
- [x] 2.16 `ThrowTelemetryTest`: Zähler mit Attributen, ohne UUIDs, inklusive `charge_aborts`.
- [x] 2.17 `ColumnArchitectureTest`: Modul hängt nur von `core` ab, nicht von anderen Features.

Nachweis: Die Tests schlagen vor der Umsetzung fehl.

## 3. Umsetzung

- [x] 3.1 Modul `features/throw` mit `build.gradle.kts`, `package-info.java` (Column `throwColumn`), Eintrag in `settings.gradle.kts`.
- [x] 3.2 Reine Logik: `ThrowRules`, `ThrowCooldowns` (Clock), `ThrowVelocity`, `Charge` (Stärke aus Ticks), `ThrowState`, `ThrowTrajectory` (Punkte und Landemarker).
- [x] 3.3 Ladeträger-Fabrik: Stock mit `consumable` gemäß 1.1; Snapshot als Feld `Carry.snapshot` statt eigener Klasse `HandSnapshot`.
- [x] 3.4 `ThrowModule` mit `FeatureNode`: Aufnahme über `PlayerEntityInteractEvent`, Ladung über `PlayerBeginItemUseEvent`, Wurf über `PlayerCancelItemUseEvent`, Schleichen über `PlayerPacketEvent`, Hotbar-Platzwechsel über `PlayerChangeHeldSlotEvent`, Abbau über `PlayerDisconnectEvent`, Instanzwechsel und Tod.
- [x] 3.5 Vorschau-Ausgabe über `player.sendPacket(...)`, Aktualisierung alle 2 Ticks, Obergrenze 40 Punkte.
- [x] 3.6 Fortschritt: Actionbar-Leiste, Ton alle 5 Ticks mit steigender Tonhöhe, nur für den Werfer.
- [x] 3.7 Portal-Schutz: kleine Abfrage vor der Übergabe im Portal-Modul, ohne Umbau des Portal-Moduls.
- [x] 3.8 Telemetrie `ThrowTelemetry` mit den fünf Zählern.
- [x] 3.9 Messages `messages_de.properties` und `messages_en.properties` nach dem Muster von `DanceMessages`; Actionbar-Hinweis.
- [x] 3.10 Settings-Schlüssel `titan:throw.target` über den Port aus `lobby-settings` (Standard an), inklusive sofortiger Freigabe bei Einschalten während der Trage.
- [x] 3.11 Immunität: Prüfung des Rechts `titan.throw.immune` bei Aufnahme und Wurf.

## 4. Verifikation

- [ ] 4.1 `./gradlew spotlessApply` und `./gradlew build` sind grün.
- [ ] 4.2 Lokal: zwei Testspieler; Aufnahme, Aufladen mit sichtbarer Leiste, Vorschau nur beim Werfer, Wurf, Schleichen, Opt-out, Hotbar-Abbruch, Portal-Flug; Ergebnis im PR festhalten.
- [ ] 4.3 Telemetrie in Grafana sichtbar; Log-Zeile beim Modulstart.
- [ ] 4.4 Integrationstest gegen den echten Settings-Port aus `lobby-settings` (nach dessen Merge).

## 5. Doku

- [x] 5.1 `docs/lobby-modules.md` um das Modul und die Rechte `titan.throw`, `titan.throw.immune` ergänzen; Hinweis, dass lokal niemand werfen kann (`DenyAllPermissionService`).

## 6. Nachbesserung nach Test auf Produktion

- [x] 6.1 Ein Klick, eine Aufnahme: Wiederholungen der Interaktion ignoriert, Sperre nur bei Erfolg (D14). `fix(throw)`.
- [x] 6.2 Ablehnungen nur in der Actionbar, höchstens einmal pro Sekunde; ohne Recht stumm (D15). `fix(throw)`.
- [x] 6.3 Flugmodell auf Luftwiderstand 0,91 horizontal und 0,98 vertikal korrigiert; Geschwindigkeit 18 bis 50 Blöcke/s, gepinnt in `ThrowRangeTest` (D2, D4). `fix(throw)`.
- [x] 6.4 Der Werfer sieht das getragene Ziel nicht; die Sichtregel wird auf jedem Pfad zurückgesetzt (D16). `fix(throw)`.
- [x] 6.5 Ladeanzeige: XP-Leiste, farbiges Messgerät mit Prozentzahl, Hinweis nur zu Beginn der Trage; Erfahrung auf jedem Pfad zurückgesetzt (D7). `feat(throw)`.
- [x] 6.6 Tickle und Wurf: keine gemeinsame Eingabe, `features/tickle` unverändert (D17).
- [x] 6.7 Dieser Stand im Change nachgezogen. `docs(openspec)`.

## 7. Pull Request

- [ ] 7.1 Pull Request auf `main` öffnen mit dem Titel `feat(throw): let permitted players pick up and throw others in the lobby` (Titel und Beschreibung Englisch). Die Beschreibung nennt den Abhängigkeitsschritt `lobby-settings`, die Betreiber-Schritte (LuckPerms: `titan.throw`, `titan.throw.immune`) und die CI läuft grün.
