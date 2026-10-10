# Proposal

## Why

Das Jump and Run hält Rekorde nur, solange der Spieler in der Lobby ist. Mit dem Verlassen, einem Lobby-Wechsel oder einem Neustart ist alles weg. Ohne dauerhaften Rekord und ohne Vergleich mit anderen fehlt der Grund, wiederzukommen. Bestenlisten sind aber der Hebel, den der Retention-Plan für die Lobby vorsieht.

## What Changes

- Jeder Lauf, dessen Ende den Score wertet (`EndReason.submitsScore()`), wird mit Spieler, Name, Modus, Score, Endgrund und Zeitpunkt in PostgreSQL gespeichert. Das gilt auch für Läufe mit Score 0.
- Der Rekord pro Spieler und Modus überdauert Verlassen, Lobby-Wechsel und Neustart. Er gilt über alle Lobby-Instanzen hinweg.
- Neue Sidebar rechts, nur während eines Laufs und nur für dessen Modus. Sie zeigt Titel mit Modus, den aktuellen Score, den eigenen Rekord des Modus und die Top 3 des Modus mit Spielernamen. Ist der Läufer selbst in der Top 3, steht seine Zeile fett. Nach dem Lauf verschwindet die Sidebar.
- Die Action Bar mit dem Score bleibt unverändert.
- Neues gemeinsames Modul `persistence`. Es baut aus der Konfiguration eine Hibernate-`SessionFactory` über einen HikariCP-Pool. Flyway legt beim Start das Schema an und migriert es, Hibernate prüft es nur (`validate`).
- HikariCP und Hibernate sind vollständig über die YAML einstellbar: `titan.database.hikari.*` und `titan.database.hibernate.*` werden unverändert durchgereicht. Nur die Schema-Einstellung von Hibernate bleibt auf `validate` gesperrt, weil Flyway das Schema besitzt.
- Ohne Datenbank-Konfiguration verhält sich die Lobby wie bisher: Rekorde liegen im Speicher und enden mit dem Verlassen. Die Sidebar zeigt dann nur Score und Rekord, ohne Top 3.
- Datenbankzugriffe laufen nie auf dem Tick-Thread. Rekorde werden beim Betreten asynchron geladen, die Top 3 regelmäßig asynchron aufgefrischt, Läufe asynchron geschrieben.
- **Bewusste Abweichung vom Retention-Plan:** Dort waren Stats als eigener Dienst vorgesehen. Titan hält diese Daten jetzt selbst, als Zwischenschritt. Die Tabelle ist append-only und ohne Titan-interne Typen geschnitten, damit ein späterer Stats-Dienst sie übernehmen kann.

## Capabilities

### New Capabilities
- `lobby-persistence`: optionale Datenbankanbindung der Lobby. Sie wird über die Konfiguration aktiviert, Pool und ORM sind frei über die YAML einstellbar, das Schema wird beim Start migriert, ohne Konfiguration bleibt die Datenbank aus. Außerdem regelt sie, wie die Lobby bei nicht erreichbarer Datenbank startet.

### Modified Capabilities
- `lobby-jumprun`: Die Anforderung „Score und Rekord“ wird ersetzt. Rekorde sind dauerhaft und lobbyübergreifend, sobald eine Datenbank konfiguriert ist, und jeder gewertete Lauf landet in der Historie. Neu ist die Anforderung „Sidebar während des Laufs“ mit Score, Rekord und Top 3 des Modus.

## Impact

- **Code:** neues Gradle-Modul `persistence` (Top-Level wie `core`). `features/jumprun` hängt daran und bekommt eine zweite `RunRecords`-Implementierung, ein Repository für Läufe, einen Top-3-Cache und die Sidebar. `InMemoryRunRecords` bleibt der Fallback ohne Datenbank. Die App-Varianten übernehmen beides über die bestehenden Scans. Der Shadow-Jar muss die Service-Dateien von Hibernate zusammenführen.
- **Neue Laufzeit-Abhängigkeiten:** Hibernate ORM 7 (`hibernate-core`), HikariCP, PostgreSQL-JDBC-Treiber, Flyway (`flyway-core`, `flyway-database-postgresql`, Apache 2.0; Liquibase scheidet aus, weil es seit 5.0 unter der FSL steht). Zusammen etwa 15–20 MB mehr im Shaded-Jar.
- **Neue Test-Abhängigkeiten:** Testcontainers (PostgreSQL) für wenige Integrationstests des Repositorys.
- **Konfiguration:** neue Schlüssel `titan.database.url`, `titan.database.user` und `titan.database.password` (avaje-config, per Umgebungsvariable überschreibbar). Ohne `url` bleibt alles im Speicher. Dazu kommen die Abschnitte `titan.database.hikari.*` (alle HikariCP-Eigenschaften) und `titan.database.hibernate.*` (alle Hibernate-Eigenschaften ohne Präfix `hibernate.`).
- **Betrieb:** Die Produktion braucht eine PostgreSQL-Datenbank samt Zugangsdaten im CloudNet-Task. Das AOT-Training beim Deployment sollte einen Lauf mit Datenbank einschließen.
- **Nutzertexte (neu):** Sidebar-Zeilen „Score“ und „Rekord“, beide in `messages_en`/`messages_de` mit Englisch als Fallback. Titel und Modusname bleiben sprachneutral wie im Kopf-Label, Spielernamen und Zahlen werden nicht übersetzt.
- **Nutzertexte (geändert):** keine. Die Meldungen zum Laufende und zum neuen Rekord bleiben, sie gelten nun nur gegenüber dem dauerhaften Rekord.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(jumprun): persist run scores per mode and show a sidebar with the top 3`
