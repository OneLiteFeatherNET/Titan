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
- **Sichtbarkeit für andere (seit lokalem Test):** Die echten Fake-Blöcke bekommt nur der Läufer. Andere sehen das Fenster über `BlockDisplay`-Entities (D12).
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
d(n)        = 1 - e^(-n / K)                 K = 80, n = Score (seit D15, vorher 40)
Ziel(n)     = d(n) * C_max + N(0, σ)         σ = 1.0, auf [0, C_max] begrenzt
Kosten(s)   = 2*Typ(s) + 1.5*Lücke(s) + 1*Aufstieg(s)

Typ:      Vollblock 0 | Falltür (unten, zu) 1 | Stufe (unten) 1 | Zaun / Mauer 2 | Glasscheibe / Eisengitter 3 | Pfosten (Endstab, Kette, Blitzableiter) 4
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
- **Schaffbar:** `Oberkante(Ziel) - Oberkante(Start) ≤ 1.0`. Bei Aufstieg ist die Lücke ≤ 3, sonst ≤ 4; diagonale Sprünge sind auf Lücke 2 begrenzt, weil eine diagonale Lücke 4 effektiv etwa 5,7 Blöcke misst, und kosten eine Lückenstufe mehr (Lückenstufe + 1). Oberkanten: Vollblock 1.0, Falltür 0.1875, Stufe 0.5, Zaun/Mauer 1.5, Glasscheibe/Gitter 1.0, Pfosten 1.0.
- **Platz:** Die Zielposition ist Luft. Über der Oberkante sind 2 Blöcke frei (beim Zaun also y+1 bis y+3). Die Flugbahn ist frei: Für jede XZ-Zelle auf der Linie (Bresenham) zwischen Start und Ziel sind die Blöcke von der höheren Oberkante bis +2 frei. Das Ziel liegt in der Instanzgrenze (Dimension min/max Y, Worldborder), mit der eigenen Konstante `MAX_Y_MARGIN = 5` Abstand zur Obergrenze der Dimension. `spawn.maxHeight` liest die Column nicht, weil das die Config einer anderen Column wäre (siehe Risiken). Das Ziel überschneidet keinen Block des eigenen Fensters. Nach der Aufstiegsphase sind unter dem Ziel mindestens `MIN_AIR_BELOW = 6` Blöcke Luft (y−1 bis y−6), damit der Parcours nicht über Wegen und Dächern verläuft, und das Ziel liegt waagrecht mindestens `MIN_SPAWN_DISTANCE = 16` Blöcke vom `LobbySpawn` entfernt (der `SpaceProbe`-Nachbar `SpawnZone` kennt den Spawn; die Prüfung ist rein).
- **Vorliebe für offenen Raum:** Jeder gültige Kandidat bekommt eine Offenheit `o = 0.7 · Säule + 0.3 · Nachbarn`, beide in `[0, 1]`: Säule = Anteil Luft unter dem Ziel bis 16 Blöcke tief, Nachbarn = Anteil Luft in den 8 Nachbarzellen auf Ziel- und Fußhöhe. Die senkrechte Luft zählt damit deutlich mehr. Gerankt wird nach `|Kosten − Ziel| + W_OPEN · (1 − o)` mit `W_OPEN = 1.5`. Die Kostenabweichung bleibt bestimmend, bei ähnlich schweren Kandidaten gewinnt der offenere. Ein Gewichtungs-Rang statt eines zweiten harten Filters, weil ein harter Filter in engen Lobbys viele Läufe früh beenden würde.
- **Sackgassen:** Ein Kandidat zählt nur, wenn `CourseGenerator` von ihm aus mit Tiefe 1 mindestens einen weiteren gültigen Kandidaten findet. Gibt es keinen, endet der Lauf (Spec „Gar kein Platz mehr“).
- **Built-in:** `instance.getBlock(x, y, z, Block.Getter.Condition.TYPE).isAir()` und `WorldBorder` über den Adapter `InstanceSpaceProbe`. Eine eigene Kollisionsprüfung mit Minestom-`BoundingBox` wurde verworfen, weil die Prüfung auf Blockraster für Schaffbarkeit reicht und rein testbar bleibt.
- **Test:** Unit mit `FakeSpaceProbe` (Set belegter Positionen): Wand blockiert Ziel und Flugbahn, Decke blockiert Kopffreiheit, Sackgasse wird verworfen, Zaun nach Vollblock nur bei dy ≤ 0. Ein Property-artiger Test erzeugt 10 000 Sprünge mit festen Seeds und prüft die Grenzen.
- **SOLID:** SRP (`JumpRules` vs. `CourseGenerator`), DIP (`SpaceProbe`).

### D5 Start, Vorab-Prüfung und Aufstiegsphase

- Der Startblock liegt unter den Füßen des Spielers (die Position, auf der er steht). Er wird nicht als Fake-Block gesetzt, ist also ein echter Block der Welt. Ist der Spieler gerade in der Luft, startet kein Lauf (Meldung „kein Platz“).
- Die Aufstiegsphase besteht aus mindestens `ASCENT_JUMPS = 5` Sprüngen und läuft weiter, bis der letzte Block waagrecht mindestens `MIN_SPAWN_DISTANCE = 16` vom Spawn entfernt ist und unter ihm `ASCENT_AIR_BELOW = 8` Blöcke Luft sind, höchstens `MAX_ASCENT_JUMPS = 30`. Gelingt das nicht, scheitert die Vorab-Prüfung. Jeder Aufstiegssprung: Vollblock, Oberkante +1, Lücke 1–2. Die Richtung wird gewichtet nach `dot(Sprungrichtung, normalize(Spieler - LobbySpawn))`. Steht der Spieler genau auf dem Spawn (Vektor ≈ 0), gilt seine Blickrichtung.
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

- **Built-in:** Adventure `TranslationStore` und `GlobalTranslator`. Geprüft: Adventure 5.2.0 enthält `MiniMessageTranslationStore` (Argumente per `Argument.numeric("score", n)` als `<score>`), die Column verwendet ihn direkt. Ist `MiniMessageTranslationStore` in der mitgelieferten Adventure-Version vorhanden, wird er verwendet, sonst ein `TranslationStore.messageFormat` plus MiniMessage-Rendering in `RunMessages`. Der globale Schalter `minestom.automatic-component-translation` wurde verworfen. Er müsste in `runtime` vor `ServerFlag` gesetzt werden, wirkt auf die ganze Lobby und braucht einen eigenen Smoke-Test. Das wäre ein eigener Change, wenn Titan i18n insgesamt einführt.
- **Item-Name:** „Jump & Run“ ist sprachneutral und ohne Beschreibung (seit D13 MiniMessage-gestaltet auf einem Schleimblock), weil ein `LobbyItem` einen `ItemStack` für alle Spieler hat (Spec).
- **Test:** Unit: Jeder Schlüssel aus `RunMessages` steht in jedem Bundle, und jedes Bundle hat dieselben Schlüssel. Unit: `de`-Locale rendert deutsch, `ja`-Locale rendert englisch. **Smoke-Test** mit Shaded-Jar: Eine Action Bar ist nicht leer (Minestom-26.1-Falle, siehe Context).
- **SOLID:** SRP (`RunMessages`).

### D9 Rekorde im Speicher

`RunRecords` ist ein Interface (`OptionalInt best(UUID)`, `boolean submit(UUID, int)`, liefert `true` bei neuem Rekord). Die einzige Implementierung ist `InMemoryRunRecords` mit einer `ConcurrentHashMap<UUID, Integer>` und `merge(..., Math::max)`. Seit D21 löscht ein Disconnect den Rekord des Spielers. Die Menge wächst mit eindeutigen Spielern seit dem Start. Das ist für eine Lobby vernachlässigbar (UUID + Integer).

- **Built-in:** `ConcurrentHashMap`. Ein Caffeine-Cache wurde verworfen, weil es keine Ablaufregeln braucht.
- **Test:** Unit (Spec „Neuer Rekord“ / „Kein neuer Rekord“).
- **SOLID:** DIP/OCP. Der spätere Stats-Dienst ersetzt die Bean, ohne die Column umzubauen.

### D10 Logging, Metriken, Traces

- `LoggerFactory.getLogger(JumprunModule.class)`: Start und Ende eines Laufs gehen auf DEBUG mit `addKeyValue("player", uuid)`, Score und Grund. Pro Move-Event wird nichts geloggt. „Generator fand keinen Platz“ ist DEBUG, weil es ein erwarteter Ausgang ist. INFO gibt es nicht, der Modulstart loggt wie die anderen Columns nichts Eigenes.
- **Keine Metriken und keine Spans:** Titan exportiert noch nichts. Eine Lauf-Metrik ohne Leser wird nicht angelegt. Das kommt mit dem Stats-Dienst oder einem eigenen Observability-Change.

### D11 Material-Paletten

Jede `Surface` hat eine feste Palette von Minestom-`Block`s gleicher Kollisionsform: Vollblock (16 Betonfarben, 16 Wollfarben, Terrakotta), Falltür (Holzfalltüren aller Holzarten, Eisenfalltür, jeweils unten und geschlossen), Stufe (Stein-, Ziegel-, Quarz- und Holzstufen, jeweils unten), Zaun/Mauer (Holzzäune, Netherziegelzaun, Bruchstein-, Ziegel- und Steinziegelmauer), Scheibe (bunte Glasscheiben, Eisengitter), Pfosten (Endstab und Kette senkrecht, Blitzableiter). Pro Block zieht der Lauf-`RandomGenerator` ein Material. Das Material lebt im `CourseBlock` und geht in Pakete und Neusendungen ein. Es ändert weder Oberkante noch Kosten.

- **Built-in:** Minestom-`Block`-Konstanten mit `withProperty` für Hälfte, Ausrichtung und Zustand. Eine eigene Kollisionsform-Tabelle gibt es nicht, die Oberkante kommt weiter aus `Surface`. Unverbundene Zäune, Mauern und Scheiben sind beim Client nur ein Mittelpfosten. Das ist gewollt, die Kosten berücksichtigen es.
- **Test:** Unit. Jedes Palettenmaterial hat dieselbe Oberkante wie seine `Surface` (geprüft gegen Minestoms Kollisionsform `registry().collisionShape()`, wo verfügbar). Mit festem Seed haben 10 Vollblöcke nacheinander mehr als ein Material.
- **SOLID:** OCP. Eine neue Palette oder Form ist ein neuer Enum-Eintrag.

### D12 Darstellung für andere Spieler

Für jeden gezeigten Fake-Block spawnt der Lauf eine `Entity(EntityType.BLOCK_DISPLAY)` mit `BlockDisplayMeta#setBlockState(material)` an der Blockposition in der Lobby-Instanz. Ihre Sichtbarkeit wird per `updateViewableRule(viewer -> viewer != runner)` auf alle außer dem Läufer beschränkt. Display-Entities haben keine Kollision, andere können also nicht mitspringen. Über dem Kopf des Läufers hängt eine `Entity(EntityType.TEXT_DISPLAY)` als Passagier (Billboard `CENTER`), ebenfalls für den Läufer unsichtbar, mit dem sprachneutralen Text „Jump & Run · <Score>“. Ein Display-Entity rendert einen Text für alle Betrachter, deshalb ist die Anzeige bewusst ohne Übersetzung. Beim Vorrücken werden Displays des entfernten Blocks entfernt und für den neuen gespawnt, beim Laufende und beim Shutdown werden alle Displays des Laufs entfernt.

- **Built-in:** Minestom-Display-Entities. Verworfen wurden: Fake-Blöcke auch an andere schicken (Kollision beim Client, andere könnten mitspringen), der Name-Tag des Spielers (gehört der Butterfly-Extension, Konflikt mit Team-Präfixen), Partikel (flackern und sagen nichts über den Score).
- **Test:** Integration (Cyano). Beim Start sieht B 2 Block-Displays mit dem Material der Laufblöcke, A keins. Vorrücken entfernt und spawnt je eins. Das Ende entfernt alle, und die Instanz enthält danach keine Entities des Laufs. Der Text-Display zeigt den Score nach einem Sprung und ist für A unsichtbar.
- **SOLID:** SRP. Eine eigene Klasse `Spectators` kümmert sich um die Displays, `FakeBlocks` bleibt nur für den Läufer.
- **Threading:** Displays werden aus denselben Events wie die Fake-Blöcke unter dem Lauf-Lock angelegt und entfernt. Es gibt keinen Tick-Task.

### D13 Item, Kopfanzeige mit MiniMessage, Ton pro Punkt (Nachtrag nach drittem lokalen Test)

- **Item:** Das Hotbar-Item ist ein Schleimblock (`Material.SLIME_BLOCK`), weil er im Spiel für Springen steht. Der Name „Jump & Run“ ist in MiniMessage gestaltet und sprachneutral.
- **Kopfanzeige:** Der Text-Display-Inhalt wird aus einer MiniMessage-Vorlage in Code gerendert (`<sprite:blocks:block/slime_block> <gradient:#7CFC00:#00C853><b>Jump & Run</b></gradient> <gray>·</gray> <white><score></white>`, Score über `Placeholder`/`Argument`, keine String-Verkettung). Das Item-Symbol kommt über ein Objekt-Text-Component mit Atlas-Sprite (seit 1.21.9 im Spiel, in Adventure als MiniMessage-Tag `<sprite>`). **Ergebnis:** Der `<sprite>`-Tag (`SpriteTag`) ist in der mitgelieferten MiniMessage vorhanden, ein zusätzliches `ITEM_DISPLAY` entfällt. Fehlte der Tag, zeigte ein zusätzliches `ITEM_DISPLAY` (Schleimblock, klein) links neben dem Text dasselbe Symbol, mit denselben Sichtbarkeitsregeln wie die Kopfanzeige. Ein neues Paket an die Betrachter geht nur raus, wenn sich der Score ändert.
- **Ton (Shepard-Skala):** Bei jedem Score-Anstieg spielt der Lauf dem Läufer allein (`player.playSound`) zwei gleichzeitige `BLOCK_NOTE_BLOCK_PLING` (Quelle `PLAYER`) im Oktavabstand. Minecraft spielt Tonhöhen von 0.5 bis 2.0, das sind zwei Oktaven, also 24 Halbtöne. Für Schritt `t = score mod 12` liegen die Teiltöne bei Halbton `h_k = t + 12k` (k = 0, 1), mit Tonhöhe `0.5 · 2^(h_k / 12)` und Lautstärke `sin²(π · h_k / 24)`. Jeder Teilton steigt pro Punkt um einen Halbton. Der obere blendet aus, bevor er bei 24 aus dem Umfang fällt, und der untere setzt bei 0 lautlos ein. Das Ohr hört dadurch ein endloses Steigen. Landet der Spieler direkt auf +2, gibt es einen Ton mit dem neuen Score. Aufstiegssprünge bleiben stumm.
- **Built-in:** MiniMessage aus `core` (Adventure 5.2), Adventure `Sound` und Minestoms `SoundEvent`. Eine eigene Notenberechnung gibt es nicht, nur die Standardformel für Halbtöne.
- **Test:** Unit: Jeder Teilton steigt von Score s auf s+1 um genau einen Halbton (Faktor 2^(1/12)), außer beim Umlauf. Die Lautstärke eines Teiltons ist an den Rändern 0 (h = 0 und h = 24). Die Summe der Lautstärken ist für alle 12 Schritte konstant (sin² + cos² = 1). Die Vorlage rendert für Score 7 einen Text, der „Jump & Run“ und „7“ enthält. Integration: Nach einem Punkt bekommt der Läufer ein Sound-Paket, ein Zuschauer keins. Ein Aufstiegssprung erzeugt keins. Ein unveränderter Score sendet kein Metadaten-Paket.
- **SOLID:** SRP. `RunSounds` übernimmt den Ton, `ScoreLabel` rendert nur die Vorlage.

### D14 Anklicken lässt Blöcke nicht verschwinden (Bugfix nach drittem lokalen Test)

Ursache (zwei Stück, beide mit echten Client-Paketen reproduziert): Lobby-Spieler sind im Creative-Modus, dort bricht `PlayerActionListener` den Block bei `STARTED_DIGGING` sofort ab (`breakTicks == 0`), ohne `PlayerStartDiggingEvent`, und `InstanceContainer.breakBlock` schickt für die dem Server bekannte Luft sofort ein `BlockChangePacket` mit Luft; unser Event-basiertes Neusenden lief nie. Außerdem schickt der Client nach dem Rechtsklick mit dem Item auf einen Block zusätzlich `ClientUseItemPacket`, das über das `PlayerUseItemEvent` des Hotbar-Dispatchers den Lauf per `toggle` abbrach und damit alle Blöcke zurücksetzte.
Fix: Das Neusenden hängt jetzt an `PlayerPacketEvent` (`ClientPlayerActionPacket` Start/Abbruch/Ende und `ClientPlayerBlockPlacementPacket` auf einen Laufblock) und sendet im nächsten Tick, hinter Minestoms Antwort. Ein Rechtsklick auf einen Laufblock sperrt die Item-Benutzung für zwei Ticks (`JumprunModule.use`), alles innerhalb von `features/jumprun`; ein Klick des Items in die Luft schaltet den Lauf weiterhin um.
- **Test:** Integration mit echten Client-Paketen. Links- und Rechtsklick auf einen Laufblock, mit und ohne Item, lassen als letztes Block-Paket an den Spieler den Laufblock zurück, und der Lauf läuft weiter.

### D15 Zielgerichtet, länger leicht, Signal- und Scheiter-Ton (Nachtrag nach viertem lokalen Test)

- **Freischaltung der Formen:** `Surface` bekommt eine Mindest-Score-Schwelle: Vollblock 0, Stufe und Falltür 10, Zaun/Mauer und Scheibe/Gitter 25, Pfosten 40. Der Generator betrachtet nur freigeschaltete Formen. Dazu steigt `K` von 40 auf 80, sodass die halbe Maximalschwierigkeit erst bei etwa Score 55 erreicht ist. `C_max` und das Ziel beziehen sich auf die bei diesem Score freigeschalteten Formen. Damit kippt das Ziel nicht in breite Lücken, nur weil schmale Formen fehlen.
- **Hauptrichtung:** Der Kurs führt eine Hauptrichtung `H` als Einheitsvektor. Sie startet als Aufstiegsrichtung und wird nach jedem Block geglättet: `H ← normalize(0.8·H + 0.2·Schritt)`. Kandidaten mit `cos(H, Schritt) < 0` sind ungültig. Ins Ranking geht `W_DIR · (1 − cos)/2` mit `W_DIR = 2.0` ein, gleichrangig mit Kosten und Offenheit. Die Hauptrichtung ist reiner Zustand im `Course` und wird mit dem Seed reproduzierbar.
- **Vorausschau gegen Sackgassen:** Mit der Hauptrichtung reicht die Vorausschau der Tiefe 1 aus D4 nicht mehr, weil der Kurs nicht umkehren kann. Der Generator prüft deshalb mit einer Tiefensuche bis Tiefe 4 (höchstens 200 untersuchte Stellen, dahinter nur Vollblöcke), ob es weitergeht. In engen, umschlossenen Bereichen endet ein Lauf trotzdem gelegentlich nach mehreren hundert Sprüngen in einer Ecke. Das ist nach Spec erlaubt („Gar kein Platz mehr“), eine randbewusste Lenkung wäre ein möglicher Folge-Change.
- **Abstand zu früheren Blöcken:** Ziel und jede XZ-Zelle der Flugbahn müssen waagrecht (Chebyshev) mindestens 2 Blöcke von jedem sichtbaren Block außer dem Absprungblock entfernt sein, in der Höhenspanne des Sprungs ±2. Das ergänzt die bestehende Überlappungsprüfung (`OccupiedProbe`) als reine Funktion.
- **Signalton im Aufstieg:** `RunSounds.signal` spielt dem Läufer `BLOCK_NOTE_BLOCK_HAT` (Quelle `PLAYER`, Lautstärke 0.6, Tonhöhe 1.0) bei jeder geschafften Aufstiegslandung. Er ist gleichbleibend und klar anders als der Shepard-Punkte-Ton.
- **Ton beim Scheitern:** `RunSounds.fail` spielt dem Läufer drei absteigende `BLOCK_NOTE_BLOCK_BASS` (Tonhöhen 1.0, 0.84, 0.67, je 3 Ticks versetzt über `player.scheduler()`), aber nur bei den Endgründen Absturz und Elytra. `EndReason` bekommt dafür eine Eigenschaft `failed()`.
- **Built-in:** Adventure `Sound`, Minestom-Scheduler für die Staffelung. Ein eigener Sequenzer wurde verworfen, drei geplante Aufgaben genügen.
- **Test:** Unit:
  - Unter Score 10 nur Vollblöcke (Statistik mit festem Seed).
  - Freischalt-Schwellen je Form.
  - Kein Kandidat mit `cos < 0`.
  - Kein Kandidat im 2er-Abstand eines früheren Blocks.
  - Bei Gleichstand gewinnt der Kandidat in Hauptrichtung.

  Die bestehenden Statistik-Tests werden an die neuen Schwellen angepasst und begründet. Integration:
  - Aufstiegslandung → ein Hat-Sound nur an den Läufer.
  - Absturz → drei Bass-Sounds über 6 Ticks (`env.tick()`).
  - Abbruch über das Item → keiner.
- **SOLID:** OCP (Schwelle als Eigenschaft von `Surface`), SRP (`RunSounds`).

### D16 Sichere Landungserkennung und Abstand zu Portalen (Bugfix nach fünftem lokalen Test)

- **Fehlerbild:** Spieler wurden zurückgesetzt, obwohl sie gelandet waren. Ursache: Die Landung wurde nur bei einem `PlayerMoveEvent` mit `isOnGround` erkannt. Minestom 26.2 meldet `isOnGround` aus dem neuen Paketzustand, ruft aber für ein reines Boden-Status-Paket (`ClientPlayerPositionStatusPacket`) und für ein Paket mit unveränderter Position kein Move-Event auf (`PlayerPositionListener`, dort nur `refreshOnGround` bzw. früher Rücksprung). Meldet der Client die Landung erst so, bleibt sie unerkannt, und auf einem absteigenden Kurs liegt der Spieler nach zwei verpassten Landungen mehr als 3 unter dem zuletzt erkannten Block, was ein falsches Absturz-Ende auslöst. Fix: Boden-Status- und Rotationspakete mit Bodenkontakt werden über `PlayerPacketEvent` an der aktuellen Position als Landung geprüft, die Landungssuche läuft über alle sichtbaren Blöcke voraus, und die Absturzschwelle liegt 3 unter dem tiefsten Block von aktuellem und sichtbaren Folgeblöcken. Das Absturz-Ende schreibt `y`, Schwelle und aktuellen Index ins Debug-Log.
- **Portale:** Der Kurs meidet die Portale aus `LobbyPortals` (`core`) samt 3 Blöcken Rand, für Ziel und Flugbahn. Lokal hat ein Lauf mehrfach das ElytraRace-Portal ausgelöst. In Produktion hätte das den Läufer weggeschickt.

### D17 Schlangenlinie um den Spawn (Nachtrag nach fünftem lokalen Test)

Die Hauptrichtung `H` aus D15 wird nach der Aufstiegsphase nicht mehr nur geglättet, sondern zu einer Wunschrichtung `W` hingezogen:
- **Umlauf:** Die Grundrichtung ist die Tangente eines Kreises um den Spawn. Die Drehrichtung (im oder gegen den Uhrzeigersinn) wird pro Lauf mit dem Seed gewählt.
- **Ring:** Eine radiale Korrektur hält den Abstand im Ring 20–60 Blöcke. Unter 24 Blöcken zieht sie nach außen, über 56 nach innen, linear stärker zu den Rändern.
- **Pendeln:** Darauf kommt ein Schlangen-Pendel von ±50° mit einer Periode von 14 Blöcken (Phase pro Lauf aus dem Seed).
- **Folgen:** `H ← normalize(0.75·H + 0.25·W)`. Die Regel „kein Kandidat gegen `H`“ und der Richtungs-Bonus aus D15 bleiben. Da `H` sich dreht, kommt der Kurs zwangsläufig auch wieder näher an den Spawn, nie aber unter 16 Blöcke (D4).
- **Built-in:** reine Vektorrechnung mit `Math`. Alles ist deterministisch per Seed.
- **Test:** Unit mit offener Welt und festem Seed über 60 Punkte:
  - Die Seite (Vorzeichen der Querabweichung zu `H`) wechselt mehrfach.
  - Der Abstand zum Spawn sinkt mindestens einmal um ≥ 8 Blöcke gegenüber einem früheren Maximum.
  - Er bleibt im Mittel im Ring.

  Die bestehenden Invarianten (kein Schritt gegen `H`, Abstand zu früheren Blöcken, Mindestabstand 16) bleiben grün.
- **SOLID:** SRP. Die Wunschrichtung berechnet ein eigener reiner Typ (`Steering`), den `Phase`/`CourseGenerator` nur nutzen.

### D18 Levelaufstieg bei neuem Rekord

`Run` merkt sich beim Start den bisherigen Rekord aus `RunRecords.best`. Steigt der Score beim Vorrücken zum ersten Mal darüber, spielt `RunSounds.record` dem Läufer allein `ENTITY_PLAYER_LEVELUP` (Quelle `PLAYER`, Lautstärke 1.0, Tonhöhe 1.0), zusätzlich zum Shepard-Ton dieses Punkts. Ein Flag im `Run` verhindert die Wiederholung. Ohne bisherigen Rekord erklingt das Geräusch beim Laufende zusammen mit der Rekord-Meldung (Score > 0).
- **Test:** Integration. Mit Rekord 12 gibt es bei Score 13 genau ein Levelup-Paket an den Läufer und danach keins mehr, ein Zuschauer bekommt keins. Ohne Rekord kommt eins beim Laufende mit Score > 0, mit Score 0 keins.

### D19 Paletten mit Gewichten in der Konfiguration

Die Paletten aus D11 wandern nach `features/jumprun/src/main/resources/titan/defaults/jumprun.yaml`:
```yaml
jumprun:
  palettes:
    full:
      white_concrete: 1
      black_wool: 1
      # ...
```
Schlüssel ist der Block-Name ohne Namensraum, Wert das Gewicht. Die Form setzt weiter die nötigen Zustände (unten, geschlossen, senkrecht), der Betreiber wählt nur Material und Gewicht.
- **Lesen:** über die `Config`-Fassade wie die anderen Columns. Beim Start prüft `JumprunSettings` jeden Eintrag: Block bekannt (`Block.fromKey`), Oberkante der Kollisionsform passt zur Form (wie der Paletten-Test aus D11), Gewicht > 0, Liste nicht leer. Sonst bricht der Start ab, mit Schlüssel `jumprun.palettes.<form>.<block>` und Grund. Zur Laufzeit gelten die Regeln von `lobby-module-config`: Gelesen wird beim Start eines Laufs. Ein ungültiger Live-Wert führt zu WARN, und es bleiben die zuletzt gültigen Paletten.
- **Ziehen:** gewichtet mit dem Lauf-`RandomGenerator` (kumulative Gewichte, binäre Suche). Pro Seed bleibt das deterministisch.
- **Built-in:** avaje-config (`Config`) wie bei `sit`, `spawn` usw. Ein eigener Lader wurde verworfen.
- **Test:** Unit:
  - Die Standard-YAML enthält alle heutigen Materialien.
  - Ein falscher Block, eine falsche Form, ein Gewicht 0 und eine leere Liste brechen jeweils mit Schlüssel ab.
  - Die gewichtete Ziehung 3:1 ergibt bei festem Seed über 4000 Ziehungen 70–80 %.

  Integration: Ein Override aus `application.yaml` (Test-`Configuration`) wirkt auf die gesendeten Materialien.
- **Abweichung von KISS-Regel:** Seltenes gehört eigentlich in Code. Der Nutzer will die Auswahl aber ausdrücklich ohne Release ändern können.

### D20 Fall- und Aufstiegs-Animation

Die Zuschauer-Displays aus D12 übernehmen die Animation für alle:
- **Einblenden:**
  - Der `BLOCK_DISPLAY` des neuen Blocks spawnt zunächst **für alle sichtbar, auch für den Läufer**, mit Translation `(0, +6, 0)`.
  - Im nächsten Tick setzt der Lauf Translation `(0, 0, 0)` mit Interpolationsdauer 8 Ticks (Start-Delta 0). Der Client fällt den Block dadurch flüssig herab.
  - Nach 8 Ticks bekommt der Läufer den echten Fake-Block (Kollision), und der Display wird über die Sichtbarkeitsregel wieder für den Läufer ausgeblendet. Andere sehen weiter den Display.
- **Ausblenden:**
  - Der Läufer bekommt sofort den echten Block zurück (keine Kollision mehr).
  - Der Display wird wieder für alle sichtbar, steigt mit Interpolation 8 Ticks auf `(0, +6, 0)`, also exakt die Einfall-Bewegung rückwärts, ohne Größenänderung (seit dem sechsten lokalen Test; vorher schrumpfte er zusätzlich).
  - Nach 8 Ticks wird er entfernt.
- **Start:** Die ersten zwei Blöcke fallen ebenfalls ein.
- **Laufende und Shutdown:** Beim Laufende spielen die verbleibenden Blöcke die Aufstiegs-Animation. Beim Shutdown wird ohne Animation sofort entfernt.
- **Built-in:** Minestoms `AbstractDisplayMeta` (Translation, Skala, `transformationInterpolationDuration`, Start-Delta) und `scheduler().buildTask(...).delay(TaskSchedule.tick(8))`. Die Schritte laufen unter dem Lauf-Lock. Wurde ein Block inzwischen schon wieder entfernt oder ist der Lauf vorbei, verfällt der Schritt (Generationszähler pro Block).
- **Test:** Integration (Cyano, `env.tick()`):
  - Ein neuer Block erzeugt sofort einen Display-Spawn mit Translation y = 6, im nächsten Tick ein Metadaten-Paket mit y = 0 und Dauer 8.
  - Der echte Block kommt erst nach 8 Ticks.
  - Beim Entfernen kommt der echte Block sofort, und der Display steigt ohne Größenänderung auf y = 6 und verschwindet nach 8 Ticks.
  - Endet der Lauf während einer Animation, bleibt nichts zurück.
  - Shutdown entfernt sofort.
- **Risiko:** Springt ein Läufer innerhalb von 8 Ticks zwei Blöcke weiter, ist der neueste Block noch nicht begehbar. Bei einem Fenster von 2 voraus ist das praktisch ausgeschlossen. Ein Test prüft, dass der direkt nächste Block nie in der Animation steckt, wenn der Läufer landet.

### D21 Rekord pro Sitzung, Scheiter-Ton nur ohne Rekord

- `RunRecords` bekommt `forget(UUID)`. `JumprunModule.onDisconnect` ruft es nach dem Laufende auf, der Rekord gilt also nur für die Sitzung des Spielers in der Lobby. Das Verlassen der Instanz ohne Disconnect lässt den Rekord stehen, weil der Spieler die Lobby dabei nicht verlässt.
- `RunSounds.fail` spielt nur, wenn der Endgrund `failed()` ist **und** der Lauf den bisherigen Rekord nicht übertroffen hat (`Run.passedPreviousBest()` bzw. erster Rekord mit Score > 0).
- **Test:** Integration:
  - Ein Absturz mit neuem Rekord ergibt kein Bass-Paket, ohne neuen Rekord drei.
  - Nach Disconnect und Rejoin gilt der nächste Lauf mit Score > 0 als Rekord, inklusive Rekord-Meldung.
  - Unit: `forget` entfernt den Rekord.

### D22 Umrandung des nächsten Blocks

Für den Läufer allein spawnt der Lauf am nächsten Block einen zweiten `BLOCK_DISPLAY` mit demselben Material, Skala 1.02 und Translation −0.01. Er hat Leuchten (`setHasGlowingEffect(true)`) mit fester Leuchtfarbe (`glowColorOverride`, Grün `#7CFC00` passend zum Titel) und ist nur für den Läufer sichtbar (`HiddenDisplay` mit umgekehrter Regel `viewer == runner`). Er erscheint, sobald der nächste Block gelandet ist (Ende der Fall-Animation, D20), und wechselt bei jeder Landung. Beim Laufende und beim Shutdown verschwindet er sofort.
- **Built-in:** Display-Glow von Minecraft. Partikel-Umrisse wurden verworfen, sie sind flackernd und teuer.
- **Test:** Integration. Nach dem Landen des nächsten Blocks sieht nur der Läufer ein glühendes Display an dessen Position. Nach einer Landung steht die Umrandung beim neuen nächsten Block. Ein Zuschauer bekommt kein Spawn-Paket. Beim Ende bleibt nichts zurück.

### D23 Modi Easy, Medium, Hard

- **`Mode`** ist ein Enum mit Parametern: Freischalt-Schwellen je `Surface` (Easy: FULL 0 und SLAB 10, alle anderen nie; Medium: wie D15; Hard: 0/5/10/20), Steilheit `K` (Easy 160, Medium 80, Hard 40), maximale Lücke (Easy 2, sonst 4) und ein Aufstiegs-Gewicht im Kostenmodell (Easy: Aufstieg kostet das Dreifache, damit er selten gewählt wird). `CourseGenerator`, `Difficulty` und `Surface` lesen diese Werte aus dem `Mode` des Laufs statt aus Konstanten.
- **Wahl:** `JumprunModule.use(player)` unterscheidet `player.isSneaking()`.
  - Mit Schleichtaste und ohne Lauf wird zyklisch gewechselt.
  - Danach gehen eine übersetzte Meldung `titan.jumprun.mode.changed` (Argument: Modusname, MiniMessage-Gradient) und ein `UI_BUTTON_CLICK` an den Spieler.
  - Mit Schleichtaste im Lauf passiert nichts.
  - Ohne Schleichtaste gilt das bisherige Verhalten (Start bzw. Abbruch).
  - Der Modus liegt pro Spieler in einer `ConcurrentHashMap`, Standard ist `MEDIUM`. Er wird beim Disconnect gelöscht (wie die Rekorde, D21).
- **Rekorde:** `RunRecords` wird pro `(UUID, Mode)` geführt (`best(UUID, Mode)`, `submit(UUID, Mode, int)`, `forget(UUID)` löscht alle Modi). `Run` kennt seinen Modus.
- **Anzeige:**
  - Die Kopfanzeige wird `… Jump & Run · <Mode> · <score>`.
  - Die Modusnamen „Easy“, „Medium“ und „Hard“ sind sprachneutral und je Modus in Grün, Gelb und Rot eingefärbt.
  - `end.score` und `end.record` bekommen das Argument `<mode>`, in allen Sprach-Bundles.
- **Built-in:** Minestoms `isSneaking`, Adventure-Übersetzungen.
- **Test:**
  - Unit: `Mode`-Parameter.
  - Easy über 60 Punkte nur FULL/SLAB, Lücke ≤ 2.
  - Hard schaltet Formen bei 5/10/20 frei.
  - Rekorde pro Modus getrennt.
  - Integration:
    - Schleich-Rechtsklick ohne Lauf wechselt, startet nichts und schickt die Meldung.
    - Im Lauf ändert er nichts.
    - Normaler Rechtsklick startet im gewählten Modus.
    - Die Kopfanzeige nennt den Modus.
    - Nach Disconnect gilt wieder Medium.
- **SOLID:** OCP. Ein neuer Modus ist ein Enum-Eintrag.

### D24 Elytra im Lauf ablegen (ersetzt das Laufende durch Gleiten)

Die DEBUG-Logs aus dem lokalen Test zeigen, dass Läufe durch versehentliches Gleiten enden (`reason=ELYTRA`), wie im Risiko oben vermutet. Beim Start setzt der Lauf deshalb `player.getEquipment(CHESTPLATE)` auf leer. Beim Ende gibt `LobbyItems.equip(player)` (aus `core`, per `Provider` wie bei Elytra, um den Zyklus mit `hotbar` zu vermeiden) die Standardausstattung zurück, außer bei Disconnect und Shutdown. Der Listener auf `PlayerStartFlyingWithElytraEvent` und der Endgrund `ELYTRA` entfallen. Der Scheiter-Ton gilt damit nur noch für den Absturz.
- **Built-in:** `LobbyItems.equip` als bestehender Andockpunkt. Ein eigenes Merken und Zurücklegen des Items wurde verworfen (DRY).
- **Test:** Integration. Nach dem Start ist der Brustplatz leer. Nach dem Abbruch, dem Absturz und dem Erschöpfen ist die Standardausstattung wieder da (Elytra und Item in Slot 0). Disconnect ruft `equip` nicht auf.

## Risks / Trade-offs

- **Elytra durch Leertaste in der Luft (gelöst durch D24):** Im Spiel startet ein erneuter Druck auf die Leertaste in der Luft das Gleiten. Spieler, die beim Springen hektisch drücken, beenden ihren Lauf versehentlich. → Bewusst so entschieden (Elytra-Gleiten = Ende). Bei der Abnahme wird geprüft, wie oft das passiert. Falls nötig, gibt es einen Folge-Change, der statt Laufende das Gleiten nur unterbindet.
- **Minestom schickt bei Interaktionen echte Blöcke zurück** (Korrektur nach abgebrochenem Platzieren/Abbauen). Der Fake-Block wäre dann weg, und der Spieler fällt. → Fenster neu senden bei Interact/Digging (D1). Ein Integrationstest deckt Rechtsklick auf einen Laufblock ab.
- **Andere Spieler sehen einen schwebenden Spieler.** → Gewollt (Proposal), keine Maßnahme.
- **Der Spawn-Höhen-Teleport greift über 310.** → Die Column hält Abstand über die eigene Grenze aus der Dimension (D4). Ein Lauf erreicht 310 praktisch nie, weil der Aufstieg nur +1 pro Sprung macht und der Score-Teil meist eben oder abwärts verläuft. Wenn doch, teleportiert die Spawn-Column, und der Fall-Check beendet den Lauf. Das Ergebnis ist sauber, nur ohne Rücksetz zum Startpunkt.
- **Kollision bleibt dem Client überlassen:** Minestom prüft die Bewegung nicht gegen Blöcke, der Server glaubt `onGround`. → Für einen Zeitvertreib ohne Belohnung akzeptiert (Non-Goal Anti-Cheat). Mit dem Stats-Dienst ist das neu zu bewerten.
- **Diagonale Sprünge über Ecken:** Die Bresenham-Linie kann je nach Rundung eine Ecke streifen, die der Spieler tatsächlich trifft. → Die Flugbahnprüfung nimmt bei diagonalen Schritten beide Nachbarzellen mit (konservativ).
- **`PlayerChunkLoadEvent` kommt vor dem Chunk-Paket statt danach:** Dann würde der Chunk die Fake-Blöcke überschreiben. → Ein Integrationstest prüft die Reihenfolge der Pakete. Falls nötig, wird über `scheduleNextTick` neu gesendet.

## Migration Plan

Es ist nichts zu migrieren. Die Column landet über den Verzeichnis-Scan in beiden Varianten, Rückbau heißt Column entfernen. Hotbar-Slot 0 war frei, also gibt es keinen Konflikt.
