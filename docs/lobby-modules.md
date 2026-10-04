# Lobby-Features bauen

Ein Lobby-Feature ist eine ganz normale [Avaje Inject](https://avaje.io/inject/)-Bean in einer
eigenen **Column** - einem eigenen Gradle-Modul unter `features/`. Dieses Dokument erklärt die
Modul- und Varianten-Architektur, den Aufbau eines Features, die Regeln für den Tick-Thread, den
Testaufbau ohne Harness und die Checkliste für ein neues Feature. Alle Codebeispiele stammen, wo
nicht anders vermerkt, aus dem lauffähigen Vorlagefeature
`apps/cloudnet/src/test/java/net/onelitefeather/titan/runtime/feature/example/` (`ExampleModule`,
`ExampleGreetingItems`, `ExampleGreetingRule`, `ExampleGreetingSettings`, `ExampleGreetingTracker`)
- kopierbar als Ausgangspunkt für ein echtes Feature. Es ist bewusst test-only, damit es nie als
echtes Feature mitläuft: Avaje Inject prozessiert Annotationen nur für `src/main` (kein
`testAnnotationProcessor`, s. `buildSrc/src/main/kotlin/titan.app-variant.gradle.kts`).

## Module: Columns, `core`, `runtime`, App-Varianten

```
apps/cloudnet ─┐
apps/local ────┼─▶ runtime ─────────────▶ common ─▶ core
               ├─▶ features/* ──────────────────────▶ core
               └─▶ platform/* (z. B. luckperms) ─────▶ core
```

`apps/cloudnet` bindet `platform/luckperms` immer ein; `apps/local` nur mit der Gradle-Property
`-Ptitan.luckperms` (s. "Permission-Plattform" unten).

- **`core`** enthält nur APIs, keine Implementierung: die Andockpunkte einer Column
  (`net.onelitefeather.titan.core.module.FeatureNode`, `LobbySpawn`, `LobbyItem`, `ItemSlot`,
  `ItemUseHandler`, `net.onelitefeather.titan.core.module.item.LobbyItems`), `FeatureFlags`,
  `EntityDismountEvent`, `Cancelable` sowie `Deliver` (`net.onelitefeather.titan.api.deliver`).
  Dazu kommen `testFixtures` - `TestTitanNode`, `DummyDeliver`, `EventListenerCounter` und die
  geteilten ArchUnit-Regeln `ColumnArchitectureRules` (s. "Architekturregeln" unten).
- **`features/<name>`** (Paket `net.onelitefeather.titan.feature.<name>`) ist eine Column: ein
  eigenes Gradle-Modul, das die Convention `titan.column` anwendet und **nur** an `core` hängt -
  nie an einer anderen Column, nie an `runtime` oder einer App-Variante. Die heutigen
  Columns: `protection`, `spawn`, `respawn`, `navigator`, `sit`, `tickle`, `elytra`, `jumprun`, `hotbar`
  (Hotbar-/Ausrüstungsitems, `LobbyItems`-Implementierung) und `admin` (`/stop`, `/end`).
- **`runtime`** ist der gemeinsame Starter: `TitanApplication` (`main`), `Titan` (baut den
  `BeanScope`), `PlatformBeans`-Äquivalent (`runtime`s eigene `package-info.java` deklariert die
  Plattform-Typen als `provides`), die Start-Logs sowie der Rechte-Vertrag `PermissionService`
  (`core`) und dessen Fallback `DenyAllPermissionService` (`@Secondary`, liefert immer „nicht
  erteilt“). LuckPerms und Butterfly gehören nicht mehr zu `runtime` - LuckPerms steckt in
  `platform/luckperms` (s. "Permission-Plattform" unten), Butterfly (Tab-Sortierung, Präfix über dem Kopf,
  Chat-Format) ist kein Teil von Titan mehr, sondern liegt als Minestom-Extension im Artefakt
  `butterfly-minestom` (genau ein Jar in `extensions/`, ab Version 1.1.1; 1.1.0 spammt bei jedem Spawn „Error creating missing file flags.properties“ und aktualisiert Team-Präfixe bereits online befindlicher Spieler nicht; noch nicht auf Maven: [GitHub-Release](https://github.com/OneLiteFeatherNET/Butterfly/releases/download/v1.1.1/butterfly-minestom-1.1.1.jar), sha256 `033c7ddf02daae27635a1467d4f9055ea52fb1133a2b3eb1a22b3030fbb3226e`).
- **`platform/<name>`** (Paket `net.onelitefeather.titan.platform.<name>`) ist eine
  Permission-Plattform: ein eigenes Gradle-Modul, das nur an `core` hängt und einen
  `PermissionService` liefert. Der heutige Eintrag: `luckperms`.
- **`apps/cloudnet`** (Produktion, mit AOT-Cache, veröffentlicht als `titan-cloudnet`) und
  **`apps/local`** (Entwicklung, ohne AOT-Cache, nicht veröffentlicht) sind dünne
  Assembly-Module: eigener Code nur als Querschnitts-Tests (`WiringTest`,
  `NavigatorProtectionOrderingTest`, `StandardLoadoutTest`, ...), sonst nur eine
  `build.gradle.kts`, die `titan.app-variant` anwendet.
- **`settings.gradle.kts`** bindet `features/*`, `platform/*` und `apps/*` per Verzeichnis-Scan
  ein - jedes Unterverzeichnis mit eigener `build.gradle.kts` wird automatisch ein Projekt. Eine
  neue Column, eine neue Permission-Plattform oder eine neue Variante braucht dafür keine
  Änderung an `settings.gradle.kts`.

### App-Varianten

`titan.app-variant` (Convention-Plugin in `buildSrc`) hängt eine Variante standardmäßig an
`runtime` und an **jede** Column unter `features/*`. Eine Variante lässt einzelne Columns über
`titanVariant { exclude("<name>") }` weg - z. B. in `apps/local/build.gradle.kts`, falls eine
Entwicklungsvariante künftig eine Column nicht mitbringen soll. `apps/local` lässt heute nur
`season` weg (kein Supervisor startet einen lokalen Server neu, s. "Saison-Welt" unten); sonst
enthalten beide Varianten dieselben Columns. Die Liste der Columns einer Variante steht damit an genau
einer Stelle (dem Verzeichnis-Scan in `settings.gradle.kts`), nicht pro Variante gepflegt.

`titan.app-variant` erzeugt außerdem:

- den Shadow-Jar `titan-<variantname>.jar` (z. B. `titan-cloudnet.jar`, `titan-local.jar`) mit
  `mergeServiceFiles()` und einer auf `META-INF/services/**` beschränkten
  `duplicatesStrategy = DuplicatesStrategy.INCLUDE` - ohne die zweite Einstellung wirft Shadows
  eigener Default (`DuplicatesStrategy.EXCLUDE`) jede doppelte `META-INF/services/...`-Datei schon
  *vor* dem Merge-Transformer weg, und nur eine Column würde im Jar landen (s. "Wie eine Column
  Plattform-Beans bekommt" unten);
- `META-INF/titan/variant.properties` mit dem Variantennamen und der Liste der erwarteten
  Avaje-Modulnamen (s. "Erwartete Columns einer Variante" unten);
- die zusammengeführte `application.yaml` sowie `application.example.yaml` in der Distribution
  (s. "Standardwerte je Column" unten);
- bei `titanVariant { aotCache.set(true) }` (nur `apps/cloudnet`) den AOT-Cache
  `titan-<variantname>.aot`.

### Permission-Plattform

`titanVariant { platform("<name>") }` hängt `:platform:<name>` an die Variante und ergänzt
`"<name>Platform"` in `expectedModules` - genau wie eine Column ihr eigenes `"<name>Column"`.
`apps/cloudnet/build.gradle.kts` setzt `platform("luckperms")` fest: die Produktionsvariante
startet nie ohne LuckPerms (fehlt `luckpermsPlatform` beim Start, bricht `VariantStartupCheck`
ab, s. "Erwartete Columns einer Variante" unten - die Prüfung gilt gleichermaßen für Columns und
Plattform-Module). `apps/local/build.gradle.kts` ruft `platform("luckperms")` nur auf, wenn die
Gradle-Property `titan.luckperms` gesetzt ist (`./gradlew :apps:local:build -Ptitan.luckperms`);
ohne die Property bleibt `apps/local` bei `runtime`s Fallback `DenyAllPermissionService` (aktiver
Dienst `deny-all`, Spieler ohne Rechte).

Bindet eine Variante `platform("luckperms")` ein, schließt `titan.app-variant` zusätzlich
`net.luckperms:minestom-loader` von der `testRuntimeClasspath` dieser Variante aus: Der Loader ist
ein JarInJar-Bootstrap mit einem eigenen, unrelocateten, veralteten Gson, das sonst Minestoms
Registry-Initialisierung in Tests bricht (derselbe Ausschluss wie in
`platform/luckperms/build.gradle.kts` für `platform/luckperms` selbst). Jeder Scope-bauende Test
in `apps/cloudnet` ersetzt `PermissionService` deshalb über Avajes Test-API
(`BeanScope.builder().forTesting().mock(PermissionService.class,
LuckPermsPermissionService.QUALIFIER)...`), statt echtes LuckPerms zu starten -
`LuckPermsPermissionService` trägt dafür ein explizites `@Named(LuckPermsPermissionService.QUALIFIER)`
(`QUALIFIER = "luckperms"`) statt sich auf Avajes aus dem Klassennamen abgeleiteten Qualifier zu
verlassen, und die generierte `isBeanAbsent(...)`-Prüfung, die ein gemocktes Bean von seiner
eigenen Konstruktion abhält, vergleicht genau diesen Namen. Der unbenannte
`mock(PermissionService.class)` ohne Namen verhindert die echte LuckPerms-Bean **nicht** - er
ersetzt nur, was ein Konsument injiziert bekommt, während `LuckPermsPermissionService`s eigenes
`@PostConstruct` trotzdem läuft und echtes LuckPerms startet. `apps/local`s eigener
`VariantStartTest` referenziert die Konstante nicht direkt: `platform/luckperms` liegt dort nur
mit `-Ptitan.luckperms` auf dem Klassenpfad, also bleibt der Name dort ein Literal, das mit
`QUALIFIER` übereinstimmen muss.

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

(`features/tickle/src/main/java/net/onelitefeather/titan/feature/tickle/TickleModule.java`)

- Der Avaje-Container ruft `@PostConstruct` beim Aufbau des `BeanScope` auf - also bevor ein
  Spieler die Lobby erreichen kann - und `@PreDestroy` beim Schließen des `BeanScope`
  (`Titan#initialize` schedult `beanScope::close` als Shutdown-Task). Es gibt keinen separaten
  Enable-/Disable-Schritt außerhalb des Bean-Lebenszyklus mehr.
- `FeatureNode.attach(parent, featureId, priority)` (in `core`) legt den Feature-eigenen
  Event-Node an, registriert ihn beim geteilten `titan`-Node und setzt seine Priorität. `on(type,
  listener)` registriert einen Listener, gekapselt in `ListenerGuard.guard(featureId, listener)`,
  damit ein Fehler dem Feature und - falls vorhanden - dem Spieler zugeordnet wird.
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

## Gefunden werden: `@Singleton` genügt

Es gibt keine zentrale Feature-Liste. `Titan` baut im Konstruktor `BeanScope.builder().build()` -
das allein reicht, damit jedes `@Singleton`-Feature aus jeder in die Variante eingebundenen Column
gebaut und über sein `@PostConstruct` gestartet wird. Fehlt `@Singleton` an einer Klasse mit
`@PostConstruct`, wird sie nie gebaut und ihr `start()` läuft nie -
`ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON`, angewendet über den
`ColumnArchitectureTest` jeder Column, lässt den Build in diesem Fall fehlschlagen.

**Die Reihenfolge, in der zwei Features dasselbe Event verarbeiten, legt `EVENT_PRIORITY` fest**,
nicht eine Startreihenfolge - Minestoms `EventNode#setPriority(int)` ordnet Geschwisterknoten. Die
heutigen Features, in Hunderterschritten mit Platz dazwischen:

| Feature | `EVENT_PRIORITY` |
|---|---|
| protection | 100 |
| spawn | 200 |
| respawn | 300 |
| navigator | 400 |
| sit | 500 |
| tickle | 600 |
| elytra | 700 |
| portal | 900 |
| jumprun | 1000 |

(`hotbar` und `admin` reagieren nicht über einen eigenen Feature-Node auf ein Event, das mit einem
anderen Feature kollidieren könnte, und tragen deshalb kein `EVENT_PRIORITY`.)

Ein neues Feature wählt eine freie Zahl aus der Lücke. Eindeutigkeit ist **kein Build-Test**,
sondern eine Startlaufzeit-Prüfung: `FeatureNode.attach(parent, featureId, priority)` wirft eine
`IllegalStateException`, sobald `parent` bereits ein Kind mit derselben `priority` hat, und nennt
darin beide Feature-Ids sowie die kollidierende Position. Diese Prüfung ersetzt eine frühere
Bytecode-Prüfung über ein einziges Modul - die geht nicht mehr, weil seit der Column-Architektur
keine Column mehr alle anderen Columns kennt (jede Column sieht beim Kompilieren nur `core`).

**Fehlt eine Konstruktor-Abhängigkeit ganz** (kein passender `@Bean`/`@Singleton` im Scope), bricht
`BeanScope.builder().build()` mit einer Exception ab, die den fehlenden Typ nennt - noch bevor ein
Spieler verbinden kann. `TitanApplication.main` fängt jede `RuntimeException`/`Error` aus
`new Titan()`/`titan.initialize()` ab, loggt sie als `Titan failed to start: …` und beendet den
Prozess mit Exit-Code 1.

Sind alle Features gestartet, loggt `Titan`s Konstruktor einmal die tatsächliche
Startreihenfolge auf INFO-Level: `Lobby features started in event order: {}` (s.
`runtime/src/main/java/net/onelitefeather/titan/runtime/bootstrap/FeatureStartupLog.java`). Die
Liste kommt nicht aus einer gepflegten Feature-Liste, sondern aus den Kindknoten des
`titan`-Knotens selbst, aufsteigend nach `EventNode#getPriority()` sortiert - dieselbe Reihenfolge
wie die Tabelle oben. Direkt davor prüft `VariantStartupCheck` (s. "Erwartete Columns einer
Variante" unten), dass alle für diese Variante erwarteten Columns tatsächlich geladen wurden.

Welche Plattform-Dienste als Bean zur Verfügung stehen, steht in `runtime`s eigener
`package-info.java` (`@InjectModule(provides = {...})`): `InstanceContainer`, `MapProvider` (aus
`common`), `LobbySpawn`, `Deliver`, der `@Named("titan")`-qualifizierte `EventNode<Event>`,
`FeatureFlags`, `Clock`, `Scheduler`, `CommandManager`. `LobbyItems` ist selbst eine
`@Singleton`-Bean, implementiert in der Column `features/hotbar` (s. unten). Braucht ein neues
Feature einen **neuen** geteilten Dienst, der im Kern ein Plattformtyp aus `common` oder Minestom
ist, kommt eine weitere `@Bean`-Methode in `runtime`s Plattform-Factory dazu; trägt er selbst
feature-übergreifende Logik, wird er eine eigene `@Singleton`-Klasse, die betroffene Features per
Konstruktor anfordern.

## Items als Beans: `LobbyItem`, `@Factory`, `LobbyItems`

Ein Hotbar- oder Ausrüstungsitem meldet ein Feature nicht über einen Kontext an, sondern stellt es
als `@Bean LobbyItem` in einer eigenen, paketprivaten `@Factory`-Klasse bereit:

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

(`features/navigator/src/main/java/net/onelitefeather/titan/feature/navigator/NavigatorItems.java`)

Die `LobbyItems`-Implementierung (Column `features/hotbar`,
`net.onelitefeather.titan.feature.hotbar.HotbarLobbyItems`, gegen die Schnittstelle
`net.onelitefeather.titan.core.module.item.LobbyItems` aus `core`) bekommt jedes `LobbyItem` per
Listen-Injektion (`List<LobbyItem>`), stempelt es mit dem Identitäts-Tag `LobbyItems.IDENTITY_TAG`
und dispatcht ein einziges `PlayerUseItemEvent` am `titan`-Node an den passenden `onUse`-Handler -
ein Feature braucht dafür keinen eigenen Listener. Zwei Items mit demselben `key()` oder demselben
festen `ItemSlot` (`hotbar(0..8)` oder `equipment(EquipmentSlot)`) lassen den
`HotbarLobbyItems`-Konstruktor mit `IllegalStateException` abbrechen, die beide Items nennt;
`ItemSlot.unplaced()` (z. B. das Elytra-Feuerwerk, das nur während des Fliegens in der Nebenhand
liegt) ist davon ausgenommen. `LobbyItems#equip(player)` räumt das Inventar und setzt alle Items
mit festem Platz - das rufen Spawn- und Respawn-Feature auf, nicht jedes Feature selbst;
`LobbyItems#stack(key)` gibt den gestempelten Stack für ein unplatziertes Item heraus, das ein
Feature selbst aushändigt (`ElytraModule`s Feuerwerk).

Der Navigator ist der einzige Sonderfall ohne Andockpunkt: Seine Ziele stehen fest im
package-privaten `enum Destination` (s. `openspec/changes/navigator-entries-in-code/design.md`).
Nur `Destination.SLENDER` bleibt hinter der Feature-Flag `NAVIGATOR_SLENDER` versteckt, ausgewertet
über `FeatureFlags`, dem `NavigatorModule` per Konstruktor übergeben.

Auf Platz 2 beider Inventare liegt der feste Eintrag „Spawn“ (Kompass), kein `Destination`: Der
Klick schickt den Spieler über `SpawnReturn#sendToSpawnAndTell` zurück an den Spawn und leitet nicht
weiter. Das Modul holt `SpawnReturn` über einen `Provider`, damit zwischen den Columns `spawn` und
`navigator` kein Zyklus entsteht.

Der Navigator besitzt zwei geteilte Aves-Inventare: das öffentliche (unverändert) und das
Team-Inventar (öffentlich plus `Destination.BUILD`). Das Recht `titan.navigator.buildserver`
entscheidet beim Öffnen, welches sich öffnet: `PermissionService#check` liefert `ALLOWED` ->
Team-Inventar, `NOT_SET` und `DENIED` -> öffentliches. `BUILD` liegt auf Platz 7 und leitet an den
CloudNet-Task `Build` weiter (getrennt von `MemberBuild` des Creative-Ziels). Beim Klick wird das
Recht erneut geprüft; ist es inzwischen weg, gibt es keine Weiterleitung, das Inventar schließt.

Betrieb: Der CloudNet-Task `Build` muss existieren, und das LuckPerms-Recht
`titan.navigator.buildserver` wird der Team-Gruppe erteilt. In der lokalen Variante gilt
`deny-all` (`DenyAllPermissionService`), dort sieht niemand das Ziel.

## Tasks über den injizierten `Scheduler`

Ein wiederkehrender Task wird direkt über den injizierten Minestom-`Scheduler` geplant, nicht über
einen eigenen Andockpunkt:

```java
this.task = this.scheduler.scheduleTask(this.boosts::advance, TaskSchedule.tick(1), TaskSchedule.tick(1));
```

(`features/elytra/src/main/java/net/onelitefeather/titan/feature/elytra/ElytraModule.java`,
`start()`). `stop()` bricht ihn **nach** `node.close()` ab (s. oben, "Aufbau eines Features") -
erst der Event-Node weg, dann der Task.

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
- **Standardwerte gehören in die Column-eigene `titan/defaults/<column>.yaml`** (s. "Standardwerte
  je Column" unten), nicht in den Code - ein Feature liest ohne eigenen Fallback (`Config.get(key)`,
  nicht `Config.get(key, "…")`). Fehlt ein Schlüssel, bricht der Start mit `Missing required
  configuration parameter [key]` ab.

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

## Standardwerte je Column

Jede konfigurierbare Column liefert ihre eigenen Standardwerte als
`src/main/resources/titan/defaults/<column>.yaml`, kommentiert, mit genau ihrem Abschnitt (z. B.
`features/spawn/src/main/resources/titan/defaults/spawn.yaml` mit dem Abschnitt `spawn`;
`navigator` liefert den Abschnitt `features` mit allen `NAVIGATOR_*`-Flags). `runtime` liefert
`runtime/src/main/resources/titan/defaults/runtime.yaml` (`config.watch.*`). Eine Column ohne
eigene Konfiguration (`protection`, `respawn`, `hotbar`, `admin`) liefert keine Default-Datei.

Beim Bauen einer App-Variante verkettet `titan.app-variant`s `MergeApplicationDefaultsTask` alle
`titan/defaults/*.yaml`-Dateien von `runtime` und jeder in die Variante eingebundenen Column
(sortiert nach Dateiname) zu einer klassenpfad-`application.yaml` - das ist derselbe Mechanismus,
der auch die per-Column `application-test.yaml` einer einzelnen Column für ihre eigenen Tests
erzeugt (`titan.column`s `mergeTestDefaults`-Task). Die Dateien werden **verkettet, nicht tief
zusammengeführt** - Kommentare für Betreiber bleiben dadurch erhalten. Davor prüft
`DefaultsMerger`/`DefaultsMergerTask` (SnakeYAML mit `allowDuplicateKeys=false`, flach gemacht auf
Punkt-Schlüssel), ob zwei Dateien denselben Schlüssel setzen - dann schlägt der Build fehl, mit
einer Meldung, die beide Dateien und den Schlüssel nennt (`DefaultsConflictException`). Ein
Top-Level-Abschnitt (z. B. `features`) darf deshalb nur in einer Datei einer Variante stehen.

Die zusammengeführte Datei wird zusätzlich, unverändert, als `application.example.yaml` in die
Distribution kopiert, damit ein Betreiber eine kommentierte Kopie neben dem Jar hat; geladen wird
sie nicht - die shipped Defaults kommen aus der klassenpfad-`application.yaml` im Jar selbst. Die
Rangfolge aus `lobby-module-config` (Shipped-Default < `application.yaml` < Profil < externe Datei
< Umgebungsvariable < System-Property) bleibt dadurch unverändert gültig.

## Saison-Welt (`season`, nur `cloudnet`)

Die Column `season` lässt die Lobby in einem Zeitfenster in einer eigenen Welt laufen. Sie steckt
nur in `apps/cloudnet`, weil der Wechsel ein Neustart ist und nur ein Supervisor (CloudNet) den
Dienst danach wieder startet. Die Weltwahl beim Start läuft über `LobbyWorldChoice` (`core`), die
`PlatformBeans` per `ServiceLoader` findet; `season` trägt sich dafür unter
`META-INF/services/net.onelitefeather.titan.core.module.LobbyWorldChoice` ein.

Eine Saisonwelt anlegen:

1. Die Welt im Setup-Server bauen und speichern.
2. `worlds/<name>/` mit der `map.json` neben `worlds/world/` ablegen.
3. In `application.yaml` eintragen:

   ```yaml
   seasons:
     zone: Europe/Berlin           # Standard; Zeitzone der Fenster
     winter:                       # beliebige Id ("zone" ist reserviert)
       world: winter               # Verzeichnis unter worlds/
       from: "2026-12-01T00:00:00" # inklusiv, lokale Zeit in seasons.zone
       to: "2027-01-07T00:00:00"   # exklusiv
       enabled: true               # Abschalter, wirkt ohne Neustart der Konfiguration
   ```

Datum-Zeit-Werte müssen in Anführungszeichen stehen (`from: "2026-12-01T00:00:00"`); der
YAML-Lader verwirft einen unquotierten Wert, er wird als fehlend gemeldet.

Ein ungültiger, aktivierter Eintrag (fehlender Schlüssel, Datum, Welt oder `map.json`) bricht den
Start mit dem Schlüssel und dem Grund ab. Der Wechsel selbst braucht einen Neustart: Die Column
prüft jede Minute und nach jedem Verlassen, ob die gestartete von der gewünschten Welt abweicht,
und stoppt die Lobby erst, wenn kein Spieler mehr online ist. Ein belegter Dienst kann daher
länger warten; ein Betreiber kann ihn mit `/stop` sofort beenden.

## Portale (`portal`)

Portale sind Kartendaten: Die Liste `portals` steht in der `map.json` der Welt (auch der einer
Saisonwelt), auf oberster Ebene neben `spawn`. Die Column `portal` schickt Spieler, die eine Form
betreten, an einen CloudNet-Task.

```json
"portals": [
  {"id": "survival", "task": "Survival", "shape": {"type": "box", "min": {"x": 10, "y": 64, "z": 10}, "max": {"x": 14, "y": 68, "z": 11}}},
  {"id": "elytra-ring", "task": "ElytraRace", "permission": null, "shape": {"type": "disc", "center": {"x": 0.5, "y": 72, "z": 40.5}, "radius": 5.5, "normal": {"x": 0, "y": 0, "z": 1}}}
]
```

- `id` (eindeutig je Welt), `task` (Name des CloudNet-Tasks) und `shape` sind Pflicht;
  `permission` ist optional, fehlend oder `null` heißt "für alle".
- `box`: `min` und `max` sind Blockkoordinaten, beide einschließlich; die Box deckt `[min, max + 1]`
  je Achse ab.
- `disc`: `center`, `radius` (Rand einschließlich) und `normal` (Richtung der Ebene, beliebig
  ausgerichtet, muss kein Einheitsvektor sein).
- Ausgelöst wird beim Gehen und beim Fliegen: Die Bewegung zählt als Strecke, ein schneller
  Elytra-Schritt durch eine dünne Scheibe löst also aus. Es entscheidet der Fußpunkt.
- Ein Portal löst einmal je Betreten aus; wer drinnen bleibt, löst nicht erneut aus. Nach einer
  Weiterleitung gilt 3 s Abklingzeit.
- Mit `permission` schickt das Portal nur Spieler mit diesem Recht weiter; ohne Recht passiert
  nichts und keine Abklingzeit läuft.
- Ein ungültiges Portal (leere `id` oder `task`, doppelte `id`, `radius <= 0`, `min > max`) bricht
  den Start mit Welt, Id und Grund ab.

Portale ändern: Task in CloudNet anlegen, `map.json` bearbeiten, die Lobby neu starten. Die Liste
wird nur beim Start gelesen. Im Setup-Server bearbeitet `/setup portal` die Portale, siehe
[README](../README.md#portals).

## Erwartete Columns einer Variante

`titan.app-variant` schreibt beim Bauen die Avaje-Modulnamen aller in eine Variante eingebundenen
Columns (`<name>Column`) und Permission-Plattformen (`<name>Platform`, s. "Permission-Plattform"
oben) in die Ressource `META-INF/titan/variant.properties` (`name=<variantname>`,
`modules=<kommagetrennte Liste>`). `Titan`s Konstruktor ruft direkt nach dem Aufbau des
`BeanScope` `net.onelitefeather.titan.runtime.variant.VariantStartupCheck.verify(...)` auf: Sie
liest `variant.properties` über den Classloader, vergleicht die erwartete Liste mit den
tatsächlich geladenen Avaje-Modulen (`LoadedModules.discover`) und wirft eine
`IllegalStateException` mit den fehlenden Modulnamen, falls eine erwartete Column oder
Plattform nicht geladen wurde - so startet `apps/cloudnet` ohne `platform/luckperms` nicht.
`TitanApplication.main` fängt diese Exception wie jeden anderen Startfehler ab und beendet
den Prozess. Sind alle Module geladen, loggt die Prüfung einmal auf INFO:

```
Variant {} started with modules {}
```

Diese Zeile kommt **vor** `Lobby features started in event order: {}` und ersetzt keine bestehende
Log-Zeile. Ein Modul, das `mergeServiceFiles()` beim Shaden verliert (s. "Wie eine Column
Plattform-Beans bekommt" unten), führt so zu einem klaren Startabbruch statt zu einer Lobby, die
still ohne ein Feature läuft.

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

`net.onelitefeather.titan.core.testfixtures.TestTitanNode` (aus `core`s `testFixtures`, s.
`ProtectionModuleTest`, `SpawnModuleTest`, `TickleModuleTest`, `ElytraFixture`,
`NavigatorFixture`) hängt einen frisch benannten Node unter `env.process().eventHandler()` und
hebt ihn beim `close()` wieder ab - genau das, was `runtime` in Produktion tut, ohne den
`BeanScope`. Ein Feature mit eigenen Items baut daneben eine eigene `LobbyItems`-Instanz aus den
Items, die dessen `@Factory`-Klasse liefert (s. `SpawnModuleTest`, `ElytraFixture`). Ein Test, der
prüft, dass nach `stop()` keines der Events mehr Code des Features auslöst, ist Pflicht für jedes
Feature (s. `SpawnModuleTest#stopLeavesNoListenerBehind` u. Ä.).

### Oben: der echte `BeanScope`

Ein Test, der die reale Verdrahtung mehrerer Features zusammen prüft (z. B. dass ein
Navigator-Klick trotz `ProtectionModule`s Abbruch weiterleitet), baut den echten `BeanScope` -
`apps/cloudnet/src/test/java/net/onelitefeather/titan/runtime/bootstrap/WiringTest.java`,
`NavigatorProtectionOrderingTest`, `StandardLoadoutTest` (alle Querschnitts-Tests einer Variante,
nicht in einem einzelnen Feature-Paket):

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
Jedes andere Bean (alle Features, `LobbyItems`, `Deliver`, `Clock`, `Scheduler`) wird exakt so
gebaut wie in Produktion. `.bean(Type, instance)` liefert statt eines Mocks eine echte Testinstanz
(z. B. einen aufzeichnenden `Deliver`).

Item-Dispatch wird über ein direkt gefeuertes `PlayerUseItemEvent` getestet, Chat-Ausgaben über
`TestConnection#trackIncoming(SystemChatPacket.class)`. `Collector#collect()` (und die
`assertSingle()`/`assertEmpty()`-Kurzformen) **entnimmt** den Tracker aus der Verbindung - für
einen Test mit mehreren Aktionen deshalb erst alle Events feuern und danach genau einmal
`collect()` aufrufen.

## Architekturregeln

Ein Teil der früheren ArchUnit-Regeln ist mit der Column-Architektur überflüssig geworden, weil
die Modulstruktur sie schon beim Kompilieren erzwingt: "Features hängen nicht voneinander ab" und
"Plattform hängt nicht an Features" gelten automatisch, weil eine Column nur `core` als
Abhängigkeit deklariert und deshalb keinen Code einer anderen Column oder von `runtime` überhaupt
sehen kann. Die Regel "nur `*Module` ist in einer Column public" ist ebenfalls entfallen - eine
fremde Column sieht ohnehin nichts, und `runtime` findet eine Column nur über Avajes eigene
Modul-Discovery, nicht über einen öffentlichen Typ.

Die verbleibenden, geteilten Regeln laufen als `ColumnArchitectureRules`
(`core`s `testFixtures`, `net.onelitefeather.titan.core.testfixtures.architecture`) - jede Column
hat einen kleinen `ColumnArchitectureTest`, der sie per `@ArchTest` auf ihr eigenes Paket anwendet:

```java
@AnalyzeClasses(packages = "net.onelitefeather.titan.feature.protection", importOptions = ImportOption.DoNotIncludeTests.class)
class ColumnArchitectureTest {
    @ArchTest
    static final ArchRule featuresRegisterListenersOnlyThroughFeatureNode = ColumnArchitectureRules.FEATURES_REGISTER_LISTENERS_ONLY_THROUGH_FEATURE_NODE;
    @ArchTest
    static final ArchRule classesWithPostConstructAreSingleton = ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON;
    @ArchTest
    static final ArchRule featureModulesDoNotUseBeanScope = ColumnArchitectureRules.FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE;
}
```

Die drei geteilten Regeln:

1. Eine Klasse ruft `EventNode#addListener`/`#addChild` oder
   `MinecraftServer#getGlobalEventHandler()` nie direkt auf - nur über `FeatureNode`.
2. Jede Klasse mit einer `@PostConstruct`-Methode trägt `@Singleton`.
3. Kein Feature-Code hängt von `io.avaje.inject.BeanScope` ab - Abhängigkeiten kommen
   ausschließlich über den Konstruktor.

`features/navigator`s `ColumnArchitectureTest` fügt eine eigene, vierte Regel hinzu:
`..feature.navigator..` hängt nicht von `io.avaje.config..` ab, weil die Ziele des Navigators fest
im Code stehen, nicht aus Konfiguration gelesen werden.

Die Eindeutigkeit von `EVENT_PRIORITY` ist keine ArchUnit-Regel, sondern eine
Startlaufzeit-Prüfung in `FeatureNode.attach` (s. oben, "Gefunden werden: `@Singleton` genügt").

## Wie eine Column Plattform-Beans bekommt

Eine Column hängt nur an `core`, nie an `runtime`. Beans wie den geteilten
`@Named("titan") EventNode<Event>` sieht sie beim Kompilieren also nicht - ein Fall, den der Spike
an `features/protection` geklärt hat (design.md, Entscheidung D2). Die drei Spike-Fragen und ihre
Antworten:

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
   Spiegelbildlich braucht `runtime` nur die einfache Form -
   `@InjectModule(provides = {EventNode.class})` -, weil es die Bean selbst definiert und keine
   eigene Übersetzungsprüfung dafür braucht. `provides` **und** `providesString` zusammen auf
   diesem (unbenannten) Standard-Scope-Modul auszuprobieren, hat in avaje-inject-generator 12.7
   einen Codegen-Fehler ausgelöst: Im generierten Modul fehlte das Komma zwischen den beiden
   Attributen, ein nicht mehr übersetzbares `@InjectModule(provides = {...}providesString =
   {...})`. Einen entsprechenden Fehlerbericht an avaje-inject sollte ein Folge-Change einreichen.

2. **Wie liest die Startprüfung (s. "Erwartete Columns einer Variante" oben) den Modulnamen, ohne
   dass er mit der Klasse `ProtectionModule` kollidiert?** Über
   `@InjectModule(name = "protectionColumn", ...)` - ein expliziter, von der Bean-Klasse
   verschiedener Name. Der Annotationsprozessor generiert daraus die Modulklasse
   `ProtectionColumnModule` (Name plus `Module`-Suffix), registriert unter
   `META-INF/services/io.avaje.inject.spi.InjectExtension`. Die Startprüfung liest also nicht "gibt
   es eine Bean `ProtectionModule`", sondern vergleicht die erwartete Liste von Column-Namen
   (`"protectionColumn"`, ...) gegen das, was beim Aufbau des `BeanScope` tatsächlich geladen
   wurde.

3. **Behält `mergeServiceFiles()` alle Avaje-Module im Shadow-Jar?** Nicht ohne Weiteres. Jede
   Column liefert ihre eigene `META-INF/services/io.avaje.inject.spi.InjectExtension`-Datei; die
   `ShadowJar`-Aufgabe hat als eigene, von `mergeServiceFiles()` unabhängige Voreinstellung
   `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` - und die wirft jede doppelte Ressource schon
   *vor* dem Merge-Transformer weg, sodass am Ende nur ein Modul übrigbleibt. Die Behebung setzt
   `duplicatesStrategy = DuplicatesStrategy.INCLUDE` nicht für den ganzen Task, sondern gezielt über
   `filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }`
   zusätzlich zu `mergeServiceFiles()`, in `titan.app-variant` (`buildSrc/src/main/kotlin/titan
   .app-variant.gradle.kts`): erst dann landen alle Modulnamen in derselben Datei, ohne dass andere
   `META-INF`-Dateien (LICENSE, NOTICE, ...) plötzlich doppelt im Jar landen.

### Das Muster für neue Columns

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
- `runtime` braucht die spiegelbildliche `provides`-Deklaration nur als einfache `Class<?>`-Form,
  niemals zusammen mit der `providesString`-Form auf demselben (Standard-Scope-)Modul (siehe Frage
  1 oben).
- `titan.app-variant`s `shadowJar` braucht die auf `META-INF/services/**` beschränkte
  `duplicatesStrategy = DuplicatesStrategy.INCLUDE` neben `mergeServiceFiles()` - sonst
  verschwindet die neue Column beim Shaden stillschweigend (siehe Frage 3 oben). Das ist bereits im
  Convention-Plugin gesetzt; eine neue Column muss daran nichts ändern.
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
`package-info.java` abgelesen (`runtime`s eigene `package-info.java` deklariert die Plattform-Typen
als `provides`):

| Column | `requires` | `requiresString` | `provides` |
|---|---|---|---|
| `protection` | `EventNode.class` | `EventNode<Event>:titan` | - |
| `admin` | `CommandManager.class` | - | - |
| `spawn` | `Instance.class`, `LobbySpawn.class`, `EventNode.class`, `LobbyItems.class` | `EventNode<Event>:titan` | - |
| `respawn` | `EventNode.class`, `LobbyItems.class` | `EventNode<Event>:titan` | - |
| `navigator` | `EventNode.class`, `Deliver.class`, `FeatureFlags.class`, `PermissionService.class` | `EventNode<Event>:titan` | `LobbyItem.class` |
| `sit` | `EventNode.class` | `EventNode<Event>:titan` | - |
| `tickle` | `EventNode.class`, `Clock.class` | `EventNode<Event>:titan` | - |
| `elytra` | `EventNode.class`, `Scheduler.class` | `EventNode<Event>:titan` | `LobbyItem.class` |
| `jumprun` | `EventNode.class`, `LobbySpawn.class`, `LobbyItems.class` | `EventNode<Event>:titan` | `LobbyItem.class` |
| `hotbar` | `EventNode.class` | `EventNode<Event>:titan` | `LobbyItems.class` |
| `portal` | `EventNode.class`, `LobbyPortals.class`, `Deliver.class`, `PermissionService.class`, `Clock.class` | `EventNode<Event>:titan` | - |
| `season` (nur `cloudnet`) | `Scheduler.class`, `Clock.class`, `EventNode.class` | `EventNode<Event>:titan` | - |

`EventNode` ist in jeder Zeile der einzige `requiresString`-Eintrag, weil es der einzige
qualifizierte, generische Plattform-Typ ist (Frage 1 oben); die anderen Typen sind weder generisch
noch `@Named`, für sie reicht die `requires`-Form allein. `LobbyItems` steht bei `spawn` und
`respawn`, weil ihre Module `LobbyItems` direkt injizieren; `elytra` injiziert es stattdessen als
`Provider<LobbyItems>` (siehe oben) und lässt es deshalb aus `requires` weg, obwohl es
`LobbyItem` liefert.

## spawn

Die Column `spawn` stellt `/spawn` bereit, den Weg zurück zum Lobby-Spawn: für alle Spieler, ohne
Recht, sofort (kein Countdown). Der Befehl beendet auch einen Gleitflug und bestätigt in der
Sprache des Spielers (`titan/spawn/messages_de|en.properties`). Hat die aktive Karte keinen
Spawn-Punkt, gibt es eine Meldung und keinen Teleport. Die Konsole bekommt einen Hinweis für
Operatoren, dass der Befehl nur für Spieler gilt. `/lobby` und `/hub` sind bewusst keine Aliase: Der
Proxy belegt sie für den Wechsel in die Lobby.

Command und Navigator-Eintrag (Platz 2) nutzen dieselbe `core`-Schnittstelle `SpawnReturn`
(`sendToSpawn` ohne, `sendToSpawnAndTell` mit Rückmeldung); implementiert ist sie von
`LobbySpawnReturn`.

### Andockpunkt: `LobbyReturnToSpawnEvent`

Vor dem Teleport feuert `SpawnReturn` synchron `LobbyReturnToSpawnEvent(player)` (`core`), nur wenn
die Karte einen Spawn-Punkt hat. Features mit Spielerzustand räumen dort auf: Sie hängen einen
Listener an den Titan-Event-Node, der beim Teleport schon fertig ist. Die Listener laufen auf dem
Thread des Aufrufers, müssen schnell sein und sollen nicht werfen: Minestom gibt eine Exception an seinen Exception-Handler weiter, sie verzögert und verrauscht die Rückkehr nur.
Beispiel: `jumprun` beendet einen laufenden Lauf mit `EndReason.SPAWN_RETURN`, der Score zählt.

## jumprun

Ein zufälliges Einzelspieler-Jump-and-Run: Das Item `titan:jumprun` (Hasenpfote „Jump & Run“) in
Hotbar-Slot 0 startet einen Lauf, erneutes Benutzen beendet ihn. Die Plattformen sind reine
Client-Blöcke (`BlockChangePacket`, nur der Spieler sieht sie, die Welt bleibt unverändert); es
existieren immer 2 Blöcke hinter und 2 vor dem Spieler, und die Schwierigkeit (Lücke,
Höhenunterschied, Blockart) steigt mit dem Punktestand. `EVENT_PRIORITY` ist 1000.

- **Modi:** Rechtsklick mit dem Item bei gedrückter Schleichtaste wechselt außerhalb eines Laufs
  zyklisch Easy → Medium → Hard → Rainbow → Ultra (Standard Medium; im Lauf passiert nichts). Der Modus steuert, ab
  welchem Punktestand die Formen erscheinen, wie steil die Schwierigkeit steigt und wie weit die
  Lücken sind (`Mode`). Er gilt pro Spieler und Sitzung und steht im Kopf-Label und in den
  Meldungen.
- **Rainbow und Ultra:** Rainbow spielt wie Medium, Ultra wie Hard (ohne Umrandung). Steht der
  Läufer so viele Ticks auf dem aktuellen Block, wie der Modus vorgibt (`jumprun.rainbow.rerollTicks`,
  Standard 10; `jumprun.ultra.rerollTicks`, Standard 40; jeweils eine ganze Zahl über 0),
  wechseln in Rainbow alle sichtbaren Blöcke ihr Material, in Ultra werden die Blöcke voraus neu
  erzeugt (Aufstieg und Fall wie sonst). Ein Sprung oder eine Landung setzt den Zähler zurück. Pro
  Lauf läuft dafür ein Tick-Task auf dem Scheduler des Läufers, der mit dem Lauf endet
  (`Reroller`). Beide Werte werden wie die Paletten beim Start geprüft (Meldung mit dem
  vollen Schlüssel) und zur Laufzeit je Lauf neu gelesen; der Modus des Laufs wählt den Schlüssel.
- **Töne:** Alle Lauftöne (Punkt, Aufstiegssignal, Absturz, Rekord, Modus-Klick) spielen am Läufer
  gebunden (`Sound.Emitter.self()`, `EntitySoundEffectPacket`) und nur für ihn. Das Level-up beim
  ersten Rekord bleibt so hörbar, obwohl der Absturz im selben Tick zum Startpunkt teleportiert.
- **Rekorde:** Mit konfigurierter Datenbank (siehe [Datenbank](#datenbank-persistence)) bleiben die
  Bestwerte je Modus über Neustarts erhalten: jeder beendete Lauf wird als Zeile in `jumprun_run`
  angehängt (auf einem virtuellen Thread, nie auf dem Tick-Thread), die Bestwerte eines Spielers
  werden beim Beitritt geladen (`StoredRunRecords`). Ohne Datenbank liegen sie je Modus nur im
  Speicher (`InMemoryRunRecords`) und gehen beim Neustart verloren.
- **Sidebar:** Nur während eines Laufs sieht der Läufer eine Sidebar, und nur für den Modus des
  Laufs: Score, Rekord und (mit Datenbank) die Top drei des Modus (`Leaderboard`). Die eigene Zeile
  in den Top drei ist fett, die Beschriftungen (`titan.jumprun.sidebar.*`) werden pro Spieler
  übersetzt. Ohne Datenbank entfallen die Top-drei-Zeilen. Die Actionbar bleibt unverändert. Die Top drei
  werden alle 30 Sekunden aus der Datenbank neu gelesen (nicht auf dem Tick-Thread); ein neuer Rekord
  steht sofort darin, bevor die Datenbank ihn hat. Die Sidebar endet mit dem Lauf.
- **Höhenband:** Ein Lauf bleibt innerhalb von `spawn.minHeight`/`spawn.maxHeight` (Standard
  −64/310), damit ein Absturz immer am Startpunkt des Laufs endet und nie den Spawn-Teleport der
  Spawn-Column auslöst. jumprun liest die Grenzen über die Bean `LobbyHeightBounds` der Spawn-Column
  (bei jeder Prüfung frisch), die Column wird also vorausgesetzt; ohne sie bricht der Start ab.
  Nach unten muss die Blockoberkante mehr als 3 + 5 Blöcke über `minHeight` liegen (3 Blöcke
  Absturzschwelle, 5 Blöcke Fallstrecke eines Ticks als Puffer); nach oben muss die Oberkante plus
  Sprunghöhe (≈ 1,25) plus 1 Block höchstens `maxHeight` sein.
- **Sprungfreiheit:** Über jedem Block ist so viel Luft, dass man dort springen kann: Körperhöhe
  1,8 plus Sprunghöhe ≈ 1,25. Die Flugbahn zwischen zwei Blöcken ist bis zum Scheitel frei, auch in
  der Nähe von Portalen. Blöcke, die diese Freiheit verletzen, werden nicht erzeugt.
- **Enges Band:** Liegen `spawn.minHeight` und `spawn.maxHeight` so dicht beieinander, dass kein Block
  die beiden Abstände einhält, startet kein Lauf, und der Spieler sieht die Meldung „kein Platz“
  (`titan.jumprun.start.no_space`).
- **Eigene Übersetzungen:** Texte (`titan.jumprun.*`, de/en, Englisch als Fallback) kommen aus einem
  eigenen Bundle und werden pro Spieler gerendert. Minestoms globales Flag für automatische
  Übersetzung bleibt aus.
- **Materialien konfigurieren:** `jumprun.palettes.<form>.<block>: <gewicht>` mit den Formen `full`,
  `trapdoor`, `slab`, `fence`, `pane`, `post`; Schlüssel ist der Blockname ohne Namensraum, das
  Gewicht eine ganze Zahl ab 0 (3 wird dreimal so oft gezogen wie 1; 0 schaltet das Material ab,
  so lässt sich ein mitgeliefertes per Override in `application.yaml` entfernen). Die Standardwerte stehen in
  `features/jumprun/src/main/resources/titan/defaults/jumprun.yaml`. Die Form setzt die Zustände
  (untere Hälfte, geschlossen, senkrecht), der Betreiber wählt nur Block und Gewicht. Ein weiterer
  Eintrag in `application.yaml` ergänzt die Standardliste, er ersetzt sie nicht.
- **Prüfung:** `JumprunSettings` bricht den Start ab, wenn ein Block unbekannt ist, seine
  Kollisionsoberkante nicht zur Form passt (Zustände der Form angewendet), das Gewicht keine
  ganze Zahl ab 0 ist oder eine Form keinen Eintrag mit Gewicht über 0 hat. Abgeschaltete Einträge
  (Gewicht 0) werden trotzdem geprüft, damit ein Tippfehler im Namen auffällt. Die Meldung nennt
  `jumprun.palettes.<form>.<block>` (bei leerer Form oder nur Gewicht 0 `jumprun.palettes.<form>`) und den Grund.
- **Zur Laufzeit:** Die Paletten werden beim Start jedes Laufs neu gelesen. Ein ungültiger Wert
  erzeugt eine WARN-Zeile mit dem Schlüssel (einmal je Fehler), und es bleiben die zuletzt
  gültigen Paletten.

## Datenbank (`persistence`)

Das Modul `persistence` stellt eine gemeinsame PostgreSQL-Anbindung bereit (HikariCP, Hibernate,
Flyway). Sie ist aus, solange `titan.database.url` nicht gesetzt ist; dann läuft die Lobby wie
zuvor ohne Datenbank.

| Schlüssel | Umgebungsvariable |
| --- | --- |
| `titan.database.url` (`jdbc:postgresql://host:5432/db`) | `TITAN_DATABASE_URL` |
| `titan.database.user` | `TITAN_DATABASE_USER` |
| `titan.database.password` | `TITAN_DATABASE_PASSWORD` |

Rangfolge wie bei jedem Schlüssel (niedrig nach hoch): Datei im Jar < Profil < `CONFIG_FILE` <
Umgebungsvariable < Systemproperty. Die drei Schlüssel stehen bewusst nicht in den Standardwerten
(`persistence/src/main/resources/titan/defaults/database.yaml`).

- **Start:** Eine leere URL bricht den Start ab (zum Abschalten den Schlüssel entfernen). Ist die
  Datenbank nicht erreichbar, bricht der Start nach etwa 5 s ab, ebenso bei fehlgeschlagener
  Migration oder wenn das Schema nicht zu den Mappings passt (`validate`).
- **Migrationssperre:** Gleichzeitig startende Lobbys werden durch eine PostgreSQL-Advisory-Lock der
  Sitzung (`pg_advisory_lock`) um alle Flyway-Migrationen herum nacheinander abgearbeitet; Flyways
  eigene Sperre deckt seine Vorprüfungen nicht ab. `titan.database.hikari.maximumPoolSize` muss
  mindestens 3 sein (Flyway braucht zwei Verbindungen, die Startsperre eine; Standard 4), sonst bricht
  der Start mit einer Meldung ab. `titan.database.migrationLockTimeoutSeconds` (Standard 60) begrenzt
  das Warten: Hält eine andere Lobby die Sperre länger, bricht der Start mit klarer Meldung ab.
  PostgreSQL ist Voraussetzung; hinter PgBouncer im Transaction-Pooling-Modus funktioniert die Sperre
  nicht.
- **Laufzeit:** Fällt die Datenbank später aus, bleibt die Lobby spielbar; Lesen und Schreiben
  melden dann eine WARN-Zeile, die Bestwerte bleiben im Speicher.
- **Hikari:** `titan.database.hikari.*` nimmt jede HikariCP-Eigenschaft unter ihrem
  `HikariConfig`-Namen, `dataSource.*` geht an den JDBC-Treiber. Ein unbekannter oder ungültiger
  Schlüssel bricht den Start ab; die Meldung nennt den Schlüssel, nie den Wert. `dataSourceClassName`,
  `dataSourceJNDI` und `dataSource` sind gesperrt, die URL kommt immer aus `titan.database.url`;
  `titan.database.user`/`password` gewinnen gegen `username`/`password` unter `hikari`.
- **Hibernate:** `titan.database.hibernate.*` nimmt jede Hibernate-Eigenschaft ohne das Präfix
  `hibernate.` (`jakarta.*`-Schlüssel unverändert). Gesperrt, weil Flyway das Schema besitzt:
  `hbm2ddl.auto` darf nur `validate` oder `none` sein (die Lobby setzt am Ende immer `validate`), die
  JPA-Schema-Aktion nur `validate`; Schlüssel, die an der Pool-Verbindung vorbei verbinden
  (`connection.url`, `connection.username`, `connection.password`, `connection.driver_class`,
  `connection.provider_class`, `connection.datasource`, `jakarta.persistence.jdbc.*`, JTA-/Non-JTA-DataSource),
  werden abgelehnt.
- **Wo die Pool- und ORM-Werte stehen:** `titan.database.hikari.*` und `titan.database.hibernate.*`
  gehören in eine YAML-Datei (mitgelieferte Standardwerte, `application.yaml`, `application-<profil>.yaml`
  oder eine Datei über `CONFIG_FILE`). Systemproperties (`-D`) und Umgebungsvariablen überschreiben
  dort nur Schlüssel, die in einer YAML-Datei schon vorkommen (etwa `maximumPoolSize`, das die
  Standardwerte mitbringen); ein Schlüssel, der nur als `-D`/Umgebungsvariable existiert
  (z. B. `-Dtitan.database.hikari.minimumIdle=2`), wird ignoriert. Neue Pool- und ORM-Einstellungen
  also in `application.yaml` eintragen. `url`, `user` und `password` funktionieren dagegen auch
  allein als Umgebungsvariable (`TITAN_DATABASE_URL` usw.), ohne YAML-Eintrag.

```yaml
titan:
  database:
    hikari:
      maximumPoolSize: 4          # Standard
      connectionTimeout: 5000     # Standard, ms
      initializationFailTimeout: 5000  # Standard, ms
      dataSource.reWriteBatchedInserts: true
    hibernate:
      jdbc.batch_size: 20
```

- **Migrationen:** Jede Column bringt ihre Flyway-Skripte unter `db/migration/<column>/` mit (Name
  `[a-z][a-z0-9_]*`), die Historie liegt je Column in `flyway_<column>_history`. Das Schema ist
  gemeinsam: der erste Lauf einer Column findet fremde Tabellen ohne eigene Historie und
  baselined bei Version 0, deshalb darf keine Column ein `V0` ausliefern. jumprun:
  `db/migration/jumprun/V1__create_jumprun_run.sql`.
- **Tabelle `jumprun_run`** (nur angehängt, nie geändert): `id` (bigint, identity), `player_uuid`,
  `player_name` (varchar 32), `mode` und `end_reason` (Namen der Enum-Konstanten, nie Ordinale),
  `score` (>= 0), `finished_at` (timestamptz).
- **Logging:** Hibernate, Flyway und Hikari loggen ab WARN (`common/src/main/resources/logback.xml`);
  ein INFO `Database ready` (Anzahl Units und angewandter Migrationen, nie URL oder Zugangsdaten)
  zeigt den erfolgreichen Start.
- **Lokal testen:**

  ```sh
  docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=titan -e POSTGRES_DB=titan postgres:17
  TITAN_DATABASE_URL=jdbc:postgresql://localhost:5432/titan TITAN_DATABASE_USER=postgres \
    TITAN_DATABASE_PASSWORD=titan java -jar titan-local.jar
  ```

- **AOT-Training:** Den Trainingslauf (`-XX:AOTCacheOutput=...`) mit konfigurierter Datenbank
  fahren, sonst fehlen die Datenbank-Klassen im Cache.
- **Zurückrollen:** `titan.database.url` entfernen und neu starten; die Tabelle bleibt bestehen,
  die Rekorde liegen dann wieder nur im Speicher.

## Traces und Metriken

Die Lobby kompiliert nur gegen die OpenTelemetry-**API**; das SDK und die Exporter liefert der
OpenTelemetry-Java-Agent (live 2.16.0, API/BOM 1.50.0 - beide ziehen gemeinsam an). Ohne Agent sind
alle Spans und Zähler No-ops, die Lobby verhält sich unverändert.

- **`Telemetry`** (`core.telemetry`) ist ein Record aus `Tracer` und `Meter` und eine Bean aus
  `runtime`. Ein Feature bekommt sie über den Konstruktor, nie über `GlobalOpenTelemetry` (nur
  `runtime` darf das, ArchUnit prüft es). `telemetry.inSpan(name, attributes, body)` startet den
  Span, macht ihn current, zeichnet eine Ausnahme mit Status ERROR auf, wirft sie weiter und beendet
  den Span immer.
- **`FeatureNode.attach(parent, id, priority, telemetry)`** hält die `Telemetry` je Instanz. Der
  alte Aufruf ohne `telemetry` nutzt `Telemetry.noop()`. `attach` und `close` hängen die Span-Events
  `feature.started` und `feature.stopped` (`titan.feature`, `titan.feature.priority`) an den
  aktuellen Span, das ist die Startreihenfolge in `titan.startup` und `titan.shutdown`.
- **`on(...)`** bleibt wie es war; der `ListenerGuard` zählt jeden gefangenen Fehler als
  `titan.listener.failures{titan.feature}`.
- **`onTraced(Typ, spanName, listener)`** und `onTracedIncludingCancelled(...)` führen den Listener
  in einem Span aus (`titan.feature`, bei einem `PlayerEvent` `user.id`). Wirft er, trägt der Span
  die Ausnahme, danach erreicht sie den Guard wie sonst.
- **Sperrliste:** `onTraced` wirft beim Registrieren `IllegalArgumentException` für
  `PlayerMoveEvent`, `PlayerPacketEvent`, `PlayerPacketOutEvent`, `PlayerChunkLoadEvent`,
  `PlayerChunkUnloadEvent`, `PlayerTickEvent`, `PlayerTickEndEvent`, `EntityTickEvent`,
  `InstanceTickEvent`, `ServerTickMonitorEvent` und Unterklassen. Ein neuer hochfrequenter
  Event-Typ gehört in `FeatureNode.HIGH_FREQUENCY_EVENTS`.

### Span, Span-Event oder Zähler

| Operation | Mittel |
| --- | --- |
| selten (höchstens ein paar je Spieler und Minute), hat Ergebnis oder Dauer, Fehler möglich | Span, Name `<modul>.<operation>` |
| häufig, nur das Zählen zählt (Bewegung, Schaden, Rechteprüfung) | Zähler `<modul>.<größe>{ergebnis}` |
| häufig, aber im Lauf eines schon bestehenden Spans | Span-Event am aktuellen Span (`Span.current().addEvent`) |
| Verteilung (Score, Dauer) | Histogramm |

### Datenschutz und Attribute

- `user.id` ist die Spieler-UUID. Kein Spielername, keine IP, keine Locale, kein Chat.
- Spannamen sind feste Strings; Ergebnis-Attribute sind kleine Aufzählungen (`ok`, `denied`, ...).
- Metriken tragen **nie** `user.id` oder Namen (Kardinalität). Attribute dort haben eine kleine,
  feste Wertemenge.
- Eigene Schlüssel liegen unter `<modul>.*`; gemeinsam sind `titan.feature` und `user.id`
  (`Telemetry.FEATURE`, `Telemetry.USER_ID`).

### Was `runtime` schon liefert

`titan.startup` (mit `titan.variant`, `titan.profiles`, `titan.modules.loaded`), `titan.shutdown`,
`player.configure`, `player.join`, `player.disconnect` (je mit `user.id`), die Zähler
`player.joins` und `player.disconnects`, das Gauge `titan.players.online` und
`titan.listener.failures`. Datenbankarbeit über den `DatabaseWriter` nimmt den Kontext des
Auftraggebers mit, Hibernate-Spans hängen so am auslösenden Span.

### Testen mit `TestTelemetry`

`TestTelemetry.create()` (`core/testFixtures`) baut je Aufruf einen eigenen In-Memory-Exporter und
-Metric-Reader, ohne `GlobalOpenTelemetry`; `SimpleSpanProcessor` und Reader sind synchron, es gibt
kein Warten. Mit `spans()`, `span(name)`, `attribute(span, key)`, `counter(name, attributes)` und
`gauge(name)` liest ein Test zurück, was sein Code erzeugt hat. Ein Test legt sich seine eigene
Instanz an und schließt sie (`try`-with-resources oder `@AfterEach`). Wirft ein Test-Listener
absichtlich, fängt der Test die Server-Ausnahme über `env.process().exception().setExceptionHandler`
ab, sonst bricht Cyano den Test ab.

### Lokal sichtbar machen

Ohne Agent passiert nichts. Zum Ansehen den Agent laden und auf die Konsole exportieren:

```
JAVA_TOOL_OPTIONS="-javaagent:opentelemetry-javaagent.jar \
  -Dotel.traces.exporter=logging -Dotel.metrics.exporter=logging \
  -Dotel.logs.exporter=none -Dotel.service.name=Lobby-local" \
  ./gradlew :apps:local:run
```

Dann erscheinen `titan.startup` mit den `feature.started`-Events beim Start und `player.*`-Spans
beim Beitreten. Live exportiert der Agent per OTLP nach Tempo und Mimir; dort `{ name =
"titan.startup" }` bzw. `titan_players_online` abfragen.

## Checkliste: neues Feature = neues Modul unter `features/`

Ein neues Feature ist ein neues Gradle-Modul unter `features/<name>/` - weder eine andere Column
noch `runtime` noch eine zentrale Liste ändert sich dafür.

1. Verzeichnis `features/<name>/` mit Standard-Gradle-Layout
   (`src/main/java/net/onelitefeather/titan/feature/<name>/`, `src/test/java/...`) anlegen.
2. `features/<name>/build.gradle.kts` anlegen, das nur `titan.column` anwendet - das bringt die
   Abhängigkeit auf `core`, Minestom, Aves, den Avaje-Generator und den Test-Stack mit
   (`settings.gradle.kts` bindet das Modul automatisch per Verzeichnis-Scan ein).
3. `package-info.java` im Wurzelpaket der Column mit `@InjectModule(name = "<name>Column",
   requires = {...}, requiresString = {...})` nach dem Muster oben ("Das Muster für neue
   Columns").
4. `<Name>Module` (`@Singleton`, ein noch nicht vergebenes `EVENT_PRIORITY` - s. "Gefunden werden"
   oben und die Tabelle dort) anlegen: `@PostConstruct start()` hängt den `FeatureNode` an und
   registriert die Listener, `@PreDestroy stop()` ruft `node.close()` (und danach ggf.
   `task.cancel()`).
5. `ColumnArchitectureTest` anlegen, das `ColumnArchitectureRules` per `@ArchTest` auf das eigene
   Paket anwendet (s. "Architekturregeln" oben - als Vorlage dient jede bestehende Column).
6. Braucht das Feature Konfiguration: Schlüssel-Konstanten und das strenge Lesen über `Config`
   (inklusive `Config.getAs` für Zahlen) in `start()`, die Validierung in einer reinen,
   paketprivaten Funktion (s. "Konfiguration lesen" oben), sowie optional
   `features/<name>/src/main/resources/titan/defaults/<name>.yaml` mit den Standardwerten,
   kommentiert (s. "Standardwerte je Column" oben).
7. Braucht das Feature ein Hotbar- oder Ausrüstungsitem: eine eigene, paketprivate `@Factory`-Klasse
   mit einer `@Bean LobbyItem`-Methode (s. "Items als Beans" oben) und `provides = {LobbyItem
   .class}` in der `package-info.java`.
8. Abhängigkeiten (eine `Instance`, ein `Deliver`, ein `Clock`, der `Scheduler`, ...) über den
   Konstruktor anfordern. Braucht das Feature einen Plattform-Dienst, den es noch nicht gibt, kommt
   der entweder als weiteres `@Bean` in `runtime`s Plattform-Factory oder, falls er selbst
   feature-übergreifende Logik trägt, als eigene `@Singleton`-Klasse dazu.
9. Tests schreiben, bevor (oder während) der Code entsteht: Unit-Tests für die reine Logik und die
   Config-Validierung, ein Env-Integrationstest über direkte Konstruktion mit `TestTitanNode` für
   alles, was einen `Player` braucht - inklusive eines Tests, dass `stop()` keinen weiteren
   Event-Effekt mehr hat.

Das war's - **keine** zentrale Feature-Liste zu pflegen: `@Singleton` in einer Column unter
`features/` genügt, damit `runtime` das neue Feature beim Aufbau des `BeanScope` findet und
startet, und jede Variante, die alle Columns einbindet, nimmt es ohne eigene Änderung auf. Die
einzige Ausnahme von "kein geänderter Code außerhalb des eigenen Moduls" ist ein brandneuer,
geteilter Plattform-Dienst (Schritt 8): Der berührt zwangsläufig `runtime`, weil dort - und nur
dort - Plattform-Typen zu Avaje-Beans werden.
