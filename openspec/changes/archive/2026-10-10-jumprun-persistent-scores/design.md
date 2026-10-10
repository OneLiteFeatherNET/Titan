# Design

## Context

Motivation steht in `proposal.md` (Why). Ausgangslage im Code, Stand `main` nach #347:

- `RunRecords` (`best(UUID, Mode)`, `submit(UUID, Mode, int)`, `forget(UUID)`) ist schon die Naht für dauerhafte Rekorde. `InMemoryRunRecords` ist `@Secondary`, eine zweite Bean ersetzt sie also ohne Umbau.
- `JumprunModule.begin` liest den Rekord **synchron beim Laufstart** (`records.best(...)` → `Run.previousBest`). `end` ruft `records.submit(...)` auf dem Thread des auslösenden Events. Das ist meist der Tick-Thread (Move, Death), beim Disconnect und Shutdown ein anderer.
- `onDisconnect` ruft `records.forget(...)` auf.
- Ein Run hält seine Anzeigen (`ScoreLabel`, `Spectators`) unter seinem eigenen Lock. `onMove` aktualisiert die Action Bar bei jeder Score-Änderung.
- Titan hat keine DB-Abhängigkeit. Die Konfiguration läuft über avaje-config (`titan/defaults/*.yaml` je Column, Profile, Overrides). Der Shadow-Jar führt Service-Dateien schon zusammen (`mergeServiceFiles()` in `titan.app-variant`).
- Minestom 26.1: `AsyncPlayerConfigurationEvent` läuft außerhalb des Tick-Threads und darf blockieren (`SpawnModule` nutzt es). Der Minestom-`Scheduler` läuft auf dem `TickSchedulerThread`, dort darf nichts blockieren. `net.minestom.server.scoreboard.Sidebar` bringt `ScoreboardLine` mit `NumberFormat` mit.

## Goals / Non-Goals

**Goals:**
- Hibernate genau einmal pro Prozess: ein Pool und eine `SessionFactory` für alle Columns, die später eine Datenbank brauchen.
- Kein Datenbankzugriff auf dem Tick-Thread oder dem `TickSchedulerThread`.
- Ohne `titan.database.url` ist das Verhalten byte-gleich zu heute, und keine Datenbankklasse wird geladen.
- Die Tests der Testpyramide brauchen außer der kleinen Integrationssuite weder Docker noch Netzwerk.

**Non-Goals:**
- Anti-Cheat oder Plausibilitätsprüfung von Scores. Die Kollision prüft weiter der Client, siehe Risiko im archivierten `lobby-jumprun`-Design.
- Bestenlisten außerhalb der Sidebar (Hologramm, Befehl, Web), Freundes-Bestenlisten, Zeiträume (Woche/Monat).
- Bereinigung oder Aufbewahrungsfristen der Historie. Bei Lobby-Volumen unkritisch, das kommt mit dem Stats-Dienst.
- Metriken und Spans. Titan exportiert noch keine Telemetrie. JDBC-Instrumentierung kommt mit einem eigenen Observability-Change.
- Namensauflösung über Otis.

## Decisions

### D1 Neues Top-Level-Modul `persistence` mit einer Avaje-`@Factory`

```
 persistence (neu)                               features/jumprun
 +-----------------------------------+           +----------------------------------+
 | DatabaseFactory  @Factory          |           | JumprunPersistence  @Factory      |
 |  @RequiresProperty                 |           |  @Bean PersistenceUnit            |
 |   ("titan.database.url")           |  <------- |   ("jumprun",                     |
 |  @Bean HikariDataSource            | List<     |    List.of(JumprunRunEntity))     |
 |  @Bean SessionFactory              |  Persist- |                                   |
 |   - Flyway je Unit (vorher)        |  enceUnit>| HibernateRunStore  @RequiresBean  |
 |   - managedClasses aller Units     |           |  (SessionFactory)                 |
 |   - hbm2ddl = validate             |           +----------------------------------+
 | record PersistenceUnit(name,       |
 |                        entities)   |
 +-----------------------------------+
```

`persistence` liegt neben `core`, `features/jumprun` hängt per `implementation(project(":persistence"))` daran. Die Factory sammelt alle `PersistenceUnit`-Beans (`record PersistenceUnit(String name, List<Class<?>> entities)`; Migrationen liegen unter `db/migration/<name>/`) der Columns (Avaje-Listeninjektion). Daraus baut sie Hikari, dann Flyway pro Unit in Bean-Reihenfolge, dann die `SessionFactory` über `HibernatePersistenceConfiguration` (Hibernate 7, JPA 3.2) mit `managedClasses(...)` und dem Hikari-Pool als `jakarta.persistence.nonJtaDataSource`. `@PreDestroy` schließt erst die `SessionFactory`, dann den Pool. Ohne URL entsteht keine dieser Beans, und keine Column lädt eine Hibernate-Klasse.

- **Built-in geprüft:** Avaje `@RequiresProperty`/`@RequiresBean` statt eines eigenen Schalters. Hibernate-Scanning (`hibernate.archive.autodetection`) wurde verworfen, weil es im Shaded-Jar (ein Jar für alle Columns) unzuverlässig ist und Abhängigkeiten von `persistence` zu den Columns umdrehen würde. `hibernate-hikaricp` wurde verworfen, weil Flyway denselben `DataSource` vor Hibernate braucht. Ein eigener Pool, den Hibernate selbst baut, wäre ein zweiter.
- **Warum Modul statt Column-eigener SessionFactory:** Ein zweites Feature mit Datenbank (Freunde-Teaser, Stats) soll keinen zweiten Pool öffnen. `PersistenceUnit` ist die Naht, an die es andockt, ohne `persistence` zu ändern.
- **Test:** Integration (`integrationTest`, Testcontainers PostgreSQL): Factory mit einer Test-Unit baut eine `SessionFactory`, `validate` läuft durch. Ein Unit-Test der Avaje-Verdrahtung ohne URL prüft, dass keine `SessionFactory`-Bean existiert und `InMemoryRunRecords` aktiv ist.
- **SOLID:** OCP (Columns docken über `PersistenceUnit` an), DIP (Columns hängen an `SessionFactory`, nicht an Hikari oder Postgres), SRP (Factory baut nur Infrastruktur).
- **Log:** Hibernate, Flyway und Hikari stehen in `common/src/main/resources/logback.xml` auf WARN, weil Flyway sonst die JDBC-URL auf INFO loggt. INFO einmal nach dem Start: `Database ready` mit Key-Values `units` (Anzahl) und `migrations` (angewendete Migrationen). Weder URL noch Benutzer noch Passwort, weil eine JDBC-URL Zugangsdaten enthalten kann.

### D2 Schema über Flyway, Hibernate nur `validate`

Jede Unit bringt versionierte SQL-Migrationen unter `db/migration/<column>/` mit, z. B. `V1__create_jumprun_run.sql`, mit eigener Flyway-History-Tabelle pro Unit (`flyway_<column>_history`). So stören sich die Versionsnummern verschiedener Columns nicht. Jede Unit läuft mit `baselineOnMigrate(true)` und `baselineVersion("0")`: Ab der zweiten Unit ist das Schema nicht mehr leer, und Flyway würde ohne Baseline abbrechen, mit der Standard-Baseline 1 aber `V1` überspringen. Deshalb darf keine Unit eine `V0`-Migration mitbringen. Flyway sperrt über die Datenbank, gleichzeitig startende Lobbys wenden jede Migration also genau einmal an (Spec `lobby-persistence`). Danach prüft Hibernate mit `hbm2ddl.auto=validate`, und eine Abweichung bricht den Start ab.

- **Built-in geprüft:** `hbm2ddl=update` (wie PandorasCluster) wurde verworfen, weil es weder Spaltenlöschungen noch Umbenennungen noch Daten-Migrationen kennt und keinen Sperrmechanismus für parallele Starts hat. Liquibase war zuerst gewählt, aber Liquibase Core steht seit 5.0 unter der FSL-1.1-ALv2 statt Apache 2.0 (im POM von 5.0.4 geprüft). 4.33 ist die letzte Apache-Version ohne weitere Updates. Flyway Community (`flyway-core` und `flyway-database-postgresql`, Apache 2.0) deckt den Bedarf ab und ist kleiner. Die SQL-Migrationen sind nicht datenbankneutral, aber das Ziel ist ohnehin PostgreSQL, und ein Stats-Dienst kann die Tabelle samt SQL übernehmen.
- **Test:** Integration: leere DB → Tabelle und Indizes da, zweiter Lauf wendet nichts an. Zwei Migrationen parallel (zwei Threads, eine DB) → beide fertig, genau ein Eintrag pro Version in der History-Tabelle.
- **SOLID:** SRP (Schema gehört der Migration, nicht dem Mapping).

### D3 Eine append-only-Tabelle `jumprun_run`

```
jumprun_run
  id           bigint generated always as identity  PK
  player_uuid  uuid         not null
  player_name  varchar(32)  not null      -- Bedrock-/Floodgate-Namen tragen ein Präfix
  mode         varchar(16)  not null      -- Mode.name(), nie Ordinal
  score        integer      not null  check (score >= 0)
  end_reason   varchar(16)  not null      -- EndReason.name()
  finished_at  timestamptz  not null
  index jumprun_run_player_mode  (player_uuid, mode, score desc)
  index jumprun_run_mode_player_best (mode, player_uuid, score desc, finished_at)  -- dient DISTINCT ON der Top 3
```

Die Entity `JumprunRunEntity` (package-private in der Column, `@Immutable`) mappt `mode` und `end_reason` per `@Enumerated(EnumType.STRING)` und `player_uuid` als natives `uuid` (Hibernate 7 auf PostgreSQL). Hibernate schreibt nur per `persist`. Ein Lauf wird nie geändert oder gelöscht.

- **Alternative:** zusätzlich eine `jumprun_best` mit Upsert (`GREATEST`). Verworfen, weil zwei Tabellen synchron zu halten sind. Bei Lobby-Volumen (geschätzt < 1 Mio. Zeilen pro Jahr) reichen die beiden Indizes für die Aggregationen. Wird es eng, kommt als erster Schritt eine Materialized View, ohne Schemawechsel für Schreiber.
- **Warum Strings statt Ordinal:** `Mode.next()` hängt an der Reihenfolge der Konstanten. Ein Umsortieren würde gespeicherte Ordinale still verschieben.
- **Test:** Integration (Teil von D4).
- **SOLID:** n/a (Datenmodell).

### D4 `RunStore` mit den drei Datenbankzugriffen

`RunStore` ist ein Interface in der Column mit genau den Zugriffen, die die Lobby braucht. `HibernateRunStore` hängt an derselben Property wie `DatabaseFactory` (gemeinsame Konstante) und injiziert `@External Provider<SessionFactory>`. `@RequiresBean(SessionFactory.class)` geht nicht: `persistence` braucht die `PersistenceUnit` der Column, und eine Column, die umgekehrt die `SessionFactory` verlangt, ergibt einen Ordnungszyklus der Avaje-Module, bei dem die Store-Bean nie entsteht. Ein Wiring-Integrationstest mit beiden echten Modulen belegt die Lösung. Lesezugriffe laufen in einer Read-only-Session, Ergebnisse werden typisiert (HQL-Konstruktor-Ausdruck, Tuple) statt über `Object[]` gelesen:

| Methode | Umsetzung |
|---|---|
| `Map<Mode, Integer> bestsOf(UUID)` | HQL `select r.mode, max(r.score) from JumprunRunEntity r where r.playerUuid = :p group by r.mode` |
| `Map<Mode, TopThree> topThreeOfEveryMode()` (Modi ohne Läufe fehlen) | eine native Abfrage mit `DISTINCT ON (mode, player_uuid)` (bester Lauf je Spieler, früherer gewinnt) und `row_number() over (partition by mode order by score desc, finished_at)`, gefiltert auf `<= 3`. Der Name kommt aus dem jüngsten Lauf des Spielers (Lateral-Subselect). |
| `void append(FinishedRun)` | `sessionFactory.inTransaction(s -> s.persist(entity))` |

`FinishedRun` ist ein Record (`player`, `name`, `mode`, `score`, `endReason`, `finishedAt`). `TopEntry` ist ein Record (`player`, `name`, `score`, `achievedAt`).

- **Built-in geprüft:** Hibernate `inTransaction`/`fromTransaction` statt eigenem Transaktions-Handling (anders als PandorasCluster). Native SQL nur für die Top 3, weil `DISTINCT ON` und Lateral in HQL nicht ausdrückbar sind.
- **Test:** Integration (Testcontainers): `bestsOf` liefert Maximum pro Modus und ignoriert andere Spieler. Die Top 3 nehmen jeden Spieler einmal, bei Gleichstand gewinnt der frühere, der Name kommt vom jüngsten Lauf. `append` mit Score 0 erscheint. Das deckt die Spec-Szenarien „Top 3 des Modus“ und „Jeder gewertete Lauf wird gespeichert“ auf DB-Ebene ab. Alle übrigen Klassen testen gegen einen `FakeRunStore` (In-Memory-Liste, threadsicher) in Unit-Tests. Ein gemeinsamer `RunStoreContract` in den Test-Fixtures läuft gegen beide Implementierungen, damit der Fake nicht abweicht.
- **SOLID:** DIP (Rekorde und Bestenliste hängen am Interface, nicht an Hibernate), ISP (drei Methoden, nur was gebraucht wird). Das Interface hat eine Produktiv-Implementierung, ist aber die Grenze, an der Unit-Tests ohne Docker auskommen.

### D5 `RunRecords` speichert über einen Cache, Laden beim Betreten

`RunRecords` wird erweitert:

```
OptionalInt best(UUID, Mode)                  // unverändert, immer aus dem Speicher
boolean submit(FinishedRun run)               // statt (UUID, Mode, int)
void load(UUID player)                        // darf blockieren; InMemory: no-op
void forget(UUID player)                      // unverändert
```

`StoredRunRecords` (`@RequiresProperty(DatabaseProperties.URL)` wie `HibernateRunStore`, siehe D4; verdrängt das `@Secondary` `InMemoryRunRecords`) **komponiert** ein `InMemoryRunRecords` als Cache, statt die Map-Logik zu kopieren:

```
 AsyncPlayerConfigurationEvent ---> load(uuid): store.bestsOf(uuid) -> cache füllen   (blockiert, kein Tick-Thread)
 Laufende (beliebiger Thread) ---> submit(run): cache.submit(...) -> Rekord? sofort
                                                \-> executor.submit(store.append(run))
 PlayerDisconnectEvent        ---> forget(uuid): cache.forget(uuid)
```

`JumprunModule` registriert `AsyncPlayerConfigurationEvent` an seinem `FeatureNode` und ruft `records.load(...)` auf. `end` baut den `FinishedRun` mit `player.getUsername()` und `clock.instant().truncatedTo(ChronoUnit.MICROS)`. `timestamptz` speichert Mikrosekunden, und so ist ein Gleichstand im Speicher auch nach dem Weg durch die Datenbank einer. Die `Clock` wird injiziert. Geschrieben wird über den `DatabaseWriter` aus `persistence`. Das ist ein `Executor` aus virtuellen Threads, den `DatabaseFactory` mit der `SessionFactory` als Parameter erzeugt. Avaje zerstört ihn deshalb vor ihr, und sein `close()` wartet ausstehende Schreibvorgänge ab, solange die `SessionFactory` noch offen ist. Besäße `StoredRunRecords` den Executor selbst, entstünde es wegen des `Provider` vor der `SessionFactory` und würde erst nach ihr zerstört (D4): Beim Shutdown beendete Läufe gingen still verloren. `StoredRunRecords` holt den Writer per `@External Provider<DatabaseWriter>`. Wird ein Schreibauftrag nach dem Shutdown abgelehnt, gibt es ein WARN, und der Cache behält den Wert. Die `Clock` kommt aus der Plattform (`PlatformBeans`), nicht aus der Column.

- **Fehler:** `load` schlägt fehl → WARN `Could not load jump and run records` mit `player`-UUID und der Exception. Der Cache bleibt für diesen Spieler leer, und sein erster Lauf kann fälschlich als Rekord gelten (siehe Risiken). `append` schlägt fehl → WARN `Could not store jump and run result` mit `player`, `mode` und der Exception. Der Cache behält den Wert bis zum Disconnect (Spec „Speichern schlägt fehl“). Jede Exception wird genau einmal dort geloggt.
- **Built-in geprüft:** `AsyncPlayerConfigurationEvent` ist Minestoms vorgesehener Ort für blockierendes Laden vor dem Spawn, deshalb kein eigener Lade-Thread und kein Warten im Spiel. Virtuelle Threads (`Executors.newVirtualThreadPerTaskExecutor()`) statt eines eigenen Pools für blockierendes JDBC. `CompletableFuture` als Rückgabe wurde verworfen, weil kein Aufrufer auf das Schreiben wartet.
- **Test:** Unit (`StoredRunRecordsTest`, `FakeRunStore`, ein synchroner Executor als Konstruktorparameter): `load` füllt den Rekord aus dem Store. `submit` meldet einen Rekord gegen den geladenen Wert und hängt den Lauf an. Ein Store-Fehler beim Anhängen lässt den Rekord im Cache, das WARN wird über einen `ListAppender` geprüft. `forget` leert. Integration (Cyano-`Env`): Ein Spieler mit gespeichertem Hard-Rekord 15 tritt bei, ein Lauf mit 15 ist kein Rekord (Spec „Rekord überdauert das Verlassen“, mit `FakeRunStore`-Bean).
- **SOLID:** OCP/LSP (neue Implementierung an der vorhandenen Naht, `InMemoryRunRecords` bleibt unverändert nutzbar), SRP (Cache-Logik nur einmal, in `InMemoryRunRecords`). `submit(FinishedRun)` ändert die Signatur der Naht. Das ist bewusst, denn Name und Endgrund gehören zum Lauf, und es gibt außer der Column keinen Aufrufer.
- **Log:** nur die beiden WARN. Pro Lauf nichts über DEBUG hinaus.

### D6 `Leaderboard`: Top 3 je Modus im Speicher, alle 30 s aufgefrischt

`Leaderboard` (`@Singleton @RequiresProperty(DatabaseProperties.URL)`, siehe D4) hält eine unveränderliche `Map<Mode, TopThree>` in einer `AtomicReference`. `TopThree` ist ein Record mit reiner Logik: `with(TopEntry)` sortiert einen neuen Wert ein (ein Spieler kommt nur einmal vor, höherer Score vorn, bei Gleichstand der frühere, danach die UUID vorzeichenlos wie in PostgreSQL), `entries()` liefert höchstens 3.

- `refresh()` liest `store.topThreeOfEveryMode()` (eine Abfrage für alle Modi) und ersetzt die Map. Lokal eingetragene Werte, die die DB noch nicht hat, überleben das per `with(...)`.
- `offer(FinishedRun)` wird bei einem neuen persönlichen Rekord sofort aufgerufen, auch mitten im Lauf über den Score-Wechsel in `onMove`, damit „Neuer Rekord rückt sofort auf“ gilt.
- `JumprunModule` plant in `@PostConstruct` eine Minestom-`Task` mit `repeat(TaskSchedule.seconds(30))`, die nur `executor.submit(leaderboard::refresh)` aufruft, nie selbst abfragt (TickSchedulerThread-Falle). Einen ersten `refresh()` gibt es beim Start. `JumprunModule` bekommt `Optional<Leaderboard>`. Ohne DB ist es leer, die Sidebar zeigt dann keine Top 3.

- **Built-in geprüft:** Minestom-`SchedulerManager` als Taktgeber statt eines eigenen `ScheduledExecutorService`, damit der Lebenszyklus am Server hängt. Ein Cache-Framework (Caffeine) wurde verworfen, weil es um fünf Listen mit je drei Einträgen geht, ohne Verdrängung oder Ablauf pro Schlüssel.
- **Test:** Unit (`TopThreeTest`): Einsortieren, Duplikat desselben Spielers hebt nur an, Gleichstand nach Zeit, höchstens 3. Unit (`LeaderboardTest`, `FakeRunStore`): `refresh` übernimmt den Store, ein lokal angebotener Wert überlebt `refresh`, ein Store-Fehler behält die alte Map und loggt WARN.
- **SOLID:** SRP (Bestenliste getrennt von Rekorden), DIP (`RunStore`).
- **Log:** WARN `Could not refresh jump and run leaderboard` mit der Exception, höchstens einmal pro 30 s.

### D7 `RunSidebar` pro Lauf, neben `ScoreLabel`

`RunSidebar` lebt wie `ScoreLabel` im `Run` und wird unter dessen Lock benutzt. Sie hält eine Minestom-`Sidebar` mit genau einem Viewer, dem Läufer. `RunSidebarContent` liefert pro Zeile ein `SidebarLine(text, Optional<value>)`. Der Wert steht in Minecrafts rechter Zahlenspalte (`NumberFormat.fixed(Component)`), Zeilen ohne Wert nutzen `NumberFormat.blank()`.

```
 Titel:   <slime-Sprite> <Spielname-Markup> · <mode.label()>   (sprachneutral, wie ScoreLabel)
 Score                     <score>          Wert grün
 Rekord                    <best|–>         Wert gold, "–" dunkelgrau
 (leer)                                     nur mit mind. einem Top-Eintrag
 Top 3                                      Überschrift im Verlauf, übersetzt
 <»?><Kopf> <Name>         <score>          Wert gold/silber/bronze; eigene Zeile: gelbes », Name fett gelb
```

Nach dem lokalen Test so gestaltet: Graue Labels waren auf dem halbtransparenten Hintergrund schlecht lesbar, Werte direkt am Text unruhig. Köpfe sind Adventure-Objekt-Komponenten (`ObjectContents.playerHead(uuid)`), Namen werden als reiner Text eingesetzt. Alle Farben stehen als MiniMessage-Vorlagen in den Sprachdateien. Zeilen-IDs und Scores sind pro Position fest, damit die Reihenfolge stabil bleibt. Geänderte Werte gehen per `updateLineNumberFormat`, geänderte Texte per `updateLineContent`.

- `show(...)` vergleicht mit dem zuletzt Gezeigten und schickt nur geänderte Texte oder Werte. Sie wird überall dort aufgerufen, wo heute die Action Bar aktualisiert wird, und zusätzlich nach `refresh()` für alle laufenden Runs (aus dem Executor-Thread, unter dem Run-Lock; das Senden von Paketen ist in Minestom threadsicher).
- Der Rekord in der Sidebar ist `max(gespeicherter Rekord, aktueller Score)`, sobald der Score den bisherigen übertrifft. So passt er zu „Neuer Rekord rückt sofort auf“.
- `end` entfernt den Viewer (`removeViewer`) in jedem `EndReason`-Zweig, wo heute `ScoreLabel` abgeräumt wird.
- Die Beschriftungen werden pro Spieler-Locale über die vorhandenen `RunMessages` (D8 des archivierten Designs) gerendert, also `GlobalTranslator.render`, nicht als String.
- **Built-in geprüft:** Minestom-`Sidebar` übernimmt Teams, Zeilen-IDs und Pakete. Eine eigene Paketlösung wurde verworfen. Eine geteilte Sidebar pro Modus wurde verworfen, weil die fette eigene Zeile, Score und Locale pro Spieler verschieden sind.
- **Test:** Unit (`RunSidebarContentTest`): Die reine Funktion „Score, Rekord, Top, eigene UUID, Locale → Zeilen-Components“ macht nur die eigene Zeile fett, gibt `–` ohne Rekord aus, lässt die Top 3 ohne Leaderboard weg und ändert keine Zeile bei unverändertem Score. Integration (Cyano-`Env`, `env.tick()`): Laufstart → Läufer bekommt Display-Objective-Paket mit Titel des Modus, ein Zuschauer nicht. Score 7 → Zeile aktualisiert. Laufende → Objective entfernt. Das deckt die Spec-Szenarien „Sidebar beim Start“, „Score steigt“, „Sidebar verschwindet“ und „Nicht für andere“ ab.
- **SOLID:** SRP (Darstellung getrennt von Run-Logik, wie `ScoreLabel`).

### D8 Texte

Neue Schlüssel in `messages_en.properties`/`messages_de.properties` unter `titan.jumprun.sidebar.*` (Labels, Wertevorlagen, `record.none`, `top.header`, `top.entry`, `self.marker`, `place.1..3.value`), im MiniMessage-Format. Namen und Zahlen kommen als Argumente, nie per String-Verkettung. Der vorhandene `RunMessagesBundleTest` deckt die Vollständigkeit beider Bundles ab.

### D9 Konfiguration

**Verbindung:** `titan.database.url`, `.user` und `.password` bleiben eigene Schlüssel. `url` ist der Schalter (`@RequiresProperty(DatabaseProperties.URL)`). Die drei werden in `HikariConfig` als `jdbcUrl`, `username` und `password` gesetzt und gewinnen gegen gleichnamige Einträge unter `hikari`.

Ergebnis (Tasks 1.1 und 1.3): Jeder Schlüssel lässt sich per Umgebungsvariable setzen (Großbuchstaben, `.` → `_`, also `TITAN_DATABASE_URL`, `TITAN_DATABASE_USER`, `TITAN_DATABASE_PASSWORD`; Rang: Datei < Profil < `CONFIG_FILE` < Umgebung < System-Property). Das gilt auch für Schlüssel, die in keiner Datei stehen, sofern sie einzeln gelesen werden (`Config.getNullable`), also für `url`, `user` und `password`. Avaje Inject liest `@RequiresProperty` über avaje-config (`DConfigProps` → `Config.getNullable`). `notEqualTo = ""` wirkt nicht, weil `""` der Default des Attributs ist. Eine gesetzte, aber leere URL bricht den Start deshalb mit der Meldung `titan.database.url is set but empty; unset it to run without a database` ab.

**Pool und ORM durchgereicht:**

```yaml
titan:
  database:
    hikari:            # jede HikariCP-Eigenschaft, Name wie in HikariConfig
      maximumPoolSize: 4
      connectionTimeout: 5000
      initializationFailTimeout: 5000
      poolName: titan
      # dataSource.reWriteBatchedInserts: true   -> an den Treiber
    hibernate:         # jede Hibernate-Eigenschaft ohne Präfix "hibernate."
      # jdbc.batch_size: 20
      # generate_statistics: false
```

`DatabaseSettings` liest beide Abschnitte über avaje-configs `forPath("titan.database.hikari").asProperties()` bzw. `forPath("titan.database.hibernate")`. `forPath` sieht nur Schlüssel, die in einer YAML-Datei (Defaults, `application.yaml`, Profil, `CONFIG_FILE`) stehen. `-D` und Umgebungsvariablen überschreiben dort nur vorhandene Schlüssel, ein neuer Schlüssel per `-D` wird ignoriert (Smoke-Test 6.2). Bewusst so belassen: Pool und ORM werden in der YAML eingestellt. Ein eigener Scan von System-Properties und Umgebung wurde als Mehrcode ohne Bedarf verworfen. Die gesperrten Werte lassen sich auf diesem Weg nicht einschleusen, weil ignorierte Schlüssel gar nicht ankommen. `HikariSettings` wendet jeden Schlüssel selbst über den gleichnamigen Bean-Setter von `HikariConfig` an (Umwandlung für int, long, boolean, String; `dataSource.*` → `addDataSourceProperty`) und sammelt unbekannte oder ungültige Schlüssel. Eine eigene Schlüsselliste gibt es nicht. `new HikariConfig(Properties)` wurde verworfen, weil Hikaris `PropertyElf` einen ungültigen Wert vor dem Abbruch selbst auf ERROR loggt, z. B. die `NumberFormatException` mit dem Wert; dort könnte ein Passwort landen. Hibernate bekommt jede Eigenschaft mit vorangestelltem `hibernate.` über `HibernatePersistenceConfiguration.property(...)`. Die Defaults stehen nur in `titan/defaults/database.yaml`, die bisherigen `titan.database.pool.*` entfallen (noch nicht veröffentlicht).

**Gesperrt** (Vergleich nach `trim().toLowerCase(Locale.ROOT)`): `hibernate.hbm2ddl.auto` muss fehlen, `validate` oder `none` sein, die Lobby setzt danach ohnehin `validate`. Das JPA-Pendant `jakarta.persistence.schema-generation.database.action` muss fehlen oder `validate` sein, weil es vor `hbm2ddl.auto` gilt und `none` die Prüfung still abschalten würde. Beides, weil Flyway das Schema besitzt (D2). Ein anderer Wert bricht den Start ab und nennt die Einstellung. Die Lobby setzt `validate` selbst. Schlüssel, die am Pool vorbei eine eigene Verbindung öffnen würden (`hibernate.connection.url`, `.username`, `.password`, `.driver_class`, `.provider_class`, `.datasource`, `jakarta.persistence.jdbc.*`, `jakarta.persistence.jtaDataSource`/`nonJtaDataSource` sowie Hikaris `dataSourceClassName`, `dataSourceJNDI`, `dataSource`), werden ebenso abgelehnt, weil die Verbindung immer aus Hikari kommt. Abstimmungsschlüssel wie `hibernate.connection.handling_mode` oder `.provider_disables_autocommit` bleiben erlaubt.

- **Built-in geprüft:** avaje-config `forPath`/`asProperties` statt eines eigenen Binders. `HikariConfig(Properties)` (validiert über Setter) und Hibernates Property-Mechanismus statt eigener Records pro Einstellung. Ein eigener Record mit ausgewählten Feldern wurde verworfen, weil jede neue Einstellung eine Codeänderung bräuchte.
- **Test:** Unit: `maximumPoolSize: 10` landet in der `HikariConfig`. Ein unbekannter Hikari-Schlüssel bricht mit dem Schlüssel in der Meldung ab. `hibernate.jdbc.batch_size: 20` landet in den Hibernate-Properties. `hbm2ddl.auto: update` bricht ab, `validate` und fehlend sind erlaubt. `hibernate.connection.url` wird abgelehnt, `hibernate.connection.handling_mode` nicht. URL, Benutzer und Passwort gewinnen gegen `hikari.jdbcUrl`. Integration (Testcontainers): Mit `maximumPoolSize: 10` meldet der laufende Pool 10 (`HikariPoolMXBean`/`getMaximumPoolSize`), eine Hibernate-Einstellung ist in `SessionFactory.getProperties()` sichtbar.
- **SOLID:** OCP (neue Einstellungen ohne Codeänderung), SRP (`DatabaseSettings` liest, die Factory baut).
- **Log:** keins zusätzlich. Die Fehlermeldungen enthalten Schlüssel, nie Werte, damit kein Passwort aus `dataSource.password` o. ä. ins Log gerät.

### D10 Build und Tests

- **Versionskatalog** (Maven Central, Stand 03.10.2026): `org.hibernate.orm:hibernate-core` 7.4.11.Final (8.0 ist erst Beta), `com.zaxxer:HikariCP` 7.1.0, `org.postgresql:postgresql` 42.7.13, `org.flywaydb:flyway-core` und `flyway-database-postgresql` 13.9.0, `org.testcontainers:testcontainers-postgresql` und `testcontainers-junit-jupiter` 2.0.5 (seit 2.x mit Präfix `testcontainers-`). Hibernate 7: `new HibernatePersistenceConfiguration(name).managedClasses(...).property(JpaSettings/`"jakarta.persistence.nonJtaDataSource"`, dataSource).createEntityManagerFactory()`; die `SessionFactory` erhält man über `buildSessionFactory()` bzw. `unwrap(SessionFactory.class)`.
- **Integrationstests:** eigene Gradle-JVM-Test-Suite `integrationTest` (Gradle `jvm-test-suite`, built-in) in `persistence` und `features/jumprun`. `check` hängt daran, `test` bleibt ohne Docker und Netzwerk (F.I.R.S.T. Fast). CI auf GitHub Actions hat Docker. Jede Integrationsklasse bekommt eine frische Datenbank (eigenes Schema pro Test oder `@Container` pro Klasse mit Truncate im `@BeforeEach`), damit die Reihenfolge egal ist.
- **Defaults eines Nicht-Column-Moduls:** `titan.app-variant` mischt zusätzlich `titan/defaults/*.yaml` aus `:persistence` in die `application.yaml` der Varianten, weil der `features/*`-Scan das Modul nicht erfasst. Die gemeinsame Test-Suite liegt als Convention `titan.integration-test` in `buildSrc`.
- **Shadow:** `mergeServiceFiles()` ist schon gesetzt, das deckt Hibernates und Flyways `META-INF/services` (Flyway findet seine Datenbank-Plugins darüber) ab. Der Smoke-Test bestätigt das am Shaded-Jar.

## Risks / Trade-offs

- [Rekord-Laden schlägt beim Betreten fehl → der erste Lauf meldet fälschlich „neuer Rekord“] → Nur bei DB-Ausfall genau beim Join. Es wird als WARN geloggt, und die Historie bleibt korrekt, weil nur die Meldung falsch ist. Eine Markierung „unbekannt“ mit unterdrückter Meldung wäre mehr Zustand für einen seltenen Fall.
- [Join wird um eine Abfrage langsamer] → Indexzugriff über `(player_uuid, mode, score desc)`. Hikari-Timeouts begrenzen das Warten. Ein Fehler lässt den Join zu (s. o.).
- [Lobby-Wechsel kurz nach Laufende: die neue Lobby lädt, bevor der Write-behind geschrieben hat] → Der Rekord wirkt dort kurz veraltet. Das korrigiert sich beim nächsten Join, die Historie ist vollständig.
- [Top 3 aus anderen Lobbys bis zu 30 s alt] → Von der Spec gedeckt (höchstens eine Minute).
- [Shaded-Jar wächst um etwa 15–20 MB, AOT-Cache ohne DB-Pfad trainiert] → Das AOT-Training beim Deployment läuft mit DB-Konfiguration und einem Lauf (Doku-Task). Der Start ohne Cache bleibt korrekt, nur langsamer.
- [Hibernate/JBoss-Logging bringt eigene Logger mit] → Er läuft über SLF4J und das vorhandene Logback. Die Hibernate-Kategorien stehen auf WARN, damit SQL nicht im Log landet.
- [Konfiguriert, aber Datenbank beim Start weg → Lobby startet nicht] → Bewusst: Fail-fast macht eine Fehlkonfiguration sichtbar, CloudNet startet den Dienst neu. Ein stiller Fallback auf den Speicher würde Daten ohne Hinweis verlieren.
- [Abweichung vom Retention-Plan (Stats als eigener Dienst)] → Die Tabelle ist append-only mit sprachneutralen Strings und lässt sich 1:1 übernehmen. Ein späterer Change ersetzt `HibernateRunStore` durch einen HTTP-Client am selben `RunStore`.
- [Ein mitten im Lauf angebotener Wert bleibt in der Top 3, obwohl der Lauf nie gespeichert wird (Ende durch Shutdown oder fehlgeschlagener Schreibvorgang)] → `refresh()` behält lokale Einträge, damit „Neuer Rekord rückt sofort auf“ gilt. Der Geist lebt nur im Speicher dieser Lobby bis zum Neustart, beim Shutdown also praktisch gar nicht. Ein `retract` wurde als Mehrcode für einen Fall verworfen, der mit der JVM endet.
- [Spieler mit wechselndem Namen steht unter altem Namen in einer zwischengespeicherten Top 3] → Höchstens 30 s, danach kommt der Name vom jüngsten Lauf.

## Migration Plan

1. Deploy ohne `titan.database.url`: Das Verhalten ist unverändert, die neue Sidebar zeigt Score und Rekord aus dem Speicher.
2. PostgreSQL-Datenbank und Benutzer anlegen (Rechte: Schema-DDL für Flyway), Zugangsdaten im CloudNet-Task als Umgebung setzen.
3. Lobby neu starten: Flyway legt `jumprun_run` an, und ab dann wird gespeichert. Bisherige Rekorde aus dem Speicher werden nicht migriert, sie waren per Spec flüchtig.
4. Rollback: URL entfernen und neu starten, dann läuft wieder alles im Speicher. Die Tabelle bleibt unberührt und wird bei erneuter Aktivierung weiter genutzt.

## Open Questions

- Wie heißt die Produktionsdatenbank und wo läuft sie (vorhandenes Postgres aus `feathre-core` oder eigenes)? Das betrifft nur Deployment und Doku, nicht Code oder Specs.
