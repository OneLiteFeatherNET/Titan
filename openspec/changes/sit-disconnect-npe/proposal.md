# Proposal

## Why

Live (Loki, Lobby-1, 2026-10-03 23:32) steht für den Spieler `theEvilReaper`:

```
ERROR Unhandled exception in module sit while handling an event for theEvilReaper
java.lang.NullPointerException
  at java.util.Objects.requireNonNull(Objects.java:220)
  at net.onelitefeather.titan.feature.sit.Seats.standUp(Seats.java:64)
  at net.onelitefeather.titan.feature.sit.SitModule.lambda$start$3(SitModule.java:98)
  at ...ListenerGuard...
  at net.minestom.server.network.player.PlayerConnection.disconnect(PlayerConnection.java:162)
  at ...Server.playerReadLoop ... VirtualThread.run
```

Der `PlayerDisconnectEvent` ruft für **jeden** Spieler `seats.standUp(player)`. In `standUp` steht:

```java
Optional.ofNullable(player.getTag(ARROW)).map(player.getInstance()::getEntityByUuid)...
```

`player.getInstance()::getEntityByUuid` ist eine gebundene Methodenreferenz: Der Empfänger `player.getInstance()` wird **sofort beim Bauen des Ausdrucks** ausgewertet, auch wenn der Spieler gar nicht sitzt, und `Objects.requireNonNull` wirft, sobald der Spieler keine Instanz mehr hat. Das passiert, wenn ein Spieler die Verbindung verliert, bevor er in eine Instanz gesetzt wurde oder nachdem er schon aus ihr entfernt wurde (Verbindungsabbruch in der Konfigurationsphase, Kick, Timeout). Der bestehende Test `disconnectingWhileSittingRemovesTheSeatEntity` ruft das Event, solange der Spieler noch in der Instanz steht, und kann den Fehler deshalb nicht finden.

Folgen: Ein ERROR mit Stacktrace pro solchem Disconnect (Rauschen in Loki, `titan.listener.failures` steigt), und bei einem sitzenden Spieler ohne Instanz bleibt das Tag stehen, bis der Spieler-Datensatz verschwindet.

## What Changes

- `Seats.standUp` löst die Instanz einmal lokal auf und ist null-sicher: Hat der Spieler keine Instanz, entfernt es nur seine Sitz-Tags und kehrt zurück. Den Sitz räumt dann `SeatEntity.update` ab (er entfernt sich, sobald er keinen Passagier mehr hat).
- Die Instanz wird nur einmal gelesen (eine lokale Variable), damit ein gleichzeitiges Entfernen auf dem Tick-Thread zwischen Prüfung und Benutzung keine neue NPE erzeugt.
- Regressionstests: Disconnect eines Spielers ohne Instanz (sitzend und nicht sitzend) wirft nicht.

Nicht Teil dieser Änderung: ein anderes Teleport-Verhalten beim Disconnect (siehe Design D3), andere Module, Telemetrie.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-modules`: neue Anforderung zum Sitzen und Verlassen (Disconnect wirft nie).

## Impact

- **Code:** `features/sit` (`Seats`, Tests). Keine Abhängigkeiten.
- **Betrieb:** Die ERROR-Zeile „Unhandled exception in module sit“ beim Disconnect verschwindet.
- **Spielerverhalten:** unverändert. **Nutzertexte:** keine.

## Delivery

Pull-Request-Titel und Squash-Commit: `fix(sit): do not throw when a player without an instance disconnects`
