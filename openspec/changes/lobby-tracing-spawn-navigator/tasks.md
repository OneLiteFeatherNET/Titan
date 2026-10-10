# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing-spawn-navigator` von `origin/main`, **nachdem `lobby-tracing` (Fundament) auf `main` ist**. Unabhängig von den anderen Modul-Changes, sie berühren verschiedene Module. Ein Agent (sonnet, eigener Worktree) je Gruppe; Gruppen ohne Abhängigkeit laufen parallel. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | spawn | 1.1–1.3 | sonnet | `features/spawn/**` | `features/navigator/**`, `core/**` |
| 1 | navigator | 2.1–2.2 | sonnet | `features/navigator/**` | `features/spawn/**`, `core/**` |
| 2 | smoke / verify | 3.1–3.2 | sonnet + Mensch / haiku | lokale Läufe / read-only | Code |
| 3 | pr | 4.1 | sonnet | Git/GitHub | Code |

Regeln für jeden Agent-Prompt:
- **Built-in first:** `Telemetry` aus `core` per Konstruktor, `FeatureNode.attach(…, telemetry)` und `onTraced`; kein `GlobalOpenTelemetry`, kein SDK.
- **Test zuerst** mit `TestTelemetry` (je Test frisch), **F.I.R.S.T.:** kein `Thread.sleep`, `Clock` injiziert, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick): Zähler. Metriken ohne `user.id`, nur Attribute mit kleiner fester Wertemenge. Spieler nur als UUID.
- Die Gruppen sind unabhängig; der Navigator testet mit einem Fake-`SpawnReturn`.
- **Commits:** `feat(telemetry): …`, ein Typ.

## 1. spawn

- [x] 1.1 Test zuerst (Unit, `LobbySpawnReturn`): `sendToSpawn` erzeugt `spawn.return` mit Quelle, Ergebnis, `user.id`; `titan.spawn.returns{source,result}` stimmt für `sent` und `blocked`. Rot, dann `SpawnModule` und `LobbySpawnReturn` mit `Telemetry` (`requires Telemetry.class`, `attach(…, telemetry)`). Grün.
- [x] 1.2 Test zuerst (Integration, `Env`, `env.tick()`): Ein Spieler unter `minHeight` erzeugt genau einen `spawn.bounds_teleport`; 100 Bewegungen innerhalb der Grenzen erzeugen keinen Span. Rot, dann nur der Teleport-Zweig in `inSpan`. Grün.
- [x] 1.3 Test zuerst (Integration): Beitritt erzeugt `spawn.join` über `onTraced`; ein werfender Join-Teleport trägt Ausnahme und ERROR. Rot, dann grün. Nachweis: `./gradlew :features:spawn:build`.

## 2. navigator

- [x] 2.1 Test zuerst (Integration, Aves-Klick in `Env`): `open` erzeugt `navigator.open` (Art, Einträge); die Auswahl eines erlaubten Ziels `navigator.select` mit `result=sent`, eines verbotenen `denied`, des Spawn-Eintrags `spawn`; `titan.navigator.selections` stimmt. Rot, dann `NavigatorModule` (`Telemetry`, `attach(…, telemetry)`). Grün.
- [x] 2.2 Test zuerst (Unit): `navigator.layout.apply` nur bei geändertem Layout. Rot, dann grün. Nachweis: `./gradlew :features:navigator:build`; `docs/lobby-modules.md` (spawn, navigator) nennt Spans und Zähler.

## 3. Abnahme

- [ ] 3.1 Smoke-Test mit Shaded-Jar, Agent und `logging`-Exporter: `/spawn`, Navigator öffnen und Ziel wählen, unter die Höhengrenze fallen. Ohne Agent keine Fehler. Nachweis: Checkliste im PR-Text. Nach dem Deploy (Mensch): Spans in Tempo, Zähler in Mimir.
- [x] 3.2 Verifikation (read-only): Jedes Szenario ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. erfüllt. Nachweis: Zuordnungstabelle im PR-Text.

## 4. Pull Request

- [x] 4.1 Pull Request vom Zweig `feat/lobby-tracing-spawn-navigator` auf `main` unter dem Titel `feat(telemetry): trace spawn returns and navigator use` öffnen (Titel und Beschreibung Englisch), mit Checkliste und Zuordnung. Nachweis: PR-URL, CI grün.
