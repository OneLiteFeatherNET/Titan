# Design

## Context

`origin/main`, `features/sit`:

- `SitModule` hängt vier Listener an den `FeatureNode`; `PlayerDisconnectEvent` ruft `seats.standUp(event.getPlayer())` **ohne** vorherige `isSitting`-Prüfung (anders als der `EntityDismountEvent`-Listener).
- `Seats.standUp` (Zeile 64 im Log): `Optional.ofNullable(player.getTag(ARROW)).map(player.getInstance()::getEntityByUuid).ifPresent(...)`. Der Empfänger der Methodenreferenz wird beim Bauen der Referenz ausgewertet. Ist `getInstance()` dann `null`, wirft `Objects.requireNonNull` (genau der Stack im Log), **unabhängig davon, ob das Tag gesetzt ist**.
- `Seats.sit` prüft `player.getInstance() == null` schon und kehrt zurück; `standUp` hat diese Absicherung nicht.
- Der Event läuft auf einem virtuellen Read-Loop-Thread (`PlayerConnection.disconnect`), nicht auf dem Tick-Thread. Entfernt wird der Spieler aus seiner Instanz auf dem Tick-Thread oder im Disconnect-Pfad; `Player#getInstance()` ist ein volatiles Feld, `Instance#getEntityByUuid` liest eine nebenläufige Map.
- Der Sitz (`SeatEntity`) entfernt sich selbst in `update`, sobald er keinen Passagier mehr hat.

Ursache, kurz: **nicht** ein fehlender Sitz oder ein Rennen allein, sondern ein eager ausgewerteter Methodenreferenz-Empfänger, der bei jedem Disconnect eines Spielers ohne Instanz wirft. Welcher Pfad den Spieler im Log-Fall instanzlos machte (Abbruch vor dem ersten Spawn, Kick, Timeout), lässt sich aus dem Log nicht sagen und muss es nicht: Die Korrektur deckt alle.

## Goals / Non-Goals

**Goals:**
- `standUp` und `isSitting` werfen für keinen Spieler, auch nicht ohne Instanz, auch nicht, wenn die Instanz zwischen Prüfung und Benutzung verschwindet.
- Tags eines gehenden Spielers sind danach entfernt.

**Non-Goals:** Der Teleport zurück an die Ursprungsposition beim Disconnect (D3), Sitz-Logik.

## Decisions

### D1 Instanz einmal lesen, früh abbrechen

```java
void standUp(Player player) {
    UUID arrowId = player.getTag(ARROW);
    if (arrowId == null) { return; }
    Instance instance = player.getInstance();       // einmal lesen: kein TOCTOU gegen den Tick-Thread
    player.removeTag(ARROW);
    if (instance == null) { player.removeTag(ORIGIN); return; }   // Sitz räumt SeatEntity.update ab
    Entity arrow = instance.getEntityByUuid(arrowId);
    ...bisheriger Ablauf (teleport, removePassenger, remove)
}
```

Ohne Tag passiert gar nichts (kein `getInstance()` mehr im Normalfall des Disconnects eines nicht sitzenden Spielers, der Hauptfall im Log). Die Lösung bleibt in `Seats` (eine Verantwortung); `SitModule` ändert sich nicht, die Prüfung `isSitting` davor im Disconnect-Listener wäre doppelte Logik (DRY).

### D2 Thread-Sicherheit

Der Disconnect-Listener läuft auf einem virtuellen Thread, der Tick-Thread kann gleichzeitig `SeatEntity.update` ausführen. Das ist unkritisch, weil:
- das Lesen von `getInstance()` einmal geschieht und der Rest mit der lokalen Referenz arbeitet (eine vom Tick-Thread soeben entfernte Instanz bleibt eine gültige Referenz; `getEntityByUuid` liefert dann höchstens `null`, `ifPresent`/Null-Prüfung behandelt das);
- `Entity#remove` und `removePassenger` in Minestom idempotent sind (zweimaliges Entfernen durch Disconnect-Thread und `SeatEntity.update` ist harmlos). **Zu prüfen in Task 1.3** mit einem Test, der `remove` erst durch `update` (`env.tick()`) und dann durch `standUp` auslöst.
Kein neuer Lock, keine neue Konkurrenz-Abstraktion (KISS). Die Tags `ARROW` und `ORIGIN` gehören dem Spieler und werden nur von seinem eigenen Disconnect/Dismount berührt.

### D3 Teleport beim Disconnect bleibt wie er ist

Ein sitzender Spieler wird beim Disconnect weiter an `ORIGIN` teleportiert, solange er eine Instanz hat. Das ist nach dem Disconnect nutzlos (die Verbindung ist zu), aber harmlos und nicht der Fehler. Wegfallen lassen wäre eine Verhaltensänderung in einem Fix-Change und gehört nicht hierher. Offene Frage unten.

### D4 Test-Lücke schließen

`SitModuleIntegrationTest.disconnectingWhileSittingRemovesTheSeatEntity` ruft den Event bei noch vorhandener Instanz. Die neuen Tests rufen `standUp` (Unit, `SeatsTest`) und den echten Listener (`SitModuleIntegrationTest`) für einen Spieler, den vorher `player.remove()`/`instance.removeEntity(player)` instanzlos gemacht hat.

## Risks / Trade-offs

- Bleibt der Sitz nach einem Disconnect ohne Instanz stehen, bis `SeatEntity.update` ihn entfernt? Er hat keinen Passagier mehr, also entfernt er sich im nächsten Tick, solange er in einer geladenen Instanz ist. Test in 1.4 zeigt es mit `env.tick()`.
- Ein Spieler, der ohne Instanz sitzt, kann realistisch nicht vorkommen (er sitzt in einer Instanz); der Zweig ist reine Absicherung.

## Open Questions

- Soll der Teleport beim Disconnect ganz entfallen (spart Pakete an eine tote Verbindung)? Eigener Change, falls ja.
