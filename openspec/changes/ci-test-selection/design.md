# Design

## Context

Der PR-Workflow `build-pr.yml` ruft den geteilten Workflow `gradle-build-pr.yml@v2.9.0` (OneLiteFeatherNET/workflows) auf. Dieser Workflow hat:

- einen Job „Detect changes“ mit `dorny/paths-filter`, der nur Code-Änderungen durchlässt (Dokumentation überspringt den Build);
- eine Matrix über `runs-on`, die für alle Einträge denselben `gradle-task` ausführt (ein einziger Input);
- `cache-read-only` für alle Refs außer dem Default-Branch;
- eine `concurrency`-Gruppe aus Workflow-Name und PR-Nummer mit `cancel-in-progress`;
- den festen Check-Namen „Test results“.

Heute gibt es keinen Lauf auf `main`. Der Gradle-Build-Cache wird deshalb nie geschrieben, und jeder PR-Lauf führt alle 281 Tasks aus. Der offene PR #472 (`ci(build): speed up pull request builds`) ergänzt einen `push`-Trigger für `main`. Seine lokale Messung zeigt den Effekt: Eine Änderung in `features/sit` führt nur `features:sit:test`, `apps:cloudnet:test` und `apps:local:test` aus, in 1 min 09 s; ein kalter Lauf dauert 8 min 51 s.

Gemessene Läufe (Job-Dauern aus `gh run view --json jobs`):

| Lauf | Ubuntu | macOS | Windows | Wand-Zeit |
|---|---|---|---|---|
| 40066100354 (`feat/lobby-settings`, 24.10.) | 517 s | 462 s | 765 s | 13 min 19 s |
| 40023690236 (`feat/tickle-portal-sounds`, 23.10.) | 747 s | 658 s | 983 s | 16 min 55 s |
| Median der letzten 15 Läufe (laut #472) | 599 s | 677 s | 899 s | 964 s |
| Docs-PR 40021681709 | übersprungen | übersprungen | übersprungen | Detect changes 5 s |

Die Job-Dauer folgt grob der Testzeit: In beiden gemessenen Läufen liegt sie bei rund der Hälfte der Summe aller Testzeiten aus den JUnit-Berichten (Ubuntu 517 s bei 1032 s, Windows 765 s bei 1478 s). Diese Verhältniszahl ist die Grundlage der Zielwerte unten.

Die Testebenen legt `module-test-levels` fest. Diese Änderung setzt die Tags `smoke` und `sweep` und die Tasks `smokeTest` und `sweepTest` voraus.

## Goals / Non-Goals

**Goals:**

- Ein PR führt nur die Tests aus, deren Eingaben sich geändert haben, samt ihrer Abhängigkeiten.
- Der volle Scope und die Sweeps laufen nur einmal je PR (Smoke, Ubuntu) bzw. auf `main` und nachts.
- Die Laufzeitziele aus der Spec sind mit den Job-Zeiten belegbar.

**Non-Goals:**

- Kein eigenes Werkzeug zur Erkennung betroffener Module. Gradle kennt den Abhängigkeitsgraph.
- Keine Änderung am geteilten Workflow im Repository `OneLiteFeatherNET/workflows`. Die Caller-Dateien in Titan genügen.
- Kein Remote-Build-Cache. Er ist eine eigene Infrastrukturentscheidung (siehe Open Questions).
- Keine Änderung der Release- oder Publish-Workflows.

## Decisions

**C1. Auswahl über den Gradle-Build-Cache und Up-to-date-Prüfung.** Gradle führt einen Testtask nur aus, wenn seine Eingaben sich geändert haben. Seine Eingaben umfassen den Testklassenpfad, und der enthält alle Abhängigkeiten. Modul und Abhängige werden damit automatisch neu geprüft; unveränderte Module kommen aus dem Cache.
Verworfen:
- Pfadfilter je Modul (`dorny/paths-filter` mit `gradle -p`): Der Abhängigkeitsgraph würde von Hand nachgeführt. Die Features werden per Scan in `settings.gradle.kts` gefunden, eine neue Column müsste im Workflow stehen. Transitive Abhängigkeiten (`core` → alles) werden leicht vergessen.
- Eigenes Skript, das betroffene Projekte aus `git diff` errechnet: ein zusätzliches Werkzeug, das ein Gradle-Feature nachbaut (KISS).
Test: ein PR, der nur `features/sit` ändert, zeigt in den Task-Ausgaben `FROM-CACHE` oder `UP-TO-DATE` für alle Testtasks außer `features:sit:test` und den Smoke-Tests. SOLID: nicht betroffen, reine Build-Konfiguration.

**C2. Build-Logik-Änderungen ohne Sonderregel.** Ändert sich `buildSrc/`, der Versionskatalog oder `settings.gradle.kts`, ändern sich die Klassenpfade und Konfigurationen aller Testtasks. Gradle führt sie dann aus. Eine Pfadregel im Workflow wäre eine zweite Quelle der Wahrheit.
Verworfen: Pfadfilter im Workflow, der bei `buildSrc/**` den Cache leert. Das bräuchte Pflege und ist für Gradle nicht nötig.
Test: ein PR, der `gradle/libs.versions.toml` ändert, zeigt keine `FROM-CACHE`-Ausgabe für Testtasks.
Hinweis: `gradle.properties` (JVM-Argumente) gehört nicht zu den Eingaben der Testtasks und löst keinen vollen Lauf aus.

**C3. Smoke einmal auf Ubuntu, Mac und Windows mit `test`.** Der geteilte Workflow führt für alle Einträge der Matrix denselben `gradle-task` aus. Deshalb entstehen zwei Aufrufe:
- `build-pr.yml` (bestehend, Name „Build PR“): `runs-on: ["ubuntu-latest"]`, `gradle-task: build` (enthält `check` mit `smokeTest` und Docker-Integrationstests);
- `build-pr-cross-os.yml` (neu): `runs-on: ["windows-latest", "macos-latest"]`, `gradle-task: test`.
Die Caller-Dateien haben unterschiedliche `name:`-Werte. Die `concurrency`-Gruppe des geteilten Workflows enthält den Workflow-Namen. Mit gleichem Namen würden sich die beiden Aufrufe vermutlich gegenseitig abbrechen (`cancel-in-progress`); unterschiedliche Namen trennen die Läufe. Das wird in Task 2.2 geprüft.
Verworfen:
- Alle drei Betriebssysteme mit `build`: Smoke-Tests liefen dreimal, und der Windows-Lauf wäre der Engpass.
- Smoke nur über Matrix-Bedingung `if: matrix.os == ...`: der geteilte Workflow lässt keine Bedingung je Matrix-Eintrag zu.
- Nur Ubuntu im PR: Die Frage ist als Open Question an den Nutzer gestellt, siehe unten.
Test: ein PR zeigt drei Build-Jobs, `smokeTest` läuft nur im Ubuntu-Job, die Zeilen „Test results“ werden gezählt (zwei Check-Runs mit demselben Namen sind bekannt, siehe Risiken).

**C4. `main` und Nacht als eigener Caller mit `force-build`.** Eine Datei `build-main.yml` mit den Triggern `push` (Branch `main`) und `schedule` ruft den geteilten Workflow mit `force-build: true` auf:
- Push auf `main`: `gradle-task: build sweepTest`, alle drei Betriebssysteme; schreibt den Cache.
- Nachts (`schedule`): `gradle-task: build sweepTest --no-build-cache`, alle drei Betriebssysteme; das Flag wird direkt an `./gradlew` gereicht, der geteilte Workflow kennt keine eigene Option dafür.
Test: manueller Lauf (`workflow_dispatch` oder Push nach Merge) zeigt die Sweeps in den Testberichten; der Nachtlauf zeigt keine `FROM-CACHE`-Ausgabe.
Risiko: Ein Push auf `main` bricht einen laufenden Nachtlauf ab, weil beide die Gruppe `main` teilen. Siehe Risiken.

**C5. Laufzeitziele aus Job-Zeiten und Verhältniszahl.** Die Ziele werden aus den gemessenen Job-Zeiten abgeleitet, nicht aus Vermutungen:

| Messgröße | Heute (gemessen) | Ziel | Herleitung |
|---|---|---|---|
| Docs-PR | 5 s Detect, Build übersprungen | ≤ 30 s | gemessen, bleibt |
| Feature-PR, warmer Cache | nicht messbar, kein Lauf auf `main` schreibt den Cache | ≤ 5 min | lokal: 1 min 09 s gegenüber 8 min 51 s kalt; kalter CI-Ubuntu-Lauf 8 min 37 s entspricht dem lokalen Wert; Overhead für Checkout, JDK und Cache-Wiederherstellung geschätzt 1,5 bis 3 min |
| core/runtime-PR, kalt | 13 min 19 s bzw. 16 min 55 s; Median 964 s | ≤ 10 min | Job-Dauer ≈ 0,5 × Testsumme. Ubuntu: 1032 s → rund 950 s nach Entfernen von `ModeGenerationTest` aus dem PR-Teil → ≈ 475 s. Windows: 1478 s → Testsumme ohne `ModeGenerationTest` (−230 s) und ohne Smoke-Tests (≈ −100 s) ≈ 1150 s → ≈ 575 s |
| Ubuntu-Job, kalt | 517 s / 747 s; Median 599 s | ≤ 480 s | s. o., geschätzt |
| Windows-Job, kalt | 765 s / 983 s; Median 899 s | ≤ 600 s | s. o., geschätzt, nur wenn Windows im PR `test` ausführt |

Die Zielwerte für kalte Läufe sind Schätzungen, die erst nach `module-test-levels` gemessen werden können. Die Messung erfolgt über `gh run view <id> --json jobs` (Job-Zeiten) und die `test-reports-*`-Artefakte (Testsummen).

**C6. Caches nur aus `main`, Lesen auf PRs.** Der geteilte Workflow setzt `cache-read-only` für alle Refs außer dem Default-Branch. PRs schreiben keinen Cache, sondern lesen den von `main`. Damit kann ein Branch mit kaputten Tests keinen Cache für andere Branches füllen.

## Risks / Trade-offs

- [Cache-Eviction: GitHub entfernt Caches nach 7 Tagen ohne Zugriff und bei mehr als 10 GB je Repository] → Ein Lauf nach einer Lücke ist kalt; das Ziel „Feature-PR ≤ 5 min“ gilt nur bei warmem Cache, der kalte Ziel-Lauf ist „Core-PR ≤ 10 min“. Der Nachtlauf schreibt den Cache nicht neu, also erfolgt die Erneuerung über `main`.
- [Nicht deterministische Tests werden aus dem Cache bestanden] → Die F.I.R.S.T.-Regel „keine versteckten Eingaben“ (Spec `test-levels`) und der Nachtlauf ohne Cache fangen das ab.
- [Zwei Check-Runs mit dem Namen „Test results“ im selben PR] → Der geteilte Workflow setzt den Namen fest. Die Ergebnisse sind trotzdem je Job sichtbar. Ein eigener Name braucht eine Änderung im Workflows-Repository (Open Question).
- [Ein Push auf `main` bricht den Nachtlauf ab] → Dieselbe Gruppe. Ein eigener Gruppenschlüssel ist nur über den geteilten Workflow möglich; bis dahin wird der Nachtlauf bei einem Merge-Push neu gestartet.
- [Windows und macOS im PR führen keine Smoke-Tests aus] → Der Smoke-Test prüft die Verdrahtung über das Jar, das auf allen drei Systemen gleich gebaut wird; der Entscheid liegt beim Nutzer (Open Question).
- [Die Verhältniszahl „Job-Dauer ≈ 0,5 × Testsumme“ stammt aus zwei Läufen] → Die Zielwerte gelten als Richtwert und werden nach der Umsetzung neu gemessen.

## Migration Plan

1. `module-test-levels` ist gemergt (Tags und Tasks vorhanden), PR #472 ist gemergt (Push-Trigger, Cache wird geschrieben).
2. Caller für Mac und Windows anlegen, `build-pr.yml` auf Ubuntu einschränken. Ein Test-PR prüft die drei Jobs und die Log-Ausgaben.
3. `build-main.yml` für Push auf `main` und Nachtlauf anlegen; ein manueller Lauf nach dem Merge prüft Cache und Sweeps.
4. Zielwerte nach zehn PR-Läufen aus den Job-Zeiten messen und in der Beschreibung des letzten Pull Requests der Reihe nennen.

Rollback: Die neue Caller-Datei und `build-main.yml` löschen; `build-pr.yml` zurück auf die alte Matrix (`runs-on` mit drei Betriebssystemen und `gradle-task` leer).

## Open Questions

- **Windows und macOS im PR (entscheidet der Nutzer vor der Umsetzung):** Der Entwurf lässt beide im PR den Task `test` ausführen. Alternative: beide nur auf `main` und nachts, PR nur Ubuntu. Die Alternative spart den Windows-Engpass (ca. 575 s geschätzt), verliert aber die frühe Prüfung auf Windows. Dieser Punkt ändert das Requirement „macOS und Windows führen im Pull Request den Task test aus“.
- **Remote-Build-Cache:** Ein gemeinsamer Cache-Server würde auch Läufe auf anderen Branches beschleunigen. Er ist eine Infrastrukturentscheidung mit Betriebsaufwand und ist nicht Teil dieser Änderung.
- **Eigener Check-Name im geteilten Workflow:** Nur über das Repository `OneLiteFeatherNET/workflows` lösbar; für diese Änderung nicht nötig.
