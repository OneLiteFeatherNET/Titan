# Spec Delta

## Purpose

Legt fest, wie die Lobby Rechte von Spielern und Konsole prüft: über einen austauschbaren Permission-Dienst, mit LuckPerms in Produktion und einem sicheren Verhalten, wenn keine Permission-Plattform eingebunden ist.

## ADDED Requirements

### Requirement: Rechte kommen aus der eingebundenen Permission-Plattform
Ist eine Permission-Plattform eingebunden, MUSS jede Rechteprüfung eines Spielers ihr Ergebnis liefern, einschließlich der Kontexte, die für den Spieler gerade gelten. Ein Recht, das die Plattform weder erlaubt noch verbietet, MUSS als nicht erteilt gelten.

#### Scenario: Recht erteilt
- **WHEN** LuckPerms dem Spieler „Alex“ `titan.command.stop` erteilt und Alex `/stop` ausführt
- **THEN** fährt die Lobby herunter

#### Scenario: Recht nicht gesetzt
- **WHEN** LuckPerms für den Spieler „Alex“ `titan.command.stop` weder erteilt noch verbietet
- **THEN** darf Alex `/stop` nicht ausführen

#### Scenario: Recht nur in einem Kontext
- **WHEN** LuckPerms dem Spieler ein Recht nur im Kontext `server=lobby` erteilt und die Lobby in diesem Kontext läuft
- **THEN** gilt das Recht für den Spieler als erteilt

### Requirement: Ohne Permission-Plattform haben Spieler keine Rechte
Ist keine Permission-Plattform eingebunden, MUSS jede Rechteprüfung eines Spielers „nicht erteilt“ ergeben. Befehle, die die Konsole ausführen darf, MÜSSEN von der Konsole weiterhin ausführbar sein. Beim Start MUSS die Lobby melden, welcher Permission-Dienst aktiv ist.

#### Scenario: Spieler ohne Plattform
- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler `/stop` ausführt
- **THEN** wird der Befehl abgelehnt

#### Scenario: Konsole ohne Plattform
- **WHEN** die Lobby ohne Permission-Plattform läuft und in der Konsole `stop` eingegeben wird
- **THEN** fährt die Lobby herunter

#### Scenario: Aktiver Dienst im Start-Log
- **WHEN** die Lobby startet
- **THEN** nennt das Start-Log den aktiven Permission-Dienst

### Requirement: Die Produktionsvariante startet nur mit LuckPerms
Die Variante `cloudnet` MUSS LuckPerms als Permission-Plattform enthalten. Fehlt LuckPerms in dieser Variante beim Start, DARF die Lobby NICHT starten. Die Variante `local` MUSS ohne LuckPerms starten und LuckPerms nur enthalten, wenn es beim Bauen ausdrücklich eingeschaltet wurde.

#### Scenario: Produktionsvariante ohne LuckPerms
- **WHEN** die Variante `cloudnet` startet und LuckPerms nicht geladen ist
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt LuckPerms

#### Scenario: Entwicklungsvariante ohne Schalter
- **WHEN** die Variante `local` ohne den LuckPerms-Schalter gebaut und gestartet wird
- **THEN** startet die Lobby ohne LuckPerms, und Spieler haben keine Rechte

### Requirement: LuckPerms startet genau einmal und behält seine Daten
LuckPerms MUSS von der Lobby selbst gestartet werden, bevor ein Spieler sich verbinden kann, und beim Herunterfahren beendet werden. Die LuckPerms-Daten und -Konfiguration MÜSSEN am bisherigen Ort `data/` bleiben. Liegt zusätzlich eine LuckPerms-Extension im Ordner `extensions/`, DARF die Lobby NICHT starten, und die Fehlermeldung MUSS die Extension nennen.

#### Scenario: Update mit bestehenden Daten
- **WHEN** ein Betreiber auf die neue Version wechselt und `data/` unverändert lässt
- **THEN** gelten alle bisherigen Gruppen und Rechte weiter

#### Scenario: Doppeltes LuckPerms
- **WHEN** `extensions/luckperms.jar` noch vorhanden ist und die Lobby mit LuckPerms-Plattform startet
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt die LuckPerms-Extension

### Requirement: CloudNet prüft Rechte wie die Lobby
Fragt CloudNet ein Recht eines Spielers ab, MUSS das Ergebnis dasselbe sein wie bei einer Rechteprüfung in der Lobby. Das gilt auch ohne Permission-Plattform.

#### Scenario: Gleiches Ergebnis
- **WHEN** LuckPerms dem Spieler ein Recht erteilt und CloudNet dieses Recht abfragt
- **THEN** meldet CloudNet das Recht als erteilt

#### Scenario: Ohne Plattform
- **WHEN** keine Permission-Plattform eingebunden ist und CloudNet ein Recht eines Spielers abfragt
- **THEN** meldet CloudNet das Recht als nicht erteilt
