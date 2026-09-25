# lobby-modules Specification

## Purpose
Legt fest, wie Lobby-Features als eigenständige Module hoch- und heruntergefahren werden. Neue Features sollen hinzukommen, ohne bestehende Features oder die zentrale Verdrahtung umzubauen, und beim Abschalten keine Reste hinterlassen.

## Requirements

### Requirement: Module starten in Registrierungsreihenfolge
Die Lobby MUSS alle registrierten Module genau einmal starten, in der Reihenfolge ihrer Registrierung, bevor ein Spieler die Lobby betreten kann.

#### Scenario: Start in fester Reihenfolge
- **WHEN** die Module A, B und C in dieser Reihenfolge registriert sind und die Lobby startet
- **THEN** wird A vor B und B vor C gestartet, und jedes Modul genau einmal

#### Scenario: Spieler erst nach dem Start
- **WHEN** ein Spieler sich verbindet
- **THEN** sind alle registrierten Module bereits gestartet

### Requirement: Module fahren in umgekehrter Reihenfolge herunter
Beim Herunterfahren MUSS die Lobby die Module in umgekehrter Registrierungsreihenfolge beenden. Bevor die eigene Abschaltlogik eines Moduls läuft, MUSS das Modul bereits von allen Events getrennt sein und seine geplanten Aufgaben MÜSSEN abgebrochen sein.

#### Scenario: Umgekehrte Reihenfolge
- **WHEN** die Module A, B und C registriert sind und die Lobby herunterfährt
- **THEN** wird C vor B und B vor A beendet

#### Scenario: Keine Events während des Abschaltens
- **WHEN** die Abschaltlogik eines Moduls läuft und währenddessen ein Event eintrifft, auf das das Modul gehört hat
- **THEN** reagiert das Modul nicht mehr auf dieses Event

### Requirement: Abschalten hinterlässt keine Reste
Alles, was ein Modul über seinen Kontext angemeldet hat (Event-Listener, geplante Aufgaben, Befehle, Hotbar-Items, Navigator-Einträge), MUSS nach dem Abschalten des Moduls vollständig entfernt sein. Das Modul muss sich dafür nichts merken.

#### Scenario: Listener sind nach dem Abschalten entfernt
- **WHEN** ein Modul beim Start Listener für drei Event-Typen angemeldet hat und danach abgeschaltet wird
- **THEN** löst keines dieser Events mehr Code des Moduls aus

#### Scenario: Wiederkehrende Aufgaben enden
- **WHEN** ein Modul eine jede Sekunde wiederkehrende Aufgabe geplant hat und abgeschaltet wird
- **THEN** läuft die Aufgabe danach nicht mehr

#### Scenario: Befehle verschwinden
- **WHEN** ein Modul einen Befehl angemeldet hat und abgeschaltet wird
- **THEN** ist der Befehl nicht mehr ausführbar

### Requirement: Keine Listener-Registrierung zur Laufzeit
Ein Modul MUSS seine Event-Listener während des Starts anmelden. Das Betreten, Verlassen oder die Interaktion eines Spielers DARF NICHT dazu führen, dass zusätzliche Event-Listener angemeldet werden.

#### Scenario: Spielerwechsel verändert die Listener-Anzahl nicht
- **WHEN** 100 Spieler nacheinander die Lobby betreten, den Navigator öffnen und die Lobby wieder verlassen
- **THEN** ist die Anzahl angemeldeter Event-Listener danach genauso groß wie vorher

### Requirement: Fehler in einem Listener bleiben dem Modul und Spieler zugeordnet
Wirft ein Listener eines Moduls eine Ausnahme, MUSS die Lobby weiterlaufen. Die Fehlermeldung MUSS das betroffene Modul und, falls vorhanden, den betroffenen Spieler nennen.

#### Scenario: Ausnahme im Listener
- **WHEN** ein Listener des Moduls „sit“ bei einem Event des Spielers „Alex“ eine Ausnahme wirft
- **THEN** läuft die Lobby weiter und der gemeldete Fehler nennt das Modul „sit“ und den Spieler „Alex“

### Requirement: Module sind voneinander unabhängig
Ein Feature-Modul DARF NICHT direkt von einem anderen Feature-Modul abhängen. Gemeinsam genutzte Funktionen MÜSSEN über die Andockpunkte des Modulkontexts oder über gemeinsame Bibliotheken außerhalb der Feature-Module laufen. Gemeinsame Bibliotheken DÜRFEN NICHT von Feature-Modulen abhängen. Ein automatisierter Test MUSS Verstöße im Build melden.

#### Scenario: Verbotene Abhängigkeit zwischen Features
- **WHEN** Code im Feature „tickle“ Code aus dem Feature „sit“ verwendet
- **THEN** schlägt der Build mit einem Hinweis auf die verbotene Abhängigkeit fehl

#### Scenario: Gemeinsame Bibliothek hängt von einem Feature ab
- **WHEN** Code in einer gemeinsamen Bibliothek Code aus einem Feature verwendet
- **THEN** schlägt der Build fehl

### Requirement: Ein neues Feature ändert keinen fremden Code
Ein neues Feature-Modul MUSS sich allein durch ein neues Paket und einen einzigen Eintrag in der Modulliste hinzufügen lassen. Dafür DÜRFEN keine anderen Feature-Module und keine Andockpunkte geändert werden.

#### Scenario: Beispielmodul aus der Vorlage
- **WHEN** ein Entwickler nach der Feature-Vorlage ein Modul mit eigenem Config-Abschnitt, einem Hotbar-Item und einem Befehl anlegt
- **THEN** besteht die Änderung außerhalb des neuen Pakets aus genau einer Zeile in der Modulliste

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
