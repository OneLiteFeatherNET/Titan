# lobby-daytime Specification

## Purpose
Legt fest, wie die Tageszeit der Lobby der echten Uhrzeit folgt: linear nach der Wanduhr einer konfigurierbaren Zeitzone, abschaltbar und ohne Neustart umstellbar.

## Requirements

### Requirement: Die Lobby folgt der Wanduhr der konfigurierten Zeitzone
Die Tageszeit der Lobby MUSS linear der lokalen Uhrzeit der konfigurierten Zeitzone entsprechen: 06:00 Uhr ist Sonnenaufgang, 12:00 Uhr Mittag, 18:00 Uhr Sonnenuntergang und 00:00 Uhr Mitternacht. Der Wert MUSS im Bereich eines Minecraft-Tages (0 bis unter 24000 Ticks) liegen und über 24 Stunden genau einmal umlaufen. Der Standard der Zeitzone MUSS `Europe/Berlin` sein.

#### Scenario: Ankerzeiten
- **WHEN** es in der konfigurierten Zeitzone 06:00, 12:00, 18:00 bzw. 00:00 Uhr ist
- **THEN** steht die Instanzzeit auf 0, 6000, 12000 bzw. 18000 Ticks

#### Scenario: Zwischenwert
- **WHEN** es in der konfigurierten Zeitzone 09:00 Uhr ist
- **THEN** steht die Instanzzeit auf 3000 Ticks

#### Scenario: Andere Zeitzone
- **WHEN** die Zeitzone auf `Asia/Tokyo` steht und es dort 06:00 Uhr ist
- **THEN** steht die Instanzzeit auf 0 Ticks, unabhängig von der Uhrzeit in Berlin

### Requirement: Der Minestom-Zeitzyklus ist angehalten
Die Lobby DARF die Zeit NICHT von selbst weiterlaufen lassen. Zwischen zwei Aktualisierungen MUSS die Instanzzeit konstant bleiben, damit ausschließlich die Wanduhr sie bestimmt.

#### Scenario: Zeit läuft nicht von allein
- **WHEN** der Server Ticks verarbeitet, ohne dass eine Aktualisierung fällig ist
- **THEN** ändert sich die Instanzzeit nicht

### Requirement: Die Zeit stammt aus der Uhr der Plattform
Die Lobby MUSS die Uhrzeit aus der Uhr beziehen, die die Plattform bereitstellt, nicht aus einer eigenen Zeitquelle. Wird diese Uhr ersetzt, MUSS die Instanzzeit dem ersetzten Wert folgen.

#### Scenario: Ersetzte Uhr
- **WHEN** die bereitgestellte Uhr 18:00 Uhr in der konfigurierten Zeitzone anzeigt
- **THEN** steht die Instanzzeit nach der nächsten Aktualisierung auf 12000 Ticks

### Requirement: Zeitumstellungen springen mit der Wanduhr
Bei Zeitumstellungen MUSS die Instanzzeit der lokalen Wanduhr folgen. Beim Vorstellen der Uhr (Frühjahr) MUSS die Zeit nach vorn springen, ohne dass sie sich rückwärts bewegt. Beim Zurückstellen (Herbst) wiederholt sich die Uhrzeit der doppelten Stunde, die Instanzzeit MUSS dabei ebenfalls dem Wanduhr-Wert dieser Stunde folgen. Außerhalb der Umstellung DARF die Zeit NICHT rückwärts springen, und ein Neustart DARF sie nicht verschieben.

#### Scenario: Vorstellen der Uhr
- **WHEN** in `Europe/Berlin` die Uhr von 01:59:59 auf 03:00:00 Uhr vorgestellt wird
- **THEN** springt die Instanzzeit von dem Wert für 01:59:59 auf den Wert für 03:00:00 nach vorn und bewegt sich zu keinem Zeitpunkt rückwärts

#### Scenario: Zurückstellen der Uhr
- **WHEN** in `Europe/Berlin` die Uhr von 02:59:59 Sommerzeit auf 02:00:00 Winterzeit zurückgestellt wird
- **THEN** entspricht die Instanzzeit danach dem Wert für 02:00:00 Uhr

#### Scenario: Neustart verschiebt nichts
- **WHEN** die Lobby zur selben Uhrzeit einmal früher und einmal später gestartet wird
- **THEN** steht die Instanzzeit nach dem Start in beiden Fällen auf demselben Wert

### Requirement: Aktualisierung höchstens einmal pro Sekunde
Die Lobby MUSS die Instanzzeit einmal pro Sekunde aktualisieren und DARF sie NICHT öfter setzen.

#### Scenario: Takt
- **WHEN** die Lobby 100 Ticks lang läuft
- **THEN** wurde die Instanzzeit höchstens fünfmal gesetzt

### Requirement: Abschalter für die Echtzeit
Steht `daytime.enabled` auf `false`, MUSS die Lobby innerhalb einer Sekunde auf Mittag (6000 Ticks) stehen und dort bleiben. Steht der Wert wieder auf `true`, MUSS die Lobby ohne Neustart der Wanduhr folgen. Der Standard MUSS `true` sein.

#### Scenario: Abschalten
- **WHEN** die Lobby der Wanduhr folgt und `daytime.enabled` zur Laufzeit auf `false` gesetzt wird
- **THEN** steht die Instanzzeit nach der nächsten Aktualisierung auf 6000 Ticks

#### Scenario: Wieder einschalten
- **WHEN** `daytime.enabled` von `false` zur Laufzeit auf `true` gesetzt wird
- **THEN** folgt die Instanzzeit nach der nächsten Aktualisierung wieder der Wanduhr

### Requirement: Zeitzone ist ohne Neustart änderbar und wird geprüft
Die Zeitzone MUSS aus `daytime.zone` gelesen werden, und eine Änderung zur Laufzeit MUSS mit der nächsten Aktualisierung wirken. Ist die Zeitzone beim Start ungültig, MUSS die Lobby den Start abbrechen; die Fehlermeldung MUSS `daytime.zone` und den Grund nennen. Wird sie zur Laufzeit auf einen ungültigen Wert geändert, MUSS die Lobby die zuletzt gültige Zeitzone weiterverwenden und eine Warnung mit `daytime.zone` protokollieren.

#### Scenario: Zeitzone wechselt zur Laufzeit
- **WHEN** `daytime.zone` von `Europe/Berlin` auf `America/New_York` geändert wird
- **THEN** folgt die Instanzzeit nach der nächsten Aktualisierung der Wanduhr in New York

#### Scenario: Ungültige Zeitzone beim Start
- **WHEN** die Lobby mit `daytime.zone: Mars/Olympus` startet
- **THEN** bricht der Start ab, und die Fehlermeldung nennt `daytime.zone`

#### Scenario: Ungültige Zeitzone zur Laufzeit
- **WHEN** `daytime.zone` zur Laufzeit auf `Mars/Olympus` geändert wird
- **THEN** bleibt die zuletzt gültige Zeitzone in Kraft, und das Log enthält eine Warnung mit `daytime.zone`
