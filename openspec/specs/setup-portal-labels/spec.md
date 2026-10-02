# setup-portal-labels Specification

## Purpose

Legt fest, wie das Build-Team im Setup-Server das Label eines Lobby-Portals mit `/setup portal <id> label ...` anlegt, ändert, entfernt und in der Vorschau sieht und wie nur gültige Labels in die Map-Datei gelangen.

## Requirements

### Requirement: Das Label eines Portals wird mit label-Befehlen bearbeitet
Der Setup-Server MUSS für ein Portal die Befehle `/setup portal <id> label here`, `label text <minimessage…>`, `label offline <minimessage…>`, `label source <type> [name]` und `label remove` anbieten. `label here` MUSS die aktuelle Position des Spielers als Anker des Labels setzen. `label text` und `label offline` MÜSSEN den Rest der Eingabe, auch mit Leerzeichen, als MiniMessage-Text übernehmen. `label source` MUSS einen der Typen `task`, `group`, `service` oder `local` annehmen; `task`, `group` und `service` brauchen einen Namen, `local` nicht. Jeder dieser Befehle MUSS nur den Entwurf des Spielers ändern und mit dem Entwurfsstand antworten, wie die übrigen Bearbeitungsbefehle. Ein Label MUSS Position und Text haben, um vollständig zu sein.

#### Scenario: Anker setzen
- **WHEN** ein Spieler an Position (12.5, 66.0, -3.5) `/setup portal survival label here` ausführt
- **THEN** merkt der Entwurf diese Position als Anker, nichts wird gespeichert und der Chat nennt den Stand

#### Scenario: Text mit Leerzeichen
- **WHEN** ein Spieler `/setup portal survival label text <gold>Survival <gray><online>/<max>` ausführt
- **THEN** merkt der Entwurf den vollständigen Text samt Leerzeichen

#### Scenario: Offline-Text setzen
- **WHEN** ein Spieler `/setup portal survival label offline <red>Survival startet gleich` ausführt
- **THEN** merkt der Entwurf diesen Offline-Text

#### Scenario: Quelle setzen
- **WHEN** ein Spieler `/setup portal survival label source group Games` ausführt
- **THEN** merkt der Entwurf die Quelle `group` mit Name „Games“

#### Scenario: Lokale Quelle ohne Namen
- **WHEN** ein Spieler `/setup portal survival label source local` ausführt
- **THEN** merkt der Entwurf die Quelle `local`, ohne einen Namen zu verlangen

#### Scenario: Quelle ohne Namen
- **WHEN** ein Spieler `/setup portal survival label source task` ohne Namen ausführt
- **THEN** lehnt der Chat den Befehl ab und der Entwurf bleibt unverändert

#### Scenario: Unbekannter Quellentyp
- **WHEN** ein Spieler `/setup portal survival label source proxy x` ausführt
- **THEN** lehnt der Chat den Typ ab und nennt die erlaubten Typen

#### Scenario: Label ohne Position ist unvollständig
- **WHEN** der Entwurf einen Text, aber keinen Anker hat
- **THEN** meldet der Chat, dass die Position des Labels fehlt

#### Scenario: Label entfernen
- **WHEN** ein Spieler `/setup portal survival label remove` ausführt und `save` folgt
- **THEN** steht das Portal danach ohne `label` in der Map-Datei

#### Scenario: Konsole
- **WHEN** die Konsole `/setup portal survival label here` ausführt
- **THEN** wird der Befehl wie alle `/setup`-Befehle abgelehnt

### Requirement: Ein Label wird nur mit save gespeichert und nur, wenn es gültig ist
Das Label MUSS wie die übrigen Portaldaten ausschließlich durch `/setup portal <id> save` in die Map-Datei geschrieben werden. `save` MUSS dieselben Regeln prüfen, die die Lobby beim Start prüft (MiniMessage, Quellentyp, Name, Position, Ausrichtung). Ist das Label ungültig oder unvollständig, MUSS der Chat die Gründe nennen, der Entwurf offen bleiben und die Map-Datei unverändert. Ein Portal ohne Label MUSS weiter ohne Label speicherbar sein. Texte, die ein Spieler als Label-Text eingibt, DÜRFEN in Rückmeldungen nicht als Markup der Rückmeldung ausgewertet werden.

#### Scenario: save mit gültigem Label
- **WHEN** der Entwurf Anker und Text hat und gültig ist und der Spieler `save` ausführt
- **THEN** steht das Portal samt `label` in der Map-Datei der Welt

#### Scenario: save mit ungültigem MiniMessage
- **WHEN** der Text des Entwurfs `<gold>Survival</red>` ist und der Spieler `save` ausführt
- **THEN** nennt der Chat den Grund, der Entwurf bleibt offen und die Map-Datei bleibt unverändert

#### Scenario: Änderung am Label eines gespeicherten Portals
- **WHEN** ein gespeichertes Portal ein Label hat und der Spieler `label text <red>Neu` ausführt
- **THEN** steht der neue Text erst nach `save` in der Map-Datei

#### Scenario: Portal ohne Label
- **WHEN** ein Portal ohne Label gespeichert wird
- **THEN** schreibt die Map-Datei keinen Block `label` für dieses Portal

#### Scenario: Tags im Text werden in Rückmeldungen nicht ausgeführt
- **WHEN** der Spieler `label text <click:run_command:/op x>Hi` ausführt
- **THEN** zeigt die Rückmeldung den Text wörtlich an und führt keinen Klick-Befehl aus

### Requirement: Die Vorschau markiert den Anker des Labels
Die Partikel-Vorschau eines Portals (`show` und die Live-Vorschau des Entwurfs) MUSS zusätzlich den Anker des Labels markieren, sofern einer gesetzt ist, und nur dem ausführenden Spieler. Ohne Anker ändert sich die Vorschau nicht.

#### Scenario: Anker sichtbar
- **WHEN** ein Portal einen Label-Anker hat und der Spieler `/setup portal show` ausführt
- **THEN** erhält nur er zusätzlich Partikel am Anker

#### Scenario: Kein Anker
- **WHEN** ein Portal kein Label hat
- **THEN** enthält die Vorschau nur den Umriss wie bisher

#### Scenario: Entwurfsanker folgt dem Entwurf
- **WHEN** der Entwurf einen Anker hat und der Spieler ihn mit `label here` verschiebt
- **THEN** markiert die Live-Vorschau den neuen Anker

### Requirement: Text-Vorschau des Labels
Der Setup-Server MUSS das Label eines Entwurfs, sobald es Position und Text hat, als echte `TextDisplay`-Entität am Anker anzeigen, und nur dem Bearbeiter. Der angezeigte Text MUSS derselbe sein, den die Lobby aus Text, Platzhaltern und Offline-Text bildet (gemeinsame Rendering-Funktion), mit festen Beispielwerten: `<online>` = 12, `<max>` = 50, `<task>` = Task des Entwurfs (ohne Task die Id des Portals), `<prefix>` wie im Chat. Die Vorschau MUSS jeder Änderung des Entwurfs folgen und dieselbe Entität aktualisieren, nie eine zweite anlegen. `/setup portal <id> label preview offline|online` MUSS zwischen der Offline-Variante (`offlineText`, sonst `text` mit 0/0) und der Online-Variante wechseln; Standard ist online. Die Vorschau MUSS verschwinden, wenn der Entwurf endet (`save`, `cancel`, `remove`), wenn das Label entfernt wird wenn der Spieler die Verbindung trennt und wenn er die Instanz wechselt. Ist der Text nach den Regeln des Validators ungültig, MUSS die Vorschau den zuletzt gültigen Text behalten (oder nichts neu anzeigen, wenn es noch keinen gab), und die Antwort des Befehls MUSS das Problem nennen.

#### Scenario: Vorschau erscheint
- **WHEN** der Entwurf Anker und Text `<gold><task> <gray><online>/<max>` hat und der Task „Survival“ ist
- **THEN** zeigt eine echte `TextDisplay` am Anker „Survival 12/50“, mit aufgelöstem `<prefix>`, wenn der Text es enthält

#### Scenario: Vorschau ohne Task
- **WHEN** der Entwurf Anker und Text mit `<task>` hat, aber keinen Task
- **THEN** zeigt die Vorschau anstelle von `<task>` die Id des Portals

#### Scenario: Vorschau folgt Änderungen
- **WHEN** der Spieler `label here`, `label text`, `label offline`, `label source` ausführt oder das Billboard eines gespeicherten Labels der Karte gilt
- **THEN** ändern sich Position, Text oder Billboard derselben Entität, und es existiert danach genau eine Vorschau-Entität für dieses Portal

#### Scenario: Offline-Variante
- **WHEN** der Spieler `/setup portal survival label preview offline` ausführt
- **THEN** zeigt die Vorschau den Offline-Text, oder den Text mit 0/0, wenn es keinen gibt

#### Scenario: Zurück zur Online-Variante
- **WHEN** der Spieler danach `/setup portal survival label preview online` ausführt
- **THEN** zeigt die Vorschau wieder den Text mit 12/50

#### Scenario: Nur für den Bearbeiter sichtbar
- **WHEN** ein zweiter Spieler in derselben Welt ist, während die Vorschau läuft
- **THEN** erhält nur der Bearbeiter die Entität, der andere nicht

#### Scenario: Vorschau verschwindet beim Speichern, Verwerfen und Entfernen
- **WHEN** der Spieler `save`, `cancel` oder `remove` für das Portal ausführt
- **THEN** ist die Vorschau-Entität entfernt

#### Scenario: Vorschau verschwindet mit dem Label
- **WHEN** der Spieler `label remove` ausführt
- **THEN** ist die Vorschau-Entität entfernt

#### Scenario: Vorschau verschwindet beim Trennen
- **WHEN** der Bearbeiter die Verbindung trennt
- **THEN** ist die Vorschau-Entität entfernt

#### Scenario: Vorschau verschwindet beim Instanzwechsel
- **WHEN** der Bearbeiter in eine andere Instanz wechselt
- **THEN** ist die Vorschau-Entität in der alten Instanz entfernt

#### Scenario: Ungültiger Text
- **WHEN** der Spieler `label text <gold>Survival</red>` ausführt, während die Vorschau „Survival 12/50“ zeigt
- **THEN** bleibt der letzte gültige Text sichtbar, nichts stürzt ab, und die Antwort nennt das Problem (unbekannter oder falsch geschlossener Tag)
