# Proposal

## Why

Die Klickhinweise der Modus-Einträge stehen als eine Lore-Zeile mit „ · “ getrennt: „Linksklick: Server wählen · Rechtsklick: zum Portal laufen“. Das ist schwer zu lesen, und die beiden Klicks lassen sich in der Zeile kaum voneinander trennen.

## What Changes

- Linksklick und Rechtsklick stehen auf zwei eigenen Lore-Zeilen, die Spielerzahl bleibt die erste Zeile:
  - Zeile 1 (immer): „Linksklick: Server wählen“ bzw. „Linksklick: verbinden“ (en: „Left click: choose a server“ / „Left click: connect“).
  - Zeile 2 (nur mit Portal): „Rechtsklick: zum Portal laufen“ (en: „Right click: walk to portal“).
- Kein „ · “ mehr zwischen den Klicks.
- Übersetzungen: je Klick ein Schlüssel (Serverauswahl, Verbinden, Portal), die kombinierten Schlüssel entfallen.
- Die Spec sagt ausdrücklich, dass der Serverauswahl-Teil der letzten abgeschlossenen Aktualisierung folgt, während der Klick die Dienste neu liest.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-navigator`: die Anforderung „Modus-Einträge erklären die Klicks“ und „Klickhinweis ist übersetzt“ beschreiben die Hinweise als zwei Zeilen.

## Impact

- **Code:** `features/navigator` (`NavigatorMessages`, `Destination.item`, `NavigatorModule.itemOf`), Bundles `messages_de.properties` / `messages_en.properties`, Tests des Navigators.
- **Nutzertexte:** Lore-Zeilen der Modus-Einträge (de/en) werden getrennt; Inhalt bleibt gleich.
- **Abhängigkeiten:** keine.

## Delivery

Pull-Request-Titel und Squash-Commit: `fix(navigator): show left and right click hints on separate lines`
