# Design

## Context

Grundregeln stehen in `lobby-tracing/design.md` (D1 bis D5, D9, D10): `Telemetry` per Konstruktor, Span nur für seltene Operationen, Zähler oder Span-Event für häufige, Spieler nur als `user.id` (UUID), keine Zähler-Attribute mit hoher Kardinalität. Dieser Change wendet sie auf sit, elytra, tickle und respawn an.

- Sitzen, Elytra-Start und -Landung und Respawn sind Ereignisse einzelner Spieler und selten genug für Spans. Der Elytra-Flug selbst ist kein Span (Dauer: Sekunden bis Minuten). Statt dessen entsteht ein kurzer Span bei der Landung mit der gemessenen Dauer (Start-Zeitpunkt je Spieler im Modul, über `Clock`, beim Disconnect vergessen wie `boosts.forget`).
- **Entschieden:** `tickle` bleibt nur mit Metriken; Ausnahmen deckt `titan.listener.failures` ab.
- Raketenzündungen und Kitzel-Angriffe können gehäuft auftreten: nur Zähler. Der Zähler `tickle.attacks` zählt auch den Cooldown-Fall, damit ein Spam sichtbar wird.
- `sit.block` ist der Block-Schlüssel aus der konfigurierten Liste erlaubter Blöcke (kleine Menge).
