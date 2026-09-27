# Spec Delta

## ADDED Requirements

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
Die Lobby MUSS einen Konfigurationswert nur beim Start prüfen. Ist ein Wert beim Start ungültig, MUSS die Lobby den Start abbrechen; die Fehlermeldung MUSS den Schlüssel und den Grund nennen. Zur Laufzeit MUSS jede Stelle ihren Wert unvalidiert über die `Config`-Fassade lesen, ohne erneute Prüfung und ohne Rückfall auf einen anderen Wert. Ein zur Laufzeit ungültiger, aber parsbarer Wert MUSS so wirken, wie die einfache Verwendung dieses Werts es ergibt. Ein Wert, der sich nicht parsen lässt, oder ein kaputter Navigator-Eintrag, DARF genau die betroffene Aktion mit einer Exception fehlschlagen lassen; die Lobby DARF NICHT abstürzen, und andere Einstellungen DÜRFEN NICHT betroffen sein. Ist eine Datei beim Neuladen kein gültiges YAML, protokolliert avaje-config die Datei und die Stelle des Fehlers auf ERROR und übernimmt aus dieser Datei keinen Wert; die Lobby DARF deswegen aus ihr keinen Wert anwenden.

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
