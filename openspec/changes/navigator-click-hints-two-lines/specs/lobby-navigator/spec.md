## MODIFIED Requirements

### Requirement: Modus-Einträge erklären die Klicks
Jeder Modus-Eintrag des Navigators MUSS seine Klickaktionen als Lore-Zeilen zeigen, und zwar den Linksklick und den Rechtsklick auf je einer eigenen Zeile, nach der Zeile mit der Spielerzahl. Die Linksklick-Zeile steht immer und beschreibt den Klick als „Server wählen“, wenn das Ziel eine Serverauswahl hat und der Snapshot der letzten Aktualisierung mehrere laufende Server zeigt, weil der Klick dann die Auswahl öffnet. Sonst beschreibt sie den Klick als „verbinden“. Die Rechtsklick-Zeile („zum Portal laufen“) steht nur, wenn das Ziel ein Portal in der Lobby hat, und sie steht auf einer eigenen Zeile, nicht hinter einem Trennzeichen. Der Serverauswahl-Teil folgt dem Snapshot der letzten abgeschlossenen Aktualisierung; eine langsame Lesung verlängert diesen Rückstand, weil Perioden von `navigator.refreshSeconds`, in denen die vorige Lesung noch läuft, übersprungen werden. Der Klick selbst liest die Dienste dagegen neu. Der Spawn-Eintrag DARF KEINE solche Zeile zeigen.

#### Scenario: Ziel mit Serverauswahl auf mehreren Servern und Portal
- **WHEN** ein Spieler den Navigator öffnet, ElytraRace auf mehr als einem Server läuft und für ElytraRace ein Portal in der Lobby existiert
- **THEN** zeigt der Eintrag ElytraRace die Zeile „Left click: choose a server“ und darunter auf einer eigenen Zeile „Right click: walk to portal“

#### Scenario: Ziel mit Serverauswahl auf einem Server
- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nur auf einem Server läuft
- **THEN** zeigt der Eintrag ElytraRace die Zeile „Left click: connect“, weil der Klick direkt verbindet

#### Scenario: Ziel mit Serverauswahl ohne laufenden Server
- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nirgends läuft
- **THEN** zeigt der Eintrag ElytraRace die Zeile „Left click: connect“

#### Scenario: Ziel ohne Portal zeigt nur den Linksklick
- **WHEN** ein Spieler den Navigator öffnet und für Survival kein Portal existiert
- **THEN** zeigt der Eintrag Survival die Zeile „Left click: connect“ und keine Rechtsklick-Zeile

#### Scenario: Ziel ohne Serverauswahl mit Portal
- **WHEN** ein Spieler den Navigator öffnet und für Survival ein Portal existiert
- **THEN** zeigt der Eintrag Survival die Zeilen „Left click: connect“ und „Right click: walk to portal“ untereinander

#### Scenario: Serverauswahl folgt dem Snapshot der letzten Aktualisierung
- **WHEN** ElytraRace nach dem letzten Aktualisierungszyklus des Navigators auf einen zweiten Server wechselt und ein Spieler den Navigator vor dem nächsten Zyklus öffnet
- **THEN** zeigt der Eintrag ElytraRace weiter „Left click: connect“, weil der Snapshot nur einen Server kennt, und die Anzahl der Spieler stammt aus demselben Snapshot
- **AND** ein Klick auf ElytraRace liest die Dienste neu und öffnet die Auswahl, wenn dann mehrere Server laufen

#### Scenario: Portal seit dem letzten Zyklus hinzugekommen
- **WHEN** ein Portal für Survival nach dem letzten Aktualisierungszyklus des Navigators angelegt wird und ein Spieler den Navigator danach öffnet, bevor der nächste Zyklus läuft
- **THEN** zeigt der Eintrag Survival sofort die Zeile „Right click: walk to portal“

#### Scenario: Spawn ohne Hinweis
- **WHEN** ein Spieler den Navigator öffnet
- **THEN** hat der Spawn-Eintrag keine Lore-Zeile mit Klickhinweisen

#### Scenario: Portal wird entfernt
- **WHEN** das Portal eines Ziels entfernt wird und danach der nächste Aktualisierungszyklus des Navigators läuft
- **THEN** verschwindet die Rechtsklick-Zeile aus dem Eintrag, und die Linksklick-Zeile bleibt

### Requirement: Klickhinweis ist übersetzt
Jede Klickhinweis-Zeile MUSS in Deutsch oder Englisch erscheinen, je nach Sprache des Spielers, und DARF nie leer sein. Unbekannte Sprachen MÜSSEN Englisch zeigen. Die Zeile mit der Spielerzahl bleibt unverändert und steht vor den Klickhinweis-Zeilen.

#### Scenario: Deutsche Texte
- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt der Eintrag die Zeilen „Linksklick: Server wählen“ und „Rechtsklick: zum Portal laufen“ und keine davon ist leer

#### Scenario: Unbekannte Sprache
- **WHEN** ein Spieler mit einer Sprache ohne Bundle den Navigator öffnet
- **THEN** zeigt der Hinweis den englischen Text

#### Scenario: Spielerzahl bleibt unverändert
- **WHEN** die Spielerzahl eines Ziels bekannt ist
- **THEN** steht die Zeile „<Anzahl> online“ unverändert vor den Klickhinweis-Zeilen
