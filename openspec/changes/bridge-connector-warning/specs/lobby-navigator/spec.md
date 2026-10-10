# Spec Delta

## ADDED Requirements

### Requirement: Fehlende CloudNet-Anbindung wird gemeldet
Läuft die Lobby als CloudNet-Service, MUSS sie nach dem Laden der Extensions prüfen, ob die Anbindung zum Wechseln des Servers installiert ist, und andernfalls einmal einen Fehler mit dem Hinweis auf die fehlende Bridge-Extension loggen. Kann eine angeforderte Weiterleitung mangels Anbindung nicht ausgeführt werden, MUSS die Lobby das mit Spieler und Ziel loggen. Beides DARF die Lobby NICHT beenden und DARF NICHT zu einem Fehler beim Spieler führen.

#### Scenario: Bridge fehlt beim Start
- **WHEN** die Lobby als CloudNet-Service startet und nach dem Laden der Extensions keine Anbindung installiert ist
- **THEN** loggt sie einmal einen Fehler, der die fehlende Bridge-Extension nennt, und startet trotzdem

#### Scenario: Bridge vorhanden beim Start
- **WHEN** die Lobby als CloudNet-Service startet und die Anbindung installiert ist
- **THEN** loggt sie keine Meldung zur fehlenden Anbindung

#### Scenario: Start ohne CloudNet
- **WHEN** die Lobby ohne CloudNet startet
- **THEN** loggt sie keine Meldung zur fehlenden Anbindung, auch wenn keine installiert ist

#### Scenario: Weiterleitung ohne Anbindung
- **WHEN** die Lobby als CloudNet-Service läuft, keine Anbindung installiert ist und ein Spieler ein Ziel anklickt
- **THEN** loggt die Lobby eine Warnung mit Spielername, UUID, Art (Task oder Server) und Ziel, es entsteht kein Fehler, und der Spieler wird nicht weitergeleitet

#### Scenario: Weiterleitung mit Anbindung
- **WHEN** die Anbindung installiert ist und ein Spieler ein Ziel anklickt
- **THEN** wird die Weiterleitung an die Anbindung übergeben, und die Lobby loggt keine Warnung
