# lobby-tracing Specification

## Purpose
Legt fest, welche Traces und Metriken die Lobby erzeugt: Spans für Start, Spielerlebenszyklus und Läufe, Zähler für hochfrequente Vorgänge, Datenschutz über die Spieler-UUID hinaus, und dass die Lobby ohne OpenTelemetry-Agent unverändert läuft.

## Requirements

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

### Requirement: Läufe erzeugen Spans mit ihrem Ausgang
Die Lobby MUSS für jeden beendeten Jump-and-Run-Lauf genau einen kurzen Span `jumprun.end` erzeugen, mit dem Grund (`EndReason`), dem Modus, dem Score und der Spieler-UUID als Attribute. Beim Grund FALL MUSS der Span zusätzlich die Höhe `y`, die Absturzschwelle und den Index des letzten Blocks tragen. Ein Lauf, der schon beendet ist, DARF keinen zweiten Span erzeugen. Ein Span DARF NICHT einen ganzen Lauf umspannen.

#### Scenario: Absturz mit Details
- **WHEN** ein Lauf mit dem Grund FALL endet
- **THEN** gibt es einen Span `jumprun.end` mit `jumprun.end.reason=FALL`, `jumprun.fall.y`, `jumprun.fall.threshold` und `jumprun.fall.course_index`, ohne Fehlerstatus

#### Scenario: Anderer Grund ohne Absturzdetails
- **WHEN** ein Lauf durch den Spieler abgebrochen wird (ABORT)
- **THEN** gibt es einen Span `jumprun.end` mit `jumprun.end.reason=ABORT`, Modus und Score, und ohne `jumprun.fall.*`

#### Scenario: Doppeltes Ende
- **WHEN** ein Lauf durch Disconnect endet und danach der Shutdown ihn erneut beenden will
- **THEN** existiert nur ein Span `jumprun.end` für diesen Lauf

#### Scenario: Fehler beim Speichern
- **WHEN** `records.submit` beim Beenden eine Ausnahme wirft
- **THEN** trägt der Span `jumprun.end` die Ausnahme und den Fehlerstatus, ist beendet, und die Ausnahme erreicht den Aufrufer

### Requirement: Startversuche und Bestenlisten-Aktualisierungen sind sichtbar
Die Lobby MUSS für jeden Startversuch einen Span `jumprun.start` mit dem Ergebnis `started` oder `no_room` erzeugen, und für jede Aktualisierung der Bestenliste einen Span `jumprun.leaderboard.refresh`, der im Executor-Task liegt.

#### Scenario: Kein Platz zum Starten
- **WHEN** ein Spieler startet und die Lobby keinen Platz für den Aufstieg findet
- **THEN** gibt es einen Span `jumprun.start` mit `jumprun.start.outcome=no_room` und keinen Span `jumprun.end`

#### Scenario: Bestenliste wird aktualisiert
- **WHEN** der Scheduler eine Aktualisierung anstößt und der Executor sie ausführt
- **THEN** gibt es einen Span `jumprun.leaderboard.refresh`

### Requirement: Lauf-Metriken
Die Lobby MUSS beim Beenden den Zähler `titan.jumprun.runs.ended` mit `reason` und `mode`, beim Start den Zähler `titan.jumprun.runs.started` mit `mode` und `outcome` erhöhen und den Score in das Histogramm `titan.jumprun.run.score` mit `mode` eintragen. Diese Metriken DÜRFEN KEIN `user.id` tragen.

#### Scenario: Zähler beim Absturz
- **WHEN** ein Lauf im Modus MEDIUM mit FALL endet
- **THEN** steht `titan.jumprun.runs.ended{reason=FALL,mode=MEDIUM}` auf eins

### Requirement: Datenbankspans eines Laufs hängen unter `jumprun.end`
Der Schreibauftrag eines beendeten Laufs MUSS im Span `jumprun.end` abgegeben werden, sodass Spans der Datenbank mit derselben Trace-Id Kinder von `jumprun.end` sind.

#### Scenario: Schreiben nach dem Rücksprung
- **WHEN** `jumprun.end` einen Schreibauftrag abgibt, der erst nach dem Ende der Methode läuft
- **THEN** hat ein im Auftrag erzeugter Span `jumprun.end` als Elternspan

### Requirement: Die Testhilfe liest Histogramme wie Zähler
Die Testhilfe `TestTelemetry` MUSS neben Spans, Zählern und Gauges auch Histogramme auslesen können: alle aufgezeichneten Punkte eines Histogramms sowie Anzahl und Summe für genau einen Attributsatz. Sie MUSS dabei, wie für Zähler, je Test einen eigenen Zustand verwenden. Ein Test DARF dafür KEINEN eigenen Metrik-Reader aufbauen müssen.

#### Scenario: Anzahl und Summe je Attributsatz
- **WHEN** ein Test zwei Werte (3 und 4) mit den Attributen `mode=MEDIUM` und einen Wert (10) mit `mode=HARD` in dasselbe Histogramm aufzeichnet
- **THEN** liefert die Abfrage für `mode=MEDIUM` die Anzahl 2 und die Summe 7, und die für `mode=HARD` die Anzahl 1 und die Summe 10

#### Scenario: Nichts aufgezeichnet
- **WHEN** ein Histogramm nie befüllt wurde oder ein Attributsatz nie vorkam
- **THEN** liefert die Abfrage Anzahl 0 und Summe 0, ohne Ausnahme

#### Scenario: Alle Punkte
- **WHEN** ein Test Werte unter zwei Attributsätzen aufzeichnet
- **THEN** liefert die Punkteliste genau einen Punkt je Attributsatz

#### Scenario: Zwei Tests ohne gemeinsamen Zustand
- **WHEN** zwei Tests je ein eigenes `TestTelemetry` benutzen und nur einer ein Histogramm befüllt
- **THEN** sieht der andere keinen Punkt
