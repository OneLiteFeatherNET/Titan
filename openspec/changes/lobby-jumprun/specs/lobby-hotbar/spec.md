# Spec Delta

## MODIFIED Requirements

### Requirement: Module melden Items mit festem Platz an
Ein Feature MUSS ein Lobby-Item zusammen mit seinem Platz bereitstellen können, entweder als Hotbar-Slot oder als Ausrüstungsplatz, ohne die Plattform zu ändern. Beim Betreten der Lobby und nach einem Respawn MUSS der Spieler genau die bereitgestellten Items auf ihren Plätzen erhalten. Sein Inventar enthält danach sonst nichts.

#### Scenario: Standardausstattung
- **WHEN** ein Spieler die Lobby betritt
- **THEN** liegt das Jump-and-Run-Item in Hotbar-Slot 0 und der Navigator (Feder) in Hotbar-Slot 4, er trägt eine unzerstörbare Elytra auf dem Brustplatz, und sonst ist sein Inventar leer

#### Scenario: Ausstattung nach Respawn
- **WHEN** ein Spieler stirbt und respawnt
- **THEN** hat er wieder genau die Standardausstattung
