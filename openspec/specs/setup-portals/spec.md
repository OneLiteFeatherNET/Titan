# setup-portals Specification

## Purpose
Legt fest, wie das Build-Team im Setup-Server Lobby-Portale mit Befehlen und einem geführten Ablauf anlegt, ändert, auflistet, entfernt und anzeigt (mit Live-Vorschau und Tab-Vervollständigung) und wie nur gültige Portale in die Map-Datei der aktuellen Welt gelangen.

## Requirements

### Requirement: Portale werden mit /setup portal verwaltet
Der Setup-Server MUSS den Befehl `/setup portal` für Spieler anbieten. Ein Portal MUSS über eine Id angesprochen werden, die aus Kleinbuchstaben, Ziffern, `-` und `_` besteht; `list`, `show` und `create` MÜSSEN als Id abgelehnt werden. Jede Änderung wirkt auf die Map-Datei der Welt, die der Setup-Server geladen hat. Alle Bearbeitungsbefehle (`pos1`, `pos2`, `shape`, `centre`, `radius`, `disc`, `task`, `permission`) MÜSSEN nur den Entwurf des Spielers ändern und mit dem Entwurfsstand antworten: was noch fehlt, oder bei einem vollständigen Entwurf (Form und Aufgabe) den Hinweis „vollständig" mit einem anklickbaren Knopf [save]. Ein Portal MUSS ausschließlich durch `/setup portal <id> save` gespeichert werden, für den geführten Ablauf und für Befehle gleichermaßen.

#### Scenario: Unvollständiger Entwurf meldet, was fehlt
- **WHEN** ein Spieler `/setup portal survival pos1` ausführt und für „survival“ noch keine zweite Ecke und keine Aufgabe gesetzt ist
- **THEN** wird nichts gespeichert und der Chat nennt, was noch fehlt (zweite Ecke, Aufgabe)

#### Scenario: Vollständiger Entwurf wird nicht ohne save gespeichert
- **WHEN** für „survival“ zwei Ecken gesetzt sind und der Spieler `/setup portal survival task Survival` ausführt
- **THEN** ist der Entwurf vollständig, der Chat zeigt „vollständig" mit [save], und die `map.json` bleibt unverändert

#### Scenario: save speichert den vollständigen Entwurf
- **WHEN** der Entwurf „survival" vollständig und gültig ist und der Spieler `/setup portal survival save` ausführt
- **THEN** steht das Portal in der `map.json` der Welt und der Chat bestätigt das Speichern

#### Scenario: Änderung an gespeichertem Portal bleibt Entwurf
- **WHEN** das gespeicherte Portal „survival“ vorliegt und der Spieler `/setup portal survival permission titan.portal.survival` ausführt
- **THEN** steht das neue Recht erst nach `save` in der `map.json`

#### Scenario: Reservierte oder ungültige Id
- **WHEN** ein Spieler `/setup portal list pos1`, `/setup portal create pos1` oder `/setup portal Survival! pos1` ausführt
- **THEN** lehnt der Chat die Id ab und nichts wird gespeichert

#### Scenario: Konsole
- **WHEN** die Konsole `/setup portal list` ausführt
- **THEN** wird der Befehl wie alle `/setup`-Befehle abgelehnt

### Requirement: Nur save speichert, cancel verwirft
`/setup portal <id> save` MUSS den Entwurf des Spielers auf Vollständigkeit, die Regeln des Befehls und `PortalValidator.problems(...)` für diese Id prüfen und dann in die Map-Datei schreiben. Ist der Entwurf unvollständig oder ungültig, MUSS der Chat die Gründe nennen, der Entwurf offen bleiben und die Map-Datei unverändert. `/setup portal <id> cancel` MUSS den Entwurf verwerfen, ohne die Map-Datei zu ändern.

#### Scenario: save bei unvollständigem Entwurf
- **WHEN** dem Entwurf die Aufgabe fehlt und der Spieler `save` ausführt
- **THEN** nennt der Chat die fehlende Aufgabe, der Entwurf bleibt offen und nichts wird gespeichert

#### Scenario: save mit Problemen
- **WHEN** der Entwurf vollständig ist, `PortalValidator.problems(...)` für die Id aber ein Problem liefert
- **THEN** nennt der Chat den Grund, der Entwurf bleibt offen und die Map-Datei bleibt unverändert

#### Scenario: cancel verwirft
- **WHEN** ein Spieler `cancel` für einen offenen Entwurf ausführt
- **THEN** ist der Entwurf weg und die Map-Datei unverändert

### Requirement: Ein Quader entsteht aus zwei Ecken
`/setup portal <id> pos1` und `pos2` MÜSSEN die Blockposition des Spielers (ganzzahlig, abgerundet) als erste bzw. zweite Ecke merken. Der Quader MUSS die achsenparallele, blockinklusive Spanne der beiden Ecken sein, unabhängig davon, welche Ecke größer ist. Die Ecken gehören dem ausführenden Spieler; Ecken eines anderen Spielers zählen nicht. Setzt ein Spieler `pos1` oder `pos2` für ein Portal, das ein Ring ist, MUSS die Form beim Vervollständigen zum Quader werden.

#### Scenario: Zwei Ecken bilden einen Quader
- **WHEN** ein Spieler auf Block (14, 68, 11) `pos1` und auf Block (10, 64, 10) `pos2` für „survival“ ausführt, eine Aufgabe gesetzt ist und er `save` ausführt
- **THEN** speichert das Portal einen Quader von (10, 64, 10) bis (14, 68, 11), beide Blöcke eingeschlossen

#### Scenario: Ecken zweier Spieler
- **WHEN** Spieler A `pos1` und Spieler B `pos2` für dieselbe Id ausführt
- **THEN** ist der Quader unvollständig und nichts wird gespeichert

#### Scenario: Neue Ecke ersetzt die alte
- **WHEN** ein Spieler `pos1` für „survival“ zweimal an verschiedenen Blöcken ausführt
- **THEN** zählt die zuletzt gesetzte Ecke

### Requirement: Ein Ring entsteht aus Standort, Blickrichtung und Radius
`/setup portal <id> disc <radius>` MUSS einen Ring anlegen, dessen Mittelpunkt die Augenposition des Spielers ist, auf ein halbes Feld gerundet (jede Koordinate auf das nächste Vielfache von 0,5), und dessen Normale die normierte Blickrichtung ist. Liegt die Blickrichtung höchstens 5 Grad neben einer der sechs Achsenrichtungen, MUSS die Normale auf diese Achse einrasten. Der Radius MUSS eine Zahl größer null sein; mit `disc` ersetzt der neue Ring eine vorhandene Form des Portals. `/setup portal <id> centre` MUSS Mittelpunkt und Normale wie `disc` festlegen, ohne den Radius zu ändern, und `/setup portal <id> radius <radius>` MUSS nur den Radius des Ring-Entwurfs setzen (Mittelpunkt und Normale bleiben); ein Ring ohne Radius ist unvollständig.

#### Scenario: Blick fast entlang einer Achse
- **WHEN** ein Spieler am Augenpunkt (0,52 | 72,0 | 40,47) mit einer Blickrichtung 2 Grad neben +Z `/setup portal elytra-ring disc 5.5` ausführt
- **THEN** hat der Ring den Mittelpunkt (0,5 | 72,0 | 40,5), den Radius 5,5 und die Normale (0, 0, 1)

#### Scenario: Schräge Blickrichtung
- **WHEN** die Blickrichtung mehr als 5 Grad von jeder Achse abweicht
- **THEN** ist die Normale die normierte Blickrichtung ohne Einrasten

#### Scenario: Ungültiger Radius
- **WHEN** ein Spieler `/setup portal elytra-ring disc 0`, `disc -2` oder `disc abc` ausführt
- **THEN** nennt der Chat den Fehler und das Portal ändert sich nicht

### Requirement: Aufgabe und Recht werden je Portal gesetzt
`/setup portal <id> task <task>` MUSS die Aufgabe (Ziel) des Portals setzen; eine leere Aufgabe MUSS abgelehnt werden. `/setup portal <id> permission <recht>` MUSS das Recht setzen, `permission none` MUSS es entfernen (das Portal ist dann für alle offen).

#### Scenario: Aufgabe setzen
- **WHEN** ein Spieler `/setup portal survival task Survival` ausführt
- **THEN** ist „Survival“ die Aufgabe von „survival“

#### Scenario: Recht entfernen
- **WHEN** „survival“ ein Recht hat und der Spieler `/setup portal survival permission none` ausführt
- **THEN** hat das Portal kein Recht mehr

#### Scenario: Recht auf unbekanntem Portal
- **WHEN** ein Spieler `permission` für eine Id ausführt, die es weder gespeichert noch als eigenen Entwurf gibt
- **THEN** wird für diese Id ein Entwurf angelegt, der ohne Form und Aufgabe nicht gespeichert wird

### Requirement: Portale werden aufgelistet und entfernt
`/setup portal list` MUSS alle gespeicherten Portale der Welt mit Id, Form, Aufgabe und Recht anzeigen und darunter in einem eigenen Abschnitt „Entwürfe" die offenen Entwürfe des Spielers mit Id und Stand (was fehlt, oder „vollständig, nicht gespeichert"). `/setup portal <id> remove` MUSS das gespeicherte Portal aus der Map-Datei löschen und den Entwurf des Spielers verwerfen.

#### Scenario: Liste
- **WHEN** die Welt zwei Portale hat und ein Spieler `/setup portal list` ausführt
- **THEN** zeigt der Chat beide mit Id, Form, Aufgabe und Recht

#### Scenario: Entwürfe getrennt
- **WHEN** die Welt ein Portal hat und der Spieler einen offenen Entwurf „arena" mit fehlender Aufgabe
- **THEN** zeigt `list` das Portal und getrennt davon den Entwurf „arena" mit „Aufgabe fehlt"

#### Scenario: Leere Liste
- **WHEN** die Welt kein Portal und der Spieler keinen Entwurf hat
- **THEN** meldet der Chat, dass es keine gibt

#### Scenario: Entfernen
- **WHEN** ein Spieler `/setup portal survival remove` ausführt
- **THEN** ist „survival“ nicht mehr in der `map.json` und aus der Liste verschwunden

#### Scenario: Unbekannte Id
- **WHEN** ein Spieler `remove` für eine unbekannte Id ausführt
- **THEN** nennt der Chat, dass es die Id nicht gibt, und nichts ändert sich

#### Scenario: Radius ändert nur den Radius
- **WHEN** ein Ring-Entwurf einen Mittelpunkt hat, der Spieler sich bewegt und `/setup portal elytra-ring radius 8` ausführt
- **THEN** ist der Radius 8, Mittelpunkt und Normale sind unverändert

### Requirement: Umrisse werden für den Spieler angezeigt
`/setup portal show` MUSS die Umrisse aller gespeicherten Portale der Welt als Partikel nur dem ausführenden Spieler zeigen: bei einem Quader die zwölf Kanten der blockinklusiven Spanne, bei einem Ring den Kreis um den Mittelpunkt in der Ebene zur Normalen. `show` gilt für gespeicherte Portale; offene Entwürfe zeigt die Live-Vorschau. Die Anzeige MUSS nach wenigen Sekunden von selbst enden; ein erneutes `show` MUSS die laufende Anzeige desselben Spielers ersetzen.

#### Scenario: Quader
- **WHEN** ein Spieler `show` ausführt und die Welt einen Quader hat
- **THEN** sieht nur dieser Spieler Partikel entlang der zwölf Kanten, die den ganzen Block der Ecken einschließen

#### Scenario: Ring
- **WHEN** die Welt einen Ring hat
- **THEN** sieht der Spieler Partikel auf einem Kreis mit dem Radius des Rings in der Ebene senkrecht zur Normalen

#### Scenario: Ende der Anzeige
- **WHEN** die Anzeigedauer abgelaufen ist
- **THEN** erscheinen keine Partikel mehr

#### Scenario: Keine Portale
- **WHEN** die Welt kein Portal hat
- **THEN** meldet der Chat, dass es nichts anzuzeigen gibt

### Requirement: Ein geführter Ablauf leitet mit Knöpfen durch das Anlegen
`/setup portal create <id>` MUSS für den Spieler einen geführten Ablauf im Chat starten, der auf dem Entwurf des Spielers für diese Id beruht (bei einer vorhandenen Id vorbefüllt). Jeder Schritt MUSS anklickbare Knöpfe zeigen, die genau einen der Unterbefehle `/setup portal <id> shape box|ring`, `pos1`, `pos2`, `centre`, `radius <r>`, `task <t>`, `permission <p|none>`, `save` oder `cancel` ausführen; der Ablauf darf keine Änderung vornehmen, die nicht über diese Befehle geht. Knöpfe mit freier Eingabe MÜSSEN den Befehl im Chat-Eingabefeld vorschlagen. Die Schritte sind: Form wählen (Box, Ring); Box: erste Ecke, zweite Ecke; Ring: Mittelpunkt (Spieler steht in der Mitte und schaut durch den Ring), Radius (Knöpfe mit Vorschlägen und freie Eingabe); Aufgabe (Knöpfe mit den Aufgaben, die vorhandene Portale der Welt verwenden, und freie Eingabe); Recht (Knopf „keins“ und freie Eingabe); Zusammenfassung mit `save` und `cancel`. Nach jedem ausgeführten Schritt MUSS der nächste Schritt erscheinen. Gespeichert wird wie bei Befehlen nur durch `save`; `cancel` und das Trennen des Spielers MÜSSEN den Entwurf verwerfen.

#### Scenario: Ablauf startet
- **WHEN** ein Spieler `/setup portal create survival` ausführt
- **THEN** zeigt der Chat die Formwahl mit den Knöpfen [Box] und [Ring], die `shape box` bzw. `shape ring` für „survival“ ausführen

#### Scenario: Box-Schritte
- **WHEN** der Spieler [Box] geklickt hat
- **THEN** fordert der Chat auf, an der ersten Ecke zu stehen, mit dem Knopf [set corner 1] für `pos1`; nach `pos1` erscheint [set corner 2] für `pos2`

#### Scenario: Ring-Schritte
- **WHEN** der Spieler [Ring] geklickt hat
- **THEN** fordert der Chat auf, in der Mitte zu stehen und durch den Ring zu schauen, mit dem Knopf [set centre] für `centre`; danach erscheinen Radius-Knöpfe, die `radius <r>` ausführen, und ein Vorschlag für freie Eingabe

#### Scenario: Aufgabe und Recht
- **WHEN** die Form vollständig ist und vorhandene Portale der Welt die Aufgaben „Survival“ und „Creative“ verwenden
- **THEN** bietet der Chat [Survival] und [Creative] (führen `task Survival` bzw. `task Creative` aus) und einen Knopf, der `task ` im Eingabefeld vorschlägt; danach [none] für `permission none` und einen Knopf für `permission `

#### Scenario: Zusammenfassung und Speichern
- **WHEN** Form, Aufgabe und (optional) Recht gesetzt sind
- **THEN** zeigt der Chat eine Zusammenfassung mit [save] und [cancel]; erst [save] schreibt das Portal in die `map.json`, davor nicht

#### Scenario: Abbrechen
- **WHEN** der Spieler [cancel] klickt
- **THEN** ist der Entwurf verworfen, nichts wird gespeichert und die Vorschau endet

#### Scenario: Trennen im Ablauf
- **WHEN** der Spieler mitten im Ablauf den Server verlässt
- **THEN** ist sein Entwurf verworfen

#### Scenario: Benutzertext in Knöpfen
- **WHEN** eine vorhandene Aufgabe Zeichen wie `<` oder `'` enthält
- **THEN** führt ihr Knopf `task` mit genau diesem Text aus, ohne dass MiniMessage ihn auswertet

### Requirement: Eine Live-Vorschau zeigt den offenen Entwurf
Solange ein Spieler einen offenen Entwurf hat, MUSS der Setup-Server ihm den Entwurf als Partikel zeigen, nur diesem Spieler, in einem festen Intervall von 5 Ticks: gesetzte Ecken; bei einem Quader mit einer Ecke den Quader von dieser Ecke bis zum aktuellen Block des Spielers (folgt dem Spieler), bei zwei Ecken den Quader; bei einem Ring den Kreis um den Mittelpunkt mit dem gewählten Radius in der Ebene senkrecht zur Normalen. Solange der Radius fehlt, MUSS die Vorschau den Ring mit Blickrichtung, Standardradius 3 und dem Hinweis im Chat zeigen, dass es der Standardradius ist. Die Punktzahl je Vorschau MUSS begrenzt sein. Die Vorschau MUSS enden, wenn der Entwurf gespeichert, abgebrochen oder entfernt wird oder der Spieler den Server verlässt; je Spieler läuft höchstens eine.

#### Scenario: Nur der Entwurfsspieler sieht sie
- **WHEN** Spieler A einen offenen Entwurf hat und Spieler B in der Nähe steht
- **THEN** erhält nur A Partikelpakete

#### Scenario: Quader folgt dem Spieler
- **WHEN** ein Quader-Entwurf nur `pos1` hat und der Spieler sich bewegt
- **THEN** zeigt die nächste Aktualisierung den Quader von `pos1` bis zum neuen Block des Spielers

#### Scenario: Ring mit Standardradius
- **WHEN** ein Ring-Entwurf noch keinen Radius hat
- **THEN** zeigt die Vorschau einen Kreis mit Radius 3 in der Ebene senkrecht zur Blickrichtung, und der Chat weist auf den Standardradius hin

#### Scenario: Ring mit gewähltem Radius
- **WHEN** der Mittelpunkt und der Radius 5,5 gesetzt sind
- **THEN** zeigt die Vorschau den Kreis mit Radius 5,5 um den Mittelpunkt, in der Ebene senkrecht zur gespeicherten Normalen, unabhängig von der Blickrichtung

#### Scenario: Vorschau endet
- **WHEN** der Entwurf gespeichert oder abgebrochen wird oder der Spieler den Server verlässt
- **THEN** läuft kein Vorschau-Task mehr und es gehen keine Partikel mehr an den Spieler

#### Scenario: Punktzahl begrenzt
- **WHEN** der Entwurf sehr groß ist (großer Quader oder Radius)
- **THEN** überschreitet die Zahl der Punkte je Aktualisierung die Obergrenze nicht

### Requirement: Tab-Vervollständigung unterstützt die Eingabe
`/setup portal` MUSS Vorschläge liefern: an der Id-Position die Ids der gespeicherten Portale der aktuellen Welt und der offenen Entwürfe des Spielers (sowie die Verben `list`, `show`, `create`); nach der Id die Verben (`pos1`, `pos2`, `disc`, `centre`, `radius`, `shape`, `task`, `permission`, `remove`, `save`, `cancel`); nach `task` die Aufgaben, die vorhandene Portale verwenden; nach `disc` und `radius` Radius-Hinweise; nach `permission` den Wert `none`; nach `shape` `box` und `ring`.

#### Scenario: Ids
- **WHEN** die Welt die Portale „survival“ und „creative“ hat, der Spieler einen Entwurf „arena“ hat und `/setup portal ` vervollständigt
- **THEN** enthalten die Vorschläge „survival“, „creative“ und „arena“ sowie `list`, `show`, `create`

#### Scenario: Verben
- **WHEN** der Spieler `/setup portal survival ` vervollständigt
- **THEN** enthalten die Vorschläge `pos1`, `pos2`, `disc`, `task`, `permission`, `remove`

#### Scenario: Aufgaben
- **WHEN** vorhandene Portale die Aufgaben „Survival“ und „Creative“ verwenden und der Spieler `/setup portal survival task ` vervollständigt
- **THEN** enthalten die Vorschläge beide, ohne Doppelte

#### Scenario: Radius-Hinweise
- **WHEN** der Spieler `/setup portal survival disc ` oder `radius ` vervollständigt
- **THEN** enthalten die Vorschläge die festen Radius-Hinweise

### Requirement: Ungültige Portale werden nie gespeichert
Vor jedem Speichern MUSS die Portalliste, wie sie danach in der Map-Datei stünde, `PortalValidator.problems(...)` aus `lobby-portals` bestehen (dieselben Regeln, die die Lobby beim Start prüft); der Editor bildet die Probleme des bearbeiteten Portals auf Chat-Meldungen ab und kopiert keine Regel. Die Regeln sind: Id nicht leer und in der Welt eindeutig, Aufgabe vorhanden, bei einem Ring Radius größer null und Normale ungleich null, bei einem Quader beide Ecken gesetzt. Verletzt eine Änderung eine Regel, MUSS der Chat den Grund nennen und die Map-Datei MUSS unverändert bleiben. Beim Speichern eines vorhandenen Portals gilt seine eigene Id nicht als Duplikat.

#### Scenario: Fehlende Aufgabe
- **WHEN** ein Quader mit beiden Ecken vorliegt, aber keine Aufgabe, und der Spieler `save` ausführt
- **THEN** wird nichts gespeichert und der Chat nennt die fehlende Aufgabe

#### Scenario: Leere Aufgabe
- **WHEN** ein Spieler `task` mit einem leeren Wert ausführt
- **THEN** lehnt der Chat das ab und das gespeicherte Portal behält seine Aufgabe

#### Scenario: Validator meldet ein Problem
- **WHEN** `PortalValidator.problems(...)` für die Id des bearbeiteten Portals beim `save` ein Problem liefert (z. B. Normale null)
- **THEN** nennt der Chat dessen Grund, und die Map-Datei bleibt unverändert

#### Scenario: Bestehende Id
- **WHEN** ein Spieler einen vollständigen Entwurf mit der Id eines bereits gespeicherten Portals per `save` speichert
- **THEN** ersetzt das Speichern das vorhandene Portal (kein Duplikat in der Liste), und der Chat sagt, dass es aktualisiert wurde

### Requirement: Speichern erhält die übrigen Map-Daten
Speichern oder Entfernen eines Portals MUSS Spawn, Name, Autoren und alle anderen Portale der Map-Datei unverändert lassen. Umgekehrt MÜSSEN `/setup map setspawn`, `setname` und `setauthor` die gespeicherten Portale erhalten. Nach einem Neustart des Setup-Servers MÜSSEN die Portale aus der Map-Datei wieder geladen sein.

#### Scenario: Spawn bleibt
- **WHEN** eine Welt einen Spawn hat und ein Spieler ein Portal speichert
- **THEN** hat die Map-Datei danach denselben Spawn und das Portal

#### Scenario: Portale bleiben nach setspawn
- **WHEN** die Welt Portale hat und ein Spieler `/setup map setspawn` ausführt
- **THEN** stehen die Portale unverändert in der Map-Datei

#### Scenario: Neustart
- **WHEN** ein Portal gespeichert wurde und der Setup-Server neu startet
- **THEN** listet `/setup portal list` es mit denselben Werten
