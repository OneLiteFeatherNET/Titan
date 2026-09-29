# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/lobby-navigator`.

Ist-Zustand (`features/navigator`):

- `Destination` ist ein Enum mit festen Plätzen 0 (ElytraRace), 4 (Survival), 5 (Slender, Flag `NAVIGATOR_SLENDER`) und 8 (Creative, Task `MemberBuild`). **Platz 7 ist frei** (heute graue Glasscheibe). `Destination.visible(FeatureFlags)` filtert nach Flag.
- `NavigatorModule` (`@Singleton`) besitzt genau einen `GlobalInventoryBuilder`, ruft `builder.register()` in `@PostConstruct` und `builder.unregister()` in `@PreDestroy`. `open(player)` ruft `applyLayoutIfChanged()` (`synchronized`, vergleicht die sichtbare Zielliste mit `appliedVisible`, setzt dann Layout und `invalidateLayout()`) und öffnet `builder.getInventory()`. Der Klick-Handler je Ziel ruft `Deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName(task).player(player).build())` und schließt das Inventar.
- `package-info.java`: `@InjectModule(name = "navigatorColumn", requires = {EventNode.class, Deliver.class, FeatureFlags.class}, ...)`. `ColumnArchitectureTest` erzwingt Listener nur über `FeatureNode`, `@PostConstruct` nur an `@Singleton`, kein `BeanScope` und **kein `io.avaje.config`** im Navigator.
- `PermissionService` (`core`, `check(UUID, String)` liefert `ALLOWED`/`DENIED`/`NOT_SET`). `runtime` deklariert ihn in `provides`; `platform/luckperms` liefert `LuckPermsPermissionService`, sonst greift `DenyAllPermissionService` (`@Secondary`, immer `NOT_SET`). Bisher verlangt keine Column den Dienst (`TitanPlayer` in `runtime` nutzt ihn).
- Tests: `NavigatorFixture.start(env, deliver, flags)` baut Modul und Feder wie in Produktion; `RecordingDeliver`, `FakeFeatureFlags`; `NavigatorModuleLeakTest` zählt Listener am Knoten des Moduls und am `eventNode()` des geteilten Inventars.

Befunde, die geprüft wurden und **keinen** Konflikt ergeben:

- Die Column darf `PermissionService` verlangen: Er liegt in `core` (die Column hängt schon daran), ist weder generisch noch `@Named`, also reicht `requires = {..., PermissionService.class}` ohne `requiresString`. Keine Regel in `ColumnArchitectureRules` oder im `ColumnArchitectureTest` spricht dagegen; ohne Plattform erfüllt `runtime`s Fallback die Anforderung.
- Zwei Inventare machen die Neuberechnung nicht kompliziert: Der Zustand `appliedVisible` gehört pro Inventar und wird pro Inventar synchronisiert (D2). Kein gemeinsamer Zustand zwischen beiden.
- Platz 7 ist unbelegt; das öffentliche Inventar behält dort seine Glasscheibe.

## Goals / Non-Goals

**Goals:**
- Berechtigte sehen `BUILD`, alle anderen ein Menü, das sich durch nichts vom heutigen unterscheidet.
- Das Recht wird beim Öffnen und beim Klick geprüft; nie pro Öffnung entstehen Inventare, Listener oder Objekte, die bleiben.
- Bestehendes Verhalten (Flag-Neuberechnung beim Öffnen, Klick-Ablauf, Leak-Freiheit) bleibt für beide Inventare gleich.

**Non-Goals:**
- Konfigurierbarkeit von Recht, Platz, Icon oder Task; Liste einzelner Server; Meldung an den Spieler bei entzogenem Recht; ein offenes Team-Inventar sofort umbauen, wenn das Recht entzogen wird (siehe Risiken).

## Decisions

### D1: `BUILD` als festes Ziel mit optionalem Recht

`Destination` bekommt ein weiteres Feld `@Nullable String permission` und den Eintrag `BUILD(7, Material.SCAFFOLDING, "<!i><gold>Build", "Build", null, "titan.navigator.buildserver")`. Der Wert `titan.navigator.buildserver` ist eine Konstante im Enum, nicht Konfiguration (die Regel „kein `io.avaje.config`“ bleibt erfüllt). `visible(FeatureFlags)` bleibt für die Flags zuständig; neu ist `visible(FeatureFlags, boolean withPermissioned)`, das Ziele mit Recht nur bei `true` einschließt. Das öffentliche Inventar nutzt `false`, das Team-Inventar `true`.

**Annahmen (vom Nutzer nicht bestätigt, ändern weder Specs-Struktur noch Aufgaben):** Platz 7 (neben Creative), Icon `Material.SCAFFOLDING`, Anzeigename „Build“ im Stil der übrigen Ziele (`<gold>`), kein Feature-Flag.
Built-in geprüft: Enum und `Material` reichen; keine Registry nötig. Verworfen: Ziele aus der Konfiguration (widerspricht der bestehenden Anforderung „im Modul festgelegt“) und eine Liste einzelner CloudNet-Dienste (PR #219; braucht Diensterkennung, `:bridge`-Halter und Aktualisierung, während der Task `Build` CloudNets Routing nutzt).
SOLID: OCP im Kleinen (ein Eintrag docken an `visible`/Layout an, ohne den Klick-Ablauf zu ändern); SRP bleibt: `Destination` beschreibt, `NavigatorModule` verdrahtet.
Test: Unit `NavigatorDestinationTest` (Platz 7, Task `Build`, paarweise verschiedene Plätze, `visible` mit und ohne Recht, mit Slender-Flag an/aus).

### D2: Zwei geteilte Inventare, je einmal gebaut

`NavigatorModule` hält zwei Aves-`GlobalInventoryBuilder` (öffentlich, Team), jeden in einer kleinen paketinternen Hülle `SharedNavigator` (Builder, `withPermissioned`, `appliedVisible`). Beide werden im Konstruktor gebaut, in `@PostConstruct` mit `register()` angemeldet und in `@PreDestroy` mit `unregister()` abgemeldet. `open(player)` wählt per D3 die Hülle, ruft deren `applyLayoutIfChanged()` (`synchronized` auf der Hülle, sonst wie heute) und öffnet ihr Inventar. Beide tragen den Titel „Navigator“.

Warum zwei statt eines mit Umbau pro Spieler: Ein Inventar ist für alle geteilt; ein Umbau je Öffnung würde einem gleichzeitigen zweiten Spieler den falschen Stand zeigen. Ein Inventar je Spieler oder je Öffnung ist ausdrücklich ausgeschlossen (Listener- und Objekthaufen, Regel des Nutzers und Anforderung „Öffnen häuft nichts an“).
Built-in geprüft: Aves' `GlobalInventoryBuilder` ist bereits das geteilte Inventar; ein eigener Cache entfällt. Die Flag-Neuberechnung bleibt je Hülle unverändert, also gilt sie für beide Inventare.
SOLID: SRP (Hülle kapselt Builder plus Stand), DRY (dieselbe `toAvesLayout` für beide, nur die Zielliste unterscheidet sich).
Test: Integration (Cyano-`Env`): Berechtigter sieht Platz 7, andere nicht; Flag-Wechsel wirkt auf beide Inventare beim nächsten Öffnen; `NavigatorModuleLeakTest` zählt Listener an beiden Inventar-Knoten und am Modulknoten über 50 Öffnungen und 100 Spieler; nach `stop()` reagiert keines der beiden Inventare mehr.
**Zu prüfen als erster Test:** Dass zwei angemeldete `GlobalInventoryBuilder` sich nicht gegenseitig die Klicks zuordnen (jeder hängt am eigenen `eventNode()` seines Inventars). Bricht das, wird es vor der Umsetzung gemeldet, nicht still umgangen.

### D3: Recht über `PermissionService`, beim Öffnen und beim Klick

`NavigatorModule` bekommt `PermissionService` per Konstruktor. Eine Methode `isAllowed(UUID, Destination)` liefert `destination.permission() == null || service.check(uuid, permission) == ALLOWED`; `NOT_SET` und `DENIED` gelten gleich als nicht erteilt (`lobby-permissions`). Beim Öffnen: `ALLOWED` für das Recht von `BUILD` → Team-Inventar, sonst öffentliches. Im Klick-Handler eines Ziels mit Recht: nochmals prüfen; ist es weg, `cancelClick`, **keine** Weiterleitung, `player.closeInventory()`, keine Chat-Nachricht. Ein Spieler, der im Team-Inventar sitzt, dem das Recht entzogen wurde, sieht das Symbol also bis zum Schließen, kann es aber nicht mehr nutzen.
`Deliver` bleibt unverändert: `DeliverComponent.taskBuilder().taskName("Build")` über denselben Pfad wie die anderen Ziele; kein `GuardedDeliver`, kein FeatureGate.
Die Prüfung läuft synchron auf dem Tick-Thread; `LuckPermsPermissionService` und `DenyAllPermissionService` antworten aus dem Speicher (das ist der Vertrag von `check`), also keine Blockierung.
Built-in geprüft: `PermissionService` ist der vorhandene Vertrag (`permission-spi`); Minestoms `Player#hasPermission` entfällt, weil `TitanPlayer` dasselbe nur über denselben Dienst leitet und der Test dann einen Spieler mit Verbindung bräuchte.
SOLID: DIP (Konstruktor, Schnittstelle aus `core`), ISP (nur `check` wird benutzt).
Test: Unit/Integration mit einer `FakePermissionService` je Test (frisch, Ergebnis pro UUID einstellbar): `ALLOWED` → Team-Inventar; `NOT_SET`/`DENIED` → öffentliches; Recht zwischen Öffnen und Klick entzogen → `RecordingDeliver` leer, Inventar geschlossen, Klick abgebrochen; Recht erteilt → genau eine Weiterleitung an `Build`.

### D4: Column verlangt `PermissionService`

`package-info.java`: `requires = {EventNode.class, Deliver.class, FeatureFlags.class, PermissionService.class}`, `requiresString` und `provides` unverändert. `docs/lobby-modules.md` (Tabelle der Columns) ergänzt die Zeile `navigator`.
Test: bestehende Verdrahtungstests in `apps/cloudnet` und der Starttest von `apps/local` laufen unverändert grün (Kontrolle beim Build); `ColumnArchitectureTest` bleibt unverändert und grün.
SOLID: DIP.

### D5: Logging, Metriken, Spans, Nutzertexte

- Keine neuen Logs, Metriken oder Spans: Öffnen und Klick sind pro Spieleraktion und gehören nach der Regel nicht in Logs oberhalb `DEBUG`; ein abgewiesener Klick ist ein Randfall ohne Betreiberhandlung.
- Nutzertext: Der Anzeigename „Build“ steht wie die übrigen Ziele als MiniMessage-Literal im Code. Das weicht von der Regel „Text aus Übersetzungsdateien“ ab und bleibt bewusst so, weil alle Ziele heute Eigennamen sind und eine Übersetzung der Destinationsnamen ein eigener Change wäre. Keine Chat-Nachricht.

## Risks / Trade-offs

- [Zwei Aves-Builder stören sich gegenseitig (D2)] → erster Test prüft das; bei Fehlschlag Meldung an den Nutzer vor der Umsetzung.
- [Ein bereits geöffnetes Team-Inventar zeigt `BUILD` nach Rechtentzug weiter] → Klick prüft erneut und blockt (US-5.03); Umbau des offenen Inventars ist Non-Goal.
- [`NavigatorDestinationTest.visibleIncludesSlenderWhileItsFlagIsOn` erwartet „alle Ziele“] → der Test ändert sich mit `BUILD`: Er erwartet alle Ziele ohne Recht bzw. alle mit Recht; die Anpassung ist Teil von Aufgabe 2.
- [Task `Build` existiert in CloudNet nicht] → die Weiterleitung schlägt wie bei jedem fehlenden Task im vorhandenen Pfad fehl; Betriebsvoraussetzung, siehe Migration.
- [Ohne Plattform (`local`) sieht niemand `BUILD`] → gewollt; `local` braucht keinen Zugang zu Build-Servern.

## Migration Plan

1. In CloudNet den Task `Build` anlegen (Build-Server, getrennt von `MemberBuild`).
2. In LuckPerms `titan.navigator.buildserver` der Build-Gruppe erteilen.
3. `titan-cloudnet.jar` deployen (AOT-Cache neu trainieren). Ohne Recht ändert sich für niemanden etwas.
4. Zurück: Recht entziehen (wirkt beim nächsten Öffnen bzw. Klick) oder Revert des Squash-Commits.
