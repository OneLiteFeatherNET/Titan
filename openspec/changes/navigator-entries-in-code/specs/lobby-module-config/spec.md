# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Ein Konfigurationsabschnitt pro Modul
**Reason**: Das Beispielszenario „Liste von Einträgen“ beschrieb Navigator-Ziele in der Konfiguration, die es nicht mehr gibt. Ersetzt durch „Ein Konfigurationsabschnitt pro einstellbarem Modul“ mit gleichem Inhalt, ergänzt darum, dass ein Modul ohne einstellbare Werte keinen Abschnitt braucht.
**Migration**: Keine für Module mit Abschnitt. `navigator.*` aus eigenen Konfigurationsdateien entfernen.
