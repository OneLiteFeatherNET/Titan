# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing-movement` von `origin/main`, **nachdem `lobby-tracing` (Fundament) auf `main` ist**. Unabhängig von den anderen Modul-Changes, sie berühren verschiedene Module. Ein Agent (sonnet, eigener Worktree) je Gruppe; Gruppen ohne Abhängigkeit laufen parallel. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | sit | 1.1–1.2 | sonnet | `features/sit/**` | übrige Module |
| 1 | elytra | 2.1–2.3 | sonnet | `features/elytra/**` | übrige Module |
| 1 | tickle-respawn | 3.1–3.2 | sonnet | `features/tickle/**`, `features/respawn/**` | übrige Module |
| 2 | smoke / verify | 4.1–4.2 | sonnet + Mensch / haiku | lokale Läufe / read-only | Code |
| 3 | pr | 5.1 | sonnet | Git/GitHub | Code |

Regeln für jeden Agent-Prompt:
- **Built-in first:** `Telemetry` aus `core` per Konstruktor, `FeatureNode.attach(…, telemetry)` und `onTraced`; kein `GlobalOpenTelemetry`, kein SDK.
- **Test zuerst** mit `TestTelemetry` (je Test frisch), **F.I.R.S.T.:** kein `Thread.sleep`, `Clock` injiziert, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick): Zähler. Metriken ohne `user.id`, nur Attribute mit kleiner fester Wertemenge. Spieler nur als UUID.
- Drei unabhängige Gruppen, ein Agent je Gruppe parallel.
- **Commits:** `feat(telemetry): …`, ein Typ.

## 1. sit

- [x] 1.1 Test zuerst (Integration, `Env`): Hinsetzen erzeugt `sit.start` mit `sit.block`, `user.id`; Aufstehen durch Schleichen, Dismount und Disconnect erzeugt `sit.stop` mit dem jeweiligen Grund; `titan.sit.sessions` stimmt. Rot, dann `SitModule` (`Telemetry`, `attach(…, telemetry)`, `onTraced` für Block-Interaktion und Dismount). Grün.
- [x] 1.2 Test zuerst: Ein Disconnect ohne Sitz erzeugt keinen `sit.stop`; `PlayerPacketEvent` (Schleichen-Paket) erhält **keinen** Span (nur der Dismount-Zweig). Rot, dann grün. Nachweis: `./gradlew :features:sit:build`.

## 2. elytra

- [x] 2.1 Test zuerst (Integration): Start erzeugt `elytra.glide.start`, Landung `elytra.glide.end` mit `elytra.glide.duration_ms` aus einer festen `Clock`; Disconnect im Flug vergisst den Start ohne Span-Leck. Rot, dann `ElytraModule` mit `Telemetry` und `Clock`. Grün.
- [x] 2.2 Test zuerst (Unit): `titan.elytra.boosts` zählt Zündungen; `FireworkBoostTracker.advance` über 100 Ticks erzeugt keinen Span. Rot, dann grün.
- [x] 2.3 Test zuerst: `titan.elytra.flights{event}` stimmt für Start und Landung. Rot, dann grün. Nachweis: `./gradlew :features:elytra:build`.

## 3. tickle und respawn

- [x] 3.1 Test zuerst (Unit, `TickleAttackHandler`, feste `Clock`): `titan.tickle.attacks{result=tickled}` und `{result=cooldown}`, kein Span. Rot, dann `TickleModule`/`TickleAttackHandler` mit `Telemetry`. Grün.
- [x] 3.2 Test zuerst (Integration, `env.tick()`): Tod und verzögerter Respawn erzeugen `respawn.perform` mit `user.id`, `titan.player.respawns` steigt; ein Disconnect vor dem Tick erzeugt keinen Span. Rot, dann `RespawnModule` (`Telemetry`). Grün. Nachweis: `./gradlew :features:tickle:build :features:respawn:build`; `docs/lobby-modules.md` nennt die Spans und Zähler dieser vier Module.

## 4. Abnahme

- [ ] 4.1 Smoke-Test mit Shaded-Jar, Agent und `logging`-Exporter: setzen und aufstehen, mit Elytra fliegen und landen, kitzeln, sterben. Ohne Agent keine Fehler. Nachweis: Checkliste im PR-Text. Nach dem Deploy (Mensch): Spans in Tempo, Zähler in Mimir.
- [x] 4.2 Verifikation (read-only): Jedes Szenario ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. erfüllt. Nachweis: Zuordnungstabelle im PR-Text.

## 5. Pull Request

- [x] 5.1 Pull Request vom Zweig `feat/lobby-tracing-movement` auf `main` unter dem Titel `feat(telemetry): trace sit, elytra, tickle and respawn` öffnen (Titel und Beschreibung Englisch), mit Checkliste und Zuordnung. Nachweis: PR-URL, CI grün.
