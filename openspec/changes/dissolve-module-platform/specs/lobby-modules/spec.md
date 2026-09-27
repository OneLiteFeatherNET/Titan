# Spec Delta

## ADDED Requirements

### Requirement: Features starten vor dem ersten Spieler
Jedes Feature MUSS beim Start der Lobby genau einmal gestartet werden, bevor ein Spieler die Lobby betreten kann. Features DÜRFEN sich beim Start NICHT auf die Reihenfolge anderer Features verlassen.

#### Scenario: Spieler erst nach dem Start
- **WHEN** ein Spieler sich verbindet
- **THEN** sind alle Features bereits gestartet, und jedes genau einmal

#### Scenario: Fehler beim Start eines Features
- **WHEN** ein Feature beim Start eine Ausnahme wirft
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt das Feature

### Requirement: Reihenfolge der Event-Verarbeitung ist festgelegt
Reagieren mehrere Features auf dasselbe Event, MUSS die Reihenfolge, in der sie es erhalten, für jedes Feature fest vorgegeben und eindeutig sein. Sie DARF NICHT von der Startreihenfolge abhängen. Ein automatisierter Test MUSS melden, wenn zwei Features dieselbe Position haben.

#### Scenario: Schutz und Navigator beim selben Klick
- **WHEN** ein Spieler im geöffneten Navigator ein Ziel anklickt, egal in welcher Reihenfolge „protection“ und „navigator“ gestartet wurden
- **THEN** ist der Klick abgebrochen, das Symbol bleibt im Navigator, und die Weiterleitung findet statt

#### Scenario: Doppelte Position
- **WHEN** zwei Features dieselbe Position in der Event-Reihenfolge angeben
- **THEN** schlägt der Build fehl und nennt beide Features

### Requirement: Features trennen sich beim Herunterfahren zuerst von Events
Beim Herunterfahren MUSS jedes Feature von allen Events getrennt und seine geplanten Aufgaben MÜSSEN abgebrochen sein, bevor seine übrige Abschaltlogik läuft.

#### Scenario: Keine Events während des Abschaltens
- **WHEN** die Abschaltlogik eines Features läuft und währenddessen ein Event eintrifft, auf das das Feature gehört hat
- **THEN** reagiert das Feature nicht mehr auf dieses Event

### Requirement: Herunterfahren hinterlässt keine Reste
Alle Event-Listener und geplanten Aufgaben eines Features MÜSSEN nach seinem Herunterfahren vollständig entfernt sein.

#### Scenario: Listener sind nach dem Herunterfahren entfernt
- **WHEN** ein Feature beim Start Listener für drei Event-Typen angemeldet hat und danach heruntergefahren wird
- **THEN** löst keines dieser Events mehr Code des Features aus

#### Scenario: Wiederkehrende Aufgaben enden
- **WHEN** ein Feature eine jeden Tick wiederkehrende Aufgabe geplant hat und heruntergefahren wird
- **THEN** läuft die Aufgabe danach nicht mehr

### Requirement: Ein neues Feature ist nur ein neues Paket
Ein neues Feature MUSS sich allein durch ein neues Paket hinzufügen lassen. Dafür DÜRFEN weder andere Features noch die Plattform noch eine zentrale Liste geändert werden.

#### Scenario: Feature mit Listener und Hotbar-Item
- **WHEN** ein Entwickler ein Feature mit eigenem Config-Abschnitt, einem Event-Listener und einem Hotbar-Item in einem neuen Paket anlegt
- **THEN** startet die Lobby mit dem Feature, und außerhalb des neuen Pakets ist keine Zeile geändert

## MODIFIED Requirements

### Requirement: Module sind voneinander unabhängig
Ein Feature DARF NICHT direkt von einem anderen Feature abhängen. Gemeinsam genutzte Funktionen MÜSSEN über Beans der Plattform oder über gemeinsame Bibliotheken außerhalb der Features laufen. Plattform und gemeinsame Bibliotheken DÜRFEN NICHT von Features abhängen. Ein automatisierter Test MUSS Verstöße im Build melden.

#### Scenario: Verbotene Abhängigkeit zwischen Features
- **WHEN** Code im Feature „tickle“ Code aus dem Feature „sit“ verwendet
- **THEN** schlägt der Build mit einem Hinweis auf die verbotene Abhängigkeit fehl

#### Scenario: Gemeinsame Bibliothek hängt von einem Feature ab
- **WHEN** Code in einer gemeinsamen Bibliothek Code aus einem Feature verwendet
- **THEN** schlägt der Build fehl

### Requirement: Fehler in einem Listener bleiben dem Modul und Spieler zugeordnet
Wirft ein Listener eines Features eine Ausnahme, MUSS die Lobby weiterlaufen. Die Fehlermeldung MUSS das betroffene Feature und, falls vorhanden, den betroffenen Spieler nennen.

#### Scenario: Ausnahme im Listener
- **WHEN** ein Listener des Features „sit“ bei einem Event des Spielers „Alex“ eine Ausnahme wirft
- **THEN** läuft die Lobby weiter und der gemeldete Fehler nennt das Feature „sit“ und den Spieler „Alex“

## REMOVED Requirements

### Requirement: Module starten in Registrierungsreihenfolge
**Reason**: Es gibt keine Modulliste mehr. Features sind voneinander unabhängige Beans, die der DI-Container startet. Relevant ist nur, dass alle vor dem ersten Spieler laufen und in welcher Reihenfolge sie Events verarbeiten. Ersetzt durch „Features starten vor dem ersten Spieler“ und „Reihenfolge der Event-Verarbeitung ist festgelegt“.
**Migration**: `@Priority` am Feature durch die Priorität seines Event-Knotens ersetzen, mit demselben Wert.

### Requirement: Module fahren in umgekehrter Reihenfolge herunter
**Reason**: Ohne Abhängigkeiten zwischen Features hat die Stopp-Reihenfolge keine Bedeutung. Übrig bleibt die Garantie, dass ein Feature vor seiner Abschaltlogik von Events getrennt ist. Ersetzt durch „Features trennen sich beim Herunterfahren zuerst von Events“.
**Migration**: Abschaltlogik aus `LobbyModule#disable()` in eine `@PreDestroy`-Methode verschieben, die zuerst den Event-Knoten abhängt.

### Requirement: Abschalten hinterlässt keine Reste
**Reason**: Der Befehls-Andockpunkt entfällt, weil ihn kein Feature nutzt. Hotbar-Items sind Beans und leben so lange wie die Lobby. Ersetzt durch „Herunterfahren hinterlässt keine Reste“ für Listener und Aufgaben.
**Migration**: Befehle werden wie `StopCommand` direkt beim `CommandManager` angemeldet.

### Requirement: Ein neues Feature ändert keinen fremden Code
**Reason**: Die Modulliste gibt es nicht mehr. Avaje findet Features selbst, deshalb ist ein neues Feature nur ein neues Paket, ohne eine einzige Zeile außerhalb. Ersetzt durch „Ein neues Feature ist nur ein neues Paket“.
**Migration**: Keine.
