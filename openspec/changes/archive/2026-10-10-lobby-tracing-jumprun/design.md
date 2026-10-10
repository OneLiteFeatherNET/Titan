# Design

## Context

Grundregeln stehen in `lobby-tracing/design.md` (D1 bis D5, D9, D10): `Telemetry` per Konstruktor, Span nur für seltene Operationen, Zähler oder Span-Event für häufige, Spieler nur als `user.id` (UUID), keine Zähler-Attribute mit hoher Kardinalität. Dieser Change wendet sie auf jumprun an.

- `end(Run, EndReason, details)` läuft auf dem Tick-Thread, dem virtuellen Read-Loop-Thread (Disconnect) oder im Shutdown-Hook. Der Span entsteht erst, nachdem `runs.remove(run)` gelang, damit ein zweites Ende keinen zweiten Span erzeugt. Er umschließt `records.submit(...)` und endet im `finally`. FALL und die anderen Gründe sind keine Fehler (Status unset), nur eine Ausnahme setzt ERROR.
- `jumprun.end`: `jumprun.end.reason`, `jumprun.mode`, `jumprun.score`, `jumprun.record`, `user.id`, bei FALL `jumprun.fall.y`, `jumprun.fall.threshold`, `jumprun.fall.course_index`.
- `jumprun.start`: `jumprun.mode`, `jumprun.start.outcome` (`started`/`no_room`), `user.id`.
- `jumprun.leaderboard.refresh`: Der Scheduler-Task ist eine Wurzel; der Span wird im Executor-Task um `board.refresh()` gestartet. Attribut `jumprun.leaderboard.runs_shown`.
- Verworfen: ein Span über einen ganzen Lauf (Minuten, bricht beim Disconnect ab), Spans je Tick/Move/Reroll. `PlayerMoveEvent` bleibt ohne Span.
- Zähler-Attribute `reason`, `mode`, `outcome` sind kleine Aufzählungen.
- Metriknamen tragen wie im Fundament das Präfix `titan.` (`titan.jumprun.runs.ended`, `titan.jumprun.runs.started`, `titan.jumprun.run.score`); Span-Namen bleiben `jumprun.*`.
- `jumprun.end` entsteht auch für Gründe ohne Speicherung (SHUTDOWN); `records.submit` wird dort nicht aufgerufen, `jumprun.record` ist `false`. Zähler und Histogramm zählen jedes Ende, auch SHUTDOWN.
- Das Histogramm hat keinen Test über `TestTelemetry`, weil dessen Reader nur Zähler und Gauges liest; `RunTelemetryTest` liest es über einen eigenen `InMemoryMetricReader`.
