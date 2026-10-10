# Proposal

## Why

Warum ein Jump-and-Run-Lauf endete (`EndReason`: ABORT, FALL, EXHAUSTED, DEATH, SPAWN_RETURN, DISCONNECT, LEFT_INSTANCE, SHUTDOWN, bei FALL mit `y`/Schwelle/Index), steht nur in einer DEBUG-Zeile, die live nicht nach Loki geht. Die Hibernate-Spans der Lauf-Speicherung hängen an keinem Elternspan.

Teil der Aufteilung aus `lobby-tracing` (Fundament): Dieser Change nutzt `Telemetry`, `FeatureNode.attach(…, telemetry)` und `onTraced` und setzt voraus, dass das Fundament auf `main` ist.

## What Changes

- `JumprunModule` bekommt `Telemetry`, ein kleines `RunTelemetry` kapselt Namen und Attribute.
- Spans: `jumprun.end` (je beendetem Lauf), `jumprun.start` (je Startversuch), `jumprun.leaderboard.refresh` (je Aktualisierung).
- Metriken: Zähler `titan.jumprun.runs.ended{reason,mode}`, `titan.jumprun.runs.started{mode,outcome}`, Histogramm `titan.jumprun.run.score{mode}`.
- Kontext: `records.submit` läuft im Span `jumprun.end`; über den `DatabaseWriter` (Fundament) hängen die Hibernate-Spans darunter. Die Bestenlisten-Aktualisierung startet ihren Span im Executor-Task.

Rollout: nach dem Fundament, gemeinsam mit dem ersten Schub.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: neue Anforderungen für jumprun.

## Impact

- **Code:** `features/jumprun` (`RunTelemetry`, `begin`, `end`, `refreshLeaderboard`, `package-info.java`: `requires Telemetry`).
- **Abhängigkeiten:** keine neuen (`opentelemetry-api` kommt aus `core`).
- **Spielerverhalten, Nutzertexte, Logs:** unverändert. Ohne Agent sind alle Spans und Zähler No-ops.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): trace jump and run starts, ends and leaderboard refreshes`
