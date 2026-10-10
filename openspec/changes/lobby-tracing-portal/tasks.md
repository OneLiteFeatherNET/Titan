# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing-portal` von `origin/main`, **nachdem `lobby-tracing` (Fundament) auf `main` ist**. Unabhängig von den anderen Modul-Changes, sie berühren verschiedene Module. Ein Agent (sonnet, eigener Worktree) je Gruppe; Gruppen ohne Abhängigkeit laufen parallel. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | deliver | 1.1–1.2 | sonnet | `runtime/**` (`PlatformBeans`, `TracedDeliver`) | `features/**` |
| 1 | portal | 2.1–2.3 | sonnet | `features/portal/**` | `runtime/**`, `core/**` |
| 2 | smoke / verify | 3.1–3.2 | sonnet + Mensch / haiku | lokale Läufe / read-only | Code |
| 3 | pr | 4.1 | sonnet | Git/GitHub | Code |

Regeln für jeden Agent-Prompt:
- **Built-in first:** `Telemetry` aus `core` per Konstruktor, `FeatureNode.attach(…, telemetry)` und `onTraced`; kein `GlobalOpenTelemetry`, kein SDK.
- **Test zuerst** mit `TestTelemetry` (je Test frisch), **F.I.R.S.T.:** kein `Thread.sleep`, `Clock` injiziert, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick): Zähler. Metriken ohne `user.id`, nur Attribute mit kleiner fester Wertemenge. Spieler nur als UUID.
- Die Gruppen 1 und 2 sind unabhängig, `TracedDeliver` wird im Portal-Test mit einem Fake-`Deliver` ersetzt.
- **Commits:** `feat(telemetry): …`, ein Typ.

## 1. `Deliver` umspannen (runtime)

- [x] 1.1 Test zuerst (Unit): `TracedDeliver.sendPlayer` erzeugt `deliver.send_player` mit `titan.deliver.target_type`, `target`, `user.id` und `result=ok`; wirft der umhüllte `Deliver`, trägt der Span Ausnahme und ERROR, und die Ausnahme geht weiter. Rot, dann `TracedDeliver` (Konstruktor `Telemetry`, `Deliver`). Grün.
- [x] 1.2 Test zuerst (Wiring): `PlatformBeans.deliver()` liefert den umhüllten `Deliver`, die Variant-Tests bleiben grün. Rot, dann grün. Nachweis: `./gradlew :runtime:build`.

## 2. portal

- [x] 2.1 Test zuerst (Integration, Cyano-`Env`, `env.tick()`): Ein Spieler läuft durch ein Portal: `portal.transfer` mit `portal.id`, `portal.task`, `user.id`, `portal.result=delivered`; `portal.transfers{result=delivered}` steht auf eins; bei Fake-`Deliver`, der wirft: ERROR, `result=error`. Bewegung ohne Portal erzeugt keinen Span. Rot. Dann `PortalModule` (`Telemetry` per Konstruktor, `requires Telemetry.class`, `attach(…, telemetry)`, `deliver` in `inSpan`). Grün.
- [x] 2.2 Test zuerst (Unit, `PortalTriggerTest`): Verweigertes Recht erhöht `portal.denied{portal.id}` und erzeugt keinen Span; Abkühlzeit zählt nicht als verweigert. Rot, dann grün (der Zähler wird im Aufrufer erhöht, `PortalTrigger` bleibt rein).
- [x] 2.3 Test zuerst (Unit, `LabelRefresh` mit Fake-Scheduler und Fake-Lookup): Ein Zyklus erzeugt `portal.labels.refresh` mit Anzahl Labels und fehlgeschlagenen Abfragen; ein werfender Lookup erhöht `portal.player_count.lookups{result=error}`, die übrigen Labels werden aktualisiert. Rot, dann grün. Nachweis: `./gradlew :features:portal:build`; `docs/lobby-modules.md` (portal) nennt Spans und Zähler.

## 3. Abnahme

- [ ] 3.1 Smoke-Test mit Shaded-Jar, Agent und `logging`-Exporter: Portal betreten (Transfer ok, Rechte verweigert), Label-Aktualisierung; `portal.transfer` mit Kind `deliver.send_player`, Zähler `portal.transfers`. Ohne Agent keine Fehler. Nachweis: Checkliste im PR-Text. Nach dem Deploy (Mensch): Spans in Tempo, Zähler in Mimir.
- [x] 3.2 Verifikation (read-only): Jedes Szenario ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. erfüllt. Nachweis: Zuordnungstabelle im PR-Text.

## 4. Pull Request

- [ ] 4.1 Pull Request vom Zweig `feat/lobby-tracing-portal` auf `main` unter dem Titel `feat(telemetry): trace portal transfers and player count lookups` öffnen (Titel und Beschreibung Englisch), mit Checkliste und Zuordnung. Nachweis: PR-URL, CI grün.
