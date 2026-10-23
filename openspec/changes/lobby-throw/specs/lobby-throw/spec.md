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

### Requirement: Werfen per Linksklick

Ein Werfer, der ein Ziel trägt, MUSS den Wurf mit einem Linksklick auslösen. Der Wurf MUSS das Ziel in Blickrichtung des Werfers bewegen, mit einem Betrag zwischen 8 und 16 Blöcken pro Sekunde, Standard 12. Die vertikale Komponente DARF 10 Blöcke pro Sekunde nicht überschreiten. Die Lobby DARF den Impuls nur setzen; die Bewegung führt der Client des Ziels aus. Der Wurf DARF nicht innerhalb von 500 ms nach der Aufnahme erfolgen.

#### Scenario: Wurf in Blickrichtung

- **WHEN** der Werfer ein Ziel trägt, 500 ms vergangen sind und er links klickt
- **THEN** wird das Ziel von seinem Kopf gelöst und erhält einen Impuls in Blickrichtung mit Betrag im Bereich von 8 bis 16 Blöcken pro Sekunde

#### Scenario: Wurf steil nach unten

- **WHEN** der Werfer nach unten blickt und einen Wurf auslöst
- **THEN** ist die vertikale Komponente des Impulses nicht negativ, und der Betrag bleibt im Bereich

#### Scenario: Klick direkt nach der Aufnahme

- **WHEN** der Werfer 200 ms nach der Aufnahme links klickt
- **THEN** wird nicht geworfen, und das Ziel bleibt auf dem Kopf

#### Scenario: Rechtsklick wirft nicht

- **WHEN** der Werfer ein Ziel trägt und rechts klickt
- **THEN** wird nicht geworfen

### Requirement: Ziel kann sich durch Schleichen lösen

Schleicht das getragene Ziel, MUSS es absteigen. Das MUSS als Abbruch gezählt werden und eine Sperre für das Paar aus Werfer und Ziel von 60 Sekunden auslösen. Ein Abbruch DARF KEINEN Impuls setzen.

#### Scenario: Ziel schleicht

- **WHEN** das getragene Ziel schleicht
- **THEN** steigt es ab ohne Impuls, und es darf innerhalb von 60 Sekunden nicht erneut von demselben Werfer aufgenommen werden

#### Scenario: Ziel schleicht ohne Trage

- **WHEN** ein Spieler, der nicht getragen wird, schleicht
- **THEN** ändert sich am Throw-Zustand nichts

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

#### Scenario: Ziel hat Opt-out

- **WHEN** ein Spieler mit `titan.throw` ein Ziel aufnehmen will, dessen Einstellung `titan:throw.target` ausgeschaltet ist
- **THEN** wird das Ziel nicht aufgenommen, der Werfer erhält eine Nachricht, und das Ziel erhält keine Nachricht

#### Scenario: Ziel ist immun

- **WHEN** ein Spieler mit `titan.throw` ein Ziel mit `titan.throw.immune` aufnehmen will
- **THEN** wird das Ziel nicht aufgenommen, und der Werfer erhält eine Nachricht

#### Scenario: Opt-out wird zwischen zwei Aufnahmen geändert

- **WHEN** ein Ziel seine Einstellung ausschaltet, nachdem es einmal aufgenommen wurde, und danach erneut aufgenommen werden soll
- **THEN** wird die Aufnahme abgelehnt

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

### Requirement: Rückmeldung für Werfer und Zuschauer

Beim Aufnehmen MUSS ein Vanilla-Ton und beim Wurf ein Vanilla-Knockback-Ton zu hören sein; beim Wurf MUSS eine Vanilla-Partikelwolke am Werfer erscheinen. Der Werfer MUSS während der Trage eine Actionbar mit „Linksklick zum Werfen“ sehen. Es DARF keine Resource-Pack-Datei benötigt werden. Texte MÜSSEN in Deutsch und Englisch vorliegen.

#### Scenario: Aufnahme

- **WHEN** ein Ziel aufgenommen wird
- **THEN** hört der Werfer einen Vanilla-Ton, und der Werfer sieht die Actionbar „Linksklick zum Werfen“

#### Scenario: Wurf

- **WHEN** der Werfer wirft
- **THEN** hören die Nähe-Spieler den Wurf-Ton und sehen die Partikelwolke

### Requirement: Telemetrie zählt Aufnahmen, Würfe und Abbrüche

Die Lobby MUSS die Zähler `titan.throw.pickups`, `titan.throw.throws`, `titan.throw.escapes` und `titan.throw.refusals` führen. Der Zähler `refusals` MUSS das Attribut `reason` mit einem der Werte `permission`, `opt_out`, `immune`, `cooldown` oder `state` tragen. Die Attribute DÜRFEN keine Spieler-UUIDs enthalten.

#### Scenario: Ablehnung wegen Opt-out

- **WHEN** eine Aufnahme am Opt-out scheitert
- **THEN** zählt `titan.throw.refusals` mit `reason=opt_out`

#### Scenario: Abbruch durch Schleichen

- **WHEN** das Ziel schleicht und die Trage endet
- **THEN** zählt `titan.throw.escapes` um eins
