# Spec Delta

## ADDED Requirements

### Requirement: Serverauswahl blättert durch alle Dienste
_The server list MUST show every running service on pages of at most 45 entries, with the navigation row at the bottom._

Die Serverauswahl MUSS jeden laufenden Dienst des Ziels zeigen, auf Seiten zu höchstens 45 Einträgen. Die unterste Reihe ist immer die Navigationszeile mit dem Sortier-Item. Ab zwei Seiten zeigt sie den Vorige-Pfeil, die Anzeige „Seite x/y“ und den Nächste-Pfeil; ein Pfeil erscheint nur, wenn es in seine Richtung eine Seite gibt.

#### Scenario: Wenige Dienste ohne Seiten
- **WHEN** sechs Dienste eines Minispiels laufen und ein Spieler die Serverauswahl öffnet
- **THEN** zeigt das Inventar eine Reihe Einträge und darunter die Navigationszeile mit dem Sortier-Item
- **AND** es erscheinen keine Pfeile und keine Seitenanzeige

#### Scenario: Mehr Dienste als eine Seite fasst
- **WHEN** 100 Dienste laufen und ein Spieler die Serverauswahl öffnet
- **THEN** zeigt Seite 1 die ersten 45 Einträge der aktiven Ordnung, die Anzeige „Seite 1/3“ und einen Pfeil nach rechts, aber keinen nach links

#### Scenario: Seite zurück
- **WHEN** der Spieler auf Seite 2 den Pfeil nach links anklickt
- **THEN** zeigt die Serverauswahl Seite 1 wieder, mit der aktiven Ordnung

#### Scenario: Letzte Seite
- **WHEN** der Spieler auf der letzten Seite steht
- **THEN** fehlt der Pfeil nach rechts

#### Scenario: Kein Dienst geht verloren
- **WHEN** 200 Dienste laufen und der Spieler alle fünf Seiten nacheinander aufruft
- **THEN** erscheint jeder der 200 Dienste genau einmal

#### Scenario: Seite verschwindet beim Neulesen
- **WHEN** der Spieler auf Seite 2 von 2 den Pfeil nach rechts anklickt, die Dienste inzwischen auf eine Seite geschrumpft sind
- **THEN** zeigt die Serverauswahl die einzige vorhandene Seite, ohne Fehler

#### Scenario: Mehr als 54 Dienste werden nicht abgeschnitten
- **WHEN** 60 Dienste laufen und der Spieler auf Seite 2 wechselt
- **THEN** zeigt Seite 2 die restlichen 15 Dienste und die Anzeige „Seite 2/2“

### Requirement: Sortier-Item schaltet die Ordnung weiter
_The sort item MUST cycle through four orders on each named click, start at name and return to page one on every change._

Die Navigationszeile der Serverauswahl MUSS ein Sortier-Item haben. Ein benannter Klick darauf schaltet die Ordnung weiter: Name, Meiste Spieler, Freie Plätze, Status, danach wieder Name. Beim Öffnen ist Name aktiv. Nach jedem Ordnungswechsel MUSS die Serverauswahl auf Seite 1 springen. Die Lore des Sortier-Items MUSS alle vier Ordnungen nennen und die aktive hervorheben.

#### Scenario: Standardordnung
- **WHEN** die Dienste `Lobby-2`, `Lobby-1` und `Lobby-3` laufen und ein Spieler die Serverauswahl öffnet
- **THEN** stehen die Einträge in der Reihenfolge `Lobby-1`, `Lobby-2`, `Lobby-3`

### Requirement: Ordnungen sind eindeutig festgelegt
_Each order MUST be fully defined, and ties MUST be broken by the service name ascending._

Die Ordnungen sind: **Name** (Dienstname aufsteigend), **Meiste Spieler** (Spielerzahl absteigend), **Freie Plätze** (Zahl der freien Plätze absteigend; Dienste ohne gemeldetes Limit stehen am Ende) und **Status** (beitretbare Dienste, dann nicht bereite, dann volle). Gleichstand entscheidet in jeder Ordnung der Dienstname aufsteigend (A–Z).

#### Scenario: Meiste Spieler
- **WHEN** `A` 12 von 100, `B` 40 von 100 und `C` 0 von 100 Spielern hat und der Spieler auf Meiste Spieler wechselt
- **THEN** stehen die Einträge in der Reihenfolge `B`, `A`, `C`

#### Scenario: Gleichstand bei Spielerzahl
- **WHEN** `ElytraRace-2` und `ElytraRace-1` jeweils 12 Spieler haben und die Ordnung Meiste Spieler aktiv ist
- **THEN** steht `ElytraRace-1` vor `ElytraRace-2`

#### Scenario: Freie Plätze
- **WHEN** `A` 12 von 100, `B` 40 von 50 und `C` ohne gemeldetes Limit hat und der Spieler auf Freie Plätze wechselt
- **THEN** stehen die Einträge in der Reihenfolge `A`, `B`, `C`, und `C` steht am Ende

#### Scenario: Status
- **WHEN** `A` beitretbar, `B` voll (10 von 10) und `C` nicht bereit ist und der Spieler auf Status wechselt
- **THEN** stehen die Einträge in der Reihenfolge `A`, `C`, `B`

#### Scenario: Ein Klick nach dem anderen durch alle Ordnungen
- **WHEN** der Spieler viermal auf das Sortier-Item klickt
- **THEN** ist wieder Name aktiv, und die Reihenfolge der Einträge ist die der Standardordnung

#### Scenario: Wechsel springt auf Seite 1
- **WHEN** der Spieler auf Seite 2 steht und das Sortier-Item anklickt
- **THEN** zeigt die Serverauswahl Seite 1 in der neuen Ordnung

#### Scenario: Lore zeigt die aktive Ordnung
- **WHEN** Status aktiv ist
- **THEN** nennt die Lore des Sortier-Items alle vier Ordnungen und hebt Status als aktive Ordnung hervor

#### Scenario: Erneutes Öffnen beginnt mit Name
- **WHEN** der Spieler die Ordnung auf Status gestellt hat, die Serverauswahl schließt und sie danach erneut öffnet
- **THEN** ist Name aktiv und die Liste zeigt Seite 1

### Requirement: Serverauswahl liest beim Öffnen und bei jeder Interaktion neu
_The server list MUST read the services on open, on every page or order change and on every entry click, and MUST NOT refresh itself in between._

Die Serverauswahl liest die laufenden Dienste beim Öffnen, bei jedem Seitenwechsel, bei jedem Ordnungswechsel und beim Klick auf einen Eintrag neu. Zwischen diesen Ereignissen aktualisiert sie sich nicht von selbst. Der Klick auf einen Eintrag prüft den Dienst weiterhin vor dem Wechsel.

#### Scenario: Stand bleibt bis zum nächsten Klick
- **WHEN** ein Dienst nach dem Öffnen der Serverauswahl Plätze verliert, der Spieler aber keine Taste drückt
- **THEN** zeigt die Liste bis zum nächsten Klick den Stand vom Öffnen

#### Scenario: Seitenwechsel liest neu
- **WHEN** ein Dienst nach dem Öffnen gestoppt wurde und der Spieler auf die nächste Seite wechselt
- **THEN** erscheint der gestoppte Dienst auf der neuen Seite nicht mehr

### Requirement: Ordnung und Seite gelten nur für das offene Inventar
_The server list MUST keep no order or page per player; the open inventory is the only state, and a player who leaves leaves nothing behind._

Die Serverauswahl DARF Ordnung und Seite eines Spielers nicht speichern. Die Ordnung und die Seite, die ein Spieler sieht, sind die des Inventars, das er offen hat. Ändert ein Spieler die Ordnung, bleibt die Ansicht eines anderen Spielers unverändert.

#### Scenario: Zwei Spieler mit verschiedenen Ordnungen
- **WHEN** Spieler A die Ordnung auf Status stellt und Spieler B danach die Serverauswahl öffnet
- **THEN** sieht Spieler B die Ordnung Name auf Seite 1

#### Scenario: Spieler verlässt die Lobby bei offener Serverauswahl
- **WHEN** ein Spieler mit offener Serverauswahl die Lobby verlässt
- **THEN** hält die Serverauswahl keine Daten mehr zu diesem Spieler

## MODIFIED Requirements

### Requirement: Menü-Varianten bleiben acht geteilte Inventare
_The navigator MUST keep one shared inventory per combination of permissions and language, and the server list one per destination, language, order, page and row count; opening, paging and sorting MUST NOT create a new inventory or listener once it exists._

Der Navigator MUSS je Kombination aus Rechten und Sprache genau ein geteiltes Inventar haben, beim Start angelegt, mit dem Titel in dieser Sprache. Die Serverauswahl MUSS je Ziel, Sprache, Ordnung, Seite und Zeilenzahl höchstens ein geteiltes Inventar haben. Ein Serverauswahl-Inventar entsteht, wenn seine Ansicht zum ersten Mal gebraucht wird, und wird danach wiederverwendet. Das Öffnen, Blättern und Sortieren DARF, wenn das Inventar schon existiert, KEIN neues Inventar und keinen neuen Listener erzeugen.

#### Scenario: Titel je Sprache

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt das Inventar den Titel „Navigator“ im Verlauf und die deutschen Zeilen, und ein Spieler mit englischem Client sieht das zweite Inventar mit englischem Titel und englischen Zeilen

#### Scenario: Häufiges Öffnen der Serverauswahl

- **WHEN** ein Spieler die Serverauswahl 20 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Häufiges Blättern und Sortieren

- **WHEN** ein Spieler alle vier Ordnungen einmal durchläuft, danach jede Ordnung ein zweites Mal aufruft und zwischen den Seiten hin- und herblättert
- **THEN** ist die Anzahl angemeldeter Event-Listener nach dem ersten Durchlauf unverändert

#### Scenario: Stoppen entfernt jede geöffnete Serverauswahl

- **WHEN** das Navigator-Modul gestoppt wird, nachdem Serverauswahlen auf Deutsch und Englisch geöffnet wurden
- **THEN** hat keine dieser Serverauswahlen mehr einen Klick-Listener

### Requirement: Nur benannte Klicks wirken
_The navigator and its server list MUST cancel every click, act only on left and right clicks, and apply the same rule to the sort item and the page arrows._

Der Navigator und seine Serverauswahl MÜSSEN jeden Klick abbrechen. Gewirkt wird nur bei Links- und Rechtsklick, mit oder ohne Shift. Jeder andere Klicktyp (Zahlentasten, Q, F, Mittelklick, Doppelklick, Ziehen) DARF weder schnell beitreten, eine Serverauswahl öffnen, verbinden, zum Portal laufen, die Ordnung wechseln noch eine Seite wechseln; das Inventar MUSS dabei offen bleiben. Das gilt für Minispiele, Server, die Einträge der Serverauswahl, das Sortier-Item und die Pfeile.

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
