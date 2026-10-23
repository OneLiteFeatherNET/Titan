# Proposal

## Why

Die Lobby-Liste hat keinen Ausgang außer Esc. Die Navigationszeile gibt es nur ab 55 Lobbys, und dort trägt sie nur Seitenpfeile. Ein Spieler, der die Liste öffnet und wieder schließen will, soll das mit einem sichtbaren Knopf an derselben Stelle tun können, egal wie viele Lobbys laufen. Der Serverauswahl des Navigators bekommt dasselbe Layout, damit beide Menüs gleich zu bedienen sind.

## What Changes

- Die Liste hat immer eine Navigationszeile in der untersten Reihe, auch bei einer einzigen Seite.
- Die Navigationszeile hat feste Plätze, bezogen auf ihren ersten Slot (Start + 0 bis Start + 8):
  - **Start + 0** (links unten): Schließen-Knopf (`BARRIER`, „Schließen“ / „Close“). Er ist immer da und schließt das Inventar. Die Liste hat keine übergeordnete Ansicht, deshalb schließt „zurück“ hier und führt nirgendwohin.
  - **Start + 3** und **Start + 5**: Vorige- und Nächste-Pfeil, nur wenn es in die Richtung eine Seite gibt.
  - **Start + 4**: Seitenanzeige „Seite x/y“, nur bei mehr als einer Seite; sonst bleibt der Platz leer.
- Die Zeilenzahl ist `ceil(n/9)` Inhaltszeilen (mindestens eine), höchstens fünf, plus die Navigationszeile.
- Mehr als 45 Lobbys werden auf Seiten zu 45 Einträgen verteilt (`Pagination`, unverändert). Auf jeder Seite stehen fünf Inhaltszeilen und die Navigationszeile.
- Die Anforderungen „Größe der Liste“ und „Seiten bei mehr als 54 Lobbys“ der Capability `lobby-switcher` werden entsprechend geändert (Delta in `specs/lobby-switcher/spec.md`). Da `lobby-switcher` noch nicht archiviert ist, werden die Texte dort ebenfalls angepasst (siehe `design.md`).
- Neue Übersetzungsschlüssel `titan.lobbyswitcher.close` (Englisch und Deutsch).
- Die Aktualisierung, der Klick auf einen Eintrag und die Telemetrie bleiben unverändert.

## Non-Goals

- Kein Zurück zu einer übergeordneten Ansicht: Die Liste wird nur über das Hotbar-Item geöffnet, es gibt keinen Vorgänger, zu dem man zurückkehren könnte.
- Keine Änderung an `core`-`Pagination`; andere Aufrufer bleiben bei 45 Einträgen je Seite.
- Keine Änderung an der Serverauswahl des Navigators in diesem Change. Sie zieht in einem eigenen Change auf dieses Layout um.

## Capabilities

### New Capabilities
- Keine.

### Modified Capabilities
- `lobby-switcher`: Größe der Liste und Seitenaufteilung ändern sich; neue Anforderung für den Schließen-Knopf in der Navigationszeile. Die Capability ist noch nicht archiviert (Change `lobby-switcher`), das Delta wird beim Archivieren angewendet.

## Impact

- **Code:** `features/lobbyswitcher`: `SwitcherLayout` (Zeilen, Slots der Navigationszeile), `SwitcherInventory` (Navigation immer, Klick auf den Schließen-Knopf, Klicks auf Pfeile je nach Zeilenzahl), Nachrichten `LobbySwitcherMessages`.
- **Tests:** `SwitcherLayoutTest`, `SwitcherPagingTest`, neuer Test für die Navigationszeile, `LobbySwitcherMessagesBundleTest` über den neuen Schlüssel.
- **Texte:** neu `titan.lobbyswitcher.close` in `messages_en.properties` und `messages_de.properties`.
- **Dokumentation:** `README.md` (Beschreibung des Switchers), `docs/lobby-modules.md`.
- **Abhängigkeiten:** keine.
- **Spielerverhalten:** die Liste hat eine Zeile mehr als bisher (ab zwei Zeilen Inhalt) und zeigt bei genau einer Seite die Navigationszeile mit dem Schließen-Knopf.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(lobbyswitcher): add a back button to the switcher`

Ein Typ (`feat`), Scope `lobbyswitcher`. Keine Breaking Change.
