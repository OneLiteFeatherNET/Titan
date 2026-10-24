# Tasks

## Execution Plan

Die Wellen laufen nacheinander, weil `buildSrc` und `titan.java-conventions` in den Wellen 2a bis 2c gemeinsam geändert werden. Eine Ausnahme: 2d (Javadoc in `core/src/main`) darf parallel zu 2b (ArchUnit in `core/src/test`) laufen. Jeder Agent startet mit `isolation: "worktree"` von der aktuellen `origin/main` und bekommt den Prompt vollständig (Task-IDs, Pfade, Prüfbefehl, Regeln aus `design.md`, Commit-Konvention). Der Versionskatalog steht in `settings.gradle.kts` (`create("libs")`), nicht in `gradle/libs.versions.toml`.

| Wave | Agent | Task IDs | May Touch | Must Not Touch |
| ---- | ----- | -------- | --------- | -------------- |
| 1 | measure | 1.1–1.4 | nur Messung; Ergebnis in `design.md`, Abschnitt Migration Plan; `settings.gradle.kts` nur für Versionen (1.4) | Produktionscode, Baseline-Dateien |
| 2a | gates-build | 2.1–2.4 | `buildSrc/**`, `titan.java-conventions`, `titan.column`, `settings.gradle.kts` (Versionen) | Produktionscode, `features/*/src/main` |
| 2b | gates-arch | 3.1–3.5 | `core/src/test/**/architecture/**`, `core/src/testFixtures/**/architecture/**`, je Modul `src/test/**/MaintainabilityArchitectureTest.java`, `bridge/src/main` (3.5) | Build-Logik außer dem Guard in 3.3 |
| 2c | gates-pmd | 4.1–4.4 | `buildSrc/**`, `config/quality/**`, `titan.java-conventions` | Produktionscode außer den Suppressions in 4.4 |
| 2d | gates-javadoc | 5.1–5.2 | `core/src/main/**` (nur Javadoc), `core/build.gradle.kts` | Logik-Änderungen |
| 3 | verify | 7.1–7.2 | nur lesen | alles |
| 4 | pr | 8.1 | Git und GitHub | Code |

Regeln für alle Wellen: Test zuerst (rot, dann grün, dann aufräumen); F.I.R.S.T. (keine Sleeps, keine Systemuhr, kein gemeinsamer statischer Zustand, Fixtures in `@TempDir`); ein Verhalten je Testfall; Kommentare nur für das Warum; `./gradlew build` vor dem Abhaken; eine Commit-Konvention je Commit (`build(quality): ...`, `docs(core): ...` für 5.1, `fix(bridge): ...` für 3.5).

## 1. Messen und Entscheidungen (Welle 1)

- [x] 1.1 PMD mit dem Regelset aus `design.md` (D4) über `src/main` aller Module laufen lassen, ohne Fehler zu erzeugen: Verstöße je Regel, Modul und Datei, CC und NCSS je Methode und Klasse, Laufzeit je Modul. Prüfen, dass Spotless an `check` hängt. Nachweis: Messtabelle in `design.md`.
- [x] 1.2 ArchUnit-Messung: Paketzyklen je Modul (`slices()`), Verstöße der Projektregeln aus `specs/maintainability` als Zählung (nicht als Test), Bridge-Abweichungen (`ServiceReadings`, `TitanBridgePermissionExtension`; ob der CloudNet-Classloader SLF4J bereitstellt), Speicherverhalten von `FreezingArchRule`. Nachweis: Tabelle in `design.md`.
- [x] 1.3 `:core:javadoc` mit doclint `missing` laufen lassen: Lücken bei Methoden zählen, Warnungsgruppen prüfen, ob Warnungen den Task abbrechen. Compiler-Warnungen (`-Xlint:deprecation,unchecked`) zählen. Nachweis: Zahlen in `design.md`.
- [x] 1.4 Versionen festlegen: PMD-Version mit Java-25-Unterstützung (Release-Notes lesen; Regelnamen `ProperLogger`, `NcssCount`, `CyclomaticComplexity` in dieser Version bestätigen). Eintrag im Versionskatalog. Nachweis: lokaler Build mit den gepinnten Versionen grün, Link zu den Release-Notes im PR-Text.
- [x] 1.5 Open Questions 1 bis 8 aus `design.md` mit dem Nutzer geklärt; die Antworten stehen in `design.md`, Abschnitt "Open Questions". Nachweis: keine offene Frage mehr, die 2.x oder 3.x blockiert.

## 2. Modulgrenzen und Column-Beschreibung im Build (Welle 2a)

- [x] 2.1 Test zuerst (Unit, `buildSrc`): Die Prüffunktion für die Erlaubnisliste aus D2 liefert für Column → Column, Column → `runtime`, Column → `apps/*`, Plattform → Feature und `common` → `runtime` eine Verletzung; für Column → `core`, Column → `persistence` und Test-Fixtures von `core` keine. Rot. Nachweis: `./gradlew :buildSrc:test` rot, Fehlerausgabe im Commit-Text.
- [x] 2.2 Die Prüffunktion implementieren und in `titan.java-conventions` für alle Module anbinden: Prüfung über alle Konfigurationen beim Konfigurieren, Fehlermeldung nennt Quell- und Zielmodul. Nachweis: `:buildSrc:test` grün, `./gradlew help` grün.
- [x] 2.3 Negativtest manuell: `implementation(project(":features:tickle"))` in `features/navigator/build.gradle.kts` setzen, Konfiguration muss brechen und das Paar nennen; Änderung danach verwerfen, nicht committen. Nachweis: Auszug der Fehlermeldung im PR-Text.
- [x] 2.4 Test zuerst (Unit, `buildSrc`): Die Prüffunktion für `package-info.java` erkennt einen Javadoc-Block vor der Paketdeklaration; ohne Javadoc und nur mit `//` schlägt sie an. Rot, dann implementieren und in `titan.column` anbinden. Nachweis: `./gradlew check` grün für alle 16 Columns.

## 3. Architekturregeln (Welle 2b, ArchUnit)

- [x] 3.1 Test zuerst: Für jede neue Projektregel (Logger, Konsole und `printStackTrace`, Reflexion, Systemuhr, `Thread.sleep` in Tests) je eine Verletzungs-Fixture unter `core/src/test/.../architecture/fixture/` (dieses Paket schließt `ImportOption.DoNotIncludeTests` aus) und ein Testfall, der den Verstoß sieht (rot). Zusätzlich je eine saubere Fixture, bei der die Regel nicht anschlägt (grün). Ein Verhalten je Testfall. Nachweis: `./gradlew :core:test` mit erwarteten Fehlern, dann grün.
- [x] 3.2 `MaintainabilityRules` in `core/src/testFixtures` implementieren, bis 3.1 grün ist. Die Ausnahme für `EventListenerCounter` (Reflexion, Test-Fixture) als benannte Liste mit `because`. Nachweis: `:core:test` grün.
- [x] 3.3 Pro Modul mit Produktionscode eine `MaintainabilityArchitectureTest` schreiben (Module laut 1.1 und 1.2; Columns eingeschlossen; explizit, nicht generiert). Den Guard test-first in `buildSrc` ergänzen (Unit-Test: Modul mit `src/main/java` ohne Architekturtest in einem `@TempDir` schlägt an; mit Test nicht). Nachweis: `./gradlew build` grün, Guard-Test grün.
- [x] 3.4 Paketzyklen: `slices()`-Regel mit `FreezingArchRule`. Test zuerst mit einer Fixture, die einen Zyklus enthält (neuer Zyklus schlägt an). Bestehende Zyklen aus 1.2 in die Freeze-Datei aufnehmen; die Datei steht unter CODEOWNERS. Nachweis: Negativtest in der Fixture, `./gradlew build` grün.
- [x] 3.5 Bridge-Abweichungen beheben (Entscheidung aus 1.5, Open Question 6). `ServiceReadings`: Logger-Feld wird `private static final`. `TitanBridgePermissionExtension`: `System.getLogger` wird SLF4J, sofern der Bridge-Classloader SLF4J bereitstellt (Messung in 1.2). Stellt er es nicht bereit, bleibt `System.getLogger`; dann begründet im Code und als benannte Ausnahme in der Regelklasse mit `because`, und die Begründung steht im PR-Text. Commit `fix(bridge): ...`. Nachweis: `./gradlew :bridge:check` grün.

## 4. Statische Analyse und Größen (Welle 2c, PMD)

- [x] 4.1 Test zuerst (Unit, `buildSrc`): `PmdRatchet` mit XML-Fixtures in einem `@TempDir`. Fälle: Bericht mit zwei Verstößen einer Regel in einer Datei, Baseline mit einem: schlägt an. Baseline mit zwei: besteht. Baseline mit drei: besteht und meldet, dass die Baseline verkleinert werden kann. Neue Datei: schlägt an. Keine Verstöße: besteht. Rot, dann implementieren. Nachweis: `./gradlew :buildSrc:test` grün.
- [x] 4.2 `config/quality/pmd-ruleset.xml` mit den Regeln aus D4 anlegen, PMD im `titan.java-conventions` nur für `src/main` aktivieren (Version aus 1.4, Bericht immer, Ratchet als Abhängigkeit von `check`). Negativtest manuell: `System.out.println` in eine Produktionsklasse eines Moduls setzen, `check` muss mit Datei, Zeile und Regel brechen; danach verwerfen. Nachweis: Auszug im PR-Text.
- [x] 4.3 `config/quality/pmd-baseline.properties` aus dem Messstand aus 1.1 erzeugen. Schwellen 15 (CC), 60 (Methode NCSS), 500 (Klasse NCSS) bestätigen; nur anpassen, wenn die Messung zeigt, dass sie den Bestand blockieren, und die Messung dokumentieren (Open Question 5). Nachweis: `./gradlew build` grün auf dem Branch.
- [x] 4.4 Jede Stelle mit `@SuppressWarnings("PMD.…")`, die durch 4.2 nötig wird, mit einem Kommentar zur Begründung versehen. Nachweis: `grep` über den Branch, jede Zeile mit Kommentar.

## 5. Javadoc der core-API (Welle 2d)

- [x] 5.1 Die 3 Typen ohne Javadoc (`DeliverComponent`, `DeliverType`, `Cancelable`) dokumentieren. Methoden-Lücken aus 1.3: bis zu einer Obergrenze, die 1.3 festlegt, im selben Commit dokumentieren; darüber hinaus in die Baseline. Die Javadoc beschreibt das Verhalten, nicht die Implementierung. Eigener Commit `docs(core): document the public core api`. Nachweis: `./gradlew :core:javadoc` mit Warnungen vor der Aktivierung listet die Lücken, danach ohne (bzw. nur noch Baseline-Einträge).
- [x] 5.2 `:core:javadoc` mit doclint `missing` konfigurieren, `:core:check` hängt daran. Negativtest manuell: öffentliche Methode ohne Javadoc in `core` einfügen, Task muss brechen; danach verwerfen. Nachweis: Auszug im PR-Text.

## 6. Folgearbeit (nicht in diesem Change)

Ungenutzte Abhängigkeiten (Dependency-Analysis-Plugin, SOLLTE) sind Folgearbeit. Ein Dry-Run auf `main` misst den Bestand; danach führt ein eigener Change das Gate ein (Open Question 4, `design.md` D7). Keine Aufgabe in diesem Change.

## 7. Verifikation (Welle 3, read-only)

- [x] 7.1 Jedes Scenario im Spec-Delta `specs/maintainability/spec.md` einem Test, einem Gradle-Lauf oder einer Review-Prüfung zuordnen. Neue Unit-Tests auf F.I.R.S.T. prüfen (kein `Thread.sleep`, keine Systemuhr, kein statischer Zustand, Reihenfolge egal). Baseline-Dateien gegen `origin/main` prüfen: nur die im Plan genannten Einträge, keine Erweiterungen ohne Begründung. Nachweis: Zuordnungstabelle im PR-Text.
- [x] 7.2 `./gradlew build` lokal grün, `openspec validate maintainability-gates --strict` ohne Fehler. Nachweis: Ausgabe der letzten Zeilen im PR-Text.

## 8. Pull Request (Welle 4)

- [ ] 8.1 Pull Request mit dem Titel `build(quality): enforce maintainability rules in the build` öffnen, als Basis `docs/openspec-maintainability` (gestapelt auf #481). Beschreibung auf Englisch: Regeln und Werkzeuge, Baseline-Umfang und Pflege, Ausnahmen, Messergebnisse, Zuordnungstabelle, Negativtest-Auszüge, zurückgestellt (Dependency-Analysis). Einmal `gh pr checks <nummer> --watch --fail-fast` abwarten, nicht mergen. Nachweis: PR-URL, CI-Status.
