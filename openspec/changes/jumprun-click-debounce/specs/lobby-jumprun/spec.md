# Spec Delta

## MODIFIED Requirements

### Requirement: Schwierigkeitsmodi
Ein Spieler MUSS zwischen den Modi Easy, Medium, Hard, Rainbow und Ultra wählen können, indem er das Jump-and-Run-Item mit gedrückter Schleichtaste rechtsklickt; jeder solche Klick MUSS zum nächsten Modus wechseln (Easy → Medium → Hard → Rainbow → Ultra → Easy) und den gewählten Modus in der Sprache des Spielers anzeigen, außer er liegt innerhalb von 5 Ticks (250 ms) nach dem letzten Moduswechsel desselben Spielers. Ein solcher Klick innerhalb des Fensters wird ignoriert: er wechselt nichts und verlängert das Fenster nicht, denn das Fenster läuft ab dem Moduswechsel. Außerhalb eines Laufs DARF ein Klick mit Schleichtaste KEINEN Lauf starten; während eines Laufs DARF er den Modus NICHT wechseln und den Lauf NICHT beenden. Standard ist Medium; der gewählte Modus gilt, bis der Spieler die Lobby verlässt. Ein Lauf behält den Modus, mit dem er gestartet wurde:
- Easy: nur Vollblöcke und Stufen, Lücke höchstens 2, seltene Aufstiege, Schwierigkeit steigt halb so schnell wie Medium.
- Medium: Verhalten wie in den übrigen Anforderungen beschrieben.
- Hard: Formen früher frei (Stufen und Falltüren ab 5, Zäune, Mauern, Scheiben und Gitter ab 10, Pfosten ab 20), Schwierigkeit steigt doppelt so schnell wie Medium.
- Rainbow: Schwierigkeit wie Medium; solange der Läufer auf einem Block steht, wechseln alle sichtbaren Laufblöcke alle `jumprun.rainbow.rerollTicks` Ticks (Standard 10) ihr Material, Position und Form bleiben.
- Ultra: Schwierigkeit wie Hard, ohne Umrandung; solange der Läufer auf einem Block steht, werden die Blöcke voraus alle `jumprun.ultra.rerollTicks` Ticks (Standard 40) an neuer Stelle mit neuer Form und neuem Material neu erzeugt (mit Aufstiegs- und Fall-Animation).
Rekorde MÜSSEN pro Modus getrennt geführt werden, und Score-Meldungen, Rekord-Meldungen und die Anzeige über dem Läufer MÜSSEN den Modus nennen.

#### Scenario: Modus wechseln
- **WHEN** ein Spieler ohne laufenden Lauf das Item mit Schleichtaste rechtsklickt, während Medium aktiv ist
- **THEN** ist Hard aktiv, er sieht den neuen Modus, und es startet kein Lauf

#### Scenario: Doppelklick wechselt einmal
- **WHEN** ein Spieler ohne laufenden Lauf zweimal innerhalb von 5 Ticks mit Schleichtaste rechtsklickt, während Medium aktiv ist
- **THEN** ist Hard aktiv, und der zweite Klick hat den Modus nicht weitergeschaltet

#### Scenario: Klick nach dem Fenster wechselt weiter
- **WHEN** ein Spieler 5 Ticks nach einem Moduswechsel erneut mit Schleichtaste rechtsklickt
- **THEN** ist der nächste Modus aktiv

#### Scenario: Ignorierter Klick verlängert das Fenster nicht
- **WHEN** ein Spieler nach einem Moduswechsel 1 bis 4 Ticks lang mehrfach mit Schleichtaste klickt und 5 Ticks nach dem Wechsel erneut
- **THEN** wechselt der Modus bei diesem letzten Klick

#### Scenario: Kein Wechsel im Lauf
- **WHEN** ein Läufer das Item mit Schleichtaste rechtsklickt
- **THEN** bleiben Modus und Lauf unverändert

#### Scenario: Easy bleibt leicht
- **WHEN** ein Lauf im Modus Easy Score 60 erreicht
- **THEN** waren alle Blöcke Vollblöcke oder Stufen, und keine Lücke war größer als 2

#### Scenario: Rainbow wechselt das Material
- **WHEN** ein Läufer im Modus Rainbow 10 Ticks auf einem Block steht
- **THEN** haben seine sichtbaren Blöcke neue Materialien an denselben Stellen und mit derselben Form

#### Scenario: Ultra würfelt neu
- **WHEN** ein Läufer im Modus Ultra 40 Ticks auf einem Block steht
- **THEN** steigen die Blöcke voraus auf, an neuen gültigen Stellen fallen neue Blöcke ein, und er sieht keine Umrandung

#### Scenario: Kein Wechsel im Sprung
- **WHEN** ein Läufer in Rainbow oder Ultra springt, bevor das Intervall seines Modus vergangen ist
- **THEN** wechselt nichts, und der Zähler beginnt nach der nächsten Landung neu

#### Scenario: Rekord pro Modus
- **WHEN** ein Spieler in Easy Rekord 30 und in Hard Rekord 8 hat und in Hard 9 erreicht
- **THEN** ist das ein neuer Hard-Rekord, und sein Easy-Rekord bleibt 30

### Requirement: Laufende
Ein Lauf MUSS enden, wenn der Spieler mehr als drei Blöcke unter den Block fällt, auf dem er zuletzt gelandet ist, wenn er das Jump-and-Run-Item erneut benutzt (außer innerhalb der Startsperre), wenn er stirbt, wenn er zum Lobby-Spawn zurückkehrt (per Befehl oder Navigator, siehe `lobby-spawn-return`) oder wenn er die Lobby verlässt. Bei einem Absturz MUSS die Lobby ihn an den Startpunkt seines Laufs zurücksetzen. Eine Rückkehr zum Spawn MUSS den Lauf beenden wie ein Abbruch über das Item, also mit gewertetem Score, bevor der Spieler versetzt wird; der Spieler landet am Spawn, nicht am Startpunkt des Laufs. Während eines Laufs DARF der Läufer KEINE Elytra tragen, damit ein Leertastendruck in der Luft kein Gleiten auslöst; mit dem Laufende MUSS er seine normale Lobby-Ausstattung zurückerhalten. Nach dem Ende DÜRFEN keine Blöcke des Laufs für ihn sichtbar bleiben, und an ihren Stellen MUSS er wieder die echte Welt sehen.

Die Startsperre dauert 20 Ticks (1 s) ab dem Start eines Laufs. Innerhalb der Startsperre DARF die Benutzung des Items den Lauf NICHT beenden. Die Startsperre endet mit dem Lauf, aus welchem Grund auch immer, sodass ein neuer Lauf sofort gestartet werden kann.

Ein Klick auf einen Block, echt oder als Laufblock, DARF einen Lauf NICHT beenden: die Benutzung des Items, die dem Klick folgt, wird für 2 Ticks ignoriert. Außerhalb eines Laufs hat ein Klick auf einen Block keine solche Wirkung; die folgende Benutzung des Items startet den Lauf oder wechselt den Modus.

#### Scenario: Absturz
- **WHEN** der Spieler mehr als drei Blöcke unter seinen letzten Block fällt
- **THEN** endet der Lauf, alle Blöcke verschwinden, und er steht wieder am Startpunkt des Laufs

#### Scenario: Keine Elytra im Lauf
- **WHEN** ein Spieler einen Lauf startet
- **THEN** trägt er keine Elytra mehr, und ein Leertastendruck in der Luft beendet den Lauf nicht

#### Scenario: Ausstattung zurück
- **WHEN** der Lauf endet, egal aus welchem Grund außer Verlassen der Lobby
- **THEN** trägt der Spieler wieder seine Elytra und hat seine normale Hotbar

#### Scenario: Abbruch über das Item
- **WHEN** der Spieler 20 Ticks oder später nach dem Start eines Laufs das Jump-and-Run-Item benutzt
- **THEN** endet der Lauf, und alle Blöcke verschwinden

#### Scenario: Doppelklick kurz nach dem Start
- **WHEN** der Läufer weniger als 20 Ticks nach dem Start das Jump-and-Run-Item benutzt
- **THEN** läuft der Lauf weiter

#### Scenario: Klick auf einen Lobby-Block im Lauf
- **WHEN** der Läufer auf einen Lobby-Block klickt und die Benutzung des Items dem Klick folgt
- **THEN** läuft der Lauf weiter

#### Scenario: Klick auf einen Lobby-Block vor dem Start
- **WHEN** ein Spieler ohne Lauf auf einen Lobby-Block klickt und die Benutzung des Items dem Klick folgt
- **THEN** startet ein Lauf

#### Scenario: Neustart nach Tod in der Startsperre
- **WHEN** ein Läufer weniger als 20 Ticks nach dem Start stirbt und danach sofort das Item benutzt
- **THEN** startet ein neuer Lauf

#### Scenario: Spieler verlässt die Lobby
- **WHEN** der Spieler während eines Laufs die Verbindung trennt
- **THEN** endet der Lauf, und die Lobby hält keinen Zustand dieses Laufs mehr

#### Scenario: Rückkehr zum Spawn im Lauf
- **WHEN** ein Läufer mit Score 12 im Lauf `/spawn` eingibt
- **THEN** endet der Lauf mit Score 12 und der Endmeldung wie bei einem Abbruch, alle Blöcke verschwinden, er hat seine Ausstattung zurück und steht am Lobby-Spawn
