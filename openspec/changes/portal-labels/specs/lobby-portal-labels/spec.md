# Spec Delta

## Purpose

Legt fest, wie die Lobby vor einem Portal ein Label mit Ziel und optionaler Spielerzahl anzeigt: welche Daten die Karte liefert, welche Quellen und Platzhalter es gibt, wie Offline-Fälle, Aktualisierung und ein austauschbarer Zähler arbeiten und welche ungültigen Labels den Start verhindern.

## ADDED Requirements

### Requirement: Ein Portal kann ein Label als Kartendaten tragen
Ein Portal in der Map-Datei (`map.json`) DARF einen optionalen Block `label` haben mit `position` (x, y, z), `text` (MiniMessage), optional `offlineText` (MiniMessage), optional `source` und optional `billboard`. `billboard` MUSS `center` (Standard, die Anzeige dreht sich zum Spieler) oder `fixed` (feste Ausrichtung über `yaw`) sein. Ein Portal ohne `label` DARF keine Anzeige erzeugen und MUSS sich verhalten wie ohne diese Fähigkeit. Das Label MUSS beim Speichern und erneuten Laden der Karte unverändert erhalten bleiben, auch bei Portalen, die ein anderer Befehl ändert.

#### Scenario: Portal mit Label
- **WHEN** die Map-Datei ein Portal „survival“ mit einem `label` an Position (12.5, 66.0, -3.5) enthält
- **THEN** zeigt die Lobby an dieser Position eine Anzeige für dieses Portal

#### Scenario: Portal ohne Label
- **WHEN** ein Portal keinen Block `label` hat
- **THEN** gibt es für dieses Portal keine Anzeige, und das Portal löst weiter wie bisher aus

#### Scenario: Label überlebt das Speichern
- **WHEN** die Karte mit einem Portal samt `label` gespeichert und neu geladen wird, zum Beispiel nach `/setup map setspawn`
- **THEN** steht dasselbe `label` mit denselben Werten wieder im Portal

#### Scenario: Standard-Ausrichtung
- **WHEN** ein `label` kein `billboard` angibt
- **THEN** dreht sich die Anzeige zu jedem Spieler (`center`)

#### Scenario: Feste Ausrichtung
- **WHEN** ein `label` `"billboard": "fixed"` und `"yaw": 90` angibt
- **THEN** ist die Anzeige fest mit dieser Drehung ausgerichtet und dreht sich nicht mit dem Spieler

### Requirement: Die Anzeige ist eine gemeinsame Entität pro Portal
Die Lobby MUSS für jedes Portal mit `label` genau eine Anzeige-Entität erzeugen, die alle Spieler gleich sehen. Die Anzeige DARF NICHT je Spieler verschiedene Texte oder Übersetzungen zeigen. Die Lobby MUSS die Anzeigen beim Herunterfahren des Moduls wieder entfernen.

#### Scenario: Zwei Spieler sehen dasselbe
- **WHEN** zwei Spieler vor einem Portal mit Label stehen
- **THEN** sehen beide denselben Text derselben Entität

#### Scenario: Zwei Portale, zwei Anzeigen
- **WHEN** zwei Portale je ein Label haben
- **THEN** gibt es zwei getrennte Anzeigen mit jeweils eigenem Text

#### Scenario: Modul wird beendet
- **WHEN** das Portal-Modul stoppt
- **THEN** sind alle Anzeigen aus der Welt entfernt

### Requirement: Platzhalter im Text
`text` und `offlineText` MÜSSEN als MiniMessage mit diesen Platzhaltern aufgelöst werden: `<online>` (Spieler der Quelle), `<max>` (Plätze der Quelle; bei `task` und `group` die Summe über alle laufenden Server), `<task>` (Name des Task des Portals) sowie der globale `<prefix>`. Argument-Tags wie `<online:group:x>` MÜSSEN NICHT unterstützt werden. `<online>`, `<max>` und `<task>` DÜRFEN keinen Spielereingaben-Text als Markup auswerten.

#### Scenario: Zahlen einsetzen
- **WHEN** der Text `<gold>Survival<newline><gray><online>/<max> Spieler` lautet und die Quelle 12 von 40 Spielern meldet
- **THEN** zeigt die Anzeige „Survival“ in Gold und darunter „12/40 Spieler“ in Grau

#### Scenario: Task-Name
- **WHEN** der Text `<task>` enthält und das Portal den Task `Survival` hat
- **THEN** steht an dieser Stelle „Survival“

#### Scenario: Globaler Prefix
- **WHEN** der Text `<prefix>` enthält
- **THEN** wird `<prefix>` wie in den übrigen Lobby-Nachrichten aufgelöst

### Requirement: Genau eine Spielerzahl-Quelle je Anzeige
Jedes Label MUSS genau eine Quelle haben: `task` (Summe über alle Server des benannten Ziels), `group` (Summe über alle Server der benannten Gruppe), `service` (genau eine benannte Server-Instanz, die auch nicht existieren darf) oder `local` (nur diese Lobby, ohne Namen). Wenn `source` fehlt, MUSS die Quelle `task` mit dem Task des Portals sein. `task`, `group` und `service` MÜSSEN einen Namen haben; `local` DARF keinen brauchen. Die Quelle `local` MUSS aus der Zahl der verbundenen Spieler dieser Lobby kommen und immer funktionieren; sie kennt keine Platzzahl, `<max>` MUSS dort als `?` erscheinen.

#### Scenario: Task-Quelle
- **WHEN** `source` `{ "type": "task", "name": "Survival" }` ist und drei Server des Ziels mit 10, 5 und 7 Spielern laufen
- **THEN** zeigt `<online>` 22

#### Scenario: Quelle fehlt
- **WHEN** ein Label ohne `source` zu einem Portal mit Task `Survival` gehört
- **THEN** zählt die Anzeige wie mit `{ "type": "task", "name": "Survival" }`

#### Scenario: Einzelner Service
- **WHEN** `source` `{ "type": "service", "name": "Survival-1" }` ist und dieser Server 4 Spieler hat
- **THEN** zeigt `<online>` 4, unabhängig von anderen Servern desselben Ziels

#### Scenario: Gruppe
- **WHEN** `source` `{ "type": "group", "name": "Games" }` ist
- **THEN** zeigt `<online>` die Summe über alle Server der Gruppe „Games“

#### Scenario: Lokale Quelle
- **WHEN** `source` `{ "type": "local" }` ist und 9 Spieler in dieser Lobby sind
- **THEN** zeigt `<online>` 9, auch ohne verfügbaren Zähler-Anbieter

#### Scenario: Lokale Quelle kennt kein Maximum
- **WHEN** `source` `{ "type": "local" }` ist und der Text `<online>/<max>` enthält
- **THEN** zeigt die Anzeige die lokale Zahl, gefolgt von „/?“

### Requirement: Offline-Anzeige
Läuft für die Quelle kein Server, ist kein Zähler-Anbieter verfügbar oder unterstützt der aktive Anbieter den Quellentyp nicht, MUSS die Anzeige `offlineText` zeigen, wenn er gesetzt ist, sonst `text` mit `<online>` und `<max>` gleich 0. Ein Server in der Quelle läuft, sobald der Anbieter ihn als laufend meldet. Die Quelle `local` DARF nie als offline gelten.

#### Scenario: Eigener Offline-Text
- **WHEN** für die Quelle kein Server läuft und `offlineText` `<red>Survival startet gleich` lautet
- **THEN** zeigt die Anzeige „Survival startet gleich“ in Rot

#### Scenario: Ohne Offline-Text
- **WHEN** für die Quelle kein Server läuft und kein `offlineText` gesetzt ist
- **THEN** zeigt die Anzeige den `text` mit 0/0

#### Scenario: Kein Anbieter verfügbar
- **WHEN** die Lobby ohne Cloud-System läuft (lokal oder Setup-Server) und die Quelle `task` ist
- **THEN** gilt die Quelle als nicht laufend und die Anzeige zeigt den Offline-Fall

#### Scenario: Server fährt hoch
- **WHEN** für die Quelle zuerst kein Server läuft und bei einer späteren Aktualisierung einer
- **THEN** wechselt die Anzeige vom Offline-Text zum normalen Text mit den Zahlen

### Requirement: Der Zähler ist austauschbar
Die Spielerzahlen MÜSSEN von einem Zähler-Anbieter stammen, der unabhängig von einem bestimmten Cloud-System ist; ein Cloud-System, Redis oder ein Proxy sind gleichwertige mögliche Anbieter. Der Anbieter beantwortet je Quellentyp (`task`, `group`, `service`) und Name, ob etwas läuft und wie viele Spieler und Plätze es hat. Die Portale und die Anzeige MÜSSEN mit jedem Anbieter funktionieren, ohne dass das Portal-Modul geändert wird. Es MUSS genau ein Anbieter aktiv sein: ein vom Betrieb bereitgestellter Anbieter hat Vorrang vor dem eingebauten Ersatz, mehrere Anbieter werden nicht zusammengeführt. Der eingebaute Ersatz MUSS für `task`, `group` und `service` „läuft nicht“ melden. Unterstützt der aktive Anbieter einen Quellentyp nicht, MUSS die Anzeige den Offline-Fall zeigen und die Lobby MUSS einmal je Quelle eine Warnung ins Log schreiben.

#### Scenario: Anderer Anbieter als CloudNet
- **WHEN** ein Anbieter bereitgestellt ist, der für den Task `Survival` 3 von 20 Spielern meldet, und kein Cloud-System vorhanden ist
- **THEN** zeigt die Anzeige 3/20 für ein Portal mit dieser Quelle

#### Scenario: Anbieter hat Vorrang
- **WHEN** ein Anbieter bereitgestellt ist und der eingebaute Ersatz ebenfalls bereitsteht
- **THEN** stammen die Zahlen vom bereitgestellten Anbieter und nicht vom Ersatz

#### Scenario: Ersatz ohne Anbieter
- **WHEN** kein Anbieter bereitgestellt ist
- **THEN** meldet der Ersatz für `task`, `group` und `service` „läuft nicht“, und `local` zeigt weiter die lokale Zahl

#### Scenario: Nicht unterstützter Quellentyp
- **WHEN** der aktive Anbieter den Typ `group` nicht unterstützt und ein Label `group` verwendet
- **THEN** zeigt die Anzeige den Offline-Fall und das Log enthält genau eine Warnung für diese Quelle

#### Scenario: Unbekannter Name zur Laufzeit
- **WHEN** der Anbieter den Task eines Labels nicht kennt
- **THEN** startet die Lobby trotzdem, die Anzeige zeigt den Offline-Fall und das Log enthält genau eine Warnung für diese Quelle

### Requirement: Regelmäßige Aktualisierung ohne unnötigen Verkehr
Die Lobby MUSS die Spielerzahlen aller Anzeigen in einem einstellbaren Takt neu abfragen; der Standard sind 5 Sekunden, einstellbar über den Konfigurationsschlüssel `portal.labelRefreshSeconds` des Portal-Moduls. Ein Wert unter 1 oder ein Wert, der keine ganze Zahl ist, MUSS den Start abbrechen und den Schlüssel und den Grund nennen. Die Lobby DARF die Anzeige an die Clients nur senden, wenn sich der gerenderte Text gegenüber dem zuletzt gesendeten geändert hat. Die Abfrage DARF den Server-Tick nicht blockieren.

#### Scenario: Zahl ändert sich
- **WHEN** die Spielerzahl der Quelle von 12 auf 13 steigt und der nächste Takt abläuft
- **THEN** zeigt die Anzeige 13

#### Scenario: Zahl bleibt gleich
- **WHEN** der Takt abläuft und der gerenderte Text identisch zum letzten ist
- **THEN** werden keine neuen Metadaten gesendet

#### Scenario: Standardtakt
- **WHEN** `portal.labelRefreshSeconds` nicht gesetzt ist
- **THEN** fragt die Lobby alle 5 Sekunden ab

#### Scenario: Ungültiger Takt
- **WHEN** `portal.labelRefreshSeconds` 0 oder `abc` ist
- **THEN** startet die Lobby nicht und meldet `portal.labelRefreshSeconds` mit dem Grund

### Requirement: Ungültige Labels verhindern den Start
Die Lobby MUSS den Start abbrechen, wenn ein Label ungültig ist, so wie bei ungültigen Portalen: ungültiges MiniMessage in `text` oder `offlineText`, fehlende `position`, ein unbekannter `source.type`, ein fehlender Name bei `task`, `group` oder `service`, ein unbekanntes `billboard`. Die Fehlermeldung MUSS das Portal und den Grund nennen. Das Vokabular von `source.type` ist fest: `task`, `group`, `service`, `local`; ein Anbieter erweitert es nicht.

#### Scenario: Ungültiges MiniMessage
- **WHEN** `text` eines Labels `<gold>Survival</red>` ist (Tag falsch geschlossen)
- **THEN** startet die Lobby nicht und nennt das Portal und `text`

#### Scenario: Unbekannter Quellentyp
- **WHEN** `source.type` `proxy` ist
- **THEN** startet die Lobby nicht und nennt das Portal und den Typ

#### Scenario: Name fehlt
- **WHEN** `source.type` `task` ist und `name` fehlt
- **THEN** startet die Lobby nicht und nennt das Portal und den fehlenden Namen

#### Scenario: Lokale Quelle ohne Namen
- **WHEN** `source.type` `local` ist und kein `name` angegeben ist
- **THEN** ist das Label gültig

#### Scenario: Fehlende Position
- **WHEN** ein Label keine `position` hat
- **THEN** startet die Lobby nicht und nennt das Portal und die fehlende Position
