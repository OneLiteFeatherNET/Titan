# Design

## Context

Den Anlass beschreibt `proposal.md` (Why), die Anforderungen stehen in `specs/lobby-modules/spec.md`. Der Stand auf `main` nach `lobby-feature-modules`:

- **Composition Root:** `app/.../Titan.java` baut von Hand `MapProvider`, `Deliver` (`DeliverProvider`), `ConfigStore`, `NavigatorEntries`, `ItemRegistry` (braucht den `titan`-EventNode), `FeatureFlags` (`TogglzFeatureFlags`) und reicht sie an `ModuleRegistry.builder()…modules(new ProtectionModule(), new SpawnModule(instance, () -> spawn), new RespawnModule(), new NavigatorModule(deliver, entries, flags), new SitModule(), new TickleModule(), new ElytraModule())` weiter. Die Position in der Liste ist die Startreihenfolge.
- **Konstruktoren:**
  - `SpawnModule(Instance, Supplier<Pos>)`
  - `NavigatorModule(Deliver, NavigatorEntries, FeatureFlags)`
  - `TickleModule()` plus `TickleModule(Clock)`
  - die übrigen ohne Parameter
- **Lebenszyklus:** `ModuleRegistry` (Builder, `enableAll`/`disableAll`, `ModuleContext` pro Modul) mit getesteter Semantik (Spec `lobby-modules`). `ModuleHarness` baut Registries für Tests über den Builder.
- **Build:** `app/build.gradle.kts` hat bereits `mergeServiceFiles()` im `shadowJar`. Damit ist ein Prüfpunkt des Spikes aus dem Proposal schon erledigt. Togglz hat eine eigene `META-INF/services`-Datei in `common`.
- **Laufzeit:** Java 25 (aktuelles LTS, per Toolchain festgelegt), Minestom 2026.08.x, AOT-Cache. ArchUnit prüft die Grenzen der Features (`app/src/test/.../ArchitectureTest`).

## Goals / Non-Goals

**Goals:**
- Objekte baut Avaje Inject beim Kompilieren. Module und Plattform-Dienste sind Beans, die Composition Root schrumpft auf das Starten und Schließen des `BeanScope`.
- Der Lebenszyklus bleibt unverändert beim `ModuleRegistry`. DI ersetzt nur das Erzeugen, nicht das An- und Abschalten.
- Kein Modul kann still verloren gehen: Wer vergisst, eine Annotation zu setzen, bricht den Build.

**Non-Goals:**
- Config über den Konstruktor injizieren, und generell die Config umstellen. Beides folgt in `standardized-config-profiles`.
- Den Setup-Server auf DI umstellen.
- OpenTelemetry bzw. Metriken. Titan hat noch kein OTel. Das ist eine eigene Change (vorgemerkt: TPS/MSPT).
- `@PostConstruct`/`@PreDestroy` für Modul-Logik. Der Lebenszyklus läuft weiter über `enable`/`disable`.

## Decisions

### 1. Avaje Inject statt manueller Composition Root, Guice oder SmallRye/CDI

**Entscheidung:** `io.avaje:avaje-inject` als Laufzeit-Abhängigkeit in `app` und `io.avaje:avaje-inject-generator` als `annotationProcessor` nur für `main`. Die Version legt der Spike fest (Task 1), zu nehmen ist die aktuelle Maven-Central-Version.

**Built-in first:** Die eingebaute Option ist Java selbst, also die manuelle Composition Root von heute. Sie wird verworfen, weil jedes neue Feature und jeder neue gemeinsame Dienst `Titan.java` ändern muss. Das widerspricht dem Ziel „neues Feature = nur neues Paket“ (Spec). Guice wurde verworfen, weil es zur Laufzeit per Reflection arbeitet und Klassen erzeugt, die der AOT-Cache nicht erfasst, und weil es keine Liste aller Implementierungen und keine Lebenszyklus-Annotationen mitbringt. CDI/SmallRye fallen raus, weil der Container schwer ist, zur Laufzeit Proxies erzeugt und auf Quarkus zugeschnitten ist. Avaje erzeugt reinen Java-Code (`$DI`-Klassen und `*Module`, gefunden über den `ServiceLoader`) und kommt ohne Reflection aus.

**Test:** Integrationstest „der Container verdrahtet sich vollständig“ (Entscheidung 6).

**SOLID:** DIP, weil Module von Abstraktionen im Konstruktor abhängen, und OCP, weil neue Module ohne Änderung an bestehendem Code dazukommen.

### 2. Module werden automatisch gefunden, als `@Singleton` mit `@Priority`

**Entscheidung:** Jedes Feature-Modul ist `@Singleton` mit `@Priority(n)` (Annotation `io.avaje.inject.Priority`, Begründung unten).

**Spike-Befund, der diese Entscheidung ändert (Task 1.2):** Eine per Konstruktor injizierte `List<LobbyModule>` wird von Avaje **nicht** nach `@Priority` sortiert – weder mit `jakarta.annotation.Priority` noch mit `io.avaje.inject.Priority`. Ein Prototyp mit drei Beans (Prioritäten 100/200/300, in absichtlich verwürfelter Deklarationsreihenfolge angelegt) lieferte über die reine Konstruktor-Injektion stets dieselbe, von der Priorität unabhängige Reihenfolge. Sortiert wird nur, wenn explizit `BeanScope.listByPriority(Type.class)` aufgerufen wird – und das geht nachweislich **nur nach** `BeanScope.builder().build()`: ein Aufruf während des Aufbaus (aus einem `@Singleton`-Konstruktor oder einer `@Factory`-`@Bean`-Methode heraus, mit `BeanScope` als injiziertem Parameter) schlägt reproduzierbar fehl mit `IllegalStateException: Proxy BeanScope can't use listByPriority() while scope is being built`.

**Konsequenz für Entscheidung 3 und 5:** `ModuleRegistry` kann deshalb kein `@Bean` aus `PlatformBeans` sein, der einfach `List<LobbyModule>` konstruktorinjiziert bekommt. Stattdessen baut `Titan` (die Composition Root) den `ModuleRegistry` **nach** `BeanScope.builder().build()` selbst: `scope.listByPriority(LobbyModule.class)` liefert die sortierte Liste, die zusammen mit den übrigen, aus dem Scope geholten Plattform-Beans an den bestehenden `ModuleRegistry.builder()....modules(...).build()` geht. Das ist kein Service-Locator in Feature-Code – die ArchUnit-Regel aus Entscheidung 6 verbietet `BeanScope`-Zugriff nur in `..app.feature..`; `Titan` ist Bootstrap-Code und darf den Scope halten. Details siehe Entscheidung 3 und 5.

Die Prioritäten bilden die heutige Reihenfolge ab, in Hunderter-Schritten mit Platz dazwischen:

| Modul | Priorität |
|---|---|
| protection | 100 |
| spawn | 200 |
| respawn | 300 |
| navigator | 400 |
| sit | 500 |
| tickle | 600 |
| elytra | 700 |

**Empirisch bestätigte Richtung (Spike, Task 1.2):** Niedrigere Werte kommen zuerst (aufsteigend), `listByPriority` sortiert also genau wie in der Tabelle angenommen. Prototyp: drei Beans A/B/C mit Prioritäten 300/100/200, in dieser (verwürfelten) Reihenfolge deklariert; `scope.listByPriority(Greeter.class)` lieferte über 5 saubere Rebuilds stabil `[B(100), C(200), A(300)]`. Die Tabelle oben muss **nicht** gespiegelt werden.

**Gleiche Priorität (Spike, Task 1.2):** Drei zusätzliche Beans mit identischer Priorität 100 (B, D, E) ergaben über 5 saubere Rebuilds eine deterministische, aber von der Priorität unabhängige Reihenfolge (stabile Sortierung über die zugrunde liegende Deklarations-/Generierungsreihenfolge). Das bestätigt: Auf die Reihenfolge bei Gleichstand darf man sich **nicht verlassen** – die Eindeutigkeitsprüfung aus Entscheidung 6 bleibt Pflicht, nicht nur Vorsichtsmaßnahme.

**Welche Priority-Annotation:** Getestet wurden `jakarta.annotation.Priority` und `io.avaje.inject.Priority` – beide funktionieren identisch mit `listByPriority`. Gewählt wird **`io.avaje.inject.Priority`**, weil die Klasse bereits im `avaje-inject`-Jar liegt und keine zusätzliche Abhängigkeit braucht. `jakarta.annotation.Priority` bräuchte eine eigene `jakarta.annotation-api`-Abhängigkeit, die `avaje-inject` **nicht** transitiv mitbringt (dessen POM zieht nur `jakarta.inject:jakarta.inject-api`, nicht `jakarta.annotation-api`).

**Deterministisch bei gleicher Priorität:** Wir verlassen uns nicht auf eine Reihenfolge bei Gleichstand. Die Prioritäten müssen **eindeutig** sein, das prüft ein Test (Entscheidung 6). Damit ist die Spec-Anforderung „gleiche Priorität ist deterministisch“ erfüllt, ohne eine eigene Sortierung oder Reflection zur Laufzeit.

**Built-in first:** `io.avaje.inject.Priority` (bzw. alternativ `jakarta.annotation.Priority`) ist die Annotation, die Avajes eigenes `BeanScope.listByPriority(...)` auswertet – das ist der vorgesehene Weg, um eine priorisierte Liste zu bekommen, auch wenn er (anders als ursprünglich angenommen) explizit aufgerufen statt implizit injiziert werden muss. Eine eigene Methode `LobbyModule#priority()` wurde verworfen, weil sie dieselbe Information doppelt pflegen würde.

**Test:** Unit-Test für die Eindeutigkeit (ArchUnit oder ein Reflection-Test **nur im Test**, Entscheidung 6) und ein Integrationstest für die Startreihenfolge, der `Titan`s `scope.listByPriority(...)`-Aufruf prüft statt einer injizierten Liste.

**SOLID:** OCP.

### 3. Plattform-Dienste über eine `@Factory` im `app`-Modul

**Entscheidung:** `app/.../bootstrap/PlatformBeans` (`@Factory`) stellt diese Beans per `@Bean` bereit:
- `InstanceContainer` (registriert bei `MinecraftServer.getInstanceManager()`)
- `MapProvider`
- `LobbySpawn`
- `Deliver` (`DeliverProvider.create()`)
- `ConfigStore` (`ConfigStore.open(Path.of("app.json"))`)
- `EventNode<Event>` `titan` (`@Named("titan")`, am globalen Handler eingehängt)
- `ItemRegistry`
- `NavigatorEntries`
- `FeatureFlags` (`TogglzFeatureFlags`)
- `Clock` (`Clock.systemUTC()`)

**Korrektur aus dem Spike (Task 1.2, siehe Entscheidung 2):** `ModuleRegistry` ist **kein** `@Bean` von `PlatformBeans` mehr. `BeanScope.listByPriority(...)`, das die Modulliste sortiert, lässt sich nachweislich nicht während des Scope-Aufbaus aufrufen (auch nicht aus einer `@Factory`-Methode heraus) – siehe die dort dokumentierte `IllegalStateException`. `ModuleRegistry` baut deshalb `Titan` selbst, direkt nach `BeanScope.builder().build()` (Entscheidung 5), über den bestehenden Builder mit der per `scope.listByPriority(LobbyModule.class)` sortierten Liste. Der Builder bleibt als öffentliche API für Tests und den `ModuleHarness` erhalten.

`common` bekommt **keine** Annotationen und keinen Annotation-Processor. Die Klassen dort sind Bibliothekscode, der Setup-Server nutzt sie ohne DI. Die Factory in `app` hüllt sie ein.

**Built-in first:** Avajes `@Factory`/`@Bean` ist der vorgesehene Weg für Objekte aus fremdem oder statischem Code (Minestom-Singletons). Den Dienst-Klassen in `common` selbst Annotationen zu geben, wurde verworfen, weil `common` dann von Avaje abhinge, obwohl der Setup-Server es nicht braucht.

**Test:** Integrationstest der Verdrahtung.

**SOLID:** SRP, weil nur die Factory weiß, wie Plattform-Objekte entstehen, und DIP.

### 4. Kleine Anpassungen an Konstruktoren statt Sonderfällen

**`SpawnModule(Instance, Supplier<Pos>)`** wird zu `SpawnModule(Instance, LobbySpawn)`. `LobbySpawn` ist eine neue funktionale Schnittstelle (`Pos position()`) in `app/.../module` bzw. dem Platform-Paket. Die Factory liefert sie als `() -> mapProvider.getActiveLobby().spawn()`. `Supplier<Pos>` wäre als Bean mehrdeutig, weil es ein generischer Typ ohne Bedeutung ist.

**`TickleModule`** behält nur noch den Konstruktor `@Inject TickleModule(Clock)`. Den parameterlosen Konstruktor gibt es dann nicht mehr, der `Clock` kommt als Bean. Tests bauen das Modul weiterhin mit `new TickleModule(fixedClock)`.

**`NavigatorModule`, `ElytraModule` usw.:** Ihre Konstruktoren bekommen `@Inject`, wo es mehrere gibt, sonst ändert sich nichts.

**Test:** Die bestehenden Unit- und Integrationstests der Module laufen unverändert, weil sie die Konstruktoren direkt aufrufen.

**SOLID:** ISP und DIP, weil `LobbySpawn` nur das bietet, was Spawn braucht.

### 5. Composition Root: `BeanScope` in `Titan`

**Entscheidung:** `Titan` baut `BeanScope.builder().build()`. Anders als ursprünglich angenommen holt `Titan` `ModuleRegistry` **nicht** direkt aus dem Scope, sondern baut ihn selbst: `scope.listByPriority(LobbyModule.class)` (erst nach `build()` gültig, siehe Entscheidung 2) liefert die sortierte Modulliste, die übrigen Plattform-Beans (`EventNode<Event>` „titan“, `ConfigStore`, `NavigatorEntries`, `ItemRegistry`, `FeatureFlags`) holt `Titan` per `scope.get(...)` und reicht sie an `ModuleRegistry.builder()....modules(...).build()`. `initialize()` ruft wie heute `enableAll()` auf und registriert die Plattform-Befehle `stop`/`end` und Butterfly. Beim Herunterfahren gilt diese Reihenfolge: `moduleRegistry.disableAll()`, danach Butterfly, danach `beanScope.close()`, jeweils als Shutdown-Tasks in dieser Reihenfolge (FIFO). Fehler beim Aufbau des Scopes, etwa eine fehlende Abhängigkeit, landen als `RuntimeException` im bestehenden Abbruch beim Start in `TitanApplication` (Log-Zeile und Exit-Code 1).

**Logging:** Nach `enableAll()` gibt es **eine** INFO-Zeile mit der Startreihenfolge, parametrisiert: `Lobby modules enabled in order: {}` mit der Liste der IDs. Das ist ein Lebenszyklus-Ereignis im Sinne der Logging-Regeln.

**Test:** Der Smoke-Test des Jars (Task 7) prüft den Start, die Log-Zeile, das saubere Herunterfahren und den Abbruch bei fehlender Abhängigkeit.

**SOLID:** SRP, weil `Titan` nur noch den Bootstrap macht.

### 6. Schutzgeländer: Test der Verdrahtung und ArchUnit-Regeln

Das größte Risiko von automatischem Finden ist ein Modul, das still fehlt. Dagegen gibt es drei Absicherungen:
- **`ModuleWiringTest`** (Integrationstest mit `Env`, weil die Factory Minestom-Singletons braucht): Er baut den echten `BeanScope` und prüft vier Dinge:
  - alle 7 bekannten Module sind genau einmal vorhanden,
  - die Reihenfolge von `scope.listByPriority(LobbyModule.class)` entspricht der Tabelle aus Entscheidung 2,
  - der Scope baut sich ohne fehlende Abhängigkeit,
  - der Scope lässt sich sauber schließen.
- **ArchUnit, neue Regeln:**
  - Jede Klasse in `..app.feature..`, die `LobbyModule` implementiert, trägt `@Singleton` **und** `@Priority`. Fehlt eine Annotation, schlägt der Build fehl, statt dass das Modul still verschwindet.
  - Kein Feature-Code nutzt `BeanScope` oder `ApplicationContext`-artige Lookups. Das verhindert das Service-Locator-Muster, Abhängigkeiten kommen nur per Konstruktor.
- **Eindeutige Prioritäten:** ein Unit-Test, der über ArchUnit alle `@Priority`-Werte der Module einsammelt und auf Duplikate prüft. Reflection nur im Test.

**Built-in first:** ArchUnit ist schon im Build, ebenso Minestoms `Env` über Cyano.

**SOLID:** Diese Tests sichern OCP und DIP dauerhaft ab.

### 7. Beispielmodul und Doku

`app/src/test/.../feature/example/ExampleModule` bekommt `@Singleton`/`@Priority`, damit die Vorlage stimmt. Der Annotation-Processor läuft **nicht** für Testquellen (`testAnnotationProcessor` wird nicht gesetzt). Das Beispiel wird deshalb im Wiring-Test nicht gefunden, bleibt aber eine korrekte Kopiervorlage.

`docs/lobby-modules.md` erklärt den neuen Weg in Checklisten-Form: neues Paket, `@Singleton` plus `@Priority`, Abhängigkeiten per Konstruktor, und einen neuen gemeinsamen Dienst als `@Bean` in `PlatformBeans`, bzw. einen eigenen `@Singleton`, wenn er Feature-übergreifend ist.

**Test:** Die Doku wird im Review geprüft, die Vorlage kompiliert.

## Risks / Trade-offs

- **[Risiko] Ein Modul fehlt still, weil `@Singleton` vergessen wurde.** → Die ArchUnit-Regel verlangt `@Singleton`/`@Priority` an jedem `LobbyModule` in Features, der Wiring-Test zählt die Module.
- **[Risiko] Die Reihenfolge ändert sich unbemerkt,** weil die Richtung von `@Priority` falsch verstanden wurde. → Der Spike klärt die Richtung, der Wiring-Test prüft die konkrete Reihenfolge.
- **[Risiko] Der Annotation-Processor bricht den Build** in Kombination mit Spotless oder dem Gradle-Build-Cache. → Das deckt der Spike ab, und der CI-Build läuft auf drei Betriebssystemen.
- **[Risiko] Startzeit bzw. AOT-Cache verschlechtern sich.** → Im Spike vorher und nachher messen (Task 1, Werte ins Design nachtragen). Laut Doku gibt es keine Reflection, der erzeugte Code wird wie normaler Code gecacht.
- **[Trade-off] Weniger Sichtbarkeit:** Die Startreihenfolge steht nicht mehr an einer Stelle als Liste, sondern verteilt in `@Priority`-Werten. → Die INFO-Log-Zeile beim Start und die Tabelle in `docs/lobby-modules.md` machen sie sichtbar.
- **[Trade-off] Eine neue Abhängigkeit** (Avaje Inject, zur Laufzeit klein, ohne weitere Abhängigkeiten). Sie wird bewusst in Kauf genommen, der Mehrwert steht in `proposal.md`.

## Migration Plan

- Kein Einfluss auf Betreiber: Config, Befehle und Spielerverhalten bleiben gleich, eine Umstellung beim Deploy ist nicht nötig.
- Ein Rollback bedeutet, den PR zurückzunehmen. Daten sind nicht betroffen.

## Open Questions

Geklärt durch den Spike (Welle A, Tasks 1.1–1.4, Ergebnisse am 2026-09-25 nachgetragen). Umgebung: JDK 25.0.3 (Temurin, `Temurin-25.0.3+9-LTS`), Gradle 9.7.1 (Wrapper) bzw. Gradle 9.5.0 (Standalone-Prototyp), Linux-Entwicklermaschine (32 Kerne, 62 GiB RAM). Der komplette Prototyp-Code war Wegwerfcode und wurde vor dem Commit entfernt.

### 1.1 Version, Java 25, Artefakte

- **Aktuelle stabile Version:** `io.avaje:avaje-inject:12.7`, ermittelt über `https://repo1.maven.org/maven2/io/avaje/avaje-inject/maven-metadata.xml` (abgefragt 2026-09-25). Metadata nennt `12.7-javax` als `<latest>`/`<release>`, das ist aber die Variante für die alte `javax.annotation`-Namensgebung; `12.7` ist die aktuelle Version für `jakarta.*` (passend zu Minestom/Java 25) und die letzte Nicht-„-javax“-Version in der Versionsliste.
- **Java 25:** Bestätigt. Ein eigenständiger Gradle-Prototyp mit Toolchain `JavaLanguageVersion.of(25)` (Temurin 25.0.3) kompiliert und läuft mit `io.avaje:avaje-inject:12.7` plus `io.avaje:avaje-inject-generator:12.7` als `annotationProcessor` ohne Fehler oder Warnungen zur Java-Version.
- **Artefakte:**
  - Laufzeit: `io.avaje:avaje-inject:12.7`
  - Generator (`annotationProcessor`): `io.avaje:avaje-inject-generator:12.7`
  - Test: `io.avaje:avaje-inject-test:12.7` (gleiche Versionsschiene, in Maven Central vorhanden; im Spike nicht ausprobiert, da für 1.1–1.4 kein Test-Szenario gebraucht wurde)
  - `jakarta.annotation-api`: **nicht nötig.** `avaje-inject` zieht laut POM nur `jakarta.inject:jakarta.inject-api:2.0.1` transitiv, **nicht** `jakarta.annotation-api`. Für `@Priority` reicht `io.avaje.inject.Priority`, die bereits im `avaje-inject`-Jar liegt (Entscheidung 2). `jakarta.annotation.Priority` funktioniert im Spike ebenfalls, bräuchte aber eine zusätzliche, explizite `jakarta.annotation-api`-Abhängigkeit – deshalb die Wahl für die avaje-eigene Annotation.

### 1.2 Richtung von `@Priority` und Gleichstand

Siehe auch Entscheidung 2, dort die vollständige Herleitung inkl. der Korrektur an Entscheidung 3/5.

- **Mechanismus (wichtigster Befund):** Eine per Konstruktor injizierte `List<T>` wird **nicht** nach `@Priority` sortiert, egal ob mit `jakarta.annotation.Priority` oder `io.avaje.inject.Priority` annotiert. Sortiert liefert nur `BeanScope.listByPriority(Type.class)`, explizit aufgerufen – und zwar nachweislich erst **nachdem** `BeanScope.builder().build()` zurückgekehrt ist. Ein Aufruf während des Aufbaus (injizierter `BeanScope` in einem `@Singleton`-Konstruktor oder einer `@Factory`-`@Bean`-Methode) schlägt reproduzierbar fehl: `java.lang.IllegalStateException: Proxy BeanScope can't use listByPriority() while scope is being built`.
- **Richtung:** Aufsteigend – niedrigere Werte zuerst. Prototyp: drei Beans A/B/C mit `@Priority` 300/100/200, absichtlich in dieser (verwürfelten) Reihenfolge als Java-Dateien angelegt. `scope.listByPriority(Greeter.class)` lieferte über 5 aufeinanderfolgende saubere Rebuilds (`gradle clean run`) stabil `[B(100), C(200), A(300)]`. Die geplante Prioritätstabelle (protection=100 … elytra=700) muss **nicht** gespiegelt werden.
- **Gleiche Priorität:** Drei weitere Beans B/D/E mit identischer `@Priority(100)` ergaben über 5 saubere Rebuilds eine deterministische Reihenfolge (`[B(100), D(100), E(100), …]`), die aber nicht von der Priorität, sondern von der zugrunde liegenden (stabilen) Deklarations-/Generierungsreihenfolge abhängt. Fazit: Gleichstand ist zwar in der Praxis reproduzierbar, aber nicht aus der Priorität herleitbar – die Pflicht zu eindeutigen Prioritäten (Entscheidung 6, Unique-Test) bleibt bestehen, nicht nur als Vorsichtsmaßnahme.
- **Annotation-Wahl:** `io.avaje.inject.Priority` und `jakarta.annotation.Priority` sortieren identisch über `listByPriority`. Gewählt: `io.avaje.inject.Priority`, weil sie ohne zusätzliche Abhängigkeit auskommt (siehe 1.1).

### 1.3 Annotation-Processor mit Spotless, Build-Cache und `shadowJar`

Im echten `app`-Modul getestet (Version-Catalog-Eintrag + `implementation`/`annotationProcessor` in `app/build.gradle.kts`, zwei triviale Beans – ein `@Factory` und ein `@Singleton`, siehe auch 1.4):

- **Spotless:** `./gradlew :app:spotlessCheck` und `:app:build` laufen grün mit aktivem `annotationProcessor`. Keine Konflikte zwischen Prozessor und Spotless/Eclipse-Formatter.
- **Gradle-Build-Cache:** `./gradlew :app:build --build-cache` zweimal hintereinander: zweiter Lauf komplett `UP-TO-DATE` (29 von 29 Tasks). Nach `:app:clean` erneut mit `--build-cache`: `:app:compileJava` **und** `:app:shadowJar` kommen `FROM-CACHE` (zusammen mit `:app:compileTestJava`, `:app:test`, `:app:jacocoTestReport`). Der generierte Code des Annotation-Processors destabilisiert den Cache-Key nicht.
- **`shadowJar` / `mergeServiceFiles()`:** `unzip -l app-titan.jar | grep META-INF/services` zeigt beide Service-Dateien nebeneinander:

  ```
        95  1980-02-01 00:00   META-INF/services/io.avaje.inject.spi.InjectExtension
        68  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.minimessage.MiniMessage$Provider
        69  1980-02-01 00:00   META-INF/services/org.togglz.core.spi.FeatureManagerProvider
        68  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.event.ClickCallback$Provider
        82  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.event.DataComponentValueConverterRegistry$Provider
        70  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.logger.slf4j.ComponentLoggerProvider
        78  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer$Provider
        78  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.serializer.gson.GsonComponentSerializer$Provider
        80  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer$Provider
        83  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer$Provider
        81  1980-02-01 00:00   META-INF/services/net.kyori.adventure.text.serializer.json.JSONComponentSerializer$Provider
       389  1980-02-01 00:00   META-INF/services/org.togglz.core.spi.ActivationStrategy
        65  1980-02-01 00:00   META-INF/services/jakarta.servlet.ServletContainerInitializer
        49  1980-02-01 00:00   META-INF/services/org.slf4j.spi.SLF4JServiceProvider
       250  1980-02-01 00:00   META-INF/services/org.apache.commons.logging.LogFactory
         0  1980-02-01 00:00   META-INF/services/
  ```

  `io.avaje.inject.spi.InjectExtension` (Avaje) und `org.togglz.core.spi.FeatureManagerProvider` (Togglz) liegen beide vor, `mergeServiceFiles()` (schon vorher aktiv) reicht aus. Inhalt der Avaje-Service-Datei: `io.avaje.inject.events.spi.ObserverManagerPlugin` und der generierte `net.onelitefeather.titan.app.spike.SpikeModule` – der Annotation-Processor wurde also tatsächlich ausgeführt und im Shadow-Jar landet sein Ergebnis.

### 1.4 Startzeit mit/ohne AOT-Cache

Gemessen: Zeit vom Prozessstart bis zur Log-Zeile `Minestom server started successfully` (aus `net.minestom.server.ServerProcessImpl`), 5 Läufe je Zelle, `java -jar app-titan.jar` bzw. `java -XX:AOTCache=app-titan.aot -jar app-titan.jar`, jeweils in einem eigenen Scratch-Verzeichnis unter dem Worktree (nicht im Hauptcheckout), mit Kopien von `worlds/`, `app/data/` (unbenutzt, nur vorsorglich kopiert), `data/` (LuckPerms), `flags.properties`, `portals.json`, `time.json`, `app.json` aus dem Hauptcheckout. AOT-Cache trainiert wie im Produktions-Rezept (`-Dtitan.aot.trainSeconds=15 -XX:AOTCacheOutput=app-titan.aot`, danach `-XX:AOTCache=app-titan.aot`). „main“ = unveränderter Code von `feat/avaje-dependency-injection` (Plan-Commit, kein Avaje). „avaje“ = derselbe Code plus die Abhängigkeiten aus 1.1/2.3 und zwei trivialen Beans (ein `@Factory`, ein `@Singleton`), `BeanScope.builder().build()` zusätzlich in `Titan`s Konstruktor gebaut und sofort wieder geschlossen (rein additive Messung des DI-Overheads, keine echte Modul-Verdrahtung – die kommt erst in Welle B).

| Variante | ohne AOT (Median) | mit AOT (Median) |
|---|---|---|
| main | 1,756 s | 0,804 s |
| avaje (2 Beans) | 1,806 s | 0,853 s |
| **Abweichung avaje ./. main** | **+2,85 %** | **+6,10 %** |

Beide Abweichungen liegen **unter der 10-%-Schwelle** aus Task 1.4 – die Umsetzung muss deshalb nicht pausiert werden. Einzelmesswerte (Sekunden, jeweils 5 Läufe):
- main, ohne AOT: 1,806 / 1,808 / 1,756 / 1,708 / 1,756
- avaje, ohne AOT: 1,958 / 1,858 / 1,806 / 1,757 / 1,806
- main, mit AOT: 0,903 / 0,803 / 0,904 / 0,803 / 0,804
- avaje, mit AOT: 0,903 / 0,853 / 0,803 / 0,853 / 0,803

Alle Läufe ohne Warnungen im Log (insbesondere kein AOT-Cache-Mismatch); der AOT-Cache bleibt also nutzbar (Ziel aus `proposal.md`/`design.md` bestätigt).
