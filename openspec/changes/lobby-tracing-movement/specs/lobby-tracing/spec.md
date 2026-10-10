# Spec Delta

## ADDED Requirements

### Requirement: Sitzen ist sichtbar
Die Lobby MUSS für jedes Hinsetzen einen Span `sit.start` mit Block und Spieler-UUID und für jedes Aufstehen einen Span `sit.stop` mit dem Grund (`sneak`, `dismount`, `disconnect`) erzeugen und `sit.sessions{event}` erhöhen.

#### Scenario: Hinsetzen und Aufstehen
- **WHEN** ein Spieler sich auf einen erlaubten Block setzt und mit Schleichen aufsteht
- **THEN** gibt es einen Span `sit.start` und einen Span `sit.stop` mit `sit.stop.reason=sneak`, und `sit.sessions{event=started}` und `{event=stopped}` stehen auf eins

#### Scenario: Disconnect im Sitzen
- **WHEN** ein sitzender Spieler die Verbindung trennt
- **THEN** gibt es einen Span `sit.stop` mit `sit.stop.reason=disconnect`

### Requirement: Elytra-Flüge sind sichtbar
Die Lobby MUSS beim Start eines Elytra-Flugs einen Span `elytra.glide.start` und bei der Landung einen Span `elytra.glide.end` mit der Flugdauer erzeugen. Raketenzündungen MÜSSEN nur über `elytra.boosts` gezählt werden. Die Lobby DARF KEINEN Span je Tick des Flugs erzeugen.

#### Scenario: Flug mit Landung
- **WHEN** ein Spieler zu fliegen beginnt, zwei Raketen zündet und landet
- **THEN** gibt es einen Span `elytra.glide.start` und einen Span `elytra.glide.end` mit `elytra.glide.duration_ms`, und `elytra.boosts` steht auf zwei

#### Scenario: Keine Spans pro Tick
- **WHEN** ein Spieler 100 Ticks fliegt
- **THEN** entstehen für diese Ticks keine weiteren Spans

### Requirement: Kitzeln und Respawn sind messbar
Die Lobby MUSS `tickle.attacks{result}` bei jedem Kitzel-Angriff (`tickled` oder `cooldown`) erhöhen und dafür keinen Span erzeugen. Für jeden Respawn MUSS ein Span `respawn.perform` entstehen und `player.respawns` steigen.

#### Scenario: Kitzeln im Cooldown
- **WHEN** ein Spieler innerhalb der Abkühlzeit erneut kitzelt
- **THEN** steigt `tickle.attacks{result=cooldown}` um eins, und es entsteht kein Span

#### Scenario: Respawn nach Tod
- **WHEN** ein Spieler stirbt und im nächsten Tick respawnt
- **THEN** gibt es einen Span `respawn.perform` mit `user.id`, und `player.respawns` steht auf eins
