# Spec Delta

## ADDED Requirements

### Requirement: Admin-Befehle sind sichtbar
Die Lobby MUSS für jede Ausführung eines Admin-Befehls (`stop`, `end`) einen kurzen Span `admin.command` mit Befehl, Absenderart (`console`, `player`), Ergebnis (`executed`, `denied`) und bei Spielern der UUID erzeugen und `titan.admin.commands{command,result}` erhöhen. Der Span MUSS enden, bevor das Herunterfahren beginnt.

#### Scenario: Stop durch die Konsole
- **WHEN** die Konsole `/stop` ausführt
- **THEN** gibt es einen Span `admin.command` mit `admin.command=stop`, `admin.sender=console` und `admin.result=executed`, der vor `titan.shutdown` endet

#### Scenario: Stop ohne Recht
- **WHEN** ein Spieler ohne Recht `/stop` versucht
- **THEN** steigt `titan.admin.commands{command=stop,result=denied}` und der Server stoppt nicht

### Requirement: Hotbar-Items sind sichtbar
Die Lobby MUSS für das Ausrüsten eines Spielers einen Span `hotbar.equip` mit der Item-Zahl und für jede Nutzung eines Lobby-Items einen Span `hotbar.item.use` mit dem Item-Schlüssel erzeugen und `titan.hotbar.item.uses{item}` erhöhen. Ein beim Start erkannter Item-Konflikt MUSS als Span-Event `hotbar.item_conflict` am Span `titan.startup` stehen.

#### Scenario: Item benutzen
- **WHEN** ein Spieler das Navigator-Item benutzt
- **THEN** gibt es einen Span `hotbar.item.use` mit `hotbar.item` und `user.id`, und `titan.hotbar.item.uses{item}` steigt um eins

### Requirement: Rechteprüfungen sind messbar, ohne Span je Prüfung
Die Lobby MUSS jede Rechteprüfung in `titan.permission.checks{result}` zählen und, wenn gerade ein Span aktiv ist, als Span-Event `permission.check` mit dem Recht und dem Ergebnis an diesen Span hängen. Sie DARF dafür KEINEN eigenen Span erzeugen. Der Start der Plattform MUSS einen Span `permission.platform.start` mit dem Plattformnamen erzeugen.

#### Scenario: Prüfung innerhalb eines Spans
- **WHEN** das Portal im Span `portal.transfer` die Rechte prüft und sie verweigert werden
- **THEN** trägt `portal.transfer` ein Event `permission.check` mit `result=denied`, und `titan.permission.checks{result=denied}` steigt

#### Scenario: Prüfung ohne aktiven Span
- **WHEN** eine Rechteprüfung ohne aktiven Span läuft
- **THEN** steigt der Zähler, und es entsteht weder Span noch Fehler
