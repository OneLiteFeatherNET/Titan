# Spec Delta

## Purpose

Die englische Kurzzeile unter jeder Anforderung ist für `openspec validate --strict` nötig (RFC 2119); maßgeblich ist der deutsche Text darunter.

Die Liste der laufenden Lobbys bekommt immer eine Navigationszeile mit einem Schließen-Knopf. Die Zeilenzahl und die Seitenaufteilung ändern sich entsprechend.

## RENAMED Requirements

- FROM: `### Requirement: Seiten bei mehr als 54 Lobbys`
- TO: `### Requirement: Seiten bei mehr als 45 Lobbys`

## MODIFIED Requirements

### Requirement: Größe der Liste
_The menu MUST have as many content rows as its entries need, up to five, plus one navigation row, and MUST close when a refresh changes its row or page count._

Ein Inventar MUSS so viele Inhaltszeilen haben, wie seine Einträge brauchen (mindestens eine, höchstens fünf), und darunter immer die Navigationszeile. Ändert eine Aktualisierung die Zeilen- oder Seitenzahl, MUSS das offene Inventar geschlossen werden. Bleibt die Größe gleich, bleibt es offen und zeigt den neuen Stand.

#### Scenario: Eine Lobby
- **WHEN** der Task eine laufende Lobby hat und ein Spieler das Inventar öffnet
- **THEN** hat das Inventar zwei Zeilen: eine Inhaltszeile und die Navigationszeile

#### Scenario: Zwei Lobbys
- **WHEN** der Task zwei laufende Dienste hat und ein Spieler das Inventar öffnet
- **THEN** hat das Inventar zwei Zeilen: eine Inhaltszeile und die Navigationszeile

#### Scenario: Zwölf Lobbys
- **WHEN** der Task zwölf laufende Dienste hat und ein Spieler das Inventar öffnet
- **THEN** hat das Inventar drei Zeilen: zwei Inhaltszeilen und die Navigationszeile

#### Scenario: Größenänderung bei der Aktualisierung
- **WHEN** die Lobbys von zwei auf zwölf steigen, während ein Spieler das Inventar offen hat
- **THEN** wird das Inventar geschlossen, und beim nächsten Öffnen hat es drei Zeilen

### Requirement: Seiten bei mehr als 45 Lobbys
_More than 45 entries MUST be paged, five content rows and the navigation row on every page._

Mehr als 45 Einträge MÜSSEN auf Seiten zu 45 verteilt werden. Jede Seite hat fünf Inhaltszeilen und die Navigationszeile. Ein Pfeil erscheint nur, wenn es in seine Richtung eine Seite gibt. Ein Klick auf einen Eintrag MUSS den Dienst dieses Eintrags betreffen, auf jeder Seite.

#### Scenario: Mehr als 45 Lobbys
- **WHEN** der Task 100 laufende Dienste hat und ein Spieler das Inventar öffnet
- **THEN** zeigt Seite 1 die ersten 45 Lobbys, einen Pfeil nach rechts, keinen nach links, und die Anzeige „Seite 1/3“

#### Scenario: Seite zurück
- **WHEN** der Spieler auf Seite 2 den Pfeil nach links anklickt
- **THEN** zeigt das Inventar Seite 1 wieder

## ADDED Requirements

### Requirement: Navigationszeile mit Schließen-Knopf
_The bottom row MUST always be a navigation row: the close button at its first slot, the page indicator in its centre and the page arrows at the third and fifth slot._

Die unterste Reihe ist immer die Navigationszeile, auch bei einer Seite. Start + 0 ist der Schließen-Knopf „Schließen“, ein Klick schließt das Inventar. Start + 3 und Start + 5 sind die Pfeile, jeweils nur wenn es in ihre Richtung eine Seite gibt. Start + 4 zeigt „Seite x/y“, nur bei mehr als einer Seite.

#### Scenario: Eine Seite
- **WHEN** der Task drei laufende Dienste hat und ein Spieler das Inventar öffnet
- **THEN** steht der Schließen-Knopf in Slot 9, dem ersten Slot der Navigationszeile
- **AND** es gibt weder Pfeile noch Seitenanzeige

#### Scenario: Schließen-Knopf
- **WHEN** der Spieler auf den Schließen-Knopf klickt
- **THEN** ist das Inventar geschlossen und die Aktualisierung läuft für ihn nicht weiter

#### Scenario: Mehrere Seiten
- **WHEN** der Task 100 laufende Dienste hat und ein Spieler auf Seite 2 steht
- **THEN** steht der Schließen-Knopf in Slot 45, der Vorige-Pfeil in Slot 48, die Anzeige „Seite 2/3“ in Slot 49 und der Nächste-Pfeil in Slot 50

#### Scenario: Letzte Seite
- **WHEN** der Spieler auf der letzten Seite steht
- **THEN** fehlt der Pfeil nach rechts in Slot 50, und der Platz bleibt leer
