# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing-jumprun` von `origin/main`, **nachdem `lobby-tracing` (Fundament) auf `main` ist**. Unabhängig von den anderen Modul-Changes, sie berühren verschiedene Module. Ein Agent (sonnet, eigener Worktree) je Gruppe; Gruppen ohne Abhängigkeit laufen parallel. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | jumprun | 1.1–1.6 | sonnet | `features/jumprun/**`, `docs/lobby-modules.md` (Abschnitt jumprun) | `core/**`, `runtime/**`, `persistence/**` |
| 2 | smoke / verify | 2.1–2.2 | sonnet + Mensch / haiku | lokale Läufe / read-only | Code |
| 3 | pr | 3.1 | sonnet | Git/GitHub | Code |

Regeln für jeden Agent-Prompt:
- **Built-in first:** `Telemetry` aus `core` per Konstruktor, `FeatureNode.attach(…, telemetry)` und `onTraced`; kein `GlobalOpenTelemetry`, kein SDK.
- **Test zuerst** mit `TestTelemetry` (je Test frisch), **F.I.R.S.T.:** kein `Thread.sleep`, `Clock` injiziert, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick): Zähler. Metriken ohne `user.id`, nur Attribute mit kleiner fester Wertemenge. Spieler nur als UUID.
- Berührungspunkte in `JumprunModule`: nur `start`, `begin`, `end`, `refreshLeaderboard`; wer an `jumprun-more-surfaces` oder `jumprun-climbing` arbeitet, rebased vor dem Merge auf `main`.
- **Commits:** `feat(telemetry): …`, ein Typ.

## 1. Spans und Metriken in jumprun

- [x] 1.1 Test zuerst (Unit, `RunTelemetryTest`, `TestTelemetry`): `jumprun.end` für FALL trägt Grund, Modus, Score, `user.id`, `fall.y`, `fall.threshold`, `fall.course_index`, Status unset; für ABORT ohne `fall.*`. Rot. Dann `RunTelemetry` (Namen und Schlüssel als Konstanten). Grün.
- [x] 1.2 Test zuerst (Integration, Cyano-`Env`, `env.tick()`): Ein Absturz im echten `JumprunModule` erzeugt genau einen `jumprun.end` mit FALL; ein zweites Ende (Disconnect nach Shutdown) keinen zweiten. `JumprunModule` bekommt `Telemetry` per Konstruktor (`requires Telemetry.class`, `FeatureNode.attach(…, telemetry)`), `end` ruft `RunTelemetry`. Rot, dann grün.
- [x] 1.3 Test zuerst (Unit): Wirft `records.submit`, trägt der Span Ausnahme und ERROR, ist beendet, und die Ausnahme erreicht den Aufrufer. Ein Kind-Span aus einem verzögert laufenden Executor-Task (`QueuedExecutor`, dann `runAll()`) hat `jumprun.end` als Eltern (Kontext über `DatabaseWriter` bzw. im Test über `Context.taskWrapping`). Rot, dann grün.
- [x] 1.4 Test zuerst (Integration): `begin` ohne Platz erzeugt `jumprun.start` mit `no_room` und keinen `jumprun.end`; ein erfolgreicher Start `started`. Rot, dann grün.
- [x] 1.5 Test zuerst: `refreshLeaderboard` erzeugt `jumprun.leaderboard.refresh` im Executor-Task, mit Kind-Span-Test wie in 1.3. Rot, dann grün.
- [x] 1.6 Test zuerst (Unit): `titan.jumprun.runs.ended{reason,mode}`, `titan.jumprun.runs.started{mode,outcome}` und `titan.jumprun.run.score{mode}` stehen nach Start und Ende richtig, kein Metrik-Attribut `user.id`; kein Attributwert und Spanname enthält den Spielernamen. Rot, dann grün. Dazu `docs/lobby-modules.md` (jumprun): Spans, Attribute, Zähler. Nachweis: `./gradlew :features:jumprun:build`.

## 2. Abnahme

- [ ] 2.1 Smoke-Test mit Shaded-Jar, Agent und `logging`-Exporter: Lauf starten, fallen, abbrechen, ohne Platz starten. `jumprun.start`/`jumprun.end` mit den Attributen erscheinen, der Hibernate-Span `Session.persist … JumprunRunEntity` hängt unter `jumprun.end`. Ohne Agent keine Fehler. Nachweis: Checkliste im PR-Text. Nach dem Deploy (Mensch): Tempo `{ name = "jumprun.end" }`.
- [ ] 2.2 Verifikation (read-only): Jedes Szenario ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. erfüllt. Nachweis: Zuordnungstabelle im PR-Text.

## 3. Pull Request

- [x] 3.1 Pull Request vom Zweig `feat/lobby-tracing-jumprun` auf `main` unter dem Titel `feat(telemetry): trace jump and run starts, ends and leaderboard refreshes` öffnen (Titel und Beschreibung Englisch), mit Checkliste und Zuordnung. Nachweis: PR-URL, CI grün.
