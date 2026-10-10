# Design

## Context

Grundregeln stehen in `lobby-tracing/design.md` (D1 bis D5, D9, D10): `Telemetry` per Konstruktor, Span nur für seltene Operationen, Zähler oder Span-Event für häufige, Spieler nur als `user.id` (UUID), keine Zähler-Attribute mit hoher Kardinalität. Dieser Change wendet sie auf season, daytime und protection an.

- `SeasonModule.check` läuft alle paar Minuten auf dem Scheduler (TickSchedulerThread) und beim Disconnect einen Tick später. Der Span entsteht im Check, nicht im Disconnect-Listener. Das Ergebnis ist die Aufzählung aus `SeasonSchedule.Desired` und der Restart-Politik (`NONE`/`PENDING`/`STOP`); Werte werden auf die festen `outcome`-Namen abgebildet.
- `ProtectionModule` hängt `Cancelable::cancel` an viele Events (`PickupItemEvent`, `InventoryPreClickEvent`, Block-Events, Schaden). Diese Events sind hochfrequent: Die Handler werden mit `on` registriert und zählen nur. Ein Hilfsaufruf `Cancelable::cancel` wird durch einen Lambda ersetzt, der zählt und abbricht (eine Stelle, eine Hilfsmethode, kein Span).
- **Entschieden:** `daytime` bleibt nur mit Metriken; Ausnahmen deckt `titan.listener.failures` ab.
- `daytime`: Ein Span alle paar Sekunden wäre Rauschen; nur Zähler. Die Konfigurationsablehnung steht heute als Einmal-Warnung im Log; der Zähler macht sie in Mimir sichtbar.
