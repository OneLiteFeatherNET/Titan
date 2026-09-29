# Spec Delta

## MODIFIED Requirements

### Requirement: Ungültige Saison-Konfiguration verhindert den Start
Ist eine aktivierte Saison ungültig, MUSS die Lobby den Start abbrechen, und die Fehlermeldung MUSS den vollständigen Schlüssel (`seasons.<id>.<feld>`) und den Grund nennen. Ungültig sind ein fehlender `world`-, `from`- oder `to`-Wert, ein Datum, das sich nicht lesen lässt, `from` nicht vor `to`, ein Weltverzeichnis, das unter `worlds/` fehlt, und ein Weltverzeichnis ohne Map-Datei. Datum-Zeit-Werte MÜSSEN in der YAML-Datei in Anführungszeichen stehen, weil ein unquotierter Wert vom Konfigurationslader nicht als Text geliefert wird und wie ein fehlender gilt. Fehlt `from` oder `to` einer Saison, die sonst Schlüssel hat, MUSS die Meldung (beim Start wie als Warnung zur Laufzeit) den Schlüssel nennen und darauf hinweisen, dass Datum-Zeit-Werte in Anführungszeichen stehen müssen. Auch eine ungültige `seasons.zone` MUSS den Start abbrechen. Eine abgeschaltete Saison DARF den Start nicht verhindern. Ein zur Laufzeit ungültig gewordener Wert DARF weder einen Neustart auslösen noch die Lobby stoppen; er MUSS als Warnung geloggt werden, und die Lobby behält bis zur Korrektur ihren Zustand: kein Neustart wird vorgemerkt, und es gibt keinen Stopp.

#### Scenario: Welt existiert nicht
- **WHEN** `seasons.winter.world` auf `winter` zeigt und `worlds/winter` fehlt
- **THEN** startet die Lobby nicht und nennt `seasons.winter.world` und den Grund

#### Scenario: Welt ohne Map-Datei
- **WHEN** `worlds/winter` existiert, enthält aber keine Map-Datei
- **THEN** startet die Lobby nicht und nennt `seasons.winter.world` und den Grund

#### Scenario: Datum nicht lesbar
- **WHEN** `seasons.winter.from` den Wert `morgen` hat
- **THEN** startet die Lobby nicht und nennt `seasons.winter.from`

#### Scenario: Fenster verkehrt herum
- **WHEN** `seasons.winter.from` nicht vor `seasons.winter.to` liegt
- **THEN** startet die Lobby nicht und nennt `seasons.winter.from` und `seasons.winter.to`

#### Scenario: Abgeschaltete Saison mit fehlender Welt
- **WHEN** `seasons.winter.enabled` `false` ist und `worlds/winter` fehlt
- **THEN** startet die Lobby normal

#### Scenario: Live aktivierte Saison ohne Welt
- **WHEN** die Lobby läuft und `seasons.winter.enabled` im aktiven Fenster auf `true` gesetzt wird, obwohl `worlds/winter` fehlt
- **THEN** merkt die Lobby keinen Neustart vor, loggt eine Warnung mit `seasons.winter.world` und läuft weiter

#### Scenario: Quotiertes Datum wird gelesen
- **WHEN** die YAML-Datei `from: "2026-12-01T00:00:00"` enthält
- **THEN** liest die Lobby den Wert als lokales Datum mit Uhrzeit und startet

#### Scenario: Unquotiertes Datum
- **WHEN** die YAML-Datei `from: 2026-12-01T00:00:00` ohne Anführungszeichen enthält
- **THEN** startet die Lobby nicht, und die Meldung nennt `seasons.winter.from` sowie den Hinweis, Datum-Zeit-Werte in Anführungszeichen zu setzen

#### Scenario: Unquotiertes Datum zur Laufzeit
- **WHEN** die Lobby läuft und ein unquotierter Wert eine aktivierte Saison ungültig macht
- **THEN** merkt die Lobby keinen Neustart vor, und die Warnung nennt den Schlüssel und den Hinweis auf Anführungszeichen
