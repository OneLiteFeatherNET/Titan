# Spec Delta

## ADDED Requirements

### Requirement: Saison-Prüfungen sind sichtbar
Die Lobby MUSS für jede periodische Saison-Prüfung einen kurzen Span `season.check` mit der aktuellen Saison, der gewünschten Saison, dem Ergebnis (`unchanged`, `pending_restart`, `restart_requested`, `unresolvable`) und der Zahl der Spieler online erzeugen und `titan.season.checks{outcome}` erhöhen. Beim Anfordern eines Neustarts MUSS `titan.season.restarts_requested` steigen und der Span ein Event `season.stop_requested` tragen.

#### Scenario: Neustart wird angefordert
- **WHEN** die Prüfung eine andere gewünschte Saison findet und der Neustart nach der Politik erlaubt ist
- **THEN** hat `season.check` `season.outcome=restart_requested` und das Event `season.stop_requested`, und `titan.season.restarts_requested` steht auf eins

#### Scenario: Nichts zu tun
- **WHEN** die Saison zur gewünschten passt
- **THEN** hat `season.check` `season.outcome=unchanged`

### Requirement: Tageszeit und Schutz sind messbar, ohne Spans
Die Lobby MUSS `titan.daytime.updates` bei jeder Aktualisierung und `titan.daytime.config_rejected{reason}` bei einer abgelehnten Konfiguration erhöhen. Sie MUSS `titan.protection.denied{event}` bei jedem von einer Schutzregel abgebrochenen Event erhöhen. Für beides DARF KEIN Span entstehen.

#### Scenario: Abgebrochener Item-Aufheber
- **WHEN** ein Spieler ein Item aufheben will und die Schutzregel das Event abbricht
- **THEN** steigt `titan.protection.denied{event=pickup}` um eins, das Event ist abgebrochen, und es entsteht kein Span

#### Scenario: Abgelehnte Tageszeit-Konfiguration
- **WHEN** die Tageszeit-Konfiguration einen ungültigen Wert hat
- **THEN** steigt `titan.daytime.config_rejected{reason}` und die Lobby läuft weiter
