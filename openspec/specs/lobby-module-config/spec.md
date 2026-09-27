# lobby-module-config Specification

## Purpose
Legt fest, wie jedes Lobby-Modul seine eigene Konfiguration aus den mitgelieferten Standardwerten, `application.yaml`, Profilen und Overrides per Env-Variable oder System-Property erhält und wie ungültige Werte behandelt werden.

## Requirements

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

### Requirement: Konfiguration wird zur Laufzeit neu geladen
Die Dateiüberwachung MUSS standardmäßig ausgeschaltet sein und DARF NUR durch `config.watch.enabled: true` in einer Betreiber-Datei (`application.yaml`, Profil-Datei oder externe Datei) oder per Override eingeschaltet werden. Ist sie eingeschaltet, MUSS die Lobby Änderungen an ihren Konfigurationsdateien, die beim Start existierten, ohne Neustart übernehmen. Sie MUSS geänderte Dateien selbstständig innerhalb eines dokumentierten, konfigurierbaren Intervalls bemerken (`config.watch.delay` für die erste Prüfung nach dem Start, `config.watch.period` danach). Ein Override per Env-Variable oder System-Property gilt nur, solange sein Schlüssel in keiner geänderten Datei erneut geschrieben wird; danach gilt er erst wieder nach dem nächsten Neustart der Lobby.

#### Scenario: Überwachung standardmäßig aus
- **WHEN** die Lobby nur mit den mitgelieferten Standardwerten oder mit einer `application.yaml` ohne `config.watch.enabled: true` startet
- **THEN** wird keine Datei überwacht, eine Änderung wirkt erst nach einem Neustart, und beim Start erscheint deswegen kein ERROR im Log

#### Scenario: Geänderte Datei wird bemerkt
- **WHEN** der Betreiber im laufenden Betrieb in `application.yaml` `tickle.cooldownMillis` von 4000 auf 1000 ändert
- **THEN** gilt für „tickle“ spätestens nach Ablauf des Überwachungsintervalls eine Cooldown-Dauer von 1000 ms, ohne dass die Lobby neu startet

#### Scenario: Override nach Dateiänderung
- **WHEN** die Env-Variable für `spawn.simulationDistance` den Wert 4 setzt und `application.yaml`, die diesen Schlüssel ebenfalls enthält, im Betrieb geändert wird
- **THEN** gilt bis zum nächsten Neustart der Lobby der Dateiwert; nach dem Neustart gilt wieder 4

#### Scenario: Neu angelegte Datei
- **WHEN** `application-dev.yaml` erst nach dem Start der Lobby angelegt wird und das Profil `dev` aktiv ist
- **THEN** wirken ihre Werte erst nach dem nächsten Neustart der Lobby

### Requirement: Geänderte Werte wirken beim nächsten Gebrauch, ohne Neustart
Übernimmt die Dateiüberwachung eine Änderung, MUSS ein geänderter Wert spätestens beim nächsten Mal gelten, wenn er gebraucht wird. Die Lobby DARF dafür WEDER ein Modul NOCH sich selbst neu starten. Laufender Modulzustand (z. B. ein sitzender Spieler, ein laufender Elytra-Flug) DARF davon nicht betroffen sein. Hat sich kein Wert geändert, DARF sich am Verhalten nichts ändern.

#### Scenario: Tickle-Cooldown wirkt beim nächsten Angriff
- **WHEN** im Betrieb `tickle.cooldownMillis` geändert wird und die Dateiüberwachung die Änderung übernimmt
- **THEN** gilt der neue Wert beim nächsten Tickle-Angriff, ohne dass die Lobby oder das Modul „tickle“ neu startet

#### Scenario: Sitzender Spieler bleibt sitzen, wenn sich der Sitz-Versatz ändert
- **WHEN** ein Spieler gerade sitzt und im Betrieb `sit.offset.y` geändert wird und die Dateiüberwachung die Änderung übernimmt
- **THEN** bleibt der Spieler unverändert sitzen, und der neue Versatz gilt erst bei der nächsten Blockinteraktion

#### Scenario: Speichern ohne geänderten Wert
- **WHEN** eine Konfigurationsdatei gespeichert wird, ohne dass sich ein Wert darin ändert
- **THEN** ändert sich am Verhalten der Lobby nichts

### Requirement: Konfigurationswerte werden nur beim Start geprüft
Die Lobby MUSS einen Konfigurationswert nur beim Start prüfen. Ist ein Wert beim Start ungültig, MUSS die Lobby den Start abbrechen; die Fehlermeldung MUSS den Schlüssel und den Grund nennen. Zur Laufzeit MUSS jede Stelle ihren Wert unvalidiert über die `Config`-Fassade lesen, ohne erneute Prüfung und ohne Rückfall auf einen anderen Wert. Ein zur Laufzeit ungültiger, aber parsbarer Wert MUSS so wirken, wie die einfache Verwendung dieses Werts es ergibt. Ein Wert, der sich nicht parsen lässt, DARF genau die betroffene Aktion mit einer Exception fehlschlagen lassen; die Lobby DARF NICHT abstürzen, und andere Einstellungen DÜRFEN NICHT betroffen sein. Ist eine Datei beim Neuladen kein gültiges YAML, protokolliert avaje-config die Datei und die Stelle des Fehlers auf ERROR und übernimmt aus dieser Datei keinen Wert; die Lobby DARF deswegen aus ihr keinen Wert anwenden.

#### Scenario: Ungültiger Wert beim Start bricht den Start ab
- **WHEN** `tickle.cooldownMillis: -5` bereits beim Start in `application.yaml` steht
- **THEN** bricht die Lobby den Start ab, und die Fehlermeldung nennt `tickle.cooldownMillis` und den Grund

#### Scenario: Negative Dauer zur Laufzeit wirkt unvalidiert
- **WHEN** im Betrieb `tickle.cooldownMillis: -5` gesetzt und die Änderung übernommen wird
- **THEN** nutzt „tickle“ beim nächsten Angriff den Wert -5 unvalidiert (wie kein Cooldown), die Lobby läuft ohne Neustart weiter, und es erscheint weder eine WARN-Zeile noch ein Rückfall auf einen Standardwert

#### Scenario: Kaputte Datei beim Neuladen
- **WHEN** `application.yaml` im Betrieb so geändert wird, dass sie kein gültiges YAML mehr ist
- **THEN** bleiben alle bisher gültigen Werte in Kraft, avaje-config nennt im Log die Datei und die Stelle des Fehlers auf ERROR, und aus dieser Datei wird kein Wert übernommen

### Requirement: Feature-Flags sind Teil der Konfiguration
Feature-Flags MÜSSEN als Wahrheitswerte im Abschnitt `features` der Konfiguration stehen, mit dem Namen der Flag als Schlüssel (z. B. `features.NAVIGATOR_SLENDER`). Für sie gelten dieselben Quellen und dieselbe Rangfolge wie für alle anderen Werte. Die mitgelieferten Standardwerte MÜSSEN jede bekannte Flag enthalten, standardmäßig ausgeschaltet. Eine Flag, die nicht in den mitgelieferten Standardwerten steht, ist unbekannt. Eine geänderte Flag MUSS wirken, nachdem die Dateiüberwachung die Änderung übernommen hat, ohne dass ein Modul neu gestartet wird. Die Lobby DARF `flags.properties` NICHT mehr lesen.

#### Scenario: Flag per Profil
- **WHEN** `application-dev.yaml` `features.NAVIGATOR_SLENDER: true` setzt und das Profil `dev` aktiv ist
- **THEN** ist die Flag `NAVIGATOR_SLENDER` an

#### Scenario: Flag per Env-Variable
- **WHEN** die Env-Variable `FEATURES_NAVIGATOR_SLENDER` auf `true` steht und `application.yaml` die Flag nicht setzt
- **THEN** ist die Flag `NAVIGATOR_SLENDER` an

#### Scenario: Flag ohne Eintrag
- **WHEN** weder eine Datei noch ein Override `features.NAVIGATOR_SLENDER` setzt
- **THEN** ist die Flag aus

#### Scenario: Übrig gebliebene flags.properties
- **WHEN** im Arbeitsverzeichnis noch eine `flags.properties` mit `NAVIGATOR_SLENDER=true` liegt und die Konfiguration die Flag nicht setzt
- **THEN** ist die Flag aus

### Requirement: Ein Konfigurationsabschnitt pro einstellbarem Modul
Die Lobby-Konfiguration MUSS aus je einem benannten Abschnitt pro Modul bestehen. Die Grundlage bildet `application.yaml` im Arbeitsverzeichnis, ergänzt durch die Dateien der aktiven Profile und durch Overrides. Der Abschnittsname MUSS der Modul-ID entsprechen. Jeder Schlüssel eines Moduls MUSS unter seinem eigenen Abschnitt liegen (`<modul-id>.<feld>`). Ein Modul DARF KEINE Werte aus dem Abschnitt eines anderen Moduls lesen. Ein Modul ohne einstellbare Werte braucht keinen Abschnitt. Die Schlüssel und ihre Bedeutung MÜSSEN dokumentiert sein.

#### Scenario: Modul liest seinen Abschnitt
- **WHEN** `application.yaml` den Abschnitt `sit` mit `offset: {x: 0.5, y: 0.25, z: 0.5}` enthält
- **THEN** erhält das Modul „sit“ genau diesen Versatz

#### Scenario: Profil ändert nur einen Wert eines Abschnitts
- **WHEN** `application-dev.yaml` nur `sit.offset.y: 0.5` setzt und das Profil `dev` aktiv ist
- **THEN** gilt für „sit“ der Versatz y = 0.5, und x und z behalten ihre Standardwerte

#### Scenario: Modul ohne Abschnitt
- **WHEN** die mitgelieferte `application.yaml` keinen Abschnitt `navigator` enthält und die Lobby startet
- **THEN** startet das Modul „navigator“ ohne Fehler
