# Spec Delta

## MODIFIED Requirements

### Requirement: Abschalten hinterlässt keine Reste
Alles, was ein Modul über seinen Kontext angemeldet hat (Event-Listener, geplante Aufgaben, Befehle, Hotbar-Items), MUSS nach dem Abschalten des Moduls vollständig entfernt sein. Das Modul muss sich dafür nichts merken.

#### Scenario: Listener sind nach dem Abschalten entfernt
- **WHEN** ein Modul beim Start Listener für drei Event-Typen angemeldet hat und danach abgeschaltet wird
- **THEN** löst keines dieser Events mehr Code des Moduls aus

#### Scenario: Wiederkehrende Aufgaben enden
- **WHEN** ein Modul eine jede Sekunde wiederkehrende Aufgabe geplant hat und abgeschaltet wird
- **THEN** läuft die Aufgabe danach nicht mehr

#### Scenario: Befehle verschwinden
- **WHEN** ein Modul einen Befehl angemeldet hat und abgeschaltet wird
- **THEN** ist der Befehl nicht mehr ausführbar
