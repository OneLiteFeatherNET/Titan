# Spec Delta

## ADDED Requirements

### Requirement: Zurück-Taste führt in den Navigator
_The back button MUST open the navigator menu that matches the player's current permissions and language, through the same path as the feather._

Die Navigationszeile der Serverauswahl MUSS auf Navigationsstart + 0 eine Zurück-Taste haben, auf jeder Seite und auch bei leerer Liste. Ein benannter Klick öffnet den Navigator wie das Feder-Item, mit den Rechten und der Sprache des Spielers im Moment des Klicks.

#### Scenario: Zurück von der ersten Seite

- **WHEN** ein Spieler mit dem Recht `titan.navigator.slender` die Serverauswahl von ElytraRace auf Seite 1 offen hat und die Zurück-Taste in Slot 45 anklickt
- **THEN** zeigt sein Inventar den Navigator mit dem Eintrag Slender, also das Menü seiner Rechte

#### Scenario: Zurück von der zweiten Seite

- **WHEN** der Spieler auf Seite 2 einer Serverauswahl mit 100 Diensten die Zurück-Taste anklickt
- **THEN** zeigt sein Inventar den Navigator, nicht Seite 1 der Serverauswahl

#### Scenario: Zurück bei leerer Liste

- **WHEN** die Serverauswahl geöffnet ist, während das Ziel nirgends läuft, und der Spieler die Zurück-Taste in Slot 9 anklickt
- **THEN** zeigt sein Inventar den Navigator

#### Scenario: Entzogenes Recht vor dem Zurück

- **WHEN** ein Spieler die Serverauswahl offen hat, danach das Recht `titan.navigator.slender` verliert und die Zurück-Taste anklickt
- **THEN** zeigt sein Inventar den Navigator ohne den Eintrag Slender

#### Scenario: Zurück auf Deutsch

- **WHEN** ein Spieler mit deutschem Client die Zurück-Taste anklickt
- **THEN** zeigt sein Inventar das deutsche Navigator-Menü mit dem Titel „Navigator“

#### Scenario: Häufiges Zurück ohne neuen Listener

- **WHEN** ein Spieler die Serverauswahl 20 Mal öffnet und jedes Mal über die Zurück-Taste den Navigator wieder öffnet
- **THEN** ist die Anzahl angemeldeter Event-Listener am Navigator-Modul und an der Serverauswahl unverändert

## MODIFIED Requirements

### Requirement: Nur benannte Klicks wirken
_The navigator and its server list MUST cancel every click, act only on left and right clicks, and apply the same rule to the sort item, the page arrows and the back button._

Der Navigator und seine Serverauswahl MÜSSEN jeden Klick abbrechen. Gewirkt wird nur bei Links- und Rechtsklick, mit oder ohne Shift. Jeder andere Klicktyp (Zahlentasten, Q, F, Mittelklick, Doppelklick, Ziehen) DARF weder schnell beitreten, eine Serverauswahl öffnen, verbinden, zum Portal laufen, den Navigator öffnen, die Ordnung wechseln noch eine Seite wechseln; das Inventar MUSS dabei offen bleiben. Das gilt für Minispiele, Server, die Einträge der Serverauswahl, das Sortier-Item, die Pfeile und die Zurück-Taste.

#### Scenario: Zahlentaste auf einem Minispiel

- **WHEN** ein Spieler auf ElytraRace die Zahlentaste drückt
- **THEN** passiert nichts: keine Übergabe, keine Serverauswahl, und der Navigator bleibt offen

#### Scenario: Q auf einem Server

- **WHEN** ein Spieler auf Survival Q drückt
- **THEN** wird niemand übergeben, und der Navigator bleibt offen

#### Scenario: Mittelklick auf einen Eintrag der Serverauswahl

- **WHEN** ein Spieler in der Serverauswahl mit dem Mittelklick auf einen Dienst klickt
- **THEN** wird er nicht übergeben, und die Serverauswahl bleibt offen

#### Scenario: Zahlentaste auf dem Sortier-Item

- **WHEN** ein Spieler in der Serverauswahl auf das Sortier-Item die Zahlentaste drückt
- **THEN** bleibt die Ordnung unverändert, und die Serverauswahl bleibt offen

#### Scenario: Zahlentaste auf der Zurück-Taste

- **WHEN** ein Spieler in der Serverauswahl auf die Zurück-Taste die Zahlentaste drückt
- **THEN** öffnet sich der Navigator nicht, und die Serverauswahl bleibt offen
