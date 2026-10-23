# Proposal

## Why

Zu Halloween soll die Lobby das Minispiel Slender zeigen. Heute gibt es in der Lobby keinen Weg dorthin außer dem Navigator. Spieler sollen durch einen Gang auf Slender zulaufen, dabei Hinweise finden und an einem NPC direkt in das Minispiel wechseln. Die Kartendaten einer Welt (`map.json`) sind der einzige Ort für Position und Inhalt. Ein neuer Halloween-Effekt darf deshalb keinen Code nur für Halloween brauchen.

## What Changes

- Neue Column `features/npc` mit zwei generischen Modulen, beide aus `map.json` gesteuert:
  - **Join-NPCs**: optionales Feld `npcs` in `map.json`. Jeder NPC ist eine Mannequin-Figur mit fester Position, optionaler Skin-Textur, optionalem Schild (Text über dem Kopf), optionalem Recht und optionalem Blick zum nächsten Spieler. Ein Rechts- oder Linksklick schickt den Spieler in die CloudNet-Aufgabe des NPCs, nach derselben Auswahl wie der Schnellbeitritt des Navigators.
  - **Lebendige Zonen**: optionales Feld `ambience` in `map.json`. Jede Zone ist ein achsenparalleler Quader mit Partikeln, Umgebungsgeräuschen, Texttafeln, schwebenden Seiten und einer Erscheinung. Die Erscheinung ist eine Slender-Silhouette, die nur dem betroffenen Spieler gezeigt wird.
- Die gemeinsame Auswahl „welcher Dienst wird betreten“ (`TaskJoin`) wandert von `features/navigator` nach `core`. Der Navigator nutzt sie weiter, so dass beide Wege dieselbe Regel haben.
- Die Map-Daten werden in `common` gelesen und in `runtime` als Ports (`LobbyNpcs`, `LobbyAmbiences`) bereitgestellt. Der Setup-Editor übernimmt die neuen Felder beim Speichern.
- Beispiel für `halloween_lobbyleyna` in `docs/halloween-slender-showcase.md`, zum Einfügen in die Live-`map.json` nach Prüfung. Die Live-Karte wird in dieser Änderung nicht verändert.

Nicht Teil dieser Änderung:

- Ein Slender-Skin. In der OneLiteFeatherNET-Organisation liegt keine Skin-Textur des Minispiels (Cygnus nutzt Spielerskins zur Laufzeit). Der Eintrag `skin` bleibt optional; der NPC zeigt dann den Standardskin.
- Skin-Auflösung per UUID über Mojang. Der Jumprun-Kopf-Lookup liegt in der Jumprun-Column und ist nicht in `core`.
- Ein Zustand „ingame“ für Dienste. Er kommt mit `navigator-ingame-servers`; bis dahin gilt die Regel des Navigators: angekündigtes Limit und nicht voll.
- Ressourcenpakete. Alles muss mit dem Vanilla-Client funktionieren (siehe Design).

## Delivery

Der Pull-Request-Titel und der Squash-Commit sind `feat(npc): add join npcs and a slender showcase for the halloween lobby`. Die Änderung ist nicht breaking. Die OpenSpec-Artefakte stehen in `docs(openspec): propose halloween-slender-showcase`, die Verschiebung der Auswahl in `refactor(core): move the task join selection to core`, die Umsetzung in `feat(npc): ...`.

## Capabilities

### New Capabilities

- `npc-join`: Join-NPCs aus `map.json`, Beitritt per Klick, Recht und Abklingzeit.
- `lobby-ambience`: Lebendige Zonen aus `map.json`: Partikel, Geräusche, Texttafeln, schwebende Seiten und Erscheinungen.

### Modified Capabilities

Keine. Die Kartendaten erweitern sich um zwei optionale Felder, bestehende Felder und Verhalten bleiben gleich.

## Impact

- **Code:**
  - `core`: `core.npc` (`LobbyNpc`, `NpcSkin`, `LobbyNpcs`, `NpcValidator`), `core.ambience` (`AmbienceZone`, `LobbyAmbiences`, `AmbienceValidator`), `core.portal.TaskJoin` (verschoben aus dem Navigator).
  - `common`: `LobbyMap` und `LobbyMapBuilder` mit den Feldern, Gson-Adapter für `npcs` und `ambience`, die Label-Lesung des Portal-Adapters wird wiederverwendet.
  - `runtime`: `PlatformBeans` liefert `LobbyNpcs` und `LobbyAmbiences` aus der aktiven Welt.
  - `features/navigator`: nutzt `TaskJoin` aus `core`; das Verhalten bleibt gleich.
  - `features/npc`: neue Column, mit `package-info.java`, `NpcModule`, `AmbienceModule` und Telemetrie.
- **Abhängigkeiten:** keine neuen Bibliotheken.
- **Nutzertexte:** Übersetzungen `de` und `en` für Beitrittsmeldungen und Schilder.
- **Betrieb:** Die Live-Karte `halloween_lobbyleyna` bekommt die Felder aus `docs/halloween-slender-showcase.md`; dafür ist kein Neustart der Welt nötig, nur der Lobby.
