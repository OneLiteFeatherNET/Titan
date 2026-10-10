# Spec Delta

## ADDED Requirements

### Requirement: Spawn-Rückkehr ist sichtbar
Die Lobby MUSS für jede Rückkehr zum Spawn einen kurzen Span `spawn.return` mit Quelle (`command`, `navigator`, `event`), Ergebnis und Spieler-UUID erzeugen und `titan.spawn.returns{source,result}` erhöhen. Setzt der Spawn-Modul-Listener einen Spieler wegen der Höhengrenzen zurück, MUSS ein Span `spawn.bounds_teleport` mit `y` und den Grenzen entstehen. Der Listener auf `PlayerMoveEvent` DARF selbst KEINEN Span erzeugen.

#### Scenario: `/spawn`
- **WHEN** ein Spieler `/spawn` nutzt und zum Spawn gesetzt wird
- **THEN** gibt es einen Span `spawn.return` mit `spawn.return.source=command`, `spawn.return.result=sent` und `user.id`, und `titan.spawn.returns{source=command,result=sent}` steht auf eins

#### Scenario: Höhengrenze
- **WHEN** ein Spieler unter `spawn.minHeight` fällt
- **THEN** gibt es genau einen Span `spawn.bounds_teleport` mit `spawn.y` und den Grenzen, und für Bewegungen innerhalb der Grenzen entsteht kein Span

### Requirement: Navigator-Nutzung ist sichtbar
Die Lobby MUSS für das Öffnen des Navigators einen Span `navigator.open` mit Art und Eintragszahl und für jede Auswahl eines Ziels einen Span `navigator.select` mit Ziel und Ergebnis (`sent`, `denied`, `spawn`) erzeugen und `titan.navigator.selections{destination,result}` erhöhen. Ein neu angewendetes Layout MUSS einen Span `navigator.layout.apply` erzeugen, ein unverändertes keinen.

#### Scenario: Verweigertes Ziel
- **WHEN** ein Spieler ein Ziel wählt, für das ihm das Recht fehlt
- **THEN** gibt es einen Span `navigator.select` mit `navigator.result=denied`, und es wird kein Transfer ausgelöst

#### Scenario: Layout unverändert
- **WHEN** der Navigator geöffnet wird und sich das Layout nicht geändert hat
- **THEN** gibt es einen Span `navigator.open`, aber keinen Span `navigator.layout.apply`
