# Proposal

## Why

Die kleinen Spielfunktionen der Lobby (Sitzen, Elytra, Kitzeln, Respawn) laufen unbeobachtet: Ob Spieler sie nutzen und ob sie Fehler auslösen, zeigt weder Trace noch Metrik.

Teil der Aufteilung aus `lobby-tracing` (Fundament): Dieser Change nutzt `Telemetry`, `FeatureNode.attach(…, telemetry)` und `onTraced` und setzt voraus, dass das Fundament auf `main` ist.

## What Changes

- **sit:** Span `sit.start` (`sit.block`, `user.id`) und `sit.stop` (`sit.stop.reason`: `sneak`/`dismount`/`disconnect`); Zähler `titan.sit.sessions{event}` (`started`/`stopped`).
- **elytra:** Span `elytra.glide.start` (bei `PlayerStartFlyingWithElytraEvent`, legt die Rakete ab) und `elytra.glide.end` (Landung, `elytra.glide.duration_ms` über die injizierte `Clock`); Zähler `titan.elytra.flights{event}`, Zähler `titan.elytra.boosts` (Raketenzündungen). `FireworkBoostTracker.advance` läuft jeden Tick und bekommt **keinen** Span.
- **tickle:** Zähler `titan.tickle.attacks{result}` (`tickled`/`cooldown`), kein Span (Kämpfe können häufig sein).
- **respawn:** Span `respawn.perform` (der um einen Tick verzögerte `player::respawn`), Zähler `titan.player.respawns`.
- Alle vier: `FeatureNode.attach(…, telemetry)`, Listener mit `onTraced`, wo ein Span entsteht.

Rollout: später, nach Fundament und `lobby-tracing-jumprun`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: neue Anforderungen für sit, elytra, tickle und respawn.

## Impact

- **Code:** `features/sit`, `features/elytra`, `features/tickle`, `features/respawn` (je `package-info.java`: `requires Telemetry`).
- **Abhängigkeiten:** keine neuen (`opentelemetry-api` kommt aus `core`).
- **Spielerverhalten, Nutzertexte, Logs:** unverändert. Ohne Agent sind alle Spans und Zähler No-ops.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): trace sit, elytra, tickle and respawn`
