# Proposal

## Why

Wer die Serverauswahl eines Minispiels geöffnet hat, kommt bisher nur über Schließen zurück in den Navigator und muss ihn dann neu öffnen. Eine Zurück-Taste führt direkt in das Navigator-Menü, das zu seinen Rechten und seiner Sprache passt.

## What Changes

- Die Navigationszeile der Serverauswahl bekommt eine Zurück-Taste auf Navigationsstart + 0 (unten links), auf jeder Seite und auch bei leerer Liste. Sie ist ein Item mit `Material.DARK_OAK_DOOR` und dem Namen „Zurück zum Navigator“ (Englisch: „Back to the navigator“).
- Die Seitenpfeile wandern neben das Sortier-Item: der Vorige-Pfeil auf Navigationsstart + 3, der Nächste-Pfeil auf Navigationsstart + 5. Das Sortier-Item bleibt in der Mitte (Navigationsstart + 4). Ein Pfeil erscheint weiterhin nur, wenn es in seine Richtung eine Seite gibt.
- Ein benannter Klick auf die Zurück-Taste öffnet den Navigator über denselben Weg wie das Feder-Item. Rechte und Sprache werden beim Klick neu gelesen; das Menü entspricht also den aktuellen Rechten des Spielers.
- Neue Texte: `titan.navigator.picker.back` in `messages_en.properties` und `messages_de.properties`.
- Die Anforderung „Serverauswahl blättert durch alle Dienste“ stammt aus dem unarchivierten Change `navigator-picker-pages` und wird dort auf die neuen Positionen angepasst (siehe `design.md`, D5).
- **Nicht brechend**: Die Dienste, Ordnungen, Seiten und Klicks auf Einträge ändern sich nicht. Die Pfeile liegen an anderen Slots, das ist eine sichtbare Änderung.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: neue Anforderung „Zurück-Taste führt in den Navigator“; geändert wird „Nur benannte Klicks wirken“ (Zurück-Taste). Siehe `specs/lobby-navigator/spec.md`. Die Positionen der Navigationszeile stehen in der Anforderung „Serverauswahl blättert durch alle Dienste“ des Changes `navigator-picker-pages`, der nicht Teil dieses Changes ist.

## Delivery

Der Pull-Request-Titel und der Squash-Commit sind `feat(navigator): add a back button to the server picker`. Die Änderung ist nicht brechend und ein einziger Typ `feat`. Dieser OpenSpec-Change liegt in einem eigenen `docs(openspec)`-Commit; die Dokumentation in `docs/lobby-modules.md` hat einen eigenen `docs(navigator)`-Commit.

## Impact

- **Code:** `features/navigator`: `PickerLayout` (Slot der Zurück-Taste, neue Pfeilslots), `SharedServerPicker` (Zurück-Taste und Klick-Callback), `ServerPicker` (Item der Zurück-Taste), `NavigatorModule` (Klick führt zu `open(player)`), `NavigatorMessages` (neuer Schlüssel), Bundles und Tests der Serverauswahl.
- **Abhängigkeiten:** keine neuen Bibliotheken.
- **Nutzertexte:** neu, Englisch und Deutsch: „Zurück zum Navigator“ / „Back to the navigator“. Bestehende Texte bleiben unverändert.
- **Konfiguration:** keine.
- **Spielerverhalten:** die Serverauswahl hat unten links eine Zurück-Taste, die Seitenpfeile stehen neben dem Sortier-Item.
