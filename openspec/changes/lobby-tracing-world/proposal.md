# Proposal

## Why

Die Saison-Prüfung, die Tageszeit und die Schutzregeln der Lobby laufen still. Warum eine Saison wechselt oder ein Neustart angefragt wird, und wie oft Schutzregeln greifen, ist nicht zu sehen.

Teil der Aufteilung aus `lobby-tracing` (Fundament): Dieser Change nutzt `Telemetry`, `FeatureNode.attach(…, telemetry)` und `onTraced` und setzt voraus, dass das Fundament auf `main` ist.

## What Changes

- **season:** Span `season.check` je periodischer Prüfung (Minutentakt, `SeasonModule.check`): `season.current`, `season.desired`, `season.outcome` (`unchanged`/`pending_restart`/`restart_requested`/`unresolvable`), `season.online_players`. Span-Event `season.stop_requested`. Zähler `titan.season.restarts_requested`, `titan.season.checks{outcome}`.
- **daytime:** kein Span (periodisches Housekeeping, `UPDATE_INTERVAL`). Zähler `titan.daytime.updates` und `titan.daytime.config_rejected{reason}` (statt nur einmaliger Warnung im Log), Gauge `daytime.minute_of_day` ist **nicht** Teil (Wert ändert sich laufend, kaum nützlich).
- **protection:** kein Span, Zähler `titan.protection.denied{event}` je abgebrochenem Event (`pickup`, `inventory_click`, `block_break`, … nach dem tatsächlich registrierten Handler). Hochfrequente Events, daher nur Zähler.
- `FeatureNode.attach(…, telemetry)` in allen drei.

Rollout: später, nach Fundament und `lobby-tracing-jumprun`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: neue Anforderungen für season, daytime und protection.

## Impact

- **Code:** `features/season`, `features/daytime`, `features/protection` (`package-info.java`: `requires Telemetry`).
- **Abhängigkeiten:** keine neuen (`opentelemetry-api` kommt aus `core`).
- **Spielerverhalten, Nutzertexte, Logs:** unverändert. Ohne Agent sind alle Spans und Zähler No-ops.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): trace season changes, daytime and protection denials`
