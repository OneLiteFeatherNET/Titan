# Lobby-Features bauen

Ein Lobby-Feature ist eine ganz normale [Avaje Inject](https://avaje.io/inject/)-Bean, kein
eigener Plattformtyp mehr. Dieses Dokument erklärt den Aufbau, die Regeln für den Tick-Thread, den
Testaufbau ohne Harness und die Checkliste für ein neues Feature. Alle Codebeispiele stammen, wo
nicht anders vermerkt, aus dem lauffähigen Vorlagefeature
`apps/cloudnet/src/test/java/net/onelitefeather/titan/runtime/feature/example/` (`ExampleModule`,
`ExampleGreetingItems`, `ExampleGreetingRule`, `ExampleGreetingSettings`, `ExampleGreetingTracker`)
- kopierbar als Ausgangspunkt für ein echtes Feature. Es ist bewusst test-only, damit es nie als
echtes Feature mitläuft: Avaje Inject prozessiert Annotationen nur für `src/main` (kein
`testAnnotationProcessor`, s. `buildSrc/src/main/kotlin/titan.column.gradle.kts`).

## Aufbau eines Features

Ein Feature ist eine `@Singleton`-Klasse ohne gemeinsame Schnittstelle. Es hängt in
`@PostConstruct` seinen eigenen Event-Node an den geteilten `titan`-Node und trennt sich in
`@PreDestroy` zuerst wieder davon, bevor die restliche Abschaltlogik läuft:

```java
@Singleton
public final class TickleModule {
    static final int EVENT_PRIORITY = 600;
    private final EventNode<Event> titan;
    private FeatureNode node;

    TickleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Clock clock) { ... }

    @PostConstruct
    void start() {
        Config.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis); // Startprüfung
        this.node = FeatureNode.attach(this.titan, "tickle", EVENT_PRIORITY)
                .on(EntityAttackEvent.class, new TickleAttackHandler(this.clock));
    }

    @PreDestroy
    void stop() { this.node.close(); }
}
```

(`app/src/main/java/net/onelitefeather/titan/app/feature/tickle/TickleModule.java`)

- Der Avaje-Container ruft `@PostConstruct` beim Aufbau des `BeanScope` auf - also bevor ein
  Spieler die Lobby erreichen kann - und `@PreDestroy` beim Schließen des `BeanScope`
  (`Titan#initialize` schedult `beanScope::close` als Shutdown-Task). Es gibt keinen separaten
  Enable-/Disable-Schritt außerhalb des Bean-Lebenszyklus mehr.
- `FeatureNode.attach(parent, featureId, priority)` legt den Feature-eigenen Event-Node an,
  registriert ihn beim geteilten `titan`-Node und setzt seine Priorität. `on(type, listener)`
  registriert einen Listener, gekapselt in `TitanObservability.guard(featureId, listener)`, damit
  ein Fehler dem Feature und - falls vorhanden - dem Spieler zugeordnet wird.
  `onIncludingCancelled(type, listener)` liefert das Event auch, wenn es beim Erreichen des
  Feature-Node schon abgebrochen ist (Minestoms Standardverhalten für einen `Consumer`-Listener
  überspringt ein bereits abgebrochenes `CancellableEvent` sonst). Kein heutiges Feature braucht
  das - der Navigator etwa reagiert gar nicht über einen Event-Node-Listener auf Klicks, sondern
  über Aves' eigenen, direkt aufs Inventar gemappten Click-Handler, der vor jedem regulären
  Event-Node läuft (s. `NavigatorModule`, Javadoc).
- `node.close()` in `@PreDestroy` hängt den Feature-Node ab, **bevor** die übrige Abschaltlogik
  läuft (s. `ElytraModule#stop()`, das erst `node.close()`, dann `task.cancel()` aufruft) - so
  erreicht kein Event mehr das Feature, während es sich selbst abbaut.
- Abhängigkeiten kommen über den **Konstruktor**: eine `Instance`, ein `Deliver`, ein `Clock`
  werden einfach angefordert (s. `NavigatorModule(EventNode, Deliver, FeatureFlags)`,
  `SpawnModule(Instance, LobbySpawn, EventNode, LobbyItems)`). `@Inject` braucht nur eine Klasse
  mit **mehr als einem** Konstruktor (s. `TickleModule`, dessen echter Konstruktor `@Inject
  TickleModule(EventNode, Clock)` trägt).
- Ein Feature-Paket unter `app/feature/<name>` folgt einer festen Sichtbarkeit (s.
  `ArchitectureTest`, unten): nur die Klasse `<Name>Module` ist `public`, alles andere - Handler,
  Item-Factory, Tags, paketprivate Prüffunktionen - ist paketprivat. Avaje Inject generiert seine
  Verdrahtung in derselben Package wie die annotierte Klasse, braucht also nirgends eine `public`
  Item-Factory. Geprüft wird das nur für Produktionscode unter `app/src/main` - das test-only
  Vorlagefeature läuft mit `ImportOption.DoNotIncludeTests` nicht mit.

## Gefunden werden: `@Singleton` genügt

Es gibt keine zentrale Feature-Liste. `Titan` baut im Konstruktor `BeanScope.builder().build()` -
das allein reicht, damit jedes `@Singleton`-Feature gebaut und über sein `@PostConstruct`
gestartet wird. Fehlt `@Singleton` an einer Klasse mit `@PostConstruct`, wird sie nie gebaut und
ihr `start()` läuft nie - `AppFeatureColumnArchitectureTest#classesWithPostConstructAreSingleton`
(bzw. je Column deren eigener `ColumnArchitectureTest`) lässt den Build in diesem Fall
fehlschlagen.

**Die Reihenfolge, in der zwei Features dasselbe Event verarbeiten, legt `EVENT_PRIORITY` fest**,
nicht mehr eine Startreihenfolge - Minestoms `EventNode#setPriority(int)` ordnet Geschwisterknoten.
Die heutigen sieben Features, in Hunderterschritten mit Platz dazwischen:

| Feature | `EVENT_PRIORITY` |
|---|---|
| protection | 100 |
| spawn | 200 |
| respawn | 300 |
| navigator | 400 |
| sit | 500 |
| tickle | 600 |
| elytra | 700 |

Ein neues Feature wählt eine freie Zahl aus der Lücke. Eindeutigkeit ist kein ArchUnit-Test,
sondern eine Startlaufzeit-Prüfung: `FeatureNode.attach(parent, featureId, priority)` wirft eine
`IllegalStateException`, sobald `parent` bereits ein Kind mit derselben `priority` hat, und nennt
darin beide Feature-Ids sowie die kollidierende Position.

**Fehlt eine Konstruktor-Abhängigkeit ganz** (kein passender `@Bean`/`@Singleton` im Scope), bricht
`BeanScope.builder().build()` mit einer Exception ab, die den fehlenden Typ nennt - noch bevor ein
Spieler verbinden kann. `TitanApplication.main` fängt jede `RuntimeException`/`Error` aus
`new Titan()`/`titan.initialize()` ab, loggt sie als `Titan failed to start: …` und beendet den
Prozess mit Exit-Code 1.

Sind alle Features gestartet, loggt `Titan`s Konstruktor einmal die tatsächliche
Startreihenfolge auf INFO-Level: `Lobby features started in event order: {}` (siehe
`app/.../bootstrap/FeatureStartupLog`). Die Liste kommt nicht aus einer gepflegten Feature-Liste,
sondern aus den Kindknoten des `titan`-Knotens selbst, aufsteigend nach `EventNode#getPriority()`
sortiert - dieselbe Reihenfolge wie die Tabelle oben.

Welche Plattform-Dienste als Bean zur Verfügung stehen, steht in
`app/src/main/java/net/onelitefeather/titan/app/bootstrap/PlatformBeans.java` (`@Factory` mit
einer `@Bean`-Methode je Dienst: `InstanceContainer`, `MapProvider`, `LobbySpawn`, `Deliver`, der
`@Named("titan")`-qualifizierte `EventNode<Event>`, `FeatureFlags`, `Clock`, `Scheduler`).
`LobbyItems` ist selbst eine `@Singleton`-Bean im Paket `app.module.item` (s. unten). Braucht ein
neues Feature einen **neuen** geteilten Dienst, der im Kern ein Plattformtyp aus `common` oder
Minestom ist, kommt eine weitere `@Bean`-Methode in dieselbe `PlatformBeans`-Factory dazu; trägt er
selbst feature-übergreifende Logik, wird er eine eigene `@Singleton`-Klasse, die betroffene
Features per Konstruktor anfordern.

## Items als Beans: `LobbyItem`, `@Factory`, `LobbyItems`

Ein Hotbar- oder Ausrüstungsitem meldet ein Feature nicht mehr über einen Kontext an, sondern
stellt es als `@Bean LobbyItem` in einer eigenen, paketprivaten `@Factory`-Klasse bereit:

```java
@Factory
final class NavigatorItems {
    @Bean
    LobbyItem navigatorFeather(NavigatorModule navigator) {
        ItemStack feather = ItemStack.builder(Material.FEATHER)
                .customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();
        return new LobbyItem("navigator", Key.key("titan:navigator"), feather,
                ItemSlot.hotbar(4), (player, event) -> navigator.open(player));
    }
}
```

(`app/src/main/java/net/onelitefeather/titan/app/feature/navigator/NavigatorItems.java`)

Die Plattform-Bean `LobbyItems` (`app/src/main/java/net/onelitefeather/titan/app/module/item/`)
bekommt jedes `LobbyItem` per Listen-Injektion (`List<LobbyItem>`), stempelt es mit dem
Identitäts-Tag `LobbyItems.IDENTITY_TAG` und dispatcht ein einziges `PlayerUseItemEvent` am
`titan`-Node an den passenden `onUse`-Handler - ein Feature braucht dafür keinen eigenen Listener.
Zwei Items mit demselben `key()` oder demselben festen `ItemSlot` (`hotbar(0..8)` oder
`equipment(EquipmentSlot)`) lassen den `LobbyItems`-Konstruktor mit `IllegalStateException`
abbrechen, die beide Items nennt; `ItemSlot.unplaced()` (z. B. das Elytra-Feuerwerk, das nur
während des Fliegens in der Nebenhand liegt) ist davon ausgenommen. `LobbyItems#equip(player)`
räumt das Inventar und setzt alle Items mit festem Platz - das rufen Spawn- und Respawn-Feature auf
(`LobbyItems#equip`), nicht jedes Feature selbst; `LobbyItems#stack(key)` gibt den gestempelten
Stack für ein unplatziertes Item heraus, das ein Feature selbst aushändigt (`ElytraModule`s
Feuerwerk).

Der Navigator ist der einzige Sonderfall ohne Andockpunkt: Seine vier Ziele stehen fest im
package-privaten `enum Destination` (s. `openspec/changes/navigator-entries-in-code/design.md`).
Nur `Destination.SLENDER` bleibt hinter der Feature-Flag `NAVIGATOR_SLENDER` versteckt, ausgewertet
über `FeatureFlags`, dem `NavigatorModule` per Konstruktor übergeben.

## Tasks über den injizierten `Scheduler`

Ein wiederkehrender Task wird direkt über den injizierten Minestom-`Scheduler` geplant, nicht über
einen eigenen Andockpunkt:

```java
this.task = this.scheduler.scheduleTask(this.boosts::advance, TaskSchedule.tick(1), TaskSchedule.tick(1));
```

(`app/src/main/java/net/onelitefeather/titan/app/feature/elytra/ElytraModule.java`, `start()`).
`stop()` bricht ihn **nach** `node.close()` ab (s. oben, "Aufbau eines Features") - erst der
Event-Node weg, dann der Task.

## Konfiguration lesen: die statische Fassade `Config`

Unverändert gegenüber vorherigen Changes (s. `openspec/changes/avaje-config-facade/design.md` und
`openspec/changes/config-reload-feature-flags/design.md`): Ein Feature liest seine Werte direkt
über die statische Fassade `io.avaje.config.Config`, an zwei Stellen mit unterschiedlichem Zweck.

**Einmal streng in `start()`**, wo ein ungültiger Wert weiterhin den Start abbricht:

```java
// TickleModule.start()
long cooldownMillis = Config.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis);
```

- **Zahlen** kommen über `Config.getAs(key, Integer::parseInt)` (entsprechend `Long::parseLong`,
  `Double::parseDouble`), nicht über `Config.getInt/getLong/getDecimal`: `getAs` fängt einen Fehler
  der eigenen Funktion ab und wirft eine `IllegalStateException`, die den vollen Schlüssel nennt
  und die ursprüngliche Exception als `cause` behält.
- Der **Schlüssel** ist eine `private static final String`-Konstante im Feature, nach dem Schema
  `<feature-id>.<feld>` (z. B. `"tickle.cooldownMillis"`).
- Die **Prüfung** liegt in einer reinen, statischen, paketprivaten Funktion - für einen Einzelwert
  dient sie direkt als `getAs`-Funktion (`TickleSettings.cooldownMillis(String)`); für eine Prüfung
  über mehrere Felder (`spawn.minHeight`/`maxHeight`) oder eine, die nicht über `getAs` läuft
  (`sit.allowedBlocks`), nennt sie den vollen Schlüssel selbst.
- **Standardwerte gehören in `application.yaml`**, nicht in den Code - ein Feature liest ohne
  eigenen Fallback (`Config.get(key)`, nicht `Config.get(key, "…")`). Fehlt ein Schlüssel, bricht
  der Start mit `Missing required configuration parameter [key]` ab.

**Ein zweites Mal am Gebrauchsort**, für jeden Wert, der sich zur Laufzeit ändern soll (heute jeder
Wert von tickle, sit, elytra und spawn): direkt über `Config.<Methode>(key)`, ohne erneute Prüfung
und ohne Rückfall, z. B. `TickleAttackHandler` bei jedem Angriff. Konfiguration wird nur einmal
geprüft, beim Start; ein Lesevorgang am Gebrauchsort kann bei einem zur Laufzeit ungültig
gewordenen Wert entweder einfach falsch wirken oder fehlschlagen - nichts davon wird geloggt oder
abgefangen. Ein Wert, der so gelesen wird, hat trotzdem seine eigene Prüffunktion für den einen
strengen Start-Read (s. oben).

Es gibt seit `dissolve-module-platform` keine Bindung an ein Config-Record und keinen
`ConfigChangeHandler` mehr. Ein Feature, das seine Werte am Gebrauchsort liest, braucht für ein
Neuladen der Konfiguration zur Laufzeit (avaje-configs Dateiüberwachung, s. README, Abschnitt
"Runtime reloading") nichts Eigenes zu tun: Der geänderte Wert gilt beim nächsten Lesevorgang, ohne
Abschalten oder erneutes `start()`.

## Regeln für den Tick-Thread

`start()` läuft beim Aufbau des `BeanScope`; jeder darüber registrierte Listener läuft danach auf
dem Tick-Thread. Daraus folgen vier Regeln:

1. **Keine Listener-Registrierung zur Laufzeit.** Ein Feature registriert alles, was es braucht,
   in `start()` - nie erst, wenn ein Spieler joint oder ein Menü öffnet. Der Navigator-Speicherleck
   auf `main`, der diese Regel motiviert hat, entstand genau dadurch, dass pro Spieler zur Laufzeit
   neue Listener angemeldet wurden, ohne sie je wieder abzumelden.
2. **Kein blockierendes IO/HTTP in Handlern.** Ein Listener, der auf eine HTTP-Antwort wartet,
   blockiert den gesamten Tick und damit jeden Spieler in der Lobby.
3. **Pakete/Components zwischenspeichern statt neu bauen.** `ExampleItems` baut die feste
   Rückmeldung `ON_COOLDOWN` einmal als `static final Component`; `NavigatorModule` legt das
   geteilte Aves-Inventar nur neu an, wenn sich die sichtbare Zielmenge geändert hat, nicht bei
   jedem Öffnen.
4. **Spielerbezogener Zustand gehört aufgeräumt.** Zustand pro Spieler (ein Cooldown-Zeitstempel)
   muss bei `PlayerDisconnectEvent` entfernt werden, sonst wächst er unbegrenzt -
   `ExampleGreetingTracker#clear` und `FireworkBoostTracker#forget` folgen diesem Muster.

## Tests: ohne Harness

Es gibt keinen eigenen Test-Harness mehr. Ein Feature-Test baut das Feature entweder direkt oder
über einen echten `BeanScope`.

### Unten: reine Unit-Tests

Reine Entscheidungs- und Formatierungslogik gehört in eine eigene, paketprivate Klasse ohne
Minestom-Abhängigkeit - `ExampleGreetingRuleTest` prüft `ExampleGreetingRule.isOnCooldown(...)` und
`.greeting(...)` ganz ohne `Env` oder `Player`. Ein Unit-Test prüft ebenso die paketprivate
Prüffunktion aus "Konfiguration lesen" direkt, ganz ohne `Config`.

### Mitte: direkte Konstruktion mit `TestTitanNode`

Ein Env-Integrationstest baut das Feature mit Fakes und einem frischen Test-`titan`-Node:

```java
try (TestTitanNode titan = TestTitanNode.attach(env)) {
    TickleModule module = new TickleModule(titan.node(), Clock.fixed(NOW, ZoneOffset.UTC));
    module.start();
    try {
        env.process().eventHandler().call(new EntityAttackEvent(attacker, target));
        // ...
    } finally {
        module.stop();
    }
}
```

`net.onelitefeather.titan.app.testutils.TestTitanNode` (s. `ProtectionModuleTest`, `SpawnModuleTest`,
`TickleModuleTest`, `ElytraFixture`, `NavigatorFixture`) hängt einen frisch benannten Node unter
`env.process().eventHandler()` und hebt ihn beim `close()` wieder ab - genau das, was
`PlatformBeans` in Produktion tut, ohne den `BeanScope`. Ein Feature mit eigenen Items baut daneben
eine eigene `LobbyItems`-Instanz aus den Items, die dessen `@Factory`-Klasse liefert (s.
`SpawnModuleTest`, `ElytraFixture`). Ein Test, der prüft, dass nach `stop()` keines der Events mehr
Code des Features auslöst, ist Pflicht für jedes Feature (s. `SpawnModuleTest#stopLeavesNoListenerBehind`
u. Ä.).

### Oben: der echte `BeanScope`

Ein Test, der die reale Verdrahtung mehrerer Features zusammen prüft (z. B. dass ein
Navigator-Klick trotz `ProtectionModule`s Abbruch weiterleitet), baut den echten `BeanScope` -
`app/src/test/java/net/onelitefeather/titan/app/bootstrap/WiringTest.java`,
`NavigatorProtectionOrderingTest`, `StandardLoadoutTest` (alle in `app.bootstrap`, neben der
Kompositionswurzel, nicht in einem einzelnen Feature-Paket):

```java
BeanScope scope = BeanScope.builder().forTesting()
        .mock(MapProvider.class).mock(FeatureFlags.class).build();
try {
    NavigatorModule navigator = scope.get(NavigatorModule.class);
    // ...
} finally {
    scope.close();
}
```

`BeanScope.builder().forTesting().mock(Type)` registriert vor dem Aufbau einen Mockito-Mock für
Typen, die die Filesystem oder eine statische Fassade berühren (`MapProvider`, `FeatureFlags`) -
jede generierte `@Bean`-Methode prüft, ob ihr Typ schon geliefert wurde, bevor sie ihn selbst baut.
Jedes andere Bean (alle sieben Features, `LobbyItems`, `Deliver`, `Clock`, `Scheduler`) wird exakt
so gebaut wie in Produktion. `.bean(Type, instance)` liefert statt eines Mocks eine echte
Testinstanz (z. B. einen aufzeichnenden `Deliver`).

Item-Dispatch wird über ein direkt gefeuertes `PlayerUseItemEvent` getestet, Chat-Ausgaben über
`TestConnection#trackIncoming(SystemChatPacket.class)`. `Collector#collect()` (und die
`assertSingle()`/`assertEmpty()`-Kurzformen) **entnimmt** den Tracker aus der Verbindung - für
einen Test mit mehreren Aktionen deshalb erst alle Events feuern und danach genau einmal
`collect()` aufrufen.

## Architekturregeln (ArchUnit)

`app/src/test/java/net/onelitefeather/titan/app/architecture/ArchitectureTest` und
`AppFeatureColumnArchitectureTest` (wendet `core`s `ColumnArchitectureRules`-Testfixtures auf
`..app.feature..` an) prüfen im Build, nicht nur per Konvention:

1. Feature-Pakete unter `..app.feature.(*)..` hängen nicht voneinander ab.
2. Klassen in `..app.module..` und `..titan.common..` hängen nicht von `..app.feature..` ab.
3. In `..app.feature..` ist nur `*Module` `public`, dazu die von Avaje Inject generierten
   `$DI`-Klassen.
4. Klassen in `..app.feature..` rufen `EventNode#addListener`/`#addChild` oder
   `MinecraftServer#getGlobalEventHandler()` nie direkt auf - nur über `FeatureNode`
   (`ColumnArchitectureRules.FEATURES_REGISTER_LISTENERS_ONLY_THROUGH_FEATURE_NODE`).
5. Jede Klasse in `..app.feature..` mit einer `@PostConstruct`-Methode trägt `@Singleton`
   (`ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON`).
6. Kein Feature-Code hängt von `io.avaje.inject.BeanScope` ab - Abhängigkeiten kommen
   ausschließlich über den Konstruktor
   (`ColumnArchitectureRules.FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE`).
7. `..app.feature.navigator..` hängt nicht von `io.avaje.config..` ab.
8. `..app.module..` (die Plattform) hängt nicht von `..app.bootstrap..` (der Kompositionswurzel)
   ab.

Die Eindeutigkeit von `EVENT_PRIORITY` ist keine ArchUnit-Regel, sondern eine
Startlaufzeit-Prüfung in `FeatureNode.attach` (s. oben, "Gefunden werden: `@Singleton` genügt").

## Wie eine Column Plattform-Beans bekommt

Ab der Column-Architektur (`openspec/changes/split-titan-into-columns`) hängt eine Column nur an
`core`, nie an `:app` (später `runtime`). Beans wie den geteilten `@Named("titan") EventNode<Event>`
sieht sie beim Kompilieren also nicht - ein Fall, den der Spike an `features/protection` klärt
(design.md, Entscheidung D2). Die drei Spike-Fragen und ihre Antworten:

1. **Passt `requires` mit dem qualifizierten, generischen `EventNode<Event>` zur Übersetzung?**
   Nein, nicht mit der einfachen `Class<?>`-Form. `@InjectModule(requires = {EventNode.class})`
   verliert den `@Named("titan")`-Qualifier und den generischen Parameter; der
   Annotationsprozessor bricht mit einer eigenen, sehr genauen Fehlermeldung ab: `No dependency
   provided for net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan`. Die
   Lösung ist `requiresString`, mit genau demselben `Typ:Qualifier`-Schlüssel, den die
   Fehlermeldung selbst benutzt:
   ```java
   @InjectModule(requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
   ```
   Das reicht aber nur für die Übersetzung. Für die Bau-**Reihenfolge** der Module zur Laufzeit
   (welches Modul baut `BeanScope` zuerst) braucht es zusätzlich die einfache `Class<?>`-Form, weil
   nur diese die generierte `AvajeModule.requiresBeans()` füllt, auf der die Reihenfolge beruht -
   `requiresString`-Einträge tauchen dort nicht auf. Beide Formen zusammen, nicht eine allein:
   ```java
   @InjectModule(requires = {EventNode.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})
   ```
   Spiegelbildlich braucht `:app` (später `runtime`) nur die einfache Form -
   `@InjectModule(provides = {EventNode.class})` -, weil es die Bean selbst definiert und keine
   eigene Übersetzungsprüfung dafür braucht. `provides` **und** `providesString` zusammen auf
   diesem (unbenannten) Standard-Scope-Modul auszuprobieren, hat in avaje-inject-generator 12.7
   einen Codegen-Fehler ausgelöst: Im generierten `AppModule` fehlte das Komma zwischen den beiden
   Attributen, ein nicht mehr übersetzbares `@InjectModule(provides = {...}providesString =
   {...})`. Einen entsprechenden Fehlerbericht an avaje-inject sollte ein Folge-Change einreichen.

2. **Wie liest eine spätere Welle (D5) den Modulnamen, ohne dass er mit der Klasse `ProtectionModule`
   kollidiert?** Über `@InjectModule(name = "protectionColumn", ...)` - ein expliziter, von der
   Bean-Klasse verschiedener Name. Der Annotationsprozessor generiert daraus die Modulklasse
   `ProtectionColumnModule` (Name plus `Module`-Suffix), registriert unter
   `META-INF/services/io.avaje.inject.spi.InjectExtension`. D5 liest also nicht "gibt es eine Bean
   `ProtectionModule`", sondern vergleicht die erwartete Liste von Column-Namen (`"protectionColumn"`,
   ...) gegen das, was beim Aufbau des `BeanScope` tatsächlich geladen wurde.

3. **Behält `mergeServiceFiles()` alle Avaje-Module im Shadow-Jar?** Nicht ohne Weiteres. Jede
   Column liefert ihre eigene `META-INF/services/io.avaje.inject.spi.InjectExtension`-Datei; die
   `ShadowJar`-Aufgabe hat als eigene, von `mergeServiceFiles()` unabhängige Voreinstellung
   `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` - und die wirft jede doppelte Ressource schon
   *vor* dem Merge-Transformer weg, sodass am Ende nur `:app`s eigenes Modul übrigblieb (per
   `unzip -p app-titan.jar META-INF/services/io.avaje.inject.spi.InjectExtension` nachgewiesen: nur
   `AppModule`, kein `ProtectionColumnModule`). Die Behebung setzt `duplicatesStrategy =
   DuplicatesStrategy.INCLUDE` nicht für den ganzen Task, sondern gezielt über
   `filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }`
   zusätzlich zu `mergeServiceFiles()`, in `app/build.gradle.kts`: erst dann landen beide
   Modulnamen (`net.onelitefeather.titan.app.AppModule` und
   `net.onelitefeather.titan.feature.protection.ProtectionColumnModule`) in derselben Datei, ohne
   dass andere `META-INF`-Dateien (LICENSE, NOTICE, ...) plötzlich doppelt im Jar landen.
   `titan.app-variant` (Welle 3) muss dieselbe Einstellung übernehmen.

### Das Muster für neue Columns (Welle 2)

Jede Column bekommt in ihrem eigenen `package-info.java` (Wurzelpaket der Column, also
`net.onelitefeather.titan.feature.<name>`):

```java
@InjectModule(
    name = "<name>Column",
    requires = {<PlattformTyp>.class},
    requiresString = {"<voll.qualifizierter.PlattformTyp><generisch, falls vorhanden>:<qualifier, falls @Named>"}
)
package net.onelitefeather.titan.feature.<name>;
```

- `requires` und `requiresString` je nur für einen Plattform-Typ, den die Column tatsächlich
  braucht (aktuell nur `EventNode<Event>:titan`; `hotbar`s `LobbyItems`-Interface hat keinen
  Qualifier und keinen generischen Parameter, dafür reicht `requires = {LobbyItems.class}` allein).
- `name` immer explizit und nie identisch mit einem Bean-Klassennamen der Column.
- `:app` (später `runtime`) braucht die spiegelbildliche `provides`-Deklaration nur als einfache
  `Class<?>`-Form, niemals zusammen mit der `providesString`-Form auf demselben (Standard-Scope-)
  Modul (siehe Frage 1 oben).
- `app/build.gradle.kts`s (später `titan.app-variant`s) `shadowJar` braucht die auf
  `META-INF/services/**` beschränkte `duplicatesStrategy = DuplicatesStrategy.INCLUDE` neben
  `mergeServiceFiles()` - sonst verschwindet die neue Column beim Shaden stillschweigend
  (siehe Frage 3 oben).
- **Eine Column, die selbst `LobbyItem`-Beans beisteuert (`provides = {LobbyItem.class}`) und
  daneben `LobbyItems` benutzt, nimmt `Provider<LobbyItems>` statt `LobbyItems` direkt und lässt
  `LobbyItems` aus `requires`/`requiresString` weg.** `hotbarColumn` sammelt jede `LobbyItem`-Bean
  über Listen-Injektion, bevor es selbst `LobbyItems` bereitstellt - ein direktes `requires =
  {LobbyItems.class}` auf einer Column, die zugleich `LobbyItem` liefert, erzwingt also zwei
  widersprüchliche Bau-Reihenfolgen (diese Column vor `hotbarColumn`, wegen ihrer Items; und nach
  `hotbarColumn`, wegen `LobbyItems`) und lässt `BeanScope.builder().build()` mit "Injecting null
  for ...LobbyItems" abbrechen. Der `Provider` verschiebt den Lookup auf Verwendungszeit (frühestens
  im eigenen `@PostConstruct`, meist erst im Event-Handler), wenn der Scope bereits vollständig
  gebaut ist - siehe `features/elytra`s `ElytraModule` (nimmt `Provider<LobbyItems>`, ruft
  `.get().stack(key)` beim `PlayerStartFlyingWithElytraEvent`) und dessen `package-info.java`.
  `spawn` und `respawn` brauchen das nicht: Sie benutzen `LobbyItems`, liefern aber selbst kein
  `LobbyItem`, also entsteht dort kein Zyklus und `requires = {LobbyItems.class}` bleibt direkt.

Die tatsächlichen `requires`/`requiresString`/`provides` jeder Column, aus ihrer
`package-info.java` abgelesen (`:app`s eigene `package-info.java` deklariert die Plattform-Typen
als `provides`):

| Column | `requires` | `requiresString` | `provides` |
|---|---|---|---|
| `protection` | `EventNode.class` | `EventNode<Event>:titan` | - |
| `admin` | `CommandManager.class` | - | - |
| `spawn` | `Instance.class`, `LobbySpawn.class`, `EventNode.class`, `LobbyItems.class` | `EventNode<Event>:titan` | - |
| `respawn` | `EventNode.class`, `LobbyItems.class` | `EventNode<Event>:titan` | - |
| `navigator` | `EventNode.class`, `Deliver.class`, `FeatureFlags.class` | `EventNode<Event>:titan` | `LobbyItem.class` |
| `sit` | `EventNode.class` | `EventNode<Event>:titan` | - |
| `tickle` | `EventNode.class`, `Clock.class` | `EventNode<Event>:titan` | - |
| `elytra` | `EventNode.class`, `Scheduler.class` | `EventNode<Event>:titan` | `LobbyItem.class` |
| `hotbar` | `EventNode.class` | `EventNode<Event>:titan` | `LobbyItems.class` |

`EventNode` ist in jeder Zeile der einzige `requiresString`-Eintrag, weil es der einzige
qualifizierte, generische Plattform-Typ ist (Frage 1 oben); die anderen Typen sind weder generisch
noch `@Named`, für sie reicht die `requires`-Form allein. `LobbyItems` steht bei `spawn` und
`respawn`, weil ihre Module `LobbyItems` direkt injizieren; `elytra` injiziert es stattdessen als
`Provider<LobbyItems>` (siehe oben) und lässt es deshalb aus `requires` weg, obwohl es
`LobbyItem` liefert.

## Checkliste: neues Feature = neues Paket

1. Neues Paket `app/src/main/java/net/onelitefeather/titan/app/feature/<name>/` anlegen -
   `app/src/test/.../feature/example/` als Kopiervorlage nehmen.
2. `<Name>Module` (public, `@Singleton`, ein noch nicht vergebenes `EVENT_PRIORITY` - s. "Gefunden
   werden" oben und die Tabelle dort) anlegen: `@PostConstruct start()` hängt den `FeatureNode` an
   und registriert die Listener, `@PreDestroy stop()` ruft `node.close()` (und danach ggf.
   `task.cancel()`).
3. Braucht das Feature Konfiguration: Schlüssel-Konstanten und das strenge Lesen über `Config`
   (inklusive `Config.getAs` für Zahlen) in `start()`, die Validierung in einer reinen,
   paketprivaten Funktion (s. "Konfiguration lesen" oben).
4. Braucht das Feature ein Hotbar- oder Ausrüstungsitem: eine eigene, paketprivate `@Factory`-Klasse
   mit einer `@Bean LobbyItem`-Methode (s. "Items als Beans" oben).
5. Abhängigkeiten (eine `Instance`, ein `Deliver`, ein `Clock`, der `Scheduler`, ...) über den
   Konstruktor anfordern. Braucht das Feature einen Plattform-Dienst, den es noch nicht gibt, kommt
   der entweder als weiteres `@Bean` in `PlatformBeans` oder, falls er selbst
   feature-übergreifende Logik trägt, als eigene `@Singleton`-Klasse dazu.
6. Tests schreiben, bevor (oder während) der Code entsteht: Unit-Tests für die reine Logik und die
   Config-Validierung, ein Env-Integrationstest über direkte Konstruktion mit `TestTitanNode` für
   alles, was einen `Player` braucht - inklusive eines Tests, dass `stop()` keinen weiteren
   Event-Effekt mehr hat.
7. Falls das Feature Konfiguration hat: die neuen Schlüssel samt Standardwert in
   `app/src/main/resources/application.yaml` eintragen und im README unter "Configuration Options
   Explained" bzw. "Environment variable reference" dokumentieren.

Das war's - **keine** zentrale Feature-Liste zu pflegen: `@Singleton` genügt, damit `Titan` das
neue Feature beim Aufbau des `BeanScope` findet und startet. Die einzige Ausnahme von "kein
geänderter Code außerhalb des eigenen Pakets" ist ein brandneuer, geteilter Plattform-Dienst
(Schritt 5): Der berührt zwangsläufig `PlatformBeans`, weil dort - und nur dort - Plattform-Typen
zu Avaje-Beans werden.
