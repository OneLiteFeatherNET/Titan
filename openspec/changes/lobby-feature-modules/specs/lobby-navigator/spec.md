# Spec Delta

## Purpose

Legt fest, wie die Ziele des Lobby-Navigators entstehen: aus der Konfiguration oder von anderen Modulen beigesteuert. Neue Spielmodi sollen so ohne Codeänderung am Navigator erscheinen, und das Öffnen soll keine Ressourcen anhäufen.

## ADDED Requirements

### Requirement: Navigator-Ziele kommen aus der Konfiguration
Der Navigator MUSS seine Ziele aus seinem Konfigurationsabschnitt lesen. Jedes Ziel besteht aus Symbol, angezeigtem Namen, Platz im Navigator und Weiterleitungsziel. Ein neues Ziel MUSS sich durch einen Konfigurationseintrag hinzufügen lassen, ohne Codeänderung.

#### Scenario: Standardziele
- **WHEN** ein Spieler ohne angepasste Konfiguration den Navigator öffnet
- **THEN** sieht er eine Reihe mit ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5 und Creative auf Platz 8, die übrigen Plätze sind mit grauen Glasscheiben gefüllt

#### Scenario: Zusätzliches Ziel per Konfiguration
- **WHEN** der Betreiber im Abschnitt `"navigator"` ein Ziel „Parkour“ auf Platz 2 ergänzt und die Lobby neu startet
- **THEN** zeigt der Navigator „Parkour“ auf Platz 2 an

### Requirement: Module können Navigator-Ziele beisteuern
Ein Modul MUSS dem Navigator eigene Ziele hinzufügen können, ohne den Navigator zu ändern. Wird das Modul abgeschaltet, MUSS sein Ziel aus dem Navigator verschwinden.

#### Scenario: Ziel eines Moduls
- **WHEN** ein Modul „teaser“ ein Ziel „Voyager“ beisteuert
- **THEN** erscheint „Voyager“ im Navigator

#### Scenario: Modul abgeschaltet
- **WHEN** das Modul „teaser“ abgeschaltet wird
- **THEN** erscheint „Voyager“ beim nächsten Öffnen nicht mehr

### Requirement: Doppelt belegte Navigator-Plätze werden beim Start erkannt
Belegen zwei Ziele denselben Platz, MUSS die Lobby den Start abbrechen und beide Ziele sowie ihre Herkunft nennen.

#### Scenario: Zwei Ziele auf Platz 4
- **WHEN** die Konfiguration „Survival“ und ein Modul „Voyager“ auf Platz 4 legen
- **THEN** startet die Lobby nicht und meldet den Konflikt auf Platz 4

### Requirement: Auswahl eines Ziels leitet weiter
Klickt ein Spieler ein Ziel an, MUSS die Lobby ihn an das konfigurierte Weiterleitungsziel übergeben und den Navigator schließen. Der Klick DARF NICHT das Symbol ins Spielerinventar verschieben.

#### Scenario: Survival wählen
- **WHEN** ein Spieler im Navigator auf Survival klickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen, und das Symbol bleibt im Navigator

#### Scenario: Weiterleitung ohne Cloud
- **WHEN** die Lobby ohne CloudNet läuft und ein Spieler ein Ziel anklickt
- **THEN** passiert keine Weiterleitung, und es entsteht kein Fehler

### Requirement: Öffnen des Navigators häuft nichts an
Das Öffnen des Navigators DARF NICHT dazu führen, dass pro Spieler oder pro Öffnung zusätzliche Event-Listener oder dauerhaft gehaltene Objekte entstehen. Nach dem Verlassen der Lobby DARF der Navigator keine Referenz mehr auf den Spieler halten.

#### Scenario: Häufiges Öffnen
- **WHEN** ein Spieler den Navigator 50 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Spieler verlässt die Lobby
- **WHEN** ein Spieler, der den Navigator geöffnet hatte, die Lobby verlässt
- **THEN** hält der Navigator keine Daten mehr zu diesem Spieler
