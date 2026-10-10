# Spec Delta

## MODIFIED Requirements

### Requirement: Laufende
Ein Lauf MUSS enden, wenn der Spieler mehr als drei Blöcke unter den Block fällt, auf dem er zuletzt gelandet ist, wenn er das Jump-and-Run-Item erneut benutzt, wenn er stirbt, wenn er zum Lobby-Spawn zurückkehrt (per Befehl oder Navigator, siehe `lobby-spawn-return`) oder wenn er die Lobby verlässt. Bei einem Absturz MUSS die Lobby ihn an den Startpunkt seines Laufs zurücksetzen. Eine Rückkehr zum Spawn MUSS den Lauf beenden wie ein Abbruch über das Item, also mit gewertetem Score, bevor der Spieler versetzt wird; der Spieler landet am Spawn, nicht am Startpunkt des Laufs. Während eines Laufs DARF der Läufer KEINE Elytra tragen, damit ein Leertastendruck in der Luft kein Gleiten auslöst; mit dem Laufende MUSS er seine normale Lobby-Ausstattung zurückerhalten. Nach dem Ende DÜRFEN keine Blöcke des Laufs für ihn sichtbar bleiben, und an ihren Stellen MUSS er wieder die echte Welt sehen.

#### Scenario: Absturz
- **WHEN** der Spieler mehr als drei Blöcke unter seinen letzten Block fällt
- **THEN** endet der Lauf, alle Blöcke verschwinden, und er steht wieder am Startpunkt des Laufs

#### Scenario: Keine Elytra im Lauf
- **WHEN** ein Spieler einen Lauf startet
- **THEN** trägt er keine Elytra mehr, und ein Leertastendruck in der Luft beendet den Lauf nicht

#### Scenario: Ausstattung zurück
- **WHEN** der Lauf endet, egal aus welchem Grund außer Verlassen der Lobby
- **THEN** trägt der Spieler wieder seine Elytra und hat seine normale Hotbar

#### Scenario: Abbruch über das Item
- **WHEN** der Spieler während eines Laufs das Jump-and-Run-Item benutzt
- **THEN** endet der Lauf, und alle Blöcke verschwinden

#### Scenario: Spieler verlässt die Lobby
- **WHEN** der Spieler während eines Laufs die Verbindung trennt
- **THEN** endet der Lauf, und die Lobby hält keinen Zustand dieses Laufs mehr

#### Scenario: Rückkehr zum Spawn im Lauf
- **WHEN** ein Läufer mit Score 12 im Lauf `/spawn` eingibt
- **THEN** endet der Lauf mit Score 12 und der Endmeldung wie bei einem Abbruch, alle Blöcke verschwinden, er hat seine Ausstattung zurück und steht am Lobby-Spawn
