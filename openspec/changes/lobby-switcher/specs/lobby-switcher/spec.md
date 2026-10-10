# Spec Delta

## Purpose

Die englische Kurzzeile unter jeder Anforderung ist für `openspec validate --strict` nötig (RFC 2119); maßgeblich ist der deutsche Text darunter.

Legt fest, wie ein Spieler in der Lobby die anderen laufenden Lobbys desselben CloudNet-Tasks mit ihren Spielerzahlen sieht und per Klick in eine davon wechselt, und wann das nur in einer CloudNet-Umgebung und mit eingeschaltetem Flag gilt.

## ADDED Requirements

### Requirement: Einstieg über ein Hotbar-Item
_The lobby MUST place a "Lobbys" item in hotbar slot 8 only when the flag is on, the lobby runs as a CloudNet service and its own identity is known._

Ist das Flag `features.LOBBYSWITCHER` an, läuft die Lobby als CloudNet-Dienst und ist ihre Identität bekannt, MUSS jeder Spieler beim Beitritt das Item „Lobbys“ in Hotbar-Slot 8 erhalten. Ein Klick darauf MUSS die Liste öffnen. Sonst DARF das Item NICHT erscheinen.
#### Scenario: Flag an, CloudNet-Dienst
- **WHEN** das Flag an ist, die Lobby als CloudNet-Dienst startet und ein Spieler beitritt
- **THEN** liegt das Item „Lobbys“ in Hotbar-Slot 8

#### Scenario: Flag aus
- **WHEN** das Flag aus ist und ein Spieler beitritt
- **THEN** liegt kein Item „Lobbys“ in seiner Hotbar

#### Scenario: Lokale Variante
- **WHEN** die lokale Variante ohne CloudNet läuft
- **THEN** gibt es kein Item, kein Inventar und keine Abfrage von Spielerzahlen

### Requirement: Liste der laufenden Lobbys des eigenen Tasks
_The inventory MUST list every running service of the own task, sorted by service name, each with its online/max count._

Ein geöffnetes Inventar MUSS alle laufenden Dienste des Tasks der eigenen Lobby zeigen, sortiert nach Dienstname aufsteigend. Jeder Eintrag MUSS den Dienstnamen und die Spielerzahl als `online/max` zeigen. Dienste, die nicht laufen, DÜRFEN NICHT erscheinen. Dienste anderer Tasks DÜRFEN NICHT erscheinen.

#### Scenario: Drei laufende Lobbys
- **WHEN** der Task drei laufende Dienste `Lobby-2`, `Lobby-1` und `Lobby-3` mit 12/100, 40/100 und 0/100 Spielern hat und ein Spieler das Inventar öffnet
- **THEN** stehen die Einträge in der Reihenfolge `Lobby-1`, `Lobby-2`, `Lobby-3` mit den jeweiligen Zahlen

#### Scenario: Gestoppter Dienst
- **WHEN** ein Dienst des Tasks nicht läuft
- **THEN** erscheint er nicht in der Liste

### Requirement: Zustand jedes Eintrags
_Each entry MUST have exactly one state: current, full, not ready or joinable._

Jeder Eintrag MUSS genau einen Zustand haben: `CURRENT`, `FULL`, `NOT_READY` oder `JOINABLE`. Die eigene Lobby MUSS markiert sein. Ein Eintrag ist voll, wenn die Maximalzahl größer als null ist und die Spielerzahl sie erreicht.
#### Scenario: Eigene Lobby
- **WHEN** die Liste den Dienst der eigenen Lobby zeigt
- **THEN** ist der Eintrag markiert und als „hier“ gekennzeichnet

#### Scenario: Volle Lobby
- **WHEN** ein anderer Dienst 100 von 100 Plätzen belegt
- **THEN** ist der Eintrag als voll gekennzeichnet

#### Scenario: Lobby ohne gemeldete Kapazität
- **WHEN** ein laufender Dienst die Maximalzahl 0 meldet
- **THEN** ist der Eintrag als nicht bereit gekennzeichnet

### Requirement: Wechsel per Klick
_A click on a joinable entry MUST re-check that service and then send the player to it; any other entry MUST NOT send the player._

Ein Klick auf einen beitretbaren Eintrag MUSS den Zieldienst vor dem Wechsel frisch prüfen und, wenn er dann noch beitretbar ist, den Spieler über den bestehenden Weiterleitungsweg (`Deliver`) an genau diesen Dienst schicken und das Inventar schließen. Ist der Zieldienst bei der Prüfung voll, nicht bereit oder nicht mehr laufend, DARF der Spieler NICHT weitergeleitet werden; er MUSS stattdessen die passende Meldung erhalten und das Inventar MUSS den aktuellen Stand zeigen. Ein Klick auf die eigene Lobby DARF den Spieler NICHT weiterleiten; er MUSS eine Meldung erhalten. Die Meldungen MÜSSEN in der Sprache des Spielers erscheinen, mit Englisch als Fallback.

#### Scenario: Beitretbare Lobby
- **WHEN** ein Spieler auf `Lobby-2` klickt, die bei der Prüfung beitretbar ist
- **THEN** wird er an den Dienst `Lobby-2` weitergeleitet und das Inventar schließt sich

#### Scenario: Eigene Lobby
- **WHEN** ein Spieler auf den Eintrag der eigenen Lobby klickt
- **THEN** bleibt er, wo er ist, und erhält die Meldung, dass er schon hier ist

#### Scenario: Volle Lobby
- **WHEN** ein Spieler auf eine Lobby klickt, die bei der Prüfung voll ist
- **THEN** wird er nicht weitergeleitet, und er erhält die Meldung, dass die Lobby voll ist

#### Scenario: Lobby wird zwischen Anzeige und Klick voll
- **WHEN** eine Lobby in der Liste beitretbar war, beim Klick aber 100 von 100 Plätzen belegt
- **THEN** wird der Spieler nicht weitergeleitet, die Meldung „voll“ erscheint und die Liste zeigt den aktuellen Stand

#### Scenario: Lobby ist beim Klick gestoppt
- **WHEN** der Zieldienst beim Klick nicht mehr läuft
- **THEN** wird der Spieler nicht weitergeleitet, er erhält die Meldung „nicht mehr verfügbar“ und die Liste wird aktualisiert

#### Scenario: Prüfung schlägt fehl
- **WHEN** der Provider bei der Klick-Prüfung eine Ausnahme wirft
- **THEN** wird der Spieler nicht weitergeleitet und erhält die Meldung „Lobbys nicht verfügbar“

#### Scenario: Deutsche Sprache
- **WHEN** ein Spieler mit deutscher Client-Sprache auf eine volle Lobby klickt
- **THEN** erscheint die Meldung auf Deutsch

### Requirement: Aktualisierung nur bei offenem Inventar
_The counts MUST refresh at the configured interval while an inventory is open, and MUST NOT be read while none is open._

Die Zahlen MUSS das Inventar im Intervall `lobbyswitcher.refreshSeconds` aktualisieren, solange ein Spieler es offen hat. Ohne offenes Inventar DARF KEINE Abfrage stattfinden. Beim Öffnen MUSS der Stand sofort gelesen werden.
#### Scenario: Zwei Spieler öffnen, einer schließt
- **WHEN** zwei Spieler das Inventar öffnen und einer es schließt
- **THEN** läuft die Aktualisierung weiter, bis auch der zweite es schließt

#### Scenario: Niemand offen
- **WHEN** kein Inventar offen ist
- **THEN** finden keine Abfragen der Spielerzahlen statt

#### Scenario: Ungültiges Intervall
- **WHEN** `lobbyswitcher.refreshSeconds` auf 0 oder den Text `fünf` gesetzt ist
- **THEN** bricht der Start ab und nennt den Schlüssel und den Grund

### Requirement: Dieselbe Zahlenquelle wie die Portale
_The counts MUST come from the same CloudNet provider the portal labels use._

Die Spielerzahlen MÜSSEN aus derselben CloudNet-Anbindung stammen wie die Portal-Labels (`PlayerCounts`-Provider über die Bridge). Die Lobby DARF KEINEN zweiten Zugriffspfad auf CloudNet für Zahlen aufbauen.

#### Scenario: Provider ist die Bridge
- **WHEN** die Lobby als CloudNet-Dienst mit Bridge läuft
- **THEN** kommen die Zahlen der Liste aus dem Provider, den auch die Portal-Labels nutzen

### Requirement: Fehler beim Lesen
_A failing provider MUST keep the last known counts and MUST NOT stop the lobby._

Wirft der Provider oder fehlt er, MUSS das Inventar den letzten gelesenen Stand zeigen. Gab es noch keinen Stand, MUSS es eine Meldung zeigen, dass die Lobbys nicht verfügbar sind, und die eigene Lobby zeigen, sofern sie bekannt ist. Ein Fehler DARF die Lobby NICHT beenden. Ein Fehler MUSS einmal je Art als Warnung ins Log, danach nur auf Debug-Stufe.

#### Scenario: Provider wirft nach einem guten Stand
- **WHEN** der Provider beim Aktualisieren eine Ausnahme wirft
- **THEN** bleibt der letzte Stand sichtbar und die Lobby läuft weiter

#### Scenario: Provider ohne Stand
- **WHEN** der Provider beim ersten Öffnen nichts liefert
- **THEN** erscheint die Meldung „Lobbys nicht verfügbar“ und höchstens die eigene Lobby

### Requirement: Telemetrie
_The lobby MUST emit the spans and the counter named here; the counter MUST NOT carry names or user ids._

Öffnen und Klick MÜSSEN je einen Span erzeugen (`lobbyswitcher.open`, `lobbyswitcher.select`). Der Klick-Span MUSS das Ergebnis (`sent`, `current`, `full`, `not_ready`, `gone` oder `error`) tragen; bei `error` MUSS der Span-Status `ERROR` sein. Der Zähler `titan.lobbyswitcher.selections` MUSS je Klick das Ergebnis tragen und DARF KEINE Dienstnamen und keine `user.id` tragen. Die Aktualisierung DARF KEINEN Span erzeugen.
#### Scenario: Erfolgreicher Wechsel
- **WHEN** ein Spieler auf eine beitretbare Lobby klickt
- **THEN** enthält der Span `lobbyswitcher.select` das Ergebnis `sent` und den Zieldienst, und der Zähler steigt um eins mit `result=sent`

#### Scenario: Metrik ohne Namen
- **WHEN** der Zähler `titan.lobbyswitcher.selections` ausgewertet wird
- **THEN** enthält er nur das Attribut `result`

### Requirement: Texte in der Sprache des Spielers
_All texts MUST come from the bundles with English as fallback, and both bundles MUST have the same keys._

Alle Texte des Inventars und der Meldungen MÜSSEN über Übersetzungsschlüssel `titan.lobbyswitcher.*` in `messages_en.properties` und `messages_de.properties` stehen. Englisch ist der Fallback. Beide Dateien MÜSSEN dieselben Schlüssel haben.

#### Scenario: Fehlende Übersetzung
- **WHEN** ein Spieler eine Client-Sprache ohne eigene Übersetzung hat
- **THEN** erscheinen die Texte auf Englisch

### Requirement: Konfiguration und Standardwerte
_The flag MUST default to true in the CloudNet profile and is read at start only; an invalid refresh value MUST abort the start._

Das Flag `LOBBYSWITCHER` MUSS in den Standardwerten mit `true` stehen und in der Liste der Flags der Lobby enthalten sein. Das Flag wird beim Start gelesen; ein Umschalten wirkt erst nach einem Neustart. Ein ungültiger Wert von `lobbyswitcher.refreshSeconds` MUSS den Start mit Schlüssel und Grund abbrechen.

#### Scenario: Auslieferung ohne Änderung
- **WHEN** die CloudNet-Lobby mit Standardwerten startet
- **THEN** ist das Flag an und das Item liegt auf Slot 8

#### Scenario: Flag nachträglich ausgeschaltet
- **WHEN** das Flag in der Konfiguration ausgeschaltet wird und die Lobby läuft noch
- **THEN** bleibt das Item bis zum Neustart sichtbar; nach dem Neustart fehlt es
