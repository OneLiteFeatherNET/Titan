# app-variants Specification

## Purpose
Legt fest, wie Titan je Betriebsumgebung als eigene App-Variante gebaut und gestartet wird: Jede Variante bringt genau ihre Columns mit, startet nur vollständig und liefert die Standardwerte aller ihrer Columns einsehbar aus.

## Requirements

### Requirement: Je Betriebsumgebung eine startbare Variante
Titan MUSS für jede Betriebsumgebung als eigene, allein startbare Variante gebaut werden: `cloudnet` für den Produktionsbetrieb und `local` für die Entwicklung. Jede Variante MUSS sich ohne weitere Dateien außer Welten und optionaler Konfiguration mit `java -jar` starten lassen. Die Variante `cloudnet` MUSS zusätzlich mit einem AOT-Cache ausgeliefert werden, der zu genau diesem Build passt.

#### Scenario: Produktionsvariante startet
- **WHEN** ein Betreiber das Jar der Variante `cloudnet` mit dem zugehörigen AOT-Cache startet
- **THEN** startet die Lobby, und der AOT-Cache wird angenommen

#### Scenario: Entwicklungsvariante startet
- **WHEN** ein Entwickler das Jar der Variante `local` ohne CloudNet startet
- **THEN** startet die Lobby mit denselben Funktionen wie die Variante `cloudnet`

### Requirement: Eine Variante startet nur mit allen erwarteten Columns
Jede Variante MUSS festlegen, welche Columns sie enthält. Beim Start MUSS die Lobby prüfen, dass jede erwartete Column geladen ist. Fehlt eine, DARF die Lobby NICHT starten, und die Fehlermeldung MUSS die fehlende Column nennen.

#### Scenario: Column fehlt im Start
- **WHEN** die Variante die Column `sit` erwartet, diese beim Start aber nicht geladen wird
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt `sit`

#### Scenario: Alle Columns vorhanden
- **WHEN** alle erwarteten Columns geladen werden
- **THEN** startet die Lobby und meldet jede Column als gestartet

### Requirement: Varianten verhalten sich wie die bisherige Lobby
Solange sich die Varianten nur im Namen unterscheiden, MÜSSEN beide Varianten dieselben Columns enthalten und sich für Spieler und Betreiber genauso verhalten wie die bisherige einzelne Lobby: gleiche Features, gleiche Konfigurationsschlüssel, gleiche Befehle, gleiche Texte.

#### Scenario: Gleiches Verhalten nach dem Umbau
- **WHEN** ein Spieler die Lobby der Variante `cloudnet` betritt, den Navigator nutzt, sitzt, kitzelt und mit der Elytra fliegt
- **THEN** verhält sich jede Funktion wie vor dem Umbau

#### Scenario: Bestehende Konfiguration gilt weiter
- **WHEN** ein Betreiber seine bisherige `application.yaml` unverändert neben das Jar der Variante legt
- **THEN** übernimmt die Lobby alle Werte daraus wie vorher

### Requirement: Standardwerte kommen aus den Columns der Variante
Jede Column MUSS ihre eigenen Standardwerte mitbringen. Die Variante MUSS die Standardwerte aller ihrer Columns ausliefern und für Betreiber einsehbar als eine kommentierte Beispieldatei neben das Jar legen. Beanspruchen zwei Columns denselben Konfigurationsschlüssel, MUSS der Build fehlschlagen und beide Columns nennen.

#### Scenario: Beispieldatei listet alle Abschnitte
- **WHEN** die Variante gebaut wird
- **THEN** enthält die mitgelieferte Beispieldatei die Standardwerte jeder Column der Variante

#### Scenario: Doppelter Schlüssel
- **WHEN** zwei Columns denselben Konfigurationsschlüssel als Standardwert mitbringen
- **THEN** schlägt der Build fehl und nennt beide Columns und den Schlüssel
