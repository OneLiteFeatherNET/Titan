# Proposal

## Why

Die Serverauswahl eines Minispiels (ElytraRace, Slender) zeigt höchstens 54 Dienste, nach Namen sortiert. Alle weiteren werden still abgeschnitten (`ServerPicker.entries`, `limit(COLUMNS * MAX_ROWS)`). Bei vielen Instanzen sieht der Spieler deshalb nicht alle Lobbys, und er kann nicht nach Auslastung oder freien Plätzen suchen. Die Lobbyswitcher-Liste hat das Seitenschema mit Navigationszeile bereits; die Serverauswahl soll es übernehmen, damit beide Listen gleich bedient werden.

## What Changes

- Die Serverauswahl blättert durch alle laufenden Dienste. Eine Seite hat 45 Einträge in fünf Reihen; die sechste Reihe ist die Navigationszeile mit Vorige-Pfeil, Sortier-Item und Nächste-Pfeil. Die Seite steht als „Seite x/y“ in der Lore des Sortier-Items und in den Pfeilnamen, ohne eigenes Item. Ein Pfeil erscheint nur, wenn es in seine Richtung eine Seite gibt. Eine nicht volle Reihe ist zentriert. Kein Dienst wird mehr verworfen.
- Die Navigationszeile hat zusätzlich ein Sortier-Item in ihrer Mitte (Navigationsstart + 4). Jeder Klick schaltet die Ordnung weiter: Name (A–Z, Standard), Meiste Spieler (absteigend, Gleichstand nach Name), Freie Plätze (meiste freie Plätze zuerst, Dienste ohne gemeldetes Limit zuletzt, Gleichstand nach Name), Status (beitretbar, dann nicht bereit, dann voll; Gleichstand nach Name).
- Die Lore des Sortier-Items listet alle vier Ordnungen und markiert die aktive. Ein Wechsel der Ordnung springt auf Seite 1.
- Ordnung und Seite gelten je Spieler, solange die Liste offen ist. Die Lobby speichert dafür nichts je Spieler: der Zustand ist das Inventar, das der Spieler offen hat. Ein erneutes Öffnen beginnt mit Name und Seite 1.
- Die Liste liest die Dienste beim Öffnen, bei jedem Seiten- und Sortierwechsel sowie beim Klick auf einen Eintrag neu. Sie aktualisiert sich nicht von selbst, wie bisher. Eine geteilte Ansicht wird neu gezeichnet, wenn ein anderer Spieler dieselbe Ansicht öffnet.
- Die Inventare bleiben geteilt: je Ziel, Sprache, Ordnung, Seite und Zeilenzahl ein Inventar, beim ersten Bedarf angelegt und beim Stoppen entfernt. Die Zahl ist nach oben durch die Seitenzahl begrenzt (etwa 20 unseitige Varianten plus 4 je Seite einer langen Liste), nicht durch die Anzahl der Öffnungen (Design: `design.md`).
- Die reine Seitenmathematik (45 Einträge je Seite, Ausschnitt einer Seite, Slots der Navigationszeile) wandert nach `core` und wird von Serverauswahl und Lobbyswitcher gemeinsam genutzt. Das Verhalten des Lobbyswitchers ändert sich nicht.
- Neue Texte für das Sortier-Item, die vier Ordnungen, Pfeile und Seitenanzeige in `messages_en.properties` und `messages_de.properties` des Navigators.

Nicht Teil dieser Änderung:

- Der Zustand „ingame“ für Dienste. Er kommt mit `navigator-ingame-servers` und hängt sich in der Ordnung Status hinter „voll“ an. Die Status-Ordnung wird so gebaut, dass dieser Zustand ohne Umbau dazukommt.
- Suche, Filter, periodische Aktualisierung der offenen Liste.
- Telemetrie für Sortier- und Seitenklicks (Span oder Zähler).
- Änderungen an Schnellbeitritt, Klickzuordnung, Zuständen je Eintrag und Ablehnungen.

## Delivery

Der Pull-Request-Titel und der Squash-Commit sind `feat(navigator): page and sort the server picker`. Die Änderung ist nicht breaking. Sie ist ein einziger Typ `feat`. Die Extraktion der Seitenmathematik nach `core` (Lobbyswitcher-Umstellung) ist ein eigener `refactor(core)`-Commit im selben PR, weil ein Commit nur einen Typ trägt. Dokumentation und OpenSpec-Änderungen haben eigene `docs`-Commits.

## Capabilities

### New Capabilities

Keine. Blättern und Sortieren sind Verhalten des Navigators und gehören in die bestehende Capability.

### Modified Capabilities

- `lobby-navigator`: neue Anforderungen für Seiten, Sortierung und Neulesen der Serverauswahl; geändert werden „Menü-Varianten bleiben acht geteilte Inventare“ (Serverauswahl-Inventare je Ordnung und Seite) und „Nur benannte Klicks wirken“ (Sortier- und Seitenfeld).

## Impact

- **Code:**
  - `features/navigator`: `ServerPicker` (ohne Obergrenze, Sortierung, Navigationszeile), `SharedServerPicker` (Inventare je Ansicht), `NavigatorModule` (Lesen und Entscheidung für Sortier- und Seitenklicks, nach dem Muster `readServices`), neue Klassen für Sortierung und Layout, `NavigatorMessages` (neue Schlüssel).
  - `core`: neue reine Klasse für Seitenmathematik (`core.paging`), ohne Minestom.
  - `features/lobbyswitcher`: `SwitcherLayout` nutzt die gemeinsame Seitenmathematik; Verhalten und Tests bleiben unverändert.
- **Abhängigkeiten:** keine neuen Bibliotheken. Aves 1.16.7 bleibt bei `GlobalInventoryBuilder`; `PageableInventory` wird nicht genutzt (Begründung in `design.md`).
- **Nutzertexte:** neu, in Englisch und Deutsch: Sortier-Item mit Name und Lore, vier Ordnungsnamen, „Vorige“, „Nächste“, „Seite x/y“ und der Klicktext. Bestehende Texte bleiben unverändert.
- **Konfiguration:** keine.
- **Spielerverhalten:** die Serverauswahl zeigt mehr als 54 Dienste; die Navigationszeile ist bei jeder Größe sichtbar, auch ohne Seiten. Die Zahl der Zeilen wächst um eine, weil die Navigationszeile immer da ist.
