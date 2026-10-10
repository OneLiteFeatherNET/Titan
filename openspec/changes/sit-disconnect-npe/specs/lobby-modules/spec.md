# Spec Delta

## ADDED Requirements

### Requirement: Verlassen der Lobby löst einen sitzenden Spieler ohne Fehler
Verlässt ein Spieler die Lobby (Disconnect), MUSS das Feature „sit“ seinen Sitz freigeben, ohne eine Ausnahme zu werfen. Das MUSS auch für Spieler gelten, die nicht sitzen und für Spieler, die zu diesem Zeitpunkt keiner Instanz mehr angehören. Die Lobby DARF dafür KEINEN Fehler melden.

#### Scenario: Nicht sitzender Spieler ohne Instanz geht
- **WHEN** ein Spieler, der nicht sitzt und keiner Instanz angehört, die Verbindung trennt
- **THEN** gibt es keine Ausnahme und keinen Fehlereintrag des Features „sit“

#### Scenario: Sitzender Spieler geht
- **WHEN** ein sitzender Spieler die Verbindung trennt
- **THEN** ist sein Sitz anschließend entfernt, ohne Ausnahme

#### Scenario: Sitzender Spieler ohne Instanz geht
- **WHEN** ein sitzender Spieler die Verbindung trennt, nachdem er schon aus seiner Instanz entfernt wurde
- **THEN** gibt es keine Ausnahme, seine Sitz-Markierungen sind entfernt, und der Sitz ist nach dem nächsten Tick verschwunden
