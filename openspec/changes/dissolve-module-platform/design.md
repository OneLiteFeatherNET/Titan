# Design

## Context

Stand auf `main` (nach #306):

- `Titan` baut eine `BeanScope` und holt sich `listByPriority(LobbyModule.class)`. Daraus baut es eine `ModuleRegistry`, die jedes Modul mit einem eigenen `ModuleContext` startet: ein Event-Knoten `titan/<id>` unter dem gemeinsamen Knoten `@Named("titan")`, außerdem Tasks, Befehle, Items und Cleanup-Hooks. Heruntergefahren wird in umgekehrter Reihenfolge. Beim Shutdown laufen nacheinander: Module abschalten, Butterfly beenden, `BeanScope.close()`.
- `ModuleContext#listen` hüllt jeden Listener in `TitanObservability.guard(moduleId, …)`, damit Fehler dem Modul und dem Spieler zugeordnet werden.
- `ItemRegistry` (eine Bean) hängt einen `PlayerUseItemEvent`-Listener an den Knoten `titan` und leitet Benutzungen über den Tag `titan:item` an den Handler des Items weiter. `equip(player)` legt die Items über `EquipPlan` auf ihre Plätze. `validate()` erkennt über zwei Detektoren doppelte Schlüssel und Plätze.
- Genutzt werden: 3 Items (Feder `Hotbar(4)`, Elytra `Equipment(CHESTPLATE)`, Feuerwerk `Unplaced`), 1 Task (Elytra) und 0 Befehle.
- Avaje Inject 12.7 bringt mit: `@PostConstruct`, `@PreDestroy(priority)`, Listen-Injektion, `@Factory`/`@Bean`, `@RequiresProperty`, `@Profile` und `BeanScope.builder().forTesting()`. Kein `@Scheduled`. Die Reihenfolge der `@PreDestroy`-Aufrufe beim Schließen der `BeanScope` ist nicht dokumentiert.

Motivation: siehe proposal.md, Why.

## Goals / Non-Goals

**Goals:**
- Features sind normale Avaje-Beans und nutzen den Lebenszyklus des Containers. Die Plattform besteht nur noch aus Beans, die Features injizieren: den Titan-Event-Knoten, `LobbyItems`, `LobbySpawn`, `Scheduler`, `Deliver` und `FeatureFlags`.
- Keine Liste, die ein neues Feature kennen müsste.
- Spielerverhalten, Fehlerzuordnung und das Leck-Verhalten bleiben gleich.

**Non-Goals:**
- Features per Konfiguration an- und abschalten (`@RequiresProperty`). Das Design lässt es zu, eingebaut wird es nicht.
- Minestom-Events über `avaje-inject-events` (`@Observes`) verteilen (siehe Entscheidung 1).
- Das Permission-Gate für den Navigator und i18n.
- Verhalten einzelner Features ändern.

## Decisions

### 1. Jedes Feature hängt einen eigenen Minestom-Event-Knoten an, über den Helfer `FeatureNode`

`FeatureNode` ist eine kleine, finale Plattform-Klasse (ohne statischen Zustand) in `app.module`:

```java
final class/record FeatureNode implements AutoCloseable {
    static FeatureNode attach(EventNode<Event> parent, String featureId, int priority);
    <E extends Event> FeatureNode on(Class<E> type, Consumer<E> listener);              // mit guard(featureId)
    <E extends Event> FeatureNode onIncludingCancelled(Class<E> type, Consumer<E> listener);
    void close();                                                                       // parent.removeChild(node)
}
```

So sieht ein Feature aus:

```java
@Singleton
public final class TickleModule {
    static final int EVENT_PRIORITY = 600;
    private final EventNode<Event> titan; private final Clock clock;
    private FeatureNode node;

    TickleModule(@Named("titan") EventNode<Event> titan, Clock clock) { … }

    @PostConstruct void start() {
        Config.getAs(TickleSettings.COOLDOWN_KEY, TickleSettings::cooldownMillis); // Startprüfung wie heute
        this.node = FeatureNode.attach(this.titan, "tickle", EVENT_PRIORITY)
                .on(EntityAttackEvent.class, new TickleAttackHandler(this.clock));
    }

    @PreDestroy void stop() { this.node.close(); }
}
```

- **Built-in first**: Minestom-`EventNode` mit `setPriority(int)` legt die Verarbeitungsreihenfolge zwischen Geschwisterknoten fest. Das ersetzt die Startreihenfolge der `ModuleRegistry`. Avajes `@PostConstruct`/`@PreDestroy` ersetzen `enable`/`disable`. Verworfen wurde `avaje-inject-events` (`@Observes`) als Brücke für Minestom-Events. Damit liefe jedes Event (auch Move- und Tick-nahe Events) zusätzlich über einen zweiten Dispatcher. Außerdem kennt es weder Minestoms Abbruch-Semantik (`ignoreCancelled`) noch die Knotenprioritäten. Die Knoten bringen beides mit. Ebenfalls verworfen: auf den Helfer verzichten und in jedem Feature `EventNode.all(...)` plus `guard(...)` direkt schreiben. Das wären 7 Kopien derselben Verdrahtung und der Fehlerzuordnung (DRY).
- **Reihenfolge**: `EVENT_PRIORITY` übernimmt die heutigen `@Priority`-Werte: protection 100, spawn 200, respawn 300, navigator 400, sit 500, tickle 600, elytra 700. Ein ArchUnit-/Reflection-Test prüft, dass die Werte eindeutig sind, und nennt bei doppelten Werten beide Features.
- **Herunterfahren**: `@PreDestroy` ruft zuerst `node.close()` auf und bricht dann Tasks ab. Erst danach läuft übrige Abschaltlogik (Spec „Features trennen sich zuerst von Events“). Auf eine Reihenfolge zwischen Features kommt es nicht an, weil sie unabhängig sind.
- **Laufzeit-Registrierung**: Den bisherigen Sperrmechanismus „Listening-Fenster“ gibt es nicht mehr. Die Spec „Keine Listener-Registrierung zur Laufzeit“ sichern weiterhin die Leck-Tests (100 Spieler, Listener-Anzahl gleich).
- **SOLID**: SRP (der Helfer verdrahtet und ordnet Fehler zu, das Feature enthält nur Fachlogik), DIP (Features bekommen alles per Konstruktor), OCP (neue Features docken an, ohne dass Plattformcode geändert wird).
- **Test**: Unit-Test für `FeatureNode` mit einem frischen `EventNode` ohne Server: `on` registriert, `close` hängt ab, eine Ausnahme im Listener wird gefangen, und die Meldung nennt Feature und Spieler (über einen gefangenen Appender bzw. den bestehenden `TitanObservability`-Test-Hook). Integrationstests pro Feature mit Cyano-`Env`.
- **Logging/Metriken/Spans**: keine neuen, `guard` bleibt unverändert.

### 2. Lobby-Items als Beans, gesammelt von `LobbyItems`

`LobbyItem` (key, ItemStack, `ItemSlot`, `ItemUseHandler`) bleibt ein Datentyp. Ein Feature stellt seine Items über eine `@Factory`-Klasse im eigenen Paket als `@Bean` bereit, zum Beispiel `NavigatorItems#navigatorFeather(NavigatorModule)`. `LobbyItems` ist eine Plattform-Bean und bekommt `List<LobbyItem>` und den Titan-Knoten injiziert. Der Konstruktor erledigt drei Dinge:
1. Er prüft, dass Schlüssel eindeutig sind und kein fester Platz doppelt belegt ist (`Unplaced` ist ausgenommen). Bei einem Konflikt wirft er eine `IllegalStateException`, die den Schlüssel bzw. den Platz und beide Items nennt.
2. Er versieht jedes Item mit dem Tag `titan:item`.
3. Er hängt den `PlayerUseItemEvent`-Dispatcher an.

`equip(Player)` leert das Inventar und legt die Items mit festem Platz ab. `stack(Key)` gibt ein Item ohne festen Platz heraus (für das Feuerwerk). `@PreDestroy` entfernt den Dispatcher.

- **Built-in first**: Avajes Listen-Injektion ersetzt die Registrierung über den Modulkontext (`ModuleItems`, `ModuleItemsImpl`). Ein Konflikt beim Bean-Aufbau bricht den Start über den Container ab, also ohne eigenen `validate()`-Schritt. Die beiden Detektoren, ihre Exceptions und `EquipPlan` werden zu je einer privaten Methode, denn eine zweite Nutzung gibt es nicht (YAGNI). Die Konfliktprüfung bleibt eine reine statische Funktion, damit sie ohne Server testbar ist.
- **Zyklen**: Die Feder braucht den Navigator (`open`), der Navigator braucht die Feder nicht. `LobbyItems` braucht keine Features. Spawn und Respawn brauchen `LobbyItems`. Es entsteht kein Zyklus.
- **SOLID**: SRP. `LobbyItems` ist allein für die Lobby-Items zuständig. Features hängen von `LobbyItems` nur zum Ausrüsten ab (Spawn, Respawn, Elytra-Feuerwerk).
- **Test**: Unit-Tests für die Konfliktfunktion (doppelter Platz, doppelter Schlüssel, `Unplaced` doppelt erlaubt, Meldungstexte). Integrationstest (`Env`) für Ausrüsten und Weiterleiten der Benutzung, einschließlich „gewöhnliche Feder löst nichts aus“.

### 3. Tasks direkt über den injizierten `Scheduler`

`PlatformBeans` stellt `Scheduler` bereit (`MinecraftServer.getSchedulerManager()`). Das Elytra-Feature plant seinen Task in `@PostConstruct` und ruft in `@PreDestroy` erst `node.close()`, dann `task.cancel()` auf.

- **Built-in first**: Minestoms `Scheduler`/`Task` reichen für einen Task. `ModuleTasks` und `ModuleTasksImpl` waren nur eine Hülle darum. Ein Micronaut-artiges `@Scheduled` gibt es in Avaje nicht, und für einen einzigen Task lohnt sich kein eigenes.
- **Test**: Integrationstest mit `Env`. Ticks werden explizit gesteuert, der Task läuft vor `stop()` und danach nicht mehr.

### 4. `Titan` ohne Modulliste; Shutdown über `BeanScope.close()`

`Titan` baut die `BeanScope`, und schon dabei laufen alle `@PostConstruct` durch, also bevor der Server Verbindungen annimmt. `ModuleRegistry`, `modules`, `enableAll` und `terminate` entfallen. Die Shutdown-Tasks werden so umgestellt, dass `beanScope::close` vor `butterfly::terminate` läuft. Damit werden Fehler beim Abschalten der Features noch gemeldet. Einen Startfehler eines Features meldet Avaje als Ausnahme beim Aufbau der `BeanScope`, und der Start bricht ab. Ein Test prüft, dass die Meldung die Feature-Klasse nennt.

- **Test**: Integrationstest „Wiring“ mit `BeanScope.builder().forTesting()`, falls Minestom-Abhängigkeiten sich per `bean(...)` ersetzen lassen. Sonst ein Smoke-Test, der die echte `BeanScope` aufbaut. Er prüft, dass alle 7 Features und `LobbyItems` existieren und dass es genau 3 Items gibt.

### 5. Entfernen, Test-Infrastruktur, Architekturregeln, Doku

- **Löschen**: `LobbyModule`, `ModuleContext`, `ModuleRegistry`, `ModulePlatform`, `ModuleLifecycleException`, `ModuleCommands(+Impl)`, `ModuleTasks(+Impl)`, `ModuleItems(+Impl)`, `ItemRegistry`, `EquipPlan`, beide Detektoren und beide Exceptions, jeweils mit ihren Tests. Außerdem `ModuleHarness`, `ModuleHarnessTest` und `ModulePlatformFixture`.
- **Feature-Tests** bauen das Feature direkt: frischer Parent-Knoten aus `env.process().eventHandler()`, Fakes, danach `start()`. Am Ende läuft `stop()` im `finally`- bzw. try-with-resources-Block (F.I.R.S.T.: unabhängig, kein geteilter Zustand).
- **ArchUnit** (`ArchitectureTest`):
  - Features hängen nicht voneinander ab (bleibt).
  - Plattform und `common` hängen nicht von Features ab (bleibt).
  - Klassen in `..feature..` registrieren Listener nur über `FeatureNode` und rufen `EventNode#addListener` nicht direkt auf (ersetzt Regel 4).
  - Jede Klasse mit `@PostConstruct` in `..feature..` ist `@Singleton` (ersetzt Regel 5).
  - Kein `BeanScope` in Features (bleibt).
  - Navigator ohne `io.avaje.config` (bleibt).
  - Die Werte von `EVENT_PRIORITY` sind eindeutig (ersetzt `modulePrioritiesAreUnique`).
  - Regel 3 (nur `*Module` öffentlich) wird zu „nur `*Module` und Avaje-`@Factory`-Klassen öffentlich“, falls Avaje dafür öffentliche Klassen verlangt.
- **Doku**: `docs/lobby-modules.md` wird neu geschrieben. Sie beschreibt: Feature = Bean, `FeatureNode`, Items als `@Bean`, Tasks über `Scheduler`, Tests, Architekturregeln und die Checkliste „nur ein neues Paket“. `README.md` wird angepasst.
- **SOLID/KISS**: Übrig bleiben 4 bis 5 Plattform-Klassen statt 21.

## Risks / Trade-offs

- [Die `@PreDestroy`-Reihenfolge ist undokumentiert] → Features sind unabhängig, und jedes trennt sich zuerst selbst. `LobbyItems` wird von Spawn und Respawn benötigt und deshalb vor ihnen erzeugt. Falls Avaje in umgekehrter Abhängigkeitsreihenfolge abbaut, wird es nach ihnen zerstört. Falls nicht, laufen nach dem Abbau keine Spieler-Events mehr, weil die Feature-Knoten bereits abgehängt sind. Ein Integrationstest schließt die `BeanScope` und prüft, dass danach keine Feature-Listener mehr hängen.
- [Ein Startfehler eines Features erscheint als Avaje-Ausnahme statt als `ModuleLifecycleException`] → Die Meldung nennt die Bean-Klasse. Ein Test sichert das ab (Spec „Fehler beim Start eines Features“).
- [Weniger Leitplanken: kein Listening-Fenster mehr] → Die ArchUnit-Regel und der Leck-Test decken das Risiko ab.
- [Großer Diff über alle Features] → Umsetzung in Wellen. Alte und neue Plattform existieren zeitweise nebeneinander (siehe tasks.md). Jede Welle endet mit einem grünen Build.
- [Umstellung `beanScope.close()` vor `butterfly.terminate()`] → Zuvor liefen die Feature-Abschaltungen ebenfalls vor Butterfly. Die Reihenfolge bleibt also gleich.

## Migration Plan

1. Umsetzung in Wellen laut tasks.md, ein PR.
2. Kein Betreiber-Eingriff nötig, Konfiguration und Verhalten bleiben gleich.
3. Rollback: Revert des PRs.
