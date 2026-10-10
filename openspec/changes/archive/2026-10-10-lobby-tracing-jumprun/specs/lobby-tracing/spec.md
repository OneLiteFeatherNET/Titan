# Spec Delta

## ADDED Requirements

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
