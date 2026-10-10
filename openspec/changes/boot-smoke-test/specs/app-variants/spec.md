# Spec Delta

## ADDED Requirements

### Requirement: Jede Variante startet aus ihrem ausgelieferten Jar mit echter Konfiguration
Jede Variante MUSS aus dem Jar, das ausgeliefert wird, mit einer echten `application.yaml` und einer Welt im Arbeitsverzeichnis fehlerfrei starten: Die Lobby meldet ihren Start, protokolliert dabei keinen ERROR und beendet sich bei einem geordneten Stopp mit Exit-Code 0. Dieser Start MUSS Teil des regulären Builds (`check`) sein und DARF weder Konfigurationsleser, Weltwahl noch Permission-Dienst durch Ersatzobjekte ersetzen.

#### Scenario: Produktionsvariante bootet mit Betreiberdatei
- **WHEN** das Jar der Variante `cloudnet` in einem leeren Arbeitsverzeichnis mit einer Welt unter `worlds/` und einer echten `application.yaml` startet und geordnet stoppt
- **THEN** meldet die Lobby den Start der Variante mit allen erwarteten Columns, der Prozess endet mit Exit-Code 0, und das Log enthält keine ERROR-Zeile

#### Scenario: Entwicklungsvariante bootet mit Betreiberdatei
- **WHEN** das Jar der Variante `local` unter denselben Bedingungen startet und geordnet stoppt
- **THEN** meldet die Lobby den Start der Variante, der Prozess endet mit Exit-Code 0, und das Log enthält keine ERROR-Zeile

#### Scenario: Ohne Startmeldung schlägt der Test mit dem Log fehl
- **WHEN** die Lobby innerhalb der Zeitgrenze keine Startmeldung ausgibt oder mit einem anderen Exit-Code als 0 endet
- **THEN** schlägt der Build fehl, und die Fehlermeldung enthält das bis dahin ausgegebene Log
