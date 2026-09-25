# lobby-module-config Specification

## Purpose
Legt fest, wie jedes Lobby-Modul seine eigene Konfiguration aus den mitgelieferten Standardwerten, `application.yaml`, Profilen und Overrides per Env-Variable oder System-Property erhält und wie ungültige Werte behandelt werden.

## Requirements

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

### Requirement: Fehlende Werte erhalten Standardwerte
Fehlt ein Abschnitt oder ein einzelner Wert, MUSS das Modul den dokumentierten Standardwert erhalten. Fehlende Werte DÜRFEN NICHT als 0, leer oder `null` ankommen, wenn der Standardwert etwas anderes ist.

#### Scenario: Fehlender Abschnitt
- **WHEN** `app.json` keinen Abschnitt `"tickle"` enthält
- **THEN** startet das Modul „tickle“ mit seiner Standard-Cooldown-Dauer von 4000 ms

#### Scenario: Fehlender Einzelwert
- **WHEN** der Abschnitt `"spawn"` nur `minHeight` enthält, aber kein `maxHeight`
- **THEN** gilt für `maxHeight` der Standardwert und nicht 0

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

### Requirement: Profile ergänzen die Basiskonfiguration
Die Lobby MUSS Profile unterstützen. Das aktive Profil bzw. die aktiven Profile werden über eine Env-Variable oder System-Property gewählt. Für jedes aktive Profil MUSS die Lobby `application-<profil>.yaml` laden, falls die Datei vorhanden ist. Werte aus einem Profil MÜSSEN die Basiswerte überschreiben, nicht gesetzte Werte bleiben aus der Basis erhalten.

#### Scenario: Profil überschreibt einen Wert
- **WHEN** `application.yaml` `tickle.cooldownMillis: 4000` enthält, `application-dev.yaml` `tickle.cooldownMillis: 1000` enthält und das Profil `dev` aktiv ist
- **THEN** gilt für das Modul „tickle“ eine Cooldown-Dauer von 1000 ms

#### Scenario: Profil ohne Datei
- **WHEN** das Profil `prod` aktiv ist, aber keine `application-prod.yaml` existiert
- **THEN** startet die Lobby mit den Werten aus `application.yaml` bzw. den Standardwerten

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
