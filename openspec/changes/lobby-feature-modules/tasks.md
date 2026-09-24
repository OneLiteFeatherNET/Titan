# Tasks

## Ausführungsplan: parallele Agents mit Worktrees

Integrations-Branch: `feat/lobby-feature-modules`, abgezweigt von `main`. Der Hauptkontext orchestriert nur. Jede Zeile unten ist ein eigener Subagent (Agent-Tool, `isolation: "worktree"`). Alle Agents einer Welle starten gemeinsam in einer Nachricht. Nach jeder Welle gilt: mergen, `./gradlew build` ausführen, Tasks abhaken, committen und erst dann die nächste Welle starten.

| Welle | Agent | Tasks | Modell | Darf anfassen | Darf NICHT anfassen |
|---|---|---|---|---|---|
| A | tests-baseline | 1.1, 1.2 | sonnet | `app/src/test/**` | `src/main/**` |
| A | tests-bugs | 1.3, 1.4 | sonnet | `app/src/test/**`, `common/src/test/**` (neue Dateien) | `src/main/**` |
| A | platform-lifecycle | 2.1–2.4 | sonnet | `app/.../module/**`, `common/.../observability/**` | `Titan.java`, `listener/**` |
| A | config-store | 3.1, 3.3, 3.4 | sonnet | `common/.../config/**` (neue Klassen) | bestehende `AppConfig*` |
| A | archunit-dep | 9.1 | haiku (erledigt, vor der Regel "Implementierung = Sonnet") | `settings.gradle.kts`, `app/build.gradle.kts` | Quellcode |
| B | config-binding | 3.2 | sonnet | `app/.../module/**` (Config-Anbindung) | `feature/**` |
| B | items | 4.1–4.3 | sonnet | `app/.../module/item/**` | andere `module/`-Unterpakete |
| B | navigator-entries | 5.1 | sonnet | `app/.../module/navigator/**` | andere `module/`-Unterpakete |
| B | setup | 7.1 | sonnet | `setup/**` | `app/**` |
| C | f-protection | 6.1 | sonnet | `app/.../feature/protection/**` + Tests | `Titan.java`, alte Klassen (noch nicht löschen) |
| C | f-spawn | 6.2 | sonnet | `feature/spawn/**` + Tests | dito |
| C | f-respawn | 6.3 | sonnet | `feature/respawn/**` + Tests | dito |
| C | f-navigator | 6.4 | sonnet | `feature/navigator/**` + Tests | dito |
| C | f-sit | 6.5 | sonnet | `feature/sit/**` + Tests | dito, `SitHelper` wird kopiert, nicht verschoben |
| C | f-tickle | 6.6 | sonnet | `feature/tickle/**` + Tests | dito |
| C | f-elytra | 6.7 | sonnet | `feature/elytra/**` + Tests | dito |
| D | wiring-cleanup | 6.8, 8.1, 8.2 | sonnet | `Titan.java`, Löschen der Altklassen, `app.json`, README | `feature/**`-Logik |
| E | archrules | 9.2 | sonnet | `app/src/test/.../ArchitectureTest` | `src/main/**` |
| E | docs-template | 10.1, 10.2 | sonnet | `docs/lobby-modules.md`, `app/src/test/.../feature/example/**` | `src/main/**` |
| F | Hauptkontext | 7.2, 11.1, 11.2 | – (manuell bzw. CI, zusammen mit dem Nutzer) | – | – |

Nach jeder Welle, vor dem Start der nächsten, laufen zwei Reviews: ein **Sonnet-Reviewer** (Clean Code, SOLID, DRY, test-first) und ein **Haiku-Checker** (Testpyramide, verbotene Pfade, Header, Spotless, Build). Beide sind read-only. Gültige Befunde behebt ein Sonnet-Fix-Agent im Worktree. Erst dann startet die nächste Welle.

Regel für Welle B: `config-binding`, `items` und `navigator-entries` ergänzen jeweils eine Zugriffsmethode in `ModuleContext` bzw. `ModuleRegistry`. Diese kleinen, erwarteten Konflikte löst der Hauptkontext beim Mergen. Alles Übrige legen die Agents in ihren eigenen Unterpaketen ab.

Regeln für Welle C: Die Feature-Agents legen nur neue Pakete an und löschen nichts. Die alten Listener bleiben kompilierbar, bis Welle D die Verdrahtung umstellt. So gibt es zwischen den sieben parallelen Worktrees keine Konflikte.

## 1. Absicherung vor dem Umbau (Charakterisierungstests)

- [x] 1.1 Fehlende Charakterisierungstests mit `Env` (Cyano) für das heutige Verhalten ergänzen:
  - Schutz: Blockabbau, Blocksetzen, Drop, Hand-Tausch, Inventarklick und Aufheben werden abgebrochen.
  - Höhen-Teleport unter `min` und über `max`.
  - Tod ohne Nachricht mit sofortigem Respawn.
  - Standardausstattung nach Join und nach Respawn (Feder in Slot 4, Elytra auf dem Brustplatz, sonst leer).

  Verifikation: `./gradlew :app:test` ist grün.
- [x] 1.2 Charakterisierungstest für den Navigator ergänzen (Layout: ElytraRace 0, Survival 4, Slender 5, Creative 8, Glas auf den übrigen Plätzen; Klick leitet über `DummyDeliver` weiter). Verifikation: der Test ist grün gegen den heutigen `NavigationHelper`.
- [x] 1.3 Leak-Test für den Navigator schreiben (100 Spieler öffnen den Navigator und verlassen die Lobby, danach unveränderte Listener-Anzahl). Zählweise gemäß design.md, Open Questions. Verifikation: Der Test ist gegen den heutigen Code **rot** und belegt damit das Leck. Bis Gruppe 6 steht er auf `@Disabled` mit Verweis auf Task 6.4.
- [x] 1.4 Config-Rundlauftest für die heutige `app.json` aus dem Repo schreiben (laden, Sitz-Versatz ändern, speichern, neu laden; die Höhengrenzen müssen erhalten bleiben). Verifikation: Der Test ist gegen den heutigen `AppConfigBuilder` **rot** und belegt den Kopier-Bug. Bis Gruppe 3 steht er auf `@Disabled`.

## 2. Plattform: Modul-Lebenszyklus

- [x] 2.1 `LobbyModule`, `ModuleContext`, `ModuleTasks` und `ModuleRegistry` in `app/.../module` anlegen: eigener Node `titan/<id>` pro Modul, Start in Reihenfolge, Herunterfahren rückwärts (Node abhängen → Tasks abbrechen → Anmeldungen entfernen → `disable()`). Verifikation: `ModuleRegistryTest` deckt die Szenarien aus der Spec `lobby-modules` zu Reihenfolge, „keine Events während des Abschaltens“ und „wiederkehrende Aufgaben enden“ ab.
- [x] 2.2 `ModuleContext.listen(Class, Consumer)` mit Guard implementieren. Ein Aufruf nach dem Ende von `enable` wirft `IllegalStateException`. Verifikation: Unit-Test für den späten Aufruf. Test „Listener sind nach dem Abschalten entfernt“ ist grün.
- [x] 2.3 `TitanObservability.guard` um die Modul-ID erweitern (MDC `module`). Verifikation: Ein Test löst in einem Test-Modul eine Ausnahme bei einem Spieler aus. Der geloggte Eintrag enthält Modul-ID und Spieler, die Lobby läuft weiter.
- [x] 2.4 `context.commands().register(Command)` mit Abmeldung beim Abschalten implementieren. Verifikation: Test „Befehle verschwinden“ ist grün.

## 3. Plattform: Config

- [x] 3.1 `ConfigStore` und `ConfigException` in `common/config` anlegen: Dokument als `JsonObject`, Abschnitt per Modul-ID, Deep-Merge mit Defaults, abschnittsweises Speichern. Verifikation: `ConfigStoreTest` deckt die Szenarien „fehlender Abschnitt“, „fehlender Einzelwert“, „erster Start“ und „Rundlauf ohne Änderung“ ab.
- [x] 3.2 `context.config(Class<R>)` an den `ConfigStore` anbinden. Eine `ConfigException` aus dem Compact Constructor bricht den Start mit Modul, Feld und Grund ab. Verifikation: Tests „negative Dauer“ und „unmögliche Höhengrenzen“ an Beispiel-Records.
- [x] 3.3 Eine syntaktisch kaputte `app.json` führt zum Abbruch mit Datei und Position, die Datei wird nicht überschrieben. Verifikation: Test prüft Fehlermeldung und unveränderten Dateiinhalt.
- [x] 3.4 `LegacyConfigMigration` nach der Tabelle in design.md, Entscheidung 5 umsetzen: Sicherung `app.json.v1.bak`, verworfene Schlüssel im Log, `allowedSitBlocks` in beiden Formen lesbar. Verifikation: Test migriert die echte `app.json` aus dem Repo und prüft Zielwerte, Sicherungsdatei und Log-Eintrag.

## 4. Plattform: Items und Hotbar

- [x] 4.1 `LobbyItem`, `ItemSlot` und `ItemRegistry` umsetzen: Identitäts-Tag `titan:item` und ein Dispatch-Listener für `PlayerUseItemEvent`. Verifikation: Tests „Navigator benutzen“ (mit Test-Item) und „gleiches Material, anderes Item“ sind grün.
- [x] 4.2 Die Konfliktprüfung nach `enableAll` bricht den Start ab und nennt Platz und beide Module. Verifikation: Test „Zwei Module wollen Slot 4“ ist grün.
- [x] 4.3 `items().equip(player)` umsetzen: Inventar leeren und alle Items mit festem Platz setzen (Hotbar und Ausrüstung). Einträge fallen weg, wenn ihr Modul abgeschaltet wird. Verifikation: Unit-Test mit zwei Test-Modulen, von denen eines abgeschaltet wird.

## 5. Plattform: Navigator-Einträge

- [x] 5.1 `NavigatorEntry` und `NavigatorEntries` umsetzen: Einträge pro Modul, Versionszähler, Entfernen beim Abschalten, Konfliktprüfung doppelter Plätze beim Start. Verifikation: Tests „Modul abgeschaltet“ und „Zwei Ziele auf Platz 4“ sind grün.

## 6. Features in Module umziehen (je Feature: Test grün → Umzug → alte Klassen löschen)

- [ ] 6.1 `feature/protection`: das Abbrechen von Aufheben, Inventarklick, Blockabbau, Blocksetzen, Hand-Tausch und Drop. Verifikation: Tests aus 1.1 (Schutz) sind grün gegen das Modul.
- [ ] 6.2 `feature/spawn` mit `SpawnConfig(minHeight, maxHeight, simulationDistance)`: Spawn-Instanz und Respawn-Punkt setzen, Simulationsdistanz-Paket senden, Teleport zum Spawn beim Join, `items().equip` beim Join, Höhen-Teleport. Verifikation: Tests aus 1.1 (Höhen-Teleport, Ausstattung nach Join) sind grün.
- [ ] 6.3 `feature/respawn`: Tod ohne Nachricht, sofortiger Respawn, `items().equip` nach dem Respawn. Verifikation: Tests aus 1.1 (Tod, Ausstattung nach Respawn) sind grün.
- [ ] 6.4 `feature/navigator` mit `NavigatorConfig(title, entries)`:
  - Feder als `LobbyItem` in Slot 4.
  - Die Standardziele kommen aus den Config-Defaults.
  - Ein geteiltes Inventar, ein Klick-Listener, Weiterleitung über `Deliver`.
  - `NavigationHelper` und die Caffeine-Abhängigkeit (falls sonst ungenutzt) entfernen.

  Verifikation: Test aus 1.2 grün. Leak-Test aus 1.3 aktiviert und grün. Test „zusätzliches Ziel per Konfiguration“ grün.
- [ ] 6.5 `feature/sit` mit `SitConfig(offset, allowedBlocks)` und eigenen Tags (`titan:sit/*`): Sitzen, Aufstehen per Schleichen, Dismount, Aufräumen beim Disconnect. `SitHelper` aus `common` hierher verschieben. Verifikation: Die vorhandenen Sit-Tests sind grün, dazu Szenario „Sitzen und Aufstehen“ aus der Spec.
- [ ] 6.6 `feature/tickle` mit `TickleConfig(cooldownMillis)` und eigenem Tag. Verifikation: Der vorhandene `TickleListenerTest` ist grün. Der Test für das gewünschte Cooldown-Verhalten steht auf `@Disabled` mit Verweis auf die Folge-Change.
- [ ] 6.7 `feature/elytra` mit `ElytraConfig(boostMultiplier)`: Feuerwerk als `LobbyItem` ohne festen Platz, Nebenhand beim Start und Stopp des Flugs, Boost über den Item-Dispatch. Verifikation: Die vorhandenen Elytra-Tests sind grün, dazu die Szenarien „Feuerwerk beim Fliegen / nach dem Landen / Boost beim Fliegen“.
- [ ] 6.8 `Titan.java` auf die Composition Root reduzieren. Das bedeutet:
  - Abhängigkeiten bauen,
  - `ModuleRegistry.of(protection, spawn, respawn, navigator, sit, tickle, elytra)`,
  - Plattform-Befehle `stop` und `end`,
  - Butterfly,
  - Shutdown-Hook für `disableAll`.

  `initListeners()` entfällt. Verifikation: `./gradlew build` ist grün, und die Lobby startet lokal über `TitanApplication`.

## 7. Setup-Server

- [x] 7.1 `setup/.../AppCommand` auf `ConfigStore.set(section, field, value)` umstellen. `fireworkBoostSlot` entfällt, die Anzeige der Config zeigt die Abschnitte. Verifikation: Test „Sitz-Versatz ändern“ (aus 1.4, jetzt aktiviert) ist grün, die Höhengrenzen bleiben erhalten.
- [ ] 7.2 Setup-Server lokal starten, einen Wert ändern und prüfen, dass die Lobby mit der gespeicherten `app.json` startet. Verifikation: manueller Durchlauf, im PR dokumentiert.

## 8. Aufräumen

- [ ] 8.1 Folgendes löschen:
  - `common/config/AppConfig`, `AppConfigImpl`, `AppConfigBuilder`, `InternalAppConfig`, `AppConfigProvider` samt Tests
  - `common/utils/Tags` und `common/utils/Items`
  - `app/listener/` und `app/helper/`

  Verifikation: `./gradlew build` ist grün, `grep -r "AppConfigBuilder\|utils.Tags\|utils.Items" --include=*.java .` findet nichts.
- [ ] 8.2 `app.json` im Repo und die README auf das neue Format umstellen, `updateRateAgones` entfernen. Verifikation: Die Lobby startet mit der Repo-`app.json` ohne Migrationshinweis im Log.

## 9. Architekturregeln

- [x] 9.1 `archunit-junit5` in den Versionskatalog und als `testImplementation` in `app` aufnehmen. Verifikation: `./gradlew :app:dependencies` zeigt ArchUnit.
- [ ] 9.2 `ArchitectureTest` mit den vier Regeln aus design.md, Entscheidung 10 anlegen. Verifikation: Die Tests sind grün. Eine absichtlich eingebaute Abhängigkeit `tickle → sit` lässt den Build lokal fehlschlagen (danach wieder entfernen).

## 10. Feature-Vorlage und Doku

- [ ] 10.1 `docs/lobby-modules.md` schreiben:
  - Aufbau eines Moduls,
  - Andockpunkte (`listen`, `config`, `items`, `navigator`, `commands`, `tasks`),
  - Regeln für den Tick-Thread (keine Listener zur Laufzeit, kein IO im Handler),
  - Checkliste „neues Feature = neues Paket + eine Zeile“.

  Verifikation: Das Dokument verweist auf ein lauffähiges Beispiel aus 10.2.
- [ ] 10.2 Test-only-Beispielmodul unter `app/src/test/.../feature/example` anlegen (Config-Record, `LobbyItem`, Befehl, Test mit `Env`). Es dient als Kopiervorlage. Verifikation: Sein Test ist grün, und außerhalb des Pakets braucht es keine Änderung (Szenario „Beispielmodul aus der Vorlage“).

## 11. Gesamtabnahme

- [ ] 11.1 Die Lobby lokal mit der migrierten `app.json` starten und im Client prüfen: Feder in Slot 4, Elytra, Navigator (4 Ziele), Sitzen, Kitzeln, Fliegen mit Boost, Höhen-Teleport, Tod und Respawn. Verifikation: Checkliste im PR abgehakt.
- [ ] 11.2 `./gradlew build` inklusive aller Tests und ArchUnit ist grün, `openspec validate lobby-feature-modules` ist ohne Fehler. Verifikation: CI-Lauf grün.

## 12. Pull Request

- [ ] 12.1 Pull Request von `feat/lobby-feature-modules` nach `main` mit dem Titel `feat(app)!: rebuild the lobby around feature modules` öffnen. Die Beschreibung enthält den `BREAKING CHANGE`-Hinweis zu `app.json`, die Rollback-Anleitung aus design.md und die abgehakte Abnahme-Checkliste aus 11.1. Verifikation: Der PR existiert, und der CI-Lauf ist grün.
