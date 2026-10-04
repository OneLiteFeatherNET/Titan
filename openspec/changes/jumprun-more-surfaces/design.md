# Design

## Context

Motivation steht in `proposal.md` (Why). Ausgangslage im Code (`origin/main`):

- `Surface` ist ein Enum mit `top` (Oberkante über der Blockposition), `typeCost` und `states` (Blockzustände, mit denen ein Material bis `top` kollidiert). `shape(Block)` setzt diese Zustände, `jumpRoomTop()` = `highestBlockReached(top + JUMP_HEIGHT)`.
- `Placement.topY()` = `pos.y + surface.top`. Davon leiten sich ab: `Jump.rise()` (Ziel-Oberkante minus Start-Oberkante, höchstens `MAX_RISE` = 1,0), `Course.fallThreshold()` (tiefste Oberkante ab dem aktuellen Block minus `FALL_DISTANCE` = 3) und die Sprungfreiheit (`jumpRoomTopY()`).
- Landen: `Course.isStandingOn` prüft `|feet.y − topY| ≤ 0,05` und je Achse `center ∈ [cell − 0,3, cell + 1 + 0,3]`. Die 0,3 sind die halbe Hitbox-Breite. Der Kommentar nennt die Nachsicht bei Scheiben und Pfosten ausdrücklich gewollt.
- Palette: `JumprunSettings.palettes` liest je `Surface` den Abschnitt `jumprun.palettes.<configKey>`, prüft Blockname, Gewicht und `collisionShape().relativeEnd().y == top` (Toleranz 1e-9). `Palettes` verlangt für jede `Surface`-Konstante eine Palette. Das Material wird erst nach der Positionswahl gezogen (`CourseGenerator.withDrawnMaterial`), damit es die Wahl nicht steuert; `redrawn` (Rainbow) tauscht das Material bei gleicher Form.
- Schwierigkeit: `Jump.cost` = `2·typeCost(Ziel) + 1,5·Lückenstufe (+ Aufstiegsgewicht)`. `Mode.Params.unlocks` ordnet Formen einem Mindestscore zu, `Phase.Scored.surfaces()` = `mode.unlockedAt(score)`; die Aufstiegsphase nutzt nur `FULL`.
- Darstellung: `FakeBlocks` (`BlockChangePacket` mit `block.material()`), `AnimatedBlock` und `Outline` (`BlockDisplayMeta.setBlockState(material)`) kennen nur das Material. Minestom simuliert die Client-Kollision nicht, der Server sieht nur Positionen.
- Die Spec nennt eine halbe Platte „Stufe“. In diesem Change heißt die Treppe durchgehend „Treppe“ (`stairs`), um die Begriffe nicht zu vermischen.

## Goals / Non-Goals

**Goals:**
- Sechs neue Formen, die der Client wirklich trägt, mit korrekter Landeerkennung auf der echten Standfläche.
- Kein Sprung wird durch eine neue Form schwerer als der bisher schwerste, und jeder erzeugte Sprung bleibt ohne Hilfsmittel schaffbar.
- Eine kleine, einheitliche Erweiterung des Landemodells statt Sonderfällen je Form.

**Non-Goals:**
- Änderung der bestehenden Formen oder ihrer Landeprüfung (Scheibe, Pfosten und Zaun bleiben nachsichtig über die ganze Zelle).
- Treppen mit Ecken (`shape=inner_*`/`outer_*`), obere Hälften, Wandköpfe, Mehrfach-Kerzen, Schneehöhen außer einer.
- Neue Nutzertexte, neue Modi, Freischaltung in Easy.
- Ein Befehl oder eine Oberfläche, um Teamköpfe zu pflegen; nur die Konfigurationsliste.
- Klettern: eigener Change `jumprun-climbing`.

## Decisions

### D1 Landefläche als Liste von Stufen

`Surface` bekommt `steps()`: eine Liste von `Step(top, minX, maxX, minZ, maxZ)`, Koordinaten in Zelleneinheiten (0 bis 1). Die sechs heutigen Formen haben genau eine Stufe `(top, 0, 1, 0, 1)`, ihr Verhalten bleibt bitgleich. Neue Formen:

| Form | Stufen (Oberkante, Grundfläche) |
| ---- | -------------------------------- |
| `stairs` | untere Hälfte `(0,5, halbe Zelle)`, obere Hälfte `(1,0, andere halbe Zelle)`; welche Hälfte wo liegt, gibt `facing` vor |
| `carpet` | `(0,0625, ganze Zelle)` |
| `snow` | `(0,25, ganze Zelle)` |
| `head` | `(0,5, 0,25 bis 0,75 je Achse)` |
| `flower_pot` | `(0,375, 0,3125 bis 0,6875 je Achse)` |
| `candle` | `(0,375, 0,4375 bis 0,5625 je Achse)` |

Für orientierte Formen (nur `stairs`) liefert `Surface.steps(Block material)` die Stufen passend zum `facing` im Materialzustand. Nur `CourseBlock` kennt `steps()` (`surface.steps(material)`), denn nur dort steht das Material mit dem `facing`; `Spot` braucht keine Stufen, weil die Geometrie vor der Wahl nur `top()` und `lowTop()` nutzt (D2). `Step.turnedClockwise()` dreht die Treppe nach `facing`; `top()` und `lowTop()` leiten sich aus den Stufen ab.

- **Alternative A: eine Oberkante je Form, die Treppe als Platte mit Zusatzprüfung.** Verworfen: Die Zusatzprüfung wäre ein Sonderfall nur für die Treppe, und schmale Formen blieben ungelöst.
- **Alternative B: die Höhe der Stufe aus `facing` und der Fußposition auflösen (kein Datenmodell).** Verworfen: Das ist dieselbe Information, nur in der Landeprüfung statt in `Surface` versteckt. Die Liste ist testbar ohne Kurs.
- **Built-in geprüft:** Minestom liefert über `Block.collisionShape()` nur die Hüllbox (`relativeStart`/`relativeEnd`), keine Einzelboxen. Die Stufen der Treppe stehen daher als kleine Tabelle in `Surface` und werden im Test gegen die echte Kollisionsform abgeglichen (Abtasten von Punkten mit `Shape.intersectBox`). Minestom 2026.08.28-26.2 legt die Einzelboxen über `ShapeImpl.boundingBoxes()` offen (`Block.collisionShape()` liefert diese Klasse); die Tabelle bleibt trotzdem in `Surface`, weil `ShapeImpl` kein stabiler Vertrag ist, und der Test gleicht sie über diese Boxen ab (Antwort auf die offene Frage unten).
- **Test:** Unit (`SurfaceStepsTest`): Jede der zwölf Kombinationen (Treppe × 4 Ausrichtungen, fünf weitere Formen) liefert die erwarteten Stufen. Die Stufen der Treppe passen für alle vier `facing` zu der Kollisionsform des Blocks `oak_stairs[half=bottom,shape=straight]`. Die sechs alten Formen haben eine Stufe über die ganze Zelle.
- **SOLID:** OCP (neue Form = neue Konstante und Stufen, kein neuer Pfad in `Course`).

### D2 Treppe: konservative Geometrie, Landen auf beiden Stufen

Die Generator-Geometrie (Erreichbarkeit, Sprungfreiheit, Absturz) darf nicht von der zufälligen Ausrichtung abhängen, weil das Material, und damit `facing`, erst nach der Positionswahl gezogen wird. Deshalb gilt der ungünstigste Fall auf beiden Seiten:

- `Surface.top()` bleibt die höchste Oberkante (Treppe: 1,0), neu ist `lowTop()` (Treppe: 0,5, sonst gleich `top()`).
- `Jump.rise()` = Ziel-`topY` (höchste Stufe) minus Start-`lowTopY` (tiefste Stufe). Der Läufer kann so auf der oberen Stufe des Ziels landen, auch wenn die untere auf der abgewandten Seite liegt, und er kann von der tieferen Stufe des Starts abspringen. Für alle alten Formen ist das dasselbe wie heute.
- `jumpRoomTop()`, `Placement.jumpRoomTopY()` und die Belegung (`occupiedBy`) nutzen die höchste Oberkante, weil der Läufer auf der oberen Stufe stehen kann. Das ist für die Treppe gleich einem Vollblock.
- `Course.fallThreshold()` nutzt `lowTopY`. Die Schwelle liegt dadurch höchstens 0,5 tiefer als bei einem Vollblock; das ist unerheblich gegenüber den 3 Blöcken Abstand.
- Landen: `Course.isStandingOn` zählt als gelandet, wenn eine Stufe des Blocks passt (D3). Wer auf der unteren oder der oberen Stufe steht, hat den Block erreicht.
- Die Ausrichtung zieht der Generator wie das Material nach der Positionswahl: gleichverteilt unter `north`, `south`, `east`, `west`, mit `half=bottom`, `shape=straight`, `waterlogged=false` aus `Surface.shape`. `redrawn` (Rainbow) behält die Ausrichtung des alten Blocks, sie gehört wie die Position zur Form.

Folge: Ein Sprung auf oder von einer Treppe zählt als Aufstieg (`rise > 0`), sobald das Ziel höher liegt als der Start, auch wenn der Läufer auf der Stufe landet, die auf gleicher Höhe liegt. Das schränkt die Lücke um 1 ein (`MAX_GAP_ASCENT` = 3) und fügt das Aufstiegsgewicht zu den Kosten hinzu. Das ist konservativ und gewollt: lieber ein leichterer Sprung als ein unmöglicher.

- **Alternative: Ausrichtung zur Sprungrichtung wählen (tiefe Stufe zeigt zum Läufer).** Verworfen: Sie widerspricht „zufällig ausgerichtet“ und würde die Treppe für den Läufer immer gleich aussehen lassen.
- **Alternative: Ausrichtung mit der Position ziehen (`Spot` bekommt `facing`, Kandidaten ×4).** Verworfen: Vervierfacht die Kandidaten der Treppe für einen Gewinn von höchstens 0,5 Blöcken Rise, den die Landefläche ohnehin hergibt.
- **Test:** Unit (`JumpTest`, `JumpRulesTest`, `CourseTest`):
  - Rise von Vollblock auf Treppe auf gleicher Blockhöhe = 0; Treppe auf Vollblock auf gleicher Blockhöhe = 0,5 (Aufstieg); Treppe auf Vollblock eine Höhe darüber = 1,5 und damit unerreichbar.
  - Sprungfreiheit der Treppe = Sprungfreiheit eines Vollblocks.
  - Absturz-Schwelle nutzt die tiefe Stufe.
  - Landen auf der unteren, auf der oberen Stufe und auf Höhe dazwischen (keine Landung), je Ausrichtung.
  - Generator (feste Seeds): jede der vier Ausrichtungen kommt vor, `redrawn` behält sie.
- **SOLID:** SRP (die Geometrie bleibt in `Surface`/`Jump`; der Generator zieht nur zufällig).

### D3 Landeprüfung gegen die echte Grundfläche

`CourseBlock.supports(feet)` (vorher `Course.isStandingOn`, damit das Prädikat ohne Kurs testbar ist) lautet: Es gibt eine Stufe `s` in `block.steps()` mit `|feet.y − (pos.y + s.top)| ≤ 0,05` und `feet.x ∈ [pos.x + s.minX − 0,3, pos.x + s.maxX + 0,3]` (ebenso `z`). Die Hitbox-Breite bleibt die einzige Toleranz (`PLAYER_HALF_WIDTH`); sie gilt jetzt gegen die Stufe statt gegen die Zelle.

- **Entscheidung: die enge Grundfläche gilt, die Zellen-Toleranz entfällt für die neuen Formen.** Bei einer Kerze (0,125 breit) würde „Zelle ± 0,3“ einen Läufer als gelandet zählen, der bis zu 0,3 neben der Zelle in der Luft in Höhe der Kerze vorbeifällt (der Abwärtsschritt pro Tick ist bis zu 3,9 Blöcke, ein Paket im 0,1 breiten Höhenband ist möglich). Mit „Stufe ± 0,3“ ist die Prüfung genau die Physik: Die Hitbox überlappt die Kollisionsbox, sonst steht der Spieler nicht darauf.
- **Alte Formen bleiben nachsichtig:** Scheibe, Pfosten und Zaun haben als Stufe die ganze Zelle. Das ist das heutige Verhalten, es bleibt ausdrücklich unberührt. Vereinheitlichen (echte Grundfläche auch für Pfosten) wäre eine Verhaltensänderung und gehört nicht in diesen Change.
- **Konsequenz für die Schwierigkeit:** Die wirksame Landebreite (Mittelpunkt des Läufers) ist bei Kopf 0,85, Topf 0,975 und Kerze 0,725 Blöcke, gegenüber 1,6 bei einem Vollblock. Die Kosten (D5) bilden das ab.
- **Offen:** Ob zusätzlich `onGround` verlangt werden soll (nur der Client weiß es), siehe Offene Fragen.
- **Test:** Unit (`CourseTest`, ohne Minestom-Server; `StepLandingTest`):
  - Kerze (Stufe 0,4375 bis 0,5625): Mitte bei 0,5 und bei 0,14 landet, bei 0,13 und bei 0,87 nicht (Grenzen 0,1375 und 0,8625); Höhe 0,375 ± 0,05 nötig.
  - Teppich: Höhe 0,0625 ± 0,05, ganze Zelle.
  - Alte Form (Pfosten): Mitte 0,3 neben der Zelle landet weiterhin (Regression).
  - Eigenschaftstest über 200 feste Seeds: Für jede Form liegt jede Mitte, die zur Stufe passt, im Bereich, den die Kollisionsform des Blocks trägt.
- **SOLID:** SRP (ein Prädikat „steht auf Stufe“, eine Tabelle der Stufen).

### D4 Teppich und Schnee: Oberkante und Schichtzahl

- **Teppich:** Oberkante 0,0625 auf voller Zelle. Palette: bunte Teppiche und Moosteppich. Der Wert liegt 0,0625 über dem Boden der Zelle. Das Höhenband 0,05 der Landeprüfung ist kleiner als der Abstand zur nächsten Oberkante (Falltür 0,1875: 0,125), eine Verwechslung gibt es nicht.
- **Schnee:** Fester Zustand `layers=3`. Die Kollisionshöhe in Vanilla ist `(layers − 1) · 0,125`, also 0,25. Eine Schicht (`layers=1`) hat keine Kollision (der Läufer fiele durch) und scheidet aus; zwei (0,125) wären nur 0,0625 über dem Teppich. Drei liegt mit 0,25 deutlich zwischen Teppich (0,0625) und Falltür (0,1875)/Platte (0,5) und ist als eigene Höhe unterscheidbar. Die Oberkante der Anzeige ist 0,375 und damit höher als die der Kollision. Das ist genau, wie der Client Schnee trägt, und fällt beim Spielen als „etwas einsinken“ kaum auf. Eine feste Schichtzahl hält die Palettenprüfung einfach (alle Materialien gleich). Mehrere Schichtzahlen wären eine eigene Form je Zahl.
- **Built-in geprüft:** Die tatsächliche Kollision liest `JumprunSettings` ohnehin aus `Block.collisionShape()` und vergleicht sie mit `Surface.top()`. Weicht Minestom 26.1 von der Vanilla-Formel ab, schlägt der Test der mitgelieferten Palette an, und die Konstante wird angepasst, nicht die Prüfung.
- **Test:** Unit (`JumprunSettingsTest`): `snow[layers=3]` und alle Teppiche der Standardpalette bestehen die Prüfung; `snow[layers=1]` als Eintrag wird nicht zugelassen, weil `Surface.shape` die Schichtzahl auf 3 zwingt.
- **SOLID:** OCP (zwei Konstanten, kein neuer Code im Kurs).

### D5 Kleine Formen: Kopf, Blumentopf, Kerze

- **Kopf:** Bodenköpfe. Oberkante 0,5, Grundfläche 0,25 bis 0,75. `rotation` (0 bis 15) zieht der Generator zufällig, ein reiner Sichteffekt ohne Einfluss auf die Kollision. Welches Profil ein Kopf zeigt, regelt D8: Mit gefüllter Profilliste ist jeder Kopf ein `player_head` mit Teamprofil; die Palette `head` (`player_head`, `zombie_head`, `creeper_head`, `skeleton_skull`, `wither_skeleton_skull`) gilt nur als Rückfall bei leerer Liste. Wandköpfe (andere Form) und `piglin_head`/`dragon_head` (andere Grundfläche) sind nicht in der Standardpalette; die Prüfung D6 lehnt sie ab.
- **Blumentopf:** `flower_pot` und die `potted_*`-Blöcke einer Auswahl (Blumen, Setzlinge, Kaktus, Farn, Bambus). Oberkante 0,375, Grundfläche 0,3125 bis 0,6875. Alle gepflanzten Töpfe haben dieselbe Kollision.
- **Kerze:** `candle` und die 16 Farben, Zustand `candles=1`, `lit=false`. Oberkante 0,375, Grundfläche 0,4375 bis 0,5625. Mehrere Kerzen in einer Zelle haben breitere Flächen; eine feste Zahl hält die Prüfung einfach, und eine Kerze ist die schmalste und damit die interessanteste.
- **Kosten (`typeCost`):** `stairs` 1, `carpet` 1, `snow` 1 (breite, gnädige Flächen, wie Platte und Falltür); `head` 2 (wie Zaun), `flower_pot` 2, `candle` 3 (wie die Scheibe, die schmalste Fläche, aber unter dem Pfosten mit 4). `Jump.MAX_COST` und damit der Höchstwert der Schwierigkeit bleiben unverändert, weil der Pfosten mit 4 der höchste Wert bleibt. Dass Kopf und Topf trotz 0,5 und 0,375 hoher Fläche gleich viel kosten, ist bewusst: Die Fläche (0,85 und 0,975 wirksam) ist ähnlich.
- **Freischaltung (`Mode.Params.unlocks`):** Easy bleibt unverändert. Medium: `stairs`, `carpet`, `snow` ab 10, `head`, `flower_pot` ab 25, `candle` ab 40. Hard: ab 5, 10 und 20. Das hält die Regel „bis Score 10 nur Vollblöcke“ (Medium) und lässt jede Form im Zeitraum ihrer Kostenklasse erscheinen. Die Aufstiegsphase bleibt Vollblock-only.
- **Darstellung:** Für `stairs`, `carpet`, `snow`, `flower_pot` und `candle` sehen `FakeBlocks`, `AnimatedBlock` (Falleffekt) und `Outline` (Umrandung) nur das Material und brauchen keine Änderung. Köpfe mit Profil brauchen einen eigenen Weg, siehe D8. Die Umrandung einer Kerze ist klein; die Skalierung 1,02 bleibt.
- **Test:** Unit (`SurfaceTest`, `ModeTest`): Oberkante, `lowTop`, Kosten und Freischaltung je Form und Modus (Tabelle oben). Alle neuen Formen unter Score 10 in Medium und unter 5 in Hard nicht freigeschaltet. Generator (`ModeGenerationTest`, feste Seeds): In Medium bei Score 80 kommt jede neue Form vor, in Easy keine. Eigenschaftstest: Kein erzeugter Sprung überschreitet Lücke und Rise, auch nicht von oder auf die neuen Formen.
- **SOLID:** OCP/DRY (Kosten und Freischaltung bleiben Tabellen an einer Stelle).

### D6 Palettenprüfung: Höhe und Grundfläche

`JumprunSettings.block` vergleicht heute `collisionShape().relativeEnd().y` mit `Surface.top()`. Neu: Bei Formen mit schmaler Grundfläche (`head`, `flower_pot`, `candle`) vergleicht sie zusätzlich die Hüllbox in `x` und `z` (`relativeStart`/`relativeEnd`) mit der Grundfläche der Stufe. Treppe prüft die Höhe 1,0 wie `full`. Der Fehlertext nennt wie bisher den vollen Schlüssel und den Grund, z. B. `jumprun.palettes.head.dragon_head: collides over 0.1875..0.8125 but the head shape needs 0.25..0.75`.

- **Alternative: Grundfläche nicht prüfen.** Verworfen: Ein Material mit anderer Grundfläche würde die Landeprüfung (D3) stillschweigend verfälschen. Die Prüfung beim Start ist die vorhandene, richtige Stelle.
- **Test:** Unit (`JumprunSettingsTest`): Die mitgelieferten Standardwerte aller zwölf Formen bestehen. Abgelehnt, mit Schlüssel und Grund: `oak_slab` in `stairs` (Höhe), `dragon_head` in `head` (Grundfläche, nur wenn Minestom dessen Hüllbox abweichend meldet; sonst ein künstlicher Fall über eine Prüf-Hilfsmethode), `carpet` in `snow` (Höhe), ein unbekannter Block, Gewicht −1, alle Gewichte 0. Ein Betreiber-Override, der nur `stairs.oak_stairs: 0` setzt, lässt die übrigen Treppen. `PalettesTest`: Für jede Konstante muss eine Palette da sein.
- **SOLID:** SRP (Prüfung bleibt in `JumprunSettings`, die Geometrie in `Surface`).

### D7 Konfiguration und Standardwerte

`titan/defaults/jumprun.yaml` bekommt sechs Abschnitte (Gewicht 1 je Material, wie bisher):

```yaml
stairs:   # lower half, straight; the generator draws the facing
  oak_stairs: 1
  spruce_stairs: 1
  birch_stairs: 1
  stone_stairs: 1
  cobblestone_stairs: 1
  stone_brick_stairs: 1
  brick_stairs: 1
  sandstone_stairs: 1
  quartz_stairs: 1
  nether_brick_stairs: 1
  # plus the remaining wood stairs, as for slab
carpet:
  white_carpet: 1        # and the other fifteen colours
  moss_carpet: 1
snow:
  snow: 1                # layers is forced to 3
head:
  player_head: 1
  zombie_head: 1
  creeper_head: 1
  skeleton_skull: 1
  wither_skeleton_skull: 1
flower_pot:
  flower_pot: 1
  potted_poppy: 1
  potted_dandelion: 1
  potted_oak_sapling: 1
  potted_fern: 1
  potted_cactus: 1
  potted_bamboo: 1
candle:
  candle: 1              # and the other fifteen colours; candles is forced to 1, lit to false
```

`JumprunSettings.palettes` iteriert über `Surface.values()` und braucht dafür nur die neuen Konstanten. Die Kommentare der Datei nennen die neuen Zustände (Treppe: untere Hälfte, gerade; Schnee: drei Schichten; Kerze: eine, nicht angezündet).

- **Test:** Unit (`JumprunConfigTest`): Die geladene Standardkonfiguration enthält zwölf Palettenabschnitte, jeder mit mindestens einem Material. Ein Betreiber-Override mit einem einzelnen Material je Abschnitt ergibt genau diese Palette.

### D8 Köpfe des Teams

**Befund (Minestom 2026.08.28-26.2, Quellen im Gradle-Cache):**
- `BlockChangePacket(Point, Block)` trägt nur die Blockzustands-ID, keine NBT. Ein Profil an einem Kopf-Block braucht ein zweites Paket: `BlockEntityDataPacket(pos, BlockEntityTypes.SKULL, nbt)` mit dem Eintrag `profile`. Das Profil kodiert `ResolvableProfile.CODEC` (`id`, `name`, `properties`). Der Client legt die Blockentity beim Empfang an.
- `BlockDisplayMeta` hat als einziges Feld `setBlockState(Block)` (`DISPLAYED_BLOCK_STATE`, nur die Zustands-ID). Ein Blockdisplay kann kein Profil tragen; ein `player_head` darin hätte höchstens die Standardhaut, und ob der Client Blockentity-Köpfe in Blockdisplays zeichnet, lässt sich aus den Quellen nicht ablesen. Für das Profil taugt es nicht.
- `ItemDisplayMeta` zeigt ein Item. `DataComponents.PROFILE` (`ResolvableProfile`) trägt das Profil am `player_head`-Item. Das Itemdisplay ist daher der Weg für Falleffekt und Umrandung.
- `ResolvableProfile` kennt `GameProfile` und `Partial(name, uuid, properties)`. Ein reines UUID-`Partial` ist erlaubt, ob der Client es von selbst über Mojang auflöst, zeigt der Servercode nicht (das geschieht im Client); es wäre je Zuschauer eine eigene, asynchrone Abfrage, bei der zuerst die Standardhaut erscheint. Texturen ohne Signatur (`textures`-Property) zeigt der Client an, wie `/give`-Köpfe mit Base64-Textur. `PlayerSkin.fromUuid(String)` (`@Blocking`, Mojang) liefert Textur und Signatur.

**Entscheidung:** Die Lobby löst die Skins selbst auf und schickt die Texturen mit; sie verlässt sich nicht darauf, dass der Client eine UUID auflöst.

**Umsetzung:** Wo gelöst wird, beantwortet `TeamHeads`: `JumprunConfig.palettes()` ruft sie beim Lesen der Liste auf, sie reicht jeden noch unbekannten UUID als Auftrag an einen eingespritzten `Executor` (produktiv virtuelle Threads) und liefert nur Skins, die schon im Speicher liegen. Der Tick-Thread wartet also nie auf Mojang; ein Skin, der noch fehlt, erscheint ab dem Lauf, der nach der Antwort startet. Der Start (`readAtStartup`) stößt die Abfragen früh an. Die Palette trägt die Teamköpfe (`Palettes.withHeads`), `CourseBlock` trägt das gewählte `Optional<HeadSkin>`. Das Profil ist ein `ResolvableProfile.Partial` mit UUID und `textures`-Property, die Blockentity-Daten sind `{profile: <ResolvableProfile.CODEC als NBT>}` an `BlockEntityType.SKULL`.
- **Config:** `jumprun.heads.profiles`: Liste von Spieler-UUIDs (Standard: leer). Wie die Paletten live gelesen, über `LiveSetting`: Ein ungültiger Eintrag (keine UUID) lässt die ganze Änderung unwirksam, protokolliert mit Schlüssel und Grund, die letzte gültige Liste bleibt. Zu Beginn ohne gültige Liste gilt die leere Liste; ein falscher Eintrag bricht den Start nicht ab.
- **Auflöser:** Das Interface `HeadSkins` (`Optional<PlayerSkin> skinOf(UUID)`) wird im Konstruktor eingespritzt. Produktiv fragt `MojangHeadSkins` über `PlayerSkin.fromUuid` ab (nicht auf dem Tick-Thread) und merkt sich Treffer im Speicher (Cache bleibt über Neuladen bestehen, fehlgeschlagene UUIDs werden beim nächsten Lesen erneut versucht). Eine UUID ohne Skin wird mit WARN (UUID, Grund) übersprungen. Bleibt nichts übrig, ist die Liste leer.
- **Ziehen:** Nach der Positionswahl zieht der Generator mit dem Material das Profil: gleichverteilt aus der Liste, ohne das Profil des zuletzt gezeigten Kopfes im Lauf, damit nicht derselbe Kopf zweimal hintereinander erscheint (wie `Palette.drawOther`); bei nur einem Eintrag darf er wiederholen. Rainbow zieht mit dem Materialwechsel ein anderes Profil (`redrawn`). Die Wahl steht im `CourseBlock` als `HeadSkin` (UUID und Textur), nicht im Blockzustand.
- **Leere Liste:** Der Kopf ist ein Block der Palette `head` ohne Profil.
- **Fake-Block:** `FakeBlocks.show` sendet nach dem `BlockChangePacket` das `BlockEntityDataPacket` mit `profile`; `reset` sendet wie bisher den echten Block, der die Blockentity des Clients ersetzt.
- **Falleffekt und Umrandung:** Für Köpfe mit Profil nutzen `AnimatedBlock` und `Outline` ein Itemdisplay mit `player_head` und `DataComponents.PROFILE` statt eines Blockdisplays; `HiddenDisplay` ist über Entitätstyp und Meta-Klasse allgemein und braucht keine neue Klasse. Versatz (Zellmitte, `BlockLook`) und Skalierung (Umrandung 1,04) sind Startwerte, die der Smoke-Test abstimmt. Die Anzeige ist nur der Kopf-Look, die Landung ändert sich nicht (Grundfläche und Oberkante bleiben D1).
- **Alternative: UUID-only-`Partial` und Auflösung durch den Client.** Einfacher (kein Auflöser, kein Netz), aber ungeprüft, je Zuschauer eine Mojang-Abfrage mit Rate-Limit und anfangs die Standardhaut. Bleibt Rückfall-Vereinfachung, falls der Smoke-Test zeigt, dass es zuverlässig geht.
- **Alternative: Block der Palette immer, Profil nur im Fake-Block.** Verworfen: Dann sähe der Falleffekt anders aus als der gelandete Block.
- **Test (F.I.R.S.T.: kein Netz, `HeadSkins` als Attrappe, feste Seeds, Log über `CapturedLog`):** Unit (`HeadProfilesSettingsTest`): gültige UUID-Liste wird gelesen; eine Nicht-UUID macht die Änderung unwirksam, die letzte Liste bleibt, der Eintrag wird mit Schlüssel protokolliert; eine nicht auflösbare UUID wird übersprungen und mit WARN genannt; nur nicht auflösbare Einträge ergeben die leere Liste. Unit (`HeadDrawTest`): Mit fester Seed kommt jedes Profil vor; nie dasselbe zweimal hintereinander bei mindestens zwei Einträgen; ein Eintrag wiederholt sich; leere Liste liefert Palettenkopf ohne Profil; `redrawn` liefert ein anderes Profil. Unit/Integration (`Env`): `FakeBlocks.show` sendet `BlockChangePacket` und danach `BlockEntityDataPacket` mit `profile` (UUID und Textur); bei leerer Liste kein `BlockEntityDataPacket`; Falleffekt und Umrandung sind Itemdisplays mit `PROFILE`, die Köpfe ohne Profil Blockdisplays.
- **SOLID:** DIP (`HeadSkins` eingespritzt), SRP (Auflösen, Ziehen und Anzeigen getrennt).

## Risks / Trade-offs

- [Konservative Treppen-Geometrie macht manche Sprünge unmöglich, die der Läufer schaffen würde] → Lässt Kandidaten aus, nie einen unmöglichen zu. Wenn die Treppe zu selten erscheint, ist die Ausrichtung mit der Position zu ziehen (verworfene Alternative) der nächste Schritt.
- [Köpfe mit Profil: Falleffekt und Umrandung als Itemdisplay sitzen nicht exakt wie ein Blockdisplay (Größe, Versatz)] → Skalierung und Versatz im Smoke-Test abstimmen; im Notfall fällt der Kopf ohne Falleffekt ein (nur Fake-Block).
- [Mojang nicht erreichbar oder UUID unbekannt] → Der Eintrag wird mit WARN übersprungen, bei leerer Restliste gelten die einfachen Köpfe; der Lauf startet immer.
- [Die wirksame Landebreite der Kerze (0,725) ist deutlich enger als die von Pfosten (nachsichtig 1,6)] → Kosten 3 und Freischaltung ab 40/20 bilden das ab; mit dem Smoke-Test zu prüfen, ob die Kerze zu hart ist.
- [`PlayerMoveEvent`-Positionen sind pro Tick, ein schneller Läufer kann die dünne Höhe der Kerze zwischen zwei Paketen überspringen und doch dort stehen] → Gilt für alle Formen gleich; der Läufer steht danach still auf der Fläche und wird beim nächsten Paket erkannt. Kein neues Risiko.
- [Schnee: Anzeige 0,375, Kollision 0,25] → Der Läufer sinkt scheinbar 0,125 ein. Beim Smoke-Test zu prüfen; sonst `layers=2`.

## Offene Fragen

- Soll `onGround` für die schmalen Formen verlangt werden, damit ein Läufer, der an der Fläche in Höhe der Oberkante vorbeifällt, nicht zählt? Heute prüft die Landeprüfung `onGround` nirgends; es wäre eine Änderung für alle Formen.
- Zeigt Minestom 26.2 über die Kollisionsform Einzelboxen (dann Stufen der Treppe ableiten)?
- Reicht dem Client ein Profil nur mit UUID (`ResolvableProfile.Partial`), um die Haut selbst aufzulösen? Der Entwurf (D8) hängt davon nicht ab, weil die Lobby die Texturen mitschickt; reicht die UUID, entfiele der Auflöser.
- Mit welcher Skalierung und welchem Versatz sitzt das Itemdisplay eines Kopfes deckungsgleich auf der Zelle (Smoke-Test)?
- Auf welchem Thread liest `LiveSetting` die Konfiguration? Die Auflösung der Skins darf nicht auf dem Tick-Thread laufen.
- Soll Easy die Treppe bekommen (sie ist gnädig wie die Platte)?
- Sollen Schneehöhen oder Mehrfach-Kerzen später als eigene Formen folgen?

## Umsetzungsnotizen

Abweichungen vom Entwurf:
- `Jump.rise()` und die Flugbahn-Prüfung (`JumpRules.isFlightPathFree`) nutzen `lowTopY` auch für die Untergrenze der freien Säule, nicht nur `rise()` und den Absturz-Schwellwert. Für die alten Formen ist das dasselbe.
- `Palette.drawOther` vergleicht den Blocktyp (`Block.id()`) statt des Zustands, weil eine gezogene Ausrichtung ein Material nicht zu einem anderen macht.
- `Surface.varied` zieht Ausrichtung (Treppe) und Drehung (Kopf), `Surface.withLookOf` überträgt sie beim Materialwechsel (Rainbow); ein Teamkopf behält Block und Drehung und wechselt nur den Skin.
- Die Grundflächenprüfung der Palette gilt für Formen, deren Grundfläche nicht die ganze Zelle ist (`Surface.hasNarrowFootprint`, also `head`, `flower_pot`, `candle`); Zaun, Scheibe und Pfosten bleiben unverglichen.
- Die Liste `jumprun.heads.profiles` wird als kommagetrennte Zeichenkette gelesen (avaje-config führt YAML-Listen so zusammen); die Standarddatei hat `profiles: []`.

Antworten auf die offenen Fragen:
- Einzelboxen: ja, `ShapeImpl.boundingBoxes()`.
- `onGround`: nicht verlangt. Die Höhenprüfung (0,05) und die enge Grundfläche reichen; ein Läufer, der in Höhe der Kerze vorbeifällt, liegt außerhalb der Grundfläche plus 0,3. Eine Änderung für alle Formen bleibt außerhalb dieses Changes.
- UUID-only-`Partial`: ungeprüft und nicht nötig, weil die Texturen mitgeschickt werden.
- Itemdisplay-Skalierung und -Versatz: Startwerte (Zellmitte `(0,5, 0,5, 0,5)`, Umrandung 1,04), die der Smoke-Test bestätigen muss.
- Thread: `LiveSetting` liest auf dem aufrufenden Thread (Tick), deshalb läuft die Auflösung über den `Executor` von `TeamHeads`.
- `TeamHeads` lässt jeden Skin-Abruf unter einem Timeout (5 s) laufen, damit `pending` immer frei wird, und fragt eine nicht aufgelöste UUID erst nach `RETRY_AFTER` (5 Minuten, eingespritzte `Clock`) erneut. Die Warnung ist neutral formuliert.
- Der Rainbow-Wechsel (`CourseGenerator.redrawn`) meidet das Profil des Kopfes davor und danach im Kurs und, solange ein anderes übrig ist, das eigene; die Liste `jumprun.heads.profiles` wird ohne Duplikate gelesen (UUID normalisiert, Reihenfolge des ersten Vorkommens).
- Itemdisplay eines Kopfes: Das Modell wird um die Entität gezeichnet (Blockraum um -0,5 verschoben), der Kopf füllt nur die untere Hälfte. Darum Translation `(0,5, 0,5, 0,5)`, Skalierung 1 (Umrandung 1,04). Ein Test hält diese Werte fest; der Smoke-Test bestätigt sie.
