# Spec Delta

## Purpose

Legt fest, wie ein Spieler in der Lobby ein zufällig generiertes Jump and Run allein spielt: wie ein Lauf startet und endet, was er sieht, wie die Schwierigkeit steigt, wo Sprünge entstehen dürfen und wie Score und Rekord geführt werden.

## ADDED Requirements

### Requirement: Lauf über das Hotbar-Item starten
Benutzt ein Spieler, der keinen Lauf hat, das Jump-and-Run-Item, MUSS die Lobby von seiner aktuellen Position aus prüfen, ob die gesamte Aufstiegsphase in der echten Welt Platz hat. Gelingt die Prüfung, MUSS der Lauf an dieser Position beginnen. Gelingt sie nicht, DARF KEIN Lauf beginnen, und der Spieler MUSS eine Meldung erhalten, dass hier kein Platz zum Starten ist.

#### Scenario: Start im Freien
- **WHEN** ein Spieler auf freier Fläche das Jump-and-Run-Item benutzt
- **THEN** beginnt ein Lauf, und vor ihm erscheinen die ersten Blöcke

#### Scenario: Kein Platz
- **WHEN** ein Spieler unter einer niedrigen Decke steht, unter der kein Aufstieg möglich ist, und das Item benutzt
- **THEN** beginnt kein Lauf, und er erhält die Meldung, dass hier kein Platz zum Starten ist

#### Scenario: Start an beliebiger Stelle
- **WHEN** ein Spieler an zwei verschiedenen freien Stellen der Lobby nacheinander einen Lauf startet
- **THEN** beginnt jeder Lauf an der Stelle, an der der Spieler beim Benutzen stand

### Requirement: Blöcke nur für den laufenden Spieler
Die Blöcke eines Laufs MÜSSEN ausschließlich für den Spieler sichtbar und begehbar sein, der den Lauf spielt. Die echte Lobby-Welt DARF sich durch einen Lauf NICHT verändern. Andere Spieler DÜRFEN die Blöcke NICHT sehen. Die Blöcke MÜSSEN für den laufenden Spieler auch dann sichtbar bleiben, wenn die Lobby ihm Chunks erneut schickt.

#### Scenario: Anderer Spieler steht daneben
- **WHEN** Spieler A einen Lauf spielt und Spieler B an derselben Stelle steht
- **THEN** sieht B keinen der Blöcke von A, und an deren Positionen ist in der echten Welt weiterhin Luft

#### Scenario: Chunk wird neu geschickt
- **WHEN** die Lobby dem laufenden Spieler den Chunk mit seinen Blöcken erneut schickt
- **THEN** sieht er danach weiterhin genau die Blöcke seines Sichtfensters

#### Scenario: Zwei Läufe gleichzeitig
- **WHEN** zwei Spieler gleichzeitig je einen Lauf spielen
- **THEN** sieht jeder nur seine eigenen Blöcke, und das Ende des einen Laufs ändert nichts am anderen

### Requirement: Sichtfenster von zwei Blöcken
Während eines Laufs MUSS der Spieler genau den Block sehen, auf dem er zuletzt gelandet ist, dazu die bis zu zwei Blöcke davor (bereits geschafft) und die zwei Blöcke danach (die nächsten Sprünge). Weitere Blöcke des Laufs DÜRFEN NICHT sichtbar sein.

#### Scenario: Vorrücken nach einer Landung
- **WHEN** der Spieler auf dem nächsten Block landet
- **THEN** verschwindet der älteste sichtbare Block, und ein neuer Block erscheint zwei Sprünge voraus

#### Scenario: Beginn des Laufs
- **WHEN** ein Lauf beginnt
- **THEN** sieht der Spieler den Startblock und die zwei nächsten Blöcke, aber noch keine Blöcke dahinter

### Requirement: Fortschritt durch Landen
Ein Sprung MUSS als geschafft gelten, sobald der Spieler auf einem der vor ihm liegenden Blöcke seines Laufs steht. Landet er direkt auf dem übernächsten Block, MÜSSEN beide Sprünge als geschafft gelten.

#### Scenario: Einen Block überspringen
- **WHEN** der Spieler vom aktuellen Block direkt auf den übernächsten springt
- **THEN** zählen beide Sprünge, und das Sichtfenster rückt um zwei Blöcke vor

### Requirement: Aufstiegsphase aus dem Spawn-Bereich
Jeder Lauf MUSS mit mindestens 5 leichten Sprüngen beginnen, die jeweils einen Block höher liegen und, soweit Platz ist, vom Lobby-Spawn wegführen. Die Aufstiegsphase MUSS so lange weitergehen, bis unter dem zuletzt erzeugten Block mindestens 4 Blöcke Luft sind, höchstens aber 20 Sprünge lang. Lässt sich dieser Abstand innerhalb von 20 Sprüngen nicht erreichen, DARF der Lauf NICHT beginnen (Meldung „kein Platz“). Die Sprünge der Aufstiegsphase DÜRFEN NICHT zum Score zählen. Erst danach MUSS die Schwierigkeit nach dem Score greifen.

#### Scenario: Weg vom Spawn
- **WHEN** ein Spieler neben dem Spawn einen Lauf startet und in Richtung weg vom Spawn Platz ist
- **THEN** führen die Sprünge der Aufstiegsphase nach oben und vom Spawn weg

#### Scenario: Aufstieg bis ins Freie
- **WHEN** ein Spieler auf flachem Boden startet
- **THEN** endet die Aufstiegsphase erst mit einem Block, unter dem mindestens 4 Blöcke Luft sind

#### Scenario: Aufstieg zählt nicht
- **WHEN** der Spieler alle Sprünge der Aufstiegsphase geschafft hat
- **THEN** zeigt sein Score 0

### Requirement: Schwierigkeit steigt mit dem Score
Nach der Aufstiegsphase MUSS die Schwierigkeit jedes neuen Sprungs aus einer festen, stetig steigenden Funktion des Scores folgen, die sich einem Höchstwert annähert, ohne ihn zu überschreiten. Die Schwierigkeit MUSS sich in der Form des Blocks (Vollblock, Falltür, Stufe, Zaun oder Mauer, Glasscheibe oder Gitter, schmaler Pfosten), in der Lückenbreite und im Höhenunterschied zeigen. Jeder erzeugte Sprung MUSS ohne Hilfsmittel schaffbar sein. Maßgeblich ist die Oberkante der Lauffläche, also z. B. bei einer Falltür knapp ein Fünftel, bei einer Stufe ein halber Block und bei einem Zaun oder einer Mauer anderthalb Blöcke über ihrer Blockposition. Die Oberkante des Ziels darf höchstens einen Block über der des Ausgangsblocks liegen. Die Lücke darf bei einem Aufstieg höchstens 3 Blöcke betragen, auf gleicher Höhe oder abwärts höchstens 4 Blöcke.

#### Scenario: Leichter Anfang
- **WHEN** der Score 0 ist
- **THEN** sind die erzeugten Sprünge überwiegend Vollblöcke mit kurzer Lücke

#### Scenario: Später schwerer
- **WHEN** der Score hoch ist (z. B. 80)
- **THEN** sind die erzeugten Sprünge im Mittel deutlich schwerer als bei Score 0: mehr schmale Formen, breitere Lücken und mehr Aufstiege

#### Scenario: Zaun nach Vollblock
- **WHEN** ein Zaun auf einen Vollblock folgt
- **THEN** liegt der Zaun auf derselben Blockhöhe oder tiefer, weil seine Oberkante sonst mehr als einen Block über der des Vollblocks läge

#### Scenario: Nie unmöglich
- **WHEN** beliebig viele Sprünge erzeugt werden
- **THEN** überschreitet kein Sprung die Grenzen für Lücke und Aufstieg, auch nicht von einem Zaun oder einer Stufe aus

### Requirement: Sprünge nur, wo Platz ist
Ein neuer Block nach der Aufstiegsphase DARF NUR an einer Stelle entstehen, unter der in der echten Welt mindestens 4 Blöcke Luft sind und an der in der echten Welt Luft ist, über deren Oberkante zwei Blöcke Kopffreiheit sind, deren Flugbahn vom vorigen Block aus frei ist, die keinen sichtbaren Block des eigenen Laufs überschneidet und die innerhalb der Grenzen der Lobby-Welt liegt. Ein neuer Block DARF NUR gewählt werden, wenn von ihm aus mindestens ein weiterer gültiger Sprung möglich ist. Unter den gültigen Stellen MUSS die Lobby solche mit mehr Luft darunter und drumherum bevorzugen, ohne dass dadurch die Schwierigkeit nach dem Score verloren geht. Findet die Lobby trotzdem keinen gültigen nächsten Block, MUSS der Lauf mit dem erreichten Score enden.

#### Scenario: Nicht über Wegen
- **WHEN** eine Stelle direkt über einem Weg liegt, sodass unter ihr weniger als 4 Blöcke Luft sind
- **THEN** entsteht dort nach der Aufstiegsphase kein Block

#### Scenario: Ins Leere bevorzugt
- **WHEN** zwei gleich schwere Stellen möglich sind, eine über offenem Raum und eine dicht neben einem Gebäude
- **THEN** wählt die Lobby die Stelle über offenem Raum

#### Scenario: Wand im Weg
- **WHEN** in Laufrichtung eine Wand der Lobby steht
- **THEN** entsteht kein Block in oder hinter der Wand, sondern der Lauf führt an ihr vorbei

#### Scenario: Keine Sackgasse
- **WHEN** ein Kandidat zwar frei ist, von ihm aus aber kein weiterer Sprung möglich wäre
- **THEN** wird er nicht gewählt

#### Scenario: Gar kein Platz mehr
- **WHEN** von der aktuellen Stelle aus kein gültiger Block mehr möglich ist
- **THEN** endet der Lauf mit dem erreichten Score

### Requirement: Optische Vielfalt der Blöcke
Jeder Block eines Laufs MUSS sein Material zufällig aus einer festen Auswahl passend zu seiner Form erhalten (z. B. bunter Beton, Wolle und Terrakotta als Vollblock, verschiedene Holz- und Steinstufen, Holzzäune und Mauern, bunte Glasscheiben und Eisengitter). Das Material DARF die Schwierigkeit und die Schaffbarkeit eines Sprungs NICHT verändern.

#### Scenario: Material wechselt
- **WHEN** ein Spieler mehrere Vollblöcke hintereinander sieht
- **THEN** haben sie nicht alle dasselbe Material

#### Scenario: Material ändert nichts an der Form
- **WHEN** ein Zaun aus Eichenholz und eine Bruchsteinmauer an derselben Stelle möglich wären
- **THEN** gelten für beide dieselbe Oberkante und dieselben Kosten

### Requirement: Laufende
Ein Lauf MUSS enden, wenn der Spieler mehr als drei Blöcke unter den Block fällt, auf dem er zuletzt gelandet ist, wenn er mit der Elytra zu gleiten beginnt, wenn er das Jump-and-Run-Item erneut benutzt, wenn er stirbt oder wenn er die Lobby verlässt. Bei einem Absturz MUSS die Lobby ihn an den Startpunkt seines Laufs zurücksetzen. Nach dem Ende DÜRFEN keine Blöcke des Laufs für ihn sichtbar bleiben, und an ihren Stellen MUSS er wieder die echte Welt sehen.

#### Scenario: Absturz
- **WHEN** der Spieler mehr als drei Blöcke unter seinen letzten Block fällt
- **THEN** endet der Lauf, alle Blöcke verschwinden, und er steht wieder am Startpunkt des Laufs

#### Scenario: Elytra
- **WHEN** der Spieler während eines Laufs mit der Elytra zu gleiten beginnt
- **THEN** endet der Lauf wie bei einem Absturz, aber ohne ihn zurückzusetzen

#### Scenario: Abbruch über das Item
- **WHEN** der Spieler während eines Laufs das Jump-and-Run-Item benutzt
- **THEN** endet der Lauf, und alle Blöcke verschwinden

#### Scenario: Spieler verlässt die Lobby
- **WHEN** der Spieler während eines Laufs die Verbindung trennt
- **THEN** endet der Lauf, und die Lobby hält keinen Zustand dieses Laufs mehr

### Requirement: Score und Rekord
Während eines Laufs MUSS der Spieler seinen aktuellen Score in der Action Bar sehen. Am Ende eines Laufs MUSS er eine Meldung mit dem erreichten Score erhalten. Die Lobby MUSS pro Spieler den höchsten Score seit ihrem Start im Speicher halten. Übertrifft ein Lauf diesen Rekord, MUSS die Meldung das als neuen Rekord kennzeichnen. Über einen Neustart der Lobby hinweg DARF der Rekord verloren gehen.

#### Scenario: Neuer Rekord
- **WHEN** ein Spieler mit bisherigem Rekord 12 einen Lauf mit Score 15 beendet
- **THEN** meldet die Lobby Score 15 als neuen Rekord, und sein Rekord ist danach 15

#### Scenario: Kein neuer Rekord
- **WHEN** derselbe Spieler danach einen Lauf mit Score 9 beendet
- **THEN** meldet die Lobby Score 9 ohne Rekord-Hinweis, und sein Rekord bleibt 15

#### Scenario: Wiederkommen ohne Neustart
- **WHEN** ein Spieler die Lobby verlässt und vor einem Neustart wiederkommt
- **THEN** gilt sein bisheriger Rekord weiter

### Requirement: Texte in der Sprache des Spielers
Alle Texte des Jump and Run (Score in der Action Bar, Meldungen zu Start, Ende und Rekord) MÜSSEN in der Sprache des Spielers erscheinen. Fehlt seine Sprache, MUSS Englisch erscheinen. Das Item trägt nur den Spielnamen „Jump & Run“, der in allen Sprachen gleich ist, und keine Beschreibung.

#### Scenario: Deutscher Client
- **WHEN** ein Spieler mit deutscher Client-Sprache einen Lauf beendet
- **THEN** ist die Meldung zum Laufende deutsch

#### Scenario: Unbekannte Sprache
- **WHEN** ein Spieler mit einer Client-Sprache ohne Übersetzung einen Lauf beendet
- **THEN** ist die Meldung zum Laufende englisch
