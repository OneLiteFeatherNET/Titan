# Proposal

## Why

Titan ist ein einziges Gradle-Modul `:app`, in dem alle Lobby-Features, die Plattform-Beans, LuckPerms, Butterfly und der Extension-Loader zusammen stecken. Die Features sind fachlich schon entkoppelt (kein Feature importiert ein anderes, ArchUnit-Regel `featureModulesDoNotDependOnEachOther`). Sie lassen sich aber nicht einzeln weglassen oder für andere Betriebsumgebungen neu kombinieren. Dieser Change schafft das Gerüst einer Column-Architektur: Jede Funktion ist eine eigene Säule (Gradle-Modul), die ihre Beans über Avaje Inject selbst mitbringt. Welche Säulen zusammen laufen, entscheidet eine App-Variante je Plattform. Auf dieses Gerüst setzen die Folge-Changes `permission-spi` und `optional-extensions-bootstrap` auf.

## What Changes

- **`core/`**: Neues Modul, das nur APIs enthält (Interfaces, Qualifier, Records, keine Implementierung): Event-Node-Qualifier (`FeatureNode`), `LobbyItem`, `LobbySpawn`, `MapProvider`, `FeatureFlags`, `Deliver`. Dazu kommen `testFixtures` (`TestTitanNode`, `DummyDeliver`) und die geteilten ArchUnit-Regeln für Columns. `:api` geht in `core` auf. Was von `:common` Implementierung ist, wandert nach `runtime` oder bleibt eine gemeinsame Bibliothek; das klärt design.md, da `:setup` davon abhängt.
- **`features/*`**: Jede Lobby-Funktion wird ein eigenes Gradle-Modul mit eigenem Avaje-Generator, eigenen Tests und eigenen Standardwerten: `navigator`, `spawn`, `elytra`, `sit`, `tickle`, `protection`, `respawn`, `hotbar` (Sammeln und Verteilen der `LobbyItem`s, heute Plattform-Code) und `admin` (`/stop`, `/end`). Eine Column hängt nur an `core`, nie an einer anderen Column, nie an `platform/*`.
- **`runtime/`**: Gemeinsamer Starter mit `main()`, dem Aufbau des `BeanScope` und `PlatformBeans`. Er nimmt später die `@Secondary`-Fallbacks der Folge-Changes auf.
- **`apps/*`**: Dünne Assembly-Module, je Betriebsumgebung eins: `apps/cloudnet` (Produktion) und `apps/local` (Entwicklung). In diesem Change tragen beide dieselben Columns und verhalten sich wie das heutige `:app`; die Plattform-Unterschiede kommen mit den Folge-Changes. Die Liste der Lobby-Columns steht an genau einer Stelle (Convention-Plugin), nicht pro Variante.
- **Build-Konventionen**: `titan.column` (Abhängigkeit auf `core`, Avaje-Generator, Test-Stack) und `titan.app-variant` (Shadow-Jar mit `mergeServiceFiles()`, Main-Class, AOT-Cache). `settings.gradle.kts` bindet `features/*`, `platform/*` und `apps/*` per Verzeichnis-Scan ein.
- **Grenzen per Build statt per ArchUnit**: Die Regeln „Features hängen nicht voneinander ab“, „Plattform hängt nicht an Features“, „nur `*Module` ist öffentlich“ und „Bootstrap hängt nicht am Kompositions-Root“ erzwingt die Modulstruktur. Die übrigen Regeln (FeatureNode, `@PostConstruct` nur an `@Singleton`, kein `BeanScope` in Features) laufen als geteilter ArchUnit-Test in jeder Column.
- **`EVENT_PRIORITY`-Eindeutigkeit**: Die Prüfung läuft beim Start über alle Columns im Scope statt als Bytecode-Test über ein einziges Modul, weil keine Column mehr alle anderen kennt.
- **Feature-Flags** bleiben unverändert live über `FeatureFlags` (kein `@RequiresProperty`), denn ein Umschalten darf keinen Neustart brauchen (`lobby-module-config`). Ob eine Column existiert, entscheidet die Variante; ob sie gerade aktiv ist, entscheidet die Flag.
- **Erster Schritt: Spike an `features/protection`**. Er klärt, wie eine Column Beans aus `runtime` bekommt, die sie beim Kompilieren nicht sieht (`@InjectModule(requires = …)` oder Convention), bevor die übrigen Columns umziehen.
- **BREAKING**: Das Artefakt `app-titan.jar` wird durch die Varianten-Jars ersetzt. Deploy (CloudNet-Template, AOT-Cache, `-jar`-Aufruf) muss auf das Jar von `apps/cloudnet` zeigen.

## Offene Punkte für design.md

- **Wo stehen die Standardwerte einer Column?** Vorschlag: im Code der `*Settings` der Column. Die `application.yaml` im Assembly überschreibt nur noch. Das kollidiert mit der Anforderung aus `lobby-module-config`, dass die Standardwerte „für Betreiber einsehbar“ ausgeliefert werden. Alternative: Jede Column liefert eine eigene Default-Datei (eindeutiger Name, keine Überschreibung im Shadow-Jar), die avaje-config lädt. Die Entscheidung legt fest, ob `lobby-module-config` hier geändert wird.
- Schnitt von `:common` zwischen `core`, `runtime` und einer gemeinsamen Bibliothek für `:setup`.

## Capabilities

### New Capabilities

- `app-variants`: Titan wird als Variante je Betriebsumgebung gebaut. Jede Variante startet genau mit ihren Columns. Fehlt eine erwartete Column im Scope, bricht der Start ab, statt stillschweigend ohne sie zu laufen.

### Modified Capabilities

- `lobby-modules`: Ein neues Feature ist ein neues Gradle-Modul unter `features/` statt eines neuen Pakets. Die Unabhängigkeit der Features erzwingt der Build. Die Prüfung eindeutiger Event-Prioritäten läuft beim Start statt als Test über ein einzelnes Modul.
- `lobby-module-config`: Nur falls design.md die Standardwerte in den Code legt (siehe offene Punkte).

## Impact

- **Code**: `app/` wird aufgeteilt in `core/`, `runtime/`, `features/*` und `apps/*`. `:api` geht in `core` auf, `:common` wird aufgeteilt. `:bridge` und `:setup` bleiben in diesem Change inhaltlich unverändert und ziehen nur auf die neuen Abhängigkeiten um.
- **Tests**: Die Tests ziehen mit ihrer Column um. Die Fixtures kommen aus `core`-testFixtures. `ArchitectureTest` wird aufgeteilt (siehe oben). Neu: ein Starttest je Variante, der die erwarteten Columns im Scope prüft.
- **Build/CI**: neue Convention-Plugins in `buildSrc`. CI baut beide Varianten. release-please führt weiter eine Version für alle Artefakte.
- **Abhängigkeiten**: keine neuen. LuckPerms und Butterfly bleiben in diesem Change unverändert (entfernt wird im Folge-Change `permission-spi`).
- **Nutzertexte**: keine Änderung.
- **Betrieb (BREAKING)**: neuer Jar-Name bzw. -Pfad; AOT-Cache muss für das neue Jar neu trainiert werden.
- **Doku**: `docs/lobby-modules.md` und README beschreiben Columns, Varianten und „neues Feature = neues Modul“.
- **Folge-Changes**: `permission-spi`, `optional-extensions-bootstrap`.

## Delivery

PR-Titel: `refactor(build)!: split titan into feature columns and app variants`

`BREAKING CHANGE: the single app-titan jar is replaced by per-platform variant jars (apps/cloudnet, apps/local); deployments and the AOT cache must point at the apps/cloudnet jar.`
