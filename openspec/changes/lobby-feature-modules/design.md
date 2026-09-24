# Design

## Context

Den Anlass beschreibt `proposal.md` (Why). Für den Ansatz sind diese Punkte am heutigen `main` maßgeblich:

- **Handverdrahtung:** `Titan.initListeners()` meldet 19 Listener von Hand an einem gemeinsamen `EventNode.all("titan")` an. Jeder Listener ist in `TitanObservability.guard(...)` verpackt, damit Fehler dem Spieler zugeordnet werden.
- **Monolithische Config:** `AppConfig` ist ein sealed Interface mit `AppConfigImpl`, `AppConfigBuilder` und `InternalAppConfig`. `AppConfig.builder(existing)` kopiert `min/maxHeightBeforeTeleport` nicht mit. Der Setup-Server (`setup/.../AppCommand`) setzt deshalb bei jeder Änderung die Höhengrenzen still zurück.
  - `fireworkBoostSlot` und `updateRateAgones` liest niemand mehr, das Feuerwerk liegt fest in der Nebenhand.
- **Navigator-Leck:** `NavigationHelper` erzeugt über einen Caffeine-Cache pro Spieler einen `PersonalInventoryBuilder` und ruft `register()` auf.
  - `refreshAfterWrite(1 min)` erzeugt neue Builder, ohne die alten per `unregister()` abzumelden.
  - Beim Ablauf aus dem Cache gibt es keinen `removalListener`.
  - Folge: Aves-Inventar-Listener sammeln sich an und halten `Player`-Referenzen fest.
  - Die Togglz-Bindung (`ThreadLocalUserProvider.bind`) dort wirkt nicht, weil kein `TitanFeatures.isActive()` abgefragt wird.
- **Zentrale Items und Tags:** Items und Tags liegen zentral in `common/utils/Items` und `common/utils/Tags`. Die Zuordnung benutzter Items läuft über `ItemStack.isSimilar`.
- **Bibliotheken:** Gson 2.14 ist im Klassenpfad (Record-Unterstützung ab 2.10). Tests laufen mit Cyano/Microtus (`MicrotusExtension`, `Env`).
- **AOT-Cache:** Produktiv läuft Titan mit AOT-Cache (JDK 25). Klassen aus eigenen Classloadern profitieren davon nicht (JEP 483/514).
- **Ideenquelle `feat/lobby-season`:** Der Branch liefert `LobbyModule`, `LobbyModules`, `ModuleContext` und `ModuleTasks`. Übernommen wird die Idee (eigener Node und eigene Tasks pro Modul, Abhängigkeiten über den Konstruktor, kein Service-Bag), nicht der Code.

## Goals / Non-Goals

**Goals:**
- Eine kleine, stabile Plattform-Schicht (`app/module`), die Lebenszyklus und Andockpunkte bereitstellt und sich für neue Features nicht ändert.
- Jedes bestehende Feature steckt vollständig in einem eigenen Paket, verhält sich für Spieler unverändert und hat Tests.
- Die Grenzen sind im Build geprüft, nicht nur per Konvention.

**Non-Goals:**
- Kein DI-Container (Guice, Avaje).
- Kein `ServiceLoader`-Discovery.
- Keine Minestom-Extensions für Features.
- Kein Neuladen der Config zur Laufzeit. Änderungen wirken nach einem Neustart, wie heute.
- Keine eigenen Gradle-Module pro Feature.
- Keine Metriken, kein Session-Muster, keine Portale/Weltzeit/Saison (siehe Proposal).
- Kein Ausbau von Togglz. Die bestehende, wirkungslose Bindung im Navigator entfällt mit dem `NavigationHelper`. `TitanFeatures` und `SingletonFeatureManagerProvider` bleiben unverändert und unbenutzt, bis eine eigene Change über Feature-Toggles entscheidet.

## Decisions

### 1. Paketstruktur: Plattform plus Feature-Pakete im `app`-Modul

```
app/src/main/java/net/onelitefeather/titan/app/
  Titan.java                     Composition Root: baut Abhaengigkeiten, Modulliste
  module/                        Plattform (oeffentliche API fuer Module)
    LobbyModule, ModuleContext, ModuleRegistry, ModuleTasks
    config/   ConfigStore, ConfigSection, ConfigException, LegacyConfigMigration
    item/     LobbyItem, ItemRegistry, ItemSlot
    navigator/NavigatorEntry, NavigatorEntries
    command/  ModuleCommands
  feature/
    protection/ spawn/ sit/ tickle/ elytra/ navigator/ respawn/
      <Name>Module (public), <Name>Config (public record), Rest package-private
  commands/                      Plattform-Befehle (stop, end) bleiben ausserhalb der Features
```

**Warum im `app`-Modul und nicht in `common`:** Die Features laufen nur in der Lobby. `common` bleibt für das, was `app` *und* `setup` wirklich teilen: Map, Block-Handler, Deliver, Observability und Permission-Bridge.

**Alternative:** ein Gradle-Modul pro Feature. Das lohnt sich bei rund 4k LOC nicht (siehe Recherche). Die Sichtbarkeit (package-private) plus ArchUnit liefern die gleiche Grenze billiger.

### 2. `LobbyModule` und `ModuleRegistry`

- `LobbyModule` hat drei Methoden: `id()`, `enable(ModuleContext)` und `default disable()`.
- `ModuleRegistry.of(modules...)` startet in Reihenfolge und fährt rückwärts herunter.
- Für jedes Modul legt die Registry an:
  - einen Kind-Node `titan/<id>` unter dem Titan-Node,
  - eine eigene `ModuleTasks`-Instanz,
  - je einen Anmelde-Tracker für Befehle, Items und Navigator-Einträge.
- Beim Herunterfahren gilt diese Reihenfolge:
  1. Node abhängen
  2. Tasks abbrechen
  3. Befehle, Items und Navigator-Einträge des Moduls entfernen
  4. `disable()` aufrufen

Die Abhängigkeiten eines Moduls (Deliver, Map, Lobby-Spawn) kommen über den **Konstruktor**. Der Kontext trägt nur, was jedes Modul braucht, genau wie auf `feat/lobby-season`.

**Alternative:** ein Service-Bag im Kontext. Verworfen, weil jedes Modul dann scheinbar alles benutzen könnte und Tests unnötig viel aufbauen müssten.

### 3. Listener nur über `context.listen(...)`

`ModuleContext` gibt den rohen `EventNode` **nicht** heraus. Stattdessen gibt es `listen(Class<E>, Consumer<E>)`. Die Methode verpackt den Listener in `TitanObservability.guard(moduleId, listener)`, das um die Modul-ID erweitert wird (SLF4J-MDC `module`, zusätzlich zum bestehenden Spieler-Kontext), und hängt ihn an den Modul-Node.

Damit sind drei Anforderungen strukturell erfüllt:
- Fehlerzuordnung zu Modul und Spieler (Spec `lobby-modules`),
- vollständiges Aufräumen, weil alles am Modul-Node hängt,
- keine Registrierung zur Laufzeit: `listen` ist nur während `enable` erlaubt, ein späterer Aufruf wirft `IllegalStateException`.

**Alternative:** den Node herausgeben und auf Disziplin setzen. Verworfen, weil gerade das Navigator-Leck zeigt, dass Disziplin nicht reicht. Falls ein Modul später Node-Filter braucht, kommt eine gezielte Methode hinzu, etwa `listen(filter, ...)`.

### 4. Config: Abschnitt pro Modul, Defaults per Merge, Validierung im Record

Neues Format von `app.json`:

```json
{
  "configVersion": 2,
  "spawn":     { "minHeight": -64, "maxHeight": 310, "simulationDistance": 2 },
  "sit":       { "offset": {"x":0.5,"y":0.25,"z":0.5}, "allowedBlocks": ["minecraft:spruce_stairs"] },
  "tickle":    { "cooldownMillis": 4000 },
  "elytra":    { "boostMultiplier": 35.0 },
  "navigator": { "title": "<yellow>Navigator", "entries": [ ... ] }
}
```

**Laden:** `context.config(SitConfig.class)` liest den Abschnitt mit der Modul-ID. Die Defaults kommen per Merge:
1. Das Modul liefert eine Default-Instanz (`SitConfig.DEFAULTS`).
2. Der `ConfigStore` serialisiert sie zu einem `JsonObject`.
3. Er legt den Abschnitt aus der Datei tief darüber (Deep-Merge).
4. Erst dann deserialisiert er das Ergebnis in den Record.

Fehlende Felder bekommen so ihren Default und nicht 0 oder `null`, auch bei primitiven Typen. Validiert wird im Compact Constructor des Records. Der wirft `ConfigException(section, field, reason)`, und die Registry bricht den Start damit ab.

**Speichern:** `ConfigStore` hält das gesamte Dokument als `JsonObject`. Ein Speichern ersetzt nur den betroffenen Abschnitt, alle anderen bleiben byte-genau erhalten. Der Kopier-Bug kann so nicht mehr entstehen, weil es keinen Kopier-Builder mehr gibt.

**Alternativen:**
- *Boxed Types mit Null-Prüfung im Record:* fehleranfälliger, weil jeder Record das von Hand richtig machen müsste.
- *Configurate:* bringt versionierte Migration mit, wäre aber eine neue Abhängigkeit für einen einzigen Migrationsschritt.

### 5. Migration des alten flachen Formats

Eine Datei ohne `configVersion`, die mindestens einen Altschlüssel enthält, gilt als Version 1. `LegacyConfigMigration` bildet die Schlüssel fest ab:

| Alt (flach) | Neu |
|---|---|
| `tickleDuration` | `tickle.cooldownMillis` |
| `sitOffset` | `sit.offset` |
| `allowedSitBlocks` | `sit.allowedBlocks` |
| `simulationDistance` | `spawn.simulationDistance` |
| `minHeightBeforeTeleport` / `maxHeightBeforeTeleport` | `spawn.minHeight` / `spawn.maxHeight` |
| `elytraBoostMultiplier` | `elytra.boostMultiplier` |
| `fireworkBoostSlot`, `updateRateAgones` | verworfen, im Log genannt |

Vor dem Überschreiben wird die Altdatei nach `app.json.v1.bak` kopiert. Eine syntaktisch kaputte Datei wird nie überschrieben.

`allowedBlocks` wird als String-Key (`"minecraft:spruce_stairs"`) gespeichert statt im heutigen `{namespace, value}`-Objekt. Die Migration liest beide Formen.

### 6. Setup-Server schreibt abschnittsweise über `ConfigStore`

`setup` hängt nicht von `app` ab, kennt die Feature-Records also nicht. Deshalb gilt:
- `ConfigStore`, `ConfigException` und die Migration liegen in `common/config`.
- Der Setup-Server ändert einzelne Felder typisiert über `ConfigStore.set(section, field, jsonValue)` und speichert.
- Validiert wird beim nächsten Start der Lobby.

**Alternativen:**
- *Feature-Records nach `common` verschieben:* würde die Features wieder zerlegen.
- *`setup` von `app` abhängig machen:* zieht die gesamte Lobby ins Setup-Artefakt.

### 7. Items: `ItemRegistry` mit Identitäts-Tag und einem Dispatch-Node

- **Anmeldung:** Ein Modul meldet `context.items().register(LobbyItem)` an. Ein `LobbyItem` besteht aus:
  - einem `Key` (`titan:navigator`),
  - dem `ItemStack`,
  - optional einem `ItemSlot` (Hotbar 0–8, Ausrüstungsplatz oder ohne festen Platz),
  - einem Handler für die Benutzung.
- **Identität:** Die Registry stempelt dem `ItemStack` beim Anmelden den Tag `titan:item = <key>` auf.
- **Ein Listener für alle:** Ein einziger Listener am Plattform-Node für `PlayerUseItemEvent` liest den Tag und ruft den Handler des besitzenden Moduls auf. Ohne Tag passiert nichts. So erfüllt eine gewöhnliche Feder die Spec.
- **Konflikte:** werden direkt nach `enableAll` geprüft (`validate()`). Bei Doppelbelegung eines Platzes wird der Start abgebrochen.
- **Ausstattung:** `items().equip(player)` räumt das Inventar und setzt alle Items mit festem Platz. Das Spawn- und das Respawn-Modul rufen es auf.
- **Items ohne festen Platz:** Das Feuerwerk ist ohne Platz angemeldet. Das Elytra-Modul gibt es selbst in die Nebenhand und entfernt es wieder, der Boost läuft trotzdem über den Dispatch.

Vorbild ist mapmakers `ItemHandler`/`ItemRegistry`.

**Alternative:** Aves `HotBarLayout`. Das ist ein reiner 9-Slot-Container ohne Konfliktprüfung und ohne Dispatch. Er kann intern als Container dienen, ist aber nicht die API.

### 8. Navigator: Einträge als Daten, ein geteiltes Inventar, ein Klick-Listener

- **Einträge als Daten:** `context.navigator().add(NavigatorEntry(slot, icon, name, destination))`. Die Registry sammelt die Einträge pro Modul. Beim Abschalten eines Moduls fallen seine Einträge weg.
- **Aufgaben des Navigator-Moduls:**
  - meldet die Feder als `LobbyItem` an,
  - trägt die Ziele aus seiner Config ein,
  - baut das Inventar mit **Aves** (Projektvorgabe: Inventare laufen über Aves), und zwar **einen** `GlobalInventoryBuilder` (`CHEST_1_ROW`) für alle Spieler, weil der Inhalt für alle gleich ist,
  - ruft `register()` genau einmal in `enable()` auf und `unregister()` in `disable()`. Damit gibt es keine Listener-Anmeldung zur Laufzeit.
  - aktualisiert das Layout über Aves (`invalidateDataLayout` bzw. neues Layout), wenn sich die Einträge geändert haben (Versionszähler der Registry),
  - leitet Klicks über die Klick-Handler der Aves-Slots weiter und schließt das Inventar.
- **Weg fallen:** Caffeine-Cache, `PersonalInventoryBuilder` pro Spieler und Listener pro Spieler. Damit ist das Leck strukturell behoben. Das Leck lag nicht an Aves, sondern daran, dass pro Spieler Builder angemeldet und nie abgemeldet wurden.
- **Zwei Stellen für Einträge:** Die Einträge (Registry) sind Plattform, die Darstellung ist das Feature. Andere Module steuern Einträge bei, ohne vom Navigator-Modul abzuhängen. Ist das Navigator-Modul aus, bleiben die Einträge einfach ungenutzt.
- **Weiterleitung:** über `Deliver` (`taskBuilder().taskName(destination)`) wie heute. Ohne CloudNet greift `NoopDeliver`.

**Alternative:** ein eigenes Minestom-`Inventory` mit eigenem Klick-Listener. Das war die ursprüngliche Wahl, wurde aber verworfen: Inventare laufen im Projekt einheitlich über Aves (Vorgabe des Maintainers). Zwei Inventar-Mechanismen nebeneinander widersprächen DRY und der Wartbarkeit.

**Testbarkeit:** Unit-Test für die Berechnung des Layouts (Slot → Item), Integrationstest mit `Env` für Öffnen, Klick und Weiterleitung. Der Leak-Test (Anzahl der Listener bleibt bei vielen Spielern gleich) muss grün bleiben.

**Später:** Ziele pro Spieler, etwa nach Permission, würden ein Inventar pro Öffnung erfordern, bei gleichem einzelnen Klick-Listener. Das ist nicht Teil dieser Change.

### 9. Tags gehören dem Feature

Jedes Feature definiert seine Tags package-private mit Namespace, z.B. `Tag.UUID("titan:sit/arrow")`. Die zentrale `common/utils/Tags` entfällt. Spieler-Tags werden nicht über Sessions hinweg gespeichert, eine Umbenennung ist also unkritisch.

### 10. Architekturregeln mit ArchUnit (`app/src/test/.../ArchitectureTest`)

1. `slices().matching("..app.feature.(*)..").should().notDependOnEachOther()`
2. Klassen in `..app.module..` und `..titan.common..` hängen nicht von `..app.feature..` ab.
3. Nur `*Module` und `*Config` in `..app.feature..` dürfen `public` sein.
4. Keine Klasse außerhalb von `..app.module..` ruft `EventNode#addListener` oder `GlobalEventHandler#addListener` auf. Ausnahmen sind `TitanApplication` und Plattform-Code.

`FreezingArchRule` ist nicht nötig, weil die Migration in dieser Change vollständig ist.

### 11. Vorgehen test-first pro Feature

1. **Charakterisierungstest** mit `Env` (Cyano) gegen den heutigen Listener-Code. Wo es Tests schon gibt, werden sie erweitert.
2. **Umzug** ins Feature-Paket.
3. **Tests grün,** alte Klassen löschen.

Bekannte Bugs außerhalb des Scopes (Tickle-Cooldown) werden **nicht** mit festgeschrieben. Der betroffene Test bekommt `@Disabled("siehe Folge-Change tickle-cooldown")` und prüft das *gewünschte* Verhalten.

### 12. Feuerwerks-Boost aus Voyager portiert (Umfang: nur Boost, keine Flugsimulation; Portierung statt Abhängigkeit, da kein Artefakt veröffentlicht)

Der bisherige Boost (`FireworkBoostPhysics` + `FireworkBoostTracker`, server-seitig per `setVelocity` mit zufälliger Lebensdauer und `elytra.boostMultiplier`) wird durch Voyagers Boost-Mechanik ersetzt: `net.elytrarace.voyager.platform.flight.FireworkBoostTracker` und `net.elytrarace.voyager.server.game.Rockets` (Repo `onelitefeather/Voyager`, Branch `main`).

- **Nur der Boost, nicht die Flugsimulation:** Voyagers server-seitige Flugsimulation (`voyager/physics`, das Server-Schatten-Tracking für Rennauswertung) ist nicht Teil dieser Portierung. Übernommen wird ausschließlich, wie ein Boost entsteht und endet.
- **Client-seitiger Impuls statt Server-`setVelocity`:** Wie im Spiel ohne Mod feuert die Lobby beim Benutzen der Feuerwerksrakete eine echte Raketen-Entität ab, die als `shooter` auf den Spieler zeigt (`FireworkRocketMeta#setShooter`), keine Schwerkraft/Physik hat und nach `elytra.burnDurationTicks` wieder entfernt wird. Der Client wendet den vanilla-Impuls selbst an; die Lobby setzt keine Geschwindigkeit mehr.
- **Determinismus statt Würfelwurf:** Vanilla würfelt die Lebensdauer einer Rakete aus (`10 * flightDuration + random(6) + random(7)`); Voyager - und jetzt Titan - verwenden stattdessen feste Ticks (`elytra.burnDurationTicks`, `elytra.cooldownTicks`), damit zwei Boosts gleich stark sind. Kein `Random` mehr im Boost-Pfad.
- **Zwei Zähler statt Physik-Nachrechnung:** `FireworkBoostTracker` (in `app.feature.elytra`, package-private) hält pro Spieler nur Brenn- und Abklingzeit-Ticks und wird einmal pro Server-Tick über `context.tasks()` in `ElytraModule#enable` fortgezählt (kein Listener, der erst zur Laufzeit registriert wird). Eine zweite Rakete während des Brennens oder der Abklingzeit wird abgelehnt; `ElytraConfig` erzwingt `cooldownTicks > burnDurationTicks`, damit zwei Raketen niemals gleichzeitig auf demselben Spieler brennen.
- **Kein Multiplikator mehr:** Der Impuls ist vanilla-fest, keine Servergröße mehr. `elytra.boostMultiplier` entfällt ersatzlos; die Migration verwirft `elytraBoostMultiplier` (siehe `lobby-module-config` Spec).
- **Portierung statt Abhängigkeit:** Voyager veröffentlicht kein Artefakt, das die Lobby einbinden könnte, und Titan bleibt Apache-2.0-only. Der Code wird deshalb kopiert/angepasst, nicht importiert; jede portierte Klasse trägt einen kurzen Javadoc-Hinweis „Ported from Voyager (…)“ und Titans Apache-Header.
- **Getestet:** `FireworkBoostTrackerTest` portiert Voyagers gleichnamige Testklasse eins zu eins auf `ElytraConfig` - reine Zähler-Arithmetik ohne `Env`, F.I.R.S.T. entsprechend die unterste Stufe der Testpyramide. `ElytraModuleTest` deckt den Weg über `ModuleHarness`/`Env` ab: Rakete wird abgefeuert und trägt den Spieler als Shooter, verschwindet nach `burnDurationTicks`, eine zweite Nutzung während Brennen/Abklingzeit feuert nichts ab, und Landen setzt den Zustand zurück.

## Risks / Trade-offs

- **[Risiko] Migration zerstört eine Betreiber-Config** → Die Altdatei wird vorher nach `.v1.bak` kopiert, eine kaputte Datei nie überschrieben. Ein Test migriert die echte `app.json` aus dem Repo und vergleicht das Verhalten.
- **[Risiko] Setup-Server schreibt einen Wert, den die Lobby später ablehnt** → Die Setup-Argumente sind bereits typisiert (`Integer`, `Material`, `Vec`). Die Lobby bricht mit einer klaren Meldung ab statt still weiterzulaufen. Das ist der gewünschte Fail-fast.
- **[Trade-off] `context.listen` statt rohem Node** → Weniger Flexibilität (Filter, Kind-Nodes). Der Gewinn ist garantierte Fehlerzuordnung und garantiertes Aufräumen. Die API wird gezielt erweitert, wenn ein Modul das braucht.
- **[Risiko] Listener-Anzahl ist über die Minestom-API schwer zu zählen** → Die Registry zählt selbst, was über `listen` angemeldet wird. Für den Navigator-Leak-Test zusätzlich: Die Plattform- und Feature-Nodes haben nach N Spielern dieselben Kinder und dieselbe Anzahl Handler. Details stehen unter Open Questions.
- **[Trade-off] Ein geteiltes Navigator-Inventar** → Ziele pro Spieler sind damit nicht möglich. Heute sind die Ziele für alle gleich, und die Togglz-Bindung war wirkungslos.
- **[Risiko] Großer Umbau in einem PR** → Die Tasks sind so geschnitten, dass nach jedem Feature-Umzug der Build grün und die Lobby lauffähig ist. Der PR kann feature-weise reviewt werden, alternativ als Commit-Serie.

## Migration Plan

1. **Deploy:** neues Jar ausrollen. Beim ersten Start migriert die Lobby `app.json` automatisch, `app.json.v1.bak` liegt daneben.
2. **Prüfen:** Das Log nennt die verworfenen Schlüssel. Im Spiel prüfen: Navigator-Feder in Slot 4, Elytra, Sitzen, Navigator-Ziele.
3. **Rollback:** altes Jar zurück und `app.json.v1.bak` nach `app.json` kopieren. Das alte Jar versteht das neue Format nicht, deshalb ist das Zurückkopieren Pflicht. Das gehört in die Release-Notes.
4. **Setup-Server** und Lobby werden zusammen ausgerollt, weil beide dieselbe `app.json`-Version erwarten.

## Open Questions

- Wie die Listener-Anzahl im Test genau gezählt wird: über eine öffentliche Minestom-API, falls vorhanden, sonst über den Zähler der Registry plus Reflection auf `EventNodeImpl` nur im Test. Die Spec ändert sich dadurch nicht.
- Ob `StopCommand`/`EndCommand` später ein eigenes „admin“-Modul werden. Für diese Change bleiben sie Plattform-Befehle in `Titan`.
