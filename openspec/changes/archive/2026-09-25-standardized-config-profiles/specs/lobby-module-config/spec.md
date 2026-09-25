# Spec Delta

## MODIFIED Requirements

### Requirement: Ein Konfigurationsabschnitt pro Modul
Die Lobby-Konfiguration MUSS aus je einem benannten Abschnitt pro Modul bestehen. Die Grundlage bildet `application.yaml` im Arbeitsverzeichnis, ergänzt durch die Dateien der aktiven Profile und durch Overrides. Der Abschnittsname MUSS der Modul-ID entsprechen. Ein Modul MUSS nur seinen eigenen Abschnitt lesen können. Die Schlüssel innerhalb eines Abschnitts MÜSSEN den Namen der Felder des Config-Records des Moduls entsprechen.

#### Scenario: Modul liest seinen Abschnitt
- **WHEN** `application.yaml` den Abschnitt `sit` mit `offset: {x: 0.5, y: 0.25, z: 0.5}` enthält
- **THEN** erhält das Modul „sit“ genau diesen Versatz

#### Scenario: Liste von Einträgen
- **WHEN** `application.yaml` im Abschnitt `navigator` unter `entries` einen Eintrag `parkour` mit Platz 2 enthält
- **THEN** zeigt der Navigator „Parkour“ auf Platz 2 zusätzlich zu den übrigen Einträgen

### Requirement: Ungültige Werte verhindern den Start
Enthält ein Abschnitt einen ungültigen Wert, MUSS die Lobby den Start abbrechen, egal ob der Wert aus einer Datei, einem Profil oder einem Override stammt. Die Fehlermeldung MUSS Modul, Feld und Grund nennen. Die Lobby DARF NICHT mit einem stillschweigend ersetzten Wert weiterlaufen. Ungültig sind zum Beispiel eine negative Dauer, eine Mindesthöhe über der Maximalhöhe oder ein unbekannter Block.

#### Scenario: Negative Dauer
- **WHEN** `application.yaml` im Abschnitt `tickle` `cooldownMillis: -5` enthält
- **THEN** startet die Lobby nicht und meldet, dass `tickle.cooldownMillis` nicht negativ sein darf

#### Scenario: Unmögliche Höhengrenzen
- **WHEN** im Abschnitt `spawn` `minHeight` größer als `maxHeight` ist
- **THEN** startet die Lobby nicht und nennt beide Felder in der Fehlermeldung

#### Scenario: Ungültiger Override
- **WHEN** eine Env-Variable den Wert für `tickle.cooldownMillis` auf `abc` setzt
- **THEN** startet die Lobby nicht und meldet `tickle.cooldownMillis` mit dem Grund

#### Scenario: Syntaktisch kaputte Datei
- **WHEN** `application.yaml` kein gültiges YAML ist
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt die Datei und die Stelle des Fehlers

## ADDED Requirements

### Requirement: Ohne Konfigurationsdatei gelten die Standardwerte
Fehlt `application.yaml`, MUSS die Lobby mit den dokumentierten Standardwerten aller Module starten. Die Lobby DARF im Betrieb keine Konfigurationsdatei anlegen oder verändern. Einzige Ausnahme ist die einmalige Umstellung einer bestehenden `app.json`.

#### Scenario: Erster Start ohne Datei
- **WHEN** die Lobby ohne `application.yaml` und ohne `app.json` startet
- **THEN** läuft sie mit den Standardwerten aller Module, und im Arbeitsverzeichnis entsteht keine neue Konfigurationsdatei

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
1. Standardwert des Records,
2. `application.yaml`,
3. Profil-Dateien,
4. externe Datei aus einer Env-Variable bzw. System-Property,
5. Env-Variable,
6. System-Property.

Die Abbildung von Schlüssel auf Env-Variable MUSS dokumentiert sein.

#### Scenario: Env-Variable schlägt Datei
- **WHEN** `application.yaml` `spawn.simulationDistance: 2` enthält und die passende Env-Variable den Wert `4` setzt
- **THEN** sendet die Lobby eine Simulationsdistanz von 4

#### Scenario: System-Property schlägt Env-Variable
- **WHEN** sowohl die Env-Variable als auch die System-Property für `spawn.simulationDistance` gesetzt sind
- **THEN** gilt der Wert der System-Property

### Requirement: Unbekannte Schlüssel werden gemeldet
Enthält ein Abschnitt Schlüssel, die der Config-Record des Moduls nicht kennt, MUSS die Lobby sie beim Start in genau einer Warnung pro Abschnitt nennen und ignorieren. Der Start DARF daran NICHT scheitern.

#### Scenario: Veralteter Schlüssel
- **WHEN** der Abschnitt `elytra` einen Schlüssel `boostMultiplier` enthält
- **THEN** startet die Lobby, und im Log steht eine Warnung, die `elytra` und `boostMultiplier` nennt

### Requirement: Bestehende app.json wird einmalig umgestellt
Findet die Lobby beim Start eine `app.json` (flaches Format v1 oder Abschnitte v2), aber keine `application.yaml`, MUSS sie die Werte einmalig in eine `application.yaml` übernehmen und die `app.json` in `app.json.migrated` umbenennen. Die Lobby MUSS die Umstellung mit einer Warnung im Log melden. Nicht mehr verwendete Schlüssel (`updateRateAgones`, `fireworkBoostSlot`, `elytraBoostMultiplier`) MÜSSEN verworfen und im Log genannt werden. Existiert bereits eine `application.yaml`, DARF die Lobby eine vorhandene `app.json` NICHT anfassen und MUSS sie nur mit einer Warnung melden.

#### Scenario: Umstellung einer flachen app.json
- **WHEN** die Lobby mit einer flachen `app.json` startet, die `"tickleDuration": 4000`, `"elytraBoostMultiplier": 35.0` und `"updateRateAgones": 2000` enthält, und keine `application.yaml` existiert
- **THEN** existiert danach eine `application.yaml` mit `tickle.cooldownMillis: 4000` und ohne `elytraBoostMultiplier` und `updateRateAgones`, die alte Datei heißt `app.json.migrated`, und das Log nennt die verworfenen Schlüssel

#### Scenario: Umstellung einer app.json mit Abschnitten
- **WHEN** die Lobby mit einer `app.json` im Format v2 startet und keine `application.yaml` existiert
- **THEN** enthält die neue `application.yaml` dieselben Abschnitte und Werte, und die Lobby verhält sich genauso wie vorher

#### Scenario: application.yaml existiert bereits
- **WHEN** sowohl `app.json` als auch `application.yaml` vorhanden sind
- **THEN** liest die Lobby nur `application.yaml`, lässt `app.json` unverändert und warnt, dass `app.json` ignoriert wird

## REMOVED Requirements

### Requirement: Fehlende Konfigurationsdatei wird angelegt
**Reason**: Die Lobby liest die Konfiguration nur noch und legt keine Dateien mehr an. Standardwerte leben in den Config-Records, eine kommentierte Beispiel-`application.yaml` wird mitgeliefert.
**Migration**: Ersetzt durch „Ohne Konfigurationsdatei gelten die Standardwerte“. Betreiber kopieren bei Bedarf die Beispieldatei.

### Requirement: Altes flaches Format wird automatisch migriert
**Reason**: Das Zielformat ist nicht mehr `app.json` mit Abschnitten, sondern `application.yaml`.
**Migration**: Ersetzt durch „Bestehende app.json wird einmalig umgestellt“. Das deckt beide Formate (v1 und v2) ab.

### Requirement: Änderungen aus dem Setup-Server verlieren keine Werte
**Reason**: Der Setup-Server bearbeitet keine Konfiguration mehr. Er ist nur noch für Map-Daten zuständig, Konfiguration wird in YAML gepflegt und versioniert.
**Migration**: Werte, die bisher per `/setup app …` gesetzt wurden, trägt man in `application.yaml` oder das passende Profil ein bzw. setzt sie per Env-Variable.
