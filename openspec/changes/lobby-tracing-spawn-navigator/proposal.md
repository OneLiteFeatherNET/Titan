# Proposal

## Why

Warum ein Spieler zum Spawn gesetzt wurde (Befehl `/spawn`, Navigator, Höhengrenze, Jump-and-Run) und welche Ziele der Navigator nutzt, ist unsichtbar.

Teil der Aufteilung aus `lobby-tracing` (Fundament): Dieser Change nutzt `Telemetry`, `FeatureNode.attach(…, telemetry)` und `onTraced` und setzt voraus, dass das Fundament auf `main` ist.

## What Changes

- `spawn.return` (Span in `LobbySpawnReturn.sendToSpawn`): `spawn.return.source` (`command`/`navigator`/`event`), `spawn.return.result` (`sent`/`already_at_spawn`/`blocked`), `user.id`.
- `spawn.bounds_teleport` (Span, wenn `SpawnBoundsListener` zum Spawn setzt): `spawn.y`, `spawn.min_height`, `spawn.max_height`. Das Event ist `PlayerMoveEvent`: Der Listener bleibt ohne Span, der Span entsteht nur im seltenen Teleport-Zweig.
- `spawn.join` (Span für den Beitritts-Teleport in `SpawnJoinListener`/`SpawnConfigurationListener`, über `onTraced`).
- `navigator.open` (Span): `navigator.kind` (`public`/`team`), `navigator.entries`, `user.id`. `navigator.select` (Span je Klick auf ein Ziel): `navigator.destination`, `navigator.result` (`sent`/`denied`/`spawn`), `user.id`; das Ziel-Senden hängt als `deliver.send_player` darunter (aus `lobby-tracing-portal`; ohne dieses ist der Kind-Span einfach nicht da). `navigator.layout.apply` (Span, nur wenn das Layout sich geändert hat).
- Zähler: `titan.spawn.returns{source,result}`, `titan.spawn.bounds_teleports`, `titan.navigator.selections{destination,result}`.

Rollout: später, nach Fundament und `lobby-tracing-jumprun`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: neue Anforderungen für spawn und navigator.

## Impact

- **Code:** `features/spawn`, `features/navigator`, beide `package-info.java` mit `requires Telemetry`.
- **Abhängigkeiten:** keine neuen (`opentelemetry-api` kommt aus `core`).
- **Spielerverhalten, Nutzertexte, Logs:** unverändert. Ohne Agent sind alle Spans und Zähler No-ops.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): trace spawn returns and navigator use`
