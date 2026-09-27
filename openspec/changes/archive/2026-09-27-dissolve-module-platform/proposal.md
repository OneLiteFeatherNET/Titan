# Proposal

## Why

Titan hat eine eigene Modul-Plattform im Paket `app/.../module/`: 21 Klassen mit rund 1.540 Zeilen, dazu `ModuleHarness`, `ModulePlatformFixture` und etwa 660 Zeilen Doku. Sie bildet einen eigenen Lebenszyklus nach (`LobbyModule`, `ModuleContext`, `ModuleRegistry`), hat Andockpunkte mit je einer Implementierung (`ModuleItems`, `ModuleTasks`, `ModuleCommands`) und prüft Konflikte mit zwei Detektoren und zwei Exceptions. Genutzt wird davon wenig. Die 7 Features melden zusammen 3 Items und 1 Task an, Befehle keinen einzigen. Das meiste davon liefert der DI-Container Avaje Inject schon selbst: `@PostConstruct` und `@PreDestroy` für den Lebenszyklus, Listen-Injektion zum Sammeln von Beiträgen, `@RequiresProperty` und `@Profile` für bedingte Beans. Titan soll wie eine Micronaut-Anwendung aufgebaut sein. Features sind normale Beans und nutzen den Container direkt statt einer eigenen Plattform.

## What Changes

- **BREAKING (Entwickler-API)**: `LobbyModule`, `ModuleContext`, `ModuleRegistry`, `ModulePlatform` und `ModuleLifecycleException` entfallen, ebenso die Andockpunkte `ModuleItems`, `ModuleTasks` und `ModuleCommands` mit ihren `*Impl`. Jedes Feature ist eine `@Singleton`-Bean. Es meldet seine Listener in `@PostConstruct` an einem eigenen Event-Knoten an und hängt ihn in `@PreDestroy` wieder ab.
- Die Reihenfolge, in der Features Events verarbeiten, legt die Priorität ihres Minestom-Event-Knotens fest. Die heutigen `@Priority`-Werte werden übernommen. Eine Start- und Stopp-Reihenfolge zwischen Features gibt es nicht mehr, weil sie unabhängig voneinander sind.
- Lobby-Items werden Beans: Ein Feature stellt ein `LobbyItem` bereit, und eine Plattform-Bean `LobbyItems` bekommt alle per Listen-Injektion. Sie rüstet Spieler aus, leitet Benutzungen weiter und bricht beim Aufbau ab, wenn zwei Items denselben Platz oder Schlüssel haben. `ItemRegistry`, `EquipPlan`, `DuplicateItemKeyDetector`, `SlotConflictDetector`, `DuplicateItemKeyException` und `ItemPlacementConflictException` gehen darin auf.
- Den einen wiederkehrenden Task (Elytra) plant das Feature direkt über den injizierten Minestom-`Scheduler` und bricht ihn in `@PreDestroy` ab.
- Der Befehls-Andockpunkt entfällt ersatzlos, weil er keine Nutzer hat. Befehle wie `StopCommand` bleiben, wo sie sind.
- Fehler in Listenern werden weiterhin dem Feature und dem Spieler zugeordnet (`TitanObservability.guard(featureId, …)`).
- Test-Infrastruktur: `ModuleHarness` und `ModulePlatformFixture` entfallen. Feature-Tests bauen das Feature direkt mit Fakes und einem Test-Event-Knoten.
- ArchUnit-Regeln, `docs/lobby-modules.md` und `README.md` werden auf „Features sind Beans“ umgestellt.
- Für Spieler ändert sich nichts.

## Capabilities

### New Capabilities

_Keine._

### Modified Capabilities

- `lobby-modules`: Keine Start- und Stopp-Reihenfolge mehr. Stattdessen starten Features vor dem ersten Spieler, die Event-Reihenfolge ist festgelegt, und beim Herunterfahren trennt sich ein Feature zuerst von den Events. „Abschalten hinterlässt keine Reste“ verliert die Befehle. Ein neues Feature ist nur noch ein neues Paket, ohne Eintrag in einer Modulliste. Die Unabhängigkeit gilt über Plattform-Beans statt über Andockpunkte.
- `lobby-hotbar`: Items werden als Beans bereitgestellt statt über einen Modulkontext angemeldet. Platzkonflikte nennen den Platz und beide Items.

## Impact

- **Code**: `app/src/main/java/net/onelitefeather/titan/app/module/` schrumpft auf `LobbySpawn` und das Item-Paket (`LobbyItem`, `ItemSlot`, `LobbyItems`), dazu ein kleiner Helfer für Feature-Event-Knoten. Alle 7 Features (`protection`, `spawn`, `respawn`, `navigator`, `sit`, `tickle`, `elytra`) werden umgestellt. Außerdem ändern sich `Titan.java` (keine Modulliste mehr, `BeanScope.close()` vor Butterfly) und `PlatformBeans` (neu: `Scheduler`, `LobbyItems`).
- **Tests**: `ModuleHarness`, `ModuleHarnessTest`, `ModulePlatformFixture`, `ModuleRegistryTest`, `ModuleContextTest`, `ModuleContextItemsWiringTest` und die Detektor-/Exception-/`EquipPlan`-Tests entfallen oder gehen in `LobbyItemsTest` auf. Die Feature-Tests werden auf direkten Aufbau umgestellt. `ArchitectureTest` bekommt neue Regeln.
- **Doku**: `docs/lobby-modules.md` wird neu geschrieben und deutlich kürzer, `README.md` wird angepasst.
- **Abhängigkeiten**: keine neuen. Avaje Inject 12.7 ist bereits eingebunden.
- **Nutzertexte**: keine Änderung.
- **Folge-Changes**: Features lassen sich danach per `@RequiresProperty` bedingt aktivieren. Das ist hier nur als Möglichkeit angelegt und wird nicht umgesetzt. Das Permission-Gate für Navigator-Ziele bleibt ein eigener Change.

## Delivery

PR-Titel: `refactor(app)!: replace the lobby module platform with plain avaje beans`

Footer:

```
BREAKING CHANGE: `LobbyModule`, `ModuleContext`, `ModuleRegistry` and the `ModuleItems`/`ModuleTasks`/`ModuleCommands` extension points are removed. Features are `@Singleton` Avaje beans that attach their own event node in `@PostConstruct` and detach it in `@PreDestroy`; lobby items are `LobbyItem` beans collected by `LobbyItems`. Player-facing behaviour is unchanged.
```
