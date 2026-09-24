# Proposal

## Why

Titans Code ist nach technischen Schichten geschnitten (`listener/`, `helper/`, `utils/`, `config/`), nicht nach Features. Ein Feature wie „Sitzen“ verteilt sich dadurch auf drei Gradle-Module und rund zehn Dateien. Jedes neue Feature ändert außerdem `Titan.java`, die monolithische `AppConfig` (vier Klassen), `Items`/`Tags` und den Setup-Server.

Mit den anstehenden Features (Freunde, Stats, Voyager-Teaser, Portale, echte Weltzeit) wächst dieser Aufwand weiter. Die Struktur muss sich deshalb *vor* diesen Features ändern: Neue Features sollen schnell, test-first und ohne spätere Umbauten entstehen.

Der Branch `feat/lobby-season` enthält bereits ein Modulsystem. Diese Change baut die Architektur auf Wunsch neu auf `main` auf und nutzt den Branch nur als Ideenquelle (`LobbyModule`, `ModuleContext`, `ModuleTasks`).

**Auslieferung:** `feat(app)!: rebuild the lobby around feature modules` (Conventional Commit, PR-Titel), mit dem Footer `BREAKING CHANGE: app.json uses one section per module; the old flat file is migrated on first start and kept as app.json.v1.bak`.

## What Changes

- **Modulsystem:** Jedes Lobby-Feature wird ein Modul mit `id`, `enable(context)` und `disable()`. Die Registry gibt jedem Modul einen eigenen `EventNode` und eine eigene Task-Liste. Beim Herunterfahren hängt sie beides in umgekehrter Reihenfolge ab bzw. bricht die Tasks ab.
- **Paket pro Feature:** Jedes Modul lebt in genau einem Paket `net.onelitefeather.titan.app.feature.<name>`. Dazu gehören seine Listener, Items, Tags, der Config-Record und die Tests. Implementierungsklassen sind package-private.
- **Andockpunkte im `ModuleContext`:** Module *melden sich an* statt gemeinsame Klassen zu ändern:
  - Config-Abschnitt (`context.config(Typ.class)`)
  - Hotbar-Items mit Slot-Konfliktprüfung und Klick-Dispatch
  - Navigator-Einträge
  - Befehle

  Alles Angemeldete räumt das Modulsystem bei `disable()` automatisch weg.
- **Config pro Modul:** Jedes Modul liest seinen eigenen Abschnitt aus `app.json` als Java-Record, mit Defaults und Validierung. Ungültige Werte brechen den Start mit einer klaren Fehlermeldung ab.
  - **BREAKING (Betreiber):** `app.json` wechselt von flachen Schlüsseln auf Abschnitte pro Modul. Eine Altdatei im flachen Format wird beim Start automatisch migriert.
  - Der tote Schlüssel `updateRateAgones` entfällt.
- **Bestehende Features migrieren:** Diese Features werden zu Modulen:
  - Schutz (Abbrechen von Item-, Block- und Inventar-Aktionen)
  - Spawn und Höhen-Teleport
  - Sitzen
  - Kitzeln
  - Elytra (Start, Stopp, Boost)
  - Navigator
  - Tod und Respawn

  Das Verhalten für Spieler bleibt gleich. Charakterisierungstests halten es vor dem Umzug fest.
- **Navigator als Registry:** Die vier hartcodierten Ziele werden Einträge, die das Navigator-Modul aus seiner Config liest. Andere Module können eigene Einträge beisteuern. Der Navigator registriert zur Laufzeit keine Inventar-Listener mehr pro Spieler.
- **Architekturregeln als Test:** ArchUnit prüft drei Regeln:
  - Feature-Module hängen nicht voneinander ab.
  - `common` hängt nicht von Features ab.
  - Feature-Code liegt nur unter `feature/`.
- **Feature-Vorlage:** Ein dokumentiertes Beispielmodul (Config-Record, Item, Test) dient als Startpunkt für neue Features.
- **Entfernt:**
  - `AppConfigBuilder`, `AppConfigImpl`, `InternalAppConfig` und die monolithische `AppConfig`
  - die zentralen `common/utils/Tags` und `common/utils/Items`
  - `NavigationHelper`
  - die Handverdrahtung in `Titan.initListeners()`

**Nicht Teil dieser Change** (jeweils eigene Folge-Changes):
- TPS/MSPT-Metriken
- Session-Muster für asynchrone Spielerdaten (Freunde/Stats)
- der Tickle-Cooldown-Bug
- Portierung von Portalen, Weltzeit und Saison aus `feat/lobby-season`

## Capabilities

### New Capabilities
- `lobby-modules`: Lebenszyklus von Lobby-Modulen. Dazu gehören Registrierungsreihenfolge, eigener EventNode und eigene Tasks pro Modul, Aufräumen beim Herunterfahren, Befehlsregistrierung und die Architekturgrenzen zwischen Modulen.
- `lobby-module-config`: Config-Abschnitt pro Modul in `app.json`. Dazu gehören Defaults, Validierung beim Start, automatische Migration des alten flachen Formats und das Speichern von Änderungen aus dem Setup-Server.
- `lobby-hotbar`: Von Modulen angemeldete Hotbar-Items. Slot-Konflikte werden beim Start erkannt, Klicks gehen an das Modul, dem das Item gehört.
- `lobby-navigator`: Navigator-Ziele als registrierte Einträge, aus der Config oder von Modulen beigesteuert, ohne Listener-Registrierung pro Spieler zur Laufzeit.

### Modified Capabilities
<!-- Keine bestehenden Specs unter openspec/specs/. -->

## Impact

- **Code:**
  - `app`: neue Pakete `module/` und `feature/<name>/`. `Titan.java` schrumpft auf die Composition Root. `listener/` und `helper/` entfallen.
  - `common`: `config/` wird ersetzt. `utils/Tags` und `utils/Items` entfallen. `helper/SitHelper` wandert ins Sit-Modul. Erhalten bleiben `map/`, `blockhandler/`, `deliver/`, `observability/` und `permission/`.
  - `setup`: `AppCommand` schreibt in die neuen Config-Abschnitte.
- **Konfiguration:** `app.json` bekommt ein neues Format mit automatischer Migration. `updateRateAgones` entfällt.
- **Abhängigkeiten:** `com.tngtech.archunit:archunit-junit5` kommt als Test-Abhängigkeit dazu.
- **Tests:** Die bestehenden Listener-Tests ziehen in die Feature-Pakete um. Neu dazu kommen Tests für Registry, Config, Hotbar, Navigator und die Architekturregeln.
- **Laufzeit:** keine neuen Classloader, keine Extensions. Die Module bleiben im App-Jar, damit der AOT-Cache (JDK 25) weiter greift.
- **Spieler:** keine sichtbare Änderung. Items, Slots, Navigator-Ziele und das Verhalten bleiben identisch.
