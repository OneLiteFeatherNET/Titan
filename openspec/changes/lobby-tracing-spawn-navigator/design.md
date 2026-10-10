# Design

## Context

Grundregeln stehen in `lobby-tracing/design.md` (D1 bis D5, D9, D10): `Telemetry` per Konstruktor, Span nur für seltene Operationen, Zähler oder Span-Event für häufige, Spieler nur als `user.id` (UUID), keine Zähler-Attribute mit hoher Kardinalität. Dieser Change wendet sie auf spawn und navigator an.

- `LobbySpawnReturn` (`core`-Interface `SpawnReturn`) wird von `/spawn`, vom Navigator-Eintrag und über `LobbyReturnToSpawnEvent` von jumprun genutzt. Der Span steht in der Implementierung, `source` kommt als Argument oder aus dem Aufrufer.
- `SpawnBoundsListener` hört auf `PlayerMoveEvent`: kein Span am Listener, nur im Teleportzweig (`spawn.bounds_teleport`, selten).
- Aves-Inventar-Klicks sind selten und von Menschen ausgelöst: Spans erlaubt. `navigator.destination` ist ein Wert aus der festen `Destination`-Aufzählung.
- Das Rechte-Ergebnis (`navigator.result=denied`) kommt aus `isAllowed`.
