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
`app/src/main`), damit es nie in `Titan`s Modulliste landet.

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
context.listenIncludingCancelled(InventoryPreClickEvent.class, this::onClick);
```

(`NavigatorModule#enable`). Diese Variante liefert das Event auch dann, wenn
es bereits abgebrochen ist. Sie ist nötig, wenn zwei Module auf dasselbe
`CancellableEvent` reagieren und eines davon es bedingungslos abbricht -
`feature.protection.ProtectionModule` bricht z. B. jedes
`InventoryPreClickEvent` ab. Mit `listen` würde `NavigatorModule`s eigener
Klick-Handler dann davon abhängen, welches der beiden Module zuerst
eingeschaltet wurde; `listenIncludingCancelled` macht das Verhalten
unabhängig von der Reihenfolge (s. `lobby-modules`-Spec, "Module sind
voneinander unabhängig"). Der Handler sieht `isCancelled()` weiterhin selbst
und kann - wie `NavigatorModule#onClick` - trotzdem `setCancelled(true)`
setzen.

Faustregel: `listen`, solange ein anderes Modul das Event nicht schon
abbrechen könnte; `listenIncludingCancelled` nur, wenn der Handler wirklich in
jedem Fall laufen muss.

### `config` - den eigenen `app.json`-Abschnitt lesen

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
abgelehnt wird - nie den Abschnitt, aus dem er geladen wurde. Deshalb wirft
der Compact Constructor `ConfigException.invalid(field, reason)`;
`ConfigStore` ergänzt Abschnitt und Datei, bevor die Ausnahme
`ModuleRegistry.enableAll()` verlässt und den Start abbricht - mit Modul, Feld
und Grund in der Meldung. `context.config` liest immer nur den **eigenen**
Abschnitt (die Modul-`id()`); es gibt keine Überladung für einen anderen
Abschnitt. Ohne konfigurierten `ConfigStore` (z. B. im
`ModuleHarness.startStandalone`-Testaufbau) liefert `config` unverändert
`defaults` zurück. Wie `listen` funktioniert `config` nur während `enable()`.

`ConfigStore.flush()` (aufgerufen von `ModuleRegistry.enableAll()` nach dem
Start aller Module) schreibt `app.json` nur, wenn die Datei bei
`ConfigStore.open()` neu angelegt oder aus dem Legacy-Format migriert wurde
(s. `ConfigStore#flush`/`#writeOnFlush`). Läuft ein neues Modul gegen ein
bereits bestehendes, aktuelles `app.json`, bekommt es seine Defaults nur im
Arbeitsspeicher - sein Abschnitt taucht in der Datei erst auf, sobald ihn
jemand tatsächlich setzt (z. B. über den Setup-Server, der `ConfigStore.save()`
explizit aufruft). Für ein neues Feature bedeutet das: seinen Abschnitt samt
Defaults im README dokumentieren (s. Checkliste, Schritt 6) und, falls
Betreiber ihn anpassen sollen, in derselben PR in `app.json` ergänzen -
sonst bleibt er unsichtbar, bis jemand ihn über den Setup-Server anfasst.

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
   Maßstab, hinter `NavigatorModule`s `SharedNavigatorInventory`: Das geteilte
   Inventar wird nur neu gebaut, wenn sich `NavigatorEntries.version()`
   geändert hat, nicht bei jedem Öffnen.
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
`ExampleConfig` direkt, ohne `ConfigStore`.

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
- Beide gibt es mit einer Überladung, die einen `ConfigStore` (oder einen
  `Path` auf eine `app.json`) entgegennimmt, für Tests, die
  `context.config(...)` abdecken sollen - `ExampleModuleTest` liest so eine
  temporäre `app.json` über `@TempDir`.
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

## Checkliste: neues Feature = neues Paket + eine Zeile

1. Neues Paket `app/src/main/java/net/onelitefeather/titan/app/feature/<name>/`
   anlegen - `app/src/test/.../feature/example/` als Kopiervorlage nehmen.
2. `<Name>Module` (public, implementiert `LobbyModule`) und, falls das Feature
   Konfiguration braucht, `<Name>Config` (public record, Defaults +
   Validierung über `ConfigException.invalid` im Compact Constructor)
   anlegen. Alles andere - Handler, reine Logik, Item-/Tag-Konstanten - bleibt
   paketprivat.
3. In `enable(ModuleContext context)` die gebrauchten Andockpunkte verdrahten:
   `context.config(...)`, `context.items().register(...)`,
   `context.commands().register(...)`, `context.navigator().add(...)`,
   `context.listen(...)`/`listenIncludingCancelled(...)`, `context.tasks()`.
4. Tests schreiben, bevor (oder während) der Code entsteht: Unit-Tests für die
   reine Logik und die Config-Validierung, ein Env-Integrationstest über
   `ModuleHarness` für alles, was einen `Player` braucht.
5. **Genau eine Zeile** in `Titan.java`s Modulliste ergänzen -
   `new <Name>Module(...)` in der `.modules(...)`-Aufzählung des
   `ModuleRegistry.builder()`. Keine andere Datei, kein anderes Feature-Paket
   ändert sich dafür (s. `lobby-modules`-Spec, Szenario "Beispielmodul aus der
   Vorlage").
6. Falls das Feature einen `app.json`-Abschnitt hat: die neuen Felder samt
   Defaults im README unter "Configuration Options Explained" dokumentieren -
   `ConfigStore.flush()` schreibt eine bestehende, aktuelle `app.json` nicht
   automatisch neu (s. "config" oben), der Abschnitt läuft bis dahin nur mit
   Defaults im Speicher. Sollen Betreiber ihn anpassen können, den Abschnitt
   zusätzlich in derselben PR in `app.json` ergänzen.

`ExampleModule` selbst bleibt test-only und taucht deshalb nicht in
`Titan.java` auf - als reguläres Feature bräuchte es genau den einen Eintrag
aus Schritt 5, sonst keine Änderung außerhalb seines eigenen Pakets.
