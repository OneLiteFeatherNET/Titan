# Tasks

## Execution Plan

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | platform | 1.1–1.4 | sonnet | `app/.../module/**` (neue Klassen neben den alten), `app/.../bootstrap/PlatformBeans.java`, zugehörige Tests | `app/.../feature/**`, `Titan.java`, `docs/**` |
| 2 | features-a | 2.2, 2.3, 2.5, 2.6 | sonnet | `feature/spawn/**`, `feature/respawn/**`, `feature/sit/**`, `feature/tickle/**` + Tests | Plattform, andere Features |
| 2 | features-b | 2.1, 2.4, 2.7 | sonnet | `feature/protection/**`, `feature/navigator/**`, `feature/elytra/**` + Tests | Plattform, andere Features |
| 3 | cleanup | 3.1–3.5 | sonnet | alte Plattform löschen, `Titan.java`, `ArchitectureTest`, `docs/lobby-modules.md`, `README.md` | `common/**`, Feature-Fachlogik |
| 4 | review | 4.1 | sonnet + haiku | – (read-only) | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Quellcode |

Welle 2 lief tatsächlich als zwei parallele Agents (`features-a`: spawn, respawn, sit, tickle; `features-b`: protection, navigator, elytra) statt in der ursprünglich geplanten Aufteilung 2.1–2.3/2.4–2.7 - reine Verteilung auf zwei gleich große Gruppen, keine inhaltliche Abhängigkeit zwischen den Gruppen.

Integrations-Branch `refactor/dissolve-module-platform` von `origin/main`. Agents in Welle 2 und 3 starten mit `git reset --hard refactor/dissolve-module-platform`. Nach jeder Welle wird gemergt, `./gradlew build` ausgeführt und Design- und Mechanik-Review laufen parallel. Die Tasks werden erst danach abgehakt. Während Welle 2 existieren alte Plattform (`LobbyModule`/`ModuleRegistry`) und neue (`FeatureNode`/`LobbyItems`) nebeneinander. Ein migriertes Feature implementiert `LobbyModule` nicht mehr und läuft über `@PostConstruct`. Solange `ItemRegistry` noch existiert, meldet `LobbyItems` seine Items zusätzlich dort an, damit Spawn und Respawn in beiden Zuständen funktionieren. Welle 3 entfernt diese Brücke.

Jeder Agent-Prompt nennt: KISS, Clean Code, SOLID, DRY, TDD mit Testpyramide, F.I.R.S.T. (keine Sleeps und keine Systemzeit, Ticks explizit mit `env.tick()`, kein geteilter statischer Zustand), built-in first (Avaje-Lebenszyklus, Listen-Injektion, Minestom-`EventNode`-Priorität, `Scheduler`), Java-25-Features, keine neuen Logzeilen, Metriken oder Spans, keine neuen Nutzertexte, Conventional Commits mit `Claude-Session`-Zeile sowie `./gradlew build` vor dem Abhaken.

## 1. Neue Plattform-Bausteine (neben der alten)

- [x] 1.1 Unit-Test zuerst, dann `FeatureNode` (design.md, Entscheidung 1): `attach(parent, featureId, priority)`, `on`, `onIncludingCancelled` mit `TitanObservability.guard(featureId, …)`, `close()`. Nachweis: Unit-Tests ohne Server prüfen Anhängen und Abhängen am Parent, die Priorität des Knotens und dass die Ausnahme eines Listeners gefangen wird. Ein Test prüft, dass die Meldung Feature und Spieler nennt, über einen gefangenen Appender oder einen vorhandenen Test-Hook.
- [x] 1.2 Unit-Test zuerst, dann eine reine Konfliktfunktion für Items: doppelter fester Platz und doppelter Schlüssel werfen eine `IllegalStateException`, die Platz bzw. Schlüssel und beide Items nennt. `Unplaced` doppelt ist erlaubt. Nachweis: `LobbyItemsConflictTest` (Unit) mit allen drei Fällen und geprüften Meldungstexten.
- [x] 1.3 `LobbyItems` (Bean, Konstruktor bekommt `List<LobbyItem>` und `@Named("titan") EventNode`): Konfliktprüfung, Tag `titan:item`, `PlayerUseItemEvent`-Dispatcher mit `guard`, `equip(Player)`, `stack(Key)`, `@PreDestroy` entfernt den Dispatcher. Übergangsweise auch bei der alten `ItemRegistry` anmelden, siehe Execution Plan. Nachweis: Integrationstest (`Env`) prüft, dass `equip` genau die festen Items ablegt, dass eine Benutzung beim richtigen Handler ankommt und dass eine gewöhnliche Feder nichts auslöst.
- [x] 1.4 `PlatformBeans`: `Scheduler`- und `LobbyItems`-Beans. Nachweis: `./gradlew build` grün, bestehende Tests unverändert grün.

## 2. Features auf Beans umstellen

Pro Feature, test-first: bestehende Integrationstests auf direkten Aufbau umstellen (`new XModule(...)`, `start()` / `stop()`). Das Feature wird `@Singleton` ohne `LobbyModule`, mit `EVENT_PRIORITY` (heutiger `@Priority`-Wert), `@PostConstruct start()` über `FeatureNode` und `@PreDestroy stop()`, das zuerst `node.close()` aufruft. Nachweis je Task: Die Tests des Features sind grün, und ein Test prüft, dass nach `stop()` keines seiner Events mehr Code des Features auslöst.

- [x] 2.1 `protection` (100), inklusive `NavigatorProtectionOrderingTest`, sobald 2.4 fertig ist. Bis dahin bleibt der Test unverändert.
- [x] 2.2 `spawn` (200): nutzt `LobbyItems#equip` statt `context.items()`.
- [x] 2.3 `respawn` (300): nutzt `LobbyItems#equip`. Test: Tod ohne Nachricht, danach Standardausstattung.
- [x] 2.4 `navigator` (400): Feder als `@Bean LobbyItem` in einer `@Factory` im Paket, `NavigatorModule` stellt `open(Player)` bereit. `NavigatorProtectionOrderingTest` (beide Reihenfolgen) auf die neue Form umstellen.
- [x] 2.5 `sit` (500).
- [x] 2.6 `tickle` (600).
- [x] 2.7 `elytra` (700): Elytra (`Equipment(CHESTPLATE)`) und Feuerwerk (`Unplaced`) als `@Bean LobbyItem`. Den Task über den injizierten `Scheduler` planen und in `stop()` nach `node.close()` abbrechen. Integrationstest mit `env.tick()`: Der Task läuft vor `stop()` und danach nicht mehr. Boost und Abklingzeit bleiben wie in `lobby-hotbar` beschrieben.

## 3. Alte Plattform entfernen

- [x] 3.1 Löschen: `LobbyModule`, `ModuleContext`, `ModuleRegistry`, `ModulePlatform`, `ModuleLifecycleException`, `ModuleCommands(+Impl)`, `ModuleTasks(+Impl)`, `ModuleItems(+Impl)`, `ItemRegistry`, `EquipPlan`, `DuplicateItemKeyDetector`, `SlotConflictDetector`, `DuplicateItemKeyException`, `ItemPlacementConflictException` und ihre Tests. Außerdem `ModuleHarness(+Test)` und `ModulePlatformFixture`. Die Übergangs-Brücke aus 1.3 entfernen. Nachweis: `rg "LobbyModule|ModuleContext|ModuleRegistry|ModuleHarness|ItemRegistry" app` findet nichts, und `./gradlew :app:test` ist grün.
- [x] 3.2 `Titan.java`: Modulliste und `ModuleRegistry` entfernen, Shutdown-Reihenfolge `beanScope::close` vor `butterfly::terminate`. Nachweis: Ein Wiring- bzw. Smoke-Test (design.md, Entscheidung 4) bestätigt 7 Features, `LobbyItems` und 3 Items. Ein Test prüft, dass ein Feature, das beim Start eine Ausnahme wirft, den Aufbau abbricht und dass die Meldung die Feature-Klasse nennt. Ein Test prüft, dass nach `beanScope.close()` keine Feature-Knoten mehr am Titan-Knoten hängen.
- [x] 3.3 `ArchitectureTest` auf die Regeln aus design.md, Entscheidung 5 umstellen, einschließlich der Eindeutigkeit von `EVENT_PRIORITY`, deren Meldung beide Features nennt. Nachweis: Jede neue Regel wird einmal mit einem absichtlichen Verstoß lokal rot gesehen. Der Verstoß wird nicht committet, dann ist die Regel grün.
- [x] 3.4 `docs/lobby-modules.md` neu schreiben (deutsch, kurz): Feature = Bean, `FeatureNode`, Items als `@Bean`, `Scheduler`, Tests ohne Harness, Architekturregeln, Checkliste „nur ein neues Paket“. `README.md` anpassen. Nachweis: `rg "LobbyModule|ModuleContext|context\.(listen|items|tasks|commands)" docs README.md` findet nichts.
- [x] 3.5 Gesamtbuild und Leck-Tests. Nachweis: `./gradlew build` ist grün (Spotless, ArchUnit, alle Tests). `NavigatorModuleLeakTest` (100 Spieler, Listener-Anzahl gleich) ist grün.

## 4. Review

- [ ] 4.1 Design-Review (Sonnet) und Mechanik-Review (Haiku) parallel, beide read-only, gegen die Deltas von `lobby-modules` und `lobby-hotbar`, KISS, built-in first und F.I.R.S.T. Nachweis: Beide Berichte sind ohne offene Befunde, oder die Befunde sind von einem Sonnet-Agent behoben.

## 5. Pull Request

- [ ] 5.1 Pull Request auf `main` mit dem Titel `refactor(app)!: replace the lobby module platform with plain avaje beans`, englische Beschreibung, `BREAKING CHANGE:`-Footer aus proposal.md. Nachweis: PR-URL, CI grün.
- [ ] 5.2 Nach dem Merge archivieren, dabei den `## Purpose` von `lobby-modules` („Features sind Beans …“) und `lobby-hotbar` („Features stellen Items als Beans bereit …“) anpassen. Commit `docs(openspec): archive dissolve-module-platform`. Nachweis: `openspec validate --specs` ohne Fehler.
