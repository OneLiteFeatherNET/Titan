# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing-world` von `origin/main`, **nachdem `lobby-tracing` (Fundament) auf `main` ist**. Unabhängig von den anderen Modul-Changes, sie berühren verschiedene Module. Ein Agent (sonnet, eigener Worktree) je Gruppe; Gruppen ohne Abhängigkeit laufen parallel. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | season | 1.1–1.3 | sonnet | `features/season/**` | übrige Module |
| 1 | daytime | 2.1 | sonnet | `features/daytime/**` | übrige Module |
| 1 | protection | 3.1–3.2 | sonnet | `features/protection/**` | übrige Module |
| 2 | smoke / verify | 4.1–4.2 | sonnet + Mensch / haiku | lokale Läufe / read-only | Code |
| 3 | pr | 5.1 | sonnet | Git/GitHub | Code |

Regeln für jeden Agent-Prompt:
- **Built-in first:** `Telemetry` aus `core` per Konstruktor, `FeatureNode.attach(…, telemetry)` und `onTraced`; kein `GlobalOpenTelemetry`, kein SDK.
- **Test zuerst** mit `TestTelemetry` (je Test frisch), **F.I.R.S.T.:** kein `Thread.sleep`, `Clock` injiziert, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick): Zähler. Metriken ohne `user.id`, nur Attribute mit kleiner fester Wertemenge. Spieler nur als UUID.
- Drei unabhängige Gruppen, ein Agent je Gruppe parallel.
- **Commits:** `feat(telemetry): …`, ein Typ.

## 1. season

- [x] 1.1 Test zuerst (Unit, `SeasonModule` mit Fake-`Scheduler`, `SeasonSchedule`, fester `Clock`): `check` erzeugt `season.check` mit `current`, `desired`, `outcome`, `online_players` für die Fälle unverändert, wartend, Neustart, nicht auflösbar. Rot, dann `SeasonModule` (`Telemetry`, `attach(…, telemetry)`, `check` in `inSpan`). Grün.
- [x] 1.2 Test zuerst: Neustart-Anforderung setzt das Event `season.stop_requested` und erhöht `titan.season.restarts_requested`; `titan.season.checks{outcome}` stimmt. Rot, dann grün.
- [x] 1.3 Test zuerst: Der Disconnect-Listener erzeugt selbst keinen Span, nur den Check im nächsten Tick (`env.tick()`). Rot, dann grün. Nachweis: `./gradlew :features:season:build`.

## 2. daytime

- [x] 2.1 Test zuerst (Unit, feste `Clock`): `titan.daytime.updates` zählt jede Aktualisierung; eine abgelehnte Zonen-ID erhöht `titan.daytime.config_rejected{reason}` einmal je Ablehnung, kein Span. Rot, dann `DaytimeModule` (`Telemetry`). Grün. Nachweis: `./gradlew :features:daytime:build`.

## 3. protection

- [x] 3.1 Test zuerst (Integration, `Env`): Jedes geschützte Event (Aufheben, Inventar-Klick, weitere der Handler-Liste) ist abgebrochen und erhöht `titan.protection.denied{event}` mit dem passenden Namen; kein Span. Rot, dann `ProtectionModule` (`Telemetry`; zählender Abbruch-Handler an einer Stelle). Grün.
- [x] 3.2 Test zuerst: 1000 abgebrochene Events erzeugen null Spans und keinen Attributwert mit Spieler-Daten. Rot, dann grün. Nachweis: `./gradlew :features:protection:build`; `docs/lobby-modules.md` nennt Spans und Zähler von season, daytime, protection.

## 4. Abnahme

- [ ] 4.1 Smoke-Test mit Shaded-Jar, Agent und `logging`-Exporter: Saison-Prüfung mit gesetztem Datum (Neustart angefordert), Item aufheben und Inventar klicken (Zähler steigt). Ohne Agent keine Fehler. Nachweis: Checkliste im PR-Text. Nach dem Deploy (Mensch): Spans in Tempo, Zähler in Mimir.
- [x] 4.2 Verifikation (read-only): Jedes Szenario ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. erfüllt. Nachweis: Zuordnungstabelle im PR-Text.

## 5. Pull Request

- [x] 5.1 Pull Request vom Zweig `feat/lobby-tracing-world` auf `main` unter dem Titel `feat(telemetry): trace season changes, daytime and protection denials` öffnen (Titel und Beschreibung Englisch), mit Checkliste und Zuordnung. Nachweis: PR-URL, CI grün.
