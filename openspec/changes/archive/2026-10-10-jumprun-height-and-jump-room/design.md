# Design

## Context

Motivation steht in `proposal.md` (Why). Ausgangslage im Code (`origin/main`):

- `Surface.headroomTop()` = `highestBlockReached(top)` = `ceil(top + 1.8) - 1`, also nur die Standhöhe. `JumpRules.hasRoomAtTarget` prüft die Säule bis dorthin, `isFlightPathFree` prüft die Flugbahn bis `highestBlockReached(max(fromTop, toTop))`, also ebenfalls ohne Sprunghöhe. `CourseGenerator` hält die Kopffreiheit sichtbarer Blöcke mit `headroomTopY()` frei.
- Die Höhengrenze nach oben ist `JumpRules.MAX_Y_MARGIN = 5` unter der Obergrenze der Dimension (`InstanceSpaceProbe.inBounds`). Der Kommentar dort sagt selbst, dass jumprun die Grenze der Spawn-Column nicht liest. Nach unten gibt es nur die Untergrenze der Dimension.
- Spawn: `HeightBounds(minHeight, maxHeight)` aus `spawn.minHeight`/`spawn.maxHeight` (Defaults −64/310, per Profil änderbar). `SpawnBoundsListener` teleportiert zum Spawn, sobald `y` das Band verlässt. Die Spawn-Column hat Event-Priorität 200, jumprun 1000. Bei demselben `PlayerMoveEvent` greift also der Teleport vor jumpruns Absturzprüfung.
- Absturz: `Course.hasFallen(y)` ist `y < fallThreshold()` = Oberkante des letzten Blocks − 3.
- Die Spec `lobby-module-config` verlangt, dass ein Modul nur seinen eigenen Konfigurationsabschnitt liest.

## Goals / Non-Goals

**Goals:**
- Jeder Block eines Laufs ist ein gültiger Absprung, und jeder erzeugte Sprung hat bis zum Scheitel freie Bahn.
- Ein Lauf verlässt nie das Höhenband, in dem jumprun selbst und nicht der Spawn-Teleport über das Ende entscheidet.

**Non-Goals:**
- Physik-Simulation der Flugkurve (Parabel pro Zelle). Eine konservative Säule bis zur Scheitelhöhe genügt und lässt höchstens Kandidaten aus.
- Änderungen am Spawn-Teleport selbst oder an den Standardwerten von `spawn.minHeight`/`maxHeight`.
- Rückwirkende Korrektur bereits laufender Kurse beim Ändern der Grenzen. Es gilt der nächste neue Block.

## Decisions

### D1 Höhengrenzen der Lobby als Bean aus `core`

`core` bekommt `LobbyHeightBounds` (Interface: `int minHeight()`, `int maxHeight()`, bei jedem Aufruf aktuell gelesen). Die Spawn-Column stellt es per `@Bean` in ihrer Factory aus ihrem eigenen Abschnitt bereit, über dieselbe Quelle, aus der `HeightBounds` heute liest. jumprun bekommt es als `Provider<LobbyHeightBounds>` und liest es bei jeder Prüfung frisch. Ein `requires` in `@InjectModule` ergäbe einen Ordnungszyklus der Avaje-Module: spawn stellt die Bean bereit, braucht aber die Hotbar, und die Hotbar braucht das Item von jumprun. Der `Provider` löst sich erst zur Laufzeit auf, wenn alle Beans stehen; das gleiche Muster nutzt jumprun schon für `LobbyItems`. Die Höhengrenzen kapselt `HeightBand(LobbyHeightBounds)`.

- **Built-in geprüft:** Dass jumprun `spawn.minHeight` direkt liest, verbietet die Spec `lobby-module-config`. Eine Kopie der Werte in `jumprun.*` wurde verworfen, weil zwei Werte auseinanderlaufen können. Die Avaje-Bean ist der vorgesehene Weg, wie Columns Plattform- oder Nachbarwerte bekommen (wie `LobbySpawn`).
- **Test:** Unit (spawn): Die Bean liefert die konfigurierten Werte und nach einer Konfigurationsänderung die neuen. Wiring: jumprun startet mit der Bean, ohne Spawn-Column mit einem Test-Double.
- **SOLID:** DIP (jumprun hängt am Interface in `core`), SRP (die Spawn-Column besitzt die Grenzen).

### D2 Sprungfreiheit statt Kopffreiheit

`Surface` bekommt `JUMP_HEIGHT = 1.2522` (Vanilla-Sprunghöhe) und `jumpRoomTop()` = `highestBlockReached(top + JUMP_HEIGHT)`. `Placement` bekommt dazu `jumpRoomTopY()`.

- `JumpRules.hasRoomAtTarget` prüft die Säule über dem Ziel bis `jumpRoomTopY()` statt `headroomTopY()`.
- `isFlightPathFree` prüft jede Zelle der Flugbahn von `floor(min(fromTop, toTop))` bis `highestBlockReached(fromTop + JUMP_HEIGHT)`. Der Scheitel liegt über dem Absprung, und ein Aufstieg landet höchstens 1 über ihm, also unter dem Scheitel.
- `CourseGenerator` hält für sichtbare Blöcke die Sprungfreiheit statt der Kopffreiheit frei, damit kein neuer Block in den Absprungraum eines anderen fällt.

`headroomTop()` bleibt nur dort, wo wirklich Standhöhe gemeint ist, oder fällt weg, wenn es keinen Nutzer mehr hat.

- **Built-in geprüft:** Minestom berechnet keine Sprungbahnen serverseitig, die Kollision macht der Client. Eine eigene Konstante ist daher nötig. Ihr Wert ist die bekannte Vanilla-Sprunghöhe ohne Effekte.
- **Test:** Unit (`JumpRulesTest` mit Fake-`SpaceProbe`):
  - Decke 3 Blöcke über einer Vollblock-Oberkante → kein Ziel. 4 Blöcke → Ziel gültig.
  - Überhang, der die Scheitelsäule einer Flugbahn schneidet → ungültig.
  - Zaun (Oberkante 1,5) verlangt entsprechend mehr.
  - Unit (`CourseGeneratorTest`): Kein neuer Block in der Sprungfreiheit eines sichtbaren Blocks.
  - Eigenschaftstest über viele Seeds: Jeder erzeugte Block hat Luft bis `jumpRoomTopY()` (Szenario „Jeder Block ist ein Absprung“).
- **SOLID:** SRP (Geometrie bleibt in `Surface`/`JumpRules`).

### D3 Höhenband aus den Lobby-Grenzen

`JumpRules` (bzw. ein kleiner reiner Typ `HeightBand`, gebaut aus `LobbyHeightBounds`) erlaubt eine Stelle nur, wenn für ihre Oberkante `top` gilt:

```
top - FALL_DISTANCE - MAX_FALL_PER_TICK  >  minHeight      // FALL_DISTANCE = 3 (Course), MAX_FALL_PER_TICK = 5
top + JUMP_HEIGHT + 1                    <=  maxHeight      // Füße im Scheitel bleiben unter der Grenze, 1 Block Puffer
```

Der Abstand nach unten enthält mehr als eine volle Tick-Fallstrecke (Endgeschwindigkeit ≈ 3,92 Blöcke/Tick; 5 statt 4, weil ein Bewegungspaket nach Lag mehr als einen Tick Fall melden kann und 4 nur 0,08 Blöcke Spielraum ließe). Die Spawn-Column verarbeitet dasselbe `PlayerMoveEvent` vor jumprun (Priorität 200 vor 1000). Ein Tick, der sowohl die Absturzschwelle als auch `minHeight` überschreitet, würde sonst zum Spawn-Teleport führen. `FALL_DISTANCE` wird aus `Course` geteilt statt dupliziert. `MAX_Y_MARGIN` entfällt, die Grenzen der Dimension bleiben über `SpaceProbe.inBounds` zusätzlich wirksam.

- **Alternative:** Die Event-Reihenfolge umdrehen (jumprun vor spawn). Verworfen, weil die Reihenfolge eine Plattformentscheidung aller Columns ist (`lobby-modules`) und das Problem an der Obergrenze nicht löst.
- **Test:** Unit (`HeightBandTest`): Grenzwerte bei `minHeight` −64 und 0 sowie `maxHeight` 310 und 100, jeweils knapp drin und knapp draußen. Für Zaun und Stufe gilt die richtige Oberkante. Unit (`JumpRulesTest`): Eine sonst gültige Stelle unter dem Band ist ungültig.
- **SOLID:** SRP (Band-Logik rein und separat testbar), OCP (`JumpRules` bekommt eine weitere Regel, ohne andere zu ändern).

### D4 Start und Aufstieg im Band

Die Vorab-Prüfung beim Start (`Course.startSteered`) und die Aufstiegsstrategie (`Phase.Ascent`) prüfen ihre Blöcke mit derselben Band-Regel und derselben Sprungfreiheit. Passt der Aufstieg nicht ins Band, liefert die Startprüfung „kein Platz“, und es erscheint die vorhandene Meldung `start.no_space`.

- **Test:** Unit (`AscentPhaseTest`): Start 6 Blöcke unter `maxHeight` → kein Start. Start knapp über der Untergrenze → kein Start. Normaler Start unverändert. Integration (Cyano-`Env`, `env.tick()`): Item benutzen nahe der Obergrenze → Meldung „kein Platz“, kein Lauf.
- **SOLID:** DRY (eine Regel für Start, Aufstieg und Score-Phase).

### D5 Abnahme über beide Columns

Ein Integrationstest in `apps/cloudnet` (dort laufen die Cross-Column-Tests mit allen Columns) lädt Spawn- und jumprun-Column zusammen, setzt `spawn.minHeight` so, dass ein Lauf nahe der Grenze startet, lässt den Läufer vom tiefsten erlaubten Block fallen (Position per Testverbindung, `env.tick()`) und prüft: Der Lauf endet mit `FALL`, und der Spieler steht am Startpunkt des Laufs, nicht am Spawn. Das ist das Szenario „Absturz knapp über der Untergrenze“.

### Log

Nichts Neues. „Kein Platz“ ist wie bisher ein erwarteter Ausgang auf DEBUG.

## Risks / Trade-offs

- [Läufe enden in engen Bereichen früher, weil mehr Kandidaten wegfallen] → Das ist gewollt, ein unspringbarer Block ist schlimmer. Der Smoke-Test prüft auf der echten Lobby-Map, dass 50+ Sprünge in allen Modi weiter möglich sind.
- [Konservative Scheitelsäule über der ganzen Flugbahn statt Parabel] → Lässt einzelne eigentlich mögliche Sprünge aus, aber nie einen unmöglichen zu.
- [`JUMP_HEIGHT` ohne Sprungkraft-Effekte] → Die Lobby vergibt keine Sprungkraft. Käme sie hinzu, wäre die Annahme neu zu prüfen.
- [Ein Betreiber setzt `spawn.minHeight` und `maxHeight` sehr eng] → Dann startet kein Lauf mehr und der Spieler sieht „kein Platz“. Das wird in `docs/lobby-modules.md` vermerkt.
