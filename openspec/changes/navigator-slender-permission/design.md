# Design

## Kontext

Der Navigator zeigt je Sprache geteilte Aves-Inventare. Ein Inventar gilt für alle Spieler, die es öffnen, also darf die Sichtbarkeit eines Ziels nur über die Wahl des Inventars laufen, nicht über Items, die je Spieler verändert werden. Build ist seit dem Change `navigator-build-destination` an ein Recht gebunden und ist das Muster für Slender.

## Entscheidungen

- **Slender folgt Build.** `Destination.SLENDER` trägt das Recht `titan.navigator.slender`. Geprüft wird über `isAllowed` mit `ALLOWED` als einzigem erteilten Ergebnis; `NOT_SET` und `DENIED` zählen als nicht erteilt. Der Klick prüft erneut, bevor weitergeleitet wird.
- **Ein Inventar je Rechte-Kombination und Sprache.** Der Navigator bildet für jede Teilmenge der gebundenen Ziele (`Destination.permissioned()`, also Slender und Build) und jede Sprache ein geteiltes Inventar. Das sind vier Varianten mal zwei Sprachen, acht Inventare, beim Start angelegt. Das ist einfacher als ein Inventar je Spieler oder ein Lazy-Cache, und die Menge wächst nur mit der Zahl gebundener Ziele. Bei mehr als zwei gebundenen Zielen würde die Menge schnell wachsen; dann wäre eine Neubewertung fällig.
- **Öffnen liest die Rechte.** `open` ermittelt die erteilten gebundenen Ziele des Spielers, wählt das Inventar für diese Menge und die Sprache und öffnet es. Die Sichtbarkeit einer Menge ist fest, daher wird ihr Layout einmal beim Start angelegt; Portal-Hinweise werden wie bisher beim Öffnen aktualisiert.
- **Telemetrie.** Das Attribut `kind` bleibt `team` für Spieler mit `titan.navigator.buildserver`, sonst `public`, wie zuvor. Slender bekommt kein eigenes Attribut.
- **Task-Name.** Slender ist auf `Slender` gesetzt (`main`), Build auf `Team-Build`. Die Serverauswahl (`ServerPicker.DESTINATIONS`) bezieht sich über das Ziel auf denselben Namen, es gibt also nichts zu ändern.
- **Kein Lazy-Cache.** Ein Lazy-Cache mit `computeIfAbsent` hätte dieselbe Menge, aber zusätzliche Synchronisation und Tests für das erstmalige Anlegen; beim Start ist die Menge klein und bekannt.

## Nicht-Ziele

- Keine Änderung an Build außer der bereits bestehenden Bindung an `titan.navigator.buildserver`.
- Keine lokale Freigabe von Slender; die lokale Variante bleibt bei `DenyAllPermissionService`.
- Keine Rückwärtskompatibilität für `NAVIGATOR_SLENDER`; die Flag wird nicht mehr gelesen.
