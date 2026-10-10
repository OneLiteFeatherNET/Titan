# Proposal

## Why

Bleibt ein Spieler in der Lobby irgendwo stecken (auf einem Dach, in einer Lücke, zwischen Blöcken), hat er heute nur einen Ausweg: die Verbindung trennen und neu verbinden. Automatisch zurück zum Spawn geht es nur beim Betreten, nach dem Tod oder beim Verlassen der Höhengrenzen.

## What Changes

- Neuer Befehl `/spawn` für alle Spieler: Er bringt den Spieler sofort an den Lobby-Spawn, ohne Countdown, und bestätigt das im Chat.
- Neuer Eintrag „Spawn“ im Navigator auf **Platz 2** mit einem **Kompass** als Symbol. Ein Klick bringt den Spieler zum Spawn und schließt den Navigator. Es gibt keine Weiterleitung auf einen anderen Server.
- Befehl und Navigator nutzen denselben Weg zurück. Bevor teleportiert wird, erfahren andere Features davon: Ein laufendes Jump and Run endet wie ein Abbruch über das Item (Score zählt, Blöcke verschwinden, Ausstattung kommt zurück).
- Hat die aktive Lobby-Karte keinen Spawn-Punkt, gibt es eine Meldung statt eines Teleports.
- Gleiten mit der Elytra wird durch den Teleport einfach unterbrochen.
- Alle neuen Texte stehen in der Sprache des Spielers, mit Englisch als Fallback. Der Name „Spawn“ im Navigator ist wie die übrigen Ziele sprachneutral.

## Capabilities

### New Capabilities
- `lobby-spawn-return`: Spieler kehren per Befehl oder Navigator an den Lobby-Spawn zurück. Andere Features erfahren vorher davon und räumen ihren Zustand auf.

### Modified Capabilities
- `lobby-navigator`: Die Anforderung „Navigator-Ziele sind im Navigator-Modul festgelegt“ belegt Platz 2 mit „Spawn“. Die Anforderung „Auswahl eines Ziels leitet weiter“ unterscheidet Server-Ziele von der Rückkehr zum Spawn.
- `lobby-jumprun`: Die Anforderung „Laufende“ nennt die Rückkehr zum Spawn als weiteren Grund, der einen Lauf beendet.

## Impact

- **Code:**
  - `core` bekommt das Interface `SpawnReturn` und das Event `LobbyReturnToSpawnEvent`.
  - `features/spawn` stellt `SpawnReturn` bereit, meldet `/spawn` beim `CommandManager` an und bekommt eigene Sprachdateien.
  - `features/navigator` bekommt den festen Eintrag auf Platz 2.
  - `features/jumprun` hört auf das Event und beendet den Lauf.
  - Die Cross-Column-Tests in `apps/cloudnet` prüfen das Zusammenspiel.
- **Abhängigkeiten:** keine neuen.
- **Nutzertexte (neu):**
  - Bestätigung „Du bist wieder am Spawn“,
  - Meldung „Diese Karte hat keinen Spawn-Punkt“,
  - Navigator-Eintrag „Spawn“ (sprachneutral).
- **Nutzertexte (geändert):** keine.
- **Rechte:** Der Befehl braucht kein Recht. Der Proxy hat `/lobby` bzw. `/hub` oft schon für „zurück zum Lobby-Server“ belegt, deshalb kommen diese Aliase bewusst nicht dazu.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(spawn): let players return to spawn with /spawn and the navigator`
