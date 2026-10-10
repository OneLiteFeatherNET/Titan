# Tasks

## Execution Plan

Integrationszweig: `test/boot-smoke-test` von `origin/main`. Jede Welle endet mit grünem `./gradlew build` (inklusive `bootSmokeTest`) und geprüften Diffs. Reihenfolge: Welle 1 zuerst (D5 kann Column-Tests rot färben), Welle 2 und 3 parallel in getrennten Worktrees, Welle 4 danach.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | column-yaml | 1.1–1.3 | sonnet | `buildSrc/src/main/kotlin/titan.column.gradle.kts`, `features/navigator/build.gradle.kts`, Column-Tests nur zur Behebung neu sichtbarer Fehler | `*/src/main/**`, `apps/**`, `runtime/**` |
| 2 | boot-smoke | 2.1–2.5 | sonnet | `core/src/testFixtures/**`, `buildSrc/src/main/kotlin/titan.app-variant.gradle.kts`, `apps/*/src/test/**/BootSmokeTest.java`, `apps/*/build.gradle.kts` | `*/src/main/**`, `features/**`, `buildSrc/.../titan.column.gradle.kts` |
| 2 | yaml-parity | 3.1–3.2 | sonnet | `apps/cloudnet/src/test/**/ApplicationYamlParityTest.java`, `apps/cloudnet/build.gradle.kts` (nur `testImplementation(libs.snakeyaml)`) | alles außer den genannten Pfaden |
| 3 | verify | 4.1 | haiku | read-only | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln für seinen Task: erst Vorhandenes nutzen (`Test`-Task mit JUnit-Tags, `ProcessBuilder`, das Muster aus `ConfigFileWatchIntegrationTest`, avaje-config `Configuration.builder()`, den Haken `titan.aot.trainSeconds`), Java 25 ohne Preview, kein Produktionscode, keine Nutzertexte, SLF4J-Regeln unverändert (kein neues Logging), keine neuen Metriken/Spans, Test zuerst, schlanke Kommentare nur fürs Warum, Conventional Commits `test(runtime): …` (der Build-Skript-Anteil `build(runtime): …` nur, wenn er allein steht; ein Commit je Typ). F.I.R.S.T.: `@TempDir` je Test, eigener Port, keine `Thread.sleep`, kein Lesen der Systemzeit; die einzigen Wartestellen der Boot-Tests sind `awaitLine(…, timeout)` und `waitFor(timeout)` mit `@Timeout` als Obergrenze; Erfolg nur über Assertions, die das Log in der Meldung tragen; die statische `Config`-Fassade wird im Paritätstest nicht benutzt.

## 1. SnakeYAML für alle Column-Tests (Welle 1)

- [x] 1.1 Charakterisierung zuerst (Befund, kein neuer Test): `./gradlew :features:daytime:dependencies --configuration testRuntimeClasspath` (und eine zweite Column ohne Navigator) zeigt heute kein SnakeYAML; als Ausgangsbefund in der PR-Beschreibung festhalten. Nachweis: Befund notiert.
- [x] 1.2 `testRuntimeOnly(lib("snakeyaml"))` in `titan.column.gradle.kts` ergänzen (Kommentar: Tests lesen `application-test.yaml` mit dem Parser des Betriebs) und die redundante Zeile samt Kommentar in `features/navigator/build.gradle.kts` streichen. Nachweis: dieselbe `dependencies`-Abfrage zeigt SnakeYAML für jede Column; `DefaultNavigatorFeatureFlagsTest` bleibt grün.
- [x] 1.3 `./gradlew build` ausführen; wird ein Column-Test durch den echten Parser rot, ist das ein Befund: Test-Fehler hier beheben, Produktionsfehler als eigenen Change melden (`season-quoted-dates` deckt die Datum-Uhrzeit-Werte ab). Nachweis: `./gradlew build` grün oder Befundliste im PR.

## 2. Boot-Smoketest je Variante (Welle 2, Integration)

- [x] 2.1 Test zuerst (Integration, `apps/cloudnet`): `BootSmokeTest` (`@Tag("boot")`, `@Timeout`) startet über den Helfer das Jar aus `-Dtitan.jar`, wartet auf `Variant cloudnet started with modules`, dann auf Exit 0 und prüft, dass keine Zeile das Level ERROR trägt; rot, weil Helfer und Task fehlen. Nachweis: Test kompiliert nicht bzw. schlägt fehl.
- [x] 2.2 Helfer `BootedVariant` in `core/src/testFixtures` umsetzen: `@TempDir`-Arbeitsverzeichnis mit `worlds/world/map.json` und `application.yaml` (Überschreibungen für Zahl, Liste, verschachtelten Abschnitt; kein `seasons`), freier Port über `-Dservice.bind.port` (Port 0 zuerst prüfen), `-Dtitan.aot.trainSeconds=1`, leere Umgebung, gemeinsamer Reader mit `awaitLine`/`waitForExit`/`close`. Reader-Logik aus `ConfigFileWatchIntegrationTest` übernehmen statt kopieren, wenn das ohne Umbau dieser Tests geht. Nachweis: Test aus 2.1 grün gegen das echte Shadow-Jar.
- [x] 2.3 Task `bootSmokeTest` in `titan.app-variant.gradle.kts`: Typ `Test`, `dependsOn(shadowJar)`, `systemProperty("titan.jar", …)`, `useJUnitPlatform { includeTags("boot") }`, Teil von `check`; `test` schließt den Tag aus. Nachweis: `./gradlew :apps:cloudnet:test` startet kein Jar, `./gradlew :apps:cloudnet:bootSmokeTest` startet es, `./gradlew check` enthält den Task.
- [x] 2.4 Dasselbe `BootSmokeTest` für `apps/local` (Startmeldung `Variant local …`, ohne `seasons`, ohne LuckPerms). Nachweis: `./gradlew :apps:local:bootSmokeTest` grün.
- [x] 2.5 Negativprobe (kein eingecheckter Test, Nachweis im PR): Welt entfernen bzw. `application.yaml` mit ungültigem YAML → der Test scheitert mit dem Log in der Meldung. Nachweis: beide Läufe rot mit lesbarer Meldung, danach zurückgesetzt.

## 3. YAML-Parität (Welle 2, Unit-artig)

- [x] 3.1 Test zuerst (`ApplicationYamlParityTest`, `apps/cloudnet`): Die klassenpfadseitige `application.yaml` wird roh mit SnakeYAML gelesen und zu Blattschlüsseln abgeflacht; eine eigene `Configuration`-Instanz (`Configuration.builder().load("application.yaml")`) muss jeden Schlüssel kennen; ein zweiter Test überschreibt per Betreiberdatei im `@TempDir` eine Zahl, eine Liste und einen verschachtelten Wert und liest sie typgerecht. Nachweis: Test grün mit SnakeYAML; rot, wenn ein Schlüssel im Test testweise aus der `Configuration` entfernt wird.
- [x] 3.2 `testImplementation(libs.snakeyaml)` in `apps/cloudnet/build.gradle.kts` für den Rohleser ergänzen, falls der Klassenpfad ihn nicht schon zur Compile-Zeit liefert. Nachweis: `./gradlew :apps:cloudnet:test` grün.

## 4. Abnahme (Welle 3)

- [ ] 4.1 Verifikation (Haiku, read-only): Szenarien von `app-variants` und `lobby-module-config` aus diesem Change Test für Test zuordnen; F.I.R.S.T.-Check (keine Sleeps außer den begrenzten Wartestellen, keine Systemzeit, eigener Port und `@TempDir` je Test, keine statische `Config`); Diff enthält keinen Produktionscode. Nachweis: Bericht ohne Lücken.

## 5. Pull Request

- [x] 5.1 Pull Request vom Integrationszweig auf `main` unter dem Titel `test(runtime): boot the shipped jar and parse a real application.yaml` öffnen (Titel und Beschreibung Englisch), mit dem Hinweis auf die Überschneidung mit `season-quoted-dates` (dessen SnakeYAML-Zeile in `features/season` wird redundant) und den Befunden aus 1.3. Nachweis: PR-URL, CI grün.
