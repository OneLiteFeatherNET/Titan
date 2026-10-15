## ADDED Requirements

### Requirement: Modus-Einträge erklären die Klicks
Jeder Modus-Eintrag des Navigators MUSS eine zusätzliche Lore-Zeile mit seinen Klickaktionen zeigen. Der Linksklick wird als „Server wählen“ beschrieben, wenn das Ziel eine Serverauswahl hat und gerade auf mehreren laufenden Servern läuft, weil der Klick dann die Auswahl öffnet. Sonst wird er als „verbinden“ beschrieben, weil der Klick direkt verbindet. Der Rechtsklick wird nur beschrieben, wenn das Ziel ein Portal in der Lobby hat. Der Spawn-Eintrag DARF KEINE solche Zeile zeigen.

#### Scenario: Ziel mit Serverauswahl auf mehreren Servern und Portal
- **WHEN** ein Spieler den Navigator öffnet, ElytraRace auf mehr als einem Server läuft und für ElytraRace ein Portal in der Lobby existiert
- **THEN** zeigt der Eintrag ElytraRace die Zeile „Left click: choose a server · Right click: walk to portal“

#### Scenario: Ziel mit Serverauswahl auf einem Server
- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nur auf einem Server läuft
- **THEN** zeigt der Eintrag ElytraRace die Zeile „Left click: connect“, weil der Klick direkt verbindet

#### Scenario: Ziel mit Serverauswahl ohne laufenden Server
- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nirgends läuft
- **THEN** zeigt der Eintrag ElytraRace die Zeile „Left click: connect“

#### Scenario: Ziel ohne Portal zeigt nur den Linksklick
- **WHEN** ein Spieler den Navigator öffnet und für Survival kein Portal existiert
- **THEN** zeigt der Eintrag Survival die Zeile „Left click: connect“ und keine Rechtsklick-Angabe

#### Scenario: Ziel ohne Serverauswahl mit Portal
- **WHEN** ein Spieler den Navigator öffnet und für Survival ein Portal existiert
- **THEN** zeigt der Eintrag Survival die Zeile „Left click: connect · Right click: walk to portal“

#### Scenario: Portal seit dem letzten Zyklus hinzugekommen
- **WHEN** ein Portal für Survival nach dem letzten Aktualisierungszyklus des Navigators angelegt wird und ein Spieler den Navigator danach öffnet, bevor der nächste Zyklus läuft
- **THEN** zeigt der Eintrag Survival sofort die Rechtsklick-Angabe „Right click: walk to portal“

#### Scenario: Spawn ohne Hinweis
- **WHEN** ein Spieler den Navigator öffnet
- **THEN** hat der Spawn-Eintrag keine Lore-Zeile mit Klickhinweisen

#### Scenario: Portal wird entfernt
- **WHEN** das Portal eines Ziels entfernt wird und danach der nächste Aktualisierungszyklus des Navigators läuft
- **THEN** verschwindet die Rechtsklick-Angabe aus dem Eintrag, und der Linksklick-Hinweis bleibt

### Requirement: Klickhinweis ist übersetzt
Der Klickhinweis MUSS in Deutsch oder Englisch erscheinen, je nach Sprache des Spielers, und DARF nie leer sein. Unbekannte Sprachen MÜSSEN Englisch zeigen. Die Zeile mit der Spielerzahl bleibt unverändert und steht vor dem Klickhinweis.

#### Scenario: Deutsche Texte
- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt der Hinweis den deutschen Text „Linksklick: Server wählen · Rechtsklick: zum Portal laufen“ und ist nicht leer

#### Scenario: Unbekannte Sprache
- **WHEN** ein Spieler mit einer Sprache ohne Bundle den Navigator öffnet
- **THEN** zeigt der Hinweis den englischen Text

#### Scenario: Spielerzahl bleibt unverändert
- **WHEN** die Spielerzahl eines Ziels bekannt ist
- **THEN** steht die Zeile „<Anzahl> online“ unverändert vor dem Klickhinweis
