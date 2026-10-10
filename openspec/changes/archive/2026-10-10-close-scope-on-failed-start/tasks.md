# Tasks

## Execution Plan

Ein kleiner Change in einem Modul: ein Agent, ein Worktree, Branch `fix/close-scope-on-failed-start` von `origin/main`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | runtime-fix | 1.1–1.4 | sonnet | `runtime/**` | `core/**`, `features/**`, `persistence/**` |
| 2 | verify | 2.1 | haiku | read-only | alles |
| 3 | pr | 3.1 | sonnet | Git/GitHub | Code |

Regeln: Test zuerst (rot, dann grün); F.I.R.S.T.: kein Server, kein Warten, kein globaler Zustand, jede Testmethode baut ihren eigenen Scope; ein Verhalten je Test; schlanke Kommentare nur fürs Warum; Commit `fix(runtime): …`, ein Typ. Vor dem Abhaken `./gradlew build`.

## 1. Scope schließen (Welle 1)

- [x] 1.1 Test zuerst (Unit, runtime): Wirft der Körper nach einem erfolgreich gebauten Scope, ist der Scope danach geschlossen (ein `@PreDestroy` ist gelaufen), und die Ausnahme erreicht den Aufrufer unverändert (gleiche Instanz). Rot (die Hilfsmethode fehlt). Dann `closingOnFailure` (Name frei) in `runtime`. Grün.
- [x] 1.2 Test zuerst (Unit): Wirft zusätzlich `close()`, ist die gemeldete Ausnahme die ursprüngliche, die andere hängt per `getSuppressed()` daran. Ein Körper ohne Fehler schließt den Scope nicht und gibt den Rückgabewert zurück. Je ein eigener Test. Rot, dann grün.
- [x] 1.3 `Titan.start` und der Konstruktorblock nach dem Aufbau laufen über die Hilfsmethode. Test (Unit, runtime, `VariantStartupCheck` mit fehlender Column über `start`-Logik, soweit ohne Minestom erreichbar; sonst Nachweis über den Test aus 1.1 plus Code-Review der zwei Aufrufstellen): der Fehlertext nennt weiter die fehlende Column. Nachweis: `./gradlew :runtime:build :apps:cloudnet:build :apps:local:build`, bestehende `VariantStartupCheckTest` und `VariantStartTest` bleiben grün.
- [x] 1.4 Befund prüfen und im PR-Text festhalten (kein Code): Räumt Avaje einen Scope auf, wenn `build()` selbst wirft? Ein kurzer Test oder Lesen der Avaje-Quelle; bei „nein“ ein Hinweis für einen Folge-Change.

## 2. Verifikation (Welle 2)

- [x] 2.1 Read-only: Jedes Szenario des Spec-Deltas ist einem Test zugeordnet, die Tests erfüllen F.I.R.S.T. (keine Sleeps, kein geteilter statischer Zustand). Nachweis: Zuordnungstabelle im PR-Text.

## 3. Pull Request

- [x] 3.1 Pull Request vom Branch `fix/close-scope-on-failed-start` auf `main` unter dem Titel `fix(runtime): close the bean scope when the startup check fails` öffnen (Titel und Beschreibung Englisch), mit Szenario-Zuordnung. Nachweis: PR-URL, CI grün.
