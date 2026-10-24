# Design

## Context

Die Testbasis ist vorhanden: `titan.java-conventions` setzt JUnit, Forks und JaCoCo, `titan.integration-test` legt die Suite `integrationTest` für Docker an, `titan.column` gibt jeder Column `testFixtures(core)`. Module hängen nur an `core` (und `persistence` für jumprun); `apps/*` bündeln alle Columns, deshalb laufen deren Tests bei jeder Feature-Änderung neu (siehe `proposal.md`, Why).

Messbasis aus den JUnit-Berichten von Lauf 40066100354 (Summe der Klassenzeiten):

| Modul (Ubuntu / Windows) | Testzeit | Größte Klasse |
|---|---|---|
| `features/jumprun` unit | 248 s / 423 s | `ModeGenerationTest` 94 s / 230 s |
| `apps/cloudnet` unit | 95 s / 146 s | `BootSmokeTest` 19 s / 23 s, `ConfigurationPrecedenceTest` 14 s / 37 s, `VariantStartTest` 16 s / 24 s |
| `apps/local` unit | 23 s / 33 s | `VariantStartTest` 15 s / – (Duplikat der Variantenprüfung in apps/cloudnet) |
| Summe aller Klassen | 1032 s / 1478 s | |

Gemeinsame Testhelfer: `AdjustableClock` (tickle, elytra, daytime, portal, season), `RecordingDeliver`, `FakePlayerCounts`, `FakePermissionService` (je in vier Feature-Modulen), `MutableClock`, `CapturedLog`, `ActiveLobby` (je zweimal).

## Goals / Non-Goals

**Goals:**

- Jede Testklasse hat genau eine Ebene, erkennbar an Ort und Tag.
- Der Task `test` läuft ohne Server-Scope, ohne Kindprozess und ohne Jar.
- Die Summe der Testzeit im Task `test` auf Ubuntu sinkt von 1032 s auf höchstens 800 s, ohne dass eine Prüfung verloren geht.

**Non-Goals:**

- Keine Architekturregeln (ArchUnit); die parallele Änderung übernimmt sie.
- Keine Workflow-Änderung. Welche Tasks in CI laufen, steht in `ci-test-selection`.
- Keine neuen Testframeworks und keine Testcontainers-Erweiterung.

## Decisions

**D1. Vier Ebenen, zwei davon als JUnit-Tags.**

| Ebene | Ort | Gradle-Task | Läuft wo |
|---|---|---|---|
| Unit und Modul-Integration | `src/test` | `test` | alle Module, alle OS |
| Docker-Integration | `src/integrationTest` | `integrationTest` (bestehend) | Ubuntu mit Docker |
| Smoke | `apps/*/src/test`, Tag `smoke` | `smokeTest` | Ubuntu, einmal je PR |
| Sweep | `src/test`, Tag `sweep` | `sweepTest` | `main` und nachts |

Die Smoke-Ebene nutzt, dass `apps/*` das gebaute Jar oder den vollen Scope braucht. Sweep-Tests sind deterministisch (feste Seeds), nur teuer.
Test: Die Gradle-Konfiguration wird mit `./gradlew test --dry-run` und `./gradlew smokeTest sweepTest --dry-run` geprüft; ein Testlauf zeigt, dass im Task `test` keine Klasse mit Tag `smoke` oder `sweep` vorkommt.
SOLID: offen für Erweiterung (eine neue Smoke-Klasse braucht nur ein Tag, keine Build-Änderung).

**D2. Tags statt neuer Source-Sets.** `test` schließt die Tags per `useJUnitPlatform { excludeTags(...) }` aus. `smokeTest` und `sweepTest` sind `tasks.register<Test>` mit `includeTags(...)`, in `titan.java-conventions` (Sweep) bzw. im Anwendungs-Plugin `titan.app-variant` (Smoke, mit Abhängigkeit auf `shadowJar`).
Verworfen:
- Eigene Source-Sets wie `integrationTest`: jede Ebene bekäme einen eigenen Classpath und eine eigene Abhängigkeitsliste je Modul. Das Muster existiert schon in `titan.integration-test` (zwei Module: `persistence`, `features/jumprun`) und würde je Ebene wiederholt.
- `--tests`-Filter in CI: die Liste der Klassen müsste im Workflow gepflegt werden und vergisst neue Tests.
- Eigenes Modul `apps/smoke`: ein weiteres Gradle-Projekt ohne eigenen Code. Der Smoke-Test braucht ohnehin die Jars aller Varianten.
Test: siehe D1. SOLID: Open/Closed, neue Tests erweitern durch Tags.

**D3. Column-Isolation über den Build und Ports.** Eine Column hängt nur an `core` (`titan.column` bindet nur `core` ein) und testet über Fakes der Ports. Eine Abhängigkeit auf eine andere Column wäre eine bewusste Abweichung im Build-Skript; der Test-Classpath-Check zeigt sie. Tests, die mehrere Columns zusammen verdrahten, werden Smoke-Tests in `apps/*`.
Verworfen: Column-Tests, die den Avaje-Scope booten. Das ist langsam und verdeckt Abhängigkeiten, die im Test nie auffallen.
Test: je Modul `./gradlew :features:<name>:dependencies --configuration testRuntimeClasspath` zeigt keine andere Column; die Verlagerung wird in Task 2 geprüft.
SOLID: Dependency Inversion. Module hängen an Ports, die Tests ersetzen die Implementierung durch Fakes.

**D4. Gemeinsame Helfer in `core` `testFixtures`.** Das Modul `core` hat `testFixtures` bereits, und `titan.column` bindet sie für jede Column ein. Eine Änderung an der Build-Konfiguration ist deshalb nicht nötig.
Verworfen: ein eigenes Modul `testing` (zusätzliches Projekt, eigene Versionierung) und Kopien in den Modulen (Duplikate, die sich auseinanderentwickeln).
Ports mit mehreren Nutzern bekommen einen Vertragstest in `core` `testFixtures` nach dem Muster `RunStoreContract` in `features/jumprun`: Real und Fake laufen dieselben Prüfungen.
Test: die Duplikate werden mit `grep` gezählt (Ergebnis je Name: 1); die bestehenden Tests der Module laufen unverändert grün.
SOLID: Liskovsche Substitution. Der Fake muss sich wie der Port verhalten, das prüft der Vertragstest.

**D5. `ModeGenerationTest` in PR-Teil und Sweep.** Die Tests mit Seed-Schleifen über 12, 20 oder 200 Seeds (etwa `easyMakesNoNewShape`, `everyTowerIsStraightAndWithinTheDefaultHeights`) werden zu `ModeGenerationSweepTest` mit `@Tag("sweep")`. Im PR-Teil bleiben Tests mit wenigen Seeds und einer kürzeren Strecke. Jede Gehstrecke wird im Test selbst berechnet; es gibt keinen statischen Cache, der die Tests verbindet (F.I.R.S.T., Independent).
Verworfen:
- nur die Seeds reduzieren: verliert die Abdeckung der langen Strecken ohne Ersatz;
- `@Disabled` oder komplett nach `sweepTest`: der Generator wäre im PR ungeschützt;
- statischer Cache der Strecken: verbindet Tests über Zustand.
`CourseGeneratorTest` (38 s) und `HeadDrawTest` (18 s) werden in Task 2.2 nach derselben Regel gemessen und entschieden.
Test: die Laufzeit der PR-Teile wird im XML-Bericht gemessen (Ziel höchstens 10 s je Klasse); die Summe der Testfälle bleibt gleich (Regel „Der Umzug verliert keine Prüfung“).
SOLID: nicht betroffen, Testcode.

**D6. Smoke-Auswahl nach einer festen Regel.** Ein Test in `apps/*` ist Smoke, wenn er mindestens eines erfüllt: baut den vollen Avaje-Scope mit allen Columns, startet einen Kindprozess oder das Jar (`BootedVariant`, `ProcessBuilder`), liest das zusammengeführte `application.yaml` oder prüft die Variantendatei `variant.properties`. Alle übrigen Tests in `apps/*` werden in das Modul verschoben, das sie betreffen, oder bleiben in `runtime`, wenn sie nur `runtime` prüfen.
Kandidaten nach der Regel: `BootSmokeTest` (beide Varianten), `VariantStartTest` (beide), `ConfigurationPrecedenceTest`, `ConfigFileWatchIntegrationTest`, `JumprunHeightBandWiringTest`, `WiringTest`, `PlatformBeansWiringTest`, `SpawnReturnWiringTest`, `NavigatorProtectionOrderingTest`, `StandardLoadoutTest`, `ApplicationYamlParityTest`, `ApplicationYamlDefaultsCharacterizationTest`, `SeasonRestartWiringTest`. Feature-Tests: `feature/example/*` und `feature/season/SeasonMapFileNameTest` in das Modul; `runtime/bootstrap/SeasonLobbyWorldTest` nach Messung.
Die Liste ist in Task 2.3 durch Prüfung der Klassen zu bestätigen, nicht durch Raten.
Verworfen: alle Tests in `apps/*` zu Smoke erklären. Dann würden Logiktests wie `ExampleGreetingRuleTest` nur auf Ubuntu in einem einzigen Task laufen.
Test: Anzahl der Smoke-Klassen je App und die Liste der übrigen `apps/*`-Tests werden in der Umsetzung geprüft (`find apps -path '*src/test*' -name '*Test.java'`).
Doppelte Smoke-Tests in `apps/local` und `apps/cloudnet` (Variantenprüfung) bleiben je Variante, ihre gemeinsame Logik liegt in `core` `testFixtures` (`BootedVariant` ist schon dort).

**D7. Zeitbudget ohne neues Werkzeug.** Die JUnit-XML-Berichte liegen bereits als Artefakte `test-reports-*` vor. Eine Testklasse im Task `test` darf auf Ubuntu höchstens 10 s, die Summe höchstens 800 s dauern. Ausnahmen werden im Pull Request begründet.
Verworfen: Gradle Build Scan (Konto und Abhängigkeit von einem Dienst) und ein eigenes Messskript im Repository (KISS, das Artefakt reicht).
Test: die Summen werden vor und nach der Änderung aus denselben Artefakten gelesen und im Pull Request genannt.
SOLID: nicht betroffen.

## Risks / Trade-offs

- [Umzug verliert eine Prüfung] → Testanzahl je Modul vor und nach dem Umzug vergleichen (Requirement „Der Umzug verliert keine Prüfung“).
- [Smoke läuft nur auf Ubuntu, Windows-spezifische Verdrahtung bleibt ungeprüft] → Bewusste Entscheidung; der Windows-Lauf führt `test` aus, und die Frage ist in `ci-test-selection` als offene Frage an den Nutzer gestellt.
- [Sweep läuft erst nach dem Merge auf `main`] → Der PR-Teil mit wenigen Seeds prüft dieselben Eigenschaften; ein Fehler im langen Lauf zeigt sich auf `main` vor dem nächsten Release.
- [Fixture-Umzug ändert Verhalten] → Die bestehenden Tests laufen vor und nach dem Umzug grün, der Umzug erfolgt in eigenen Commits.
- [Zeitbudget wird zu grob] → Die Schwelle 10 s ist ein Richtwert für Reviews; sie wird nicht maschinell erzwungen, damit kein neues Werkzeug nötig ist.

## Migration Plan

1. Tags und Tasks einführen, zuerst ohne Umzug. Im selben Commit wird `check` um `smokeTest` ergänzt, damit der Build auf Ubuntu weiterhin dieselbe Menge prüft wie heute.
2. Feature-Tests aus `apps/*` in ihre Module verschieben, Duplikate in `core` `testFixtures` zusammenführen.
3. `ModeGenerationTest` aufteilen, Langläufer nach Messung entscheiden.
4. Testanzahl und Testzeit aus den Artefakten messen, im Pull Request dokumentieren.

Rollback: jeder Schritt ist ein eigener Commit; ein Rückbau ist ein Revert der Tags und Tasks, Tests bleiben lauffähig.

## Open Questions

Keine. Die Liste der Smoke-Klassen wird in Task 2.3 durch Lesen der Klassen bestätigt. Die Entscheidung über Windows und macOS im PR liegt in `ci-test-selection`.
