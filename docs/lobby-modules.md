# Lobby-Features bauen

Ein Lobby-Feature ist eine ganz normale [Avaje Inject](https://avaje.io/inject/)-Bean, kein
eigener Plattformtyp mehr. Dieses Dokument erklärt den Aufbau, die Regeln für den Tick-Thread, den
Testaufbau ohne Harness und die Checkliste für ein neues Feature. Alle Codebeispiele stammen, wo
nicht anders vermerkt, aus dem lauffähigen Vorlagefeature
`app/src/test/java/net/onelitefeather/titan/app/feature/example/` (`ExampleModule`,
`ExampleGreetingItems`, `ExampleGreetingRule`, `ExampleGreetingSettings`, `ExampleGreetingTracker`)
- kopierbar als Ausgangspunkt für ein echtes Feature. Es ist bewusst test-only (`app/src/test`,
nicht `app/src/main`), damit es nie als echtes Feature mitläuft: Avaje Inject prozessiert
Annotationen nur für `app/src/main` (kein `testAnnotationProcessor`, s. `app/build.gradle.kts`).

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
ihr `start()` läuft nie - `ArchitectureTest#classesWithPostConstructInFeaturesAreSingleton` lässt
den Build in diesem Fall fehlschlagen.

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

Ein neues Feature wählt eine freie Zahl aus der Lücke. Zwei Features mit demselben Wert lässt
`ArchitectureTest#eventPriorityValuesAreUniqueAcrossFeatures` fehlschlagen und nennt beide.

**Fehlt eine Konstruktor-Abhängigkeit ganz** (kein passender `@Bean`/`@Singleton` im Scope), bricht
`BeanScope.builder().build()` mit einer Exception ab, die den fehlenden Typ nennt - noch bevor ein
Spieler verbinden kann. `TitanApplication.main` fängt jede `RuntimeException`/`Error` aus
`new Titan()`/`titan.initialize()` ab, loggt sie als `Titan failed to start: …` und beendet den
Prozess mit Exit-Code 1.

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

`app/src/test/java/net/onelitefeather/titan/app/architecture/ArchitectureTest` prüft im Build,
nicht nur per Konvention:

1. Feature-Pakete unter `..app.feature.(*)..` hängen nicht voneinander ab.
2. Klassen in `..app.module..` und `..titan.common..` hängen nicht von `..app.feature..` ab.
3. In `..app.feature..` ist nur `*Module` `public`, dazu die von Avaje Inject generierten
   `$DI`-Klassen.
4. Klassen in `..app.feature..` rufen `EventNode#addListener`/`#addChild` oder
   `MinecraftServer#getGlobalEventHandler()` nie direkt auf - nur über `FeatureNode`.
5. Jede Klasse in `..app.feature..` mit einer `@PostConstruct`-Methode trägt `@Singleton`.
6. Kein Feature-Code hängt von `io.avaje.inject.BeanScope` ab - Abhängigkeiten kommen
   ausschließlich über den Konstruktor.
7. Die Werte von `EVENT_PRIORITY` sind über alle Features eindeutig
   (`ArchitectureTest#eventPriorityValuesAreUniqueAcrossFeatures`, Reflection statt `ArchRule`).
8. `..app.feature.navigator..` hängt nicht von `io.avaje.config..` ab.
9. `..app.module..` (die Plattform) hängt nicht von `..app.bootstrap..` (der Kompositionswurzel)
   ab.

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
