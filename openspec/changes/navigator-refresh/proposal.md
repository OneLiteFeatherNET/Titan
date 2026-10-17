# Proposal

## Why

Der Navigator zeigt neun Plätze in einer Reihe, alle Modi gleich grau. Die Klickhinweise sagen nicht, was ein Klick tut, wenn ein Modus auf mehreren Servern läuft: der Linksklick öffnet dann eine Serverauswahl, der Rechtsklick läuft zum Portal. Wer schnell in eine Runde will, muss heute durch die Auswahl klicken. Der Rechtsklick auf ein Minispiel bringt nur dann etwas, wenn in der Lobby ein Portal steht.

## What Changes

- **Layout:** Der Navigator hat vier Reihen (36 Plätze, `CHEST_4_ROW`) mit einem Rahmen aus Glasscheiben. Minispiele liegen in Reihe 2 (ElytraRace auf Platz 11, Slender auf Platz 15), Server in Reihe 3 (Survival 20, Creative 22, Build 24), Spawn unten mittig auf Platz 31.
- **Stil:** Jeder Modus hat einen eigenen Verlauf für Name, Trennlinie und Klickhinweise. Die Lore zeigt eine Trennlinie, die Spielerzahl mit Maximum, eine Leerzeile und die Klickhinweise mit `▸`. Der Menütitel ist „Navigator“ im Verlauf.
- **Schnellbeitritt:** Linksklick auf ein Minispiel tritt direkt einer bereiten, nicht vollen Runde bei (die mit den meisten Spielern zuerst). Gibt es keine, öffnet sich die Serverauswahl.
- **Verhaltensänderung:** Der Rechtsklick auf ein Minispiel öffnet immer die Serverauswahl, auch bei nur einem Server. Das Portal liegt jetzt auf Shift-Klick (Shift-Links oder Shift-Rechts). Bisher war der Rechtsklick das Portal (PR #414). Ohne Portal hat Shift keine eigene Wirkung und der Klick verhält sich wie ohne Shift.
- **Serverauswahl:** Titel, Zeilen und Zustände (Voll, Noch nicht bereit, Klicken zum Beitreten) folgen dem Stil des Navigators und sind übersetzt. Die Zeile „Klickhinweis“ hängt nicht mehr davon ab, ob mehrere Dienste laufen.
- **Menü-Varianten:** Die acht geteilten Inventare (vier Rechte-Kombinationen mal zwei Sprachen) bleiben. Der Titel wird je Sprache beim Start gesetzt. Die Serverauswahl bekommt je Ziel und Sprache ein geteiltes Inventar.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: Layout in vier Reihen, Stil der Einträge, Klickzuordnung mit Schnellbeitritt und Shift-Portal, Klickhinweise und Serverauswahl im Stil des Navigators, übersetzt.

## Impact

- **Code**: `features/navigator` (`Destination`, `NavigatorModule`, `SharedNavigator`, `SharedServerPicker`, `ServerPicker`, `NavigatorMessages`, `NavigatorItems`, `TaskPlayers`) und neue reine Klassen für Layout, Klickzuordnung und Schnellbeitritt, dazu die Tests dort.
- **Nutzertexte**: `titan/navigator/messages_de.properties` und `messages_en.properties` bekommen die Schlüssel für Tasten, Aktionen, Spielerzeile, Titel und Serverauswahl. Die englischen Texte der Serverauswahl waren bisher fest im Code und werden übersetzt.
- **Konfiguration**: keine. Keine neuen Abhängigkeiten, keine neuen Feature-Flags.
- **Betrieb**: Spieler, die den Rechtsklick auf ein Minispiel als Portal gelernt haben, nutzen dafür jetzt Shift-Klick. Das ist im Changelog zu nennen.

## Delivery

Basis: `feat/navigator-slender-permission` (PR #425 gestapelt).

PR-Titel: `feat(navigator): refresh the navigator with gradients, rows and quick join`
