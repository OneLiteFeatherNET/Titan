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

### 2. Module werden automatisch gefunden, als `@Singleton` mit `jakarta.annotation.Priority`

**Entscheidung:** Jedes Feature-Modul ist `@Singleton` mit `@Priority(n)`. Der `ModuleRegistry` bekommt `List<LobbyModule>` injiziert, die Avaje nach `@Priority` sortiert.

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

Welche Richtung Avaje sortiert (ob ein niedriger Wert zuerst kommt), klärt der Spike (Task 1). Die Tabelle wird dann bei Bedarf gespiegelt, der Ansatz bleibt.

**Deterministisch bei gleicher Priorität:** Wir verlassen uns nicht auf eine Reihenfolge bei Gleichstand. Die Prioritäten müssen **eindeutig** sein, das prüft ein Test (Entscheidung 6). Damit ist die Spec-Anforderung „gleiche Priorität ist deterministisch“ erfüllt, ohne eine eigene Sortierung oder Reflection zur Laufzeit.

**Built-in first:** `jakarta.annotation.Priority` ist die Standard-Annotation, die Avaje selbst für das Ordnen von Listen auswertet. Eine eigene Methode `LobbyModule#priority()` wurde verworfen, weil sie dieselbe Information doppelt pflegen würde.

**Test:** Unit-Test für die Eindeutigkeit (ArchUnit oder ein Reflection-Test **nur im Test**, Entscheidung 6) und ein Integrationstest für die Startreihenfolge.

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
- `ModuleRegistry`

Der `ModuleRegistry`-Bean wird über den bestehenden Builder gebaut, mit dem injizierten `List<LobbyModule>`. Der Builder bleibt als öffentliche API für Tests und den `ModuleHarness` erhalten.

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

**Entscheidung:** `Titan` baut `BeanScope.builder().build()` und holt `ModuleRegistry` aus dem Scope. `initialize()` ruft wie heute `enableAll()` auf und registriert die Plattform-Befehle `stop`/`end` und Butterfly. Beim Herunterfahren gilt diese Reihenfolge: `moduleRegistry.disableAll()`, danach Butterfly, danach `beanScope.close()`, jeweils als Shutdown-Tasks in dieser Reihenfolge (FIFO). Fehler beim Aufbau des Scopes, etwa eine fehlende Abhängigkeit, landen als `RuntimeException` im bestehenden Abbruch beim Start in `TitanApplication` (Log-Zeile und Exit-Code 1).

**Logging:** Nach `enableAll()` gibt es **eine** INFO-Zeile mit der Startreihenfolge, parametrisiert: `Lobby modules enabled in order: {}` mit der Liste der IDs. Das ist ein Lebenszyklus-Ereignis im Sinne der Logging-Regeln.

**Test:** Der Smoke-Test des Jars (Task 7) prüft den Start, die Log-Zeile, das saubere Herunterfahren und den Abbruch bei fehlender Abhängigkeit.

**SOLID:** SRP, weil `Titan` nur noch den Bootstrap macht.

### 6. Schutzgeländer: Test der Verdrahtung und ArchUnit-Regeln

Das größte Risiko von automatischem Finden ist ein Modul, das still fehlt. Dagegen gibt es drei Absicherungen:
- **`ModuleWiringTest`** (Integrationstest mit `Env`, weil die Factory Minestom-Singletons braucht): Er baut den echten `BeanScope` und prüft vier Dinge:
  - alle 7 bekannten Module sind genau einmal vorhanden,
  - die Reihenfolge der `List<LobbyModule>` entspricht der Tabelle aus Entscheidung 2,
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

- Die exakte Avaje-Version und die konkreten Messwerte für Startzeit und AOT: Beides legt der Spike fest und trägt es hier nach. Ansatz und Tasks ändern sich dadurch nicht.
