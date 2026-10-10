# Design

## Context

Motivation steht in `proposal.md` (Why). Ausgangslage im Code (`origin/main`):

- `Jump.isReachable`: Lücke `MIN_GAP` (1) bis `maxGap`, Aufstieg höchstens `MAX_RISE` (1,0). Ein Sprung ohne Lücke ist nicht erreichbar. `Phase.Scored` zieht Kandidaten aus Lücken und Rises −1 bis 1 in allen Richtungen, die Aufstiegsphase nur Vollblöcke mit Rise 1 und Lücke 1 bis 2.
- `Course`: `advanceTo(feet)` sucht den letzten Block voraus, auf dem der Läufer steht (`isStandingOn`: Höhe ± 0,05 und Mitte innerhalb der Zelle ± 0,3). `fallThreshold()` = tiefste Oberkante ab dem aktuellen Block minus 3, `hasFallen(y)` = `y < threshold`. Es gibt keinen Zustand „klettert“.
- Platz: `JumpRules.isFree` prüft Höhenband, Sprungfreiheit über dem Ziel und die Flugbahn gegen die echte Welt (`SpaceProbe`); `Clearance` hält Abstand zu sichtbaren Blöcken (nur über `Placement.pos()`); `CourseGenerator.occupiedBy` belegt je sichtbarem Block die Säule `pos.y` bis `jumpRoomTopY()` und meldet sie über `OccupiedProbe` als Nicht-Luft.
- Darstellung: ein Laufblock = eine Zelle. `FakeBlocks.show(player, blocks)` sendet einen `BlockChangePacket` je Block, `AnimatedBlock` hat genau ein Blockdisplay, `Outline` eine umrandete Zelle. Das Fenster hat bis zu zwei Blöcke vor und zwei hinter dem Läufer.
- Rainbow/Ultra (`Reroller`) wirken, solange der Läufer auf dem aktuellen Block steht (`standsOnCurrent`).
- Vanilla-Klettern: Der Client klettert, wenn der Block an der Fußzelle des Spielers im Tag `climbable` ist (Leiter, Ranke, Gerüst, Zwirn- und Trauerranken, Höhlenranken). Er rechnet es selbst und meldet Positionen; die Kollision der Leiter ist ein dünnes Brett auf der Rückseite, die der Ranke fehlt. Der Client prüft nicht, ob ein vom Server gesendeter Block „halten“ würde.

## Goals / Non-Goals

**Goals:**
- Ein Sprungtyp „Klettern“ in der ersten Version mit Leiter und Ranke, ohne neuen Fortschritts- oder Absturzzustand.
- Fortschritt und Absturz bleiben rein positionsbasiert.
- Kosten, Freischaltung und Platzprüfung wie bei den bestehenden Sprüngen, damit jeder erzeugte Abschnitt ohne Hilfsmittel schaffbar ist.

**Non-Goals:**
- Gerüst (Zustände `bottom`/`distance`, Absteigen durch Schleichen, eigene Kollision), Zwirn- und Trauerranken als freistehende Spalten, Höhlenranken. Als mögliche zweite Stufe in den offenen Fragen.
- Klettern als Teil der Aufstiegsphase.
- Punkte je Klettersprosse.
- Serverseitige Simulation des Kletterns oder Erkennung von „klettert“ (kein Zustand).
- Änderung der Sprungphysik der bestehenden Sprünge.

## Decisions

### D1 Aufbau: Turm aus Pfeiler, Leiter und Zielblock

Ein Kletterabschnitt vom Block `A` (Vollblock oder andere Form, aktuell oder nächster) zum Zielblock `B` in Achsenrichtung `d` (Nord, Süd, Ost, West) mit Höhe `H` (Rise von `A.topY` zu `B.topY`, Bereich 3 bis 7, Konfiguration `jumprun.climb.minHeight`/`maxHeight`):

```
Seitenansicht, Richtung d nach rechts, H = 3 (y wächst nach oben)

  y = A.y+3    [L] [B]     <- Zielblock, B.topY = A.topY + 3
  y = A.y+2    [L] [P]
  y = A.y+1    [L] [P]
  y = A.y      [A]
               Spalte von A   Spalte von B
               L = Leiter oder Ranke, P = Pfeiler (Fake-Blöcke)
```

- `B` liegt bei `A.pos + d + (0, H, 0)`, also direkt neben der Säule über `A` und `H` Blöcke höher. Der Pfeiler `P` füllt die Zellen unter `B` bis `A.y + 1`. Die Leiter sitzt in den Zellen über `A` an der Seite des Pfeilers, ihr `facing` ist `−d` (sie zeigt vom Pfeiler weg). Die Ranke trägt `east`/`west`/… passend zur Pfeilerseite (`d`).
- Der Läufer steht auf `A`; seine Fußzelle ist die unterste Leiterzelle, der Client klettert sofort bei Vorwärtsdruck. Oben tritt er auf `B`: Die oberste Leiterzelle liegt auf der Höhe von `B`, ihre Oberkante auf Höhe von `B.topY`, genau wie bei einer Vanilla-Leiter an einer Plattform.
- Der Pfeiler ist Material von `B`: gleiche Auswahl wie der Zielblock (`B` ist ein Vollblock der `full`-Palette). Rainbow/Ultra tauschen Pfeiler und Block gemeinsam, ein Turm sieht immer einheitlich aus. Die Leiter nutzt `jumprun.palettes.ladder` (`ladder`), die Ranke `jumprun.palettes.vine` (`vine`; Zustand setzt der Generator je `d`).
- **Alternative: Leiter an einem Fake-Pfeiler neben `A`, `B` nicht auf dem Pfeiler.** Verworfen: Dann wären zwei Blöcke zu sehen und der Zielblock ohne Auflage; der Turm mit `B` als Spitze ist eine Einheit und braucht keine Sonderfläche im Landemodell.
- **Alternative: Leiter an der Seite des vorhandenen `A`-Blocks.** Verworfen: `A` ist ein beliebiger Block (Zaun, Platte, Pfosten), die Fläche dahinter ist nicht tragend; ein eigener Pfeiler hat immer eine tragende Seite.
- **Alternative: freistehende Zwirn- oder Trauerranken-Spalte ohne Pfeiler.** Möglich (sie brauchen keine Stützfläche), aber das Ende der Spalte hängt in der Luft und bräuchte eine eigene Optik. Zweite Stufe.
- **Test:** Unit (`ClimbTowerTest`, ohne Server): Für jede der vier Achsenrichtungen und `H` 3 bis 5 liefert `Climb.cells(...)` genau die erwarteten Zellen (Leiter, Pfeiler, Ziel) mit dem richtigen `facing`/Seitenzustand. Die oberste Leiterzelle liegt auf `B.y`, die Zahl der Leiterzellen ist `H`, die der Pfeilerzellen `H − 1`.
- **SOLID:** SRP (`Climb` kennt die Zellen eines Turms, `Course` kennt sie nicht).

### D2 Datenmodell: Anbauten am Zielblock

`Placement` bekommt `List<Cell> attachments()` (Standard: leer; `Cell(BlockPos, Block)`). Ein `CourseBlock` für einen Kletterabschnitt trägt Pfeiler und Leiter als Anbauten und die Art (`Climb`: Richtung, Höhe, Leiter oder Ranke). `Spot` bekommt eine optionale `Climb`-Angabe ohne Material, `withMaterial` setzt die Zellen mit dem gezogenen Material. Die Anbauten gehören dem Zielblock: Sie erscheinen mit ihm und verschwinden, wenn er das Fenster verlässt. Ob die Blöcke gezeigt werden, entscheidet wie bisher das Sichtfenster (zwei Blöcke voraus).

- `FakeBlocks.show/reset` und `AnimatedBlock`/`Outline` arbeiten künftig auf `cells()` (Block plus Anbauten) statt auf `pos()` + `material()`. `AnimatedBlock` verwaltet eine Liste von Blockdisplays, die zusammen fallen, landen und aufsteigen.
- `Course.recolor` tauscht Pfeiler und Block gemeinsam und lässt Leiter/Ranke stehen. `Course.rerollAhead` erzeugt Türme wie jeden anderen Block neu.
- **Alternative: Pfeiler und Leitern als eigene Laufblöcke.** Verworfen: Sie würden das Fenster füllen (zwei Blöcke voraus wären sonst ein Pfeiler) und Score und Landeprüfung durcheinanderbringen.
- **Test:** Unit (`CourseTest`): Ein Kurs mit einem Turm zeigt im Fenster einen Block, dessen `cells()` `2H` Anbauten enthält; `window()` zählt Anbauten nicht als Blöcke. Integration (`Env`, `env.tick()`): `FakeBlocks.show` sendet für Block und alle Anbauten je einen `BlockChangePacket`; nach dem Verlassen des Fensters sendet `reset` die echte Welt für alle Zellen. Andere Spieler sehen nur Displays, kein Fake-Block.
- **SOLID:** OCP (ein Block mit Anbauten, Course und Fenster ändern sich nicht in ihrer Logik), DRY (`cells()` ist die eine Quelle für Anzeige und Belegung).

### D3 Fortschritt: Landen auf dem Zielblock, kein Klettern-Zustand

Der Fortschritt ergibt sich aus `Course.advanceTo(feet)` wie bisher: Der Läufer steht auf `B` (Höhe `B.topY ± 0,05`, Mitte in `B`s Zelle ± 0,3). Das gilt auch, wenn er `A` überspringt und etwa über eine andere Strecke dorthin kommt. Das Klettern erzeugt zwischen `A` und `B` keinen Score und keinen Zustand.

- **Entscheidung: Ein Turm = ein Sprung** (`score +1`, Landung auf `B`), unabhängig von `H`. Ein Punkt je Sprosse wäre ein Punkt je Position und schaltet den Rekord leicht um; der Schwierigkeitsanteil liegt im Turm selbst.
- **Warum positionsbasiert reicht:** Der Läufer muss ohnehin seine Position über `PlayerMoveEvent` melden; beim Klettern steigt `y` stetig. Der einzige Zustand, der zählt, ist „steht auf `B`“. Ein Läufer, der bis zur Hälfte klettert und herunterfällt, hat nichts erreicht und wird nicht belohnt.
- **Umgang mit `B.topY` und dem Ende der Leiter:** Die oberste Leiterzelle liegt auf `B.y`, ihre Oberkante auf `B.topY`. Der Läufer verlässt die Leiter mit Fußhöhe `B.topY`, genau dort liegt das Höhenband der Landeprüfung (± 0,05). Ob der Client ihn dort wirklich bei 0,05 oder bei einem Rand loslässt, prüft der Smoke-Test; falls nötig, verbreitert eine eigene Toleranz für Türme das Band.
- **Test:** Unit (`ClimbProgressTest`, `CourseTest`): Läufer auf `B` (Höhe `B.topY`) zählt einen Sprung; auf halber Höhe in der Leiterspalte zählt nichts; auf `B.topY − 0,2` zählt nichts; auf `B.topY + 0,02` zählt. Der Turm zählt genau einen Sprung für `H` 3, 4, 5. Integration (`Env`): Der Test lässt den Läufer per Positionen `A → Leiterzellen → B` laufen (ein Positions-Update je Tick, wie `JumprunMoveTest`) und prüft Score +1 nach der Landung, 0 währenddessen.
- **SOLID:** SRP (Fortschritt bleibt in `Course.advanceTo`, nichts Neues im Listener).

### D4 Absturz: Schwellwert genügt

`fallThreshold()` = tiefste Oberkante ab `current` minus 3. Ein Läufer im Schacht steht in der Spalte über `A`, seine Füße sind mindestens auf `A.topY` > Schwelle. Es braucht keine Ausnahme für „klettert“. Fällt er vom Turm und unterschreitet die Schwelle, endet der Lauf wie bei jedem Absturz; fällt er neben dem Turm, ebenfalls. Ranke und Leiter bremsen den Fall (der Client rechnet es), ändern am Schwellwert nichts.

- **Folge:** Ein Läufer, der vom Turm herunter auf einen *niedrigeren* Block des Laufs fällt (etwa `A`), hat den Lauf nicht verloren, weil `A` noch aktuell ist.
- **Test:** Unit (`CourseTest`): Auf jeder Fußhöhe von `A.topY` bis `B.topY` ist `hasFallen` falsch, bei `A.topY − 3,01` wahr. Integration: Läufer klettert halb hoch, wird dann auf `A.topY − 5` gemeldet: Lauf endet mit `FALL`.
- **SOLID:** KISS (keine Sonderregel, kein neuer Zustand).

### D5 Generator: Kletterkandidaten, Erreichbarkeit, Kosten, Freischaltung

- **Kandidaten:** In `Phase.Scored` (nicht in der Aufstiegsphase) zusätzlich zu den Sprüngen für jede der vier Achsenrichtungen genau eine Höhe `H`, die D10 aus dem Score bestimmt, als `Spot` mit `Climb` bei `A.pos + d + (0, H, 0)`.
- **Erreichbarkeit:** `Jump.isReachable` bekommt einen Zweig: Für einen Kletter-Zielblock gilt Lücke 0 (Betragsabstand 1 in genau einer Achse), `H` im Bereich und `H > Jump.MAX_RISE`. Die normale Rise-/Lückenregel gilt für Türme nicht. Das Ziel ist von `A` aus auch ohne Hilfsmittel erreichbar, weil die Höhe nur zu Fuß über die Leiter gewonnen wird.
- **Kosten:** `Jump.cost` = `CLIMB_COST (2,0) + CLIMB_HEIGHT_WEIGHT (2,1) · (H − 3)`; Leiter und Ranke ohne Unterschied (`typeCost(Ziel)` ist 0, Vollblock). Die Steigung ist so gewählt, dass der höchste Turm, den die Konfiguration zulässt (`Climb.MAX_LIMIT` = 8), genau die Kosten des schwersten Sprungs im Modus Medium hat: H 3 kostet 2,0, H 4 4,1, H 5 6,2, H 8 12,5. `Jump.MAX_COST` bleibt damit 12,5 und ist weiter eine echte Obergrenze. Mit 0,5 je Block (Stand vor D10) erreichte kein Turm die Zielkosten ab Score 80 (Ziel etwa 7,9).
- **Freischaltung (`Mode`):** Easy nie. Medium ab Score 30, Hard ab Score 15; Leiter ab diesen Werten, Ranke 10 Punkte später (Medium 40, Hard 25). Die Ranke ist schwerer zu erkennen (keine Kollision, nur Sicht), daher später.
- **Heading:** Der Turm führt in Achsenrichtung `d`. Zeigt `d` gegen das Heading des Kurses, fällt der Kandidat wie jeder andere Sprung weg.
- **Test:** Unit (`JumpTest`, `ModeTest`, `ModeGenerationTest`, feste Seeds): Erreichbarkeit nur für Lücke 0 und `H` 3 bis 5; Kosten für H = 3, 4, 5; Freischaltung je Modus und Score; Easy erzeugt nie Türme; in Medium bei Score 80 kommen Türme vor; jede erzeugte Strecke hat bei jedem Turm Lücke 0 und `H` im Bereich.
- **SOLID:** OCP (Klettern ist ein weiterer Sprungtyp neben den Formen), DRY (Kosten bleiben in `Jump`).

### D6 Platzprüfung: Pfeiler, Leiter, Kopffreiheit, Höhenband

Alle Zellen des Turms (Pfeiler, Leiter) und die Säule bis `B.jumpRoomTopY()` über der Leiterspalte müssen in der echten Welt Luft sein (`SpaceProbe.isAir`), und `JumpRules.isFree` gilt für `B` (Höhenband, Sprungfreiheit über `B`). Zusätzlich:

- **Belegung:** `occupiedBy` meldet alle Anbauzellen eines sichtbaren Turms als belegt, damit kein späterer Block hineinfällt.
- **Abstand:** `Clearance` rechnet für einen Turm mit den Spalten von Leiter und Pfeiler statt nur mit `pos()`. Die Quelle `A` bleibt ausgenommen.
- **Flugbahn:** Entfällt, es gibt keinen Flug.
- **Höhenband:** `band.allows(B)` (Obergrenze mit Sprungfreiheit, Untergrenze mit Abstand). Weil `B` höher als `A` liegt, ist die Obergrenze die bindende.
- **Test:** Unit (`JumpRulesTest`, Fake-`SpaceProbe`; `ClearanceTest`): Block in Pfeilerzelle, in Leiterzelle, in der Kopffreiheit über der Leiterspalte → ungültig; Turm unter der Obergrenze des Bands gültig, 1 darüber ungültig; sichtbarer Block innerhalb von zwei Zellen neben den Spalten → Abstand verletzt; `occupiedBy` enthält alle Anbauzellen.
- **SOLID:** SRP (`JumpRules` fragt Zellen ab, die `Climb` liefert).

### D7 Konfiguration und Prüfung

```yaml
jumprun:
  climb:
    # Height of a tower in blocks (rise of the target block's top over the block it starts from).
    minHeight: 3
    maxHeight: 7
  palettes:
    ladder:
      ladder: 1
    vine:
      vine: 1
```

`JumprunSettings.palettes` und die Prüfung kennen zwei weitere Abschnitte, die statt der Kollisionshöhe das Tag `climbable` prüfen (Leiter und Ranke; über die Tag-Registry von Minestom, sonst eine feste Erlaubnisliste im Code). `jumprun.climb.minHeight`/`maxHeight` werden beim Lauf-Start gelesen, mit denselben Regeln wie `rerollTicks` (Ganzzahl, `minHeight ≥ 2`, `maxHeight ≥ minHeight`, `maxHeight ≤ 8`); eine ungültige Änderung wird protokolliert und der letzte gültige Wert bleibt.

- **Test:** Unit (`JumprunSettingsTest`, `JumprunConfigTest`): Standardwerte gültig; `stone` in `ladder` abgelehnt mit Schlüssel und Grund; `minHeight: 1`, `maxHeight < minHeight`, nicht numerisch abgelehnt; gültige Änderung zur Laufzeit wird beim nächsten Start gelesen.

### D8 Darstellung und Umrandung

- **Fake-Blöcke:** Pfeiler, Leiter und Ziel als `BlockChangePacket` an den Läufer, sobald der Zielblock gelandet ist. Der Läufer sieht die Leiter damit erst, wenn der Turm steht.
- **Falleffekt:** `AnimatedBlock` zeigt für alle ein Blockdisplay je Zelle, die gemeinsam fallen und aufsteigen. Je Turm sind das bis zu `2H` Displays (H = 5: zehn). Die Zahl ist durch das Fenster begrenzt (höchstens zwei Türme gleichzeitig voraus).
- **Umrandung:** Wie bisher umrandet sie nur den Zielblock `B`, nicht die Leiter. Der Läufer sieht das Ziel, den Weg hinauf zeigt die Leiter.
- **Test:** Integration (`Env`, `env.tick()`): `FakeBlocks`-Pakete, Anzahl der Displays je Turm (`2H`), Umrandung nur an `B`, Abräumen (kein Display bleibt zurück nach Ende und Shutdown).

### D10 Turmhöhe wächst mit dem Score

Die Höhe eines Turms folgt dem Score, damit seine Kosten der Zielschwierigkeit folgen und Türme bei jedem Score ab ihrer Freischaltung zur Wahl stehen. Ein fester Anteil für Türme wurde verworfen, weil er die Schwierigkeit ignoriert.

- **Höhe:** Aus den Zielkosten ohne Rauschen, `level(Score) · maxCost(Modus, freigeschaltete Formen)`, folgt `H = round(3 + (Zielkosten − 2,0) / 2,1)`. Das Ergebnis wird auf `[jumprun.climb.minHeight, jumprun.climb.maxHeight]` geklemmt und, wenn der Zielblock nicht in das Höhenband der Lobby passt, um je eine Stufe verkürzt, bis er passt (`HeightBand.allows`).
- **Funktion:** rein und deterministisch, eine kleine Methode in `Difficulty`. Sie nimmt Modus, Score und die Grenzen der Konfiguration. Das Rauschen der Zielkosten wählt weiterhin unter den Kandidaten; es verschiebt die Höhe nicht.
- **Monoton:** Das Ziel steigt mit dem Score nicht fallend, weil Formen nur freigeschaltet werden. Also steigt auch die Höhe nicht fallend.
- **Standardgrenzen 3 bis 7 (Entscheidung nach dem Rebase auf `main` mit #409):** `Climb.MAX_HEIGHT` und `jumprun.climb.maxHeight` sind 7, ein einziger Wert. Medium bleibt von selbst unter der Obergrenze: Das Ziel bei Score 80 ist etwa 4,1 (Schwierigkeit 6,5), also Höhe 4. Hard erreicht bei Score 80 die Obergrenze 7 (Ziel etwa 10,8, Kosten 10,4). Die Höhe folgt dem Ziel je Modus, die Grenze klemmt nur.
- **Höhenband:** Eine Höhe 7 passt nur, wenn der Zielblock in das Höhenband passt; sonst verkürzt die Schleife in `CourseGenerator.towers` die Höhe, bis sie passt (Test: `HardMakesTheTallestTower...`, Walk mit Band-Prüfung).
- **Alternative: fester Anteil für Türme je Score.** Verworfen (Entscheidung der Nutzer): ignoriert die Schwierigkeit.
- **Alternative: Höhe würfeln.** Verworfen: Höhe und Ziel würden sich gegenseitig verschieben, und Tests wären nicht deterministisch.
- **Test:** Unit (`DifficultyTest`): Höhe monoton im Score; geklemmt auf die Grenzen; Kosten bei Score 0, 40 und 80 innerhalb einer halben Höhenstufe (1,05) der Zielkosten, sofern die Höhe im Bereich 3 bis 8 liegt; bei Score 0 die Mindesthöhe. Unit (`ModeGenerationTest`, feste Seeds): In Medium kommen bei Score 80 Türme vor, mit den Standardgrenzen.
- **SOLID:** SRP (Höhe in `Difficulty`, Erzeugung in `CourseGenerator`), OCP (Grenzen kommen von außen).

### D9 Rainbow und Ultra

Rainbow tauscht Material von Pfeiler und Block und lässt die Leiter; Ultra erzeugt den Bereich voraus neu, auch einen Turm. Der Reroller wirkt nur, solange der Läufer auf dem aktuellen Block steht (`standsOnCurrent`); steht er an der Leiter nicht auf `A`, tauscht nichts. Auf `A` zählt er wie bisher, und ein Wechsel kann den Turm vor seinen Füßen ersetzen. Das ist gewollt und gleichwertig zu jedem anderen Ultra-Wechsel.

- **Test:** Integration (`Env`): Ultra tauscht einen Turm voraus, solange der Läufer auf `A` steht; steht er auf halber Höhe der Leiter (`y > A.topY`), bleibt der Turm.

## Risks / Trade-offs

- [Ob der Client auf einer Fake-Leiter klettert, deren Rückwand der Server nur als Fake sendet] → Der Client prüft die Stütze nicht, er klettert am `climbable`-Block an der Fußzelle. Smoke-Test mit echtem Client ist Pflicht (siehe Tasks); Rückfall bei Misserfolg: auf die Ranke wechseln. Echte Blöcke in der Welt scheiden aus (Anforderung „Die echte Lobby-Welt DARF sich durch einen Lauf NICHT verändern“).
- [Das Brett der Leiter blockiert Landen auf `A` an der Pfeilerseite] → Das Brett ist 3/16 dick am Pfeiler; der Läufer steht in der Mitte von `A`. Beim Smoke-Test prüfen, ob `A` an der Pfeilerkante trotzdem betretbar bleibt.
- [Position-Updates beim Klettern sind langsam (0,12 pro Tick); der Spieler steht lange auf `A.topY`] → Dort zählt er nicht, es ist aber auch nicht schlimm: kein Score, nur Zeit.
- [Ultra: Turm wird erzeugt, während der Läufer an der Leiter hängt, die zu `A` gehört] → Nur wenn er auf `A` steht, siehe D9.
- [Viele Displays pro Turm (bis 10) und Pakete pro Läufer] → Auf zwei Türme voraus begrenzt; Smoke-Test mit Last (mehrere Läufer) vor dem Release.
- [Höhenband und Platz: ein Turm braucht mehr freien Raum als ein Sprung, Läufe enden dadurch früher als „kein Platz“] → Türme sind nur Kandidaten; ohne Platz gibt es andere Sprünge.

## Offene Fragen

- Ranke und Leiter in einer Version? Die Ranke hat keine Kollision (der Läufer kann hindurchlaufen) und braucht eine Seite am Pfeiler; sie ist optisch schöner, die Leiter robuster. Vorschlag: beide, mit Freischaltung wie oben; Entscheidung nach dem Smoke-Test.
- Zählt ein Turm einen Sprung oder `H`? Vorschlag: einen (siehe D3).
- Zwirn-/Trauerranken als freistehende Spalte und Gerüst in einer zweiten Stufe? Gerüst hat Besonderheiten (Schleichen lässt absteigen, Zustände `bottom`/`distance`, Oberseite als Boden); Vorschlag: nicht in diese Version.
- Welche Toleranz soll die Landeprüfung beim Verlassen der Leiter haben (heute ± 0,05)? Hängt vom Verhalten des Clients ab (Smoke-Test).
- Soll auch die Aufstiegsphase Türme nutzen können, um aus der Lobby schneller in die Höhe zu kommen?
- Wie viele Displays je Turm sind für alle Zuschauer vertretbar, oder genügt eine Display-Säule?
