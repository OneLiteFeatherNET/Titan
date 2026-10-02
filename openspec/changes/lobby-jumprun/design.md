# Design

## Context

Motivation und Umfang stehen in `proposal.md`, das Verhalten in `specs/lobby-jumprun/spec.md`. Für das Design zählt dieser Ist-Zustand:

- Features sind Columns unter `features/<name>`. Sie hängen nur an `core`, sind `@Singleton`-Beans mit `@PostConstruct`/`@PreDestroy` und melden Listener über `FeatureNode.attach(titan, featureId, priority)` an. Belegte Prioritäten: 100–900. Items liefern sie als `@Bean LobbyItem` aus einer `@Factory` (`ItemSlot.hotbar(n)`). Belegt ist nur Slot 4 (Navigator).
- Die Lobby ist eine einzige Instanz. `core` stellt `LobbySpawn` bereit.
- Titan hat noch **keine** Übersetzungs-Infrastruktur, alle Texte sind MiniMessage in Code. `Component.translatable` rendert unter Minestom 26.1 leer, solange `minestom.automatic-component-translation` nicht vor `ServerFlag` gesetzt ist.
- Titan exportiert keine Metriken. Logging läuft über SLF4J.
- Die Elytra-Column meldet Gleiten über `PlayerStartFlyingWithElytraEvent`, der Spawn-Höhen-Teleport greift außerhalb von `spawn.minHeight`/`spawn.maxHeight` (−64/310).

## Goals / Non-Goals

**Goals:**
- Die ganze Logik für Kurs, Schwierigkeit und Platz ist reine Java-Logik ohne Minestom-Server und mit festem Seed unit-testbar.
- Pro laufendem Spieler gibt es höchstens 5 Fake-Blöcke und keinen Tick-Task. Gearbeitet wird nur in Events des Spielers.

**Non-Goals:**
- Persistente Bestenliste oder Anbindung an einen Stats-Dienst. Das Interface `RunRecords` ist nur der spätere Andockpunkt.
- Andere Spieler beim Springen ausblenden.
- Anti-Cheat (Fly-Hacks, Teleport-Hacks).
- Konfigurierbare Formel- oder Kostenwerte. Sie bleiben Konstanten in Code.

## Decisions

### D1 Client-seitige Blöcke über `BlockChangePacket`

Jeder Block eines Laufs wird nur an den Spieler als `BlockChangePacket` geschickt. Beim Entfernen bekommt er ein Paket mit dem echten Block aus `instance.getBlock(pos)`. Die Instanz wird nie verändert.

- **Geprüfte Alternativen:** eine eigene `InstanceContainer` pro Spieler, verworfen, weil der Weltwechsel `PlayerSpawnEvent` erneut feuert und damit Hotbar, Spawn und Respawn mitreagieren und der Lobby-Blick verloren geht. Echte Blöcke über `setBlock`, verworfen, weil andere Spieler sie sehen und kollidieren würden und nach einem Absturz des Servers Reste blieben.
- **Neu senden:** Minestom feuert `PlayerChunkLoadEvent`, nachdem ein Chunk an den Spieler gegangen ist. Liegen sichtbare Blöcke des Laufs darin, schickt die Column sie erneut (eingebaut, keine eigene Chunk-Verfolgung). Dasselbe gilt nach `PlayerBlockInteractEvent` und `PlayerStartDiggingEvent` an einer Laufposition, weil Minestom dort zur Korrektur den echten Block (Luft) schickt.
- **Test:** Integration (Cyano-`Env`, eingehende Pakete der Testverbindung mitschneiden): Start schickt 3 Block-Pakete an A, keins an B, und `instance.getBlock` bleibt Luft. Ein Chunk-Load sendet die Blöcke neu.
- **SOLID:** SRP. Die Klasse `FakeBlocks` kennt nur Pakete senden und zurücksetzen, die Kurslogik weiß nichts von Paketen.

### D2 Aufbau der Column

```
JumprunModule (@Singleton, Priorität 1000)
  |- FeatureNode-Listener: Move, ChunkLoad, Interact/Digging, ElytraStart, Death, Disconnect
  |- RunRegistry          Map<UUID, Run>, Start/Ende/Lookup
  |- FakeBlocks           Pakete an genau einen Spieler
  |- RunMessages          Übersetzte Action Bar / Chat (D8)
  '- RunRecords           Rekord pro Spieler (D9)
JumprunItems (@Factory) -> @Bean LobbyItem "titan:jumprun", ItemSlot.hotbar(0)
Rein (ohne Minestom-Server testbar):
  Course        Fenster aus 5 Positionen, Vorrücken, Score, Startpunkt
  CourseGenerator  nächster Sprung aus (letzter Block, Score, Phase, Random, SpaceProbe)
  Difficulty    d(n) und Zielkosten
  Surface       sealed/enum der Blocktypen: Block, Oberkante, Kosten
  JumpRules     Schaffbarkeit (Lücke, Oberkante, Flugbahn)
  SpaceProbe    interface: isAir(x,y,z), inBounds(x,y,z) - Instanz-Adapter + Fake im Test
```

- **Built-in:** `LobbyItem`/`ItemSlot` und `FeatureNode` aus `core`, `java.util.random.RandomGenerator`, Records und sealed Types aus Java 25. Es gibt keine eigene Registry außer der `Map<UUID, Run>`, und für Laufzustand pro Spieler bietet Minestom nichts Passenderes (Tags am Player wären möglich, sind aber nicht typsicher für einen komplexen Zustand und schwerer zu leeren).
- **Test:** `ColumnArchitectureTest` mit `ColumnArchitectureRules`, `EventListenerCounter` nach `@PreDestroy` gleich 0.
- **SOLID:** SRP pro Klasse, DIP über `SpaceProbe`, OCP durch Andocken an `LobbyItem` und `FeatureNode` ohne Änderung an `core` oder `features/hotbar`.

### D3 Schwierigkeit: Formel und Kostenmodell

```
d(n)        = 1 - e^(-n / K)                 K = 40, n = Score
Ziel(n)     = d(n) * C_max + N(0, σ)         σ = 1.0, auf [0, C_max] begrenzt
Kosten(s)   = 2*Typ(s) + 1.5*Lücke(s) + 1*Aufstieg(s)

Typ:      Vollblock 0 | Stufe (unten) 1 | Zaun 2 | Glasscheibe / Eisengitter 3
Lücke:    1 -> 0 | 2 -> 1 | 3 -> 2 | 4 -> 3        (Lücke = Luftblöcke zwischen den Blöcken)
Aufstieg: Oberkante Ziel - Oberkante Start > 0 -> 1, sonst 0
C_max     = Kosten des schwersten erlaubten Sprungs
```

Aus allen gültigen Kandidaten (D4) wählt der Generator den mit der kleinsten `|Kosten - Ziel|`. Bei Gleichstand entscheidet der `RandomGenerator`. `d(n)` steigt stetig und bleibt unter 1. Bei n ≈ 28 ist die halbe Maximalschwierigkeit erreicht.

- **Alternativen:** lineare Stufen (z. B. alle 10 Sprünge eine Stufe), verworfen, weil sie Sprünge in der Schwierigkeit erzeugen und eine Obergrenze von Hand brauchen. Gewichtete Zufallswahl pro Dimension, verworfen, weil die Dimensionen dann unabhängig würfeln und Extremkombinationen (Glasscheibe + 4er-Lücke + Aufstieg) schon früh auftreten.
- **Test:** Unit. `d` ist monoton, `d(0) = 0`, `d(n) < 1`. Mit festem Seed liegen über 1000 Sprünge die mittleren Kosten bei n = 80 deutlich über denen bei n = 0, und bei n = 0 sind über 80 % Vollblöcke mit Lücke ≤ 2 (Spec „Leichter Anfang“ / „Später schwerer“).
- **SOLID:** SRP. `Difficulty` ist eine reine Funktion, `Surface` trägt die Kosten (OCP für neue Blocktypen).

### D4 Kandidaten, Schaffbarkeit und Platz

- **Kandidaten** vom letzten Block aus: horizontale Versätze mit Lücke 1–4 in den vier Achsenrichtungen sowie diagonal (Lücke über die Chebyshev-Distanz), Höhe −1/0/+1, jeder `Surface`-Typ.
- **Schaffbar:** `Oberkante(Ziel) - Oberkante(Start) ≤ 1.0`. Bei Aufstieg ist die Lücke ≤ 3, sonst ≤ 4. Oberkanten: Vollblock 1.0, Stufe 0.5, Zaun/Glasscheibe 1.5 bzw. 1.0.
- **Platz:** Die Zielposition ist Luft. Über der Oberkante sind 2 Blöcke frei (beim Zaun also y+1 bis y+3). Die Flugbahn ist frei: Für jede XZ-Zelle auf der Linie (Bresenham) zwischen Start und Ziel sind die Blöcke von der höheren Oberkante bis +2 frei. Das Ziel liegt in der Instanzgrenze (Dimension min/max Y, Worldborder), mit der eigenen Konstante `MAX_Y_MARGIN = 5` Abstand zur Obergrenze der Dimension. `spawn.maxHeight` liest die Column nicht, weil das die Config einer anderen Column wäre (siehe Risiken). Das Ziel überschneidet keinen Block des eigenen Fensters.
- **Sackgassen:** Ein Kandidat zählt nur, wenn `CourseGenerator` von ihm aus mit Tiefe 1 mindestens einen weiteren gültigen Kandidaten findet. Gibt es keinen, endet der Lauf (Spec „Gar kein Platz mehr“).
- **Built-in:** `instance.getBlock(x, y, z, Block.Getter.Condition.TYPE).isAir()` und `WorldBorder` über den Adapter `InstanceSpaceProbe`. Eine eigene Kollisionsprüfung mit Minestom-`BoundingBox` wurde verworfen, weil die Prüfung auf Blockraster für Schaffbarkeit reicht und rein testbar bleibt.
- **Test:** Unit mit `FakeSpaceProbe` (Set belegter Positionen): Wand blockiert Ziel und Flugbahn, Decke blockiert Kopffreiheit, Sackgasse wird verworfen, Zaun nach Vollblock nur bei dy ≤ 0. Ein Property-artiger Test erzeugt 10 000 Sprünge mit festen Seeds und prüft die Grenzen.
- **SOLID:** SRP (`JumpRules` vs. `CourseGenerator`), DIP (`SpaceProbe`).

### D5 Start, Vorab-Prüfung und Aufstiegsphase

- Der Startblock liegt unter den Füßen des Spielers (die Position, auf der er steht). Er wird nicht als Fake-Block gesetzt, ist also ein echter Block der Welt. Ist der Spieler gerade in der Luft, startet kein Lauf (Meldung „kein Platz“).
- Die Aufstiegsphase besteht aus `ASCENT_JUMPS = 5` Sprüngen: Vollblock, Oberkante +1, Lücke 1–2. Die Richtung wird gewichtet nach `dot(Sprungrichtung, normalize(Spieler - LobbySpawn))`. Steht der Spieler genau auf dem Spawn (Vektor ≈ 0), gilt seine Blickrichtung.
- **Vorab-Prüfung:** Der Generator erzeugt die ganze Aufstiegsphase plus einen Sprung Vorausschau, bevor etwas gesendet wird. Scheitert das, startet kein Lauf. Danach werden die Blöcke erst beim Vorrücken gesendet (Fenster).
- **Startpunkt** für den Rücksetz nach einem Absturz: die Position des Spielers beim Start.
- **Test:** Unit (Aufstieg führt bei freiem Raum weg vom Spawn; niedrige Decke → kein Start; 5 Aufstiegssprünge → Score 0). Integration: Item benutzt → Lauf.
- **SOLID:** SRP. Die Aufstiegsphase ist eine eigene Strategie im Generator (`Phase.ASCENT` / `Phase.SCORED` als sealed Type mit `switch`).

### D6 Fortschritt und Ende über Spieler-Events

Es gibt keinen Tick-Task. Alles hängt an Events des Spielers in der Column-`FeatureNode` (Priorität 1000):

| Event | Wirkung |
|---|---|
| `PlayerMoveEvent` (nur Spieler mit Lauf, `isOnGround`) | Steht er auf Block +1 oder +2 → vorrücken (D3/D4 erzeugen neue Blöcke) |
| `PlayerMoveEvent` | `y < Oberkante(letzter Block) - 3` → Ende „Absturz“, Teleport zum Startpunkt |
| `PlayerStartFlyingWithElytraEvent` | Ende „Elytra“, kein Teleport |
| Item-Benutzung (`ItemUseHandler`) | kein Lauf → Start, Lauf → Ende „Abbruch“ |
| `PlayerDeathEvent`, `PlayerDisconnectEvent` | Ende, Zustand entfernen |
| `PlayerChunkLoadEvent`, `PlayerBlockInteractEvent`, `PlayerStartDiggingEvent` | Fenster neu senden (D1) |

- `@PreDestroy`: Erst `node.close()`, dann alle Läufe beenden (Blöcke zurücksetzen, keine Meldungen).
- **Built-in:** Minestom-Events und `FeatureNode`. Ein Scheduler-Task pro Lauf wurde verworfen, weil Move-Events ohnehin jede Positionsänderung liefern und ein Task Aufräumarbeit erzeugt.
- **Test:** Integration (Cyano-`Env`, `env.tick()`, Spieler über Testverbindung bewegen): Landen rückt vor, Fall um 4 beendet und teleportiert, Elytra-Event beendet, Disconnect leert die Registry, `stop()` hinterlässt 0 Listener (`EventListenerCounter`).
- **SOLID:** SRP. Der Listener übersetzt Events in Aufrufe auf `RunRegistry`/`Course`, die Entscheidung selbst ist rein (z. B. `Course.advanceTo(pos)` → Anzahl geschaffter Sprünge).

### D7 Zufall

Jeder Lauf bekommt einen eigenen `RandomGenerator`. Dafür hat das Modul einen paketinternen Konstruktor-Parameter `LongSupplier seeds` (Produktion: `ThreadLocalRandom.current()::nextLong`, Test: fester Wert). Der Generator selbst nimmt nur `RandomGenerator` entgegen.

- **Built-in:** `java.util.random.RandomGenerator` / `RandomGeneratorFactory` (`L64X128MixRandom`, mit Seed reproduzierbar).
- **Test:** Gleicher Seed ergibt denselben Kurs (Unit).
- **SOLID:** DIP.

### D8 Übersetzungen ohne globales Flag

Die Column registriert in `@PostConstruct` einen eigenen Adventure-`TranslationStore` mit MiniMessage-Format bei `GlobalTranslator` und entfernt ihn in `@PreDestroy`. Die Bundles liegen in `src/main/resources/titan/jumprun/messages_en.properties` (Fallback) und `messages_de.properties`, UTF-8, Schlüssel `titan.jumprun.<message>`: `start.no_space`, `score.actionbar`, `end.score`, `end.record`. Gesendet wird ein explizit gerendertes Component: `GlobalTranslator.render(Component.translatable(key, args), player.getLocale())`. Argumente laufen als Adventure-Argumente (Score als Zahl), nicht als String-Verkettung.

- **Built-in:** Adventure `TranslationStore` und `GlobalTranslator`. Ist `MiniMessageTranslationStore` in der mitgelieferten Adventure-Version vorhanden, wird er verwendet, sonst ein `TranslationStore.messageFormat` plus MiniMessage-Rendering in `RunMessages`. Der globale Schalter `minestom.automatic-component-translation` wurde verworfen. Er müsste in `runtime` vor `ServerFlag` gesetzt werden, wirkt auf die ganze Lobby und braucht einen eigenen Smoke-Test. Das wäre ein eigener Change, wenn Titan i18n insgesamt einführt.
- **Item-Name:** „Jump & Run“ ist sprachneutral und ohne Beschreibung, weil ein `LobbyItem` einen `ItemStack` für alle Spieler hat (Spec).
- **Test:** Unit: Jeder Schlüssel aus `RunMessages` steht in jedem Bundle, und jedes Bundle hat dieselben Schlüssel. Unit: `de`-Locale rendert deutsch, `ja`-Locale rendert englisch. **Smoke-Test** mit Shaded-Jar: Eine Action Bar ist nicht leer (Minestom-26.1-Falle, siehe Context).
- **SOLID:** SRP (`RunMessages`).

### D9 Rekorde im Speicher

`RunRecords` ist ein Interface (`OptionalInt best(UUID)`, `boolean submit(UUID, int)`, liefert `true` bei neuem Rekord). Die einzige Implementierung ist `InMemoryRunRecords` mit einer `ConcurrentHashMap<UUID, Integer>` und `merge(..., Math::max)`. Disconnect löscht den Rekord **nicht** (Spec „Wiederkommen ohne Neustart“). Die Menge wächst mit eindeutigen Spielern seit dem Start. Das ist für eine Lobby vernachlässigbar (UUID + Integer).

- **Built-in:** `ConcurrentHashMap`. Ein Caffeine-Cache wurde verworfen, weil es keine Ablaufregeln braucht.
- **Test:** Unit (Spec „Neuer Rekord“ / „Kein neuer Rekord“).
- **SOLID:** DIP/OCP. Der spätere Stats-Dienst ersetzt die Bean, ohne die Column umzubauen.

### D10 Logging, Metriken, Traces

- `LoggerFactory.getLogger(JumprunModule.class)`: Start und Ende eines Laufs gehen auf DEBUG mit `addKeyValue("player", uuid)`, Score und Grund. Pro Move-Event wird nichts geloggt. „Generator fand keinen Platz“ ist DEBUG, weil es ein erwarteter Ausgang ist. INFO gibt es nicht, der Modulstart loggt wie die anderen Columns nichts Eigenes.
- **Keine Metriken und keine Spans:** Titan exportiert noch nichts. Eine Lauf-Metrik ohne Leser wird nicht angelegt. Das kommt mit dem Stats-Dienst oder einem eigenen Observability-Change.

## Risks / Trade-offs

- **Elytra durch Leertaste in der Luft:** Im Spiel startet ein erneuter Druck auf die Leertaste in der Luft das Gleiten. Spieler, die beim Springen hektisch drücken, beenden ihren Lauf versehentlich. → Bewusst so entschieden (Elytra-Gleiten = Ende). Bei der Abnahme wird geprüft, wie oft das passiert. Falls nötig, gibt es einen Folge-Change, der statt Laufende das Gleiten nur unterbindet.
- **Minestom schickt bei Interaktionen echte Blöcke zurück** (Korrektur nach abgebrochenem Platzieren/Abbauen). Der Fake-Block wäre dann weg, und der Spieler fällt. → Fenster neu senden bei Interact/Digging (D1). Ein Integrationstest deckt Rechtsklick auf einen Laufblock ab.
- **Andere Spieler sehen einen schwebenden Spieler.** → Gewollt (Proposal), keine Maßnahme.
- **Der Spawn-Höhen-Teleport greift über 310.** → Die Column hält Abstand über die eigene Grenze aus der Dimension (D4). Ein Lauf erreicht 310 praktisch nie, weil der Aufstieg nur +1 pro Sprung macht und der Score-Teil meist eben oder abwärts verläuft. Wenn doch, teleportiert die Spawn-Column, und der Fall-Check beendet den Lauf. Das Ergebnis ist sauber, nur ohne Rücksetz zum Startpunkt.
- **Kollision bleibt dem Client überlassen:** Minestom prüft die Bewegung nicht gegen Blöcke, der Server glaubt `onGround`. → Für einen Zeitvertreib ohne Belohnung akzeptiert (Non-Goal Anti-Cheat). Mit dem Stats-Dienst ist das neu zu bewerten.
- **Diagonale Sprünge über Ecken:** Die Bresenham-Linie kann je nach Rundung eine Ecke streifen, die der Spieler tatsächlich trifft. → Die Flugbahnprüfung nimmt bei diagonalen Schritten beide Nachbarzellen mit (konservativ).
- **`PlayerChunkLoadEvent` kommt vor dem Chunk-Paket statt danach:** Dann würde der Chunk die Fake-Blöcke überschreiben. → Ein Integrationstest prüft die Reihenfolge der Pakete. Falls nötig, wird über `scheduleNextTick` neu gesendet.

## Migration Plan

Es ist nichts zu migrieren. Die Column landet über den Verzeichnis-Scan in beiden Varianten, Rückbau heißt Column entfernen. Hotbar-Slot 0 war frei, also gibt es keinen Konflikt.
