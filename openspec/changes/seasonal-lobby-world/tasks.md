# Tasks

## Execution Plan

Integrationszweig: `feat/season` von `origin/main`. Welle 1 (Spike) ist abgeschlossen: D2 wird mit dem `ServiceLoader` umgesetzt. Jede Welle endet mit grünem `./gradlew build` und geprüften Diffs. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | spike | 1.1 | sonnet | Wegwerf-Branch, danach nur `openspec/changes/seasonal-lobby-world/design.md` (Ergebnis in D2) | Produktionscode im Integrationszweig |
| 2 | world-choice | 2.1–2.4 | sonnet | `core/src/main/**/module/**`, `common/src/**/map/**`, `runtime/src/main/**/bootstrap/PlatformBeans.java` | `features/**`, `apps/**`, `buildSrc/**` |
| 2 | season-column | 3.1–3.8 | sonnet | `features/season/**` | `core/**`, `common/**`, `runtime/**`, andere `features/**`, `apps/**` |
| 3 | variants-docs | 4.1–4.4 | sonnet | `apps/**`, `README.md`, `docs/lobby-modules.md` | `features/**`, `core/src/main/**`, `runtime/src/main/**` |
| 3 | verify | 4.5 | haiku | read-only | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seinen Task gelten: erst Vorhandenes nutzen (`Config`-Fassade, Minestom-`Scheduler`, `java.time`, `ServiceLoader` für die Weltwahl), Java 25 ohne Preview (Records für `Season`/`Decision`, `switch` über die Entscheidung), keine Nutzertexte, SLF4J mit Parametern (Messages aus D6), keine neuen Metriken/Spans, Test zuerst, schlanke Kommentare nur fürs Warum, Conventional Commits `feat(season): …`. F.I.R.S.T.: `Clock` und `Scheduler` injiziert, `env.tick()` statt Warten, kein `Thread.sleep`, keine Systemzeit, `@TempDir` für `worlds/`, `Config`-Werte im Test setzen und im `@AfterEach` zurücksetzen, frische `Env`/Fixtures je Test, `ServerStop` und `OnlinePlayers` gefälscht (der Test stoppt nie den Server), Erfolg nur über Assertions, WARN/INFO über einen aufgefangenen Appender.

## 1. Spike: Optional-Bean über Modulgrenzen (Welle 1)

- [x] 1.1 Auf einem Wegwerf-Branch eine Bean `LobbyWorldChoice` in eine Test-Column legen und `PlatformBeans.mapProvider(...)` einen `Optional<LobbyWorldChoice>` nehmen lassen; mit echtem `BeanScope.builder().build()` prüfen (a), ob der Optional gefüllt ist, und (b), ob `local` ohne die Column startet. Ergebnis (Optional geht / geht nicht, samt generiertem Code und Modulreihenfolge) in D2 eintragen und die Aufgaben 2.3/2.4 auf den gewählten Weg festlegen. Nachweis: D2 nennt das Ergebnis; der Wegwerf-Branch wird verworfen.

## 2. Weltwahl in `core`, `common` und `runtime` (Welle 2)

- [x] 2.1 Charakterisierung zuerst (Unit): `MapPool` wählt heute `world` bei mehreren Welten, die einzige Welt bei genau einer und wirft ohne Treffer; Test pinnt das mit `@TempDir`-Verzeichnissen. Nachweis: Test grün vor der Änderung.
- [x] 2.2 Test zuerst (Unit): `MapPool` mit übergebenem Weltnamen wählt genau diese Welt, auch bei genau einer Welt, und wirft mit dem Namen in der Meldung, wenn er fehlt; ohne Namen unverändert. Dann `MapPool`/`MapProvider.create(..., Optional<String>)` umsetzen. Nachweis: Tests aus 2.1 und 2.2 grün.
- [x] 2.3 `LobbyWorldChoice` in `core` anlegen (`Optional<String> worldName()`; Implementierungen über `ServiceLoader`, öffentlicher Konstruktor ohne Argumente). Nachweis: `./gradlew :core:build` grün.
- [x] 2.4 Test zuerst (Unit): Die Auflösung in `PlatformBeans.mapProvider(...)` wählt mit einer `LobbyWorldChoice` deren Welt, ohne Wahl die Standardwelt (Auflösung als Hilfsmethode über eine `Iterable`, damit kein echter Service-Eintrag nötig ist); mehrere Implementierungen brechen den Start mit klarer Meldung ab. Dann `PlatformBeans` auf `ServiceLoader.load(LobbyWorldChoice.class)` umstellen. Nachweis: Test grün; `apps/local` startet unverändert.

## 3. Column `features/season` (Welle 2)

- [x] 3.1 Modul anlegen: `build.gradle.kts` (`titan.column`, `libs.avaje.config`, `libs.slf4j.api`, `testImplementation(libs.logback.classic)`), `package-info.java` mit `@InjectModule(name = "seasonColumn", requires = {Scheduler.class, Clock.class, EventNode.class}, requiresString = {"…EventNode<…Event>:titan"})`, `titan/defaults/season.yaml` (D1). Nachweis: `./gradlew :features:season:build` grün, `settings.gradle.kts` unverändert.
- [x] 3.2 Test zuerst (Unit, `SeasonCalendarTest`): aktive Saison in `[from, to)`, `to` ausgeschlossen, keine Saison, abgeschaltete Saison ignoriert, Zeitzone verschiebt das Fenster, Überlappung: früheres `from` gewinnt, bei Gleichstand die kleinere Id; rot. Dann `Season` und `SeasonCalendar` umsetzen; grün.
- [x] 3.3 Test zuerst (Unit, `SeasonConfigReaderTest`, `@TempDir`-`worlds/`): `season.yaml` liefert `zone=Europe/Berlin`; fehlender `world`/`from`/`to`, unlesbares Datum, `from >= to`, fehlendes Weltverzeichnis, fehlende `map.json`, ungültige Zone, reservierte Id `zone` brechen mit qualifiziertem Schlüssel und Grund ab; abgeschaltete Saison wird nicht geprüft; Überlappung loggt WARN mit beiden Ids (aufgefangener Appender); rot. Dann `SeasonSettings`/`SeasonConfigReader` umsetzen; grün.
- [x] 3.4 Test zuerst (Unit, `RestartPolicyTest`): gleiche Welt → `NONE`; Abweichung mit Spielern → `PENDING`; Abweichung ohne Spieler → `STOP`; rot. Dann `RestartPolicy` umsetzen; grün.
- [x] 3.5 Test zuerst (Integration, Cyano-`Env`, einstellbare `Clock`, gefälschte `ServerStop`/`OnlinePlayers`, `env.tick()`): Minutentakt merkt bei Abweichung vor und loggt einmal; kein erneutes Loggen bei weiteren Takten; Abschalter zurückgesetzt hebt auf; Stopp nur bei 0 Spielern im Minutentakt; `PlayerDisconnectEvent` des letzten Spielers stoppt im nächsten Tick ohne Minutentakt; ein Spieler geht, ein anderer bleibt → kein Stopp; nichts vorgemerkt und leer → kein Stopp; höchstens ein Stopp; live aktivierte Saison ohne Welt und live ungültiger Wert → WARN, keine Vormerkung; nach Neustart-Zustand (gestartete = gewünschte Welt) → keine Vormerkung; rot. Der Test klärt, ob der ausscheidende Spieler im nächsten Tick nicht mehr zählt (D4).
- [x] 3.6 `SeasonModule` (`@Singleton`, `@PostConstruct` liest die Startwelt und plant den Minutentask samt `titan`-Listener, `@PreDestroy` bricht den Task ab und trennt den Listener), die Nahtstellen `ServerStop` (Standard: `titan-stop`-Thread mit `MinecraftServer.stopCleanly()` und `System.exit(0)`, höchstens einmal) und `OnlinePlayers` mit ihren Standardimplementierungen umsetzen; Test aus 3.5 grün.
- [x] 3.7 `SeasonWorldChoice` (öffentlich, Konstruktor ohne Argumente mit `Clock.systemUTC()`; paketinterner Konstruktor mit `Clock` für Tests) mit Eintrag unter `META-INF/services/net.onelitefeather.titan.core.module.LobbyWorldChoice` ergänzen. Test zuerst (Unit): liefert die Welt der aktiven Saison, sonst leer; ungültige aktivierte Saison bricht ab wie die Column. Nachweis: Test grün; Service-Datei im `shadowJar` von `apps/cloudnet` vorhanden.
- [x] 3.8 `ColumnArchitectureTest` nach dem Muster von `features/daytime` ergänzen. Nachweis: `./gradlew :features:season:build` grün, keine Datei außerhalb von `features/season/**` geändert.

## 4. Varianten, Doku und Abnahme (Welle 3)

- [ ] 4.1 Test zuerst (Integration, Starttest je Variante): `apps/cloudnet` lädt `seasonColumn` und erwartet sie im Startcheck; `apps/local` hat die Column nicht auf dem Klassenpfad und keine `seasons`-Standardwerte im Beispiel-`application.yaml`. Dann `apps/local/build.gradle.kts` um `titanVariant { exclude("season") }` mit einem Kommentar zum Grund (kein Supervisor) ergänzen. Nachweis: beide Starttests grün.
- [ ] 4.2 Test zuerst (Unit in `apps/cloudnet`): Die `map.json`-Konstante der Column entspricht `MapEntry.MAP_FILE_NAME`; rot bei Abweichung. Nachweis: Test grün.
- [ ] 4.3 Integrationstest in `apps/cloudnet` (echter `BeanScope`, `@TempDir`-`worlds/`, gesetzte `seasons.*`, feste `Clock`): Bei aktivem Fenster lädt die Lobby die Saison-Welt, danach vorgemerkter Neustart bei Fensterende, gefälschter Stopp. Nachweis: Test grün (deckt die Verdrahtung aus D2 ab).
- [ ] 4.4 README (Abschnitt Betrieb) und `docs/lobby-modules.md` (Tabelle der Columns, Hinweis „nur `cloudnet`“): Wie man eine Saisonwelt anlegt (Welt im Setup-Server bauen, `worlds/<name>/` mit `map.json` ablegen, `seasons.<id>.*` eintragen, Abschalter, Zeitzone), dass der Wechsel einen Neustart bei leerer Lobby braucht und ein belegter Dienst warten kann (`/stop` als Betreibermittel). Nachweis: Doku nennt Schlüssel, Beispiel und Kompromiss.
- [ ] 4.5 Verifikation (Haiku, read-only): Szenarien von `lobby-seasons` und `app-variants` Test für Test zuordnen; F.I.R.S.T.-Check (keine Sleeps, keine Systemzeit, kein Schreiben ins echte Arbeitsverzeichnis, `Config`-Werte zurückgesetzt). Nachweis: Bericht ohne Lücken.

## 5. Pull Request

- [ ] 5.1 Pull Request vom Integrationszweig auf `main` unter dem Titel `feat(season): restart the lobby into a seasonal world during its window` öffnen (Titel und Beschreibung Englisch), mit dem Hinweis, dass er PR #225 ersetzt und die Abwägung Neustart statt Live-Wechsel enthält; nach dem Merge PR #225 mit einem Kommentar schließen, der auf den neuen PR verweist (superseded). Nachweis: PR-URL, CI grün.
