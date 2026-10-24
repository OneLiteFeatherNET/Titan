# Proposal

## Why

Titan führt seine 353 Testklassen als einen Block aus: Jeder PR-Lauf startet alle Tests, auf Ubuntu, Windows und macOS. Die Testebenen sind nicht festgelegt, deshalb landen Fälle auf der falschen Ebene.

Gemessen an den JUnit-Berichten der Läufe 40066100354 (Ubuntu, Windows) vom 24.10.2026:

- **Reine Generatorlogik im Gesamtlauf:** `ModeGenerationTest` (22 Fälle, 400 Sprünge je Lauf über 12 bis 200 Seeds) braucht 94 s auf Ubuntu und 230 s auf Windows. Im Modul `features/jumprun` ist das der größte Block: 248 s bzw. 423 s Testzeit. Gleiche Muster finden sich in `CourseGeneratorTest` (38 s / 46 s) und `HeadDrawTest` (18 s / 23 s).
- **Der volle Scope in jedem Modul-Lauf:** `apps/cloudnet` (95 s / 146 s) und `apps/local` (23 s / 33 s) bauen den gesamten Avaje-Scope, starten Kindprozesse oder prüfen das gebaute Jar. Diese Tests hängen am Classpath aller Features, weil jede Variante alle Columns bündelt. Eine Änderung an einem einzigen Feature führt sie deshalb immer neu aus.
- **Feature-Tests in `apps/*`:** `apps/cloudnet` enthält Tests für `feature/season`, `feature/example` und `runtime/bootstrap`, obwohl sie ein einzelnes Modul prüfen.
- **Kopierte Testhelfer:** `AdjustableClock` steht sechsmal, `RecordingDeliver`, `FakePlayerCounts` und `FakePermissionService` je viermal, `MutableClock`, `CapturedLog` und `ActiveLobby` je zweimal in verschiedenen Modulen.

Ziel ist, dass jedes Modul auf seiner eigenen Ebene getestet wird: Unit-Tests und Modul-Integration mit Fakes für die Ports anderer Module. Der volle Scope wird nur in einer kleinen Smoke-Menge geprüft. Die CI-Auswahl der Läufe, der Build-Cache und die OS-Matrix sind Gegenstand der Änderung `ci-test-selection`, die auf dieser aufbaut.

## What Changes

- Vier Testebenen mit klaren Regeln, je Ebene ein Gradle-Task oder ein JUnit-Tag:
  - **Unit** und **Modul-Integration** (Cyano-`Env`, Fakes für fremde Ports): Task `test`, alle Module, alle Betriebssysteme.
  - **Docker-Integration**: bestehender Task `integrationTest`, nur mit Docker.
  - **Smoke** (`@Tag("smoke")`): nur in `apps/*`, Task `smokeTest`. Prüft den vollen Scope, den Start des Jars oder die Verdrahtung mehrerer Columns.
  - **Sweep** (`@Tag("sweep")`): lange Seed-Läufe, Task `sweepTest`, nur auf `main` und nachts.
- `ModeGenerationTest` wird aufgeteilt. Der PR-Teil nutzt wenige Seeds und kurze Läufe. Die Sweeps mit 12, 20 und 200 Seeds werden `@Tag("sweep")`.
- Smoke-Tests in `apps/*` werden nach einer festen Regel ausgewählt. Tests, die nur ein Feature betreffen, wandern in das Modul.
- Gemeinsame Testhelfer, die mehr als ein Modul nutzt, wandern nach `core` `testFixtures`. Duplikate werden entfernt.
- Ein Port, den mehr als ein Modul nutzt, bekommt einen Fake und einen Vertragstest in `core` `testFixtures`, nach dem Muster von `RunStoreContract`.
- Zeitbudget: Eine Testklasse im Task `test` läuft auf Ubuntu höchstens 10 s, sonst wird die Ausnahme im Review begründet.

Nicht Teil dieser Änderung:

- Architekturregeln (ArchUnit, statische Analyse). Das übernimmt die parallele Wartbarkeits-Änderung; `ColumnArchitectureTest` bleibt unangetastet.
- Änderungen an Workflows, am Build-Cache und an der OS-Matrix. Das ist `ci-test-selection`.
- Produktverhalten. Kein Modul ändert seine Funktion.

## Delivery

Die Implementierung erscheint als ein Pull Request mit dem Titel `test(modules): run module tests per level and move app-level tests out of apps`. Der Titel ist ein Conventional Commit vom Typ `test` mit dem Scope `modules`. Die Änderung ist nicht breaking. Die Gradle-Tasks `smokeTest` und `sweepTest` gehören zur Testinfrastruktur und daher zu diesem Typ, nicht zu `build`.

Die OpenSpec-Artefakte stehen in `docs(openspec): propose module-test-levels and ci-test-selection`. Die beiden Änderungen sind getrennt, weil sie unterschiedliche Typen ausliefern: `test(modules)` hier, `ci(build)` in `ci-test-selection`. Die Abhängigkeit: `ci-test-selection` setzt die Tags und Tasks dieser Änderung voraus.

## Capabilities

### New Capabilities

- `test-levels`: Testebenen je Modul, Isolation über Ports und Fakes, gemeinsame Testfixtures in `core` und das Zeitbudget für Unit-Tests.

### Modified Capabilities

Keine. Das Produktverhalten ändert sich nicht, die bestehenden Specs bleiben unverändert.

## Impact

- **Code:**
  - `features/jumprun`: `ModeGenerationTest` wird in einen PR-Teil und `ModeGenerationSweepTest` aufgeteilt; `CourseGeneratorTest` und `HeadDrawTest` werden nach Messung geprüft.
  - `apps/cloudnet`, `apps/local`: Smoke-Tags, Umzug der Feature-Tests in ihre Module.
  - `features/tickle`, `elytra`, `daytime`, `portal`, `season`, `navigator`, `lobbyswitcher`, `npc`, `dance`: Duplikate entfernen, Helfer aus `core` nutzen.
  - `core/src/testFixtures`: gemeinsame Helfer und Port-Verträge.
  - `buildSrc`: Tasks `smokeTest` und `sweepTest`, Ausschluss beider Tags aus `test`, Abhängigkeit von `check` auf `smokeTest` im Anwendungs-Plugin.
- **Abhängigkeiten:** keine neuen Bibliotheken. JUnit-Tags gehören zu `junit-jupiter-api`, das bereits eingebunden ist.
- **Nutzertexte:** keine.
- **Betrieb:** keine. Der Build-Ablauf für Entwickler bleibt `./gradlew build`; zusätzlich gibt es `./gradlew smokeTest` und `./gradlew sweepTest`.
