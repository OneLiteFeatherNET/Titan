# lobby-modules Specification

## Purpose
Legt fest, wie Lobby-Features als eigenständige Avaje-Beans starten, Events in fester Reihenfolge verarbeiten und beim Herunterfahren keine Reste hinterlassen. Ein neues Feature ist ein neues Modul unter `features/`, ohne Änderung an Plattform oder anderen Features.

## Requirements

### Requirement: Keine Listener-Registrierung zur Laufzeit
Ein Modul MUSS seine Event-Listener während des Starts anmelden. Das Betreten, Verlassen oder die Interaktion eines Spielers DARF NICHT dazu führen, dass zusätzliche Event-Listener angemeldet werden.

#### Scenario: Spielerwechsel verändert die Listener-Anzahl nicht
- **WHEN** 100 Spieler nacheinander die Lobby betreten, den Navigator öffnen und die Lobby wieder verlassen
- **THEN** ist die Anzahl angemeldeter Event-Listener danach genauso groß wie vorher

### Requirement: Fehler in einem Listener bleiben dem Modul und Spieler zugeordnet
Wirft ein Listener eines Features eine Ausnahme, MUSS die Lobby weiterlaufen. Die Fehlermeldung MUSS das betroffene Feature und, falls vorhanden, den betroffenen Spieler nennen.

#### Scenario: Ausnahme im Listener
- **WHEN** ein Listener des Features „sit“ bei einem Event des Spielers „Alex“ eine Ausnahme wirft
- **THEN** läuft die Lobby weiter und der gemeldete Fehler nennt das Feature „sit“ und den Spieler „Alex“

### Requirement: Module sind voneinander unabhängig
Ein Feature DARF NICHT direkt von einem anderen Feature abhängen. Gemeinsam genutzte Funktionen MÜSSEN über Beans der Plattform oder über gemeinsame Bibliotheken außerhalb der Features laufen. Plattform und gemeinsame Bibliotheken DÜRFEN NICHT von Features abhängen. Der Build MUSS Verstöße melden; die Grenzen MÜSSEN durch die Modulstruktur erzwungen werden, sodass verbotener Code gar nicht erst kompiliert.

#### Scenario: Verbotene Abhängigkeit zwischen Features
- **WHEN** Code im Feature „tickle“ Code aus dem Feature „sit“ verwendet
- **THEN** schlägt der Build mit einem Hinweis auf die verbotene Abhängigkeit fehl

#### Scenario: Gemeinsame Bibliothek hängt von einem Feature ab
- **WHEN** Code in einer gemeinsamen Bibliothek Code aus einem Feature verwendet
- **THEN** schlägt der Build fehl

### Requirement: Spielerverhalten bleibt beim Umzug erhalten
Die bestehenden Lobby-Funktionen MÜSSEN sich nach dem Umzug in Module für Spieler genauso verhalten wie vorher: Schutz vor Item-, Block- und Inventar-Aktionen, Spawn und Höhen-Teleport, Sitzen, Kitzeln, Elytra mit Feuerwerks-Boost, Navigator sowie Tod und Respawn ohne Todesnachricht.

#### Scenario: Blockabbau bleibt verhindert
- **WHEN** ein Spieler in der Lobby einen Block abbauen will
- **THEN** wird die Aktion abgebrochen und der Block bleibt bestehen

#### Scenario: Fall unter die Mindesthöhe
- **WHEN** ein Spieler unter die konfigurierte Mindesthöhe fällt
- **THEN** wird er zum Lobby-Spawn teleportiert

#### Scenario: Sitzen und Aufstehen
- **WHEN** ein Spieler auf einen erlaubten Sitzblock klickt und danach schleicht
- **THEN** sitzt er zunächst auf dem Block und steht danach an seiner vorherigen Position wieder auf

#### Scenario: Tod ohne Nachricht
- **WHEN** ein Spieler stirbt
- **THEN** erscheint keine Todesnachricht, der Spieler respawnt sofort und erhält seine Lobby-Items zurück

### Requirement: Features starten vor dem ersten Spieler
Jedes Feature MUSS beim Start der Lobby genau einmal gestartet werden, bevor ein Spieler die Lobby betreten kann. Features DÜRFEN sich beim Start NICHT auf die Reihenfolge anderer Features verlassen.

#### Scenario: Spieler erst nach dem Start
- **WHEN** ein Spieler sich verbindet
- **THEN** sind alle Features bereits gestartet, und jedes genau einmal

#### Scenario: Fehler beim Start eines Features
- **WHEN** ein Feature beim Start eine Ausnahme wirft
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt das Feature

### Requirement: Reihenfolge der Event-Verarbeitung ist festgelegt
Reagieren mehrere Features auf dasselbe Event, MUSS die Reihenfolge, in der sie es erhalten, für jedes Feature fest vorgegeben und eindeutig sein. Sie DARF NICHT von der Startreihenfolge abhängen. Geben zwei Features dieselbe Position an, DARF die Lobby NICHT starten, und die Fehlermeldung MUSS beide Features und die Position nennen.

#### Scenario: Schutz und Navigator beim selben Klick
- **WHEN** ein Spieler im geöffneten Navigator ein Ziel anklickt, egal in welcher Reihenfolge „protection“ und „navigator“ gestartet wurden
- **THEN** ist der Klick abgebrochen, das Symbol bleibt im Navigator, und die Weiterleitung findet statt

#### Scenario: Doppelte Position
- **WHEN** zwei Features dieselbe Position in der Event-Reihenfolge angeben
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt beide Features und die Position

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

### Requirement: Ein neues Feature ist nur ein neues Modul
Ein neues Feature MUSS sich allein durch ein neues Modul unter `features/` hinzufügen lassen. Dafür DÜRFEN weder andere Features noch die Plattform noch eine zentrale Liste geändert werden. Jede Variante, die alle Features enthält, MUSS das neue Feature ohne Änderung aufnehmen.

#### Scenario: Feature mit Listener und Hotbar-Item
- **WHEN** ein Entwickler ein Feature mit eigenem Config-Abschnitt, einem Event-Listener und einem Hotbar-Item in einem neuen Modul unter `features/` anlegt
- **THEN** startet die Lobby mit dem Feature, und außerhalb des neuen Moduls ist keine Zeile geändert
