# Spec Delta

## Purpose

Legt fest, wie die Lobby in einem konfigurierten Zeitfenster in einer eigenen Saisonwelt läuft: Die Welt wird beim Start gewählt, ein nötiger Wechsel wird nur über einen Neustart bei leerer Lobby vollzogen, und ungültige Konfiguration verhindert den Start.

## ADDED Requirements

### Requirement: Die Welt wird beim Start nach dem aktiven Saisonfenster gewählt
Beim Start MUSS die Lobby die Welt der aktiven Saison laden. Eine Saison ist aktiv, wenn `seasons.<id>.enabled` wahr ist und die aktuelle Zeit der konfigurierten Zeitzone im Fenster von `seasons.<id>.from` (einschließlich) bis `seasons.<id>.to` (ausschließlich) liegt. Die Welt ist das Verzeichnis unter `worlds/`, das `seasons.<id>.world` nennt, mit seiner eigenen Map-Datei (Spawn). `seasons.<id>.enabled` MUSS standardmäßig wahr sein. Die Zeitzone MUSS über `seasons.zone` einstellbar sein und standardmäßig `Europe/Berlin` lauten.

#### Scenario: Fenster ist aktiv
- **WHEN** die Saison `winter` auf die Welt `winter` zeigt, ihr Fenster die aktuelle Zeit enthält und die Lobby startet
- **THEN** lädt die Lobby `worlds/winter` und spawnt Spieler an dessen Map-Spawn

#### Scenario: Fenster ist noch nicht erreicht oder vorbei
- **WHEN** die aktuelle Zeit vor `from` oder ab `to` einer Saison liegt und die Lobby startet
- **THEN** lädt die Lobby die Standardwelt (`-DTITAN_LOBBY_MAP`, sonst `world`)

#### Scenario: Fensterende ist ausgeschlossen
- **WHEN** die aktuelle Zeit genau `to` einer Saison entspricht
- **THEN** gilt die Saison als nicht aktiv

#### Scenario: Zeitzone bestimmt das Fenster
- **WHEN** `seasons.zone` auf `Asia/Tokyo` steht und `from` lokal in Tokio erreicht ist, in Berlin aber noch nicht
- **THEN** gilt die Saison als aktiv

### Requirement: Ohne aktive Saison gilt das bisherige Verhalten
Ist keine Saison konfiguriert oder keine aktiv, MUSS die Lobby die Welt wie bisher wählen. Eine Konfiguration ohne `seasons.*` DARF sich für Spieler und Betreiber nicht verändern.

#### Scenario: Keine Saison konfiguriert
- **WHEN** die Konfiguration keinen Abschnitt `seasons` enthält
- **THEN** lädt die Lobby die Standardwelt und merkt keinen Neustart vor

### Requirement: Der Abschalter wirkt ohne Konfigurations-Neustart
`seasons.<id>.enabled` MUSS live gelesen werden, sodass eine Änderung über die Konfigurationsüberwachung ohne Neustart der Lobby wirkt. Eine abgeschaltete Saison MUSS bei der Bestimmung der gewünschten Welt so behandelt werden, als wäre sie nicht konfiguriert.

#### Scenario: Abschalten während der Saison
- **WHEN** die Lobby in der Welt der Saison `winter` läuft und `seasons.winter.enabled` auf `false` gesetzt wird
- **THEN** ist die gewünschte Welt die Standardwelt, und ein Neustart ist vorgemerkt

#### Scenario: Einschalten im Fenster
- **WHEN** die Lobby in der Standardwelt läuft und `seasons.winter.enabled` im aktiven Fenster auf `true` gesetzt wird
- **THEN** ist die gewünschte Welt die Welt von `winter`, und ein Neustart ist vorgemerkt

### Requirement: Ein Neustart ist nur vorgemerkt, wenn gewünschte und gestartete Welt abweichen
Die Lobby MUSS die gewünschte Welt mindestens einmal pro Minute mit der Welt vergleichen, mit der sie gestartet ist. Weichen beide ab, MUSS ein Neustart vorgemerkt sein; das MUSS einmal pro Vormerkung mit der Saison und dem Zeitpunkt geloggt werden. Stimmen beide wieder überein, MUSS die Vormerkung aufgehoben werden. Die Lobby DARF die Welt zur Laufzeit NICHT wechseln.

#### Scenario: Saisonbeginn erreicht
- **WHEN** die Lobby in der Standardwelt läuft und das Fenster der Saison `winter` beginnt
- **THEN** ist spätestens nach einer Minute ein Neustart für `winter` vorgemerkt, und die Meldung erscheint einmal

#### Scenario: Saisonende erreicht
- **WHEN** die Lobby in der Welt von `winter` läuft und das Fenster endet
- **THEN** ist spätestens nach einer Minute ein Neustart vorgemerkt, mit der Standardwelt als Ziel

#### Scenario: Vormerkung wird aufgehoben
- **WHEN** ein Neustart vorgemerkt ist und die gewünschte Welt wieder der gestarteten entspricht, etwa weil der Abschalter zurückgesetzt wurde
- **THEN** ist kein Neustart mehr vorgemerkt, und die Lobby stoppt nicht

#### Scenario: Kein Neustart-Kreislauf
- **WHEN** die Lobby nach einem Neustart in der gewünschten Welt läuft
- **THEN** ist kein Neustart vorgemerkt

#### Scenario: Meldung nicht wiederholt
- **WHEN** ein Neustart vorgemerkt bleibt und die Minuten-Prüfung mehrfach läuft
- **THEN** wird die Vormerkung nicht erneut geloggt

### Requirement: Die Lobby stoppt nur, wenn sie leer ist
Ist ein Neustart vorgemerkt und sind keine Spieler online, MUSS die Lobby sauber stoppen, damit der Dienst-Supervisor sie neu startet. Die Prüfung MUSS im Minutentakt und nach jedem Verlassen eines Spielers laufen, wobei der ausscheidende Spieler nicht mehr mitzählt. Die Lobby DARF NICHT stoppen, solange ein Spieler online ist. Es gibt keine Obergrenze für das Warten.

#### Scenario: Leere Lobby im Minutentakt
- **WHEN** ein Neustart vorgemerkt ist und beim Minutentakt kein Spieler online ist
- **THEN** stoppt die Lobby sauber

#### Scenario: Letzter Spieler geht
- **WHEN** ein Neustart vorgemerkt ist und der letzte Spieler die Lobby verlässt
- **THEN** stoppt die Lobby, ohne den nächsten Minutentakt abzuwarten

#### Scenario: Spieler online
- **WHEN** ein Neustart vorgemerkt ist und mindestens ein Spieler online ist
- **THEN** stoppt die Lobby nicht, auch nicht nach vielen Minutentakten

#### Scenario: Ein Spieler geht, andere bleiben
- **WHEN** ein Neustart vorgemerkt ist und einer von zwei Spielern die Lobby verlässt
- **THEN** stoppt die Lobby nicht

#### Scenario: Nichts vorgemerkt
- **WHEN** kein Neustart vorgemerkt ist und die Lobby leer ist
- **THEN** stoppt die Lobby nicht

### Requirement: Überlappende Fenster ergeben genau eine Welt
Überlappen sich die Fenster aktiver Saisons, MUSS die Saison mit dem früheren `from` gelten; bei gleichem `from` die mit der alphabetisch kleineren Id. Die Lobby MUSS die Überlappung beim Start als Warnung mit beiden Ids loggen und darf deswegen nicht abbrechen.

#### Scenario: Zwei Fenster überlappen
- **WHEN** `autumn` ab 1. Oktober und `halloween` ab 25. Oktober gelten und beide am 28. Oktober aktiv wären
- **THEN** ist `autumn` die gewünschte Saison, und beim Start steht eine Warnung mit `autumn` und `halloween` im Log

### Requirement: Ungültige Saison-Konfiguration verhindert den Start
Ist eine aktivierte Saison ungültig, MUSS die Lobby den Start abbrechen, und die Fehlermeldung MUSS den vollständigen Schlüssel (`seasons.<id>.<feld>`) und den Grund nennen. Ungültig sind ein fehlender `world`-, `from`- oder `to`-Wert, ein Datum, das sich nicht lesen lässt, `from` nicht vor `to`, ein Weltverzeichnis, das unter `worlds/` fehlt, und ein Weltverzeichnis ohne Map-Datei. Auch eine ungültige `seasons.zone` MUSS den Start abbrechen. Eine abgeschaltete Saison DARF den Start nicht verhindern. Ein zur Laufzeit ungültig gewordener Wert DARF weder einen Neustart auslösen noch die Lobby stoppen; er MUSS als Warnung geloggt werden, und die Saison zählt bis zur Korrektur als nicht aktiv.

#### Scenario: Welt existiert nicht
- **WHEN** `seasons.winter.world` auf `winter` zeigt und `worlds/winter` fehlt
- **THEN** startet die Lobby nicht und nennt `seasons.winter.world` und den Grund

#### Scenario: Welt ohne Map-Datei
- **WHEN** `worlds/winter` existiert, enthält aber keine Map-Datei
- **THEN** startet die Lobby nicht und nennt `seasons.winter.world` und den Grund

#### Scenario: Datum nicht lesbar
- **WHEN** `seasons.winter.from` den Wert `morgen` hat
- **THEN** startet die Lobby nicht und nennt `seasons.winter.from`

#### Scenario: Fenster verkehrt herum
- **WHEN** `seasons.winter.from` nicht vor `seasons.winter.to` liegt
- **THEN** startet die Lobby nicht und nennt `seasons.winter.from` und `seasons.winter.to`

#### Scenario: Abgeschaltete Saison mit fehlender Welt
- **WHEN** `seasons.winter.enabled` `false` ist und `worlds/winter` fehlt
- **THEN** startet die Lobby normal

#### Scenario: Live aktivierte Saison ohne Welt
- **WHEN** die Lobby läuft und `seasons.winter.enabled` im aktiven Fenster auf `true` gesetzt wird, obwohl `worlds/winter` fehlt
- **THEN** merkt die Lobby keinen Neustart vor, loggt eine Warnung mit `seasons.winter.world` und läuft weiter
