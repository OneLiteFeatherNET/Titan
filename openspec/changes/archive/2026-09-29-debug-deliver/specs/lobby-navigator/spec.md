# Spec Delta

## MODIFIED Requirements

### Requirement: Auswahl eines Ziels leitet weiter
Klickt ein Spieler ein Ziel an, MUSS die Lobby ihn an das konfigurierte Weiterleitungsziel übergeben und den Navigator schließen. Der Klick DARF NICHT das Symbol ins Spielerinventar verschieben.

#### Scenario: Survival wählen
- **WHEN** ein Spieler im Navigator auf Survival klickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen, und das Symbol bleibt im Navigator

#### Scenario: Weiterleitung ohne Cloud
- **WHEN** die Lobby ohne CloudNet läuft und ein Spieler ein Ziel anklickt
- **THEN** passiert keine Weiterleitung, es entsteht kein Fehler, der Spieler erhält im Chat eine Nachricht, die Art (Task oder Server) und Ziel unverändert nennt, und die Lobby loggt dieselbe Information für den Betreiber
