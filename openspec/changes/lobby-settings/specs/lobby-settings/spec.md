# Spec Delta

## ADDED Requirements

### Requirement: Columns registrieren Spieleroptionen mit Standardwert
_Each option is a typed boolean key with a fixed default; Columns register it once at startup._

Die Lobby MUSS jede Spieleroption als `BooleanSetting` mit einem Schlüssel im Format `namespace:value` und einem Standardwert bereitstellen. Die Semantik MUSS entweder `OPT_IN` (Standard aus) oder `OPT_OUT` (Standard an) sein. Die Registry MUSS eine Option mit einem bereits belegten Schlüssel beim Start ablehnen und den Start abbrechen.

#### Scenario: Opt-in-Option
- **WHEN** eine Column die Option `titan:tag.enabled` als `OPT_IN` registriert und ein Spieler keinen gespeicherten Wert hat
- **THEN** liefert `isEnabled` für diesen Spieler `false`

#### Scenario: Opt-out-Option
- **WHEN** eine Column die Option `titan:throw.target` als `OPT_OUT` registriert und ein Spieler keinen gespeicherten Wert hat
- **THEN** liefert `isEnabled` für diesen Spieler `true`

#### Scenario: Doppelter Schlüssel
- **WHEN** zwei Columns den Schlüssel `titan:tag.enabled` registrieren
- **THEN** startet die Lobby nicht und meldet den doppelten Schlüssel mit beiden Registrierungen

#### Scenario: Ungültiger Schlüssel
- **WHEN** eine Column einen Schlüssel mit Großbuchstaben im Wert registriert, etwa `titan:Tag`
- **THEN** startet die Lobby nicht und meldet den ungültigen Schlüssel

### Requirement: Das Menü zeigt jede registrierte Option mit ihrem Zustand
_The menu is the only place where players see and change options; it is opened from the hotbar and from the command._

Die Lobby MUSS dem Spieler über das Hotbar-Item in Slot 7 und über `/settings` ein Menü öffnen. Das Menü MUSS für jede registrierte Option ein Vanilla-Symbol-Item in der mittleren Reihe und darunter einen Schalter zeigen, ohne Resource Pack: Der Schalter ist Lime-Farbstoff bei an und Grau-Farbstoff bei aus. Das Symbol glänzt, solange die Option an ist. Der Name des Hotbar-Items und des Menüs sowie die Namen der Optionen laufen im Titan-Gradient und sind nicht kursiv. Das Menü hat höchstens neun Optionen.

#### Scenario: Menü öffnen über das Hotbar-Item
- **WHEN** ein Spieler das Einstellungs-Item in Slot 7 benutzt
- **THEN** öffnet sich das Menü mit einem Symbol und einem Schalter je registrierter Option

#### Scenario: Menü öffnen über den Befehl
- **WHEN** ein Spieler `/settings` eingibt
- **THEN** öffnet sich dasselbe Menü wie über das Hotbar-Item

#### Scenario: Zustand im Menü
- **WHEN** eine Option für den Spieler an ist
- **THEN** zeigt ihr Schalter den Lime-Farbstoff, und ihr Symbol glänzt

#### Scenario: Klick auf Symbol oder Schalter
- **WHEN** ein Spieler das Symbol oder den Schalter einer Option anklickt
- **THEN** schaltet die Option um und ein Klickgeräusch (`ui.button.click`) ist für ihn zu hören

#### Scenario: Sprache des Spielers
- **WHEN** ein deutscher Spieler das Menü öffnet
- **THEN** sind Namen und Beschreibungen auf Deutsch

### Requirement: Ein Klick schaltet die Option um und wirkt sofort
_A click flips the value in memory at once; saving happens in the background._

Ein Klick auf das Item einer Option MUSS den Wert dieser Option für den Spieler umschalten, das Menü auf den neuen Zustand umstellen und sofort die Listener benachrichtigen. Die Umschaltung darf nicht auf das Speichern warten. Andere Optionen des Spielers MÜSSEN unverändert bleiben.

#### Scenario: Umschalten
- **WHEN** ein Spieler eine aus stehende Option anklickt
- **THEN** ist sie an, ihr Schalter zeigt Lime, und `isEnabled` liefert `true`, bevor der Speicherauftrag bei Otis angekommen ist

#### Scenario: Keine Nebenwirkung
- **WHEN** ein Spieler die Option `titan:tag.enabled` umschaltet
- **THEN** bleibt der Wert von `titan:throw.target` unverändert

### Requirement: Der Beitritt wartet nicht auf Otis
_Loading runs off the tick thread; until it completes, the player sees default values._

Das Laden der Einstellungen MUSS beim Verbinden starten und nie den Tick-Thread oder den Beitritt blockieren. Bis das Laden abgeschlossen ist, MÜSSEN alle Optionen ihren Standardwert liefern. Die geladenen Werte MÜSSEN auf dem Tick-Thread übernommen werden.

#### Scenario: Otis antwortet nicht
- **WHEN** ein Spieler der Lobby beitritt und Otis nach dem Timeout nicht antwortet
- **THEN** betritt er die Lobby, und alle Optionen liefern ihren Standardwert

#### Scenario: Otis antwortet spät
- **WHEN** Otis den gespeicherten Wert `true` für `titan:tag.enabled` nach dem Beitritt liefert
- **THEN** ist die Option für den Spieler an, sobald die Übernahme auf dem Tick-Thread läuft

#### Scenario: Spieler verlässt die Lobby während des Ladens
- **WHEN** der Spieler die Verbindung trennt, bevor das Laden abgeschlossen ist
- **THEN** wird das Ergebnis verworfen und nichts übernommen

### Requirement: Lokale Änderungen gewinnen gegen das Laden
_A player who toggles during the load must not see the toggle reverted by stale data._

Wurde eine Option seit Beginn des Ladens lokal umgeschaltet, MUSS die Übernahme dieser Option unterbleiben. Andere Optionen MÜSSEN mit dem Otis-Stand übernommen werden.

#### Scenario: Umschalten während des Ladens
- **WHEN** der Spieler `titan:tag.enabled` umschaltet, bevor das Laden fertig ist, und Otis `false` für diese Option liefert
- **THEN** bleibt die Option an

### Requirement: Änderungen werden verzögert und mit Wiederholung gespeichert
_Write-behind with a debounce: rapid toggles produce one write per option._

Eine Änderung MUSS nach `otis.writeDebounceMillis` gespeichert werden; eine weitere Änderung derselben Option innerhalb dieser Zeit verschiebt das Speichern. Netzfehler und 5xx MÜSSEN bis `otis.writeRetries` wiederholt werden, andere 4xx-Antworten nicht.

#### Scenario: Mehrfaches Umschalten
- **WHEN** ein Spieler dieselbe Option dreimal innerhalb von zwei Sekunden umschaltet
- **THEN** wird genau ein Speicherauftrag mit dem letzten Wert gesendet

#### Scenario: Speichern bei Verlassen
- **WHEN** ein Spieler eine Änderung macht und den Server verlässt, bevor der Debounce abläuft
- **THEN** wird die Änderung sofort gespeichert

#### Scenario: Wiederholung bei Netzfehler
- **WHEN** das Speichern zweimal einen Netzfehler und dann eine Antwort 200 erhält
- **THEN** gilt die Änderung als gespeichert und wird nicht mehr gesendet

#### Scenario: Keine Wiederholung bei 400
- **WHEN** Otis das Speichern mit 400 ablehnt
- **THEN** wird nicht erneut gesendet, und der Fehler wird im Log mit Schlüssel und ohne Wert vermerkt

### Requirement: Ein Ausfall von Otis bleibt ohne Folgen für das Spielen
_Failing calls open a breaker; while it is open the lobby uses memory and defaults only._

Nach `otis.circuitFailureThreshold` Fehlern in Folge MUSS der Breaker öffnen und Anfragen für `otis.circuitOpenMillis` nicht senden. Danach MUSS er genau eine Probeanfrage zulassen. Während der Breaker offen ist, MUSS die Lobby Änderungen nur im Speicher halten. Bereits geladene Werte eines Spielers MÜSSEN erhalten bleiben.

#### Scenario: Breaker öffnet
- **WHEN** drei Ladeanfragen in Folge fehlschlagen
- **THEN** sendet die Lobby für die nächsten 30 Sekunden keine Ladeanfrage mehr

#### Scenario: Probeanfrage nach der Sperre
- **WHEN** die Sperre abgelaufen ist und die Probeanfrage gelingt
- **THEN** ist der Breaker geschlossen und die nächste Änderung wird wieder gespeichert

#### Scenario: Geladene Werte bleiben bei Ausfall
- **WHEN** Otis nach einem erfolgreichen Laden ausfällt
- **THEN** behält der Spieler die geladenen Werte bis zum Verlassen des Servers

### Requirement: Der Speicher ist austauschbar und Offline lauffähig
_Without an Otis URL the lobby stores options in memory and says so at startup._

Ist `otis.url` leer, MUSS die Lobby einen In-Memory-Speicher verwenden, beim Start eine Warnung schreiben und das Telemetrie-Attribut `store` auf `memory` setzen. Mit gesetztem `otis.url` MUSS sie den Otis-Speicher verwenden. Im Speicher gehaltene Werte gehen beim Neustart verloren.

#### Scenario: Lokaler Lauf ohne Otis
- **WHEN** die Lobby mit leerer `otis.url` startet
- **THEN** schreibt sie eine Warnung, dass Einstellungen nicht persistiert werden, und alle Optionen funktionieren im Speicher

#### Scenario: Lauf mit Otis
- **WHEN** die Lobby mit einer `otis.url` startet
- **THEN** lädt und speichert sie über die Otis-Schnittstelle

### Requirement: Die Funktion ist ausschaltbar
_The feature can be switched off per deployment; off means no menu, no item and no Otis traffic._

Ist `settings.enabled` auf `false`, MUSS die Lobby kein Menü, kein Hotbar-Item und keinen Befehl bereitstellen und keine Anfrage an Otis senden. Die Optionen der Columns liefern dann ihren Standardwert.

#### Scenario: Funktion aus
- **WHEN** `settings.enabled` auf `false` steht
- **THEN** hat der Spieler kein Einstellungs-Item in Slot 7, `/settings` ist unbekannt, und es gehen keine Anfragen an Otis

### Requirement: Das Token wird nicht ausgegeben
_The Otis token is a secret: it comes from the environment and never reaches a log or metric._

Das Token für Otis MUSS aus `otis.token` bzw. der Umgebungsvariable `TITAN_OTIS_TOKEN` gelesen werden. Es DARF weder beim Start, in Log-Zeilen, in Fehlermeldungen noch in Telemetrie erscheinen. Die Konfigurationsanzeige MUSS es maskieren.

#### Scenario: Token gesetzt
- **WHEN** die Lobby mit gesetztem Token startet
- **THEN** erscheint das Token in keiner Log-Zeile und in keinem Metrik-Attribut

#### Scenario: Otis verweigert den Zugriff
- **WHEN** Otis eine Anfrage mit 401 oder 403 beantwortet
- **THEN** wird der Fehler mit Status und Schlüssel geloggt, aber ohne Token und ohne Wert

### Requirement: Telemetrie für Laden, Speichern und Umschalten
_Operators need latency, failures and the state of the breaker to judge the feature in production._

Die Lobby MUSS die Ladedauer als Histogramm `titan.settings.load` mit dem Attribut `outcome` erfassen. Sie MUSS Schreibergebnisse als Zähler `titan.settings.write`, Umschaltungen als Zähler `titan.settings.toggled` mit dem Schlüssel und Zustandswechsel des Breakers als Zähler `titan.settings.circuit` erfassen. Metriken DÜRFEN keine Spieler-UUID und keinen Wert enthalten.

#### Scenario: Erfolgreiches Laden
- **WHEN** ein Laden in 120 Millisekunden mit Erfolg endet
- **THEN** wird `titan.settings.load` mit `outcome=ok` und dem Wert 120 erfasst

#### Scenario: Umschalten
- **WHEN** ein Spieler `titan:tag.enabled` umschaltet
- **THEN** wird `titan.settings.toggled` mit dem Attribut `key=titan:tag.enabled` erhöht, ohne Spieler-UUID

### Requirement: Der Befehl ist englisch benannt
_Commands stay in English; only the menu texts are localized._

Der Befehl MUSS `settings` heißen, ohne deutschen Alias. Die Befehlsnamen DÜRFEN nicht übersetzt werden.

#### Scenario: Befehl ohne Alias
- **WHEN** ein Spieler `/einstellungen` eingibt
- **THEN** kennt die Lobby den Befehl nicht und meldet einen unbekannten Befehl
