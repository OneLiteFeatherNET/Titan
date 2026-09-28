# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Ein neues Feature ist nur ein neues Paket`
- TO: `### Requirement: Ein neues Feature ist nur ein neues Modul`

## MODIFIED Requirements

### Requirement: Module sind voneinander unabhängig
Ein Feature DARF NICHT direkt von einem anderen Feature abhängen. Gemeinsam genutzte Funktionen MÜSSEN über Beans der Plattform oder über gemeinsame Bibliotheken außerhalb der Features laufen. Plattform und gemeinsame Bibliotheken DÜRFEN NICHT von Features abhängen. Der Build MUSS Verstöße melden; die Grenzen MÜSSEN durch die Modulstruktur erzwungen werden, sodass verbotener Code gar nicht erst kompiliert.

#### Scenario: Verbotene Abhängigkeit zwischen Features
- **WHEN** Code im Feature „tickle“ Code aus dem Feature „sit“ verwendet
- **THEN** schlägt der Build mit einem Hinweis auf die verbotene Abhängigkeit fehl

#### Scenario: Gemeinsame Bibliothek hängt von einem Feature ab
- **WHEN** Code in einer gemeinsamen Bibliothek Code aus einem Feature verwendet
- **THEN** schlägt der Build fehl

### Requirement: Reihenfolge der Event-Verarbeitung ist festgelegt
Reagieren mehrere Features auf dasselbe Event, MUSS die Reihenfolge, in der sie es erhalten, für jedes Feature fest vorgegeben und eindeutig sein. Sie DARF NICHT von der Startreihenfolge abhängen. Geben zwei Features dieselbe Position an, DARF die Lobby NICHT starten, und die Fehlermeldung MUSS beide Features und die Position nennen.

#### Scenario: Schutz und Navigator beim selben Klick
- **WHEN** ein Spieler im geöffneten Navigator ein Ziel anklickt, egal in welcher Reihenfolge „protection“ und „navigator“ gestartet wurden
- **THEN** ist der Klick abgebrochen, das Symbol bleibt im Navigator, und die Weiterleitung findet statt

#### Scenario: Doppelte Position
- **WHEN** zwei Features dieselbe Position in der Event-Reihenfolge angeben
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt beide Features und die Position

### Requirement: Ein neues Feature ist nur ein neues Modul
Ein neues Feature MUSS sich allein durch ein neues Modul unter `features/` hinzufügen lassen. Dafür DÜRFEN weder andere Features noch die Plattform noch eine zentrale Liste geändert werden. Jede Variante, die alle Features enthält, MUSS das neue Feature ohne Änderung aufnehmen.

#### Scenario: Feature mit Listener und Hotbar-Item
- **WHEN** ein Entwickler ein Feature mit eigenem Config-Abschnitt, einem Event-Listener und einem Hotbar-Item in einem neuen Modul unter `features/` anlegt
- **THEN** startet die Lobby mit dem Feature, und außerhalb des neuen Moduls ist keine Zeile geändert
