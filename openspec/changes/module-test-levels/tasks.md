# Tasks

Zweig: `test/module-test-levels` von `origin/main`. Jeder Commit hat genau einen Typ, hier `test(modules)`; die Gradle-Tasks gehören zur Testinfrastruktur und tragen denselben Typ. Vor dem Abhaken einer Aufgabe läuft `./gradlew build` auf Ubuntu. Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`.

## Ausführungsplan

| Welle | Agent | Aufgaben | darf ändern | darf nicht ändern |
|---|---|---|---|---|
| 1 | Lesen | 1.1, 1.2 | nur Scratch-Verzeichnis | Repository |
| 2 | Umsetzung, Worktree | 2.1 | `buildSrc/` | Testklassen |
| 2 | Umsetzung, Worktree | 2.2, 2.3 | `features/jumprun/src/test` | andere Module |
| 2 | Umsetzung, Worktree | 2.4, 2.5 | `apps/*/src/test`, Zielmodule der Verschiebung | `buildSrc/`, `core/src/testFixtures` |
| 2 | Umsetzung, Worktree | 2.6, 2.7 | `core/src/testFixtures`, Kopien in `features/*/src/test` | Produktionscode |
| 3 | Umsetzung | 2.8 | `docs/lobby-modules.md` | Code |
| 4 | Prüfung | 3.1 | nur Scratch-Verzeichnis | Repository |
| 5 | Pull Request | 3.2, 3.3 | nur Git und `gh` | Repository-Inhalt |

Welle 2 läuft parallel. Der Integrationszweig führt die Ergebnisse zusammen; danach läuft der Build. Die Reviews nach jeder Welle (Clean Code, F.I.R.S.T.) übernehmen die Agenten nach den Regeln des Schemas.

## 1. Ausgangswerte

- [ ] 1.1 Testzeit je Klasse und Summe je Modul aus den `test-reports-*`-Artefakten eines PR-Laufs auf dem aktuellen `main`-Stand lesen (Skript im Scratch-Verzeichnis, nicht im Repository). Nachweis: Tabelle im Pull Request; die Summen weichen höchstens 5 % von `design.md` ab.
- [ ] 1.2 Testfälle je Modul und Ebene zählen (`src/test`, `src/integrationTest`, Tag-Suche nach `smoke` und `sweep`). Nachweis: Tabelle im Pull Request; Ausgangswert 353 Klassen in `src/test` und 7 in `src/integrationTest`.

## 2. Umsetzung

- [ ] 2.1 Tags und Tasks in `buildSrc` (`test(modules)`): `test` schließt die Tags `smoke` und `sweep` aus; `smokeTest` läuft in den Anwendungs-Plugins mit Abhängigkeit auf `shadowJar`; `sweepTest` läuft nur mit Tag `sweep`; `check` der Anwendungen hängt von `smokeTest` ab. Test zuerst: eine Testklasse mit Tag `sweep` in `features/jumprun` läuft nur in `sweepTest`, danach wird die Markierung zurückgenommen. Nachweis: `./gradlew test --dry-run` und `./gradlew smokeTest sweepTest --dry-run` zeigen die Tasks, der Testlauf zeigt die Trennung.
- [x] 2.2 `ModeGenerationTest` aufteilen (`test(modules)`): Tests mit Seed-Schleifen über 12, 20 oder 200 Seeds nach `ModeGenerationSweepTest` mit `@Tag("sweep")`; im PR-Teil wenige Seeds und kürzere Strecken; gleiche Prüfungen in beiden Teilen. Test zuerst: die Prüfung „Schwierigkeit steigt“ und „keine Lücke über zwei“ laufen vor und nach dem Umzug gleich. Nachweis: XML-Bericht: PR-Teil höchstens 10 s auf Ubuntu; die Summe der Testfälle bleibt 22.
- [ ] 2.3 `CourseGeneratorTest` (38 s) und `HeadDrawTest` (18 s) nach derselben Regel messen. Nachweis: je Klasse eine Zeile im PR mit Entscheidung (kürzen, aufteilen, Sweep oder unverändert mit Begründung).
- [ ] 2.4 Smoke-Auswahl in `apps/*`: jede Klasse gegen die Regel in `design.md` (D6) prüfen und mit `@Tag("smoke")` markieren, sofern sie zutrifft. Test zuerst: nach Markierung läuft `./gradlew test` keine Apps-Klasse mit dem Tag. Nachweis: Liste der markierten und der übrigen Klassen im Pull Request.
- [ ] 2.5 Feature-Tests aus `apps/cloudnet` in ihr Modul verschieben: `feature/season/SeasonMapFileNameTest` nach `features/season`; `feature/example/*` entscheiden: Beispielcode bleibt nur als Dokumentation, Tests dazu werden entfernt, wenn sie keinen Produktionscode prüfen. Nachweis: `find apps/cloudnet/src/test -path '*feature*'` zeigt nur noch Smoke-Klassen; Testanzahl je Modul gleich wie in 1.2 (Ausnahmen dokumentiert).
- [ ] 2.6 Duplikate in `core` `testFixtures` zusammenführen: `AdjustableClock`, `RecordingDeliver`, `FakePlayerCounts`, `FakePermissionService`, `MutableClock`, `CapturedLog`, `ActiveLobby`. Test zuerst: die bestehenden Tests der Module laufen vor dem Umzug grün (Charakterisierung). Nachweis: je Name genau eine Klasse im Repository (`find` zählt 1); `./gradlew test` grün.
- [ ] 2.7 Ports mit mindestens zwei Nutzern ermitteln (Interfaces, die in mehr als einem Modul importiert werden). Je Port einen Fake und einen Vertragstest in `core` `testFixtures` anlegen, der Real und Fake gleich prüft. Test zuerst: der Vertragstest schlägt für einen absichtlich falschen Fake fehl. Nachweis: Liste der Ports im Pull Request; Vertragstest grün mit beiden Implementierungen.
- [ ] 2.8 Abschnitt „Testebenen“ in `docs/lobby-modules.md` mit der Tabelle aus D1 und der Smoke-Regel aus D6. Nachweis: Abschnitt vorhanden, die Tabelle nennt die Tasks `test`, `integrationTest`, `smokeTest`, `sweepTest`.

## 3. Prüfung und Pull Request

- [ ] 3.1 `./gradlew build` auf Ubuntu grün. Messung nach D7 aus einem PR-Lauf des Branches: Summe der Testzeit im Task `test` höchstens 800 s, kein Test im Task `test` über 10 s ohne Begründung, Testanzahl je Modul gleich wie in 1.2. Nachweis: Tabelle im Pull Request.
- [ ] 3.2 `openspec validate --strict module-test-levels` ohne Fehler; die Warnungen zu deutschen Formulierungen sind wie bei den übrigen Änderungen erlaubt. Nachweis: Ausgabe des Befehls.
- [ ] 3.3 Pull Request öffnen mit dem Titel `test(modules): run module tests per level and move app-level tests out of apps`. Beschreibung auf Englisch mit den Messwerten aus 1.1 und 3.1 und der Session-URL als letzter Zeile. Nachweis: PR-URL; `gh pr checks <n> --watch` einmal.
