# Spec Delta

## MODIFIED Requirements

### Requirement: Je Betriebsumgebung eine startbare Variante
Titan MUSS für jede Betriebsumgebung als eigene, allein startbare Variante gebaut werden: `cloudnet` für den Produktionsbetrieb und `local` für die Entwicklung. Jede Variante MUSS sich ohne weitere Dateien außer Welten und optionaler Konfiguration mit `java -jar` starten lassen. Die Variante `cloudnet` MUSS zusätzlich mit einem AOT-Cache ausgeliefert werden, der zu genau diesem Build passt.

#### Scenario: Produktionsvariante startet
- **WHEN** ein Betreiber das Jar der Variante `cloudnet` mit dem zugehörigen AOT-Cache startet
- **THEN** startet die Lobby, und der AOT-Cache wird angenommen

#### Scenario: Entwicklungsvariante startet
- **WHEN** ein Entwickler das Jar der Variante `local` ohne CloudNet startet
- **THEN** startet die Lobby mit denselben Funktionen wie die Variante `cloudnet`, außer denen einer Column, die nur `cloudnet` enthält

### Requirement: Varianten verhalten sich wie die bisherige Lobby
Solange sich die Varianten nur im Namen unterscheiden, MÜSSEN beide Varianten dieselben Columns enthalten und sich für Spieler und Betreiber genauso verhalten wie die bisherige einzelne Lobby: gleiche Features, gleiche Konfigurationsschlüssel, gleiche Befehle, gleiche Texte. Eine Column, die einen Dienst-Supervisor zum Neustart braucht, wie `season`, ist nur in `cloudnet` enthalten; die Variante `local` DARF sie auslassen, und ihre Konfigurationsschlüssel gelten dort nicht.

#### Scenario: Gleiches Verhalten nach dem Umbau
- **WHEN** ein Spieler die Lobby der Variante `cloudnet` betritt, den Navigator nutzt, sitzt, kitzelt und mit der Elytra fliegt
- **THEN** verhält sich jede Funktion wie vor dem Umbau

#### Scenario: Bestehende Konfiguration gilt weiter
- **WHEN** ein Betreiber seine bisherige `application.yaml` unverändert neben das Jar der Variante legt
- **THEN** übernimmt die Lobby alle Werte daraus wie vorher

#### Scenario: Saison-Column nur in der Produktionsvariante
- **WHEN** die Variante `local` mit einer `application.yaml` startet, die `seasons.*` enthält
- **THEN** lädt sie keine Saison-Column, wählt die Standardwelt und stoppt nie wegen einer Saison, während `cloudnet` die Column enthält und ihr Start sie als erwartete Column prüft
