# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/app-variants`, `specs/lobby-module-config`.

Ist-Zustand:

- `titan.column` (`buildSrc`) gibt Columns `minestom`, `cyano`, `mockito`, `archunit`, JUnit und `core`-Testfixtures, aber kein SnakeYAML. Produktion bekommt es transitiv über `common`. Nur `features/navigator` ergänzt `testRuntimeOnly(libs.snakeyaml)`. `mergeTestDefaults` legt jeder Column ihre eigenen `titan/defaults/*.yaml` als `application-test.yaml` auf den Test-Klassenpfad; ohne SnakeYAML liest avaje-config sie nicht als YAML.
- `apps/cloudnet` und `apps/local` haben `VariantStartTest` (gemockte `MapProvider`, `FeatureFlags`, `PermissionService`, das heißt genau `PlatformBeans`). `SeasonRestartWiringTest` und `SeasonLobbyWorldTest` setzen `Config.setProperty` und umgehen den Parser. `ApplicationYamlDefaultsCharacterizationTest` lädt die zusammengeführte `application.yaml` schon über `Configuration.builder().load(...)`, prüft aber nur einzelne Werte einiger Columns.
- Kindprozess-Tests gibt es bereits (`ConfigFileWatchIntegrationTest`, `ConfigurationPrecedenceTest`): `ProcessBuilder` mit `java.class.path`, Ausgabe-Reader, `awaitLine` mit Zeitgrenze, `waitFor(timeout)`. Sie starten aber Test-Mains, nicht `TitanApplication` aus dem Shadow-Jar.
- `TitanApplication.main` bootet Server, `Titan`-Scope und `bootstrap.start(...)`; mit `-Dtitan.aot.trainSeconds=<n>` beendet ein virtueller Thread den Prozess danach mit `System.exit(0)`. `generateAotCache` nutzt das, prüft aber nichts. `service.bind.host`/`service.bind.port` steuern die Bind-Adresse.
- Startmeldung: `VariantStartupCheck` loggt `Variant {} started with modules {}`; der Text „started successfully“ existiert nicht.
- `MapPool` braucht unter `worlds/` mindestens ein Verzeichnis mit `map.json`. Weltdaten liegen nicht im Repository.

## Goals / Non-Goals

**Goals:**
- Der Test läuft auf dem ausgelieferten Weg: Shadow-Jar, echte Datei, echter Parser, echter `BeanScope`, `ServiceLoader`.
- Ein Fehler in dieser Kette bricht `check`, nicht erst den Betrieb.

**Non-Goals:**
- Spielerverhalten im gebooteten Jar (Login, Navigator); das bleibt bei den Cyano-Tests.
- Der Datum-Uhrzeit-Fehler selbst und ein Regressionstest mit Saisonfenstern (`season-quoted-dates`).
- Den AOT-Cache im Test prüfen; `generateAotCache` bleibt unverändert.

## Decisions

### D1: JUnit-Integrationstest im eigenen Gradle-Test-Task, nicht Exec-Task

Je Variante eine Klasse `BootSmokeTest` (`@Tag("boot")`) in `apps/cloudnet/src/test` und `apps/local/src/test`. `titan.app-variant` registriert `bootSmokeTest` (Typ `Test`, Gruppe `verification`, `dependsOn(shadowJar)`, `systemProperty("titan.jar", ...)`, nur Tag `boot`), hängt ihn an `check`; der normale `test`-Task schließt den Tag aus. Die Tests brauchen so keinen Sonderfall in IDE oder `./gradlew test`, laufen aber in `build`.
Built-in: `Test`-Task mit `useJUnitPlatform { includeTags(...) }`, `ProcessBuilder`, das Muster aus `ConfigFileWatchIntegrationTest`. Verworfen: `Exec`-Task wie `generateAotCache` (Exit-Code und Log ließen sich nur per Skript prüfen, keine Assertion-Meldung mit dem Log; Self-validating verletzt); `Env`-Test im selben JVM (bootet nicht das Jar, das ausgeliefert wird, und teilt statischen Minestom-Zustand).
Test: der Test ist der Nachweis; rot zuerst über ein Jar ohne Welt (Startabbruch, Exit 1) bzw. eine verstümmelte `application.yaml`.
SOLID: SRP (ein Helfer startet, die Klassen prüfen), OCP (eine neue Variante braucht nur ihre Klasse).

### D2: Geordneter Stopp über den vorhandenen AOT-Trainingshaken

Der Kindprozess startet mit `-Dtitan.aot.trainSeconds=1`. `main` beendet ihn dann nach dem Start mit `System.exit(0)`, also über Shutdown-Hooks; der Test erwartet Exit-Code 0. Gewartet wird nur mit Zeitgrenze: `awaitLine(predicate, timeout)` für die Startmeldung `Variant <name> started with modules`, dann `waitFor(timeout)`; kein `Thread.sleep` im Test, `@Timeout` als Obergrenze. Das ist die einzige Wartestelle und für einen Integrationstest mit fremdem Prozess vertretbar; Fast ist dort bewusst gelockert (Sekunden), Independent (eigenes `@TempDir`, eigener Port), Repeatable (keine Uhrzeit, keine Netzwerkziele) und Self-validating (Assertions mit Log in der Meldung) bleiben.
Built-in: der Haken existiert und läuft schon im AOT-Training. Verworfen: SIGTERM (`destroy()` liefert Exit 143 und prüft den geordneten Pfad nicht); ein Stop-Befehl über stdin (es gibt in `core` keinen, `StopCommand` liegt in `features/admin`, das hier nicht Voraussetzung sein soll); ein neuer Haken in Produktionscode (Non-Goal, kein Produktionscode).
Test: der Integrationstest selbst; ein Log mit ERROR (Filter auf Level-Feld des Logback-Musters) oder Exit ≠ 0 lässt ihn scheitern.
SOLID: nutzt bestehende Naht statt `TitanApplication` zu ändern (OCP).

### D3: Fixture im `@TempDir`, gemeinsamer Helfer in `core` testFixtures

Der Helfer `BootedVariant` (in `core/src/testFixtures`, wo `TestTitanNode` schon liegt) legt `worlds/world/map.json` (minimal, Inhalt beim Umsetzen gegen `LobbyMap` geprüft), eine `application.yaml` mit ein paar Überschreibungen (Zahl, Liste, verschachtelt), einen freien Port (`ServerSocket(0)`, Bind-Host `127.0.0.1` über `-Dservice.bind.host`/`-Dservice.bind.port`) an und startet `java -jar $titan.jar` mit leerer Umgebung, `redirectErrorStream(true)`, Arbeitsverzeichnis = `@TempDir`. Er sammelt Ausgabezeilen, bietet `awaitLine`, `waitForExit` und beim Schließen `destroyForcibly`. Die Reader-Logik aus `ConfigFileWatchIntegrationTest` wird dabei nicht kopiert, sondern dorthin verschoben (DRY), falls das ohne Umbau der bestehenden Tests geht; sonst bleibt die Übernahme ein Folge-Refactor und der Helfer entsteht neu.
Die Betreiberdatei enthält bewusst keine Saisonfenster; deren Regression gehört `season-quoted-dates`. Sie darf den Schlüssel `seasons` nicht setzen, damit `cloudnet` die Standardwelt lädt.
Built-in: `@TempDir`, `ProcessBuilder`. Verworfen: ein eingechecktes Welt-Verzeichnis (Weltdaten liegen nicht im Repository; die Fixture braucht nur `map.json`).
Test: der Helfer wird durch die Boot-Tests abgedeckt; kein eigener Test für Dateischreiben.

### D4: Paritätstest gegen die zusammengeführte `application.yaml`

`ApplicationYamlParityTest` in `apps/cloudnet` liest die klassenpfadseitige `application.yaml` (Ergebnis von `mergeApplicationDefaults`) einmal roh mit SnakeYAML (`testImplementation(libs.snakeyaml)`), flacht sie zu Blattschlüsseln ab und prüft, dass eine über `Configuration.builder().load("application.yaml")` gebaute Instanz jeden Schlüssel kennt; danach ein zweiter Lauf mit einer Betreiberdatei aus `@TempDir` (Liste, verschachtelt, Zahl), die einzelne Werte typgerecht überschreibt. Ein still verworfener Wert wäre so ein fehlender Schlüssel. Der Test nutzt eine eigene `Configuration`-Instanz, nie die statische `Config`-Fassade (Independent, Repeatable), und keine `Thread.sleep`.
Built-in: avaje-config `Configuration.builder()` wie `ApplicationYamlDefaultsCharacterizationTest`. Verworfen: Vergleich gegen eine handgepflegte Schlüsselliste (veraltet beim nächsten Wert); Werte statt Schlüssel vergleichen (dieselbe Parsing-Quelle, kein Erkenntnisgewinn über die Existenz hinaus).
Grenze: beide Seiten benutzen SnakeYAML; der Test fängt Werte, die avaje-config beim Übernehmen verliert, nicht einen Fehler in SnakeYAML selbst.
SOLID: SRP; reine Logik, schnell, in der Testpyramide unten.

### D5: SnakeYAML in `titan.column`

`testRuntimeOnly(lib("snakeyaml"))` in `titan.column.gradle.kts` neben den anderen Test-Abhängigkeiten, mit einem Kommentar zum Grund; `features/navigator` verliert die Zeile samt ihrem Kommentar (nur die Erklärung zu `testImplementation(libs.avaje.config)` bleibt). Kein Compile-Zeit-Zugriff, daher `testRuntimeOnly`.
Built-in: Version aus dem Katalog (`snakeyaml`, 2.7). Verworfen: pro Column nachziehen (der Fehler wiederholt sich bei jeder neuen Column); `implementation` in `titan.column` (würde Main-Code an einen Parser koppeln, `navigatorDoesNotDependOnAvajeConfig` sagt Gegenteil).
Test: `./gradlew :features:<column>:dependencies --configuration testRuntimeClasspath` zeigt SnakeYAML für jede Column; `./gradlew build` bleibt grün (ein Column-Test, der bisher zufällig ohne Parser bestand, würde sonst jetzt rot und zeigt einen echten Befund).

## Risks / Trade-offs

- [LuckPerms bootet in `cloudnet` mit eigenem `data/`-Verzeichnis und langsamem Start] → Arbeitsverzeichnis ist der `@TempDir`; großzügige Zeitgrenze (120 s) als Obergrenze, nicht als Wartezeit; beim Umsetzen prüfen, ob die H2/Dateispeicherung ohne Netz startet, sonst als Befund an den Reviewer statt mocken.
- [Freier Port kann zwischen `ServerSocket.close()` und Serverstart vergeben werden] → seltener Wettlauf, Test ist wiederholbar; Alternative Port 0 wird beim Umsetzen geprüft und bevorzugt, falls Minestom ihn annimmt.
- [Boot-Tests verlängern `build` um zwei JVM-Starts] → eigener Task `bootSmokeTest`, lässt sich mit `-x bootSmokeTest` überspringen; `test` bleibt schnell.
- [Ein Test, der bisher ohne SnakeYAML lief, wird durch D5 rot] → gewollt; der Befund wird gemeldet und in derselben Änderung behoben, wenn er ein Test-Fehler ist, sonst als eigener Change.
- [`season-quoted-dates` und D5 fügen dieselbe Abhängigkeit an zwei Orten ein] → siehe Migration.

## Migration Plan

`season-quoted-dates` ergänzt `testRuntimeOnly(libs.snakeyaml)` in `features/season`. Landet dieser Change zuerst, entfällt dessen Zeile (die Column erbt sie); landet er später, streicht er sie im selben PR. Beide Wege sind mechanisch, kein Code-Konflikt. `optional-extensions-bootstrap` verschiebt Bootstrap-Code aus `TitanApplication` und ergänzt Plattform-Module in `apps/*/build.gradle.kts`; die Boot-Tests hängen nur an Startmeldung, Exit-Code und dem Haken `titan.aot.trainSeconds`, den dieser Change ausdrücklich in `runtime` lässt. Rollback: PR zurücknehmen; kein Produktionscode betroffen.
