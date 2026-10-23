# Design

## Context

Motivation steht in `proposal.md`, die Anforderungen in `specs/lobby-switcher/spec.md`. Ausgangslage im Modul `features/lobbyswitcher`:

- Geöffnet wird die Liste nur über das Hotbar-Item (`LobbySwitcherItems`, Slot 8, `LobbyItem` mit `module.open(player)`). Es gibt keine übergeordnete Ansicht. Die Serverauswahl des Navigators ist ein anderes Feature und darf nicht importiert werden (`ColumnArchitectureTest`). Deshalb schließt der Knopf die Liste; er führt nirgendwohin.
- `SwitcherLayout` zählt `rows` als Gesamtzeilen des Inventars (`TYPES.get(rows - 1)`). Eine Navigationszeile gab es bisher nur bei sechs Zeilen, mit festen Slots 45, 49 und 53.
- `core/.../paging/Pagination` teilt in Seiten zu 45 Einträgen. Diese Klasse bleibt unverändert, andere Menüs nutzen sie ebenfalls.

## Goals / Non-Goals

- Ziel: Die Navigationszeile ist immer da, und ihre Slots hängen nur von der Zeilenzahl ab, nicht von der Seitenzahl.
- Nicht-Ziel: Die Serverauswahl des Navigators (eigener Change, zieht auf dasselbe Layout um), Änderungen an `Pagination`, Änderungen an Aktualisierung, Telemetrie oder Klick-Logik.

## Decisions

### D1: Schließen statt Zurück
Der Knopf schließt das Inventar (`player.closeInventory()`), statt zu einem Vorgänger zurückzukehren. Es gibt keinen Vorgänger, und ein Import der Serverauswahl würde die Column-Regel verletzen. Alternative verworfen: eine Zurück-Ansicht im selben Modul wäre eine zweite Ansicht ohne Nutzen.
Material `BARRIER`, Name aus `titan.lobbyswitcher.close`. Die Konvention ist dieselbe wie bei Menüs anderer Server. Prüfung: Unit-Test der Slot-Lage und Env-Test, dass ein Klick das Inventar schließt und die Aktualisierung stoppt. Die Klasse `SwitcherInventory` bleibt bei einer Aufgabe: Klicks auswerten; das Schließen selbst ist die Aufgabe von Minestom.

### D2: Zeilen als Gesamtzahl, Navigation relativ zur letzten Zeile
`SwitcherLayout.rows` bleibt die Gesamtzahl der Zeilen, die Inhaltszeilen sind `rows - 1`. Für `n` Einträge ohne Seiten sind es `max(1, ceil(n/9)) + 1` Zeilen; über 45 Einträgen `6` Zeilen auf `Pagination.pageCount(n)` Seiten. Die Slots der Navigationszeile ergeben sich aus `(rows - 1) * 9`:

| Slot | Inhalt |
|---|---|
| Start + 0 | Schließen-Knopf (immer) |
| Start + 3 | Vorige (nur ab Seite 2) |
| Start + 4 | „Seite x/y“ (nur bei mehreren Seiten) |
| Start + 5 | Nächste (nur, wenn eine Seite folgt) |

Die festen Konstanten `PREVIOUS_SLOT`, `INDICATOR_SLOT`, `NEXT_SLOT` entfallen; die Slots sind Methoden der Layout-Klasse. Alternative verworfen: feste Slots 45 und 49 für jede Zeilenzahl; das passt nicht, sobald die Navigationszeile in einer kürzeren Liste unten liegt.
Prüfung: Unit-Tests mit 0, 1, 9, 10, 45, 46 und 90 Einträgen (`SwitcherLayoutTest`) für Zeilen, Seiten und Slots.

### D3: Seitengröße kommt aus `Pagination`
`SwitcherLayout.of` verwendet `Pagination.PAGE_SIZE` und `Pagination.pageCount`. `Pagination` selbst bleibt unverändert, weil andere Aufrufer die 45 Einträge je Seite brauchen. Eingebaute Lösung statt eigener Rechnung: `Math.ceilDiv` und `Pagination` sind bereits da.

### D4: Texte über den bestehenden Bundle-Mechanismus
Ein neuer Schlüssel `titan.lobbyswitcher.close` in `messages_en.properties` (`Close`) und `messages_de.properties` (`Schließen`), in `LobbySwitcherMessages.KEYS` aufgenommen. `LobbySwitcherMessagesBundleTest` prüft beide Dateien gegen die Liste. Prüfung: Bundle-Test (rot, dann grün).

### D5: Delta und offene Capability
`lobby-switcher` ist noch nicht archiviert, sodass `openspec/specs/lobby-switcher/spec.md` fehlt. Die Anforderungen „Größe der Liste“ und „Seiten bei mehr als 54 Lobbys“ stehen nur im Change `openspec/changes/lobby-switcher`. Dieser Change ändert sie deshalb an beiden Stellen:

- `openspec/changes/lobby-switcher/specs/lobby-switcher/spec.md`: Texte und Szenarien auf die neue Zeilen- und Seitenlogik gesetzt (Name der Seiten-Anforderung „Seiten bei mehr als 45 Lobbys“). Die Zahlen in `proposal.md` und `design.md` (Q6) des Changes folgen demselben Stand.
- `openspec/changes/lobbyswitcher-back/specs/lobby-switcher/spec.md`: `RENAMED` und `MODIFIED` auf dieselben Texte, dazu `ADDED` für die Navigationszeile.

Archivreihenfolge: zuerst `lobby-switcher`, dann `lobbyswitcher-back`. Dann ersetzt das `MODIFIED`-Delta dieselben Texte im Hauptbestand, ohne Widerspruch.

## Risks / Trade-offs

- [Die Liste ist eine Zeile höher] → gewollt: der Knopf braucht die Zeile. Bei einer Lobby sind es zwei statt einer Zeile.
- [Slots relativ zur Zeilenzahl] → Aufrufer dürfen die Konstanten nicht mehr festschreiben. Prüfung: Tests nutzen die Methoden der Layout-Klasse, nicht Zahlen.
- [Klick mit `closeInventory()` im Klick-Event] → Minestom schließt das Inventar auch dort; der Env-Test prüft, dass es danach offen ist und die Aktualisierung still steht.
- [Der Serverauswahl-Change wird dasselbe Muster übernehmen] → nicht in diesem Change. Die Form der Navigationszeile ist in der Spec beschrieben, damit beide gleich bleiben.

## Migration Plan

Keine Daten, keine Konfiguration. Ausrollen mit dem nächsten Release der CloudNet-Variante. Rückbau: den Eintrag für Start + 0 entfernen; die Layout-Methoden bleiben.

## Open Questions

Keine.
