# Tasks

Zweig: `ci/test-selection` von `origin/main`, erst nach dem Merge von #472 und `module-test-levels`. Jeder Commit hat genau einen Typ, hier `ci(build)`. Die Workflow-Dateien werden in der Umgebung geprüft, bevor sie abgehakt werden: Syntax mit `actionlint`, falls verfügbar, sonst durch einen Lauf auf dem Branch. Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`.

## Ausführungsplan

| Welle | Agent | Aufgaben | Modell | darf ändern | darf nicht ändern |
|---|---|---|---|---|---|
| 1 | Lesen | 1.1, 1.2 | haiku | nur Scratch-Verzeichnis | Repository |
| 2 | Umsetzung, Worktree | 2.1, 2.2 | sonnet | `.github/workflows/build-pr.yml`, neue Caller-Datei | Produktionscode, Tests |
| 2 | Umsetzung, Worktree | 2.3 | sonnet | `.github/workflows/build-main.yml` (neu) | `build-pr.yml` |
| 3 | Prüfung | 3.1, 3.2 | haiku | nur Scratch-Verzeichnis | Repository |
| 4 | Pull Request | 4.1 | sonnet | nur Git und `gh` | Repository-Inhalt |

Welle 2 läuft parallel; die beiden Aufgaben berühren verschiedene Dateien. Vor Welle 2 steht die Entscheidung des Nutzers zu Windows und macOS im PR (Open Question in `design.md`); ohne Entscheidung gilt der Entwurf.

## 1. Vorbedingungen

- [ ] 1.1 Prüfen, dass #472 auf `main` gemergt ist und dass ein Lauf auf `main` den Cache geschrieben hat (`gh run list --branch main --workflow "Build PR"`). Nachweis: Lauf-ID im Pull Request; Log eines PR-Laufs danach zeigt die Wiederherstellung des Caches.
- [ ] 1.2 Prüfen, dass `module-test-levels` gemergt ist: `./gradlew smokeTest sweepTest --dry-run` zeigt beide Tasks. Nachweis: Ausgabe im Pull Request.

## 2. Workflows

- [ ] 2.1 `build-pr.yml` auf Ubuntu begrenzen (`runs-on: ["ubuntu-latest"]`, `gradle-task: build`). Test zuerst: der Entwurf des Caller-Auftrags wird mit `actionlint` geprüft, bevor er gepusht wird. Nachweis: Ubuntu-Job läuft, `smokeTest` und Docker-Integrationstests stehen im Log.
- [ ] 2.2 Neue Caller-Datei `build-pr-cross-os.yml` mit dem Namen „Build PR (macOS, Windows)“ und `runs-on: ["windows-latest", "macos-latest"]`, `gradle-task: test`. Prüfen, ob die beiden Aufrufe sich über die `concurrency`-Gruppe abbrechen: auf dem Branch einen PR öffnen und in den Läufen nachsehen, ob beide Gruppen durchlaufen. Nachweis: beide Aufrufe laufen vollständig; Ergebnis im Pull Request.
- [ ] 2.3 `build-main.yml` mit den Triggern `push` (Branch `main`) und `schedule` (nachts). Ein Job für Push ruft den geteilten Workflow mit `force-build: true` und `gradle-task: build sweepTest`; ein Job für den Nachtlauf mit `gradle-task: build sweepTest --no-build-cache`. Beide Jobs mit `if` auf den Ereignistyp. Nachweis: `actionlint`; ein manueller Lauf per `workflow_dispatch` (Trigger ergänzt, falls nötig) zeigt die Sweep-Tests in den Berichten.

## 3. Messung

- [ ] 3.1 Feature-Test-PR: ein Pull Request, der nur eine Datei in `features/sit` ändert, auf dem Branch öffnen und die Task-Ausgaben prüfen. Erwartung: `FROM-CACHE` oder `UP-TO-DATE` für alle Testtasks außer `features:sit:test` und den Smoke-Tests. Nachweis: Zählung je Job im Pull Request.
- [ ] 3.2 Build-Logik-Test: ein Test-PR, der eine Patch-Version einer Testabhängigkeit im Versionskatalog `gradle/libs.versions.toml` anhebt. Erwartung: keine `FROM-CACHE`-Ausgabe für Testtasks, alle laufen neu. Nachweis: Zählung je Job; der PR wird danach geschlossen, ohne Merge.
- [ ] 3.3 Zielwerte aus den Job-Zeiten der letzten zehn PR-Läufe (`gh run view <id> --json jobs`) und den Testsummen der `test-reports-*`-Artefakte berechnen. Nachweis: Tabelle im Pull Request; Abweichungen von `design.md` (C5) nennen.

## 4. Pull Request

- [ ] 4.1 Pull Request öffnen mit dem Titel `ci(build): run smoke tests once and sweeps on main`. Beschreibung auf Englisch mit den Messungen aus 3.1 bis 3.3, den offenen Punkten (Windows und macOS im PR, Remote-Build-Cache) und der Session-URL als letzter Zeile. Nachweis: PR-URL; `gh pr checks <n> --watch` einmal.
