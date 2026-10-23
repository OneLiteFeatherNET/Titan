# Design

## Context

Motivation und Anforderungen stehen in `proposal.md` und `specs/lobby-navigator/spec.md`. Ausgangslage im Code (nach `navigator-picker-pages`):

- `PickerLayout` legt die Navigationszeile auf den Startslot `(Zeilen − 1) × 9` mit Vorige-Pfeil (Start + 0), Sortier-Item (Start + 4) und Nächste-Pfeil (Start + 8).
- `SharedServerPicker.layout(...)` setzt die Schaltflächen über `button(...)`: jede Schaltfläche cancelt den Klick und ruft ihren Callback nur bei einem benannten Klick (`ClickAction.isNamed`).
- `SharedServerPicker.Clicks` ist die Schnittstelle, mit der `NavigatorModule.PickerClicks` die Klicks der Liste beantwortet (`pick`, `sort`, `turn`).
- `NavigatorModule.open(Player)` ist der einzige Weg, den Navigator zu öffnen. Er liest die Rechte über `grantedBy(player)`, wählt das Menü `Menu(granted, languageOf(player))` und ruft `navigatorTelemetry.open(...)` auf.

## Goals / Non-Goals

**Goals:**
- Eine Zurück-Taste auf Navigationsstart + 0, auf jeder Seite und bei leerer Liste.
- Die Seitenpfeile auf Start + 3 und Start + 5, das Sortier-Item bleibt auf Start + 4.
- Der Klick öffnet dasselbe Navigator-Menü wie das Feder-Item, mit den Rechten im Moment des Klicks.

**Non-Goals:**
- Kein neuer Zustand je Spieler: der Navigator wird neu gewählt, die Serverauswahl bleibt ein geteiltes Inventar.
- Keine Änderung an der Auswahl der Menüs; die Logik bleibt in `NavigatorModule.open`.
- Keine Telemetrie für die Zurück-Taste über die bestehende Navigator-Telemetrie hinaus.

## Decisions

### D1 Der Klick ruft `open(player)` auf, kein eigener Pfad

- `SharedServerPicker.Clicks` bekommt `void back(Player player)`. `NavigatorModule.PickerClicks.back` ruft `NavigatorModule.this.open(player)`.
- **Warum:** `open` liest die Rechte (`grantedBy`), wählt das Menü nach Sprache und zählt die Öffnung in der Telemetrie. Ein zweiter Weg würde die Auswahl der Menüs verdoppeln, und die Rechte-Prüfung könnte auseinanderlaufen.
- **Rechte:** Die Rechte werden beim Klick gelesen, nicht beim Öffnen der Serverauswahl. Ein entzogenes Recht führt deshalb zum Menü ohne den Eintrag (Szenario „Entzogenes Recht vor dem Zurück“).
- **Telemetrie:** Die Öffnung über die Zurück-Taste läuft durch `navigatorTelemetry.open` wie jede andere Öffnung des Navigators.
- **Keine Leerlauf-Lesung:** Die Zurück-Taste liest die Dienste nicht neu; sie wechselt nur das Inventar.
- **Alternativen:** Ein Menü direkt aus dem Picker-Schlüssel (`Destination`, Sprache) bauen: verworfen, es würde die Rechte-Auswahl duplizieren, die die Anforderung ausdrücklich verbietet.

### D2 Layout: Zurück auf Start + 0, Pfeile auf Start + 3 und + 5

- `PickerLayout` bekommt `backSlot()` (Start + 0). `previousSlot()` wird Start + 3, `nextSlot()` wird Start + 5, `sortSlot()` bleibt Start + 4.
- Die Zurück-Taste wird in `SharedServerPicker.layout` immer gesetzt, auch bei leerer Liste und ohne Seiten; die Pfeile bleiben an `previousShown`/`nextShown` gebunden.
- Zu den Slots: Eine Seite mit sechs Reihen hat Zurück in 45, Vorige in 48, Sortier in 49, Nächste in 50. Eine Liste mit einer Inhaltsreihe hat Zurück in 9, Sortier in 13.
- **Warum:** Die Anforderung nennt Start + 0 für die Zurück-Taste und die Pfeile neben dem Sortier-Item; die Mitte bleibt unverändert, damit die Lore-Tests des Sortier-Items nicht wandern.
- **Zentrierung:** unberührt. `serviceSlot` nutzt nur die Einträge, nicht die Navigationszeile.

### D3 Material und Text

- Material `Material.DARK_OAK_DOOR`: ein Ausgang, unterscheidbar vom Pfeil (`Material.ARROW`) und vom Sortier-Item (`COMPARATOR`). Die Schaltfläche hat keine Lore; die Zeile „Zurück zum Navigator“ ist ihr Name.
- Neuer Schlüssel `titan.navigator.picker.back` in `NavigatorMessages` (in `KEYS`), in `messages_en.properties` („Back to the navigator“) und `messages_de.properties` („Zurück zum Navigator“). Der Stil entspricht den übrigen Schlüsseln der Serverliste ohne Platzhalter.

### D4 Keine Listener und kein Zustand je Spieler

- Die Zurück-Taste nutzt die bestehende Schaltflächen-Logik von `button`, die in den geteilten Ansichten liegt. Es entsteht keine Ansicht und kein Listener, den das Öffnen nicht schon anlegt.
- `open` löscht nichts und merkt sich nichts; das Serverauswahl-Inventar bleibt bei `stop()` wie bisher registriert und wird entfernt.
- **Test:** `NavigatorPickerLifecycleTest`: 20 Mal Serverauswahl öffnen und über die Zurück-Taste zurück (Navigator neu öffnen) ändert weder die Listener am Modul-Knoten noch an der Serverauswahl.

### D5 Änderungen an `navigator-picker-pages`

Die Anforderung „Serverauswahl blättert durch alle Dienste“ (Positionen der Navigationszeile) existiert nur im unarchivierten Change `navigator-picker-pages`, nicht in `openspec/specs/lobby-navigator/spec.md`. Ein MODIFIED-Eintrag dafür hier würde `openspec validate --strict` abweisen. Deshalb steht die neue Position dort, und dieser Change ändert nur, was in der Hauptspezifikation schon steht:

- `navigator-picker-pages/specs/lobby-navigator/spec.md`: „Serverauswahl blättert durch alle Dienste“ nennt Zurück auf Start + 0, Vorige auf Start + 3, Nächste auf Start + 5 und die Slots 45/48/49/50 in der Seitenszene; „Nur benannte Klicks wirken“ nennt die Zurück-Taste und das Öffnen des Navigators. Die Fassung von „Nur benannte Klicks wirken“ ist in beiden Changes wortgleich, damit die Archivreihenfolge egal ist.
- `navigator-picker-pages/proposal.md`, `design.md` (D6, Test) und `tasks.md` (2.3) sind auf die Slots 45/48/49/50 umgestellt.
- **Reihenfolge beim Archivieren:** `navigator-picker-pages` zuerst. Die Anforderung „Zurück-Taste führt in den Navigator“ setzt die Positionen aus dessen Spezifikation voraus.

### D6 Telemetrie

Keine neuen Spans oder Zähler. Die Öffnung über die Zurück-Taste ist eine Navigator-Öffnung wie jede andere; das Blättern und Sortieren bleiben ohne Telemetrie (wie in `navigator-picker-pages`).

## Risks / Trade-offs

- **[Sichtbare Verschiebung]** → Die Pfeile liegen jetzt an Start + 3 und + 5. Spieler, die die alten Plätze kennen, klicken ins Leere, bis die Zurück-Taste gelernt ist. Die Anforderung verlangt das Layout ausdrücklich.
- **[Zwei offene Changes ändern dieselben Anforderungen]** → Gelöst durch die Reihenfolge beim Archivieren (D5). Ohne sie wäre die zweite MODIFIED-Fassung eine Rückänderung.
- **[Rechte beim Zurück]** → Ein Spieler, der während der Serverauswahl Rechte verliert, sieht den Navigator ohne den Eintrag. Das ist die beabsichtigte Prüfung über `open`.
