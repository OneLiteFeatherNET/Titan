# server-bootstrap Specification

## Purpose
Legt fest, wie Titan den Minestom-Server startet: über die Bootstrap-Plattform seiner Variante, mit Extension-Loader nur dort, wo er gebraucht wird, und mit gleichem Verhalten bei Velocity-Forwarding und Bind-Adresse.

## Requirements

### Requirement: Extensions lädt nur eine Variante mit Extension-Plattform
Enthält die Variante die Extension-Plattform, MUSS die Lobby den Ordner `extensions/` laden, bevor sie Spieler annimmt. Enthält sie die Plattform nicht, DARF die Lobby keinen Extension-Loader starten und den Ordner `extensions/` NICHT laden, auch wenn er existiert. Die Variante `cloudnet` MUSS die Extension-Plattform enthalten, die Variante `local` DARF sie NICHT enthalten.

#### Scenario: Produktionsvariante lädt Extensions
- **WHEN** die Variante `cloudnet` startet und in `extensions/` die CloudNet-Bridge und die Titan-Bridge liegen
- **THEN** sind beide Extensions geladen, bevor sich ein Spieler verbinden kann

#### Scenario: Entwicklungsvariante ignoriert Extensions
- **WHEN** die Variante `local` startet und ein Ordner `extensions/` mit Jars existiert
- **THEN** startet die Lobby, und keine dieser Extensions ist geladen

### Requirement: Features starten erst nach dem Server
Die Lobby MUSS den Server vollständig initialisieren, bevor irgendein Feature startet. Das MUSS in jeder Variante gelten.

#### Scenario: Start mit allen Features
- **WHEN** eine Variante mit allen Features startet
- **THEN** startet jedes Feature ohne Fehler, und erst danach nimmt der Server Verbindungen an

### Requirement: Genau eine Bootstrap-Plattform ist aktiv
Die Lobby MUSS beim Start genau einen Weg nutzen, den Server zu starten. Ist keine Bootstrap-Plattform eingebunden, MUSS sie einfaches Minestom ohne Extensions verwenden. Sind mehrere eingebunden, DARF die Lobby NICHT starten, und die Fehlermeldung MUSS alle nennen. Das Start-Log MUSS nennen, welcher Weg aktiv ist.

#### Scenario: Keine Plattform
- **WHEN** die Lobby ohne Bootstrap-Plattform startet
- **THEN** startet sie mit einfachem Minestom, und das Start-Log nennt diesen Weg

#### Scenario: Zwei Plattformen
- **WHEN** zwei Bootstrap-Plattformen eingebunden sind
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt beide

### Requirement: Forwarding und Bind-Adresse verhalten sich wie bisher
In jeder Variante MUSS die Lobby Velocity-Modern-Forwarding einschalten, wenn `forwarding.secret` im Arbeitsverzeichnis ein nicht leeres Secret enthält oder die System-Property `minestom.velocity.secret` gesetzt ist; die Datei hat Vorrang. Die Bind-Adresse MUSS aus den System-Properties `service.bind.host` und `service.bind.port` kommen, mit `localhost` und `25565` als Standard.

#### Scenario: Secret aus Datei
- **WHEN** `forwarding.secret` ein Secret enthält und eine Variante startet
- **THEN** nimmt die Lobby nur Verbindungen über Velocity-Modern-Forwarding mit diesem Secret an

#### Scenario: Bind-Adresse von CloudNet
- **WHEN** CloudNet `-Dservice.bind.host=0.0.0.0 -Dservice.bind.port=40001` setzt
- **THEN** lauscht die Lobby auf `0.0.0.0:40001`

#### Scenario: Ohne Angaben
- **WHEN** weder Secret noch Bind-Properties gesetzt sind
- **THEN** lauscht die Lobby ohne Forwarding auf `localhost:25565`
