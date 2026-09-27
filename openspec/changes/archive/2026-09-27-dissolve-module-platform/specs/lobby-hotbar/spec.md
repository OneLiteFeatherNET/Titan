# Spec Delta

## MODIFIED Requirements

### Requirement: Module melden Items mit festem Platz an
Ein Feature MUSS ein Lobby-Item zusammen mit seinem Platz bereitstellen können, entweder als Hotbar-Slot oder als Ausrüstungsplatz, ohne die Plattform zu ändern. Beim Betreten der Lobby und nach einem Respawn MUSS der Spieler genau die bereitgestellten Items auf ihren Plätzen erhalten. Sein Inventar enthält danach sonst nichts.

#### Scenario: Standardausstattung
- **WHEN** ein Spieler die Lobby betritt
- **THEN** liegt der Navigator (Feder) in Hotbar-Slot 4, er trägt eine unzerstörbare Elytra auf dem Brustplatz, und sonst ist sein Inventar leer

#### Scenario: Ausstattung nach Respawn
- **WHEN** ein Spieler stirbt und respawnt
- **THEN** hat er wieder genau die Standardausstattung

### Requirement: Platzkonflikte werden beim Start erkannt
Stellen zwei Items denselben Platz oder denselben Schlüssel bereit, MUSS die Lobby den Start abbrechen. Die Fehlermeldung MUSS den Platz bzw. den Schlüssel und beide Items nennen.

#### Scenario: Zwei Module wollen Slot 4
- **WHEN** die Navigator-Feder und ein Item „friends“ beide Hotbar-Slot 4 beanspruchen
- **THEN** startet die Lobby nicht und meldet den Konflikt um Slot 4 zwischen beiden Items

#### Scenario: Doppelter Schlüssel
- **WHEN** zwei Items denselben Schlüssel `titan:navigator` tragen
- **THEN** startet die Lobby nicht und meldet den doppelten Schlüssel mit beiden Items

#### Scenario: Items ohne festen Platz
- **WHEN** zwei Items ohne festen Platz bereitgestellt werden
- **THEN** ist das kein Konflikt
