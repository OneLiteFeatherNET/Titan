# Tasks

Zweig: `feat/lobby-throw` von `origin/main`, nach dem Merge von `lobby-settings`. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`. Die Tests folgen F.I.R.S.T.: keine Sleeps, kein Systemzeit-Zugriff (Clock injiziert), keine geteilte Zustände, jeder Test baut seine Fixtures selbst, ein Verhalten pro Test.

## 0. Voraussetzung (Blocker)

- [ ] 0.1 `lobby-settings` (Otis) ist vollständig umgesetzt und auf `main` gemergt. Erst danach beginnt der Zweig `feat/lobby-throw`. Der Opt-out `titan:throw.target` setzt auf diesem Port auf; der Integrationstest gegen den echten Port folgt in 4.4.

## 1. Spike (Minestom-Verhalten prüfen)

- [ ] 1.1 Auf einem Test-Env prüfen: `addPassenger` mit einem Spieler als Vehicle sitzt auf dem Kopf; Ausrichtung und Offset stimmen aus Sicht eines zweiten Spielers. Nachweis: Notiz im PR.
- [ ] 1.2 Einheit von `setVelocity` in Minestom 26.2 bestätigen und Werte für 8 bis 16 Blöcke/s gegen einen vanilla Knockback-Schlag vergleichen. Ergebnis fixiert die Konstanten aus D3.
- [ ] 1.3 Prüfen, ob `PlayerHandAnimationEvent` bei Luft- und Blockklick feuert, und ob `PlayerEntityInteractEvent` bei leerer Haupthand ohne Item-Benutzung kommt.
- [ ] 1.4 Prüfen, ob `PlayerPacketEvent` mit `ClientInputPacket.shift()` für ein Passagier-Ziel feuert und wie das Sit-Modul darauf reagiert (kein Seat, kein Stand-Up).

## 2. Tests zuerst (rot)

- [ ] 2.1 `ThrowRulesTest` (reine Logik, ohne Env): Aufnahme nur mit `ALLOWED`; Opt-out, Immunität, Sitzen, Tanzen, Elytra, Jump-and-Run, Verkettung blockieren mit dem richtigen `reason`. Jede Regel als eigener Test.
- [ ] 2.2 `ThrowCooldownsTest` mit fixer `Clock`: Werfer 5 s, Paar 60 s nach Abbruch, Ziel 20 s, Auto-Drop nach 15 s. Keine Zeitabhängigkeit ohne Clock.
- [ ] 2.3 `ThrowVelocityTest` (rein): Betrag im Bereich 8 bis 16, Standard 12, vertikale Komponente ≤ 10, bei Blick nach unten nicht negativ.
- [ ] 2.4 `ThrowStateTest` (rein): Zustandsübergänge Idle, Carrying, Thrown, Idle; ein Ziel in höchstens einem Zustand; ein Werfer trägt höchstens ein Ziel.
- [ ] 2.5 `ThrowMessagesTest`: de- und en-Bundle haben dieselben Schlüssel.
- [ ] 2.6 `ThrowModuleTest` (Env, `env.tick()`): Aufnahme und Wurf mit Linksklick; Rechtsklick wirft nicht; Klick innerhalb 500 ms ignoriert.
- [ ] 2.7 `ThrowModuleOptOutTest` (Env, Fake-Settings-Port): Opt-out blockiert, Änderung zwischen zwei Aufnahmen wirkt, Ziel erhält keine Nachricht.
- [ ] 2.8 `ThrowModuleLifecycleTest` (Env): Disconnect, Instanzwechsel und Modulstopp lösen die Trage ohne Impuls.
- [ ] 2.9 `ThrowPortalGuardTest`: Portal-Übergabe während Flug und 2 s nach Landung verweigert; danach wieder erlaubt; Flugende nach 5 s.
- [ ] 2.10 `ThrowTelemetryTest`: Zähler mit Attributen, ohne UUIDs.
- [ ] 2.11 `ColumnArchitectureTest`: Modul hängt nur von `core` ab, nicht von anderen Features.

Nachweis: Die Tests schlagen vor der Umsetzung fehl.

## 3. Umsetzung

- [ ] 3.1 Modul `features/throw` mit `build.gradle.kts`, `package-info.java` (Column `throwColumn`), Eintrag in `settings.gradle.kts`.
- [ ] 3.2 Reine Logik: `ThrowRules`, `ThrowCooldowns` (Clock), `ThrowVelocity`, `ThrowState`.
- [ ] 3.3 `ThrowModule` mit `FeatureNode`: Aufnahme über `PlayerEntityInteractEvent`, Wurf über `PlayerHandAnimationEvent`, Schleichen über `PlayerPacketEvent`, Abbau über `PlayerDisconnectEvent` und Instanzwechsel.
- [ ] 3.4 Portal-Schutz: kleine Abfrage vor der Übergabe im Portal-Modul, ohne Umbau des Portal-Moduls.
- [ ] 3.5 Telemetrie `ThrowTelemetry` mit den vier Zählern.
- [ ] 3.6 Messages `messages_de.properties` und `messages_en.properties` nach dem Muster von `DanceMessages`; Actionbar-Hinweis.
- [ ] 3.7 Settings-Schlüssel `titan:throw.target` über den Port aus `lobby-settings` (Standard an).
- [ ] 3.8 Immunität: Prüfung des Rechts `titan.throw.immune` am Ziel.

## 4. Verifikation

- [ ] 4.1 `./gradlew spotlessApply` und `./gradlew build` sind grün.
- [ ] 4.2 Lokal: zwei Testspieler, Aufnahme, Wurf, Schleichen, Opt-out, Portal-Flug; Ergebnis im PR festhalten.
- [ ] 4.3 Telemetrie in Grafana sichtbar; Log-Zeile beim Modulstart.
- [ ] 4.4 Integrationstest gegen den echten Settings-Port aus `lobby-settings` (nach dessen Merge).

## 5. Doku

- [ ] 5.1 `docs/lobby-modules.md` um das Modul und die Rechte `titan.throw`, `titan.throw.immune` ergänzen; Hinweis, dass lokal niemand werfen kann (`DenyAllPermissionService`).

## 6. Pull Request

- [ ] 6.1 Pull Request auf `main` öffnen mit dem Titel `feat(throw): let permitted players pick up and throw others in the lobby` (Titel und Beschreibung Englisch). Die Beschreibung nennt den Abhängigkeitsschritt `lobby-settings`, die Betreiber-Schritte (LuckPerms: `titan.throw`, `titan.throw.immune`) und die CI läuft grün.
