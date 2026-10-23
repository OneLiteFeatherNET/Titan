# Spec Delta

## ADDED Requirements

### Requirement: Join-NPCs stehen in der Kartendatei
_Join NPCs are map data: an optional `npcs` list in `map.json`, read at start._

Die Datei `map.json` einer Welt MAY ein Feld `npcs` tragen. Jeder Eintrag MUSS `id`, `task` und `position` (`x`, `y`, `z`, `yaw`, `pitch`) haben. Optional sind `skin` (`value` und `signature`), `label` (wie bei Portalen), `lookAtPlayers` (Standard `false`) und `permission` (Standard: keines). Ein Eintrag mit fehlendem Pflichtfeld, falschem Typ oder ungültigem Schild wird beim Start mit einer WARN-Zeile übersprungen, die Welt und Grund nennt; die übrigen NPCs laufen weiter.

#### Scenario: Gültiger NPC
- **WHEN** `map.json` einen NPC mit `id`, `task`, `position` und `skin` enthält
- **THEN** steht der NPC beim Start an dieser Position mit diesem Profil

#### Scenario: Pflichtfeld fehlt
- **WHEN** ein NPC kein `task`-Feld hat
- **THEN** wird er übersprungen, eine WARN-Zeile nennt `npc 'id'` und `task`, und die übrigen NPCs starten

#### Scenario: Ungültiges Schild
- **WHEN** das Schild eines NPCs einen unbekannten Tag wie `<gold` enthält
- **THEN** wird der NPC übersprungen und die WARN-Zeile nennt das Schild

### Requirement: Der NPC ist Mannequin-Figur ohne Bewegung
_The NPC is a Mannequin, immovable, without gravity and not damageable._

Ein NPC MUSS als Mannequin-Figur erscheinen, unbeweglich, ohne Schwerkraft und unverwundbar. Hat er einen Skin, trägt das Profil die Textur mit Signatur; ohne Skin zeigt er die Standardfigur. Der Skin ist eine Spielerskin-Textur, kein Ressourcenpaket; eigene Modelle, Schriften oder Geräusche sind nicht zulässig.

#### Scenario: NPC ohne Skin
- **WHEN** ein gültiger NPC ohne `skin` startet
- **THEN** steht die Standardfigur an der Position, unverwundbar und unbeweglich

#### Scenario: NPC wird nicht bewegt
- **WHEN** nach dem Start 100 Ticks vergehen
- **THEN** steht der NPC noch an derselben Position

### Requirement: Klick schickt den Spieler in die Aufgabe
_A right or left click joins the task, as the navigator's quick join does._

Ein Rechts- oder Linksklick auf einen NPC MUSS den Spieler in die CloudNet-Aufgabe des NPCs schicken. Die Auswahl folgt dem Schnellbeitritt des Navigators: Ein fertiger, nicht voller Dienst mit den meisten Spielern wird betreten; gibt es keinen, bekommt der Spieler eine Meldung, dass gerade kein Dienst bereit ist. Ist die Dienstliste nicht lesbar, bekommt der Spieler eine Meldung zur Nichtverfügbarkeit. Ein NPC mit `permission` wirft ohne dieses Recht den Klick ab und zeigt eine Meldung.

#### Scenario: Fertiger Dienst
- **WHEN** ein Spieler mit Recht auf den NPC `Slender` klickt und `Slender-1` mit angekündigtem Limit und nicht voll läuft
- **THEN** wird der Spieler zu `Slender-1` geschickt

#### Scenario: Kein fertiger Dienst
- **WHEN** die Aufgabe nur einen vollen Dienst hat
- **THEN** bleibt der Spieler auf der Lobby und bekommt eine Meldung, dass kein Dienst bereit ist

#### Scenario: Ohne Recht
- **WHEN** ein Spieler ohne `titan.navigator.slender` auf den NPC klickt
- **THEN** geschieht nichts außer einer Meldung, und der Beitritt wird nicht versucht

### Requirement: Abklingzeit je Spieler
_A second click within the cooldown is ignored, with an injected clock._

Pro Spieler MUSS zwischen zwei Beitritten eine Abklingzeit liegen. Ein Klick innerhalb der Abklingzeit löst keinen Beitritt aus. Die Zeit kommt aus einem injizierten `Clock`, nicht aus der Systemzeit.

#### Scenario: Zweiter Klick zu früh
- **WHEN** ein Spieler zweimal innerhalb der Abklingzeit klickt
- **THEN** wird nur der erste Klick zu einem Beitritt, der zweite zählt als `cooldown`

### Requirement: Klicks werden gezählt
_Each interaction is counted by NPC and result._

Jeder Klick MUSS den Zähler `titan.npc.interactions` mit dem Attribut `npc` und dem Ergebnis erhöhen (`joined`, `cooldown`, `unavailable`, `none_ready`, `denied`).

#### Scenario: Zähler bei Beitritt
- **WHEN** ein Klick zu einem Beitritt führt
- **THEN** zählt `titan.npc.interactions` mit `npc` und `result=joined` um eins

### Requirement: Der NPC kann den nächsten Spieler anschauen
_With lookAtPlayers, the head follows the nearest player within 8 blocks._

Ist `lookAtPlayers` gesetzt, dreht sich der NPC zum nächsten Spieler im Umkreis von 8 Blöcken; ohne Spieler bleibt er stehen.

#### Scenario: Spieler nähert sich
- **WHEN** ein Spieler in 5 Blöcken Abstand steht und `lookAtPlayers` gesetzt ist
- **THEN** zeigt der Kopf des NPCs zu ihm
