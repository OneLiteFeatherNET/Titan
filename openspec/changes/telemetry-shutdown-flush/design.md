# Design

## Context

Befunde (am 2026-10-04 geprüft):

- **API ohne Flush.** `io.opentelemetry.api.trace.TracerProvider` in `opentelemetry-api-1.51.0.jar` hat `noop()`, `get(String)`, `get(String,String)`, `tracerBuilder(String)`, kein `forceFlush`. Dasselbe gilt für `OpenTelemetry`/`MeterProvider`. `forceFlush` und `shutdown` existieren nur an den SDK-Typen.
- **Agent 2.16.0** (`Implementation-Version: 2.16.0`): Der SDK-Hook entsteht in `AutoConfiguredOpenTelemetrySdkBuilder` (`addShutdownHook`, im Agent-Classloader `inst/…`); `OpenTelemetryInstaller` setzt zusätzlich einen `ForceFlusher` in `io.opentelemetry.javaagent.bootstrap.OpenTelemetrySdkAccess`, der `SdkTracerProvider`, `SdkMeterProvider` und `SdkLoggerProvider` flusht. `OpenTelemetrySdkAccess` ist in der **Bootstrap-Schicht** des Agents und hat `public static void forceFlush(long, TimeUnit)` (und die `int`-Variante). Es ist **kein** API und nicht dokumentiert stabil; der Agent selbst nutzt es für Lambda-Instrumentierung.
- `GlobalOpenTelemetry.get()` liefert im Agent eine Brücke (Application-Wrapper), nicht das SDK; ein Cast auf SDK-Typen scheitert (andere Classloader).
- **Titan:** `Titan.initialize()` → `MinecraftServer.getSchedulerManager().buildShutdownTask(() -> lifecycle.shutdown(beanScope::close))`. `TitanLifecycle.shutdown` ist `Telemetry.inSpan("titan.shutdown", …)`; `inSpan` beendet den Span im `finally`. Der Task läuft in `MinecraftServer.stopCleanly()`; Aufrufer: `StopCommand`, `EndCommand`, `MinecraftServerStop` (Saison), jeweils auf eigenem Thread, **nicht** in einem JVM-Hook. Im Repo gibt es keinen `Runtime.addShutdownHook` (nur `MinestomLoader….registerShutdownHook()` in `LuckPermsPermissionService`).
- `jumprun.end` mit SHUTDOWN endet in `beanScope.close()` (JumprunModule `@PreDestroy`), also innerhalb von `titan.shutdown`.
- Der Batch-Prozessor exportiert standardmäßig alle 5 s (`otel.bsp.schedule.delay`), Metriken alle 60 s (`otel.metric.export.interval`).

Daraus folgt: Auf dem normalen Stoppweg endet jeder Shutdown-Span, bevor die JVM den Agent-Hook startet; der Hook flusht ihn mit. Risiko besteht nur, wenn die JVM ohne vorheriges `stopCleanly()` heruntergefahren wird (SIGTERM ohne Titan-Hook: dann läuft Titans Shutdown-Task gar nicht, und es gibt auch keinen `titan.shutdown`-Span, der verloren gehen könnte) oder ein Span nach dem Task endet (z. B. LuckPerms-Hook, Pool-Schließen in fremdem Hook).

## Goals / Non-Goals

**Goals:** Eine belegte Antwort, ob und wann Shutdown-Spans fehlen; das Ergebnis dokumentiert und mit einem Test/Smoke abgesichert; kein SDK in der Lobby.

**Non-Goals:** Absicherung gegen SIGKILL, Agent-Extension, eigene Exporter.

## Decisions

### D1 Erst messen (Spike)

Vor jeder Code-Entscheidung läuft der lokale Versuch aus Task 1.1 mit Agent 2.16.0 und `-Dotel.traces.exporter=logging` (oder OTLP an einen lokalen Collector) für drei Wege: Konsole `stop`, `StopCommand`-ähnlicher Aufruf (Saisonwechsel) und `kill -TERM`. Gemessen wird, ob `titan.shutdown` und `jumprun.end{reason=SHUTDOWN}` im Export erscheinen, mit der Standardverzögerung und mit `otel.bsp.schedule.delay=60000` (der Hook muss dann den Rest flushen). So wird aus der Vermutung eine Tabelle.

### D2 Optionen und Empfehlung

| Option | Aufwand | Wirkung | Nachteil |
| --- | --- | --- | --- |
| **A. Dokumentieren + Test + Smoke (empfohlen)** | klein | Garantie: Span endet vor Rückkehr des Tasks; Grenzen bekannt | Verlust bleibt möglich außerhalb des Tasks |
| B. Reflexions-Flush am Ende des Tasks | klein (eine Klasse, `Class.forName`) | flusht Traces, Metriken, Logs vor dem Hook, auch auf Pfaden mit Hook-Race | hängt an interner Agent-Klasse (`javaagent.bootstrap`), keine Stabilitätszusage, bricht still bei Agent-Update; Timeout verlängert das Abschalten |
| C. Reihenfolge der Hooks steuern | nicht machbar | JVM-Hooks haben keine Reihenfolge | – |
| D. Agent-Extension (`AgentListener`) | mittel, außerhalb des Repos | sauberer Hook im Agent | die Extension (`MinecraftOTEL.jar`) gehört dem Betrieb; zweiter Release-Zyklus |

Empfehlung: **A**. Sie passt zu KISS und zum Teamentscheid „kein SDK in der Lobby“, und der normale Stoppweg ist nach den Befunden schon sicher. **B bleibt die vorab entschiedene Rückfalloption**, falls D1 auf einem realen Stoppweg einen reproduzierbaren Verlust zeigt:

- `runtime`: `AgentFlush` (paketprivat), `static void flush(Duration timeout)`: `Class.forName("io.opentelemetry.javaagent.bootstrap.OpenTelemetrySdkAccess")`, `getMethod("forceFlush", long.class, TimeUnit.class)`, `invoke`; `ClassNotFoundException` → No-op ohne Log (kein Agent); jede andere Ausnahme → ein WARN, nie weiterwerfen.
- Aufruf **nach** dem Ende des `titan.shutdown`-Spans, als letzter Schritt des Shutdown-Tasks, mit kleinem Timeout (z. B. 2 s), damit ein hängender Exporter das Abschalten nicht blockiert.
- Test per Einspeisen eines Flushers (Interface, fester Fake), nicht per echtem Agent; der Smoke prüft die Reflexion einmal mit dem Agent.
- ArchUnit: `io.opentelemetry.javaagent` nur in dieser einen Klasse.

### D3 Der Test für Option A

`TitanLifecycleTest` hat schon Spans bei Erfolg und Ausnahme. Neu: Der Test liest `titan.shutdown` über `TestTelemetry` **unmittelbar nach** dem Rückkehren von `shutdown(...)` (SimpleSpanProcessor, synchron) und beweist so: der Span ist beendet, bevor der Task zurückkehrt, auch wenn `body` wirft; ebenso ein Span, der **im** Body endet (Stellvertreter für `jumprun.end`). Das ist die Garantie, auf der A beruht.

### D4 Dokumentation

`docs/lobby-modules.md`, Abschnitt „Traces und Metriken“: neuer Unterabschnitt „Herunterfahren“ mit (1) der Garantie, (2) den Grenzen (SIGKILL, Spans außerhalb des Tasks, Metrik-Intervall von 60 s: der letzte Zählerstand kann fehlen), (3) den Stellschrauben `otel.bsp.schedule.delay`/`otel.metric.export.interval`, (4) der Tempo-Abfrage `{ name = "titan.shutdown" }` für den Smoke nach einem geplanten Neustart.

## Risks / Trade-offs

- A beseitigt keinen Verlust, falls D1 einen zeigt; dann greift B. Das ist gewollt: erst belegen, dann Code.
- B koppelt an eine Agent-Interna. Ein Agent-Update (Renovate) kann sie entfernen; der Fallback (WARN, No-op) macht das unschädlich, und der Smoke fängt es ab.

## Open Questions

- Commit-Typ: `docs` (Option A) oder `fix` (Option B)? Entscheidet das Ergebnis aus D1; der Titel in `proposal.md` ist dann anzupassen.
- Schickt CloudNet bei `stop` Konsole oder SIGTERM? Der Spike deckt beides ab; die Antwort gehört in die Doku.
