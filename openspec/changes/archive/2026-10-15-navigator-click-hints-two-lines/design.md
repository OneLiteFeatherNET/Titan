# Design

## Entscheidungen

- **Zwei Zeilen statt einer:** `Destination.item` nimmt eine Liste von Hinweis-Zeilen entgegen (links, optional rechts) und hängt jede als eigene Lore-Zeile an, nach der Spielerzahl.
- **Ein Schlüssel je Klick:** `titan.navigator.click.left_pick`, `titan.navigator.click.left_connect`, `titan.navigator.click.right_portal`. `NavigatorMessages.clickHints(locale, serverPicker, portal)` liefert die Zeilen in der Reihenfolge links, rechts.
- **Snapshot bleibt Snapshot:** Die Entscheidung „Server wählen“ liest die letzte Aktualisierung (wie die Spielerzahl); das Portal wird beim Öffnen nachgeschlagen. Der Klick selbst liest die Dienste neu und entscheidet dort erneut.
