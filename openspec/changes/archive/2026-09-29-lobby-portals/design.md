# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/lobby-portals`.

Ist-Zustand:

- `settings.gradle.kts` erfasst jedes Verzeichnis unter `features/` mit `build.gradle.kts` als Column; `titan.column` (buildSrc) lässt eine Column nur von `core` abhängen (plus Minestom, Aves, Avaje Inject). Die Varianten `cloudnet` und `local` nehmen jede Column automatisch auf; `local` schließt nur `season` aus. Eine neue Column braucht daher keine Änderung an Build-Dateien.
- `core` enthält `LobbySpawn` (lesen, nicht cachen), `FeatureNode` (`attach(parent, id, priority)`, Listener über `on(...)`; die Prioritäten 100 bis 800 sind vergeben, eine doppelte Position bricht den Start ab), `PermissionService`/`PermissionResult` und `net.onelitefeather.titan.api.deliver.Deliver`. `runtime` deklariert in `package-info.java` per `@InjectModule(provides = ...)`, was Columns anfordern dürfen, und stellt in `PlatformBeans` `MapProvider`, `LobbySpawn`, `Deliver`, `Clock` und den `titan`-Knoten bereit.
- `common/map`: `LobbyMap extends BaseMap` (Aves) mit Spawn, Name, Bauern; `MapProvider` liest `worlds/<welt>/map.json` mit einem Gson, in dem `PositionGsonAdapter` für `Pos` und `Vec` registriert ist. Die Saison-Column wählt nur den Weltnamen (`LobbyWorldChoice`); die Karte der Saisonwelt lädt dieselbe Stelle.
- Der Navigator leitet mit `Deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(task).player(player).build())` weiter und prüft ein Recht mit `PermissionService.check(uuid, permission) == ALLOWED` (`NOT_SET` und `DENIED` zählen als nicht erteilt).
- `SpawnBoundsListener` zeigt das Muster für `PlayerMoveEvent`: ein Listener, den das Modul mit `FeatureNode.attach(...).on(PlayerMoveEvent.class, ...)` verdrahtet.
- Vorlage für die Scheibenmathematik: Voyagers `RingPass.crosses(from, to, ring)` (`Voyager/voyager/race/src/main/java/net/elytrarace/voyager/race/collision/RingPass.java`) samt `RingPassTest`. Sie wird kopiert (keine Abhängigkeit auf Voyager), die Quelle steht im Warum-Kommentar.

## Goals / Non-Goals

**Goals:**
- Portale als Kartendaten mit einem stabilen JSON-Format, das der Setup-Server später schreibt.
- Reine, ohne Server testbare Formmathematik und Auslöse-Logik; Minestom nur an der Kante.
- Eine Bewegung kostet im Normalfall zwei Hash-Abfragen.

**Non-Goals:**
- Befehle zum Bearbeiten der Portale (`setup-portal-command`), Ziel „einzelner Server“, Erreichbarkeitsprüfung/Bridge-Warnung, Nachrichten, Effekte, Neuladen zur Laufzeit.

## Decisions

### D1: Vertrag der Kartendaten (JSON in `map.json`)

Die Liste `portals` steht auf oberster Ebene der `map.json`, neben `spawn`. Der Vertrag, den der Setup-Server später schreiben MUSS:

```json
"portals": [
  {"id": "survival", "task": "Survival", "shape": {"type": "box", "min": {"x": 10, "y": 64, "z": 10}, "max": {"x": 14, "y": 68, "z": 11}}},
  {"id": "elytra-ring", "task": "ElytraRace", "permission": null, "shape": {"type": "disc", "center": {"x": 0.5, "y": 72, "z": 40.5}, "radius": 5.5, "normal": {"x": 0, "y": 0, "z": 1}}}
]
```

- `id` (Pflicht, eindeutig je Welt, nicht leer), `task` (Pflicht, nicht leer, Name des CloudNet-Tasks), `permission` (optional; `null` oder fehlend heißt „für alle“), `shape` (Pflicht) mit dem Unterscheidungsfeld `type`.
- `box`: `min`, `max` sind **Blockkoordinaten, beide einschließlich**. Die Box deckt in Weltkoordinaten `[min, max + 1]` je Achse ab (im Beispiel x 10 bis 15, y 64 bis 69, z 10 bis 12). Ganze Zahlen und Kommazahlen sind erlaubt; `min <= max` je Achse.
- `disc`: `center` (Weltkoordinaten), `radius > 0` (Rand einschließlich), `normal` (Richtung der Scheibenebene, beliebig ausgerichtet, Länge > 0). Die Lobby normiert `normal` beim Laden; Handeingabe muss kein Einheitsvektor sein.
- Punkte sind `{x, y, z}`, wie der vorhandene `PositionGsonAdapter` sie für `Vec` liest und schreibt.
- Unbekannte Felder werden ignoriert (Gson-Standard), damit ältere Lobbys neuere Dateien nicht brechen; ein unbekannter `type` ist dagegen ein Fehler (D6).

Built-in geprüft: Gson liegt über Aves im Build und liest die `map.json` bereits; ein eigenes Format oder eine zweite Datei (`portals.json` wie in #222) entfällt, weil die Portale zur Welt gehören und Saisonwelten sie so von selbst mitbringen.
Test: Unit (Gson-Rundreise): das Beispiel oben wird gelesen und wieder geschrieben; ein unbekanntes Feld wird ignoriert; fehlendes `permission` und `null` ergeben dasselbe.

### D2: Typen in `core`, Laden in `common`, Bereitstellung in `runtime`

`core`, Paket `net.onelitefeather.titan.core.portal` (Columns sehen nur `core`):

- `sealed interface PortalShape permits Box, Disc` mit `boolean crossedBy(Point from, Point to)` und `HorizontalBounds horizontalBounds()` (kleinstes achsenparalleles Rechteck in X/Z, für den Index; bei `disc` konservativ Mittelpunkt ± Radius).
- `record Box(Vec min, Vec max)` und `record Disc(Vec center, double radius, Vec normal)`; die Records halten nur Daten und prüfen nichts; die Prüfung liegt gebündelt im `PortalValidator` (D6). `Disc` liefert `normal` normiert (Berechnung beim ersten Gebrauch, nur bei gültiger Länge).
- `record Portal(String id, PortalShape shape, String task, @Nullable String permission)`; 
- `final class PortalValidator` (D6): reine, statische Prüfung, wiederverwendbar durch `runtime` (Start) und den Setup-Editor (Chat-Fehler).
- `interface LobbyPortals { List<Portal> portals(); }`: unveränderliche Liste, leer statt `null`. Eigener Typ statt `List<Portal>`, wie bei `LobbySpawn`: eine nackte Liste wäre eine mehrdeutige Bean.

`common/map`: `LobbyMap` bekommt ein Feld `portals` (`List<Portal>`), einen null-sicheren Zugriff `portals()` (Gson baut ohne Konstruktor, eine fehlende Liste bleibt `null`) und `Builder.portals(List<Portal>)`; `LobbyMap.lobbyMapBuilder(LobbyMap)` und jeder andere Kopierweg übernehmen die Portale mit (heute kopiert er nur Spawn, Name, Bauern); sonst würden `/setup map setspawn|setname|setauthor` des Setup-Servers Portale still löschen. Ein Rundreise-/Kopiertest sichert das ab (Aufgabe 2.2). Ein neuer `PortalGsonAdapter` (`JsonDeserializer`/`JsonSerializer<Portal>`) entscheidet über `shape.type`; `MapProvider` registriert ihn im vorhandenen Gson. `Vec` liest der bereits registrierte `PositionGsonAdapter` über den `JsonDeserializationContext`.

`runtime`: `PlatformBeans.lobbyPortals(MapProvider)` liefert `() -> mapProvider.getActiveLobby().portals()` (wie `LobbySpawn`), und `package-info.java` nimmt `LobbyPortals.class` in `provides` auf.

Abweichung vom ersten Entwurf: Die Prüfung sitzt nicht in `PlatformBeans`, sondern beim Laden der Karte (D6). Ein Fehler dort kennt Welt, Id und Grund, und die Karte entsteht ohnehin beim Start.

Built-in geprüft: Records und `sealed` (Java 25) tragen die Formen, Minestoms `Vec`/`Point` die Koordinaten. Verworfen: Coris `CuboidShape` (#222; zusätzliche Abhängigkeit für zwei Formen) und Portal-Typen in der Column (der Setup-Server soll sie später ohne die Column benutzen können).
SOLID: SRP (Form, Portal, Liste, Laden getrennt), DIP (die Column hängt an `LobbyPortals` aus `core`), OCP (weitere Form = neuer `permits`-Eintrag plus Adapterzweig).
Test: Unit je Record (Validierung, Normierung); Unit `LobbyMap` (Rundreise, `portals()` ohne Liste ist leer, Builder-Kopie); Integration: das Bean liefert die Portale der aktiven Karte, in einer Saisonwelt die der Saisonwelt.

### D3: Formmathematik als Strecken-Test

- `Disc.crossedBy(from, to)`: Voyagers `RingPass.crosses` in derselben Rechnung: `step = to - from`; `denominator = normal · step`; bei `|denominator| < 1e-8` gilt „parallel“ und das Ergebnis ist `false`; `t = normal · (center - from) / denominator` muss in `[0, 1]` liegen; der Schnittpunkt `from + step * t` darf höchstens `radius` vom Mittelpunkt entfernt sein (Rand einschließlich, keine Toleranz). Die Quelle steht im Warum-Kommentar.
- `Box.crossedBy(from, to)`: Slab-Methode auf dem Quader `[min, max + 1]`: je Achse Eintritts- und Austrittsparameter, `tNear = max(...)`, `tFar = min(...)`; getroffen, wenn `tNear <= tFar` und `[tNear, tFar]` das Segment `[0, 1]` schneidet. Liegt `to` im Quader, ist das Ergebnis `true`; das deckt „neuer Standort im Bereich“ ab. Eine Achse ohne Bewegung prüft nur, ob der Ausgangswert im Slab liegt. Auch `from` im Quader zählt als berührt; den Übergang entscheidet D4, nicht die Form.
- Die Bewegung geht vom Fußpunkt aus: alter Standort `player.getPosition()`, neuer `event.getNewPosition()` des `PlayerMoveEvent`.

Built-in geprüft: Minestom bringt Bounding-Box-Kollision für Entitäten, aber weder einen Segment-gegen-Quader- noch einen Segment-gegen-Scheibe-Test. Ein reiner Endpunkttest (wie in #222) ist verworfen: Ein Elytra-Spieler mit mehreren Blöcken pro Tick überspringt sonst dünne Ringe.
SOLID: SRP (Formen sind zustandslose Funktionen), LSP (beide Formen erfüllen denselben Vertrag „Strecke berührt Bereich“).
Test (Unit, ohne Server, zuerst): Voyagers `RingPassTest`-Fälle übernommen (Mitte, Rand, knapp außerhalb, parallel, Ebene nicht erreicht, Ebene hinter dem Segment, rückwärts, geneigt Mitte und außermittig, Nullschritt, Rand mit gemeinsamem X- und Y-Versatz, geneigter Ring mit echtem Z-Schritt); Slab-Fälle (Segment durch die Box, endet in der Box, startet in der Box, geht daneben vorbei, achsenparallel innerhalb und außerhalb des Slabs, Streifschuss an Kante und Ecke, `max + 1` einschließlich, `max + 1.0001` außerhalb); ein **schneller Elytra-Schritt**, der eine 1 Block dünne Scheibe mit 8 Blöcken Schrittweite überspringt (beide Endpunkte weit von der Ebene) und trotzdem `true` ergibt.

### D4: Auslöse-Logik (Übergang und Abklingzeit) als reine Klasse

Eine paketinterne Klasse `PortalTrigger` in der Column hält je Spieler-UUID (in einer `ConcurrentHashMap` der Instanz, kein statischer Zustand) die Menge der zuletzt berührten Portal-Ids und den Zeitpunkt, bis zu dem die Abklingzeit gilt. `Optional<Portal> onMove(UUID, Point from, Point to)`:

1. Kandidaten aus dem Index (D5) holen; sind sie leer und der Zustand leer, sofort zurück.
2. Für jeden Kandidaten `crossedBy(from, to)` berechnen; das Ergebnis ist die neue Menge „berührt“.
3. Ein Portal löst aus, wenn es jetzt berührt ist und in der vorherigen Menge nicht enthalten war (Übergang, wie `PORTAL_INSIDE` in #222). Die neue Menge ersetzt die alte, **auch** bei Abklingzeit oder fehlendem Recht, damit langes Verweilen später nicht auslöst.
4. Liegt `clock.instant()` vor dem Ende der Abklingzeit, löst nichts aus. Sonst prüft der Aufrufer das Recht (D7); erst eine tatsächliche Weiterleitung setzt die Abklingzeit auf `now + 3 s`.
5. Bei `disc` ist „berührt“ nur der Schritt, der die Scheibe durchquert; der nächste Schritt löscht die Berührung, damit ein späterer Durchflug wieder auslöst. Bei `box` bleibt „berührt“, solange die Strecke die Box berührt (Verweilen).
6. Den Zustand eines Spielers entfernt das Modul bei `PlayerDisconnectEvent`.

Die 3 Sekunden sind eine Konstante im Code (`Duration.ofSeconds(3)`), keine Konfiguration (Seltenes in Code statt Config). `java.time.Clock` kommt über den Konstruktor; `runtime` stellt die Bean bereits bereit.

Built-in geprüft: `ConcurrentHashMap` und `java.time` genügen; Minestom hat kein Cooldown-Werkzeug für Spielerzustand. Ein Scheduler-Timer zum Löschen der Abklingzeit ist verworfen (der Zeitstempelvergleich ist einfacher und braucht keinen Thread).
SOLID: SRP (der Trigger kennt weder Minestom noch Recht noch `Deliver`), DIP (Uhr injiziert).
Test (Unit, Fake-`Clock`, frischer Trigger je Test): Betreten löst einmal aus; Verweilen löst nicht erneut aus, auch nach Ablauf der Abklingzeit; Verlassen und Wiederbetreten innerhalb von 3 s löst nicht aus; nach 3 s löst es aus; Scheibe: Durchqueren löst, ein zweiter Durchflug nach der Abklingzeit löst wieder; zwei Spieler beeinflussen sich nicht; Disconnect räumt den Zustand auf.

### D5: Chunk-Spalten-Index

`PortalIndex` (Column, paketintern) wird einmal beim Start aus `LobbyPortals.portals()` gebaut: Schlüssel ist der Chunk-Index der Spalte (`ChunkUtils.getChunkIndex(chunkX, chunkZ)` aus Minestom), Wert sind die Portale, deren `horizontalBounds()` die Spalte berühren. `candidates(from, to)` schlägt die Spalten **beider** Endpunkte nach und liefert die Vereinigung ohne Doppelte. Eine Bewegung in einer Spalte ohne Portal kostet zwei Hash-Abfragen und liefert eine gemeinsame leere Liste.

Der Index wird zur Laufzeit nicht neu gebaut (Non-Goal). Wer die `map.json` ändert, startet die Lobby neu; das entspricht dem Weltwechsel der Saison.

Built-in geprüft: `ChunkUtils` und `HashMap` genügen. Verworfen: linearer Durchlauf aller Portale je Bewegung (Bewegungen sind die häufigsten Events der Lobby) und ein Raumbaum (bei wenigen Portalen je Welt unnötig).
Grenze: Nur die Spalten der beiden Endpunkte zählen. Eine einzelne Bewegung, die mehr als eine Chunkbreite (16 Blöcke) überbrückt und ein Portal ausschließlich in einer Zwischenspalte träfe, wird nicht erkannt. Elytra-Schritte liegen im Bereich weniger Blöcke; Teleports lösen kein `PlayerMoveEvent` aus.
SOLID: SRP (der Index kennt nur Grenzen und Spalten).
Test (Unit): Ein Portal innerhalb einer Spalte wird von beiden Endpunkten gefunden; ein Portal über einer Chunkgrenze steht in beiden Spalten; eine Bewegung über die Grenze findet es über den Endpunkt in der anderen Spalte; eine Welt ohne Portale liefert die leere Liste; ein Portal kommt nicht doppelt, wenn beide Endpunkte in seinen Spalten liegen.

### D6: Ungültige Portale brechen den Start ab; die Prüfung liegt in `core`

`core.portal.PortalValidator` ist eine reine, statische Prüfung ohne Minestom-Server: `static List<PortalProblem> problems(List<Portal> portals)` mit `record PortalProblem(String portalId, String reason)` liefert alle Probleme (Radius > 0, `normal` mit Länge > 0, `min <= max` je Achse mit Achsenname, `id` und `task` nicht leer, `id` eindeutig); `static void requireValid(String world, List<Portal> portals)` wirft daraus eine `IllegalStateException` der Form `World '<verzeichnis>': portal '<id>': <grund>` (bei fehlender Id `portal #<index>`). Der Setup-Editor nutzt später `problems(...)` für Chat-Fehler, die Lobby `requireValid(...)` für den Start. Der Validator steht bewusst nicht in `runtime`/`PlatformBeans`.

Ein unbekannter `shape.type` oder ein fehlendes Pflichtfeld ist ein Lesefehler des Gson-Adapters (`JsonParseException`); `MapProvider.loadMapData` fängt ihn, ergänzt die Weltangabe und wirft ebenfalls die `IllegalStateException`. Danach ruft `MapProvider` `requireValid` für die gelesene Liste. Der Start bricht ab. Kein Überspringen mit Log (Abweichung von #222, gemäß „ungültige Werte verhindern den Start“).

**Erste Aufgabe der Umsetzung (Risiko):** Prüfen, ob `GsonFileHandler.load` (Aves) eine `JsonParseException` schluckt und `Optional.empty()` liefert; `MapProvider` fiele dann still auf eine Karte ohne Spawn zurück. Trifft das zu, liest `MapProvider` die Datei für die Portale selbst mit dem eigenen Gson, statt den Fehler zu verlieren; der Vertrag (Abbruch mit Welt, Id, Grund) bleibt.
Built-in geprüft: Java-Ausnahmen und eine Liste genügen; ein Validierungs-Framework lohnt für sieben Regeln nicht. Verworfen: Prüfung in den Record-Konstruktoren (der Editor bräuchte Ausnahmen statt einer Problemliste) und in `PlatformBeans` (nicht wiederverwendbar).
SOLID: SRP (Daten, Prüfung, Laden getrennt), DIP.
Test (Unit, `PortalValidatorTest`): je Regel ein Test mit genauer Meldung (Welt, Id, Grund); mehrere Probleme werden alle gemeldet; ein gültiges Portal liefert die leere Liste; Integration: `MapProvider` mit einem Weltverzeichnis mit ungültiger `map.json` wirft.

### D7: Column `features/portal`

`PortalModule` (`@Singleton`, Muster `SpawnModule`): Der Konstruktor nimmt `EventNode<Event>` (`@Named("titan")`), `LobbyPortals`, `Deliver`, `PermissionService` und `Clock`. `@PostConstruct`: Index und `PortalTrigger` bauen, `FeatureNode.attach(titan, "portal", 900)` mit `on(PlayerMoveEvent.class, ...)` und `on(PlayerDisconnectEvent.class, ...)`; `@PreDestroy`: `node.close()`. Priorität **900**, da 100 bis 800 vergeben sind; die Reihenfolge ist hier unkritisch, weil kein anderes Feature diese Events abbricht.

Ablauf im Listener: `player.getInstance() == null` → zurück; `trigger.onMove(uuid, player.getPosition(), event.getNewPosition())`; hat ein Portal ausgelöst und ist `permission == null` oder `permissions.check(uuid, permission) == ALLOWED`, ruft das Modul `deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(task).player(player).build())` und meldet dem Trigger die Weiterleitung (Abklingzeit). Kein Chat, kein `Deliver`-Wrapper, keine Erreichbarkeitsprüfung (CloudNets `connectToTask` routet; `bridge-connector-warning` meldet eine fehlende Bridge).

Beide Varianten: `Deliver` ist außerhalb von CloudNet ein No-op, und `local` ohne Permission-Plattform behandelt jedes Recht als `NOT_SET` (Portale mit Recht wirken dort nicht). Ein Grund, die Column aus `local` auszuschließen, besteht nicht: sie braucht keinen Supervisor, und `local` bleibt so dieselbe Lobby wie `cloudnet` (Anforderung „Varianten verhalten sich wie die bisherige Lobby“).

`package-info.java`: `@InjectModule(name = "portalColumn", requires = {EventNode.class, LobbyPortals.class, Deliver.class, PermissionService.class, Clock.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})`. Kein `io.avaje.config` in der Column (`ColumnArchitectureTest`), weil es keine Konfiguration gibt.

Logging: INFO beim Start „Loaded {} portals“ (Anzahl); DEBUG bei einer Weiterleitung (Portal-Id, Task; keine Spielerdaten); nie oberhalb DEBUG pro Bewegung. Keine Metriken, keine Spans (Bewegung ist ein Hot Path, eine Weiterleitung eine Spieleraktion). Nutzertexte: keine.
Built-in geprüft: `FeatureNode`, `Deliver`, `DeliverComponent`, `PermissionService` und `Clock` sind die vorhandenen Bausteine; nichts Eigenes für Ereignisse oder Weiterleitung.
SOLID: SRP (das Modul verdrahtet, der Trigger entscheidet, der Index sucht), DIP, OCP (die Column dockt über `LobbyPortals` und `FeatureNode` an, ohne bestehenden Code zu ändern).
Test: Integration (Cyano-`Env`, Fake-`Clock`, `RecordingDeliver`, `FakePermissionService`, je Test frisch): Spieler läuft in eine Box → genau eine Weiterleitung an den Task; ein großer Schritt durch eine Scheibe → eine; Verweilen → keine zweite; Wiederbetreten in der Abklingzeit → keine, nach +3 s auf der Uhr → eine; Recht fehlt → keine und keine Abklingzeit; Welt ohne Portale → keine; nach `stop()` reagiert der Knoten nicht mehr; `ColumnArchitectureTest` und Verdrahtungstests der Varianten grün.

## Risks / Trade-offs

- [`GsonFileHandler.load` schluckt Parsefehler (D6)] → erste Aufgabe prüft es; bei Bedarf liest `MapProvider` die Portale selbst.
- [Sehr lange Einzelschritte überspringen eine Zwischenspalte (D5)] → dokumentierte Grenze; reale Schrittweiten sind klein.
- [Der Fußpunkt entscheidet, nicht der Körper] → ein Spieler, dessen Kopf in der Box steht, der Fußpunkt aber darunter, löst nicht aus; Portale werden in Fußhöhe gebaut. Akzeptiert.
- [Ein fehlender CloudNet-Task fällt dem Spieler nicht auf] → CloudNet meldet ihn im vorhandenen `Deliver`-Pfad; keine zusätzliche Meldung (Nicht-Ziel).
- [Eine Map-Änderung braucht einen Neustart] → gewollt (Non-Goal); der Setup-Server schreibt die Datei, die Lobby liest sie beim Start.
- [`LobbyMap` in `common` ist geteilter Code] → nur additiv (neues Feld, neuer Builder-Schritt, Null-Sicherheit); Karten ohne `portals` lesen sich unverändert (Test).

## Migration Plan

1. Den Task in CloudNet anlegen, den ein Portal nennt (z. B. `Survival`).
2. Portale in die `map.json` der Welt schreiben (Format D1); für Saisonwelten in deren eigene Datei.
3. `titan-cloudnet.jar` deployen (AOT-Cache neu trainieren). Ohne `portals` in den Dateien ändert sich für niemanden etwas.
4. Zurück: `portals` aus der Datei entfernen oder Revert des Squash-Commits.
