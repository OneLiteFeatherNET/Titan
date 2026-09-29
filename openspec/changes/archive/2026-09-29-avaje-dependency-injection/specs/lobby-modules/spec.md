# Spec Delta

## MODIFIED Requirements

### Requirement: Module starten in Registrierungsreihenfolge
Die Lobby MUSS alle vorhandenen Module genau einmal starten, bevor ein Spieler die Lobby betreten kann. Die Reihenfolge MUSS der Priorität folgen, die jedes Modul ausdrücklich deklariert. Bei gleicher Priorität MUSS die Reihenfolge deterministisch sein und darf nicht vom Zufall oder der Reihenfolge im Klassenpfad abhängen. Ein Modul wird allein dadurch vorhanden, dass seine Klasse im Build liegt. Eine zentrale Modulliste gibt es nicht.

#### Scenario: Start in fester Reihenfolge
- **WHEN** die Module A, B und C die Prioritäten 10, 20 und 30 deklarieren und die Lobby startet
- **THEN** wird A vor B und B vor C gestartet, und jedes Modul genau einmal

#### Scenario: Gleiche Priorität
- **WHEN** zwei Module dieselbe Priorität deklarieren und die Lobby mehrfach gestartet wird
- **THEN** starten sie bei jedem Start in derselben Reihenfolge

#### Scenario: Spieler erst nach dem Start
- **WHEN** ein Spieler sich verbindet
- **THEN** sind alle vorhandenen Module bereits gestartet

### Requirement: Module fahren in umgekehrter Reihenfolge herunter
Beim Herunterfahren MUSS die Lobby die Module in umgekehrter Startreihenfolge beenden. Bevor die eigene Abschaltlogik eines Moduls läuft, MUSS das Modul bereits von allen Events getrennt sein und seine geplanten Aufgaben MÜSSEN abgebrochen sein.

#### Scenario: Umgekehrte Reihenfolge
- **WHEN** die Module A, B und C in dieser Reihenfolge gestartet wurden und die Lobby herunterfährt
- **THEN** wird C vor B und B vor A beendet

#### Scenario: Keine Events während des Abschaltens
- **WHEN** die Abschaltlogik eines Moduls läuft und währenddessen ein Event eintrifft, auf das das Modul gehört hat
- **THEN** reagiert das Modul nicht mehr auf dieses Event

### Requirement: Ein neues Feature ändert keinen fremden Code
Ein neues Feature-Modul MUSS sich allein durch ein neues Paket hinzufügen lassen. Außerhalb dieses Pakets DÜRFEN weder eine Modulliste noch die Composition Root, andere Feature-Module oder Andockpunkte geändert werden. Gemeinsame Dienste (z.B. ein Client für ein Backend) MUSS das Modul über seinen Konstruktor anfordern können, ohne dass sie von Hand weitergereicht werden.

#### Scenario: Beispielmodul aus der Vorlage
- **WHEN** ein Entwickler nach der Feature-Vorlage ein Modul mit eigenem Config-Abschnitt, einem Hotbar-Item und einem Befehl anlegt
- **THEN** enthält die Änderung außerhalb des neuen Pakets keine einzige geänderte Zeile, und das Modul wird beim nächsten Start gestartet

#### Scenario: Gemeinsamer Dienst per Konstruktor
- **WHEN** ein neues Modul einen bereits vorhandenen Plattform-Dienst (z.B. `Deliver` oder `FeatureFlags`) als Konstruktor-Parameter deklariert
- **THEN** erhält es diesen Dienst beim Start, ohne dass eine andere Datei geändert wird

## ADDED Requirements

### Requirement: Fehlende Abhängigkeiten fallen vor dem Betrieb auf
Fordert ein Modul eine Abhängigkeit an, die niemand bereitstellt, oder entsteht ein Zyklus, MUSS das spätestens beim Build oder beim Start der Lobby auffallen, bevor ein Spieler verbunden ist. Die Fehlermeldung MUSS das betroffene Modul und die fehlende Abhängigkeit nennen. Die Lobby DARF NICHT ohne dieses Modul weiterlaufen.

#### Scenario: Dienst fehlt
- **WHEN** ein Modul einen Konstruktor-Parameter vom Typ `FriendsClient` deklariert, den kein Bean bereitstellt
- **THEN** schlägt der Build oder der Start fehl, und die Meldung nennt das Modul und `FriendsClient`

#### Scenario: Vollständige Verdrahtung wird geprüft
- **WHEN** der Test für die vollständige Verdrahtung läuft
- **THEN** findet er alle Feature-Module, jedes genau einmal, in der deklarierten Reihenfolge, und ohne fehlende Abhängigkeit
