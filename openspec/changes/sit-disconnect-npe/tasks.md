# Tasks

## Execution Plan

Ein Modul, ein Agent, ein Worktree, Branch `fix/sit-disconnect-npe` von `origin/main`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | sit-fix | 1.1–1.4 | sonnet | `features/sit/**` | alles andere |
| 2 | verify | 2.1 | haiku | read-only | alles |
| 3 | pr | 3.1 | sonnet | Git/GitHub | Code |

Regeln: Test zuerst (der Fehler wird zuerst als roter Test reproduziert); F.I.R.S.T.: je Test frische `Env` (`MicrotusExtension`) und frische `Seats`, `env.tick()` statt Warten, kein `Thread.sleep`, Erfolg nur über Assertions, ein Verhalten je Test, Namen nach dem Verhalten; schlanke Kommentare; Commit `fix(sit): …`, ein Typ. Vor dem Abhaken `./gradlew build`.

## 1. Disconnect ohne Instanz (Welle 1)

- [ ] 1.1 Test zuerst (Unit, `SeatsTest`): `standUp` für einen Spieler **ohne** Instanz und ohne Sitz wirft nicht. Rot (NPE aus `Objects.requireNonNull`, wie im Log). Dann `standUp` nach D1 (Tag zuerst prüfen, Instanz einmal lokal lesen). Grün.
- [ ] 1.2 Test zuerst (Unit): `standUp` für einen sitzenden Spieler, der vorher aus der Instanz entfernt wurde, wirft nicht und entfernt `ARROW`/`ORIGIN` (`isSitting` ist danach `false`). Rot, dann grün.
- [ ] 1.3 Test (Unit): Der Sitz wird erst durch `SeatEntity.update` (`env.tick()`) und danach durch `standUp` entfernt, und umgekehrt; beide Reihenfolgen werfen nicht, der Sitz ist danach weg und die Instanz enthält ihn nicht mehr (belegt D2, Idempotenz).
- [ ] 1.4 Test (Integration, `SitModuleIntegrationTest`, Cyano-`Env`): Der echte `PlayerDisconnectEvent`-Listener wirft für einen instanzlosen Spieler nicht (Server-Ausnahmen über `env.process().exception().setExceptionHandler` einsammeln und `assertTrue(exceptions.isEmpty())`); ein sitzender Spieler ohne Instanz lässt nach `env.tick()` keinen Sitz zurück. Nachweis: `./gradlew :features:sit:build`, die bestehenden Sit-Tests bleiben grün.

## 2. Verifikation (Welle 2)

- [ ] 2.1 Read-only: Jedes Szenario des Spec-Deltas ist einem Test zugeordnet, F.I.R.S.T. erfüllt. Smoke (Mensch, optional): lokal mit `apps:local` beitreten, sitzen, Verbindung hart trennen, im Log kein „Unhandled exception in module sit“. Nachweis: Zuordnungstabelle im PR-Text.

## 3. Pull Request

- [ ] 3.1 Pull Request vom Branch `fix/sit-disconnect-npe` auf `main` unter dem Titel `fix(sit): do not throw when a player without an instance disconnects` öffnen (Titel und Beschreibung Englisch), mit dem Stacktrace aus Loki als Anlass und der Szenario-Zuordnung. Nachweis: PR-URL, CI grün.
