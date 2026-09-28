# Tasks

## Execution Plan

Integrationszweig: `refactor/split-titan-into-columns`, von `origin/main`. Welle-1-Agents verzweigen von `origin/main`, spätere Wellen vom Integrationszweig nach dem Merge der vorherigen Welle. Jede Welle endet erst, wenn `./gradlew build` auf dem Integrationszweig grün ist und der Hauptkontext die Diffs geprüft hat.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | skeleton-spike | 1.1–1.9 | sonnet | `core/**`, `api/**`, `common/**`, `buildSrc/**`, `settings.gradle.kts`, `features/protection/**`, `app/**`, `setup/build.gradle.kts`, `bridge/build.gradle.kts`, `docs/lobby-modules.md` | `features/*` außer `protection`, `openspec/specs/**` |
| 2 | col-navigator | 2.1 | sonnet | `features/navigator/**`, `app/src/**/feature/navigator/**`, `app/src/main/resources/titan/defaults/navigator.yaml` | `core/**`, `buildSrc/**`, `settings.gradle.kts`, `app/build.gradle.kts`, andere `features/*` |
| 2 | col-hotbar-admin | 2.2, 2.3 | sonnet | `features/hotbar/**`, `features/admin/**`, `app/src/**/module/item/**`, `app/src/**/commands/**`, `app/src/main/java/**/Titan.java`, `app/src/main/java/**/app/package-info.java` (nur `LobbyItems` aus `provides` entfernen) | `core/**`, `buildSrc/**`, `settings.gradle.kts`, andere `features/*` |
| 2 | col-spawn-respawn | 2.4, 2.5 | sonnet | `features/spawn/**`, `features/respawn/**`, `app/src/**/feature/{spawn,respawn}/**`, `app/src/main/resources/titan/defaults/spawn.yaml` | `core/**`, `buildSrc/**`, `settings.gradle.kts`, andere `features/*` |
| 2 | col-elytra | 2.6 | sonnet | `features/elytra/**`, `app/src/**/feature/elytra/**`, `app/src/main/resources/titan/defaults/elytra.yaml` | `core/**`, `buildSrc/**`, `settings.gradle.kts`, andere `features/*` |
| 2 | col-sit-tickle | 2.7, 2.8 | sonnet | `features/sit/**`, `features/tickle/**`, `app/src/**/feature/{sit,tickle}/**`, `app/src/main/resources/titan/defaults/{sit,tickle}.yaml` | `core/**`, `buildSrc/**`, `settings.gradle.kts`, andere `features/*` |
| 2 | wave2-review | 2.9 | haiku | read-only | alles |
| 3 | runtime-variants | 3.1–3.8 | sonnet | `app/**` (wird aufgelöst), `runtime/**`, `apps/**`, `buildSrc/**`, `settings.gradle.kts`, `build.gradle.kts`, `.github/workflows/**` | `features/**`, `core/src/main/**` |
| 4 | docs | 4.1–4.3 | sonnet | `README.md`, `docs/**`, `openspec/specs/lobby-modules/spec.md` (nur Purpose) | Code |
| 4 | verify | 4.4 | haiku | read-only | alles |
| 5 | pr | 5.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seinen Task gelten: erst Vorhandenes nutzen (Avaje, Gradle, Minestom-API vor eigener Infrastruktur), Java 25 ohne Preview, Logging nur über SLF4J mit Parametern und festen Levels, keine neuen Nutzertexte (falls doch: i18n über `GlobalTranslator`), keine neuen Metriken/Spans, F.I.R.S.T., Test zuerst, schlanke Kommentare (nur das Warum), Conventional Commits `refactor(build): …` je Welle.

## 1. Gerüst, core und Spike an `features/protection` (Welle 1)

- [x] 1.1 Charakterisierung vorab: den aktuellen Stand von `WiringTest`, `NavigatorProtectionOrderingTest`, `StandardLoadoutTest` und `ApplicationYamlDefaultsCharacterizationTest` als Referenz festhalten (`./gradlew :app:test` grün). Nachweis: Testbericht im Agent-Ergebnis.
- [x] 1.2 Modul `core` anlegen (`java-library`), `:api` darin aufgehen lassen (Pakete bleiben gleich), `api/` entfernen; `:common`, `:setup` und `:app` auf `core` umstellen. Nachweis: `./gradlew build` grün, `api/` existiert nicht mehr.
- [x] 1.3 Test zuerst (Unit, `core`): `FeatureNode.attach` mit doppelter Priorität wirft eine Ausnahme mit beiden Feature-IDs und der Position; mit unterschiedlichen Prioritäten nicht (D6). Danach `FeatureNode` und `ListenerGuard` (D9) nach `core` verschieben, die Prüfung umsetzen und `TitanObservability` auf `core` delegieren lassen; Guard-Tests ziehen mit (erfasster Appender). Nachweis: neue Tests grün, `eventPriorityValuesAreUniqueAcrossFeatures` entfernt.
- [x] 1.4 APIs nach `core` verschieben: `LobbySpawn`, `LobbyItem`, `ItemSlot`, `ItemUseHandler`, `FeatureFlags`, `EntityDismountEvent`, `Cancelable`; `LobbyItems` als Interface (`equip`, `stack`) nach `core`, die bisherige Klasse als Implementierung vorerst in `:app` (D3). `ConfigFeatureFlags` bleibt bis Welle 3 in `common`. Nachweis: `./gradlew build` grün.
- [x] 1.5 `core`-testFixtures anlegen: `TestTitanNode`, `DummyDeliver`, `EventListenerCounter` und `ColumnArchitectureRules` (D7) aus `app/src/test` verschieben; `:app`-Tests nutzen sie. Nachweis: `./gradlew :core:build :app:test` grün.
- [x] 1.6 Convention `titan.column` in `buildSrc` (D8) und Verzeichnis-Scan in `settings.gradle.kts` für `features/*` (später `apps/*`); `:app` hängt per Scan an allen `features/*`, sodass Welle 2 `app/build.gradle.kts` nicht ändern muss. Nachweis: `./gradlew projects` listet `:features:protection`.
- [x] 1.7 Spike (Integration): `features/protection` mit `ProtectionModule`, `package-info.java` mit `@InjectModule(name = …, requires = …)` und `ColumnArchitectureTest` anlegen; `ProtectionModuleTest` zieht mit. Die drei Spike-Fragen aus D2 beantworten; einmal lokal einen verbotenen Import und einen ArchUnit-Verstoß ausprobieren und nicht einchecken. `:app` bekommt `@InjectModule(provides = …)`. Nachweis: `WiringTest` und `NavigatorProtectionOrderingTest` grün, der Shadow-Jar enthält das Avaje-Modul von `protection` in `META-INF/services`.
- [x] 1.8 Standardwerte vorbereiten (D4): `app/src/main/resources/application.yaml` in `app/src/main/resources/titan/defaults/{spawn,sit,tickle,elytra,navigator,runtime}.yaml` aufteilen (Kommentare bleiben); Test zuerst (Unit, `buildSrc`): Verkettung und Konfliktprüfung (doppelter Schlüssel → Fehler mit beiden Dateien); dann Gradle-Task, der die Dateien für `:app` zur Classpath-`application.yaml` und zu `application.example.yaml` zusammenführt. Der Task sammelt `titan/defaults/*.yaml` aus `:app` und aus allen Columns. Nachweis: `ApplicationYamlDefaultsCharacterizationTest` und `ConfigurationPrecedenceTest` unverändert grün, `buildSrc`-Test grün.
- [x] 1.9 Spike-Ergebnis als Abschnitt „Wie eine Column Plattform-Beans bekommt“ in `docs/lobby-modules.md` festhalten und das Muster für Welle 2 im Agent-Ergebnis nennen (genaue `@InjectModule`-Werte). Nachweis: Abschnitt vorhanden, `./gradlew build` grün.

## 2. Columns umziehen (Welle 2, parallel)

Jeder Task: Paket samt Tests aus `app/` nach `features/<x>/` verschieben (`git mv`, Paketname `net.onelitefeather.titan.feature.<x>`), `build.gradle.kts` mit `titan.column`, `package-info.java` nach dem Spike-Muster, `ColumnArchitectureTest`, ggf. die Default-Datei von `app/src/main/resources/titan/defaults/<x>.yaml` nach `features/<x>/src/main/resources/titan/defaults/<x>.yaml` verschieben. Die Tests laufen zuerst unverändert rot/grün als Charakterisierung und danach im neuen Modul grün. Nachweis je Task: `./gradlew :features:<x>:build :app:test` grün.

- [x] 2.1 `features/navigator` (Integration + Unit; `NavigatorModuleLeakTest`, `RecordingDeliver`, `FakeFeatureFlags` ziehen mit; Regel `navigatorDoesNotDependOnAvajeConfig` im eigenen ArchUnit-Test).
- [x] 2.2 `features/hotbar`: Implementierung von `LobbyItems` (`HotbarLobbyItems`), `ItemConflicts` und ihre Tests; `provides = LobbyItems.class` im Modul. Nachweis zusätzlich: `StandardLoadoutTest` in `:app` grün.
- [x] 2.3 `features/admin`: `/stop`, `/end` als `@Singleton`, die sich in `@PostConstruct` beim `CommandManager` registrieren und in `@PreDestroy` abmelden; `Titan.initialize()` registriert sie nicht mehr selbst; `CommandManager` als Bean in `PlatformBeans`. Test zuerst (Integration, Cyano-Env): Nach dem Aufbau sind `stop` und `end` registriert und die Rechteprüfung ist wie bisher; nach dem Herunterfahren sind sie abgemeldet.
- [x] 2.4 `features/spawn` (nutzt `LobbyItems` aus `core`; Tests mit einem Test-Double für `LobbyItems` oder dem aus `hotbar`-testFixtures, dabei ohne Abhängigkeit von `hotbar` im Main-Code).
- [x] 2.5 `features/respawn` (wie 2.4).
- [x] 2.6 `features/elytra` (inkl. `ElytraFixture`; `stack(key)` über `core`-`LobbyItems`).
- [x] 2.7 `features/sit`.
- [x] 2.8 `features/tickle` (`AdjustableClock` zieht mit oder nach `core`-testFixtures, falls mehrere Columns es brauchen).
- [x] 2.9 Review (Haiku, read-only) nach dem Merge der Welle: keine Column hängt an einer anderen oder an `:app`/`runtime`; `app/src/main/java/**/feature` ist leer; alle `EVENT_PRIORITY` unverändert; F.I.R.S.T.-Check der verschobenen Tests (keine Sleeps, keine Systemzeit, kein geteilter statischer Zustand). Nachweis: Review-Bericht ohne offene Befunde, `./gradlew build` grün.

## 3. runtime und App-Varianten (Welle 3)

- [x] 3.1 `:app` in `runtime` umbenennen (`git mv`): `TitanApplication`, `Titan`, `PlatformBeans`, Start-Logs, `TitanPlayer`/`CompatibilityUtil`, Butterfly; `ConfigFeatureFlags` nach `runtime`; `titan/defaults/runtime.yaml` zieht mit. Nachweis: `./gradlew :runtime:build` grün.
- [x] 3.2 Test zuerst (Unit, `runtime`): `missingModules(expected, loaded)` liefert fehlende Namen in stabiler Reihenfolge; leer, wenn alle da sind (D5). Danach die Prüfung beim Start umsetzen (liest `META-INF/titan/variant.properties`, INFO `Variant {} started with modules {}`, Abbruch mit den fehlenden Namen). Nachweis: Unit-Tests grün.
- [x] 3.3 Convention `titan.app-variant` (D8): Shadow mit `mergeServiceFiles()`, Main-Class, `titan-<variant>.jar`, alle `features/*` per Scan mit `exclude`, Erzeugung von `variant.properties`, Verkettung und Konfliktprüfung der Default-Dateien (Task aus 1.8 hierher verschieben), `application.example.yaml` in der Distribution, AOT-Task nur bei `aotCache = true`. Nachweis: `./gradlew :apps:cloudnet:shadowJar` erzeugt `titan-cloudnet.jar` mit `application.yaml` und `variant.properties`.
- [x] 3.4 `apps/cloudnet` (mit AOT-Cache, veröffentlicht als `titan-cloudnet`) und `apps/local` (ohne AOT, nicht veröffentlicht) anlegen; `settings.gradle.kts` scannt `apps/*`. Nachweis: `./gradlew build` baut beide Jars.
- [x] 3.5 Starttest je Variante (Integration): Der volle Scope der Variante baut, jede erwartete Column ist geladen. Dazu ein Test mit einer zusätzlich erwarteten, fehlenden Column, der Abbruch und Meldung prüft. Nachweis: Tests grün in `apps/cloudnet` und `apps/local`.
- [x] 3.6 Querschnitts-Tests nach `apps/cloudnet` verschieben: `WiringTest`, `NavigatorProtectionOrderingTest`, `StandardLoadoutTest`, `ApplicationYamlDefaultsCharacterizationTest` (jetzt gegen die zusammengeführte Datei), `ConfigurationPrecedenceTest`, `ConfigFileWatchIntegrationTest`; `runtime`-eigene Tests bleiben in `runtime`. Den Rest-`ArchitectureTest` in `:app` entfernen, weil D1/D7 ihn abdecken. Nachweis: alle verschobenen Tests grün, Anzahl der Tests insgesamt nicht gesunken (außer den bewusst ersetzten ArchUnit-Regeln, im Agent-Ergebnis aufgelistet).
- [x] 3.7 `:setup` auf `core` + `common` und `:bridge` unverändert auf `common` prüfen; `setup-titan.jar` baut. Nachweis: `./gradlew :setup:build :bridge:build` grün.
- [x] 3.8 CI/Publishing: Die Workflows veröffentlichen `titan-cloudnet` samt AOT-Classifier statt `titan-app`; `build-pr.yml` baut beide Varianten (über `./gradlew build`); release-please unverändert eine Version. E2E-Nachweis (manuell): `java -XX:AOTCache=titan-cloudnet.aot -jar titan-cloudnet.jar` startet lokal mit Welten, ein Client-Join oder Status-Ping klappt, und das Log zeigt `Variant cloudnet started with modules [...]` mit allen neun Columns.

## 4. Doku und Abnahme (Welle 4)

- [x] 4.1 `docs/lobby-modules.md`: Columns, Varianten, „neues Feature = neues Modul unter `features/`“, Prioritätstabelle, Prüfung beim Start statt Bytecode-Test, Standardwerte je Column (`titan/defaults/<column>.yaml`), geteilte ArchUnit-Regeln. Nachweis: Die Doku nennt keine entfernten Klassen/Regeln mehr (grep auf `ArchitectureTest`, `app-titan`).
- [x] 4.2 README und `docs/world-conversion.md`: Jar-Name `titan-cloudnet.jar`/`titan-local.jar`, AOT-Aufruf, Beispieldatei. Nachweis: `grep -rn "app-titan" README.md docs` ist leer.
- [x] 4.3 Purpose von `openspec/specs/lobby-modules/spec.md` direkt anpassen („neues Modul“ statt „neues Paket“). Nachweis: `openspec validate split-titan-into-columns` ohne Fehler.
- [x] 4.4 Verifikation (Haiku, read-only): Specs `app-variants` und `lobby-modules` Szenario für Szenario gegen Tests/Build abgleichen; F.I.R.S.T.-Check aller neuen Tests. Nachweis: Bericht mit Zuordnung Szenario → Test, keine Lücken.

## 5. Pull Request

- [ ] 5.1 Den Pull Request vom Integrationszweig auf `main` unter dem Titel `refactor(build)!: split titan into feature columns and app variants` öffnen, mit dem BREAKING-CHANGE-Footer aus dem Proposal und dem Deploy-Hinweis (neuer Jar-Name, AOT-Cache neu trainieren) in der englischen Beschreibung. Nachweis: PR-URL, CI grün.
