# lobby-persistence Specification

## Purpose
Legt fest, wann die Lobby eine Datenbank nutzt, wie sie das Schema beim Start auf den aktuellen Stand bringt und wie sie reagiert, wenn die Datenbank beim Start oder im Betrieb nicht erreichbar ist.

## Requirements

### Requirement: Datenbank nur mit Konfiguration
Die Lobby MUSS genau dann eine Datenbank nutzen, wenn eine Datenbank-URL konfiguriert ist. Ohne URL DARF die Lobby KEINE Datenbankverbindung aufbauen, und alle Features MÜSSEN wie ohne Datenbank funktionieren. URL, Benutzer und Passwort MÜSSEN sich über Umgebungsvariablen setzen lassen, ohne den Jar zu ändern. Das Passwort DARF NICHT in Logs erscheinen.

#### Scenario: Ohne Konfiguration
- **WHEN** die Lobby ohne Datenbank-URL startet
- **THEN** startet sie ohne Datenbankverbindung, und das Jump and Run hält Rekorde nur im Speicher

#### Scenario: Konfiguration über die Umgebung
- **WHEN** die Lobby mit URL, Benutzer und Passwort aus Umgebungsvariablen startet
- **THEN** verbindet sie sich mit dieser Datenbank, und kein Log enthält das Passwort

### Requirement: Pool und ORM über die Konfiguration einstellbar
Der Betreiber MUSS jede Einstellung des Verbindungspools und des ORM über die YAML-Konfiguration setzen können, ohne Code oder Jar zu ändern, also auch Einstellungen, die die Lobby selbst nicht kennt. Die Lobby MUSS diese Einstellungen unverändert an Pool und ORM weitergeben. Fehlt eine Einstellung, MUSS der mitgelieferte Standard gelten. URL, Benutzer und Passwort MÜSSEN weiter über ihre eigenen Schlüssel kommen. Die Einstellung, mit der das ORM das Schema selbst anlegt oder ändert, DARF NICHT von der reinen Prüfung abweichen, weil die Migration das Schema besitzt; ein abweichender Wert MUSS den Start mit einer Meldung abbrechen, die die Einstellung nennt. Ein unbekannter oder ungültiger Pool-Schlüssel MUSS den Start mit einer Meldung abbrechen, die den Schlüssel nennt.

#### Scenario: Pool-Größe ändern
- **WHEN** der Betreiber in der YAML die maximale Pool-Größe auf 10 setzt
- **THEN** öffnet die Lobby höchstens 10 Verbindungen

#### Scenario: ORM-Einstellung durchreichen
- **WHEN** der Betreiber in der YAML eine ORM-Einstellung setzt, die die Lobby nicht kennt (z. B. die JDBC-Batch-Größe)
- **THEN** gilt diese Einstellung im ORM

#### Scenario: Ohne eigene Einstellungen
- **WHEN** die YAML keine Pool- oder ORM-Einstellungen enthält
- **THEN** startet die Lobby mit den mitgelieferten Standards

#### Scenario: Schema-Erzeugung durch das ORM
- **WHEN** der Betreiber die Schema-Einstellung des ORM auf Anlegen oder Aktualisieren setzt
- **THEN** bricht der Start ab, und die Meldung nennt die Einstellung

#### Scenario: Tippfehler im Pool-Schlüssel
- **WHEN** der Betreiber einen Pool-Schlüssel setzt, den der Pool nicht kennt
- **THEN** bricht der Start ab, und die Meldung nennt den Schlüssel

### Requirement: Schema beim Start migrieren
Ist eine Datenbank konfiguriert, MUSS die Lobby beim Start alle noch fehlenden Schema-Änderungen anwenden, bevor ein Spieler beitreten kann. Bereits angewendete Änderungen DÜRFEN NICHT erneut laufen. Starten mehrere Lobbys gleichzeitig, MUSS jede Änderung genau einmal angewendet werden. Passt das Schema nach der Migration nicht zum erwarteten Stand, MUSS der Start abbrechen.

#### Scenario: Leere Datenbank
- **WHEN** die Lobby gegen eine leere Datenbank startet
- **THEN** legt sie alle Tabellen an, und Spieler können danach beitreten

#### Scenario: Zweiter Start
- **WHEN** die Lobby ein zweites Mal gegen dieselbe Datenbank startet
- **THEN** wendet sie keine Änderung erneut an, und vorhandene Daten bleiben erhalten

#### Scenario: Gleichzeitiger Start
- **WHEN** zwei Lobbys gleichzeitig gegen eine leere Datenbank starten
- **THEN** sind danach alle Tabellen genau einmal angelegt, und beide Lobbys laufen

### Requirement: Nicht erreichbare Datenbank
Ist eine Datenbank konfiguriert, beim Start aber nicht erreichbar oder die Migration schlägt fehl, MUSS der Start mit einer Fehlermeldung für den Betreiber abbrechen, statt still ohne Datenbank zu laufen. Fällt die Datenbank im laufenden Betrieb aus, MUSS die Lobby spielbar bleiben. Lese- und Schreibfehler MÜSSEN für den Betreiber geloggt werden. Die Spieler DÜRFEN davon nichts außer fehlenden oder veralteten Daten bemerken.

#### Scenario: Datenbank beim Start weg
- **WHEN** die Lobby mit konfigurierter, aber nicht erreichbarer Datenbank startet
- **THEN** bricht der Start ab, und das Log nennt die nicht erreichbare Datenbank ohne Passwort

#### Scenario: Datenbank fällt im Betrieb aus
- **WHEN** die Datenbank ausfällt, während Spieler in der Lobby sind
- **THEN** können sie weiter Jump and Run spielen, und das Log meldet die fehlgeschlagenen Schreibzugriffe
