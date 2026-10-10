# Design

## Context

Motivation: siehe `proposal.md`. Das Verhalten steht in `specs/lobby-world-entities/spec.md`.

- `common/.../map/MapProvider` baut die Lobby-Instanz: `LightingChunk` als Chunk-Supplier, Relight bei `InstanceChunkLoadEvent`, eingefrorene Zeit und `new AnvilLoader(path)`. Der `AnvilLoader` liest `entities/` nie. Titan selbst speichert keine Chunks.
- Falco (eigene Engine, Minestom 26.2 wie Titan, aktuell 3.0.0) bekommt mit dem Design `docs/superpowers/specs/2026-10-02-anvil-entity-loading-design.md` (Falco-Branch `docs/entity-loading-design`) ein optionales, nur lesendes Entity-Loading. Das Laden ist ein eigenes `AnvilEntityBridge`, das an eine Instanz gehängt wird. Mitgeliefert werden Übersetzer für Rüstungsständer, (leuchtende) Itemrahmen, Gemälde und Block-, Item- und Text-Displays, dazu `UnhandledEntityHandler` (Standard `SKIP`) für alles andere. Entities werden einen Tick nach dem Chunk-Load gespawnt. Beim Entladen eines Chunks verschwinden sie mit ihm, Duplikate verhindert ein UUID-Abgleich.
- Minestom implementiert keine Logik für Itemrahmen, Gemälde oder Rüstungsständer. Spieler können darüber also nichts verändern, solange niemand es programmiert. Schwerkraft wendet Minestom dagegen auf jede Entity mit Physik an.

## Goals / Non-Goals

**Goals:**
- Der Loader-Wechsel ändert für Spieler nichts an Blöcken, Block-Entities und Licht.
- Deko-Entities erscheinen, bleiben unveränderlich und werden nie geschrieben.

**Non-Goals:**
- Falcos eigene Licht-Engine (`falco-light`) und `falco-instance`. Licht bleibt bei `LightingChunk`. Ein Wechsel wäre ein eigener Change mit eigener Messung.
- Mobs, Minecarts und andere Entity-Typen. Sie laufen über `UnhandledEntityHandler` und werden übersprungen.
- Entities speichern oder im Setup-Server bearbeiten.
- Durchreichen von Passagieren und Display-Interpolation (Falco-Grenze).

## Decisions

### D1 `FalcoAnvilLoader` statt `AnvilLoader`

`MapProvider` setzt `FalcoAnvilLoader` mit demselben Weltpfad. `LightingChunk`, Relight und eingefrorene Zeit bleiben unverändert. Die Dimension ist die Overworld, wie heute.

- **Built-in geprüft:** Minestoms `AnvilLoader` bleibt verworfen, weil er `entities/` nicht liest und keinen Andockpunkt dafür hat. Ein eigener Leser für `entities/*.mca` in Titan wurde ebenfalls verworfen: Er würde Falcos Region-Lesen nachbauen (DRY), und die Nutzerentscheidung ist, das in Falco zu lösen.
- **Test:** Erst ein Charakterisierungstest (Integration, Cyano, `@TempDir`-Kopie einer kleinen Testwelt): Mit dem `AnvilLoader` stehen Block, Schild, Kopf und Lichtwert an festen Positionen. Danach muss derselbe Test mit Falco grün sein (Spec „Bestehende Welt“). Zusätzlich gibt es einen manuellen Abgleich mit den echten Welten (`world`, `winter`, `halloween`) im Smoke-Test.
- **SOLID:** DIP. `MapProvider` hängt am `ChunkLoader`-Interface, nur die Konstruktion ändert sich.

### D2 Entity-Bridge an der Lobby-Instanz

`MapProvider` öffnet direkt nach dem Loader `AnvilEntityStore.openBeside(loader)` und hängt `AnvilEntityBridge.builder(store).translators(EntityTranslators.defaults()).unhandled(warnOncePerType).attachToAndShutdownWith(instance)` an. Weitere Bridge-Optionen bleiben auf Falcos Standard. Der Spawn-Chunk wird wie heute vorgeladen, seine Entities erscheinen einen Tick später. Das ist akzeptiert, denn Spieler können erst nach dem Start beitreten.

- **Built-in:** Falcos Bridge. Ein eigenes Tracking von Chunk-Lebenszyklus und UUIDs gibt es in Titan nicht.
- **Test:** Integration mit `@TempDir`-Testwelt, deren `entities/` je einen Rüstungsständer (klein, ohne Bodenplatte, Pose, Lederhelm), einen Itemrahmen (Diamant, Drehung 3), ein Gemälde und ein Text-Display enthält. Nach Chunk-Load und `env.tick()` existiert jede Entity mit den Daten aus den Spec-Szenarien. Entladen und neu laden ergibt jede Entity genau einmal (Spec „Chunk wird neu geladen“).
- **SOLID:** SRP. `MapProvider` verdrahtet nur, die Übersetzung liegt in Falco.

### D3 Warnung einmal pro Typ

`WarnOncePerType implements UnhandledEntityHandler` hält ein `ConcurrentHashMap.newKeySet()` der schon gemeldeten Typen. Beim ersten Auftreten eines Typs loggt es WARN `Skipping unsupported world entity type {} in world {}` mit `addKeyValue("entity.type", …)` und `addKeyValue("world", …)`. Danach bleibt es still. Beschädigte Entity-Daten meldet Falco selbst über WARN bzw. seine Diagnostik, Titan loggt sie nicht doppelt.

- **Built-in:** Falcos `UnhandledEntityHandler` als Extension-Point. Die Menge ist ein JDK-Set, ein eigener Rate-Limiter wurde als unnötig verworfen.
- **Test:** Unit mit aufgefangenem Logback-Appender: Zwei Villager und ein Minecart ergeben genau 2 WARN-Zeilen mit Typ und Welt. Integration: Ein Villager in der Testwelt erscheint nicht, die Rüstungsständer schon (Spec „Villager in der Welt“).
- **SOLID:** SRP und OCP (Andocken über Falcos Interface).

### D4 Deko bleibt unveränderlich

Für jede von der Bridge geladene Entity (Tag `AnvilEntityBridge.LOADED`) setzt ein Listener auf `EntitySpawnEvent` an der Lobby-Instanz `setNoGravity(true)`. Die Entities fallen dadurch nicht und reagieren nicht auf Stöße. Andere Interaktionen (Schlagen, Rechtsklick auf Rahmen oder Rüstungsständer) ändern in Minestom ohne eigenen Code nichts. Titan fügt deshalb keine Abwehr-Listener hinzu, sondern sichert das mit Tests ab. Kommt später ein Feature, das auf `PlayerEntityInteractEvent` reagiert, schlägt dieser Test an.

- **Built-in:** Minestoms Standardverhalten (keine Rahmen- oder Rüstungsständer-Logik). Präventive Cancel-Listener in `features/protection` wurden verworfen, weil die Events nicht `Cancelable` sind bzw. nichts abzubrechen ist (KISS).
- **Test:** Integration: Ein Spieler sendet Angriffs- und Interaktionspakete an Rüstungsständer und Itemrahmen. Danach sind Ausrüstung, Item, Drehung, Position und Inventar des Spielers unverändert. Nach 100 Ticks steht ein Rüstungsständer ohne gespeichertes `NoGravity` noch an derselben Position.
- **SOLID:** SRP. Der Listener lebt in `MapProvider`, neben dem Bridge-Setup, weil er zur Welt und nicht zu einem Feature gehört.

### D5 Nur lesen

Die Bridge ist nur-lesend, und Titan ruft nirgends `saveChunksToStorage` oder `saveInstance` auf. Das bleibt so und wird getestet.

- **Test:** Integration. Eine `@TempDir`-Testwelt wird gestartet, Chunks werden geladen und entladen, dann wird die Instanz heruntergefahren. Die SHA-256 aller Dateien unter `entities/` sind vorher und nachher gleich (Spec „Herunterfahren“).

### D6 Abhängigkeit

Im Versionskatalog (`settings.gradle.kts`) kommt `version("falco", "3.1.0")` dazu, mit `library("falco-anvil", "net.onelitefeather", "falco-anvil").versionRef("falco")`, eingebunden in `common`. Falco hat Minestom als `compileOnly`, Titans `aonyx-bom` bestimmt die Minestom-Version. Vor dem Merge wird geprüft, dass beide auf derselben Minestom-Version stehen (Abhängigkeitsbaum im PR).

### D7 Logging, Metriken

Für jeden übersprungenen Typ gibt es höchstens ein WARN (D3). Sonst entstehen keine neuen Logs, keine Metriken und keine Spans. Falcos Diagnostik-Zähler werden nicht exportiert, weil Titan noch nichts exportiert.

## Risks / Trade-offs

- **Falco-Release fehlt noch** → Die Umsetzung startet erst mit Falco 3.1.0. Welle 1 (Loader-Wechsel mit Charakterisierung) kann mit Falco 3.0.0 vorab laufen.
- **Andere Ergebnisse beim Laden mit Falco** (Falco verweigert unlesbare Welten seit #45, statt Luft zu liefern) → Eine Welt, die heute leise kaputt lädt, kann künftig den Start abbrechen. Der Smoke-Test lädt alle drei Welten. Bei einem Fehler wird die Welt konvertiert (`docs/world-conversion.md`), nicht der Loader gelockert.
- **`ItemFrameMeta` kennt kein `Fixed`** → Minestom hat ohnehin keine Rahmen-Logik, die Rahmen sind faktisch fest. D4 testet das.
- **Ein-Tick-Verzögerung beim Spawn** → Für eine Lobby ohne Spieler beim Start bedeutungslos.
- **Unbekanntes Gemälde-Motiv** (offene Falco-Frage) → Falco überspringt das Gemälde mit WARN. Titan übernimmt das Verhalten.
- **Vor 1.17 konvertierte Welten** haben kein `entities/`, Falco lädt dort keine Entities → Unsere Welten liegen auf 1.21.11 (siehe `docs/world-conversion.md`), daher nicht betroffen.

## Migration Plan

Betreiber müssen nichts tun: Vorhandene `entities/` werden ab dem Release gelesen. Rückbau heißt: Version zurück und `AnvilLoader` wieder setzen.
