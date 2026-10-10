# Tasks

## Execution Plan

Zuerst ein Spike (Mensch plus Agent, lokal), dann je nach Ergebnis Option A oder B (siehe Design D2). Branch `docs/telemetry-shutdown-flush` von `origin/main`; bei Option B wird der Branch in `fix/telemetry-shutdown-flush` umbenannt.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | spike | 1.1–1.2 | sonnet + Mensch | nur lokale Läufe (Jar kopieren, Agent-Jar), Notizen im PR-Text | Code |
| 2 | test-docs | 2.1–2.2 | sonnet | `runtime/src/test/**`, `docs/lobby-modules.md` | Produktionscode |
| 2b | flush (nur Option B) | 3.1–3.3 | sonnet | `runtime/src/main/**` (eine Klasse, `Titan`), ArchUnit-Regel | `core/**`, `features/**` |
| 3 | verify | 4.1 | haiku | read-only | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Code |

Regeln: Kein SDK in der Lobby, kein `@WithSpan`; Test zuerst; F.I.R.S.T.: frisches `TestTelemetry` je Test, kein `Thread.sleep`, kein Agent im Unit-Test, ein Verhalten je Test; schlanke Kommentare; Commit `docs(telemetry): …` (A) bzw. `fix(telemetry): …` (B), ein Typ. Vor dem Abhaken `./gradlew build`. Der Spike ist keine F.I.R.S.T.-Prüfung, sondern eine einmalige manuelle Abnahme.

## 1. Spike: messen, ob Spans fehlen (Welle 1)

- [ ] 1.1 Lokal `apps:local` mit `-javaagent:opentelemetry-javaagent.jar` (2.16.0) und Exporter `logging` (oder OTLP an lokalen Collector) starten, einen Jump-and-Run-Lauf offen lassen und beenden über: (a) Konsole `stop`, (b) Saisonwechsel-Pfad (`MinecraftServerStop`), (c) `kill -TERM`. Je einmal mit `otel.bsp.schedule.delay` Standard und mit 60000. Nachweis: Tabelle im PR-Text, je Zeile: erscheinen `titan.shutdown` und `jumprun.end{reason=SHUTDOWN}` im Export, ja oder nein.
- [ ] 1.2 Entscheidung festhalten: kein Verlust auf (a) und (b) → Option A (weiter mit 2.x). Reproduzierbarer Verlust → Option B (weiter mit 3.x), Commit-Typ und PR-Titel auf `fix(telemetry): flush the agent's exporters at the end of shutdown` umstellen, `proposal.md` Delivery anpassen. Ob CloudNet `stop` oder SIGTERM schickt, gehört in die Doku.

## 2. Garantie festnageln und dokumentieren (Option A, Welle 2)

- [ ] 2.1 Test zuerst (Unit, `TitanLifecycleTest`): Direkt nach der Rückkehr von `lifecycle.shutdown(...)` ist `titan.shutdown` über `TestTelemetry.span(...)` lesbar (beendet); ebenso, wenn der Body wirft (Ausnahme erreicht den Aufrufer, Status ERROR); ein im Body beendeter Span ist beendet, bevor `shutdown` zurückkehrt. Drei Tests, ein Verhalten je Test. Die Tests sind vermutlich sofort grün (Garantie besteht schon); dann gilt der Nachweis, dass sie rot würden, wenn `inSpan` den Span nicht im `finally` beendete (Gegenprobe lokal, nicht committen).
- [ ] 2.2 `docs/lobby-modules.md`, Abschnitt „Traces und Metriken“, Unterabschnitt „Herunterfahren“ nach Design D4: Garantie, Grenzen, Stellschrauben `otel.bsp.schedule.delay`/`otel.metric.export.interval`, Tempo-Abfrage für den Smoke, Ergebnis des Spikes (welcher Stoppweg, was ankam). Nachweis: Doku nennt alle Punkte; `./gradlew build`.

## 3. Reflexions-Flush (nur bei Option B, Welle 2b)

- [ ] 3.1 Test zuerst (Unit, runtime): `AgentFlush` ruft einen eingespeisten Flusher einmal mit dem Timeout; fehlt die Agent-Klasse, passiert nichts (kein Log, keine Ausnahme); wirft der Flusher, gibt es genau ein WARN (per Appender geprüft) und keine Ausnahme. Rot, dann `AgentFlush` (paketprivat, `Class.forName` auf `io.opentelemetry.javaagent.bootstrap.OpenTelemetrySdkAccess`). Grün.
- [ ] 3.2 Test zuerst: Der Shutdown-Task ruft den Flush **nach** dem Ende von `titan.shutdown` (Reihenfolge über einen aufzeichnenden Fake). Rot, dann `Titan.initialize` anpassen. Grün.
- [ ] 3.3 ArchUnit-Regel: `io.opentelemetry.javaagent` wird nur von `AgentFlush` verwendet (Rot mit verletzender Fixture, dann Regel). Smoke mit dem Shaded-Jar und Agent: der Verlust aus 1.1 ist weg. Nachweis: Konsolenauszug im PR-Text.

## 4. Verifikation (Welle 3)

- [ ] 4.1 Read-only: Jedes Szenario des Spec-Deltas ist einem Test oder dem Spike zugeordnet, F.I.R.S.T. erfüllt (kein Agent, keine Sleeps im Unit-Test), kein SDK-Import in `features/**` oder `core`. Nachweis: Zuordnungstabelle im PR-Text.

## 5. Pull Request

- [ ] 5.1 Pull Request auf `main` öffnen, Titel bei Option A `docs(telemetry): document what spans survive shutdown and how to check`, bei Option B `fix(telemetry): flush the agent's exporters at the end of shutdown` (Titel und Beschreibung Englisch), mit der Messtabelle aus 1.1 und der Szenario-Zuordnung. Nachweis: PR-URL, CI grün.
