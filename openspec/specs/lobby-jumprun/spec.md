# lobby-jumprun Specification

## Purpose
Legt fest, wie ein Spieler in der Lobby ein zufällig generiertes Jump and Run allein spielt: wie ein Lauf startet und endet, was er sieht, wie die Schwierigkeit steigt, wo Sprünge entstehen dürfen und wie Score und Rekord geführt werden.

## Requirements

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

### Requirement: Blöcke nur für den laufenden Spieler begehbar
Die Blöcke eines Laufs MÜSSEN ausschließlich für den Spieler begehbar sein, der den Lauf spielt. Die echte Lobby-Welt DARF sich durch einen Lauf NICHT verändern. Andere Spieler MÜSSEN die Blöcke des Sichtfensters als nicht begehbare Darstellung sehen, durch die sie hindurchlaufen und auf der sie nicht stehen können. Der laufende Spieler selbst DARF diese Darstellung NICHT zusätzlich zu seinen Blöcken sehen. Die Blöcke MÜSSEN für den laufenden Spieler auch dann sichtbar und begehbar bleiben, wenn die Lobby ihm Chunks erneut schickt oder er sie mit Links- oder Rechtsklick anklickt, egal welches Item er hält.

#### Scenario: Anderer Spieler sieht den Lauf
- **WHEN** Spieler A einen Lauf spielt und Spieler B in der Nähe ist
- **THEN** sieht B an den Positionen von As Sichtfenster die Blöcke in As Material, kann aber nicht auf ihnen stehen, und an diesen Positionen ist in der echten Welt weiterhin Luft

#### Scenario: Darstellung folgt dem Fenster
- **WHEN** A auf dem nächsten Block landet
- **THEN** verschwindet bei B die Darstellung des ältesten Blocks, und die des neuen Blocks erscheint

#### Scenario: Ende räumt die Darstellung ab
- **WHEN** As Lauf endet, egal aus welchem Grund, oder die Lobby herunterfährt
- **THEN** sieht B keine Darstellung von As Blöcken mehr, und es bleiben keine Objekte des Laufs in der Welt zurück

#### Scenario: Block anklicken
- **WHEN** der laufende Spieler einen seiner Blöcke mit Links- oder Rechtsklick anklickt, auch mit dem Jump-and-Run-Item in der Hand
- **THEN** ist der Block danach weiterhin sichtbar und begehbar, und der Lauf läuft weiter

#### Scenario: Chunk wird neu geschickt
- **WHEN** die Lobby dem laufenden Spieler den Chunk mit seinen Blöcken erneut schickt
- **THEN** sieht er danach weiterhin genau die Blöcke seines Sichtfensters

#### Scenario: Zwei Läufe gleichzeitig
- **WHEN** zwei Spieler gleichzeitig je einen Lauf spielen
- **THEN** kann jeder nur auf seinen eigenen Blöcken stehen, sieht die Blöcke des anderen nur als Darstellung, und das Ende des einen Laufs ändert nichts am anderen

### Requirement: Anzeige über dem Läufer
Während eines Laufs MÜSSEN andere Spieler über dem Kopf des Läufers eine farbig gestaltete Anzeige sehen, die das Symbol des Jump-and-Run-Items, den Spielnamen „Jump & Run“ und den aktuellen Score zeigt. Die Anzeige MUSS sich aktualisieren, sobald sich der Score ändert, und mit dem Ende des Laufs verschwinden. Der Läufer selbst DARF diese Anzeige NICHT sehen, er hat seinen Score in der Action Bar. Die Anzeige ist sprachneutral.

#### Scenario: Score über dem Kopf
- **WHEN** A einen Lauf spielt und Score 7 erreicht
- **THEN** sieht B über As Kopf das Item-Symbol, „Jump & Run“ und die 7

#### Scenario: Anzeige verschwindet
- **WHEN** As Lauf endet
- **THEN** ist die Anzeige über As Kopf für alle verschwunden

### Requirement: Ton bei jedem Punkt
Erhöht sich der Score eines Läufers, MUSS der Läufer einen kurzen Ton hören, der als bei jedem Punkt höher wahrgenommen wird, ohne dass je ein hörbarer Sprung zurück nach unten entsteht (endlos ansteigende Tonleiter, Shepard-Skala). Andere Spieler DÜRFEN diesen Ton NICHT hören. Jede geschaffte Landung in der Aufstiegsphase MUSS stattdessen einen leisen, gleichbleibenden Signalton nur für den Läufer erzeugen, damit er weiß, dass der Sprung zählt.

#### Scenario: Ton nach einem Punkt
- **WHEN** der Läufer nach der Aufstiegsphase auf dem nächsten Block landet
- **THEN** hört er einen Ton, und ein danebenstehender Spieler hört ihn nicht

#### Scenario: Ton steigt scheinbar endlos
- **WHEN** der Läufer nacheinander 30 Punkte erreicht
- **THEN** steigt jeder Teilton von einem Punkt zum nächsten um einen Halbton, und ein Teilton, der oben aus dem Tonumfang fällt, ist vorher bis zur Unhörbarkeit ausgeblendet, während unten ein neuer leise einsetzt

#### Scenario: Signal im Aufstieg
- **WHEN** der Läufer in der Aufstiegsphase auf dem nächsten Block landet
- **THEN** hört er den Signalton und nicht den Punkte-Ton

### Requirement: Ton beim Scheitern
Endet ein Lauf durch einen Absturz, ohne dass er einen neuen Rekord aufgestellt hat, MUSS der Läufer einen kurzen, absteigenden Ton hören, der sich klar vom Punkte-Ton unterscheidet. Andere Spieler DÜRFEN ihn NICHT hören. Ein Abbruch über das Item, das Verlassen der Lobby und das Herunterfahren DÜRFEN diesen Ton NICHT auslösen.

#### Scenario: Absturz
- **WHEN** der Läufer mehr als drei Blöcke unter seinen letzten Block fällt
- **THEN** hört er den Ton beim Scheitern, und ein Spieler daneben hört ihn nicht

#### Scenario: Absturz mit neuem Rekord
- **WHEN** der Läufer mit einem neuen Rekord abstürzt
- **THEN** hört er den Ton beim Scheitern nicht

#### Scenario: Freiwilliger Abbruch
- **WHEN** der Läufer den Lauf über das Item beendet
- **THEN** hört er den Ton beim Scheitern nicht

### Requirement: Sichtfenster von zwei Blöcken
Während eines Laufs MUSS der Spieler genau den Block sehen, auf dem er zuletzt gelandet ist, dazu die bis zu zwei Blöcke davor (bereits geschafft) und die zwei Blöcke danach (die nächsten Sprünge). Weitere Blöcke des Laufs DÜRFEN NICHT sichtbar sein.

#### Scenario: Vorrücken nach einer Landung
- **WHEN** der Spieler auf dem nächsten Block landet
- **THEN** verschwindet der älteste sichtbare Block, und ein neuer Block erscheint zwei Sprünge voraus

#### Scenario: Beginn des Laufs
- **WHEN** ein Lauf beginnt
- **THEN** sieht der Spieler den Startblock und die zwei nächsten Blöcke, aber noch keine Blöcke dahinter

### Requirement: Blöcke fallen ein und steigen auf
Ein neuer Block des Sichtfensters MUSS für alle, die ihn sehen, aus der Höhe an seine Stelle herabfallen, bevor er steht. Ein Block, der das Sichtfenster verlässt, MUSS umgekehrt von seiner Stelle nach oben steigen und dabei verschwinden. Für den Läufer MUSS ein neuer Block spätestens dann begehbar sein, wenn die Fallbewegung endet, und diese MUSS kürzer als eine halbe Sekunde dauern. Ein Block, der aufsteigt, DARF für den Läufer NICHT mehr begehbar sein.

#### Scenario: Neuer Block fällt herab
- **WHEN** der Läufer auf dem nächsten Block landet
- **THEN** sehen er und andere Spieler den neuen Block zwei Sprünge voraus von oben an seine Stelle fallen, und danach kann der Läufer ihn betreten

#### Scenario: Alter Block steigt auf
- **WHEN** ein Block das Sichtfenster hinter dem Läufer verlässt
- **THEN** steigt er für alle sichtbar nach oben und verschwindet

### Requirement: Nächster Block hervorgehoben
Während eines Laufs MUSS der Läufer den nächsten Block, auf den er springen soll, mit einer leuchtenden Umrandung sehen, sobald dieser begehbar ist. Nach jeder Landung MUSS die Umrandung auf den neuen nächsten Block wechseln. Andere Spieler DÜRFEN die Umrandung NICHT sehen.

#### Scenario: Umrandung wandert mit
- **WHEN** der Läufer auf dem hervorgehobenen Block landet
- **THEN** ist dieser nicht mehr umrandet, und der nun nächste Block ist umrandet

#### Scenario: Nur für den Läufer
- **WHEN** ein anderer Spieler den Lauf beobachtet
- **THEN** sieht er keine Umrandung

### Requirement: Fortschritt durch Landen
Ein Sprung MUSS als geschafft gelten, sobald der Spieler auf einem der vor ihm liegenden Blöcke seines Laufs steht. Landet er direkt auf dem übernächsten Block, MÜSSEN beide Sprünge als geschafft gelten.

#### Scenario: Landen und stehen bleiben
- **WHEN** der Spieler auf dem nächsten Block landet und danach still stehen bleibt
- **THEN** zählt der Sprung, und der Lauf endet nicht durch einen Absturz

#### Scenario: Einen Block überspringen
- **WHEN** der Spieler vom aktuellen Block direkt auf den übernächsten springt
- **THEN** zählen beide Sprünge, und das Sichtfenster rückt um zwei Blöcke vor

### Requirement: Aufstiegsphase aus dem Spawn-Bereich
Jeder Lauf MUSS mit mindestens 5 leichten Sprüngen beginnen, die jeweils einen Block höher liegen und, soweit Platz ist, vom Lobby-Spawn wegführen. Die Aufstiegsphase MUSS so lange weitergehen, bis der zuletzt erzeugte Block waagrecht mindestens 16 Blöcke vom Lobby-Spawn entfernt ist und unter ihm mindestens 8 Blöcke Luft sind, höchstens aber 30 Sprünge lang. Lässt sich das innerhalb von 30 Sprüngen nicht erreichen, DARF der Lauf NICHT beginnen (Meldung „kein Platz“). Die Sprünge der Aufstiegsphase DÜRFEN NICHT zum Score zählen. Erst danach MUSS die Schwierigkeit nach dem Score greifen.

#### Scenario: Weg vom Spawn
- **WHEN** ein Spieler neben dem Spawn einen Lauf startet und in Richtung weg vom Spawn Platz ist
- **THEN** führen die Sprünge der Aufstiegsphase nach oben und vom Spawn weg

#### Scenario: Aufstieg bis ins Freie
- **WHEN** ein Spieler auf flachem Boden startet
- **THEN** endet die Aufstiegsphase erst mit einem Block, der mindestens 16 Blöcke waagrecht vom Spawn entfernt ist und unter dem mindestens 8 Blöcke Luft sind

#### Scenario: Aufstieg zählt nicht
- **WHEN** der Spieler alle Sprünge der Aufstiegsphase geschafft hat
- **THEN** zeigt sein Score 0

### Requirement: Schwierigkeit steigt mit dem Score
Nach der Aufstiegsphase MUSS die Schwierigkeit jedes neuen Sprungs aus einer festen, stetig steigenden Funktion des Scores folgen, die sich einem Höchstwert annähert, ohne ihn zu überschreiten. Die Schwierigkeit MUSS langsam steigen: Bis Score 10 MÜSSEN alle Blöcke Vollblöcke sein, Stufen und Falltüren DÜRFEN erst ab Score 10, Zäune, Mauern, Scheiben und Gitter erst ab Score 25 und schmale Pfosten erst ab Score 40 vorkommen. Die Schwierigkeit MUSS sich in der Form des Blocks (Vollblock, Falltür, Stufe, Zaun oder Mauer, Glasscheibe oder Gitter, schmaler Pfosten), in der Lückenbreite und im Höhenunterschied zeigen. Jeder erzeugte Sprung MUSS ohne Hilfsmittel schaffbar sein. Maßgeblich ist die Oberkante der Lauffläche, also z. B. bei einer Falltür knapp ein Fünftel, bei einer Stufe ein halber Block und bei einem Zaun oder einer Mauer anderthalb Blöcke über ihrer Blockposition. Die Oberkante des Ziels darf höchstens einen Block über der des Ausgangsblocks liegen. Die Lücke darf bei einem Aufstieg höchstens 3 Blöcke betragen, auf gleicher Höhe oder abwärts höchstens 4 Blöcke.

#### Scenario: Lange nur Vollblöcke
- **WHEN** der Score unter 10 liegt
- **THEN** sind alle erzeugten Blöcke Vollblöcke

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
Ein neuer Block nach der Aufstiegsphase DARF NUR an einer Stelle entstehen, die all diese Bedingungen erfüllt:
- Sie ist waagrecht mindestens 16 Blöcke vom Lobby-Spawn entfernt.
- Unter ihr sind in der echten Welt mindestens 6 Blöcke Luft.
- An ihr ist in der echten Welt Luft.
- Über ihrer Oberkante ist so viel freier Raum, dass der Spieler dort stehen und von dort aus springen kann. Das sind seine Körperhöhe plus die Höhe eines Sprungs.
- Ihre Flugbahn vom vorigen Block aus ist bis zur Scheitelhöhe des Sprungs frei.
- Sie überschneidet keinen sichtbaren Block des eigenen Laufs.
- Sie liegt innerhalb der Höhengrenzen der Lobby, mit dem Abstand aus der Anforderung „Kein Teleport durch die Höhengrenzen“.

Ein neuer Block und die Flugbahn dorthin DÜRFEN NICHT in einem Lobby-Portal oder näher als 3 Blöcke an einem liegen, damit ein Läufer nie versehentlich auf einen anderen Server geschickt wird. Ein neuer Block DARF NUR gewählt werden, wenn von ihm aus mindestens ein weiterer gültiger Sprung möglich ist. Unter den gültigen Stellen MUSS die Lobby solche mit mehr Luft darunter, vor allem in der Senkrechten, und drumherum bevorzugen, ohne dass dadurch die Schwierigkeit nach dem Score verloren geht. Findet die Lobby trotzdem keinen gültigen nächsten Block, MUSS der Lauf mit dem erreichten Score enden.

#### Scenario: Nicht über Wegen
- **WHEN** eine Stelle direkt über einem Weg liegt, sodass unter ihr weniger als 6 Blöcke Luft sind
- **THEN** entsteht dort nach der Aufstiegsphase kein Block

#### Scenario: Nicht zurück zum Spawn
- **WHEN** eine sonst gültige Stelle waagrecht weniger als 16 Blöcke vom Spawn entfernt liegt
- **THEN** entsteht dort nach der Aufstiegsphase kein Block

#### Scenario: Ins Leere bevorzugt
- **WHEN** zwei gleich schwere Stellen möglich sind, eine über offenem Raum und eine dicht neben einem Gebäude
- **THEN** wählt die Lobby die Stelle über offenem Raum

#### Scenario: Portal in der Nähe
- **WHEN** in Laufrichtung ein Lobby-Portal liegt
- **THEN** führt kein Block und keine Flugbahn in das Portal oder näher als 3 Blöcke daran

#### Scenario: Wand im Weg
- **WHEN** in Laufrichtung eine Wand der Lobby steht
- **THEN** entsteht kein Block in oder hinter der Wand, sondern der Lauf führt an ihr vorbei

#### Scenario: Keine Sackgasse
- **WHEN** ein Kandidat zwar frei ist, von ihm aus aber kein weiterer Sprung möglich wäre
- **THEN** wird er nicht gewählt

#### Scenario: Gar kein Platz mehr
- **WHEN** von der aktuellen Stelle aus kein gültiger Block mehr möglich ist
- **THEN** endet der Lauf mit dem erreichten Score

#### Scenario: Nicht unter eine Decke
- **WHEN** eine sonst gültige Stelle 2 Blöcke unter der Unterseite einer Insel oder eines Überhangs liegt, sodass der Spieler dort stehen, aber nicht springen könnte
- **THEN** entsteht dort kein Block

#### Scenario: Flugbahn unter einem Überhang
- **WHEN** zwischen zwei Blöcken ein Überhang so tief hängt, dass er die Scheitelhöhe des Sprungs schneidet
- **THEN** wird dieser Sprung nicht erzeugt

#### Scenario: Jeder Block ist ein Absprung
- **WHEN** ein Spieler auf einem beliebigen Block eines Laufs steht
- **THEN** ist über ihm bis zur Scheitelhöhe eines Sprungs nur Luft

### Requirement: Zielgerichteter Verlauf
Der Parcours MUSS eine Hauptrichtung verfolgen, die sich nur allmählich ändert. Nach der Aufstiegsphase MUSS die Hauptrichtung schlangenförmig hin und her pendeln und den Parcours mit der Zeit um den Spawn herumführen, statt nur vom Spawn weg: Der Parcours MUSS sich in einem Ring von 20 bis 60 Blöcken waagrechtem Abstand zum Spawn halten, soweit Platz ist. Ein neuer Block DARF NICHT entgegen der aktuellen Hauptrichtung liegen. Ein neuer Block und die Flugbahn dorthin DÜRFEN waagrecht keinem früheren sichtbaren Block des Laufs außer dem Absprungblock näher als 2 Blöcke kommen, damit frühere Blöcke beim Springen nie im Weg sind. Unter den gültigen Stellen MUSS die Lobby solche bevorzugen, die der Hauptrichtung am besten folgen.

#### Scenario: Schlangenlinie
- **WHEN** ein Läufer 40 Punkte erreicht
- **THEN** hat der Parcours mehrfach die Seite gewechselt (Links- und Rechtsbögen) und ist dabei mindestens einmal wieder näher an den Spawn herangekommen, ohne den Abstand von 16 Blöcken zu unterschreiten

#### Scenario: Kein Zurück
- **WHEN** der Parcours nach Osten läuft
- **THEN** liegt kein neuer Block westlich seines Absprungblocks

#### Scenario: Frühere Blöcke nicht im Weg
- **WHEN** ein Kandidat waagrecht direkt neben dem vorletzten Block läge
- **THEN** wird er nicht gewählt

### Requirement: Schwierigkeitsmodi
Ein Spieler MUSS zwischen den Modi Easy, Medium, Hard, Rainbow und Ultra wählen können, indem er das Jump-and-Run-Item mit gedrückter Schleichtaste rechtsklickt; jeder solche Klick MUSS zum nächsten Modus wechseln (Easy → Medium → Hard → Rainbow → Ultra → Easy) und den gewählten Modus in der Sprache des Spielers anzeigen. Außerhalb eines Laufs DARF ein Klick mit Schleichtaste KEINEN Lauf starten; während eines Laufs DARF er den Modus NICHT wechseln und den Lauf NICHT beenden. Standard ist Medium; der gewählte Modus gilt, bis der Spieler die Lobby verlässt. Ein Lauf behält den Modus, mit dem er gestartet wurde:
- Easy: nur Vollblöcke und Stufen, Lücke höchstens 2, seltene Aufstiege, Schwierigkeit steigt halb so schnell wie Medium.
- Medium: Verhalten wie in den übrigen Anforderungen beschrieben.
- Hard: Formen früher frei (Stufen und Falltüren ab 5, Zäune, Mauern, Scheiben und Gitter ab 10, Pfosten ab 20), Schwierigkeit steigt doppelt so schnell wie Medium.
- Rainbow: Schwierigkeit wie Medium; solange der Läufer auf einem Block steht, wechseln alle sichtbaren Laufblöcke alle `jumprun.rainbow.rerollTicks` Ticks (Standard 10) ihr Material, Position und Form bleiben.
- Ultra: Schwierigkeit wie Hard, ohne Umrandung; solange der Läufer auf einem Block steht, werden die Blöcke voraus alle `jumprun.ultra.rerollTicks` Ticks (Standard 40) an neuer Stelle mit neuer Form und neuem Material neu erzeugt (mit Aufstiegs- und Fall-Animation).
Rekorde MÜSSEN pro Modus getrennt geführt werden, und Score-Meldungen, Rekord-Meldungen und die Anzeige über dem Läufer MÜSSEN den Modus nennen.

#### Scenario: Modus wechseln
- **WHEN** ein Spieler ohne laufenden Lauf das Item mit Schleichtaste rechtsklickt, während Medium aktiv ist
- **THEN** ist Hard aktiv, er sieht den neuen Modus, und es startet kein Lauf

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

### Requirement: Optische Vielfalt der Blöcke
Jeder Block eines Laufs MUSS sein Material zufällig aus einer Auswahl passend zu seiner Form erhalten, gewichtet nach dem Gewicht jedes Materials. Auswahl und Gewichte MÜSSEN je Form im Abschnitt `jumprun.palettes.<form>` der Konfiguration stehen (Formen: `full`, `trapdoor`, `slab`, `fence`, `pane`, `post`; je Eintrag Block-Schlüssel und ganzzahliges Gewicht ≥ 0; Gewicht 0 schaltet ein Material ab). Die mitgelieferten Standardwerte MÜSSEN die heutige Auswahl enthalten (z. B. bunter Beton, Wolle und Terrakotta als Vollblock, verschiedene Holz- und Steinstufen, Holzzäune und Mauern, bunte Glasscheiben und Eisengitter). Ein unbekannter Block, ein Block, dessen Form nicht zur Form der Liste passt, ein negatives Gewicht oder eine Liste ohne Material mit Gewicht > 0 MUSS beim Start den Start abbrechen, mit vollständigem Schlüssel und Grund. Für Änderungen zur Laufzeit gelten die allgemeinen Regeln der Lobby-Konfiguration. Das Material DARF die Schwierigkeit und die Schaffbarkeit eines Sprungs NICHT verändern.

#### Scenario: Gewichtete Auswahl
- **WHEN** `jumprun.palettes.full` nur `white_concrete` mit Gewicht 3 und `black_wool` mit Gewicht 1 enthält und viele Vollblöcke erzeugt werden
- **THEN** sind etwa drei Viertel weißer Beton und ein Viertel schwarze Wolle

#### Scenario: Material abschalten
- **WHEN** die `application.yaml` des Betreibers `jumprun.palettes.full.white_concrete: 0` setzt
- **THEN** erscheint nie weißer Beton als Vollblock, alle anderen Vollblock-Materialien bleiben

#### Scenario: Falscher Block in der Liste
- **WHEN** `jumprun.palettes.fence` den Block `stone` enthält
- **THEN** startet die Lobby nicht, und die Meldung nennt `jumprun.palettes.fence` und den Grund

#### Scenario: Material wechselt
- **WHEN** ein Spieler mehrere Vollblöcke hintereinander sieht
- **THEN** haben sie nicht alle dasselbe Material

#### Scenario: Material ändert nichts an der Form
- **WHEN** ein Zaun aus Eichenholz und eine Bruchsteinmauer an derselben Stelle möglich wären
- **THEN** gelten für beide dieselbe Oberkante und dieselben Kosten

### Requirement: Laufende
Ein Lauf MUSS enden, wenn der Spieler mehr als drei Blöcke unter den Block fällt, auf dem er zuletzt gelandet ist, wenn er das Jump-and-Run-Item erneut benutzt, wenn er stirbt, wenn er zum Lobby-Spawn zurückkehrt (per Befehl oder Navigator, siehe `lobby-spawn-return`) oder wenn er die Lobby verlässt. Bei einem Absturz MUSS die Lobby ihn an den Startpunkt seines Laufs zurücksetzen. Eine Rückkehr zum Spawn MUSS den Lauf beenden wie ein Abbruch über das Item, also mit gewertetem Score, bevor der Spieler versetzt wird; der Spieler landet am Spawn, nicht am Startpunkt des Laufs. Während eines Laufs DARF der Läufer KEINE Elytra tragen, damit ein Leertastendruck in der Luft kein Gleiten auslöst; mit dem Laufende MUSS er seine normale Lobby-Ausstattung zurückerhalten. Nach dem Ende DÜRFEN keine Blöcke des Laufs für ihn sichtbar bleiben, und an ihren Stellen MUSS er wieder die echte Welt sehen.

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
- **WHEN** der Spieler während eines Laufs das Jump-and-Run-Item benutzt
- **THEN** endet der Lauf, und alle Blöcke verschwinden

#### Scenario: Spieler verlässt die Lobby
- **WHEN** der Spieler während eines Laufs die Verbindung trennt
- **THEN** endet der Lauf, und die Lobby hält keinen Zustand dieses Laufs mehr

#### Scenario: Rückkehr zum Spawn im Lauf
- **WHEN** ein Läufer mit Score 12 im Lauf `/spawn` eingibt
- **THEN** endet der Lauf mit Score 12 und der Endmeldung wie bei einem Abbruch, alle Blöcke verschwinden, er hat seine Ausstattung zurück und steht am Lobby-Spawn

### Requirement: Score und Rekord
Während eines Laufs MUSS der Spieler seinen aktuellen Score in der Action Bar sehen. Am Ende eines Laufs MUSS er eine Meldung mit dem erreichten Score erhalten. Übertrifft ein Lauf den Rekord des Spielers in diesem Modus, MUSS die Meldung das als neuen Rekord kennzeichnen.

Ist eine Datenbank konfiguriert, gilt Folgendes:
- Jeder Lauf, dessen Ende den Score wertet, MUSS mit Spieler, aktuellem Spielernamen, Modus, Score, Endgrund und Zeitpunkt dauerhaft gespeichert werden, auch bei Score 0.
- Der Rekord eines Spielers in einem Modus MUSS der höchste gespeicherte Score dieses Spielers in diesem Modus sein. Er gilt in jeder Lobby und über Verlassen und Neustart hinweg.
- Die Rekorde eines Spielers MÜSSEN geladen sein, bevor er die Lobby betritt.
- Ein gerade beendeter Lauf MUSS sofort für den nächsten Lauf in derselben Lobby zählen, auch wenn er noch nicht gespeichert ist.

Ohne Datenbank MUSS die Lobby den Rekord pro Spieler und Modus im Speicher halten, solange der Spieler in der Lobby ist, und ihn beim Verlassen löschen.

#### Scenario: Neuer Rekord
- **WHEN** ein Spieler mit bisherigem Rekord 12 einen Lauf mit Score 15 beendet
- **THEN** meldet die Lobby Score 15 als neuen Rekord, und sein Rekord ist danach 15

#### Scenario: Kein neuer Rekord
- **WHEN** derselbe Spieler danach einen Lauf mit Score 9 beendet
- **THEN** meldet die Lobby Score 9 ohne Rekord-Hinweis, und sein Rekord bleibt 15

#### Scenario: Rekord überdauert das Verlassen
- **WHEN** bei konfigurierter Datenbank ein Spieler mit Hard-Rekord 15 die Lobby verlässt und später eine andere Lobby betritt
- **THEN** ist sein Hard-Rekord dort 15, und ein Lauf mit Score 15 ist kein neuer Rekord

#### Scenario: Jeder gewertete Lauf wird gespeichert
- **WHEN** bei konfigurierter Datenbank ein Spieler einen Lauf mit Score 0 durch Absturz beendet
- **THEN** steht dieser Lauf mit Score 0, Modus und Endgrund in der Historie

#### Scenario: Rekord endet mit dem Verlassen
- **WHEN** ohne Datenbank ein Spieler die Lobby verlässt und später wiederkommt
- **THEN** hat er keinen Rekord mehr, und sein nächster Lauf mit Score > 0 ist ein neuer Rekord

#### Scenario: Speichern schlägt fehl
- **WHEN** bei konfigurierter Datenbank das Speichern eines Laufs fehlschlägt
- **THEN** bekommt der Spieler trotzdem seine Endmeldung, und der Rekord gilt in dieser Lobby bis zum Verlassen

### Requirement: Ton bei neuem Rekord
Übertrifft der Score eines Läufers während des Laufs zum ersten Mal seinen bisherigen Rekord, MUSS der Läufer sofort das Levelaufstiegs-Geräusch hören, höchstens einmal pro Lauf. Hat der Spieler noch keinen Rekord, MUSS das Geräusch mit der Meldung über den neuen Rekord am Laufende erklingen. Alle Töne des Laufs MÜSSEN dem Läufer folgen und DÜRFEN NICHT abbrechen, wenn er dabei versetzt wird (z. B. beim Zurücksetzen nach einem Absturz). Andere Spieler DÜRFEN es NICHT hören.

#### Scenario: Rekord im Lauf gebrochen
- **WHEN** ein Spieler mit Rekord 12 im Lauf Score 13 erreicht und danach weiter bis 20 springt
- **THEN** hört er bei Score 13 genau einmal das Levelaufstiegs-Geräusch und bei 14 bis 20 nicht erneut

#### Scenario: Rekord-Ton beim Absturz
- **WHEN** ein Spieler ohne bisherigen Rekord mit Score 5 abstürzt und an den Startpunkt zurückgesetzt wird
- **THEN** hört er das Levelaufstiegs-Geräusch vollständig

#### Scenario: Erster Rekord
- **WHEN** ein Spieler ohne bisherigen Rekord einen Lauf mit Score 5 beendet
- **THEN** hört er mit der Rekord-Meldung das Levelaufstiegs-Geräusch

### Requirement: Texte in der Sprache des Spielers
Alle Texte des Jump and Run (Score in der Action Bar, Meldungen zu Start, Ende und Rekord) MÜSSEN in der Sprache des Spielers erscheinen. Fehlt seine Sprache, MUSS Englisch erscheinen. Das Item trägt den Spielnamen „Jump & Run“, der in allen Sprachen gleich ist, und eine englische Beschreibung (Lore), die die Bedienung (Rechtsklick startet und beendet, Schleichen und Rechtsklick wechselt den Modus) und die drei Modi Easy, Medium und Hard kurz erklärt. Ein Item ist für alle Spieler gleich und kann deshalb nicht übersetzt werden.

#### Scenario: Deutscher Client
- **WHEN** ein Spieler mit deutscher Client-Sprache einen Lauf beendet
- **THEN** ist die Meldung zum Laufende deutsch

#### Scenario: Unbekannte Sprache
- **WHEN** ein Spieler mit einer Client-Sprache ohne Übersetzung einen Lauf beendet
- **THEN** ist die Meldung zum Laufende englisch

### Requirement: Kein Teleport durch die Höhengrenzen
Ein Lauf DARF einen Läufer NIE so hoch oder so tief führen, dass die Lobby ihn wegen ihrer Höhengrenzen zum Spawn teleportiert. Jeder Block eines Laufs, auch in der Aufstiegsphase, MUSS so weit über der unteren Höhengrenze liegen, dass ein Absturz von ihm den Lauf beendet und den Spieler an den Startpunkt zurücksetzt, bevor er die Grenze erreicht. Jeder Block MUSS so weit unter der oberen Höhengrenze liegen, dass der Spieler auch im Scheitelpunkt eines Sprungs darunter bleibt. Kann ein Lauf an der Stelle des Spielers nicht so beginnen, MUSS der Start mit der Meldung „kein Platz zum Starten“ ausbleiben. Die Höhengrenzen sind die der Lobby; ändert der Betreiber sie, MUSS der nächste neue Block die neuen Grenzen beachten.

#### Scenario: Absturz knapp über der Untergrenze
- **WHEN** ein Läufer vom tiefsten erlaubten Block senkrecht abstürzt
- **THEN** endet der Lauf, und er steht am Startpunkt des Laufs, nicht am Lobby-Spawn

#### Scenario: Kein Block nahe der Untergrenze
- **WHEN** eine sonst gültige Stelle so tief liegt, dass ein Absturz von ihr die untere Höhengrenze erreichen würde, bevor der Lauf endet
- **THEN** entsteht dort kein Block

#### Scenario: Kein Block nahe der Obergrenze
- **WHEN** eine sonst gültige Stelle so hoch liegt, dass der Spieler im Scheitelpunkt eines Sprungs von ihr die obere Höhengrenze überschreiten würde
- **THEN** entsteht dort kein Block

#### Scenario: Start zu nah an einer Grenze
- **WHEN** ein Spieler einen Lauf an einer Stelle starten will, an der die Aufstiegsphase das erlaubte Höhenband verlassen würde
- **THEN** startet kein Lauf, und er erhält die Meldung „kein Platz zum Starten“ in seiner Sprache

### Requirement: Sidebar während des Laufs
Während eines Laufs MUSS der Läufer rechts eine Sidebar sehen. Sie MUSS den Spielnamen mit dem Modus des Laufs als Titel tragen und den aktuellen Score sowie seinen Rekord in diesem Modus zeigen. Score und Rekord MÜSSEN als Werte in einer eigenen, rechtsbündigen Spalte stehen. Ist eine Datenbank konfiguriert und gibt es in diesem Modus mindestens einen Rekord, MUSS sie zusätzlich unter einer Überschrift die bis zu drei Spieler mit den höchsten Rekorden in diesem Modus zeigen, in absteigender Reihenfolge, je mit Spielerkopf, Spielername und Rekord, wobei der Rekord den Platz durch seine Farbe (Gold, Silber, Bronze) kenntlich macht. Jeder Spieler darf dabei nur einmal vorkommen. Bei gleichem Rekord steht vorn, wer ihn früher erreicht hat. Ist der Läufer selbst unter den drei, MUSS seine Zeile hervorgehoben (Markierung, fetter Name) erscheinen.

Die Sidebar MUSS sich bei jeder Score-Änderung aktualisieren. Rekorde aus anderen Lobbys DÜRFEN mit einer Verzögerung von höchstens einer Minute erscheinen, ein eigener neuer Rekord MUSS sofort einsortiert werden. Außerhalb eines Laufs DARF der Spieler KEINE Jump-and-Run-Sidebar sehen, und er DARF NIE Werte eines anderen Modus als des laufenden sehen. Andere Spieler DÜRFEN die Sidebar des Läufers NICHT sehen. Die Beschriftungen „Score“, „Rekord“ und die Überschrift der Bestenliste MÜSSEN in der Sprache des Spielers erscheinen, mit Englisch als Fallback. Titel, Modusname und Spielernamen sind sprachneutral. Die Action Bar mit dem Score bleibt zusätzlich bestehen.

#### Scenario: Sidebar beim Start
- **WHEN** ein Spieler mit Hard-Rekord 42 einen Lauf in Hard startet
- **THEN** sieht er rechts eine Sidebar mit Titel „Jump & Run · Hard“, Score 0 und Rekord 42 in der Wertespalte

#### Scenario: Score steigt
- **WHEN** der Läufer Score 7 erreicht
- **THEN** zeigt die Sidebar Score 7, und die Action Bar zeigt ebenfalls 7

#### Scenario: Top 3 des Modus
- **WHEN** in Hard die Rekorde Alex 88, Steve 61, Notch 42 und Jeb 30 gespeichert sind und Jeb einen Lauf in Hard startet
- **THEN** zeigt seine Sidebar in dieser Reihenfolge Alex 88 (gold), Steve 61 (silber), Notch 42 (bronze), je mit Kopf, und keine dieser Zeilen ist hervorgehoben

#### Scenario: Eigener Platz fett
- **WHEN** Steve mit Hard-Rekord 61 einen Lauf in Hard startet
- **THEN** ist seine Zeile (Steve 61, silber) hervorgehoben, die anderen nicht

#### Scenario: Neuer Rekord rückt sofort auf
- **WHEN** Jeb im laufenden Hard-Lauf Score 50 erreicht
- **THEN** zeigt seine Sidebar sofort Jeb 50 auf dem dritten Platz (bronze) hervorgehoben, und Notch ist nicht mehr unter den drei

#### Scenario: Nur der eigene Modus
- **WHEN** ein Spieler mit Easy-Rekord 30 und Hard-Rekord 8 einen Lauf in Hard startet
- **THEN** zeigt die Sidebar Rekord 8 und die Top 3 von Hard, nichts aus Easy

#### Scenario: Sidebar verschwindet
- **WHEN** der Lauf endet, egal aus welchem Grund
- **THEN** sieht der Spieler keine Jump-and-Run-Sidebar mehr

#### Scenario: Nicht für andere
- **WHEN** A einen Lauf spielt und B ihm zusieht
- **THEN** sieht B keine Jump-and-Run-Sidebar

#### Scenario: Ohne Datenbank
- **WHEN** ohne Datenbank ein Spieler einen Lauf startet
- **THEN** zeigt die Sidebar Titel, Score und Rekord, aber keine Überschrift und keine Top 3

#### Scenario: Deutscher Client
- **WHEN** ein Spieler mit deutscher Client-Sprache einen Lauf startet
- **THEN** zeigt die Sidebar die deutschen Beschriftungen, Spielernamen und Modus bleiben unverändert
