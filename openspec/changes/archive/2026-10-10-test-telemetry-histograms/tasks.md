# Tasks

## Execution Plan

Ein Agent, ein Worktree, Branch `test/test-telemetry-histograms` von `origin/main` (#363 ist dort bereits enthalten, `b2f7d28`; vor dem Start `git log origin/main --oneline | grep 363` prüfen).

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | fixture | 1.1–1.3 | sonnet | `core/src/testFixtures/**`, `core/src/test/**` | Produktionscode |
| 2 | migrate | 2.1 | sonnet | `features/jumprun/src/test/**` | alles andere |
| 3 | verify | 3.1 | haiku | read-only | alles |
| 4 | pr | 4.1 | sonnet | Git/GitHub | Code |

Wave 2 wartet auf Wave 1 (das Fixture muss stehen). Regeln: Test zuerst; F.I.R.S.T.: je Test ein frisches `TestTelemetry`, kein `GlobalOpenTelemetry`, kein Warten, ein Verhalten je Test; Commit `test(core): …`, ein Typ. Vor dem Abhaken `./gradlew build`.

## 1. Fixture (Welle 1)

- [x] 1.1 Test zuerst (Unit, neue `TestTelemetryTest` in `core/src/test`): zwei Werte unter `mode=MEDIUM`, einer unter `mode=HARD`; `histogram(...)` liefert Anzahl und Summe je Attributsatz. Rot (Methode fehlt), dann `TestTelemetry.histogram` und `HistogramReading`. Grün.
- [x] 1.2 Test zuerst: ungenutztes Histogramm und unbekannter Attributsatz liefern 0 und 0.0; `histogramPoints` liefert einen Punkt je Attributsatz. Je ein eigener Test. Rot, dann `histogramPoints`. Grün.
- [x] 1.3 Test: Zwei `TestTelemetry`-Instanzen sehen sich nicht (nur eine befüllt, die andere leer); zweimaliges Lesen derselben Instanz liefert kumulative, nicht doppelt gezählte Werte (D2). Nachweis: `./gradlew :core:build`.

## 2. jumprun-Test migrieren (Welle 2)

- [x] 2.1 `RunTelemetryTest.theScoreGoesIntoAHistogramPerMode` auf `testTelemetry.histogramPoints(...)` und `histogram(...)` umstellen; Eigenbau (`SdkMeterProvider`, `InMemoryMetricReader`, handgebaute `Telemetry`) und ungenutzte Importe entfernen. Die vier Aussagen bleiben (ein Punkt, Modus `MEDIUM`, Summe 12, kein `user.id`) und gewinnen Anzahl 1. Vorher den Test gegen das Fixture laufen lassen (er muss vor und nach der Umstellung grün sein, es ist ein Refactoring). Nachweis: `./gradlew :features:jumprun:build`; `git grep InMemoryMetricReader` findet nur noch `TestTelemetry`.

## 3. Verifikation (Welle 3)

- [x] 3.1 Read-only: Jedes Szenario des Spec-Deltas ist einem Test zugeordnet, F.I.R.S.T. erfüllt (kein globaler OTel-Zustand, keine Sleeps), kein Produktionscode geändert (`git diff --stat`). Nachweis: Zuordnungstabelle im PR-Text.

## 4. Pull Request

- [x] 4.1 Pull Request vom Branch `test/test-telemetry-histograms` auf `main` unter dem Titel `test(core): let TestTelemetry read histograms` öffnen (Titel und Beschreibung Englisch), mit Hinweis, dass der jumprun-Test aus #363 migriert wird. Nachweis: PR-URL, CI grün.
