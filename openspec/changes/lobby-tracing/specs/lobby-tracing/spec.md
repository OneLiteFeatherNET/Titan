# Spec Delta

## ADDED Requirements

### Requirement: Ohne Agent läuft die Lobby unverändert
Die Lobby DARF NICHT von einem OpenTelemetry-Agent abhängen. Ohne Agent MÜSSEN alle Spans und Zähler No-ops sein, ohne Fehler beim Start und ohne verändertes Spielverhalten. Features DÜRFEN `GlobalOpenTelemetry` nicht selbst aufrufen, sondern MÜSSEN die `Telemetry`-Bean über den Konstruktor bekommen.

#### Scenario: Start ohne Agent
- **WHEN** die Lobby ohne `-javaagent` startet und Spieler beitreten und gehen
- **THEN** läuft alles wie bisher, ohne Ausnahme und ohne Ausgabe zu Spans

#### Scenario: Architekturregel
- **WHEN** der Build die Architekturregeln prüft
- **THEN** ruft keine Klasse außerhalb von `runtime` `GlobalOpenTelemetry` auf

### Requirement: Keine personenbezogenen Daten über die UUID hinaus
Spans DÜRFEN die UUID des Spielers als `user.id` tragen. Sie DÜRFEN NICHT seinen Namen, seine IP-Adresse oder seine Sprache tragen. Spannamen MÜSSEN feste Zeichenketten sein. Metriken DÜRFEN KEIN `user.id` tragen.

#### Scenario: Kein Spielername im Span
- **WHEN** ein Spieler mit dem Namen „Steve“ beitritt und geht
- **THEN** enthält kein Attributwert und kein Spanname „Steve“, und `user.id` ist die UUID

### Requirement: Kein Span für hochfrequente Events
Die Lobby DARF KEINEN Span je Tick und KEINEN Span für `PlayerMoveEvent`, Packet-, Chunk- oder Tick-Events erzeugen. `FeatureNode.onTraced` MUSS solche Event-Typen beim Registrieren ablehnen. Für häufige Vorgänge MÜSSEN Zähler, Histogramme oder Span-Events an einem bestehenden Span dienen.

#### Scenario: Registrierung abgelehnt
- **WHEN** ein Feature `onTraced(PlayerMoveEvent.class, …)` aufruft
- **THEN** wirft `FeatureNode` eine `IllegalArgumentException`, und kein Listener ist registriert

### Requirement: Geführte Listener erfassen Fehler
Der `ListenerGuard` MUSS jeden von einem Feature-Listener geworfenen Fehler zählen (`titan.listener.failures{titan.feature}`) und weiterhin wie bisher loggen und weiterwerfen. Ein mit `onTraced` registrierter Listener MUSS in einem kurzen Span laufen, der bei einer Ausnahme die Ausnahme aufzeichnet, den Status auf Fehler setzt und trotzdem endet. Der Guard MUSS den `Tracer` ohne statischen globalen Zustand erhalten.

#### Scenario: Fehler in einem getracten Listener
- **WHEN** ein mit `onTraced` registrierter Listener eine Ausnahme wirft
- **THEN** trägt sein Span die Ausnahme und den Fehlerstatus, der Zähler `titan.listener.failures` steigt um eins, und die Ausnahme erreicht den Guard

#### Scenario: Fehler in einem normalen Listener
- **WHEN** ein mit `on` registrierter Listener eine Ausnahme wirft
- **THEN** steigt `titan.listener.failures` für dieses Feature, und es entsteht kein Span

#### Scenario: Zwei Tests ohne gemeinsamen Zustand
- **WHEN** zwei Tests je ein eigenes `TestTelemetry` benutzen
- **THEN** sieht keiner die Spans oder Zähler des anderen

### Requirement: Startup und Shutdown sind sichtbar
Die Lobby MUSS beim Start einen Span `titan.startup` erzeugen, mit Variante, Profilen und der Zahl geladener Module, und für jedes gestartete Feature ein Span-Event `feature.started`. Beim Stoppen MUSS ein Span `titan.shutdown` mit `feature.stopped`-Events entstehen. Scheitert der Start, MUSS der Span die Ausnahme und den Fehlerstatus tragen.

#### Scenario: Start mit Reihenfolge
- **WHEN** die Lobby startet
- **THEN** gibt es einen Span `titan.startup` mit je einem Event `feature.started` pro Feature in der Startreihenfolge

#### Scenario: Start scheitert
- **WHEN** ein Feature beim Start eine Ausnahme wirft
- **THEN** trägt `titan.startup` die Ausnahme und den Fehlerstatus, und die Ausnahme geht weiter

### Requirement: Spieler-Lebenszyklus ist sichtbar
Die Lobby MUSS für jede Konfiguration, jeden Beitritt und jeden Disconnect eines Spielers einen kurzen Span (`player.configure`, `player.join`, `player.disconnect`) mit der UUID erzeugen, die Zähler `titan.player.joins` und `titan.player.disconnects` erhöhen und die Spielerzahl als Gauge `titan.players.online` bereitstellen.

#### Scenario: Beitritt und Verlassen
- **WHEN** ein Spieler beitritt und danach die Verbindung trennt
- **THEN** gibt es die Spans `player.configure`, `player.join` und `player.disconnect` mit `user.id`, und beide Zähler stehen auf eins

### Requirement: Datenbankarbeit hängt am auslösenden Span
Arbeit, die ein Modul an den `DatabaseWriter` abgibt, MUSS den Kontext des Auftraggebers mitnehmen, sodass Spans der Datenbank (Hibernate, JDBC) Kinder des auslösenden Spans mit derselben Trace-Id werden.

#### Scenario: Schreiben nach dem Rücksprung
- **WHEN** ein Span einen Auftrag an den `DatabaseWriter` gibt, der erst nach dem Ende der Methode läuft
- **THEN** hat ein Span, der im Auftrag erzeugt wird, den auslösenden Span als Elternspan
