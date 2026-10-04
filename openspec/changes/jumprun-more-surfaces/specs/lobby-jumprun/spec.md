# Spec Delta

## MODIFIED Requirements

### Requirement: Schwierigkeit steigt mit dem Score
Nach der Aufstiegsphase MUSS die Schwierigkeit jedes neuen Sprungs aus einer festen, stetig steigenden Funktion des Scores folgen, die sich einem Höchstwert annähert, ohne ihn zu überschreiten. Die Schwierigkeit MUSS langsam steigen: Bis Score 10 MÜSSEN alle Blöcke Vollblöcke sein, Stufen (halbe Platten), Falltüren, Treppen, Teppiche und Schneeschichten DÜRFEN erst ab Score 10, Zäune, Mauern, Scheiben, Gitter, Köpfe und Blumentöpfe erst ab Score 25 und schmale Pfosten und Kerzen erst ab Score 40 vorkommen. Die Schwierigkeit MUSS sich in der Form des Blocks (Vollblock, Falltür, Stufe, Treppe, Teppich, Schneeschicht, Zaun oder Mauer, Glasscheibe oder Gitter, Kopf, Blumentopf, Kerze, schmaler Pfosten), in der Lückenbreite und im Höhenunterschied zeigen. Jeder erzeugte Sprung MUSS ohne Hilfsmittel schaffbar sein. Maßgeblich ist die Oberkante der Lauffläche, also z. B. bei einer Falltür knapp ein Fünftel, bei einer Stufe ein halber Block, bei einem Teppich ein sechzehntel Block, bei einer Treppe ein Block (obere Stufe; die untere liegt einen halben Block tiefer) und bei einem Zaun oder einer Mauer anderthalb Blöcke über ihrer Blockposition. Die Oberkante des Ziels (bei einer Treppe die obere Stufe) darf höchstens einen Block über der des Ausgangsblocks (bei einer Treppe die untere Stufe) liegen. Die Lücke darf bei einem Aufstieg höchstens 3 Blöcke betragen, auf gleicher Höhe oder abwärts höchstens 4 Blöcke.

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

#### Scenario: Treppe als Aufstieg
- **WHEN** ein Sprung von einer Treppe auf einen Vollblock auf gleicher Blockhöhe erzeugt wird
- **THEN** gilt er als Aufstieg um einen halben Block, und kein Sprung von oder auf eine Treppe überschreitet die Grenzen für Lücke und Aufstieg

### Requirement: Schwierigkeitsmodi
Ein Spieler MUSS zwischen den Modi Easy, Medium, Hard, Rainbow und Ultra wählen können, indem er das Jump-and-Run-Item mit gedrückter Schleichtaste rechtsklickt; jeder solche Klick MUSS zum nächsten Modus wechseln (Easy → Medium → Hard → Rainbow → Ultra → Easy) und den gewählten Modus in der Sprache des Spielers anzeigen. Außerhalb eines Laufs DARF ein Klick mit Schleichtaste KEINEN Lauf starten; während eines Laufs DARF er den Modus NICHT wechseln und den Lauf NICHT beenden. Standard ist Medium; der gewählte Modus gilt, bis der Spieler die Lobby verlässt. Ein Lauf behält den Modus, mit dem er gestartet wurde:
- Easy: nur Vollblöcke und Stufen, Lücke höchstens 2, seltene Aufstiege, Schwierigkeit steigt halb so schnell wie Medium.
- Medium: Verhalten wie in den übrigen Anforderungen beschrieben.
- Hard: Formen früher frei (Stufen, Falltüren, Treppen, Teppiche und Schneeschichten ab 5, Zäune, Mauern, Scheiben, Gitter, Köpfe und Blumentöpfe ab 10, Pfosten und Kerzen ab 20), Schwierigkeit steigt doppelt so schnell wie Medium.
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
Jeder Block eines Laufs MUSS sein Material zufällig aus einer Auswahl passend zu seiner Form erhalten, gewichtet nach dem Gewicht jedes Materials. Auswahl und Gewichte MÜSSEN je Form im Abschnitt `jumprun.palettes.<form>` der Konfiguration stehen (Formen: `full`, `trapdoor`, `slab`, `stairs`, `carpet`, `snow`, `fence`, `pane`, `head`, `flower_pot`, `candle`, `post`; je Eintrag Block-Schlüssel und ganzzahliges Gewicht ≥ 0; Gewicht 0 schaltet ein Material ab). Die mitgelieferten Standardwerte MÜSSEN die heutige Auswahl enthalten (z. B. bunter Beton, Wolle und Terrakotta als Vollblock, verschiedene Holz- und Steinstufen und -treppen, bunte Teppiche, Schnee, Köpfe, Blumentöpfe und Kerzen, Holzzäune und Mauern, bunte Glasscheiben und Eisengitter). Ein unbekannter Block, ein Block, dessen Form nicht zur Form der Liste passt (bei `head`, `flower_pot` und `candle` zählt dazu auch eine andere Grundfläche), ein negatives Gewicht oder eine Liste ohne Material mit Gewicht > 0 MUSS beim Start den Start abbrechen, mit vollständigem Schlüssel und Grund. Für Änderungen zur Laufzeit gelten die allgemeinen Regeln der Lobby-Konfiguration. Das Material DARF die Schwierigkeit und die Schaffbarkeit eines Sprungs NICHT verändern.

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

#### Scenario: Falsche Grundfläche in der Liste
- **WHEN** `jumprun.palettes.head` einen Block enthält, dessen Kollisionsbox in `x` oder `z` nicht die der Kopf-Form ist
- **THEN** startet die Lobby nicht, und die Meldung nennt den vollen Schlüssel, die gefundene und die erwartete Grundfläche

#### Scenario: Material ändert nichts an der Form
- **WHEN** ein Zaun aus Eichenholz und eine Bruchsteinmauer an derselben Stelle möglich wären
- **THEN** gelten für beide dieselbe Oberkante und dieselben Kosten

## ADDED Requirements

### Requirement: Landen auf Formen mit mehreren Oberkanten und schmaler Fläche
Ein Block mit schmaler Standfläche oder mehreren Oberkanten MUSS als betreten gelten, sobald die Hitbox des Spielers die Standfläche in der Höhe ihrer Oberkante berührt. Maßgeblich sind die Stufen der Form, jede mit eigener Oberkante und Grundfläche:
- Treppe: die untere Stufe (halber Block) und die obere Stufe (ein Block), jeweils auf der Hälfte der Zelle, die zur Ausrichtung der Treppe gehört. Landen auf einer der beiden Stufen zählt als Landung auf dem Block.
- Kopf, Blumentopf und Kerze: ihre echte Grundfläche, nicht die ganze Zelle.
- Alle übrigen Formen: die ganze Zelle wie bisher.
Die Toleranz in der Höhe beträgt 0,05 Blöcke, die in der Fläche die halbe Breite der Hitbox (0,3 Blöcke) rund um die Stufe. Die Ausrichtung einer Treppe MUSS zufällig sein und gleich bleiben, wenn der Block nur sein Material wechselt (Rainbow). Die Sprungfreiheit über einer Treppe richtet sich nach ihrer oberen Stufe, ihr Absturz-Schwellwert nach ihrer unteren.

#### Scenario: Landen auf der unteren Stufe
- **WHEN** der Läufer auf der unteren Stufe einer Treppe landet, die der nächste Block ist
- **THEN** zählt der Sprung, und der Lauf endet nicht durch einen Absturz

#### Scenario: Landen auf der oberen Stufe
- **WHEN** der Läufer auf der oberen Stufe einer Treppe landet, die der nächste Block ist
- **THEN** zählt der Sprung wie bei der unteren Stufe

#### Scenario: Zwischen den Stufen
- **WHEN** der Läufer über einer Treppe auf einer Höhe zwischen den beiden Stufen gemeldet wird
- **THEN** zählt das nicht als Landung

#### Scenario: Neben der Kerze
- **WHEN** der Läufer in Höhe der Oberkante einer Kerze weiter neben ihr gemeldet wird, als seine Hitbox reicht
- **THEN** zählt das nicht als Landung

#### Scenario: Am Rand der Kerze
- **WHEN** der Läufer so auf einer Kerze steht, dass seine Hitbox ihre Grundfläche nur am Rand überlappt
- **THEN** zählt die Landung

#### Scenario: Teppich und Schnee
- **WHEN** der Läufer auf einem Teppich (Oberkante 1/16) oder einer Schneeschicht (drei Schichten) landet
- **THEN** zählt die Landung auf der ganzen Fläche des Blocks

#### Scenario: Treppe bleibt nach dem Materialwechsel gleich ausgerichtet
- **WHEN** im Modus Rainbow das Material einer Treppe wechselt
- **THEN** hat sie danach dieselbe Ausrichtung und dieselben Stufen

### Requirement: Köpfe des Teams
Köpfe als Laufblöcke MÜSSEN, solange die Liste `jumprun.heads.profiles` (Spieler-UUIDs, Standard leer) mindestens ein auflösbares Profil enthält, Spielerköpfe mit einem zufällig daraus gezogenen Profil sein, im gelandeten Block, im Falleffekt und in der Umrandung. Dasselbe Profil DARF NICHT zweimal hintereinander erscheinen, solange die Liste mindestens zwei Profile enthält. Die Liste MUSS wie die Paletten live gelesen werden; ein ungültiger Eintrag (keine UUID) MUSS die Änderung unwirksam lassen, mit Schlüssel und Grund protokolliert, und die letzte gültige Liste bleibt. Eine UUID, deren Skin sich nicht auflösen lässt, MUSS übersprungen und protokolliert werden. Ist die Liste leer oder bleibt kein Profil übrig, MÜSSEN die Köpfe aus `jumprun.palettes.head` ohne Profil gewählt werden. Ein fehlendes Netz DARF den Start eines Laufs oder der Lobby NICHT verhindern. Das Profil DARF Oberkante, Grundfläche und Schwierigkeit des Kopfes NICHT verändern.

#### Scenario: Teamkopf im Lauf
- **WHEN** `jumprun.heads.profiles` zwei UUIDs enthält und viele Köpfe erzeugt werden
- **THEN** zeigt jeder Kopf eines der beiden Profile, und beide kommen vor

#### Scenario: Nie derselbe Kopf hintereinander
- **WHEN** die Liste mindestens zwei Profile enthält und zwei Köpfe aufeinander folgen
- **THEN** haben sie nicht dasselbe Profil

#### Scenario: Leere Liste
- **WHEN** `jumprun.heads.profiles` leer ist
- **THEN** sind die Köpfe einfache Köpfe aus `jumprun.palettes.head` ohne Profil

#### Scenario: Ungültiger Eintrag zur Laufzeit
- **WHEN** der Betreiber einen Eintrag einträgt, der keine UUID ist
- **THEN** bleibt die letzte gültige Liste wirksam, und das Log nennt `jumprun.heads.profiles` und den Grund

#### Scenario: Skin nicht auflösbar
- **WHEN** eine UUID der Liste keinen Skin hat oder Mojang nicht erreichbar ist
- **THEN** wird sie übersprungen und mit ihrer UUID protokolliert, und der Lauf startet

#### Scenario: Rainbow wechselt den Kopf
- **WHEN** im Modus Rainbow der Läufer 10 Ticks auf einem Block steht und die Liste mindestens zwei Profile enthält
- **THEN** zeigen sichtbare Köpfe ein anderes Profil an derselben Stelle
