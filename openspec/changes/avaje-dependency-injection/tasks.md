# Tasks

## Execution Plan

Integrations-Branch: `feat/avaje-dependency-injection`, abgezweigt vom aktuellen `origin/main`, nachdem PR #294 (Archiv) gemergt ist. Jeder Agent beginnt mit `git reset --hard feat/avaje-dependency-injection`. Nach jeder Welle wird zusammengeführt und gebaut, danach laufen ein Sonnet-Review (Clean Code, SOLID, DRY, F.I.R.S.T.) und ein Haiku-Check (Pyramide, Pfade, Build), Befunde behebt ein Sonnet-Agent.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| A | spike | 1.1–1.4 | sonnet | Scratch-Branch/Worktree; Ergebnisse nur in `openspec/changes/avaje-dependency-injection/design.md` (Open Questions, Entscheidung 2) | `main`-Code (der Spike-Code wird verworfen) |
| B | wiring | 2.1–2.7 | sonnet | `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/**` (`bootstrap/`, `module/`, `feature/*/…Module.java`, `Titan.java`), `app/src/test/**` (module/, architecture/, bootstrap/) | `common/**`, `setup/**`, Feature-Logik außer Konstruktoren und Annotationen |
| C | docs-template | 3.1–3.2 | sonnet | `docs/lobby-modules.md`, `README.md`, `app/src/test/.../feature/example/**` | `app/src/main/**` |
| D | Hauptkontext | 4.1–4.2, 5.1 | – | – | – |

## 1. Spike (Welle A, Ergebnis nur ins Design)

- [x] 1.1 Die aktuelle stabile Version von `io.avaje:avaje-inject` auf Maven Central bestimmen, Java 25 bestätigen und die Test-Artefakte benennen (`avaje-inject-test`). Verifikation: Version, Quelle und Datum stehen in design.md unter „Open Questions“.
- [x] 1.2 Die Richtung von `jakarta.annotation.Priority` beim Injizieren von `List<T>` in Avaje empirisch prüfen (drei Test-Beans mit 100/200/300), und das Verhalten bei gleicher Priorität festhalten. Verifikation: Ein Wegwerf-Test zeigt die Reihenfolge, das Ergebnis steht in design.md Entscheidung 2, die Tabelle wird bei Bedarf gespiegelt.
- [x] 1.3 In einem Wegwerf-Branch prüfen: Der Annotation-Processor funktioniert zusammen mit Spotless, dem Gradle-Build-Cache und `shadowJar` (`mergeServiceFiles` ist schon aktiv). Das erzeugte `META-INF/services/io.avaje.inject.spi.*` **und** die Togglz-Service-Datei liegen beide im Jar. Verifikation: `unzip -l app-titan.jar | grep META-INF/services` ist im Design dokumentiert.
- [x] 1.4 Startzeit und AOT-Cache vorher und nachher messen: Zeit bis „Minestom server started successfully“ über 5 Läufe, mit und ohne `-XX:AOTCache`, jeweils auf `main` und mit einem Avaje-Prototyp mit 2 Beans. Verifikation: Die Tabelle mit Median-Werten steht in design.md. Weicht der Median um mehr als 10 % ab, pausiert die Umsetzung und der Nutzer entscheidet.

## 2. Verdrahtung (Welle B, test-first)

- [x] 2.1 **Integrationstest zuerst:** `app/src/test/.../bootstrap/ModuleWiringTest` (Env) baut den echten `BeanScope` und prüft: alle 7 Module genau einmal, Reihenfolge von `listByPriority(LobbyModule.class)` wie in design.md Entscheidung 2, keine fehlende Abhängigkeit, sauberes `close()`. Verifikation: Der Test ist zunächst rot, weil Avaje noch fehlt, und nach 2.2–2.5 grün.
- [x] 2.2 **Unit-Tests zuerst:** ArchUnit-Regeln in `ArchitectureTest`:
  - (a) jede `LobbyModule`-Implementierung in `..app.feature..` trägt `@Singleton` und `@io.avaje.inject.Priority`,
  - (b) kein Feature-Code nutzt `BeanScope`,
  - (c) die `@Priority`-Werte der Module sind eindeutig.

  Verifikation: Jede Regel ist per absichtlich eingebautem Verstoß rot, danach grün.
- [x] 2.3 Abhängigkeiten: `avaje-inject` und `avaje-inject-generator` (12.7, aus 1.1) in den Versionskatalog, kein `jakarta.annotation-api`. Dazu `implementation` plus `annotationProcessor(avaje-inject-generator)` in `app`, `testImplementation(avaje-inject-test)` nur, falls gebraucht. Kein `testAnnotationProcessor`. Verifikation: `./gradlew :app:dependencies` zeigt sie, `./gradlew build` ist grün.
- [x] 2.4 `LobbySpawn` (funktionale Schnittstelle) einführen und `SpawnModule(Instance, LobbySpawn)` anpassen. `TickleModule` behält nur `@Inject TickleModule(Clock)`. Bestehende Tests werden nur an den Konstruktoren angepasst. Ebene: Unit- und Integrationstests der Module. Verifikation: alle Spawn- und Tickle-Tests grün.
- [x] 2.5 Alle 7 Module mit `@Singleton` und `@Priority` gemäß der Tabelle annotieren, und `@Inject`, wo nötig. `app/.../bootstrap/PlatformBeans` (`@Factory`) mit den Beans aus design.md Entscheidung 3 anlegen. `ModuleRegistry` ist **kein** Bean, ihn baut `Titan` (2.6). Verifikation: 2.1 und 2.2 sind grün.
- [x] 2.6 `Titan.java` auf `BeanScope` umstellen (design.md Entscheidung 5): nach `build()` den `ModuleRegistry` über den Builder mit `scope.listByPriority(LobbyModule.class)` und den Plattform-Beans aus dem Scope bauen, Shutdown-Reihenfolge `disableAll` → Butterfly → `close`, und eine INFO-Zeile `Lobby modules enabled in order: {}`. **Unit-Test zuerst** mit einem abgefangenen Appender, der die Log-Zeile mit der Reihenfolge prüft. Verifikation: Der Test ist grün, `Titan` enthält keine Modulliste mehr.
- [x] 2.7 Verifikation der Welle: `./gradlew build` ist grün, inklusive aller bestehenden Tests, ArchUnit und Spotless.

## 3. Doku und Vorlage (Welle C)

- [x] 3.1 `ExampleModule` (Testquellen) bekommt `@Singleton`/`@Priority`/`@Inject`. Der Processor läuft nicht für Tests. Verifikation: Der Test des Beispiels ist grün, der `ModuleWiringTest` findet das Beispiel nicht.
- [x] 3.2 `docs/lobby-modules.md` und die README beschreiben den neuen Weg: neues Paket, `@Singleton` plus `@Priority`, Dienste per Konstruktor, neue gemeinsame Dienste als `@Bean` in `PlatformBeans` oder als eigener `@Singleton`, die Tabelle der Prioritäten, und die Checkliste „null Zeilen außerhalb des Pakets“. Verifikation: Jede Angabe zu einer API ist gegen den Code geprüft (Review-Agent).

## 4. Abnahme (Welle D)

- [ ] 4.1 **E2E-Smoke-Test:** Das Shaded-Jar in einem Scratch-Verzeichnis starten, mit einer Kopie der Welten und `-Dtitan.aot.trainSeconds=15`. Prüfen: Die Log-Zeile mit der Startreihenfolge ist vorhanden, der Start ist sauber, das Herunterfahren ohne Exception. Außerdem eine absichtlich fehlende Abhängigkeit (z.B. per Test-Profil bzw. entferntem `@Bean`) als Probe: Der Start bricht mit einer Meldung ab, die das Modul und den Typ nennt. Verifikation: Die wichtigsten Log-Zeilen stehen im PR.
- [ ] 4.2 Lokale Abnahme im Client durch den Maintainer: Die Lobby verhält sich unverändert (Feder und Navigator, Sitzen, Kitzeln, Elytra, Höhen-Teleport, Respawn). Verifikation: Die Checkliste im PR ist abgehakt.

## 5. Pull Request

- [ ] 5.1 Den Pull Request `feat(app): discover lobby modules with Avaje Inject` von `feat/avaje-dependency-injection` nach `main` öffnen. Die Beschreibung enthält die Messwerte aus dem Spike, die Tabelle der Prioritäten und die Checkliste zur Abnahme. Verifikation: Der PR existiert, die CI ist grün.
