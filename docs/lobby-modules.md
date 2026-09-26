# Lobby-Feature-Module bauen

Dieses Dokument erklärt, wie ein Lobby-Feature als eigenständiges
`LobbyModule` gebaut wird: den Aufbau eines Moduls, die Andockpunkte des
`ModuleContext`, die Regeln für den Tick-Thread, den Testaufbau und die
Checkliste für ein neues Feature. Alle Codebeispiele stammen, wo nicht anders
vermerkt, aus dem lauffähigen Vorlagemodul
`app/src/test/java/net/onelitefeather/titan/app/feature/example/`
(`ExampleModule`, `ExampleGreetingRule`, `ExampleGreetingSettings`,
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
Klasse `<Name>Module` ist `public`, alles andere - Handler, Vorlagen, Items,
Tags, die paketprivaten Prüffunktionen aus "Konfiguration lesen" unten - ist
paketprivat. Das ist kein Stilwunsch,
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
`MapProvider`, `LobbySpawn`, `Deliver`, der `@Named("titan")` qualifizierte
`EventNode<Event>`, `ItemRegistry`, `NavigatorEntries`, `FeatureFlags`,
`Clock`). Konfiguration kommt nicht über eine Bean - ein Modul liest sie
direkt über die statische Fassade `io.avaje.config.Config` (s. "Konfiguration
lesen" unten). Braucht ein neues Feature einen **neuen** geteilten Dienst:

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

### Konfiguration lesen: die statische Fassade `Config`

Anders als bei den übrigen Andockpunkten gibt es dafür **keine** Methode auf
`ModuleContext`: Ein Modul liest seine Werte in `enable(ModuleContext)` direkt
über die statische Fassade `io.avaje.config.Config` (avaje-config) - bewusst
eine Ausnahme von der Projektregel „keine statischen Singletons“ (s.
`design.md`, Entscheidung 1). Der Zugriff bleibt deshalb auf `enable()`
beschränkt; alles darunter (Handler, reine Logik) bekommt fertige Werte über
den Konstruktor, wie jede andere Abhängigkeit auch.

```java
// TickleModule.enable()
long cooldownMillis = Config.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis);
```

- **Strings** kommen über `Config.get(key)`, **Listen** über
  `Config.list().of(key)`, **Wahrheitswerte** über `Config.getBool(key)`.
- **Zahlen** (`int`/`long`/`double`) kommen über
  `Config.getAs(key, Integer::parseInt)` (entsprechend `Long::parseLong`,
  `Double::parseDouble`) statt über `Config.getInt/getLong/getDecimal`: Die
  Fassade wirft bei einem ungültigen Zahlenwert über diese drei bloß eine
  `NumberFormatException` ohne Schlüssel, aber `Config.getAs(key, fn)` fängt
  einen Fehler von `fn` selbst ab und wirft eine `IllegalStateException`, die
  den vollen Schlüssel einmal benennt und die ursprüngliche Exception (mit
  dem ungültigen Rohwert oder dem Ablehnungsgrund in ihrer eigenen Meldung)
  als `cause` behält - Schlüssel in der Meldung, Grund in der
  Ursachenkette.
- Der **Schlüssel** ist eine `private static final String`-Konstante im
  Modul, nach dem Schema `<modul-id>.<feld>` (z. B. `"tickle.cooldownMillis"`)
  - kein Config-Record mehr, das den Abschnitt beschreibt.
- Die **Prüfung** liegt in einer reinen, statischen, paketprivaten Funktion.
  Für einen **Einzelwert** parst und prüft sie in einem Schritt und dient
  direkt als `getAs`-Funktion - `TickleSettings.cooldownMillis(String)` oben
  prüft z. B. nur „nicht negativ“ und wirft dafür ein einfaches
  `IllegalArgumentException("must not be negative, was -5")`, ohne den
  Schlüssel selbst zu nennen: Das übernimmt `Config.getAs` bereits. Eine
  reine Zahl ohne eigene Prüfung braucht gar keine eigene Funktion
  (`Integer::parseInt` reicht). Für eine **Prüfung über mehrere Felder**
  (z. B. `spawn.minHeight` gegen `spawn.maxHeight`) oder eine Prüfung, die
  nicht über `getAs` läuft (z. B. `sit.allowedBlocks`, eine über
  `Config.list().of` gelesene Liste), nennt die Funktion beide bzw. den
  vollen Schlüssel selbst im Meldungstext, weil dort kein `getAs` das mehr
  übernimmt. Beide Formen werfen ein einfaches `IllegalArgumentException` -
  es gibt keine eigene Exception-Klasse dafür. So bleibt die Prüfung ohne
  `Config` und ohne Server testbar (s. „Tests“ unten).

**Standardwerte gehören in `application.yaml`, nicht in den Code.** Ein
Modul liest ohne eigenen Fallback im Code (`Config.get(key)`, nicht
`Config.get(key, "…")`) - die Standardwerte für jeden Schlüssel stehen einmal
in der mitgelieferten `app/src/main/resources/application.yaml`, die
avaje-config vor der Datei im Arbeitsverzeichnis aus dem Classpath lädt.
Fehlt ein Schlüssel dort, ist das ein Programmierfehler: Der Start bricht mit
`Missing required configuration parameter [key]` ab, nicht mit einem stillen
Fallback im Code. Ein neues Feature trägt seine Schlüssel samt Standardwert
deshalb in `app/src/main/resources/application.yaml` ein (s. Checkliste,
Schritt 6).

Die alte Bindung an ein Config-Record (`context.config(Typ, DEFAULTS)`, eine
Validierung im Compact Constructor) gibt es nicht mehr - die Bindeschicht
dahinter ist mit dieser Change vollständig entfernt.

Die Lobby schreibt keine Konfiguration mehr: Es gibt kein `flush()`, keine
Datei wird angelegt oder verändert.

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
den Navigator selbst, die gelesenen Rohwerte, die
`NavigatorEntryValidation#buildEntry` prüft) trägt ein optionales Feld
`feature` - den Namen einer Flag aus dem Abschnitt `features` der
Konfiguration, z. B. `"NAVIGATOR_SLENDER"`. Ist die Flag aus (oder steht sie
nirgends gesetzt - ein sicherer Standard), rendert `NavigatorInventory` an
dieser Stelle die normale graue Glasscheibe statt des Eintrags; ist sie an,
erscheint der Eintrag wie gewohnt. Geprüft wird über die kleine
`net.onelitefeather.titan.common.feature.FeatureFlags`-Schnittstelle, die dem
`NavigatorModule` per Konstruktor übergeben wird - produktiv
`ConfigFeatureFlags` (liest `features.<name>` über die statische Fassade
`Config`, s. `openspec/changes/config-reload-feature-flags/design.md`,
Entscheidung 4), in Tests eine Attrappe (`FakeFeatureFlags`), damit Tests ohne
echte Konfigurationsdatei auskommen. Bekannt ist eine Flag nur, wenn sie unter
`features` in der mitgelieferten Classpath-`application.yaml` steht - eine
Betreiber-Datei kann diese Menge nicht erweitern, nur die einzelnen Flags
an- oder ausschalten. Ein Eintrag mit einem Namen, den `FeatureFlags` nicht
kennt, bricht den Start ab (`IllegalArgumentException`, nennt
`navigator.entries` und den unbekannten Namen); ändert sich ein Eintrag beim
Neuladen auf eine unbekannte Flag, verwirft der Reloader nur diese Änderung
und der Navigator zeigt weiter die bisherigen Ziele (s. "Konfiguration
neu laden zur Laufzeit" unten). Das gilt auch für Einträge, die ein anderes
Modul über `context.navigator().add(...)` beisteuert, nicht nur für die
Einträge aus der `navigator`-Config selbst - das Feld sitzt auf
`NavigatorEntry` und damit auf jedem Eintrag gleichermaßen, statt in einer
separaten Tabelle, die der Navigator sonst parallel zur Registry pflegen
müsste. Togglz und `flags.properties` sind entfernt: Eine Flag ist ein ganz
normaler Konfigurationswert unter `features.<NAME>`, mit denselben Quellen
und derselben Rangfolge wie jeder andere Schlüssel (s. README, Abschnitt
"Feature flags").

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

## Neu laden zur Laufzeit: ein Modul muss nichts dafür tun

Seit `config-reload-feature-flags` kann die Lobby ein einzelnes Modul im
laufenden Betrieb neu starten, ohne die übrigen Module zu berühren -
ausgelöst durch avaje-configs eingebaute Dateiüberwachung
(`config.watch.enabled`, standardmäßig aus; der Betreiber schaltet sie in
seiner eigenen `application.yaml`/Profil-Datei/`CONFIG_FILE` ein, s. README,
Abschnitt "Runtime reloading", für die Schalter `config.watch.delay`/
`config.watch.period`, was dabei für Spieler verloren geht und die Grenzen
der eingebauten Lösung). `ModuleRegistry#restart(String)`
(`app/src/main/java/net/onelitefeather/titan/app/module/ModuleRegistry.java`)
macht dafür beim betroffenen Modul genau das, was `disableAll()`/
`enableAll()` beim Start und Herunterfahren ohnehin tun: Event-Node abhängen,
Tasks abbrechen, die über den Kontext angemeldeten Dinge abräumen,
`disable()` aufrufen, dann mit einem frischen `ModuleContext` neu
`enable(ModuleContext)` aufrufen.

Daraus folgt für ein Modul, das die Andockpunkte oben (`listen`, `items`,
`navigator`, `commands`, `tasks`) statt eigener Listener, Felder oder Threads
nutzt: **Es muss für den Neustart nichts Eigenes tun.** Alles, was es beim
ersten `enable()` angemeldet hat, wird beim Abschalten automatisch entfernt,
und `enable()` meldet es beim Neustart einfach noch einmal an - derselbe Code
läuft ohnehin schon bei jedem normalen Start. Ein Modul, das stattdessen
außerhalb des Kontexts eigenen Zustand hält (ein selbst verwalteter Thread,
ein roher `EventNode`), muss diesen Zustand selbst in `disable()` aufräumen,
sonst bleibt er nach einem Neustart doppelt oder inkonsistent - ein weiterer
Grund, warum die Andockpunkte oben und nicht die rohe Plattform-API der Weg
sind, um etwas anzumelden.

Ein Neuladen startet nur die Module neu, deren eigener Konfigurationsabschnitt
(`<modul-id>.*`) sich geändert hat; Änderungen unter `features.*`, `titan.*`
oder `config.*` starten kein Modul. Ein Modul, das nur über `Config` liest
(s. "Konfiguration lesen" oben) und keinen eigenen Zustand außerhalb des
Kontexts hält, braucht für diese ganze Change also keine einzige geänderte
Zeile.

## Tests: Aufbau und `ModuleHarness`

Tests folgen der Testpyramide - viele schnelle, reine Unit-Tests unten, wenige
Env-Integrationstests oben - und dem F.I.R.S.T.-Prinzip (**F**ast,
**I**ndependent, **R**epeatable, **S**elf-validating, **T**imely): Tests
laufen schnell, unabhängig voneinander, liefern bei jedem Lauf dasselbe
Ergebnis (deshalb ein fester `Clock.fixed(...)` statt der Systemzeit, s.
`ExampleModuleTest`/`TickleModuleTest`), prüfen sich selbst über Assertions
statt manueller Log-Kontrolle, und entstehen zusammen mit dem Code, nicht
danach.

Weil `Config` globaler, pro JVM einmal geladener und danach unveränderlicher
Zustand ist (s. `design.md`, Entscheidung 5), gilt zusätzlich: **Kein Test**
ruft `Config.setProperty`, `Config.putAll`, `Config.clearProperty` oder
`Config.eventBuilder` auf - das wäre gemeinsamer, veränderlicher Zustand und
bricht "Independent". Es gibt auch **keine** `application-test.yaml`. Ein
Unit-Test liest grundsätzlich keine Config; er testet die reinen
Prüffunktionen (s. "Konfiguration lesen" oben) und die Klassen darunter mit
Werten per Konstruktor. Dass `Config.getAs` einen Fehler der eigenen
Prüffunktion in eine `IllegalStateException` mit dem vollen Schlüssel
übersetzt, lässt sich ebenfalls ohne Kind-JVM testen: eine **lokale**
`Configuration.builder().put(key, wert).build()`-Instanz (nicht die statische
Fassade) genügt, wie `TickleSettingsTest` es für einen ungültigen und einen
negativen Rohwert vormacht. Ein Test, der einen abweichenden Wert der
statischen Fassade selbst braucht (Rangfolge, Profil, Env, kaputte Datei),
läuft in einer eigenen Kind-JVM mit `@TempDir` als Arbeitsverzeichnis, wie
`ConfigurationPrecedenceTest`.

### Unten: reine Unit-Tests

Reine Entscheidungs- und Formatierungslogik gehört in eine eigene,
paketprivate Klasse ohne Minestom-Abhängigkeit -
`ExampleGreetingRuleTest` prüft `ExampleGreetingRule.isOnCooldown(...)` und
`ExampleGreetingRule.greeting(...)` ganz ohne `Env` oder `Player`. Genauso
prüft ein Unit-Test die paketprivate Prüffunktion aus "Konfiguration lesen"
oben direkt, ganz ohne `Config`: gültige Grenzwerte, ungültige Werte, Text der
Meldung.

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
  `ModuleContextTest`).
- `ModuleHarness` nimmt keinen Konfigurationsparameter mehr entgegen. Ein
  Modul-Integrationstest aktiviert das Modul mit den ausgelieferten
  Standardwerten aus der Classpath-`application.yaml` - es gibt **keine**
  `application-test.yaml`, damit Tests die ausgelieferten Standardwerte
  prüfen und nicht eine eigene Testwelt (s. Tests-Abschnitt oben).
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
3. In `..app.feature..` ist nur `*Module` `public`, dazu die von Avaje Inject
   generierten `$DI`-Klassen (Verdrahtungscode, keine handgeschriebene
   Feature-Oberfläche).
4. Nur Plattform-Code (`..app.module..`) und die Kompositionswurzel (`Titan`,
   `TitanApplication`, `PlatformBeans`) rufen
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
   und ein noch nicht vergebenes `@Priority(n)` - s. "Gefunden werden" oben
   und die Prioritätstabelle dort) anlegen. Braucht das Feature Konfiguration,
   kommen die Schlüssel-Konstanten und das Lesen über `Config` (inklusive
   `Config.getAs` für Zahlen) in dieselbe Klasse, die Validierung in eine
   reine, paketprivate Funktion
   (s. "Konfiguration lesen" oben) - kein eigenes Config-Record mehr. Alles
   andere - Handler, reine Logik, Item-/Tag-Konstanten - bleibt paketprivat.
3. Abhängigkeiten (eine `Instance`, ein `Deliver`, ein `Clock`, ...) über den
   Konstruktor anfordern, `@Inject` nur, falls die Klasse mehr als einen
   Konstruktor hat (s. "Gefunden werden" oben). Braucht das Feature einen
   Plattform-Dienst, den es noch nicht gibt, kommt der entweder als weiteres
   `@Bean` in `PlatformBeans` oder, falls er selbst Feature-übergreifende
   Logik trägt, als eigene `@Singleton`-Klasse dazu.
4. In `enable(ModuleContext context)` die gebrauchten Andockpunkte verdrahten:
   `Config` (inklusive `Config.getAs` für Zahlen) fürs Lesen der eigenen
   Werte (kein Andockpunkt auf `ModuleContext`, s. "Konfiguration lesen"
   oben), dazu
   `context.items().register(...)`, `context.commands().register(...)`,
   `context.navigator().add(...)`, `context.listen(...)`/
   `listenIncludingCancelled(...)`, `context.tasks()`.
5. Tests schreiben, bevor (oder während) der Code entsteht: Unit-Tests für die
   reine Logik und die Config-Validierung, ein Env-Integrationstest über
   `ModuleHarness` für alles, was einen `Player` braucht.
6. Falls das Feature Konfiguration hat: die neuen Schlüssel samt Standardwert
   in `app/src/main/resources/application.yaml` eintragen - das ist die
   einzige Stelle, an der der Standardwert steht (s. "Konfiguration lesen"
   oben) - und die Felder, ihre Standardwerte und Env-Variablen-Namen im
   README unter "Configuration Options Explained" bzw. "Environment variable
   reference" dokumentieren.

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
