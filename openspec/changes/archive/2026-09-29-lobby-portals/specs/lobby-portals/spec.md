# Spec Delta

## Purpose

Legt fest, wie Spieler die Lobby über Portale verlassen: Portale sind Kartendaten der Welt, lösen beim Betreten durch Laufen oder Fliegen genau einmal aus und leiten an einen CloudNet-Task weiter. Ungültige Portale verhindern den Start.

## ADDED Requirements

### Requirement: Portale sind Kartendaten der Welt
Die Lobby MUSS die Portale aus der Map-Datei (`map.json`) der geladenen Welt lesen, aus der Liste `portals`. Jedes Portal MUSS eine eindeutige Id, einen CloudNet-Task, eine Form und optional ein Recht haben. Die Lobby MUSS die Formen `box` (achsenparallel, mit `min` und `max` als Blockkoordinaten, beide einschließlich) und `disc` (Mittelpunkt, Radius, Normalenvektor beliebiger Ausrichtung) unterstützen. Eine Welt ohne `portals` hat keine Portale. Jede Welt, auch eine Saisonwelt, hat nur die Portale ihrer eigenen Map-Datei.

#### Scenario: Welt mit Portalen
- **WHEN** die Map-Datei der geladenen Welt ein Portal mit Form `box` und Task `Survival` enthält
- **THEN** kennt die Lobby dieses Portal mit genau diesem Bereich und diesem Task

#### Scenario: Welt ohne Portale
- **WHEN** die Map-Datei keine Liste `portals` enthält
- **THEN** löst keine Bewegung ein Portal aus, und die Lobby verhält sich wie zuvor

#### Scenario: Saisonwelt bringt eigene Portale mit
- **WHEN** die Lobby in einer Saisonwelt startet, deren Map-Datei andere Portale enthält als die der Standardwelt
- **THEN** gelten nur die Portale der Saisonwelt

### Requirement: Betreten eines Portals leitet den Spieler an den Task weiter
Berührt die Strecke vom bisherigen zum neuen Standort eines Spielers den Bereich eines Portals, MUSS die Lobby den Spieler an den CloudNet-Task des Portals weiterleiten. Laufen und Fliegen MÜSSEN gleichermaßen auslösen. Bei `box` zählt auch ein neuer Standort im Bereich; bei `disc` zählt das Durchqueren der Scheibe innerhalb des Radius (Rand einschließlich), in beide Richtungen. Weil die Strecke geprüft wird und nicht nur der Endpunkt, MUSS auch ein Schritt auslösen, der eine dünne Scheibe zwischen zwei Bewegungen vollständig überspringt.

#### Scenario: In eine Box laufen
- **WHEN** ein Spieler von außerhalb in den Bereich eines Portals mit Form `box` läuft
- **THEN** wird er genau einmal an den Task des Portals weitergeleitet

#### Scenario: Durch eine Scheibe fliegen
- **WHEN** ein Spieler mit Elytra zwischen zwei Bewegungen die Ebene einer Scheibe innerhalb ihres Radius durchquert, ohne dass einer der beiden Standorte nahe an ihr liegt
- **THEN** wird er genau einmal an den Task des Portals weitergeleitet

#### Scenario: Neben der Scheibe vorbei
- **WHEN** die Strecke die Ebene der Scheibe außerhalb ihres Radius durchquert oder parallel zur Ebene verläuft
- **THEN** wird niemand weitergeleitet

### Requirement: Ein Portal löst nur beim Betreten aus
Die Lobby MUSS ein Portal nur beim Übergang von „nicht berührt“ zu „berührt“ auslösen. Bleibt ein Spieler im Bereich, DARF sie ihn NICHT erneut weiterleiten.

#### Scenario: Im Bereich bleiben
- **WHEN** ein Spieler nach dem Betreten im Bereich einer Box weiterläuft, auch nach Ablauf der Abklingzeit
- **THEN** wird er nicht erneut weitergeleitet

### Requirement: Nach einer Weiterleitung gilt eine Abklingzeit von 3 Sekunden
Nach einer Weiterleitung MUSS die Lobby dem Spieler für 3 Sekunden keine weitere Weiterleitung durch irgendein Portal auslösen. Die Abklingzeit ist fest und gilt je Spieler. Ein Betreten während der Abklingzeit löst nichts aus und wird später nicht nachgeholt.

#### Scenario: Wiederbetreten innerhalb der Abklingzeit
- **WHEN** ein Spieler ein Portal verlässt und es innerhalb von 3 Sekunden nach der Weiterleitung erneut betritt
- **THEN** wird er nicht erneut weitergeleitet

#### Scenario: Wiederbetreten nach der Abklingzeit
- **WHEN** ein Spieler ein Portal verlässt und es 3 Sekunden oder mehr nach der Weiterleitung erneut betritt
- **THEN** wird er erneut weitergeleitet

#### Scenario: Abklingzeit gilt je Spieler
- **WHEN** ein Spieler weitergeleitet wurde und ein anderer Spieler innerhalb der 3 Sekunden ein Portal betritt
- **THEN** wird der andere Spieler weitergeleitet

### Requirement: Ein Recht am Portal schützt die Weiterleitung
Hat ein Portal ein Recht, MUSS die Lobby nur Spieler weiterleiten, für die die Rechteprüfung `ALLOWED` ergibt. Bei `NOT_SET` und `DENIED` DARF nichts passieren, insbesondere keine Weiterleitung und keine Chat-Nachricht. Ein Portal ohne Recht steht allen offen.

#### Scenario: Recht fehlt
- **WHEN** ein Spieler ohne das Recht eines Portals dessen Bereich betritt
- **THEN** wird er nicht weitergeleitet und erhält keine Nachricht

#### Scenario: Recht vorhanden
- **WHEN** ein Spieler mit dem Recht eines Portals dessen Bereich betritt
- **THEN** wird er an den Task des Portals weitergeleitet

#### Scenario: Abgewiesener Spieler startet keine Abklingzeit
- **WHEN** ein Spieler ohne Recht ein Portal betritt und danach mit Recht ein anderes Portal
- **THEN** wird er am zweiten Portal weitergeleitet

### Requirement: Ungültige Portale verhindern den Start
Enthält die Map-Datei ein ungültiges Portal, DARF die Lobby NICHT starten, und die Fehlermeldung MUSS Welt, Portal-Id und Grund nennen. Ungültig sind: eine unbekannte Form, ein Radius kleiner oder gleich 0, eine Normale der Länge 0, ein `min`, das in einer Achse größer ist als `max`, ein fehlender oder leerer Task, eine fehlende Id und eine Id, die in derselben Welt doppelt vorkommt. Die Lobby DARF ein ungültiges Portal NICHT überspringen und ohne es weiterlaufen.

#### Scenario: Unbekannte Form
- **WHEN** ein Portal die Form `sphere` nennt
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt die Welt, die Portal-Id und die unbekannte Form

#### Scenario: Radius nicht positiv
- **WHEN** eine Scheibe den Radius 0 hat
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt Welt, Portal-Id und den Radius

#### Scenario: Box mit vertauschten Ecken
- **WHEN** bei einer Box `min` in einer Achse größer ist als `max`
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt Welt, Portal-Id und die Achse

#### Scenario: Doppelte Id
- **WHEN** zwei Portale derselben Welt dieselbe Id haben
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt Welt und die doppelte Id
