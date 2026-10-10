# Spec Delta

## ADDED Requirements

### Requirement: Ein gescheiterter Start hinterlässt keine Reste
Scheitert der Start der Lobby, nachdem die Features gebaut wurden (etwa weil der Variantencheck eine erwartete Column vermisst oder die Startlogs scheitern), MUSS die Lobby alle schon gestarteten Features und Plattformdienste herunterfahren, bevor der Fehler gemeldet wird. Gemeldet MUSS die ursprüngliche Ursache werden; ein Fehler beim Herunterfahren DARF sie NICHT ersetzen.

#### Scenario: Fehlende Column
- **WHEN** die Variante die Column `sit` erwartet, diese nicht geladen wird und der Start deshalb scheitert
- **THEN** sind alle schon gestarteten Beans heruntergefahren (ihr Abschluss ist gelaufen), und der gemeldete Fehler nennt weiterhin `sit`

#### Scenario: Fehler beim Herunterfahren nach einem Startfehler
- **WHEN** der Start scheitert und das Herunterfahren einer Bean dabei selbst eine Ausnahme wirft
- **THEN** ist die gemeldete Ursache der Startfehler, und die Ausnahme des Herunterfahrens hängt als unterdrückte Ausnahme daran

#### Scenario: Erfolgreicher Start
- **WHEN** der Start gelingt
- **THEN** bleibt der Scope offen, bis die Lobby heruntergefahren wird
