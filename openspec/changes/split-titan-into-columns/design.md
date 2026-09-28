# Design

## Context

Motivation und Umfang: siehe proposal.md. Anforderungen: `specs/app-variants`, `specs/lobby-modules`.

Ist-Zustand, der den Schnitt bestimmt:

- `:app` enthält Bootstrap (`TitanApplication`, `Titan`, `PlatformBeans`, Start-Logs), LuckPerms (`TitanPlayer`), Butterfly, die Befehle `/stop` und `/end` sowie die Plattform-Typen `FeatureNode`, `LobbySpawn`, `LobbyItem`, `ItemSlot`, `ItemUseHandler`, `LobbyItems` und `ItemConflicts`. Dazu kommen die sieben Features `protection` (100), `spawn` (200), `respawn` (300), `navigator` (400), `sit` (500), `tickle` (600) und `elytra` (700).
- `spawn`, `respawn` und `elytra` injizieren die Klasse `LobbyItems` (`equip`, `stack`). Nach dem Schnitt wäre das eine Abhängigkeit von Column zu Column.
- `FeatureNode` umhüllt jeden Listener mit `TitanObservability.guard` aus `:common`. `guard` nutzt nur SLF4J und MDC; Sentry hängt über den `sentry-logback`-Appender dran. Sentry selbst wird nur in `bootstrap()` angesprochen.
- `:common` implementiert `Deliver` aus `:api` (`DeliverProvider`, `DebugDeliver`, `MessageChannelDeliver`) und wird von `:setup` und `:bridge` (`TitanPermissionBridge`, compileOnly) genutzt.
- Die Standardwerte stehen gesammelt in `app/src/main/resources/application.yaml`. Der Build kopiert sie als `application.example.yaml` in die Distribution.
- `ArchitectureTest` hat neun Regeln, darunter die Bytecode-Prüfung der `EVENT_PRIORITY`-Eindeutigkeit. `NavigatorProtectionOrderingTest` und `StandardLoadoutTest` testen über mehrere Features hinweg.

## Goals / Non-Goals

**Goals:**
- Zielbild der Module und ihrer erlaubten Abhängigkeiten (siehe D1).
- Den Umzug in Wellen mit durchgehend grünem Build, damit mehrere Columns parallel umziehen können.

**Non-Goals:**
- Unterschiede zwischen den Varianten. LuckPerms, Butterfly und CloudNet bleiben in `runtime`, beide Varianten sind gleich. Unterschiede kommen mit `permission-spi` und `optional-extensions-bootstrap`.
- Neue Metriken, Spans oder Nutzertexte. Log-Ausgaben ändern sich nur dort, wo D5 und D6 es nennen.
- Neuer Zuschnitt von `:setup` und `:bridge`. Sie ziehen nur auf die neuen Abhängigkeiten um.

## Decisions

### D1: Modulgraph

```
apps/cloudnet ─┐
apps/local ────┼─▶ runtime ─▶ common ─▶ core
               └─▶ features/* ──────────▶ core
setup ─▶ common, core        bridge ─(compileOnly)▶ common
```

- **`core`** (`java-library`) enthält `:api` (`Deliver` samt den Deliver-Typen) und die Andockpunkte der Columns: `FeatureNode` (samt dem Qualifier-Namen `TITAN_NODE`), `LobbySpawn`, `LobbyItem`, `ItemSlot`, `ItemUseHandler`, das neue Interface `LobbyItems` (siehe D3), `FeatureFlags` und `EntityDismountEvent`, außerdem `Cancelable`, weil `protection` es für Methodenreferenzen nutzt. Dazu kommen `testFixtures` (`TestTitanNode`, `DummyDeliver`, `EventListenerCounter`) und die geteilten ArchUnit-Regeln (D7).
- **`common`** behält die Implementierungen, die `runtime` und `:setup` teilen: Map (`MapProvider`, `LobbyMap` usw.), Blockhandler, `DeliverProvider` samt Implementierungen, `TitanObservability` (nur noch `bootstrap`/`installExceptionHandler`), `TitanPermissionBridge`, Utils. `ConfigFeatureFlags` zieht nach `runtime`, weil nur `PlatformBeans` es nutzt.
- **Abweichung vom Proposal:** `MapProvider` bleibt in `common`. Es ist eine Implementierung, die keine Column braucht; Columns bekommen die Spawn-Position über `LobbySpawn`.
- **`features/<name>`** (Paket `net.onelitefeather.titan.feature.<name>`; das `app` im Paketnamen fällt weg, weil es keine App mehr bezeichnet): `protection`, `spawn`, `respawn`, `navigator`, `sit`, `tickle`, `elytra`, `hotbar` (heute `LobbyItems`, `ItemConflicts`) und `admin` (`/stop`, `/end`). Jede Column hängt nur an `core`, zur Laufzeit an Minestom und Aves.
- **`runtime`** enthält `TitanApplication` (`main`), `Titan` (Aufbau des `BeanScope`), `PlatformBeans`, `ConfigFeatureFlags`, die Start-Logs, `TitanPlayer`/`CompatibilityUtil` (LuckPerms) und Butterfly.
- **`apps/cloudnet`** und **`apps/local`** enthalten keinen eigenen Code außer dem Starttest, und je eine `build.gradle.kts`, die `titan.app-variant` anwendet.

Built-in: Gradle-Projektabhängigkeiten erzwingen die Grenzen. Was eine Column nicht deklariert, kann sie nicht kompilieren. Das ersetzt die ArchUnit-Regeln `featureModulesDoNotDependOnEachOther`, `platformAndCommonDoNotDependOnFeatures` und `platformDoesNotDependOnCompositionRoot`. Die Regel `onlyModuleTypesArePublicInFeatures` entfällt ebenfalls: Eine fremde Column kann ohnehin nichts sehen, und `runtime` findet Columns nur über Avaje. Alternative wäre ArchUnit über einen zusammengesetzten Classpath gewesen; verworfen, weil die Grenze dann erst im Test statt beim Kompilieren greift.
SOLID: DIP (Columns hängen an Abstraktionen in `core`), OCP (neue Column ohne Änderung an `runtime`).
Test: Der Build selbst. Dazu ein Negativ-Beleg im Spike-Task: Ein Import von `runtime` in `features/protection` scheitert beim Kompilieren; das wird nicht eingecheckt.

### D2: Eine Column bekommt Beans, die sie beim Kompilieren nicht sieht (Spike)

Der Avaje-Generator prüft pro Modul, ob jede Abhängigkeit bereitgestellt wird. Beans aus `runtime` (`@Named("titan") EventNode<Event>`, `Deliver`, `FeatureFlags`, `LobbySpawn`, `Instance`, `Clock`, `Scheduler`, `CommandManager`) und aus `hotbar` (`LobbyItems`) sieht eine Column nicht. Built-in-Lösung: `@InjectModule(name = …, requires = {…})` in der `package-info.java` jeder Column, mit den externen Typen, die sie braucht. Umgekehrt deklariert `runtime` `@InjectModule(provides = {…})` und `hotbar` `provides = LobbyItems.class`, damit Avaje die Module beim Aufbau des `BeanScope` richtig ordnet. Alternative wäre `requiresPackages` auf das `core`-Paket; sie ist ungenauer, weil ein vergessener Provider erst zur Laufzeit auffällt, und dient nur als Rückfall, falls der Spike zeigt, dass `requires` mit dem generischen `EventNode<Event>` nicht funktioniert.

Der Spike an `features/protection` klärt dafür:
1. ob `requires` mit `EventNode.class` und dem Qualifier zur Übersetzung passt,
2. wie der Modulname gelesen wird, den D5 für die Prüfung „erwartete Column geladen“ braucht, ohne dass der generierte Modulname mit der Klasse `ProtectionModule` kollidiert (z. B. `@InjectModule(name = "protectionColumn")`),
3. dass `mergeServiceFiles()` alle Avaje-Module im Shadow-Jar erhält.

Das Ergebnis wird als kurzer Abschnitt in `docs/lobby-modules.md` festgehalten („Wie eine Column Plattform-Beans bekommt“). Es ändert weder Specs noch Task-Schnitt, nur die Annotationswerte in den Columns der Welle 2.
SOLID: DIP. Test: `WiringTest`/Starttest der Variante (Integration) baut den vollständigen Scope.

### D3: `LobbyItems` wird ein Interface in `core`, die Implementierung liegt in `features/hotbar`

`core` bekommt `interface LobbyItems { void equip(Player); ItemStack stack(String key); }` mit derselben Bedeutung wie heute. Die heutige Klasse zieht als `@Singleton final class HotbarLobbyItems implements LobbyItems` in `features/hotbar`, zusammen mit `ItemConflicts` und ihren Tests. `spawn`, `respawn` und `elytra` ändern nur ihren Import.
Built-in: Avaje-Injection über den Interface-Typ; eine eigene Registry gibt es nicht.
SOLID: DIP und ISP (Features sehen nur `equip`/`stack`, nicht `itemCount`).
Test: Die bestehenden `LobbyItems`-Tests ziehen mit nach `hotbar` (Unit/Integration). `SpawnModuleTest`, `RespawnModuleTest` und `ElytraModuleTest` bekommen einen Test-Double oder die `hotbar`-Implementierung aus deren testFixtures. Welcher Weg, entscheidet der jeweilige Task nach F.I.R.S.T.; die Column-Grenze gilt dabei auch für Testcode.

### D4: Standardwerte je Column, beim Bauen der Variante zusammengeführt

Jede konfigurierbare Column liefert `src/main/resources/titan/defaults/<column>.yaml` mit genau ihrem Abschnitt (`spawn`, `sit`, `tickle`, `elytra`; `navigator` liefert den Abschnitt `features` mit allen `NAVIGATOR_*`-Flags). `runtime` liefert `titan/defaults/runtime.yaml` (`config.watch.*`). `titan.app-variant` fügt beim Bauen alle Default-Dateien der Variante zu einer `application.yaml` im Classpath zusammen und legt dieselbe Datei als `application.example.yaml` in die Distribution. Vorher liest eine Prüfung (SnakeYAML mit `allowDuplicateKeys=false`, flach gemacht auf Punkt-Schlüssel) alle Dateien und lässt den Build scheitern, wenn zwei Columns denselben Schlüssel setzen; die Meldung nennt beide Dateien und den Schlüssel.

Ergebnis: Die Rangfolge aus `lobby-module-config` bleibt Wort für Wort gültig, weil die Standardwerte wie heute als Classpath-`application.yaml` geladen werden. Deshalb braucht `lobby-module-config` kein Delta.

Built-in geprüft:
- `load.properties` von avaje-config: verworfen, weil die Liste der Dateien dann pro Variante gepflegt werden müsste (zweite zentrale Liste).
- Eine eigene `ConfigurationSource` per ServiceLoader: verworfen. Sie läuft nach den Dateien und bräuchte Laden nur für fehlende Schlüssel, und ob `config.watch.*` dann rechtzeitig gelesen wird, ist unklar. Das ist eigene Laufzeit-Infrastruktur für ein reines Build-Problem.
- `mergeServiceFiles` bzw. Shadow-Transformer für YAML: Shadow bringt keinen YAML-Merge mit. Eine Verkettung per Gradle-Task (Build-Zeit, keine Laufzeit-Abhängigkeit) ist der kleinste Eingriff.

Einschränkung: Die Dateien werden verkettet, nicht tief zusammengeführt, damit Kommentare für Betreiber erhalten bleiben. Deshalb darf jeder Top-Level-Abschnitt nur in einer Datei stehen; das prüft der Build (siehe oben). Braucht künftig eine zweite Column Flags unter `features`, greift die Prüfung, und der Fall wird dann entschieden.
SOLID: SRP (jede Column besitzt ihre Werte). Test: Unit-Test der Merge- und Konfliktprüfung in `buildSrc` (JUnit, reine Funktion über Strings). Der bestehende `ApplicationYamlDefaultsCharacterizationTest` zieht nach `apps/cloudnet` und prüft die zusammengeführte Datei gegen die heutigen Werte. Column-Tests, die Standardwerte brauchen, laden ihre eigene Default-Datei.

### D5: Erwartete Columns einer Variante

`titan.app-variant` bindet standardmäßig jedes Projekt unter `features/` ein (Scan über `rootProject.subprojects`). Eine Variante kann einzelne Columns ausschließen (`titanVariant { exclude("…") }`). So steht die Liste an genau einer Stelle, und ein neues Feature braucht keine Änderung. Die Convention schreibt die Avaje-Modulnamen der eingebundenen Columns (`<name>Column`) in die Ressource `META-INF/titan/variant.properties` (`name`, `modules`). Plattform-Module der Folge-Changes hängen sich an dieselbe Liste an (z. B. `luckpermsPlatform`). `runtime` vergleicht beim Start, direkt nach dem Aufbau des `BeanScope`, diese Liste mit den geladenen Avaje-Modulen. Die Kennung ist der Name der generierten Modulklasse ohne das Suffix `Module`, weil Avaje 12.7 den `@InjectModule`-Namen zur Laufzeit nicht anders herausgibt. Fehlende Module führen zu einem `IllegalStateException` mit ihren Namen; `main` bricht ab.
Log: INFO beim Start: `Variant {} started with modules {}`. Das ersetzt keine bestehende Zeile, sondern kommt vor `FeatureStartupLog`. ERROR nur einmal, dort, wo `main` den Fehler behandelt.
Built-in: Avaje lädt Module ohnehin per ServiceLoader; eine eigene Column-SPI ist nicht nötig. Die Vergleichslogik ist eine reine Funktion `missingModules(expected, loaded)`.
SOLID: SRP. Test: Unit-Test für `missingModules`. Integration: Starttest je Variante prüft, dass alle erwarteten Columns geladen sind, und ein Test mit einer künstlich erwarteten, fehlenden Column prüft Abbruch und Meldung.

### D6: Eindeutige Event-Priorität wird beim Anhängen geprüft

`FeatureNode.attach(parent, featureId, priority)` prüft vor `addChild` die vorhandenen Kinder des Elternknotens (`parent.getChildren()`, Minestom-API). Hat eines dieselbe Priorität, wirft `attach` ein `IllegalStateException`, das beide Feature-IDs (aus dem Knotennamen `titan/<id>`) und die Position nennt. Weil `attach` in `@PostConstruct` läuft, bricht der Aufbau des `BeanScope` ab, und die Lobby startet nicht (Spec `lobby-modules`). Die Bytecode-Prüfung `eventPriorityValuesAreUniqueAcrossFeatures` entfällt.
Built-in: Minestom-`EventNode` kennt seine Kinder und Prioritäten; ein eigenes Register ist nicht nötig. Die Alternative, beim Start über alle Beans per Reflection zu prüfen, ist verworfen: Sie braucht Konventionen über Feldnamen und findet den Fehler später.
SOLID: SRP (die Prüfung sitzt dort, wo die Priorität gesetzt wird). Test: Unit-Test in `core` mit `EventNode.all(...)` ohne Server (zwei `attach` mit gleicher Priorität → Ausnahme mit beiden IDs; unterschiedliche → ok).

### D7: Geteilte ArchUnit-Regeln laufen in jeder Column

Die Regeln `featuresRegisterListenersOnlyThroughFeatureNode`, `classesWithPostConstructInFeaturesAreSingleton` und `featureModulesDoNotUseBeanScope` wandern als `ColumnArchitectureRules` (öffentliche `ArchRule`-Konstanten) in die `testFixtures` von `core`. `titan.column` hängt `testImplementation(testFixtures(project(":core")))` und ArchUnit an. Jede Column hat einen kleinen `ColumnArchitectureTest`, der die Regeln per `@ArchTest` auf ihr eigenes Paket anwendet. `navigatorDoesNotDependOnAvajeConfig` bleibt als eigene Regel im Navigator-Test.
Built-in: ArchUnit-`ArchRule`-Felder lassen sich wiederverwenden; ein Plugin, das Tests generiert, ist nicht nötig.
SOLID: DRY statt SOLID. Test: Die Regel selbst; beim Spike wird einmal gezielt ein Verstoß eingebaut und wieder entfernt.

### D8: Convention-Plugins in `buildSrc`

- `titan.column`: wendet `titan.java-conventions` und `java-library` an; `implementation(project(":core"))`, Minestom und Aves über das Aonyx-BOM, `avaje-inject` samt Generator, den Test-Stack (JUnit, Cyano, Mockito, ArchUnit, core-testFixtures).
- `titan.app-variant`: wendet `titan.java-conventions`, `titan.publish-conventions`, `application` und Shadow an; hängt an `runtime` und an allen Columns aus D5; Shadow mit `mergeServiceFiles()`, Main-Class `…TitanApplication`, `archiveFileName = "titan-<variant>.jar"`; erzeugt `variant.properties` und die zusammengeführte `application.yaml` (D4); AOT-Task wie heute, aber nur mit `titanVariant { aotCache = true }` (nur `cloudnet`), Ausgabe `titan-<variant>.aot`.
- `settings.gradle.kts` bindet `features/*`, `apps/*` sowie `core` und `runtime` ein. Der Scan geht über Unterverzeichnisse mit `build.gradle.kts`.
- Veröffentlicht wird nur `apps/cloudnet` als `titan-cloudnet` (Jar plus AOT-Classifier). `apps/local` wird nicht veröffentlicht. Die bisherige `titan-app` wird nicht mehr veröffentlicht; das nennt der BREAKING-Hinweis.
Built-in: Gradle-Precompiled-Script-Plugins, wie in `buildSrc` schon üblich.
Test: `./gradlew build` sowie ein Blick auf den Jar-Inhalt der Variante (Task 5.x).

### D9: Fehler-Zuordnung (`guard`) zieht nach `core`

Der SLF4J/MDC-Teil von `TitanObservability.guard(featureId, listener)` samt Spieler-Zuordnung zieht als paketprivate Klasse `ListenerGuard` neben `FeatureNode` nach `core`. Log-Text, MDC-Schlüssel und Level bleiben gleich (ERROR, einmal, dort, wo der Fehler behandelt wird). `TitanObservability` in `common` behält `bootstrap()` und `installExceptionHandler()` und nutzt für die Meldung dieselbe Logik. Um keinen Zyklus zu erzeugen, ruft es dafür eine öffentliche statische Methode in `core` auf (`common → core` ist erlaubt).
Built-in: SLF4J/MDC mit `sentry-logback`, wie heute; neu ist nur der Ort.
SOLID: SRP (Fehlerzuordnung vs. Sentry-Start). Test: Die bestehenden Guard-Tests ziehen mit nach `core` und prüfen über einen erfassten Appender (Unit).

### D10: Migration in Wellen mit grünem Build

1. **Welle 1 (Gerüst + Spike):** `core`, `titan.column`, `features/protection`; `:app` hängt per Scan an allen `features/*`, bleibt vorerst Assembly und Runtime und merged die Default-Dateien, die bereits in `app/src/main/resources/titan/defaults/<column>.yaml` aufgeteilt liegen. Dadurch müssen Welle-2-Agents keine gemeinsame Datei ändern.
2. **Welle 2 (parallel):** Die übrigen Columns ziehen je in eigenem Worktree um; jeder Agent ändert nur `features/<x>/**` und löscht `app/…/feature/<x>` bzw. `app/…/module/item`.
3. **Welle 3:** `:app` → `runtime` + `apps/*`, `titan.app-variant`, Querschnitts-Tests nach `apps/cloudnet`, CI und Publishing.
4. **Welle 4:** Doku und PR.

Rollback: Revert des Squash-Commits; das Deploy zeigt wieder auf `app-titan.jar`.
Deploy: Das CloudNet-Template startet `java -XX:AOTCache=titan-cloudnet.aot -jar titan-cloudnet.jar`. Der AOT-Cache wird mit dem neuen Jar neu erzeugt. `application.yaml` im Arbeitsverzeichnis bleibt unverändert gültig.

## Risks / Trade-offs

- [Avaje-`requires` passt nicht zu generischen/qualifizierten Typen] → Der Spike in Welle 1 klärt das vor allen anderen Columns; Rückfall `requiresPackages` (D2).
- [`mergeServiceFiles` verliert ein Avaje-Modul im Shadow-Jar] → Die Prüfung der erwarteten Columns (D5) bricht den Start ab, statt stumm ohne Feature zu laufen; der Starttest der Variante deckt es in CI ab.
- [Verkettete Default-Dateien: ein zweiter `features`-Abschnitt] → Die Konfliktprüfung lässt den Build scheitern; bewusst in Kauf genommen, damit die Kommentare erhalten bleiben (D4).
- [Parallele Welle 2 kollidiert in `app/`] → Jeder Agent löscht nur sein eigenes Paket; die gemeinsamen Dateien (`settings.gradle.kts`, `app/build.gradle.kts`, Default-Dateien) werden in Welle 1 fertig vorbereitet.
- [Die Prioritätsprüfung zur Laufzeit statt im Build fällt später auf] → Der Starttest jeder Variante baut den vollen Scope und scheitert in CI.
- [Neuer Artefaktname bricht Deploy und AOT] → BREAKING-Footer, Deploy-Hinweis in README und `docs/world-conversion.md`, AOT-Cache neu trainieren.
