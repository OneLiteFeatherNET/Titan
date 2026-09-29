# Proposal

## Why

Spieler sollen die Lobby verlassen, indem sie in einen Bereich der Welt laufen oder durch einen Ring fliegen, nicht nur über den Navigator. PR #222 („portals“) wollte das mit einer `portals.json`, Coris-`CuboidShape`, FeatureGate und einer `ServiceAvailability` der Bridge lösen. Er hängt gestapelt an #216, das nie in `main` gemergt wurde. Dieser Change ersetzt ihn durch das Kleinste, das trägt: Portale sind Kartendaten der jeweiligen Welt, und ein Portal leitet an einen CloudNet-Task weiter, wie der Navigator es tut.

## What Changes

- Neue Column `features/portal`, in **beiden** Varianten (`cloudnet` und `local`): Sie prüft bei jeder Spielerbewegung, ob die Strecke vom alten zum neuen Standort ein Portal der Welt berührt, und leitet den Spieler an den CloudNet-Task des Portals weiter. Wie beim Navigator ist `Deliver` außerhalb von CloudNet ein No-op; eine eigene Ausnahme für `local` gibt es nicht.
- Portale sind **Kartendaten**: eine Liste `portals` in der `map.json` der Welt (`LobbyMap`). Jede Saisonwelt bringt ihre eigenen Portale mit. Das Format (Formen `box` und `disc`, optionales Recht) steht als Vertrag in `design.md`. Bis der getrennte Folge-Change `setup-portal-command` Befehle im Setup-Server liefert, wird die `map.json` von Hand bearbeitet.
- Zwei Formen: `box` (achsenparallel, blockgenau einschließlich) und `disc` (Mittelpunkt, Radius, Normalenvektor, beliebig ausgerichtet; Mathematik aus Voyagers `RingPass`).
- Erkennung über die **Strecke** zwischen altem und neuem Standort, damit auch ein schneller Elytra-Flug durch einen dünnen Ring nicht übersprungen wird. Laufen und Fliegen lösen gleichermaßen aus.
- Auslösung einmal beim Betreten, dazu eine feste Abklingzeit von 3 Sekunden je Spieler (im Code, nicht in der Konfiguration).
- Ziel ist ausschließlich ein CloudNet-Task (`Deliver` mit `DeliverComponent.taskBuilder()`). Optionales Recht je Portal: ist es gesetzt und nicht `ALLOWED`, passiert nichts und es gibt keine Meldung (wie beim Navigator-Ziel Build). Keine Erreichbarkeitsprüfung; CloudNets Task-Routing übernimmt das.
- Ein ungültiges Portal in der `map.json` (unbekannte Form, Radius kleiner oder gleich 0, Normale null, `min` größer `max`, fehlender Task, doppelte Id) verhindert den Start und nennt Welt, Portal-Id und Grund.
- Neue Typen in `core` (`net.onelitefeather.titan.core.portal`), weil Columns nur `core` sehen: `PortalShape` (`Box`, `Disc`), `Portal`, `LobbyPortals` und die wiederverwendbare Prüfung `PortalValidator`. `common` (`LobbyMap` samt Builder-Kopie, Gson) liest und schreibt sie; `runtime` (`PlatformBeans`) stellt `LobbyPortals` aus der aktiven Karte bereit.

## Capabilities

### New Capabilities

- `lobby-portals`: Portale als Kartendaten, Auslösung beim Betreten durch Laufen oder Fliegen, Abklingzeit, optionales Recht, Weiterleitung an einen CloudNet-Task und Startabbruch bei ungültigen Portalen.

### Modified Capabilities

Keine. `lobby-modules` (Event-Reihenfolge, Column-Unabhängigkeit) und `app-variants` (gleiche Columns in beiden Varianten) gelten unverändert; die Column reiht sich mit einer festen Position ein und steht in beiden Varianten.

## Impact

- **Code**: neues Modul `features/portal` (von `settings.gradle.kts` automatisch erfasst, `titan.column`); `core` (neues Paket `portal`); `common` (`LobbyMap`, `LobbyMapBuilder`, `MapProvider`-Gson, neuer Adapter); `runtime` (`PlatformBeans`); `docs/lobby-modules.md` (Tabellenzeile `portal`, Format der Portale). Keine Änderung an `apps/*/build.gradle.kts`.
- **Abhängigkeiten**: keine neuen. `Deliver`, `PermissionService`, `java.time.Clock`, Minestom `Point`/`Vec` und Gson (über Aves) sind da.
- **Tests**: Unit-Tests der Formen (Voyagers `RingPass`-Fälle, Slab-Fälle, schneller Elytra-Schritt), der Auslöse-Logik mit Fake-`Clock`, des JSON-Formats und der Validierung; Integrationstest mit Cyano-`Env`; `ColumnArchitectureTest`; Starttest je Variante.
- **Nutzertexte**: keine (keine Chat-Nachricht); nur Startfehler für Betreiber (Englisch).
- **Betrieb**: Portale stehen in der `map.json` der Welt; der Ziel-Task muss in CloudNet existieren. Ohne `portals` in der Datei ändert sich nichts.
- **Ersetzt**: PR #222 wird nach dem Merge als überholt geschlossen.

## Delivery

PR-Titel: `feat(portal): switch servers by walking or flying through portals`

Nicht-Ziele, jeweils eigener Change: Befehle im Setup-Server zum Anlegen und Bearbeiten (`setup-portal-command`), Weiterleitung an einen einzelnen Server statt an einen Task, Erreichbarkeitsprüfung und Warnung bei fehlender Bridge (`bridge-connector-warning`), Nachricht an den Spieler, Partikel oder Anzeige der Portale, konfigurierbare Abklingzeit, Neuladen der Portale zur Laufzeit.
