# Spec Delta

<!-- Diese Delta baut auf dem Stand nach dem Archivieren von `standardized-config-profiles` auf.
     Die MODIFIED/REMOVED-Überschriften beziehen sich auf die Anforderungen, die jene Change in
     die Hauptspec übernimmt. -->

## MODIFIED Requirements

### Requirement: Ein Konfigurationsabschnitt pro Modul
Die Lobby-Konfiguration MUSS aus je einem benannten Abschnitt pro Modul bestehen. Die Grundlage bildet `application.yaml` im Arbeitsverzeichnis, ergänzt durch die Dateien der aktiven Profile und durch Overrides. Der Abschnittsname MUSS der Modul-ID entsprechen. Jeder Schlüssel eines Moduls MUSS unter seinem eigenen Abschnitt liegen (`<modul-id>.<feld>`). Ein Modul DARF KEINE Werte aus dem Abschnitt eines anderen Moduls lesen. Die Schlüssel und ihre Bedeutung MÜSSEN dokumentiert sein.

#### Scenario: Modul liest seinen Abschnitt
- **WHEN** `application.yaml` den Abschnitt `sit` mit `offset: {x: 0.5, y: 0.25, z: 0.5}` enthält
- **THEN** erhält das Modul „sit“ genau diesen Versatz

#### Scenario: Liste von Einträgen
- **WHEN** `application.yaml` im Abschnitt `navigator` unter `entries` einen Eintrag `parkour` mit Platz 2 enthält
- **THEN** zeigt der Navigator „Parkour“ auf Platz 2 zusätzlich zu den übrigen Einträgen

#### Scenario: Profil ändert nur einen Wert eines Abschnitts
- **WHEN** `application-dev.yaml` nur `sit.offset.y: 0.5` setzt und das Profil `dev` aktiv ist
- **THEN** gilt für „sit“ der Versatz y = 0.5, und x und z behalten ihre Standardwerte

### Requirement: Ungültige Werte verhindern den Start
Enthält ein Abschnitt einen ungültigen Wert, MUSS die Lobby den Start abbrechen, egal ob der Wert aus einer Datei, einem Profil oder einem Override stammt. Die Fehlermeldung MUSS den vollständigen Schlüssel (`<modul-id>.<feld>`) und den Grund nennen. Die Lobby DARF NICHT mit einem stillschweigend ersetzten Wert weiterlaufen. Ungültig sind zum Beispiel ein Wert, der nicht zum erwarteten Typ passt, eine negative Dauer, eine Mindesthöhe über der Maximalhöhe oder ein unbekannter Block.

#### Scenario: Negative Dauer
- **WHEN** `application.yaml` im Abschnitt `tickle` `cooldownMillis: -5` enthält
- **THEN** startet die Lobby nicht und meldet, dass `tickle.cooldownMillis` nicht negativ sein darf

#### Scenario: Unmögliche Höhengrenzen
- **WHEN** im Abschnitt `spawn` `minHeight` größer als `maxHeight` ist
- **THEN** startet die Lobby nicht und nennt `spawn.minHeight` und `spawn.maxHeight` in der Fehlermeldung

#### Scenario: Ungültiger Override
- **WHEN** eine Env-Variable den Wert für `tickle.cooldownMillis` auf `abc` setzt
- **THEN** startet die Lobby nicht und meldet `tickle.cooldownMillis` mit dem Grund

#### Scenario: Syntaktisch kaputte Datei
- **WHEN** `application.yaml` kein gültiges YAML ist
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt die Datei und die Stelle des Fehlers

### Requirement: Ohne Konfigurationsdatei gelten die Standardwerte
Fehlt `application.yaml` im Arbeitsverzeichnis, MUSS die Lobby mit den mitgelieferten Standardwerten aller Module starten. Die Standardwerte MÜSSEN mit der Lobby ausgeliefert werden und für Betreiber einsehbar sein. Die Lobby DARF im Betrieb keine Konfigurationsdatei anlegen oder verändern.

#### Scenario: Erster Start ohne Datei
- **WHEN** die Lobby ohne `application.yaml` und ohne `app.json` startet
- **THEN** läuft sie mit den Standardwerten aller Module, und im Arbeitsverzeichnis entsteht keine neue Konfigurationsdatei

#### Scenario: Datei setzt nur einzelne Werte
- **WHEN** `application.yaml` im Arbeitsverzeichnis nur `tickle.cooldownMillis: 1000` enthält
- **THEN** gilt für „tickle“ 1000 ms, und alle anderen Module laufen mit ihren Standardwerten

### Requirement: Overrides haben eine feste Rangfolge
Einzelne Werte MÜSSEN sich per Env-Variable und per System-Property überschreiben lassen. Die Rangfolge MUSS von niedrig nach hoch lauten:
1. mitgelieferte Standardwerte,
2. `application.yaml` im Arbeitsverzeichnis,
3. Profil-Dateien,
4. externe Datei aus einer Env-Variable bzw. System-Property,
5. Env-Variable,
6. System-Property.

Die Abbildung von Schlüssel auf Env-Variable MUSS dokumentiert sein.

#### Scenario: Datei schlägt Standardwert
- **WHEN** der mitgelieferte Standardwert für `spawn.simulationDistance` 2 ist und `application.yaml` im Arbeitsverzeichnis `spawn.simulationDistance: 3` enthält
- **THEN** sendet die Lobby eine Simulationsdistanz von 3

#### Scenario: Env-Variable schlägt Datei
- **WHEN** `application.yaml` `spawn.simulationDistance: 2` enthält und die passende Env-Variable den Wert `4` setzt
- **THEN** sendet die Lobby eine Simulationsdistanz von 4

#### Scenario: System-Property schlägt Env-Variable
- **WHEN** sowohl die Env-Variable als auch die System-Property für `spawn.simulationDistance` gesetzt sind
- **THEN** gilt der Wert der System-Property

## REMOVED Requirements

### Requirement: Unbekannte Schlüssel werden gemeldet
**Reason**: Die Lobby bindet Abschnitte nicht mehr an ein Schema aus Config-Records, sondern liest einzelne Schlüssel. Es gibt damit keine Liste bekannter Schlüssel mehr, gegen die sich prüfen ließe, und das verwendete Konfigurations-Framework bietet keine solche Prüfung.
**Migration**: Die gültigen Schlüssel stehen in der mitgelieferten `application.yaml` mit den Standardwerten und in der Doku. Veraltete Schlüssel in der eigenen `application.yaml` werden stillschweigend ignoriert und sollten beim Update anhand der Doku entfernt werden.

### Requirement: Bestehende app.json wird einmalig umgestellt
**Reason**: Die Umstellung war nur für den Übergang von `app.json` auf `application.yaml` gedacht und wurde mit der vorherigen Version ausgeliefert. Sie wird nicht dauerhaft mitgepflegt.
**Migration**: Vor dem Update das vorherige Release einmal starten, damit es `app.json` in `application.yaml` umstellt, oder die Werte von Hand in `application.yaml` übertragen (gleiche Abschnitte und Schlüssel). Eine übrig gebliebene `app.json` bzw. `app.json.migrated` wird nicht mehr gelesen und kann gelöscht werden.
