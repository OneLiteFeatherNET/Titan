# Lobby-Feature-Module bauen

Dieses Dokument erklärt, wie ein Lobby-Feature als eigenständiges
`LobbyModule` gebaut wird: den Aufbau eines Moduls, die Andockpunkte des
`ModuleContext`, die Regeln für den Tick-Thread, den Testaufbau und die
Checkliste für ein neues Feature. Alle Codebeispiele stammen, wo nicht anders
vermerkt, aus dem lauffähigen Vorlagemodul
`app/src/test/java/net/onelitefeather/titan/app/feature/example/`
(`ExampleModule`, `ExampleConfig`, `ExampleGreetingRule`,
`ExampleGreetingTracker`, `ExampleItems`) - kopierbar als Ausgangspunkt für ein
echtes Feature. Es ist bewusst test-only (`app/src/test`, nicht
`app/src/main`), damit es nie als echtes Modul mitläuft - s. "Gefunden
werden" unten, warum das trotz `@Singleton`/`@Priority` an der Klasse
funktioniert.

## Aufbau eines Moduls

Ein Feature ist eine Klasse, die
`net.onelitefeather.titan.app.module.LobbyModule` implementiert:

```java
public interface LobbyModule {
    String id();
    void enable(ModuleContext context);
    default void disable() { }
}
```

- `id()` ist eine kurze, stabile Kennung (z. B. `"example"`), die als Name des
  Modul-eigenen Event-Node (`"titan/" + id()`) und als SLF4J-MDC-Wert `module`
  dient.
- `enable(ModuleContext)` läuft genau einmal, bevor ein Spieler die Lobby
  erreichen kann. Hier - und nur hier - meldet ein Modul alles an, was es
  braucht: Listener, Konfiguration, Items, Befehle, Tasks, Navigator-Einträge.
- `disable()` läuft, nachdem `ModuleRegistry` den Event-Node bereits abgehängt,
  die Tasks abgebrochen und alle über den Kontext registrierten Dinge (Befehle,
  Items, Navigator-Einträge) entfernt hat. Die meisten Module brauchen kein
  eigenes `disable()`.

`ModuleRegistry` startet alle Module in der konfigurierten Reihenfolge und
fährt beim Herunterfahren in umgekehrter Reihenfolge herunter. Jedes Modul
bekommt dabei einen eigenen `ModuleContext` - das einzige Objekt, über das ein
Modul die Plattform erreicht. Der rohe `EventNode` wird nie herausgegeben (s.
`design.md`, Entscheidung 3): alles, was ein Modul über den Kontext anmeldet,
räumt sich beim Abschalten von selbst auf.

Abhängigkeiten, die ein Modul braucht (z. B. `Deliver`, eine `Instance`),
kommen über den **Konstruktor**, nicht über den Kontext - `ExampleModule`
nimmt z. B. optional einen `Clock` entgegen, damit ein Test "jetzt" festlegen
kann, statt sich auf `System.currentTimeMillis()` zu verlassen (das gleiche
Muster wie `TickleModule`).

Ein Feature-Paket unter `app/feature/<name>` folgt einer festen Sichtbarkeit
(s. `design.md`, Entscheidung 10, und `ArchitectureTest`, unten): nur die
Klassen `<Name>Module` und `<Name>Config` sind `public`, alles andere -
Handler, Vorlagen, Items, Tags - ist paketprivat. Das ist kein Stilwunsch,
sondern wird im Build geprüft - allerdings nur für Produktionscode unter
`app/src/main`: `ArchitectureTest` analysiert mit
`ImportOption.DoNotIncludeTests`, das test-only Vorlagemodul unter
`app/src/test/.../feature/example/` läuft also nicht mit und hält diese
Regel nur per Konvention ein. Erst ein echtes Feature, das aus der Vorlage
nach `app/src/main` kopiert wird, wird von der Prüfung erfasst.

## Gefunden werden: `@Singleton` und `@Priority`

Seit `avaje-dependency-injection` gibt es keine zentrale Modulliste mehr:
`Titan` holt nach dem Aufbau des `BeanScope` alle Module über
`scope.listByPriority(LobbyModule.class)` (Dependency-Injection-Container
[Avaje Inject](https://avaje.io/inject/)). Damit ein Modul dabei gefunden
wird, braucht seine Klasse zwei Annotationen:

```java
@Singleton
@Priority(400)
public final class NavigatorModule implements LobbyModule {
    // ...
}
```

(`NavigatorModule`, `app/src/main/java/net/onelitefeather/titan/app/feature/navigator/NavigatorModule.java`)

- **`@Singleton`** (`jakarta.inject.Singleton`) macht die Klasse zu einer
  Avaje-Bean - ohne sie sieht der Container das Modul überhaupt nicht.
- **`@Priority`** (`io.avaje.inject.Priority`) legt die Startreihenfolge fest:
  `scope.listByPriority(...)` sortiert aufsteigend, niedrigere Werte zuerst.
  Jeder Wert muss **eindeutig** sein - das prüft
  `ArchitectureTest#modulePrioritiesAreUnique`
  (`app/src/test/java/net/onelitefeather/titan/app/architecture/ArchitectureTest.java`).

**Warnung: Nie ein `List<LobbyModule>` injizieren, um die Startreihenfolge zu
bekommen.** Nur `BeanScope.listByPriority(...)` sortiert nach `@Priority` -
ein konstruktorinjiziertes `List<LobbyModule>` (oder jedes `List<T>` von
`@Priority`-Beans) liefert Avaje in Registrierungsreihenfolge, **nicht**
sortiert. Und `listByPriority(...)` funktioniert erst, **nachdem**
`BeanScope.builder().build()` zurückgekehrt ist - ein Aufruf während des
Scope-Aufbaus (also aus einem `@Factory`/`@Bean`, dem der Scope selbst
injiziert wurde) wirft `IllegalStateException`. Die Startreihenfolge kommt
deshalb ausschließlich aus `Titan`s eigenem
`scope.listByPriority(LobbyModule.class)`, aufgerufen nachdem
`BeanScope.builder().build()` fertig ist (s. `Titan.java`).

Die heutigen sieben Module, in Hunderterschritten mit Platz dazwischen:

| Modul | Priorität |
|---|---|
| protection | 100 |
| spawn | 200 |
| respawn | 300 |
| navigator | 400 |
| sit | 500 |
| tickle | 600 |
| elytra | 700 |

Ein neues Modul wählt eine freie Zahl aus der Lücke, an der es einschalten
soll. Fehlt `@Singleton` oder `@Priority` an einer Klasse, die `LobbyModule`
implementiert, verschwindet das Modul nicht etwa unbemerkt aus der Lobby:
`ArchitectureTest#featureLobbyModulesAreSingletonWithPriority` lässt den
Build fehlschlagen.

**Abhängigkeiten kommen über den Konstruktor.** Avaje löst sie aus dem
`BeanScope` auf - ein `Deliver`, eine `Instance`, ein `Clock` werden einfach
als Konstruktorparameter angefordert (s. `NavigatorModule(Deliver,
NavigatorEntries, FeatureFlags)`, `SpawnModule(Instance, LobbySpawn)`).
`@Inject` (`jakarta.inject.Inject`) auf dem Konstruktor braucht nur eine
Klasse mit **mehr als einem** Konstruktor, damit Avaje weiß, welchen sie
nehmen soll (s. `TickleModule`, dessen einziger echter Konstruktor `@Inject
TickleModule(Clock)` trägt); mit genau einem Konstruktor reicht der ohne
`@Inject`.

Welche Plattform-Dienste als Bean zur Verfügung stehen, steht in
`app/src/main/java/net/onelitefeather/titan/app/bootstrap/PlatformBeans.java`
(`@Factory` mit einer `@Bean`-Methode je Dienst: `InstanceContainer`,
`MapProvider`, `LobbySpawn`, `Deliver`, `Configuration` (avaje-config) und
`ConfigSections` darüber, der `@Named("titan")` qualifizierte
`EventNode<Event>`, `ItemRegistry`, `NavigatorEntries`, `FeatureFlags`,
`Clock`). Braucht ein neues Feature einen **neuen** geteilten Dienst:

- Ist er im Kern ein Plattform-Typ aus `common` oder Minestom, den mehrere
  Module brauchen (wie die bestehenden Beans oben), kommt eine weitere
  `@Bean`-Methode in dieselbe `PlatformBeans`-Factory dazu.
- Trägt er selbst Feature-übergreifende Logik, statt nur einen fremden Typ
  einzuhüllen, wird er eine eigene `@Singleton`-Klasse (ohne `@Priority` -
  das brauchen nur `LobbyModule`-Implementierungen), die betroffene Module
  dann per Konstruktor anfordern.

**Fehlt eine Abhängigkeit ganz** (kein passender `@Bean`/`@Singleton` im
Scope für einen Konstruktorparameter), bricht `BeanScope.builder().build()`
mit einer Exception ab, die den fehlenden Typ nennt. `Titan` baut den Scope
im Konstruktor; `TitanApplication.main` fängt jede `RuntimeException` aus
`new Titan()`/`titan.initialize()` ab, loggt sie als `Titan failed to
start: …` und beendet den Prozess mit Exit-Code 1 - der Fehler fällt beim
Start auf, nicht erst, wenn ein Spieler das Feature benutzt. Eine vergessene
`@Singleton`- oder `@Priority`-Annotation dagegen fällt schon beim Build auf,
über die ArchUnit-Regel oben.

Sind alle Module eingeschaltet, loggt `Titan#initialize()` einmal die
tatsächliche Startreihenfolge auf INFO-Level: `Lobby modules enabled in
order: {}`, gefüllt mit den `id()`-Werten in der Reihenfolge von
`scope.listByPriority(LobbyModule.class)`. Das macht die Reihenfolge aus der
Tabelle oben auch zur Laufzeit sichtbar, ohne dass sie noch an einer Stelle
im Code als Liste steht.

## Andockpunkte des `ModuleContext`

Der `ModuleContext`, den `enable(ModuleContext)` bekommt, bietet genau diese
Andockpunkte:

### `listen` - Events abonnieren

```java
context.listen(PlayerDisconnectEvent.class, event -> tracker.clear(event.getPlayer().getUuid()));
```

(`ExampleModule#enable`). Der Listener läuft in
`TitanObservability.guard(moduleId, listener)` gekapselt, damit ein Fehler
Modul und - falls vorhanden - Spieler zugeordnet werden kann, und hängt am
Modul-eigenen Event-Node. Bei einem
`net.minestom.server.event.trait.CancellableEvent` wird der Listener
übersprungen, sobald das Event beim Erreichen dieses Knotens schon abgebrochen
ist - das normale Verhalten eines `Consumer`-Listeners in Minestom.

`listen` funktioniert **nur, während `enable()` läuft**; ein späterer Aufruf
wirft `IllegalStateException`. Module melden alles an, was sie brauchen, beim
Start - nie erst, wenn ein Spieler joint oder ein Menü öffnet.

### `listenIncludingCancelled` - trotzdem reagieren

```java
context.listenIncludingCancelled(SomeCancellableEvent.class, this::onEvent);
```

Diese Variante liefert das Event auch dann, wenn es beim Erreichen des
Moduls schon abgebrochen ist - anders als `listen`, das einen
`Consumer`-Listener für ein bereits abgebrochenes `CancellableEvent`
überspringt (Minestoms Standardverhalten für diese Art Listener). Sie ist die
Plattform-Option für ein Modul, das auf ein `CancellableEvent` reagieren
**muss**, egal was ein anderes Modul vorher damit gemacht hat - etwa
`feature.protection.ProtectionModule`, das jedes `InventoryPreClickEvent`
bedingungslos abbricht - und das dabei unabhängig von der Einschaltreihenfolge
der beiden Module bleiben soll (s. `lobby-modules`-Spec, "Module sind
voneinander unabhängig"). Der Handler sieht `isCancelled()` weiterhin selbst
und kann das Event zusätzlich selbst abbrechen.

Kein heutiges Feature-Modul braucht das: Der Navigator etwa reagiert nicht
über einen eigenen `InventoryPreClickEvent`-Listener auf Klicks, sondern über
Aves' eigenen Click-Handler, den `NavigatorInventory` direkt auf dem gebauten
Inventar registriert (s. `NavigatorInventory`, Javadoc, und
`NavigatorProtectionOrderingTest`) - der läuft vor jedem regulären
Event-Node und damit vor `ProtectionModule`s Abbruch, unabhängig von der
Einschaltreihenfolge, ganz ohne `listenIncludingCancelled`.

Faustregel: `listen`, solange ein anderes Modul das Event nicht schon
abbrechen könnte; `listenIncludingCancelled` nur, wenn ein über
`context.listen` angemeldeter Handler wirklich in jedem Fall laufen muss.

### `config` - den eigenen Konfigurationsabschnitt lesen

```java
public record ExampleConfig(String greeting, long cooldownMillis) {
    public static final ExampleConfig DEFAULTS = new ExampleConfig("Welcome to the lobby, %s!", 5000);

    public ExampleConfig {
        if (greeting == null || greeting.isBlank()) {
            throw ConfigException.invalid("greeting", "must not be blank");
        }
        if (!greeting.contains("%s")) {
            throw ConfigException.invalid("greeting", "must contain a '%s' placeholder for the player's name");
        }
        if (cooldownMillis < 0) {
            throw ConfigException.invalid("cooldownMillis", "must not be negative");
        }
    }
}
```

```java
ExampleConfig config = context.config(ExampleConfig.class, ExampleConfig.DEFAULTS);
```

Ein Config-Record kennt nur sein eigenes Feld und den Grund, warum ein Wert
abgelehnt wird - nie die Quelle, aus der er geladen wurde. Deshalb wirft der
Compact Constructor `ConfigException.invalid(field, reason)`; der
`SectionBinder` hinter `ConfigSections` (`common/.../config/`) ergänzt
Abschnitt (und, wo bekannt, Datei), bevor die Ausnahme
`ModuleRegistry.enableAll()` verlässt und den Start abbricht - mit Modul,
Feld und Grund in der Meldung. `context.config` liest immer nur den
**eigenen** Abschnitt (die Modul-`id()`), zusammengesetzt aus
`application.yaml`, den Dateien aktiver Profile und Overrides (Env-Variable,
System-Property) - Rangfolge und die Abbildung auf Env-Variablen stehen im
README unter "Configuration". Es gibt keine Überladung für einen anderen
Abschnitt. Ohne konfigurierte `ConfigSections` (z. B. im
`ModuleHarness.startStandalone`-Testaufbau ohne Konfigurationsdatei) liefert
`config` unverändert `defaults` zurück. Wie `listen` funktioniert `config`
nur während `enable()`.

Die Lobby schreibt keine Konfiguration mehr: Es gibt kein `flush()`, keine
Datei wird angelegt oder verändert - einzige Ausnahme ist die einmalige
Umstellung einer bestehenden `app.json` beim Start (`AppJsonMigration`, s.
README unter "Migrating from app.json"). Für ein neues Feature bedeutet das:
seinen Abschnitt samt Defaults im README dokumentieren (s. Checkliste,
Schritt 6) und, falls Betreiber ihn direkt sehen sollen, in derselben PR in
`app/src/dist/application.example.yaml` ergänzen - sonst läuft er nur mit
Defaults im Speicher, bis ihn jemand in `application.yaml` einträgt.

### `items` - ein Hotbar- oder Ausrüstungsitem anmelden

```java
context.items().register(new LobbyItem(
    Key.key("titan:example"),
    ExampleItems.GREETING_TOKEN,
    ItemSlot.hotbar(GREETING_TOKEN_SLOT),
    (player, event) -> greet(player, tracker)));
```

`ItemRegistry` stempelt beim Registrieren einen Identitäts-Tag auf den Stack
und dispatcht **ein einziges** `PlayerUseItemEvent` am Plattform-Node an den
passenden `onUse`-Handler zurück - ein Modul braucht dafür keinen eigenen
Listener. `ItemSlot` ist `hotbar(0..8)`, `equipment(EquipmentSlot)` oder
`unplaced()` für ein Item ohne festen Platz (z. B. das Elytra-Feuerwerk, das
nur während des Fliegens in der Nebenhand liegt). Nach `enableAll()` prüft
`ItemRegistry.validate()`, ob zwei Module denselben festen Platz beanspruchen,
und bricht den Start sonst ab. `items().equip(player)` räumt das Inventar und
setzt alle Items mit festem Platz - das rufen Spawn- und Respawn-Modul auf,
nicht jedes Feature selbst.

### `navigator` - einen Eintrag im gemeinsamen Navigator anbieten

```java
context.navigator().add(new NavigatorEntry(slot, icon, displayName, destination));
```

(sinngemäß `NavigatorModule#enable`, dort aus der eigenen Config gebaut).
`context.navigator()` liefert nur die schmale, reine Hinzufügen-Sicht
(`NavigatorEntries.View`) auf die plattformweite `NavigatorEntries` - jedes
Modul kann Ziele beisteuern, ohne vom `NavigatorModule` selbst abzuhängen. Ist
das `NavigatorModule` ausgeschaltet, bleiben die Einträge einfach ungenutzt.
Die Einträge eines Moduls verschwinden automatisch, wenn es abgeschaltet
wird.

**Einträge hinter einer Feature-Flag verstecken:** `NavigatorEntry` (und, für
den Navigator selbst, `NavigatorConfig.Entry`) trägt ein optionales Feld
`feature` - den Namen einer `TitanFeatures`-Konstante, z. B.
`"NAVIGATOR_SLENDER"`. Ist die Flag aus (oder fehlt sie ganz in
`flags.properties` - ein sicherer Standard), rendert `NavigatorInventory` an
dieser Stelle die normale graue Glasscheibe statt des Eintrags; ist sie an,
erscheint der Eintrag wie gewohnt. Geprüft wird über die kleine
`net.onelitefeather.titan.common.feature.FeatureFlags`-Schnittstelle, die dem
`NavigatorModule` per Konstruktor übergeben wird - produktiv
`TogglzFeatureFlags` (steckt hinter `TitanFeatures`/Togglz), in Tests eine
Attrappe, damit Tests ohne echte `flags.properties`-Datei und ohne den
statischen `FeatureContext` auskommen. Ein Eintrag mit einem Namen, den
`FeatureFlags` nicht kennt, bricht den Start ab (`ConfigException`, nennt
`navigator.entries` und den unbekannten Namen). Das gilt auch für Einträge,
die ein anderes Modul über `context.navigator().add(...)` beisteuert, nicht
nur für die Einträge aus der `navigator`-Config selbst - das Feld sitzt auf
`NavigatorEntry` und damit auf jedem Eintrag gleichermaßen, statt in einer
separaten Tabelle, die der Navigator sonst parallel zur Registry pflegen
müsste.

### `commands` - einen Befehl anmelden

```java
Command command = new Command(COMMAND_NAME);
command.addSyntax((sender, commandContext) -> {
    if (sender instanceof Player player) {
        greet(player, tracker);
    }
});
context.commands().register(command);
```

(`ExampleModule#enable`). `ModuleCommands.register(Command)` registriert den
Befehl sofort bei Minestoms `CommandManager` und meldet gleichzeitig dessen
Abmeldung beim Abschalten des Moduls an - ein Modul muss sich nie selbst um
`unregister` kümmern.

### `tasks` - wiederkehrende Arbeit planen

```java
context.tasks().schedule(this::tick, TaskSchedule.seconds(1), TaskSchedule.seconds(1));
```

`ModuleTasks#schedule(Runnable, TaskSchedule delay, TaskSchedule repeat)`
plant eine Aufgabe beim Scheduler des Moduls; sie wird beim Abschalten des
Moduls automatisch abgebrochen, ohne dass das Modul sich das zurückgegebene
`Task`-Objekt merken muss (es kann es trotzdem behalten, um früher selbst
abzubrechen). Kein heutiges Feature-Modul braucht das - das Beispiel oben ist
illustrativ, nicht aus `ExampleModule` übernommen.

## Regeln für den Tick-Thread

`enable(ModuleContext)` läuft beim Start; alles, was danach über `listen`
registriert wurde, läuft **auf dem Tick-Thread**. Daraus folgen vier Regeln:

1. **Keine Listener-Registrierung zur Laufzeit.** `listen`, `listenIncludingCancelled`
   und `config` funktionieren nur, während `enable()` läuft - danach wirft
   `ModuleContext` `IllegalStateException`. Das ist kein Zufall: Der
   Navigator-Speicherleck auf `main` (s. `design.md`, Kontext) entstand genau
   dadurch, dass pro Spieler zur Laufzeit neue Listener angemeldet wurden, ohne
   sie je wieder abzumelden.
2. **Kein blockierendes IO/HTTP in Handlern.** Ein Listener, der z. B. auf eine
   HTTP-Antwort wartet, blockiert den gesamten Tick und damit jeden Spieler in
   der Lobby. Braucht ein Handler externe Daten, müssen sie vorher geladen
   (z. B. beim Start in `enable()`) oder asynchron nachgeladen und dann
   thread-sicher zwischengespeichert werden.
3. **Pakete/Components zwischenspeichern statt neu bauen.** `ExampleItems`
   baut die feste Rückmeldung `ON_COOLDOWN` einmal als `static final
   Component` statt bei jeder Benutzung neu - dasselbe Prinzip, in größerem
   Maßstab, hinter `NavigatorModule`s `NavigatorInventory`: Das geteilte
   Inventar wird nur neu gebaut, wenn sich die sichtbare Eintragsmenge
   geändert hat - weil sich `NavigatorEntries.version()` geändert hat (ein
   Eintrag kam hinzu oder fiel weg) oder weil sich der Zustand einer
   Feature-Flag geändert hat -, nicht bei jedem Öffnen.
4. **Spielerbezogener Zustand gehört aufgeräumt.** Zustand, der pro Spieler
   gehalten wird (z. B. ein Cooldown-Zeitstempel), muss bei
   `PlayerDisconnectEvent` entfernt werden, sonst wächst er über die
   Serverlaufzeit unbegrenzt. `ExampleGreetingTracker#clear`, angestoßen aus
   `ExampleModule`s `PlayerDisconnectEvent`-Listener, und
   `FireworkBoostTracker#clear` in `ElytraModule` folgen diesem Muster. Ebenso
   gehört ein wiederkehrender Task, der pro Spieler arbeitet, über
   `context.tasks()` angemeldet (automatischer Abbruch beim Abschalten des
   Moduls) statt über einen selbst verwalteten Thread.

## Tests: Aufbau und `ModuleHarness`

Tests folgen der Testpyramide - viele schnelle, reine Unit-Tests unten, wenige
Env-Integrationstests oben - und dem F.I.R.S.T.-Prinzip (**F**ast,
**I**ndependent, **R**epeatable, **S**elf-validating, **T**imely): Tests
laufen schnell, unabhängig voneinander, liefern bei jedem Lauf dasselbe
Ergebnis (deshalb ein fester `Clock.fixed(...)` statt der Systemzeit, s.
`ExampleModuleTest`/`TickleModuleTest`), prüfen sich selbst über Assertions
statt manueller Log-Kontrolle, und entstehen zusammen mit dem Code, nicht
danach.

### Unten: reine Unit-Tests

Reine Entscheidungs- und Formatierungslogik gehört in eine eigene,
paketprivate Klasse ohne Minestom-Abhängigkeit -
`ExampleGreetingRuleTest` prüft `ExampleGreetingRule.isOnCooldown(...)` und
`ExampleGreetingRule.greeting(...)` ganz ohne `Env` oder `Player`. Genauso
prüft `ExampleConfigTest` die Validierung im Compact Constructor von
`ExampleConfig` direkt, ohne `ConfigSections`.

### Oben: Env-Integrationstests über `ModuleHarness`

`net.onelitefeather.titan.app.module.testing.ModuleHarness` startet ein oder
mehrere `LobbyModule`s über eine echte `ModuleRegistry`, ohne dass jeder Test
Registry, `ItemRegistry` und `NavigatorEntries` von Hand aufbauen muss:

```java
@ExtendWith(MicrotusExtension.class)
class ExampleModuleTest {
    @Test
    void usingTheGreetingTokenSendsTheConfiguredGreeting(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ExampleModule())) {
            // harness.items(), harness.navigator(), harness.registry() ...
        }
    }
}
```

- `ModuleHarness.start(Env, LobbyModule...)` hängt einen frischen Kind-Node
  unter `env.process().eventHandler()` und nimmt Scheduler/`CommandManager`
  des `Env` - für ein Modul, dessen Verhalten einen echten `Player` oder eine
  `Instance` braucht.
- `ModuleHarness.startStandalone(LobbyModule...)` baut stattdessen einen
  eigenständigen Scheduler, `CommandManager` und Event-Node ohne `Env` - für
  reine Verdrahtungstests, die keinen Spieler brauchen (s.
  `ModuleContextTest`, `ModuleContextConfigTest`).
- Beide gibt es mit einer Überladung, die eine `ConfigSections` (oder einen
  `Path` auf eine YAML-Datei) entgegennimmt, für Tests, die
  `context.config(...)` abdecken sollen - `ExampleModuleTest` liest so eine
  temporäre `application.yaml` über `@TempDir`.
- Ein Modul, dessen Konstruktor schon die plattformweite `NavigatorEntries`
  oder `ItemRegistry` braucht (z. B. `NavigatorModule`, das beim Öffnen jedes
  Moduls Einträge zurückliest, nicht nur die eigenen), nutzt die
  `ModuleHarness.ModuleFactory`-Überladung: Der Harness baut Registry und
  Item-Registry zuerst und reicht sie der Factory.
- `close()` (bzw. Try-with-Resources) ruft `ModuleRegistry.disableAll()` und
  hängt den Harness-Node wieder ab - ohne das leckt ein Test Listener in den
  nächsten.

Item-Dispatch wird über ein direkt gefeuertes `PlayerUseItemEvent` getestet
(`env.process().eventHandler().call(new PlayerUseItemEvent(player, hand,
stampedStack, sequence))`), Chat-Ausgaben über
`TestConnection#trackIncoming(SystemChatPacket.class)`. Achtung:
`Collector#collect()` (und die `assertSingle()`/`assertEmpty()`-Kurzformen,
die es aufrufen) **entnimmt** den Tracker aus der Verbindung - nach dem ersten
`collect()` werden keine weiteren Pakete mehr mitgeschnitten. Für einen Test
mit mehreren Aktionen deshalb erst alle Events feuern und danach genau einmal
`collect()` aufrufen, nicht dazwischen (s. `ExampleModuleTest`).

## Architekturregeln (ArchUnit)

`app/src/test/java/net/onelitefeather/titan/app/architecture/ArchitectureTest`
prüft im Build, nicht nur per Konvention (s. `design.md`, Entscheidung 10):

1. Feature-Pakete unter `..app.feature.(*)..` hängen nicht voneinander ab.
2. Klassen in `..app.module..` und `..titan.common..` hängen nicht von
   `..app.feature..` ab.
3. Nur `*Module` und `*Config` in `..app.feature..` sind `public`.
4. Nur Plattform-Code (`..app.module..`) und `TitanApplication` rufen
   `EventNode#addListener`/`GlobalEventHandler#addListener` direkt auf - ein
   Feature-Modul geht immer über `context.listen`/`listenIncludingCancelled`.
5. Jede `LobbyModule`-Implementierung in `..app.feature..` trägt `@Singleton`
   **und** `@io.avaje.inject.Priority` (s. "Gefunden werden" oben).
6. Kein Feature-Code hängt von `io.avaje.inject.BeanScope` ab - Abhängigkeiten
   kommen ausschließlich über den Konstruktor, kein Service-Locator.
7. Die `@Priority`-Werte aller Module in `..app.feature..` sind eindeutig
   (`ArchitectureTest#modulePrioritiesAreUnique`, ein Reflection-Test statt
   einer `ArchRule`).

## Checkliste: neues Feature = neues Paket, null geänderte Zeilen außerhalb

1. Neues Paket `app/src/main/java/net/onelitefeather/titan/app/feature/<name>/`
   anlegen - `app/src/test/.../feature/example/` als Kopiervorlage nehmen.
2. `<Name>Module` (public, implementiert `LobbyModule`, trägt `@Singleton`
   und ein noch nicht vergebenes `@Priority(n)` - s. "Gefunden werden"
   oben und die Prioritätstabelle dort) und, falls das Feature Konfiguration
   braucht, `<Name>Config` (public record, Defaults + Validierung über
   `ConfigException.invalid` im Compact Constructor) anlegen. Alles andere -
   Handler, reine Logik, Item-/Tag-Konstanten - bleibt paketprivat.
3. Abhängigkeiten (eine `Instance`, ein `Deliver`, ein `Clock`, ...) über den
   Konstruktor anfordern, `@Inject` nur, falls die Klasse mehr als einen
   Konstruktor hat (s. "Gefunden werden" oben). Braucht das Feature einen
   Plattform-Dienst, den es noch nicht gibt, kommt der entweder als weiteres
   `@Bean` in `PlatformBeans` oder, falls er selbst Feature-übergreifende
   Logik trägt, als eigene `@Singleton`-Klasse dazu.
4. In `enable(ModuleContext context)` die gebrauchten Andockpunkte verdrahten:
   `context.config(...)`, `context.items().register(...)`,
   `context.commands().register(...)`, `context.navigator().add(...)`,
   `context.listen(...)`/`listenIncludingCancelled(...)`, `context.tasks()`.
5. Tests schreiben, bevor (oder während) der Code entsteht: Unit-Tests für die
   reine Logik und die Config-Validierung, ein Env-Integrationstest über
   `ModuleHarness` für alles, was einen `Player` braucht.
6. Falls das Feature einen Konfigurationsabschnitt hat: die neuen Felder samt
   Defaults und ihren Env-Variablen-Namen im README unter "Configuration
   Options Explained" bzw. "Environment variable reference" dokumentieren -
   die Lobby schreibt keine Konfiguration mehr (s. "config" oben), der
   Abschnitt läuft bis dahin nur mit Defaults im Speicher. Sollen Betreiber
   ihn direkt sehen, den Abschnitt zusätzlich in derselben PR in
   `app/src/dist/application.example.yaml` ergänzen.

Das war's - **keine** zentrale Modulliste mehr zu pflegen: `@Singleton` plus
`@Priority` genügen, damit `Titan` das neue Modul über
`scope.listByPriority(LobbyModule.class)` findet und an der richtigen Stelle
startet (s. `lobby-modules`-Spec, Szenario "Beispielmodul aus der Vorlage").
Die einzige Ausnahme von "null geänderte Zeilen außerhalb des eigenen
Pakets" ist ein brandneuer, geteilter Plattform-Dienst (Schritt 3): Der
berührt zwangsläufig `PlatformBeans`, weil dort - und nur dort - Plattform-
Typen zu Avaje-Beans werden.

`ExampleModule` selbst bleibt test-only (`app/src/test`, nicht
`app/src/main`) und trägt trotzdem `@Singleton`/`@Priority(800)` (mit einem
Kommentar, dass ein echtes Modul einen noch nicht vergebenen Wert braucht)
sowie `@Inject` auf seinem `Clock`-Konstruktor, damit die Vorlage als Ganzes
korrekt kopierbar bleibt. Gefunden wird es trotzdem nicht: Der
Annotation-Processor läuft nicht für Testquellen
(`testAnnotationProcessor` ist nicht gesetzt), `ModuleWiringTest` sieht also
weiterhin genau die sieben Module aus der Tabelle oben, nicht acht. Als
reguläres Feature bräuchte es genau die Annotationen aus Schritt 2, sonst
keine Änderung außerhalb seines eigenen Pakets.
