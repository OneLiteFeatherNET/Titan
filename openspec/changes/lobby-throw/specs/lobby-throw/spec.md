# Spec Delta

## ADDED Requirements

### Requirement: Aufnehmen nur mit Recht

Ein Spieler MUSS das Recht `titan.throw` haben, um einen anderen Spieler aufzunehmen. Die Aufnahme MUSS per Rechtsklick auf den Ziel-Spieler bei leerer Haupthand erfolgen. Die Rechteprüfung MUSS bei jeder Aufnahme erneut erfolgen. Ist das Recht nicht `ALLOWED`, DARF keine Aufnahme stattfinden, und der Werfer erhält eine kurze Nachricht.

#### Scenario: Werfer mit Recht nimmt Spieler auf

- **WHEN** ein Spieler mit `titan.throw` bei leerer Haupthand einen anderen Spieler rechtsklickt
- **THEN** sitzt das Ziel als Passagier auf dem Kopf des Werfers, und beide sehen die Trage

#### Scenario: Werfer ohne Recht

- **WHEN** ein Spieler ohne `titan.throw` einen anderen Spieler rechtsklickt
- **THEN** wird das Ziel nicht aufgenommen, und das Ziel bemerkt nichts

#### Scenario: Haupthand nicht leer

- **WHEN** ein Spieler mit Recht mit einem Item in der Haupthand einen anderen Spieler rechtsklickt
- **THEN** wird das Ziel nicht aufgenommen, und die Item-Benutzung läuft wie bisher

### Requirement: Der Ladeträger ersetzt die Haupthand während der Trage

Während einer Trage MUSS der Werfer in seinem Hotbar-Platz einen temporären Ladeträger halten, einen Stock mit der Komponente `consumable` und der Animation `bow`, ohne Resource Pack. Der Originalinhalt des Platzes MUSS beim Start der Trage gesichert werden. Der Ladeträger DARF NICHT gespeichert werden.

#### Scenario: Trage beginnt

- **WHEN** ein Ziel aufgenommen wird
- **THEN** hält der Werfer im Hotbar-Platz den Ladeträger, und der bisherige Inhalt ist gesichert

#### Scenario: Ladeträger ist vanilla

- **WHEN** der Werfer den Ladeträger rechtsklickt
- **THEN** zeigt der Client die vanilla Zieh-Animation des Bogens, ohne dass eine Resource-Pack-Datei geladen wird

### Requirement: Aufladen per Rechtsklick, Wurf beim Loslassen

Der Werfer MUSS den Wurf aufladen, indem er den Ladeträger rechtsklickt und hält. Das Loslassen MUSS den Wurf auslösen. Die Ladezeit in Ticks MUSS aus dem Loslassen-Signal kommen, nicht aus einer eigenen Zeitmessung. Ein Loslassen ohne Ladung DARF NICHT werfen.

#### Scenario: Ladung und Wurf

- **WHEN** der Werfer den Ladeträger 20 Ticks hält und loslässt
- **THEN** wird das Ziel geworfen, mit Ladestärke 20 von 30 Ticks

#### Scenario: Loslassen ohne Ladung

- **WHEN** der Werfer den Ladeträger loslässt, ohne ihn vorher aufgeladen zu haben
- **THEN** wird nicht geworfen

#### Scenario: Hotbar-Platzwechsel während der Ladung

- **WHEN** der Werfer während der Ladung einen anderen Hotbar-Platz wählt
- **THEN** wird die Ladung abgebrochen, das Ziel bleibt auf dem Kopf, und es wird nicht geworfen

### Requirement: Stärke linear mit Obergrenze

Die Stärke `p` MUSS `min(t, 30) / 30` für eine Ladezeit `t` in Ticks sein. Die Geschwindigkeit MUSS `8 + 8 * p` Blöcke pro Sekunde sein. Die vertikale Komponente DARF 10 Blöcke pro Sekunde nicht überschreiten. Ein Wurf unter 2 Ticks MUSS mit `p = 0` erfolgen.

#### Scenario: Obergrenze

- **WHEN** der Werfer den Ladeträger 90 Ticks hält und loslässt
- **THEN** wird mit `p = 1` geworfen, also mit 16 Blöcken pro Sekunde

#### Scenario: Tippen

- **WHEN** der Werfer den Ladeträger 1 Tick hält und loslässt
- **THEN** wird mit `p = 0` geworfen, also mit 8 Blöcken pro Sekunde

#### Scenario: Wurf steil nach unten

- **WHEN** der Werfer nach unten blickt und einen Wurf auslöst
- **THEN** ist die vertikale Komponente des Impulses nicht negativ, und der Betrag bleibt gleich

### Requirement: Wurf in Blickrichtung über den Client des Ziels

Der Wurf MUSS das Ziel in Blickrichtung des Werfers bewegen. Die Lobby DARF den Impuls nur setzen; die Bewegung führt der Client des Ziels aus.

#### Scenario: Wurf in Blickrichtung

- **WHEN** der Werfer nach Osten blickt und mit Stärke 1 wirft
- **THEN** erhält das Ziel einen Impuls nach Osten mit 16 Blöcken pro Sekunde

### Requirement: Flugbahnvorschau nur für den Werfer

Während der Ladung MUSS der Werfer eine vorhergesagte Flugbahn als vanilla Partikel sehen, mit einem Landemarker. Die Vorschau MUSS aus Blickrichtung, aktueller Stärke, Gravitation 0,08 Blöcke pro Tick² und Luftwiderstand 0,98 pro Tick berechnet werden. Sie MUSS höchstens 40 Punkte pro Aktualisierung senden und alle 2 Ticks erneuern. Die Berechnung MUSS deterministisch sein: gleiche Eingabe ergibt gleiche Punkte.

Die Partikel-Pakete MUSS die Lobby ausschließlich an den Werfer senden. Andere Spieler DÜRFEN keine Vorschau-Pakete erhalten.

#### Scenario: Vorschau während der Ladung

- **WHEN** der Werfer lädt
- **THEN** sieht er alle 2 Ticks Partikel entlang der Bahn und einen Landemarker

#### Scenario: Vorschau nur für den Werfer

- **WHEN** die Vorschau läuft
- **THEN** erhält kein anderer Spieler ein Vorschau-Paket

#### Scenario: Vorschau endet mit dem Loslassen

- **WHEN** der Werfer loslässt oder die Trage endet
- **THEN** wird die Vorschau nicht mehr aktualisiert

#### Scenario: Begrenzte Punktzahl

- **WHEN** die Bahn länger als 40 Punkte wäre
- **THEN** werden höchstens 40 Punkte gesendet

### Requirement: Fortschritt anzeigen

Während der Ladung MUSS der Werfer eine Actionbar mit einer Ladeleiste sehen, die mit der Stärke wächst. Ein vanilla Ton MUSS alle 5 Ticks erklingen, dessen Tonhöhe mit der Stärke steigt. Beides MUSS nur dem Werfer gelten.

#### Scenario: Leiste wächst

- **WHEN** der Werfer lädt
- **THEN** wächst die Leiste in der Actionbar mit jeder Ladung

### Requirement: Ziel kann sich durch Schleichen lösen

Schleicht das getragene Ziel, MUSS es absteigen. Das MUSS als Abbruch gezählt werden und eine Sperre für das Paar aus Werfer und Ziel von 60 Sekunden auslösen. Ein Abbruch DARF KEINEN Impuls setzen.

#### Scenario: Ziel schleicht

- **WHEN** das getragene Ziel schleicht
- **THEN** steigt es ab ohne Impuls, und es darf innerhalb von 60 Sekunden nicht erneut von demselben Werfer aufgenommen werden

#### Scenario: Ziel schleicht ohne Trage

- **WHEN** ein Spieler, der nicht getragen wird, schleicht
- **THEN** ändert sich am Throw-Zustand nichts

### Requirement: Hand wird auf jedem Pfad wiederhergestellt

Die Lobby MUSS den gesicherten Inhalt des Hotbar-Platzes exakt zurücksetzen, sobald eine Trage endet. Das gilt für Wurf, Schleichen, Auto-Drop, Opt-out, Immunität, Verbindungsabbruch, Instanzwechsel, Tod und Stopp der Lobby.

#### Scenario: Wurf stellt die Hand wieder her

- **WHEN** das Ziel geworfen wird
- **THEN** hat der Werfer im Hotbar-Platz wieder genau den gesicherten Inhalt

#### Scenario: Auto-Drop stellt die Hand wieder her

- **WHEN** eine Trage 15 Sekunden gedauert hat
- **THEN** steigt das Ziel ohne Impuls ab, und der Hotbar-Platz hat wieder den gesicherten Inhalt

#### Scenario: Tod stellt die Hand wieder her

- **WHEN** der Werfer während der Trage stirbt
- **THEN** hat er vor dem Respawn wieder den gesicherten Inhalt

#### Scenario: Stopp der Lobby

- **WHEN** die Lobby während einer Trage stoppt
- **THEN** erhält jeder Online-Werfer seinen gesicherten Inhalt zurück, ohne Impuls

#### Scenario: Ladeträger wird nicht abgelegt

- **WHEN** der Werfer den Ladeträger über Drop oder Tausch ablegen will
- **THEN** wird die Aktion abgebrochen, und der Ladeträger bleibt in der Hand

### Requirement: Sperren begrenzen Belästigung

Die Lobby MUSS folgende Sperren durchsetzen: Ein Werfer DARF nicht innerhalb von 5 Sekunden nach einem Wurf erneut aufnehmen. Ein Ziel DARF höchstens einmal pro 20 Sekunden aufgenommen werden, unabhängig vom Werfer. Eine Trage DARF höchstens 15 Sekunden dauern; danach MUSS sie automatisch ohne Impuls enden (Auto-Drop). Eine Ablehnung wegen einer Sperre MUSS dem Werfer als kurze Nachricht gezeigt werden.

#### Scenario: Serie von Würfen

- **WHEN** ein Werfer 2 Sekunden nach einem Wurf erneut aufnehmen will
- **THEN** wird die Aufnahme abgelehnt, und der Werfer erhält eine Nachricht

#### Scenario: Ziel wird zu oft aufgenommen

- **WHEN** ein Ziel 10 Sekunden nach seiner letzten Aufnahme durch einen anderen Werfer erneut aufgenommen werden soll
- **THEN** wird die Aufnahme abgelehnt

#### Scenario: Auto-Drop

- **WHEN** eine Trage 15 Sekunden gedauert hat
- **THEN** steigt das Ziel ab, ohne Impuls, und der Werfer ist wieder frei

### Requirement: Opt-out und Immunität schützen das Ziel

Ein Ziel, dessen Opt-out-Einstellung `titan:throw.target` ausgeschaltet ist, DARF NICHT aufgenommen werden. Ein Ziel mit dem Recht `titan.throw.immune` DARF NICHT aufgenommen werden. In beiden Fällen MUSS der Werfer eine kurze Nachricht erhalten, und das Ziel DARF KEINE Nachricht erhalten. Die Opt-out-Einstellung MUSS bei jeder Aufnahme gelesen werden, ohne Neustart der Lobby. Der Standardwert der Einstellung ist „an“.

Schaltet ein Ziel den Opt-out während einer Trage ein, MUSS die Trage sofort ohne Impuls enden. Die Hand des Werfers MUSS dabei wiederhergestellt werden.

#### Scenario: Ziel hat Opt-out

- **WHEN** ein Spieler mit `titan.throw` ein Ziel aufnehmen will, dessen Einstellung `titan:throw.target` ausgeschaltet ist
- **THEN** wird das Ziel nicht aufgenommen, der Werfer erhält eine Nachricht, und das Ziel erhält keine Nachricht

#### Scenario: Ziel schaltet Opt-out während der Trage ein

- **WHEN** das getragene Ziel seinen Opt-out einschaltet
- **THEN** steigt es sofort ohne Impuls ab, und der Hotbar-Platz des Werfers hat wieder seinen Inhalt

#### Scenario: Ziel ist immun

- **WHEN** ein Spieler mit `titan.throw` ein Ziel mit `titan.throw.immune` aufnehmen will
- **THEN** wird das Ziel nicht aufgenommen, und der Werfer erhält eine Nachricht

#### Scenario: Standardwert

- **WHEN** ein Spieler noch keine Einstellung gesetzt hat
- **THEN** ist die Einstellung `titan:throw.target` eingeschaltet, und er kann aufgenommen werden

### Requirement: Nicht werfbar in bestimmten Zuständen

Die Lobby MUSS folgende Zustände für Werfer und Ziel als Ausschluss behandeln: Tanzen, Sitzen, Elytra-Flug, Jump-and-Run, bereits getragen oder selbst als Werfer eines Ziels, sowie ein laufender Flug eines geworfenen Spielers. Ein Spieler in einem dieser Zustände DARF weder werfen noch aufgenommen werden; die Ablehnung nennt den Grund im Telemetrie-Attribut `reason`.

#### Scenario: Ziel tanzt

- **WHEN** das Ziel gerade tanzt und ein Werfer es aufnehmen will
- **THEN** wird die Aufnahme abgelehnt

#### Scenario: Ziel sitzt

- **WHEN** das Ziel auf einem Sitz sitzt und aufgenommen werden soll
- **THEN** wird die Aufnahme abgelehnt

#### Scenario: Werfer ist selbst getragen

- **WHEN** ein Spieler, der selbst auf dem Kopf eines anderen sitzt, ein Ziel aufnehmen will
- **THEN** wird die Aufnahme abgelehnt

### Requirement: Geworfene Spieler lösen keine Portale aus

Ein geworfener Spieler MUSS während des Flugs und 2 Sekunden nach der Landung von jeder Portal-Übergabe ausgenommen sein. Der Flug DARF höchstens 5 Sekunden dauern; danach MUSS der Flugzustand enden, auch ohne Landung.

#### Scenario: Flug über ein Portal

- **WHEN** ein geworfener Spieler während des Flugs durch ein Portal fliegt
- **THEN** wird er nicht an den Zielserver übergeben

#### Scenario: Landung

- **WHEN** der geworfene Spieler landet
- **THEN** bleibt der Portal-Schutz noch 2 Sekunden bestehen, danach gilt die Portal-Logik wieder

### Requirement: Trage endet bei Verbindungs- und Instanzwechseln

Verlässt der Werfer oder das Ziel das Spiel, MUSS die Trage ohne Impuls enden. Wechselt der Werfer oder das Ziel die Instanz, MUSS die Trage ohne Impuls enden. Beim Stopp der Lobby MUSS jede Trage ohne Impuls abgebaut werden. Nichts davon DARF gespeichert werden.

#### Scenario: Werfer verlässt das Spiel

- **WHEN** der Werfer das Spiel verlässt, während er ein Ziel trägt
- **THEN** steigt das Ziel ohne Impuls ab

#### Scenario: Ziel wechselt die Instanz

- **WHEN** das getragene Ziel in eine andere Instanz wechselt
- **THEN** endet die Trage ohne Impuls

### Requirement: Rückmeldung beim Aufnehmen und Werfen

Beim Aufnehmen MUSS ein Vanilla-Ton erklingen. Beim Wurf MUSS ein Vanilla-Knockback-Ton und eine Vanilla-Partikelwolke am Werfer erscheinen. Es DARF keine Resource-Pack-Datei benötigt werden. Texte MÜSSEN in Deutsch und Englisch vorliegen.

#### Scenario: Aufnahme

- **WHEN** ein Ziel aufgenommen wird
- **THEN** hört der Werfer einen Vanilla-Ton

#### Scenario: Wurf

- **WHEN** der Werfer wirft
- **THEN** hören die Nähe-Spieler den Wurf-Ton und sehen die Partikelwolke

### Requirement: Telemetrie zählt Aufnahmen, Würfe, Abbrüche und Ladeabbrüche

Die Lobby MUSS die Zähler `titan.throw.pickups`, `titan.throw.throws`, `titan.throw.escapes`, `titan.throw.refusals` und `titan.throw.charge_aborts` führen. Der Zähler `refusals` MUSS das Attribut `reason` mit einem der Werte `permission`, `opt_out`, `immune`, `cooldown` oder `state` tragen. Die Attribute DÜRFEN keine Spieler-UUIDs enthalten.

#### Scenario: Ablehnung wegen Opt-out

- **WHEN** eine Aufnahme am Opt-out scheitert
- **THEN** zählt `titan.throw.refusals` mit `reason=opt_out`

#### Scenario: Abbruch durch Schleichen

- **WHEN** das Ziel schleicht und die Trage endet
- **THEN** zählt `titan.throw.escapes` um eins

#### Scenario: Ladeabbruch

- **WHEN** der Werfer während der Ladung den Hotbar-Platz wechselt
- **THEN** zählt `titan.throw.charge_aborts` um eins
