# lobby-hotbar Specification

## Purpose
Legt fest, wie Module ihre Lobby-Items (Hotbar und Ausrüstung) anmelden, wie Konflikte um denselben Platz verhindert werden und wie ein benutztes Item beim richtigen Modul ankommt.

## Requirements

### Requirement: Module melden Items mit festem Platz an
Ein Modul MUSS ein Lobby-Item zusammen mit seinem Platz anmelden können, entweder als Hotbar-Slot oder als Ausrüstungsplatz. Beim Betreten der Lobby und nach einem Respawn MUSS der Spieler genau die angemeldeten Items auf ihren Plätzen erhalten. Sein Inventar enthält danach sonst nichts.

#### Scenario: Standardausstattung
- **WHEN** ein Spieler die Lobby betritt
- **THEN** liegt der Navigator (Feder) in Hotbar-Slot 4, er trägt eine unzerstörbare Elytra auf dem Brustplatz, und sonst ist sein Inventar leer

#### Scenario: Ausstattung nach Respawn
- **WHEN** ein Spieler stirbt und respawnt
- **THEN** hat er wieder genau die Standardausstattung

### Requirement: Platzkonflikte werden beim Start erkannt
Melden zwei Module ein Item für denselben Platz an, MUSS die Lobby den Start abbrechen. Die Fehlermeldung MUSS den Platz und beide Module nennen.

#### Scenario: Zwei Module wollen Slot 4
- **WHEN** das Modul „navigator“ und ein Modul „friends“ beide ein Item für Hotbar-Slot 4 anmelden
- **THEN** startet die Lobby nicht und meldet den Konflikt um Slot 4 zwischen „navigator“ und „friends“

### Requirement: Benutzte Items erreichen ihr Modul
Benutzt ein Spieler ein angemeldetes Item, MUSS genau das Modul die Benutzung erhalten, das das Item angemeldet hat. Die Zuordnung MUSS über die Identität des Items erfolgen, nicht über Aussehen, Material oder Namen.

#### Scenario: Navigator benutzen
- **WHEN** ein Spieler die Navigator-Feder benutzt
- **THEN** öffnet sich der Navigator, und kein anderes Modul reagiert

#### Scenario: Gleiches Material, anderes Item
- **WHEN** ein Spieler eine gewöhnliche Feder benutzt, die kein angemeldetes Item ist
- **THEN** öffnet sich der Navigator nicht

### Requirement: Items mit wechselndem Platz
Ein Modul MUSS ein Item auch ohne festen Platz anmelden können, sodass Benutzungen bei ihm ankommen. Es MUSS dieses Item selbst geben und wieder entfernen können.

#### Scenario: Feuerwerk beim Fliegen
- **WHEN** ein Spieler mit der Elytra zu fliegen beginnt
- **THEN** erhält er eine Feuerwerksrakete in die Nebenhand

#### Scenario: Feuerwerk nach dem Landen
- **WHEN** der Spieler aufhört zu fliegen
- **THEN** ist die Nebenhand wieder leer

#### Scenario: Boost beim Fliegen
- **WHEN** ein fliegender Spieler die Feuerwerksrakete benutzt und weder ein Boost noch dessen Abklingzeit gerade läuft
- **THEN** feuert die Lobby eine Feuerwerksrakete ab, die für die konfigurierte Brenndauer (`elytra.burnDurationTicks`) auf ihn abgestimmt bleibt und ihn in Blickrichtung beschleunigt, genau wie im Spiel ohne Mod beim Feuerwerks-Boost - der Client wendet diesen Impuls selbst an, die Lobby setzt selbst keine Geschwindigkeit

#### Scenario: Rakete während eines laufenden Boosts oder seiner Abklingzeit
- **WHEN** ein fliegender Spieler die Feuerwerksrakete erneut benutzt, während der zuletzt gestartete Boost noch brennt oder dessen Abklingzeit (`elytra.cooldownTicks`, gemessen ab dem Start des Boosts) noch läuft
- **THEN** wird keine weitere Rakete abgefeuert, und der laufende Boost läuft unverändert bis zu seinem eigenen Ende weiter

### Requirement: Lobby-Items sind geschützt
Angemeldete Items DÜRFEN NICHT fallen gelassen, verschoben, zwischen den Händen getauscht oder aus dem Inventar genommen werden können.

#### Scenario: Item fallen lassen
- **WHEN** ein Spieler versucht, die Navigator-Feder fallen zu lassen
- **THEN** bleibt die Feder in Slot 4
