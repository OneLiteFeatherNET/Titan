# Tasks

## Execution Plan

Integrationszweig: `feat/jumprun-persistent-scores` von `origin/main`. Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig. Ihre Diffs werden nach Review gemergt. Jede Welle endet mit grünem `./gradlew build` (ohne Docker) und, ab Welle 2, mit grünem `./gradlew integrationTest` (mit Docker).

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | spike | 1.1 | haiku | read-only, Ergebnis in `design.md` D9 | Code |
| 1 | persistence | 1.2–1.5 | sonnet | `persistence/**`, `settings.gradle.kts` (nur `include("persistence")` und Katalogeinträge), `buildSrc/**` (nur falls die Test-Suite eine Convention braucht) | `features/**`, `core/**`, `runtime/**`, `apps/**` |
| 1 | pure-logic | 2.1–2.2 | sonnet | `features/jumprun/src/{main,test}/java/**` (nur `FinishedRun`, `TopEntry`, `TopThree`, `RunSidebarContent`), `features/jumprun/src/main/resources/titan/jumprun/**` | `build.gradle.kts`, `JumprunModule`, alles außerhalb `features/jumprun` |
| 2 | store | 3.1–3.3 | sonnet | `features/jumprun/**` (Build, Entity, Migration, `RunStore`, `HibernateRunStore`, `FakeRunStore` in Test-Fixtures) | `persistence/**`, `JumprunModule`, `Run` |
| 3 | records-leaderboard | 4.1–4.3 | sonnet | `features/jumprun/**` (`RunRecords`, `InMemoryRunRecords`, `StoredRunRecords`, `Leaderboard`, jumprun-`@Factory`) | `persistence/**`, `RunSidebar`, `Run` |
| 4 | wiring-sidebar | 5.1–5.4 | sonnet | `features/jumprun/**` | alles außerhalb `features/jumprun` |
| 4b | config-passthrough | 8.1–8.2 | sonnet | `persistence/**` | `features/**`, alles andere |
| 5 | docs | 6.1 | sonnet | `docs/**`, `README.md` | Code |
| 5 | smoke | 6.2 | sonnet | nur lokale Läufe (Jar kopieren, nicht im Worktree des laufenden `titan-local.jar` bauen), Ergebnis in den PR-Text | Code |
| 6 | verify | 6.3 | haiku | read-only | alles |
| 7 | pr | 7.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seine Aufgabe gelten.

- **Built-in first:** Avaje `@Factory`/`@Bean`/`@RequiresProperty`/`@RequiresBean`/`@Secondary`/`Optional<T>`-Injektion, Hibernate `HibernatePersistenceConfiguration` und `inTransaction`, Flyway-Lock, Minestom `AsyncPlayerConfigurationEvent`, `SchedulerManager`, `Sidebar` mit `NumberFormat.blank()`, virtuelle Threads, Gradle `jvm-test-suite`.
- **Java 25 ohne Preview:** Records für `FinishedRun`, `TopEntry`, `TopThree`, `PersistenceUnit`; `_` wo passend.
- **Threads:** Datenbankzugriff nie auf dem Tick-Thread oder dem `TickSchedulerThread`; Minestom-Tasks reichen nur an den Executor weiter.
- **Texte:** Nutzertexte nur über die Bundles, Schlüssel `titan.jumprun.sidebar.*`, Englisch als Fallback.
- **Logging:** SLF4J mit Parametern, Exception als letztes Argument, jede Exception genau einmal loggen; niemals URL, Benutzer oder Passwort loggen. Keine Metriken oder Spans (design.md Non-Goals).
- **Arbeitsweise:** Test zuerst, schlanke Kommentare nur fürs Warum, Conventional Commits `feat(jumprun): …` (auch für `persistence`, es entsteht für dieses Feature).
- **F.I.R.S.T.:** injizierte `Clock` statt `Instant.now()`, synchroner Executor in Unit-Tests, kein `Thread.sleep`, frische `Env`/Fixtures/Datenbank je Test, `env.tick()` statt Warten, Erfolg nur über Assertions, WARN-Logs über `ListAppender` geprüft, kein geteilter statischer Zustand. Unit-Tests ohne Docker und Netzwerk; Testcontainers nur in `integrationTest`.

## 1. Modul `persistence` (Welle 1)

- [x] 1.1 Spike (read-only): Klären, wie avaje-config 5.2 `titan.database.url/user/password` aus Umgebungsvariablen bezieht (Platzhalter `${TITAN_DATABASE_URL}` im Profil oder direktes Mapping), und ob `@RequiresProperty` einen leeren Wert als vorhanden wertet. Dazu die aktuellen Stabilversionen von `hibernate-core` 7, HikariCP, `postgresql`, Flyway und `testcontainers` prüfen. Nachweis: Ergebnis als Absatz in `design.md` D9 und D10.
- [x] 1.2 Versionskatalog um die Bibliotheken aus design.md D10 ergänzen, `persistence` in `settings.gradle.kts` aufnehmen, `persistence/build.gradle.kts` (java-library, Avaje Inject + Generator, avaje-config, SLF4J) mit Gradle-Test-Suite `integrationTest` (Testcontainers), die `check` einschließt und `test` nicht. Nachweis: `./gradlew :persistence:build` grün ohne Docker, `./gradlew :persistence:integrationTest` läuft (noch leer).
- [x] 1.3 Test zuerst (Unit, Avaje `BeanScope` im Test): Ohne `titan.database.url` existieren keine `DataSource`- und keine `SessionFactory`-Bean. Mit URL auf eine nicht erreichbare Adresse (`jdbc:postgresql://127.0.0.1:1/x`, kurze Hikari-Timeouts) bricht der Aufbau mit einer Exception ab, deren Text und Cause-Kette das gesetzte Passwort nicht enthalten. Rot. Dann `record PersistenceUnit(String name, List<Class<?>> entities)`, `DatabaseSettings` und `DatabaseFactory` (`@Factory @RequiresProperty("titan.database.url")`, `@Bean HikariDataSource` mit `@PreDestroy close`) samt `titan/defaults/database.yaml` ohne `url` (nur `pool.maximumSize: 4`). Grün.
- [x] 1.4 Test zuerst (Integration, Testcontainers PostgreSQL, Test-Unit mit einer Mini-Entity und einer Migration unter `db/migration/<name>/`): Leere DB → Tabelle angelegt, `SessionFactory` mit `validate` gebaut. Zweiter Aufbau → keine Migration erneut angewendet. Zwei Aufbauten parallel in zwei Threads → beide fertig, jede Version genau einmal in der History-Tabelle der Unit. Ein Entity-Feld ohne Spalte → Start bricht ab. Rot. Dann Flyway pro `PersistenceUnit` (Ort `classpath:db/migration/<name>`, History-Tabelle `flyway_<name>_history`) und `@Bean SessionFactory` über `HibernatePersistenceConfiguration` (`managedClasses`, `nonJtaDataSource`, `hbm2ddl=validate`, `@PreDestroy close` vor dem Pool). Grün.
- [x] 1.5 Test zuerst (Unit, `ListAppender`): Nach dem Aufbau genau ein INFO `Database ready` mit Key-Values `units` und `migrations`, ohne URL, Benutzer oder Passwort. Rot, dann umsetzen, grün. `package-info.java` mit `@InjectModule(name = "persistence", …)` und `ColumnArchitectureTest`-Pendant, falls die Regeln auf Nicht-Columns passen. Nachweis: `./gradlew :persistence:build :persistence:integrationTest` grün.

## 2. Reine Logik und Texte (Welle 1)

- [x] 2.1 Test zuerst (Unit, `TopThreeTest`): Einsortieren in absteigender Reihenfolge, höchstens 3 Einträge, ein Spieler nur einmal (ein höherer Wert ersetzt, ein niedrigerer ändert nichts), Gleichstand → früheres `achievedAt` vorn, unveränderte Eingabe liefert gleiches Objekt. Rot. Dann `record FinishedRun(UUID player, String name, Mode mode, int score, EndReason endReason, Instant finishedAt)`, `record TopEntry(UUID player, String name, int score, Instant achievedAt)`, `record TopThree(List<TopEntry>)` mit `with(TopEntry)`. Grün.
- [x] 2.2 Test zuerst (Unit, `RunSidebarContentTest`): Aus Score, `OptionalInt` Rekord, Top-Liste (oder keine), eigener UUID und Locale entstehen die Zeilen-Components. Nur die eigene Top-Zeile ist `BOLD`. Ohne Rekord steht `–`. Ohne Leaderboard gibt es keine Leer- und Top-Zeilen. Übertrifft der Score den Rekord, zeigt die Rekordzeile den Score. `de_DE` rendert deutsch, `ja_JP` englisch. Rot. Dann die Schlüssel `titan.jumprun.sidebar.score` und `titan.jumprun.sidebar.record` in `messages_en`/`messages_de`, `RunMessages` um die beiden Render-Methoden erweitern und die reine Funktion `RunSidebarContent` schreiben. Grün, und `RunMessagesBundleTest` bleibt grün.

## 3. Speicher der Läufe (Welle 2)

- [x] 3.1 `features/jumprun/build.gradle.kts`: `implementation(project(":persistence"))`, Hibernate-API, `integrationTest`-Suite wie in 1.2, `testFixtures` für `FakeRunStore`. Nachweis: `./gradlew :features:jumprun:build` grün ohne Docker.
- [x] 3.2 Test zuerst (Integration, Testcontainers, frische DB je Test): Die Migration `db/migration/jumprun/V1__create_jumprun_run.sql` legt `jumprun_run` mit Spalten, Check `score >= 0` und den Indizes aus D3 an, `validate` gegen `JumprunRunEntity` passt. Rot. Dann Migration, Entity (`@Enumerated(STRING)` für `mode` und `end_reason`, natives `uuid`) und `JumprunPersistence` (`@Factory`, `@Bean PersistenceUnit("jumprun", List.of(JumprunRunEntity.class))`). Grün.
- [x] 3.3 Test zuerst (Integration): `append` speichert einen Lauf mit Score 0. `bestsOf` liefert das Maximum pro Modus und ignoriert andere Spieler. `topThreeOfEveryMode`: Hard mit Alex 88, Steve 61, Notch 42, Jeb 30 → genau Alex, Steve, Notch; ein Spieler mit zwei Läufen erscheint einmal mit dem besten; bei Gleichstand gewinnt der frühere; der Name kommt vom jüngsten Lauf; Modi ohne Läufe fehlen oder sind leer. Rot. Dann `RunStore` (Interface), `HibernateRunStore` (`@Singleton @RequiresBean(SessionFactory.class)`, HQL für `bestsOf`, native SQL für die Top 3, `inTransaction` für `append`) und `FakeRunStore` in den Test-Fixtures. Grün.

## 4. Rekorde und Bestenliste (Welle 3)

- [x] 4.1 Charakterisierung zuerst: `InMemoryRunRecordsTest` an das neue `submit(FinishedRun)` anpassen und sicherstellen, dass er das heutige Verhalten unverändert festhält (Rekord pro Modus, `forget`), dazu `load` als No-op. Dann Signatur in `RunRecords`/`InMemoryRunRecords` ändern und die Aufrufer in `JumprunModule` mit einer injizierten `Clock` umstellen (`@Bean Clock` in der jumprun-Factory). Nachweis: alle bisherigen jumprun-Tests grün.
- [x] 4.2 Test zuerst (Unit, `StoredRunRecordsTest`, `FakeRunStore`, synchroner Executor): `load` füllt die Rekorde aus dem Store. `submit` meldet Rekord oder keinen Rekord gegen den geladenen Wert und hängt jeden Lauf an, auch Score 0. Ein Store-Fehler beim Anhängen lässt den Rekord im Cache und loggt genau ein WARN `Could not store jump and run run`. Ein Fehler beim Laden lässt den Cache leer und loggt ein WARN. `forget` leert. `close` wartet auf ausstehende Schreibvorgänge. Rot. Dann `StoredRunRecords` (`@Singleton @RequiresProperty(DatabaseProperties.URL)`, komponiert `InMemoryRunRecords`, schreibt über den `DatabaseWriter` aus `persistence`, der vor der `SessionFactory` geschlossen wird). Grün.
- [x] 4.3 Test zuerst (Unit, `LeaderboardTest`, `FakeRunStore`): `refresh` übernimmt die Top 3 aller Modi. `offer` sortiert sofort ein. Ein angebotener Wert, den der Store noch nicht hat, überlebt `refresh`. Ein Store-Fehler behält die alte Map und loggt ein WARN `Could not refresh jump and run leaderboard`. `top(Mode)` liefert nie Einträge eines anderen Modus. Rot. Dann `Leaderboard` (`@Singleton @RequiresProperty(DatabaseProperties.URL)`, `AtomicReference<Map<Mode, TopThree>>`). Grün.

## 5. Verdrahtung und Sidebar (Welle 4)

- [x] 5.1 Test zuerst (Integration, Cyano-`Env`, `FakeRunStore`-Bean): Ein Spieler mit gespeichertem Hard-Rekord 15 tritt bei → `load` lief vor dem Spawn. Ein Lauf in Hard mit Score 15 ist kein neuer Rekord. Nach Disconnect ist der Cache leer, der Store behält den Lauf. Rot. Dann `AsyncPlayerConfigurationEvent` am `FeatureNode` → `records.load(...)`. Grün.
- [x] 5.2 Test zuerst (Integration, `env.tick()`): Laufstart → der Läufer bekommt das Sidebar-Objective mit Titel „Jump & Run · Hard“, Score 0 und Rekord 42, ein zweiter Spieler bekommt keines. Score 7 → Zeile aktualisiert, Action Bar zeigt weiter 7. Laufende durch Absturz, Abbruch, Elytra, Tod, Disconnect und Shutdown → Objective entfernt. Ohne `Leaderboard` (leeres `Optional`) → keine Top-Zeilen. Rot. Dann `RunSidebar` (Minestom `Sidebar`, ein Viewer, `NumberFormat.blank()`, nur geänderte Zeilen senden) im `Run` neben `ScoreLabel`, aktualisiert an denselben Stellen wie die Action Bar und abgeräumt in jedem `end`-Zweig. Grün.
- [x] 5.3 Test zuerst (Integration): Mit Top 3 Alex 88, Steve 61, Notch 42 zeigt Steves Hard-Sidebar seine Zeile fett. Jeb erreicht im Lauf 50 → seine Sidebar zeigt sofort „3. Jeb 50“ fett, Notch fehlt. Ein Spieler mit Easy-Rekord 30 sieht in Hard nur Hard-Werte. Nach einem simulierten `refresh()` mit neuen Store-Daten aktualisieren laufende Sidebars. Rot. Dann `Optional<Leaderboard>` in `JumprunModule`, `offer` beim Überschreiten des eigenen Rekords, Minestom-`Task` alle 30 s, die nur `refresh` an den Executor gibt, plus ein erster `refresh` beim Start, und das Aktualisieren laufender Sidebars unter dem Run-Lock. Grün.
- [x] 5.4 Test zuerst (Unit, Avaje-Verdrahtung der Column ohne DB-Konfiguration): `RunRecords` ist `InMemoryRunRecords`, `Optional<Leaderboard>` ist leer, keine Hibernate-Klasse wird geladen (ArchUnit oder Classloading-Prüfung). Rot, falls Verdrahtung fehlt. Dann ggf. anpassen, grün. Nachweis: `./gradlew build integrationTest` grün.

## 8. Pool und ORM per YAML (Welle 4b)

- [x] 8.1 Test zuerst (Unit, `DatabaseSettingsTest`):
  - `titan.database.hikari.maximumPoolSize: 10` landet in der `HikariConfig`.
  - Ein unbekannter Hikari-Schlüssel bricht mit dem Schlüsselnamen ab, ohne Wert in der Meldung.
  - `titan.database.hibernate.jdbc.batch_size: 20` landet als `hibernate.jdbc.batch_size`.
  - `hibernate.hbm2ddl.auto` mit `update`/`create` sowie die JPA-Schema-Action brechen mit Nennung der Einstellung ab; `validate`, `none` und fehlend sind erlaubt.
  - Verbindungsschlüssel am Pool vorbei (`hibernate.connection.url/username/password/driver_class/provider_class/datasource`, `jakarta.persistence.jdbc.*`) werden abgelehnt, Abstimmungsschlüssel wie `hibernate.connection.handling_mode` nicht.
  - `url`/`user`/`password` gewinnen gegen `hikari.jdbcUrl`/`username`/`password`.

  Rot. Dann `DatabaseSettings` über avaje-config `forPath(...).asProperties()`, `HikariConfig(Properties)` und die Hibernate-Properties in `DatabaseFactory`. `titan.database.pool.*` entfällt, und die Defaults (`maximumPoolSize: 4`, `connectionTimeout: 5000`, `initializationFailTimeout: 5000`, `poolName: titan`) stehen nur in `database.yaml` mit kommentierten Beispielen. Grün.
- [x] 8.2 Test zuerst (Integration, Testcontainers): Mit `hikari.maximumPoolSize: 10` meldet der laufende Pool 10. `hibernate.jdbc.batch_size: 20` steht in `SessionFactory.getProperties()`. Mit `hibernate.hbm2ddl.auto: update` bricht der Start ab. Rot, dann ggf. anpassen, grün. Nachweis: `./gradlew build` grün.

## 6. Doku und Abnahme (Welle 5–6)

- [x] 6.1 `docs/lobby-modules.md` und README ergänzen. Inhalt: Modul `persistence`, die Schlüssel `titan.database.*` samt Umgebungsvariablen aus 1.1, die durchgereichten Abschnitte `hikari`/`hibernate` mit Beispielen und den gesperrten Einstellungen, Verhalten ohne URL und bei Ausfall, Tabelle `jumprun_run`, Sidebar, Hinweis zum AOT-Training mit DB-Konfiguration und die Rollback-Schritte aus design.md (Migration Plan). Nachweis: Doku nennt Schlüssel, Fallback und Rollback.
- [x] 6.2 Smoke-Test mit dem Shaded-Jar gegen eine lokale PostgreSQL (Docker), echter Client, deutsch und englisch. Zu prüfen:
  - Start legt `jumprun_run` an, und das Log zeigt `Database ready` ohne Zugangsdaten.
  - Läufe in zwei Modi; die Sidebar zeigt nur den laufenden Modus.
  - Score in Sidebar und Action Bar.
  - Neuer Rekord rückt fett in die Top 3.
  - Rejoin behält den Rekord, Neustart behält ihn.
  - Sidebar verschwindet beim Laufende.
  - Start ohne URL läuft wie bisher.
  - Datenbank im Betrieb stoppen: Lauf geht weiter, WARN im Log.
  - Start mit URL, aber gestoppter Datenbank bricht ab.

  Nachweis: Checkliste mit Ergebnis im PR-Text.
- [x] 6.3 Verifikation (read-only): Jedes Szenario aus `specs/lobby-jumprun/spec.md` und `specs/lobby-persistence/spec.md` ist einem Test oder einem Smoke-Punkt zugeordnet. Die Tests werden auf F.I.R.S.T. geprüft (keine Sleeps, keine Systemzeit, keine Reihenfolgeabhängigkeit, frische DB je Test). Kein Log enthält Zugangsdaten. Nachweis: Zuordnungstabelle im PR-Text.

## 7. Pull Request

- [x] 7.1 Pull Request vom Integrationszweig `feat/jumprun-persistent-scores` auf `main` unter dem Titel `feat(jumprun): persist run scores per mode and show a sidebar with the top 3` öffnen (Titel und Beschreibung Englisch), mit Smoke-Test-Checkliste, Szenario-Zuordnung und dem Hinweis auf die bewusste Abweichung vom Stats-Dienst-Plan. Nachweis: PR #350 (gemergt, `ffd6f66`).
