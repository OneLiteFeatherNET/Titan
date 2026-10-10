# Spec Delta

## ADDED Requirements

### Requirement: Portale werden aus einer anderen Welt als Entwürfe kopiert
`/setup portal copy <world>` MUSS die gespeicherten Portale der Welt `<world>` (ein anderes Verzeichnis unter `worlds/` mit Map-Datei) als Entwürfe des ausführenden Spielers anlegen, je Portal einen Entwurf mit derselben Id und unveränderten Werten. Ziel ist die Welt, die der Setup-Server geladen hat. Die Quelle MUSS nur gelesen werden und darf sich nicht ändern; die geladene Welt wechselt nicht. Der Chat MUSS die kopierten Ids nennen und getrennt die Ids, die beim Speichern ein vorhandenes Portal der Ziel-Welt ersetzen. Hat der Spieler zu einer Id schon einen offenen Entwurf, MUSS dieser unverändert bleiben und die Id als übersprungen gemeldet werden. Nach erfolgreichem Kopieren MUSS das vorhandene `show` einmal die kopierten Portale als Partikel zeigen (nur dem ausführenden Spieler, ein weiteres `show` ersetzt es), damit das Team die Positionen in der Ziel-Welt prüfen kann; das Kopieren DARF keine Vorschau-Aufgabe je Entwurf starten. Die Map-Datei der Ziel-Welt MUSS beim Kopieren unverändert bleiben. Ohne kopierbare Portale, bei unbekannter Quelle, bei Quelle gleich Ziel und bei unlesbarer Quell-Map-Datei MUSS der Chat den Grund nennen und nichts ändern.

#### Scenario: Kopie legt Entwürfe an
- **WHEN** die Welt „lobby" die Portale „survival" und „creative" hat, der Setup-Server „winter" geladen hat und der Spieler `/setup portal copy lobby` ausführt
- **THEN** hat der Spieler die Entwürfe „survival" und „creative" mit den Werten aus „lobby", der Chat nennt beide Ids, und die `map.json` von „winter" ist unverändert

#### Scenario: Überschreiben wird angekündigt
- **WHEN** „winter" schon ein gespeichertes Portal „survival" hat, „creative" aber nicht, und der Spieler `/setup portal copy lobby` ausführt
- **THEN** nennt der Chat „survival" als Id, die beim Speichern das vorhandene Portal ersetzt, und „creative" als neu; das gespeicherte „survival" bleibt bis zum Speichern unverändert

#### Scenario: Offener Entwurf bleibt
- **WHEN** der Spieler einen offenen Entwurf „survival" hat und `/setup portal copy lobby` ausführt
- **THEN** behält „survival" seine Werte, der Chat nennt „survival" als übersprungen, und die übrigen Portale werden kopiert

#### Scenario: Nicht übernehmbare Id
- **WHEN** die Quelle ein Portal mit einer Id hat, die der Setup-Server als Id ablehnt (etwa `copy`)
- **THEN** wird dieses Portal nicht übernommen, der Chat nennt die Id als übersprungen mit Grund, und die übrigen werden kopiert

#### Scenario: Quelle bleibt unverändert
- **WHEN** der Spieler kopiert und danach kopierte Entwürfe speichert oder verwirft
- **THEN** ist die `map.json` der Quell-Welt danach byte-gleich zu vorher

#### Scenario: Vorschau der Kopie
- **WHEN** der Spieler zwei Portale kopiert hat
- **THEN** zeigen Partikel nur ihm beide Portale einmalig an den kopierten Koordinaten (wie `show`, danach endet die Anzeige von selbst), und es läuft genau eine Show-Aufgabe für ihn

#### Scenario: Keine Vorschau-Aufgabe je Entwurf
- **WHEN** der Spieler viele Portale kopiert hat
- **THEN** startet das Kopieren keine Live-Vorschau-Aufgabe je kopiertem Entwurf, und ein späteres `show` ersetzt die Anzeige

#### Scenario: Unbekannte Quell-Welt
- **WHEN** der Spieler `/setup portal copy nirgendwo` ausführt und es kein Verzeichnis „nirgendwo" mit Map-Datei unter `worlds/` gibt
- **THEN** meldet der Chat die unbekannte Welt und nennt die verfügbaren, und es entstehen keine Entwürfe

#### Scenario: Quelle ist die geladene Welt
- **WHEN** der Spieler `/setup portal copy winter` ausführt, während „winter" geladen ist
- **THEN** meldet der Chat, dass die Quelle die geladene Welt ist, und es entstehen keine Entwürfe

#### Scenario: Quelle ohne Portale
- **WHEN** die Quell-Welt keine Portale hat
- **THEN** meldet der Chat, dass nichts zu kopieren ist, und es entstehen keine Entwürfe

#### Scenario: Unlesbare Quelle
- **WHEN** die `map.json` der Quell-Welt ungültiges JSON oder ungültige Portale enthält
- **THEN** meldet der Chat, dass die Quelle nicht gelesen werden kann, mit dem Grund, es entstehen keine Entwürfe, und der Server läuft weiter

#### Scenario: Konsole
- **WHEN** die Konsole `/setup portal copy lobby` ausführt
- **THEN** wird der Befehl wie alle `/setup`-Befehle abgelehnt

### Requirement: save-all speichert alle offenen Entwürfe mit Einzelergebnis
`/setup portal save-all` MUSS jeden offenen Entwurf des ausführenden Spielers wie `/setup portal <id> save` behandeln (Vollständigkeit, Regeln des Befehls, `PortalValidator` für diese Id) und den Ausgang je Id im Chat nennen. Gültige Entwürfe MÜSSEN in die Map-Datei der Ziel-Welt geschrieben werden und ein vorhandenes Portal derselben Id ersetzen; unvollständige oder ungültige Entwürfe MÜSSEN mit Grund gemeldet werden und offen bleiben, ohne das Speichern der übrigen zu verhindern. Übrige Map-Daten (Spawn, Name, Autoren) MÜSSEN erhalten bleiben. Nach dem Kopieren MUSS der Chat [save all] als anklickbaren Knopf für diesen Befehl anbieten.

#### Scenario: Alle Entwürfe gültig
- **WHEN** der Spieler zwei vollständige, gültige Entwürfe hat und `/setup portal save-all` ausführt
- **THEN** stehen beide in der `map.json`, der Chat meldet je Id „gespeichert" oder „ersetzt", und der Spieler hat keine Entwürfe mehr

#### Scenario: Teilweiser Fehlschlag
- **WHEN** von drei Entwürfen einer ungültig ist (`PortalValidator.problems(...)` meldet ein Problem für seine Id) und `/setup portal save-all` ausgeführt wird
- **THEN** stehen die zwei gültigen in der `map.json`, der Chat nennt für den dritten den Grund, und nur der dritte Entwurf bleibt offen

#### Scenario: Unvollständiger Entwurf
- **WHEN** ein offener Entwurf keine Aufgabe hat und `save-all` ausgeführt wird
- **THEN** nennt der Chat für diese Id die fehlende Aufgabe, der Entwurf bleibt offen und die übrigen werden gespeichert

#### Scenario: Ersetzen wie bei save
- **WHEN** ein Entwurf „survival" gültig ist und die Welt schon ein Portal „survival" hat
- **THEN** ersetzt `save-all` es an derselben Stelle der Liste, wie `save` es täte, und der Chat meldet „ersetzt"

#### Scenario: Keine Entwürfe
- **WHEN** der Spieler keinen offenen Entwurf hat und `save-all` ausführt
- **THEN** meldet der Chat, dass nichts zu speichern ist, und die Map-Datei bleibt unverändert

#### Scenario: Entwürfe anderer Spieler
- **WHEN** ein zweiter Spieler ebenfalls Entwürfe hat und der erste `save-all` ausführt
- **THEN** werden nur die Entwürfe des ersten gespeichert

#### Scenario: Knopf nach dem Kopieren
- **WHEN** ein Spieler Portale kopiert hat
- **THEN** enthält die Rückmeldung einen anklickbaren Knopf [save all], der `/setup portal save-all` ausführt

### Requirement: copy und save-all sind keine Portal-Ids
`copy` und `save-all` MÜSSEN als Portal-Id abgelehnt werden, ebenso wie `list`, `show` und `create`.

#### Scenario: Reservierte Wörter als Id
- **WHEN** ein Spieler `/setup portal create copy` oder `/setup portal create save-all` ausführt
- **THEN** lehnt der Chat die Id als reserviert ab und es entsteht kein Entwurf

### Requirement: Tab-Vervollständigung bietet die Quell-Welten an
An der Welt-Position von `/setup portal copy` MUSS die Vervollständigung die Verzeichnisse unter `worlds/` vorschlagen, die eine Map-Datei haben, ohne die geladene Welt. Die Verben `copy` und `save-all` MÜSSEN an der Id-Position vorgeschlagen werden.

#### Scenario: Welten
- **WHEN** unter `worlds/` die Verzeichnisse „lobby" und „winter" mit Map-Datei und „leer" ohne Map-Datei liegen, „winter" geladen ist und der Spieler `/setup portal copy ` vervollständigt
- **THEN** enthalten die Vorschläge „lobby", aber weder „winter" noch „leer"

#### Scenario: Verben
- **WHEN** der Spieler `/setup portal ` vervollständigt
- **THEN** enthalten die Vorschläge zusätzlich `copy` und `save-all`
