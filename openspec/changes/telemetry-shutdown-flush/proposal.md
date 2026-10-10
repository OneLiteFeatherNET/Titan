# Proposal

## Why

Der Span `titan.shutdown` (und Spans, die während des Herunterfahrens enden, etwa `jumprun.end` mit Grund SHUTDOWN) sollen in Tempo ankommen. Befürchtung: Der OTel-Java-Agent (live 2.16.0, SDK 1.50.0) flusht und beendet sein SDK in einem eigenen JVM-Shutdown-Hook. JVM-Hooks laufen nebenläufig und ohne feste Reihenfolge; läuft Titans Abschalten in diesem Fenster, kann der Batch-Prozessor den letzten Span nicht mehr exportieren.

Die Untersuchung (siehe `design.md`) ergibt vorab:

- Mit `opentelemetry-api` allein gibt es **keinen** Flush: `TracerProvider` (API 1.51.0, per `javap` geprüft) hat nur `get`/`tracerBuilder`; `forceFlush` gibt es erst am SDK-Typ `SdkTracerProvider`, und der Agent hält sein SDK in einem eigenen Classloader hinter einer Brücke.
- Titans Abschalten läuft **nicht** in einem JVM-Hook: `Titan.initialize()` hängt `lifecycle.shutdown(beanScope::close)` an einen **Minestom-Shutdown-Task**, den `MinecraftServer.stopCleanly()` ausführt (Konsolenbefehl `stop`, `StopCommand`, Saisonwechsel). Das geschieht, **bevor** die JVM herunterfährt; der Span endet also vor dem Start des Agent-Hooks, und der Hook flusht ihn mit. Ein Verlust wäre nur dort möglich, wo `stopCleanly()` selbst aus einem JVM-Hook gerufen wird (SIGTERM-Pfad); dafür gibt es im Repository keinen Hook (nur LuckPerms registriert einen eigenen).

Ob live wirklich Spans fehlen, ist **nicht gemessen**. Der Change beginnt deshalb mit einer Messung und liefert, was die Messung hergibt.

## What Changes

Empfehlung (KISS, kein SDK in der Lobby): **Option A, nicht beheben, sondern dokumentieren und prüfbar machen.**

- Messung (Spike, Task 1): Lokal mit Agent und OTLP-/Logging-Exporter beide Stoppwege fahren (`stop` über die Konsole; SIGTERM) und prüfen, ob `titan.shutdown` und `jumprun.end{reason=SHUTDOWN}` exportiert werden.
- Dokumentation in `docs/lobby-modules.md`: was beim Herunterfahren garantiert ist (Span endet vor Rückkehr des Shutdown-Tasks), was nicht (Spans, die nach dem Task oder in fremden Hooks enden; harter Kill), und die Betriebshinweise `otel.bsp.schedule.delay` (Standard 5000 ms) und `otel.metric.export.interval` als Stellschrauben im CloudNet-Template, nicht im Code.
- Ein Test, der die Garantie festnagelt: `titan.shutdown` ist beendet und exportierbar, wenn `TitanLifecycle.shutdown` zurückkehrt, auch bei einer Ausnahme.
- Eine Smoke-Checkliste für Deployments (Tempo-Abfrage nach einem geplanten Neustart).

Liefert die Messung einen **reproduzierbaren Verlust** auf einem realen Stoppweg, gilt die vorab entschiedene Rückfalloption B (siehe Design D2): am Ende des Shutdown-Tasks ruft ein winziger Helfer in `runtime` per Reflexion `io.opentelemetry.javaagent.bootstrap.OpenTelemetrySdkAccess.forceFlush(long, TimeUnit)` auf (Klasse im Bootstrap-Classloader des Agents, ohne Agent: No-op). Dann wird aus diesem Change ein `fix(telemetry)`; der PR-Titel unten ist dann zu ändern (siehe Open Questions).

Nicht Teil dieser Änderung: ein SDK oder eine Agent-Extension in der Lobby, Änderungen an Exportern oder Endpunkten, harte Kills (SIGKILL) absichern.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: Anforderung zum Zeitpunkt, an dem die Spans des Herunterfahrens enden, und zur dokumentierten Grenze.

## Impact

- **Code:** voraussichtlich keiner im Produktionscode (Option A); `runtime/src/test` (ein Test), `docs/lobby-modules.md`. Bei Option B zusätzlich eine kleine Klasse in `runtime`.
- **Abhängigkeiten:** keine.
- **Betrieb:** Hinweis auf zwei Agent-Optionen im CloudNet-Template (Betreiber, außerhalb dieses Repos).
- **Spielerverhalten, Nutzertexte:** keine.

## Delivery

Pull-Request-Titel und Squash-Commit (Option A): `docs(telemetry): document what spans survive shutdown and how to check`

Ein Typ: Dokumentation, ein Test, keine Verhaltensänderung. Ergibt der Spike einen Verlust und kommt Option B, lautet der Titel `fix(telemetry): flush the agent's exporters at the end of shutdown`.
