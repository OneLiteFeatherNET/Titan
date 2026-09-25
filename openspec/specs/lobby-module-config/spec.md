# lobby-module-config Specification

## Purpose
Legt fest, wie jedes Lobby-Modul seine eigene Konfiguration aus `app.json` erhält, wie ungültige Werte behandelt werden und wie bestehende Konfigurationsdateien ohne manuellen Eingriff weiter funktionieren.

## Requirements

### Requirement: Ein Konfigurationsabschnitt pro Modul
Die Lobby-Konfiguration in `app.json` MUSS aus je einem benannten Abschnitt pro Modul bestehen. Der Abschnittsname MUSS der Modul-ID entsprechen. Ein Modul MUSS nur seinen eigenen Abschnitt lesen können.

#### Scenario: Modul liest seinen Abschnitt
- **WHEN** `app.json` den Abschnitt `"sit"` mit `"offset": {"x":0.5,"y":0.25,"z":0.5}` enthält
- **THEN** erhält das Modul „sit“ genau diesen Versatz

### Requirement: Fehlende Werte erhalten Standardwerte
Fehlt ein Abschnitt oder ein einzelner Wert, MUSS das Modul den dokumentierten Standardwert erhalten. Fehlende Werte DÜRFEN NICHT als 0, leer oder `null` ankommen, wenn der Standardwert etwas anderes ist.

#### Scenario: Fehlender Abschnitt
- **WHEN** `app.json` keinen Abschnitt `"tickle"` enthält
- **THEN** startet das Modul „tickle“ mit seiner Standard-Cooldown-Dauer von 4000 ms

#### Scenario: Fehlender Einzelwert
- **WHEN** der Abschnitt `"spawn"` nur `minHeight` enthält, aber kein `maxHeight`
- **THEN** gilt für `maxHeight` der Standardwert und nicht 0

### Requirement: Fehlende Konfigurationsdatei wird angelegt
Existiert `app.json` nicht, MUSS die Lobby sie beim Start mit den Standardwerten aller Module anlegen und mit diesen Werten starten.

#### Scenario: Erster Start
- **WHEN** die Lobby ohne `app.json` startet
- **THEN** existiert danach eine `app.json` mit einem Abschnitt für jedes Modul und die Lobby läuft mit den Standardwerten

### Requirement: Ungültige Werte verhindern den Start
Enthält ein Abschnitt einen ungültigen Wert, MUSS die Lobby den Start abbrechen. Die Fehlermeldung MUSS Modul, Feld und Grund nennen. Die Lobby DARF NICHT mit einem stillschweigend ersetzten Wert weiterlaufen. Ungültig sind zum Beispiel eine negative Dauer, eine Mindesthöhe über der Maximalhöhe oder ein unbekannter Block.

#### Scenario: Negative Dauer
- **WHEN** `app.json` im Abschnitt `"tickle"` `"cooldownMillis": -5` enthält
- **THEN** startet die Lobby nicht und meldet, dass `tickle.cooldownMillis` nicht negativ sein darf

#### Scenario: Unmögliche Höhengrenzen
- **WHEN** im Abschnitt `"spawn"` `minHeight` größer als `maxHeight` ist
- **THEN** startet die Lobby nicht und nennt beide Felder in der Fehlermeldung

#### Scenario: Syntaktisch kaputte Datei
- **WHEN** `app.json` kein gültiges JSON ist
- **THEN** startet die Lobby nicht, die Fehlermeldung nennt Datei und Position des Fehlers, und die Datei wird nicht überschrieben

### Requirement: Altes flaches Format wird automatisch migriert
Liegt `app.json` im bisherigen flachen Format vor, MUSS die Lobby die Werte beim Start in die neuen Abschnitte übernehmen. Anschließend MUSS sie die Datei im neuen Format speichern und die alte Datei als Sicherung aufbewahren. Nicht mehr verwendete Schlüssel (`updateRateAgones`, `fireworkBoostSlot`, `elytraBoostMultiplier`) MÜSSEN verworfen und im Log genannt werden. `elytraBoostMultiplier` hat kein Ziel mehr: Der aus Voyager portierte Feuerwerks-Boost (siehe `design.md`) verwendet den unveränderten Impuls des Spiels ohne Mod und kennt daher keinen Multiplikator; das Modul „elytra“ startet nach einer Migration stattdessen mit seinen eigenen Standardwerten.

#### Scenario: Migration der bisherigen Datei
- **WHEN** die Lobby mit einer flachen `app.json` startet, die `"tickleDuration": 4000`, `"elytraBoostMultiplier": 35.0` und `"updateRateAgones": 2000` enthält
- **THEN** enthält die neue `app.json` `tickle.cooldownMillis = 4000`, aber weder `elytraBoostMultiplier` noch `updateRateAgones`, und die alte Datei liegt als Sicherung daneben

#### Scenario: Werte bleiben bei der Migration erhalten
- **WHEN** eine flache `app.json` mit von den Standardwerten abweichenden Werten migriert wird
- **THEN** verhält sich die Lobby danach genauso wie mit der alten Datei

### Requirement: Änderungen aus dem Setup-Server verlieren keine Werte
Ändert der Setup-Server einen einzelnen Wert, MUSS die gespeicherte Konfiguration danach alle übrigen Werte unverändert enthalten, auch die Werte anderer Module.

#### Scenario: Sitz-Versatz ändern
- **WHEN** im Setup-Server der Sitz-Versatz geändert wird, während die Höhengrenzen von den Standardwerten abweichen
- **THEN** enthält die gespeicherte `app.json` den neuen Versatz und die unveränderten Höhengrenzen

#### Scenario: Rundlauf ohne Änderung
- **WHEN** eine Konfiguration geladen und ohne Änderung wieder gespeichert wird
- **THEN** ist der Inhalt danach inhaltlich identisch mit vorher
