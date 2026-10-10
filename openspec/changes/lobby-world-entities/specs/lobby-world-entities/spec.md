# Spec Delta

## Purpose

Legt fest, welche in den Lobby-Welten gespeicherten Entities in der Lobby erscheinen, wie sie sich gegenüber Spielern verhalten und dass die Lobby Entity-Daten der Welt nie verändert.

## ADDED Requirements

### Requirement: Deko-Entities der Welt erscheinen
Lädt die Lobby einen Chunk ihrer Welt, MUSS sie die im Verzeichnis `entities/` dieser Welt zu dem Chunk gespeicherten Rüstungsständer, Itemrahmen, leuchtenden Itemrahmen, Gemälde, Block-Displays, Item-Displays und Text-Displays an ihrer gespeicherten Position und Ausrichtung anzeigen. Dabei MÜSSEN erhalten bleiben:
- Rüstungsständer: Pose, Ausrüstung, unsichtbar, klein, ohne Bodenplatte, mit Armen, Marker
- Itemrahmen: Item, Drehung, unsichtbar, fest
- Gemälde: Motiv
- Displays: Transformation, Billboard, Helligkeit, Sichtweite und der Inhalt (Block, Item bzw. Text samt Hintergrund und Ausrichtung)
- bei allen: Name samt Sichtbarkeit, Leuchten und ohne Schwerkraft

Das gilt für die Standardwelt und für Saisonwelten gleichermaßen.

#### Scenario: Rüstungsständer mit Pose
- **WHEN** die Welt im Spawn-Chunk einen kleinen Rüstungsständer ohne Bodenplatte mit erhobenem rechten Arm und einem Lederhelm trägt und die Lobby startet
- **THEN** sieht ein Spieler am Spawn diesen Rüstungsständer klein, ohne Bodenplatte, mit erhobenem rechten Arm und Lederhelm an derselben Stelle

#### Scenario: Itemrahmen
- **WHEN** die Welt einen Itemrahmen mit einem Diamanten in Drehung 3 enthält
- **THEN** zeigt die Lobby den Itemrahmen an derselben Wand mit dem Diamanten in Drehung 3

#### Scenario: Text-Display
- **WHEN** die Welt ein Text-Display mit dem Text „Willkommen“, Billboard `center` und einer Skalierung von 2 enthält
- **THEN** zeigt die Lobby den Text „Willkommen“ doppelt so groß und immer zum Spieler gedreht

#### Scenario: Saisonwelt
- **WHEN** die Lobby mit der Saisonwelt `winter` startet, deren `entities/` Gemälde enthält
- **THEN** zeigt die Lobby diese Gemälde

### Requirement: Kein doppeltes Erscheinen
Eine Entity der Welt DARF zu keinem Zeitpunkt mehr als einmal in der Lobby existieren, auch dann nicht, wenn ihr Chunk entladen und erneut geladen wird. Wird ein Chunk entladen, MÜSSEN die aus ihm geladenen Entities mit ihm verschwinden.

#### Scenario: Chunk wird neu geladen
- **WHEN** ein Chunk mit einem Rüstungsständer entladen und erneut geladen wird
- **THEN** existiert der Rüstungsständer danach genau einmal

### Requirement: Deko-Entities sind unveränderlich
Spieler DÜRFEN Deko-Entities aus der Welt NICHT zerstören, verschieben oder verändern. Dazu gehört: einen Rüstungsständer schlagen oder ihm Ausrüstung nehmen oder geben, ein Item aus einem Itemrahmen nehmen, hineinlegen oder es drehen und ein Gemälde abschlagen. Deko-Entities DÜRFEN sich NICHT durch Schwerkraft oder Stöße bewegen.

#### Scenario: Itemrahmen anklicken
- **WHEN** ein Spieler einen Itemrahmen mit Diamant rechts- und linksklickt
- **THEN** bleibt der Diamant in derselben Drehung im Rahmen, und der Spieler erhält nichts

#### Scenario: Rüstungsständer schlagen
- **WHEN** ein Spieler einen Rüstungsständer schlägt
- **THEN** bleibt der Rüstungsständer unverändert stehen

### Requirement: Die Lobby schreibt keine Entity-Daten
Die Lobby DARF die Dateien unter `entities/` ihrer Welt NICHT verändern, weder beim Entladen eines Chunks noch beim Herunterfahren.

#### Scenario: Herunterfahren
- **WHEN** die Lobby nach einer Stunde Betrieb herunterfährt
- **THEN** sind die Dateien unter `worlds/<name>/entities/` byte-gleich zu denen vor dem Start

### Requirement: Nicht unterstützte Entities brechen nichts
Entities anderer Typen (z. B. Mobs oder Minecarts) und Entities mit unlesbaren Daten MUSS die Lobby überspringen, ohne dass das Laden des Chunks oder der Start scheitert. Für jeden übersprungenen Entity-Typ MUSS die Lobby höchstens einmal je Start eine Warnung für Betreiber loggen, die den Typ und die Welt nennt.

#### Scenario: Villager in der Welt
- **WHEN** die Welt zusätzlich zu Rüstungsständern einen Villager enthält
- **THEN** erscheinen die Rüstungsständer, der Villager erscheint nicht, und das Log enthält genau eine Warnung zum Typ `minecraft:villager`

#### Scenario: Beschädigte Entity-Daten
- **WHEN** die Daten einer Entity im Chunk unlesbar sind
- **THEN** lädt der Chunk mit allen Blöcken und den übrigen Entities, und nur diese Entity fehlt

### Requirement: Welt verhält sich sonst wie bisher
Nach dem Wechsel der Lade-Engine MÜSSEN Blöcke, Block-Entities (Köpfe, Schilder, Banner, Betten, Kerzen, Leuchtfeuer) und Licht der Lobby-Welt für Spieler genauso aussehen wie vorher, und die Lobby MUSS mit jeder heute unterstützten Welt starten.

#### Scenario: Bestehende Welt
- **WHEN** die Lobby mit der heutigen Welt `world` startet
- **THEN** stehen alle Blöcke, Schilder und Köpfe wie zuvor, und das Licht entspricht dem bisherigen
