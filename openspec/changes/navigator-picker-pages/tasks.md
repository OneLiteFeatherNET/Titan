# Tasks

## Execution Plan

Basis: `origin/main`, Arbeit in Worktrees je Agent. Reihenfolge: Welle 1 (core) muss vor Welle 2 (Navigator-Logik) fertig sein, weil `PickerLayout` die Seitenmathematik aus `core` nutzt. Welle 3 hängt an Welle 2. Jeder Abschnitt endet mit grünem Build der betroffenen Module.

| Welle | Agent | Aufgaben | Modell | Darf anfassen | Darf nicht anfassen |
| ----- | ----- | -------- | ------ | ------------- | ------------------- |
| 1 | A (core) | 1.1 bis 1.4 | Haiku, effort medium | `core/**/paging/**`, `features/lobbyswitcher/**/SwitcherLayout.java` | `features/navigator/**` |
| 2 | B (Ordnung, Layout) | 2.1 bis 2.5 | Haiku, effort high | `features/navigator/**` (`PickerSort`, `PickerLayout`, `ServerPicker`, Tests) | `core/**`, `features/lobbyswitcher/**` |
| 3 | C (Ansichten, Klicks, Texte) | 3.1 bis 5.2 | Haiku, effort high | `features/navigator/**` (`SharedServerPicker`, `NavigatorModule`, `NavigatorMessages`, Bundles, Tests) | `core/**`, `features/lobbyswitcher/**` |
| 4 | D (Doku, Nachweise) | 6.1 bis 6.3 | Haiku, effort low; Review Haiku, effort medium | `docs/lobby-modules.md`, Nachweise | Produktionscode |
| 5 | Nutzer / Hauptsitzung | 7.1 | - | - | - |

Regeln für alle Agenten:

- **Built-in first:** Minestom `Scheduler`/`EventDispatcher`, Aves `GlobalInventoryBuilder` (`setLayout`, `invalidateLayout`), Adventure `MiniMessage` und `GlobalTranslator`, Avaje `@Profile`. Kein eigenes Inventarsystem.
- **Java 25 ohne Preview:** Records, Enums mit Methoden, `switch`-Ausdrücke, `Math.ceilDiv`/`Math.clamp`.
- **Test zuerst:** rot, dann grün, dann aufräumen. Ein Fehler wird zuerst als fehlschlagender Test reproduziert.
- **F.I.R.S.T.:** kein `Thread.sleep`, keine Systemzeit, frische `Env` je Test, `env.tick()` statt Warten, keine statischen Zustände; Assertions mit Meldung; Logs nur über Captured Appender (hier nicht nötig).
- **i18n:** jeder neue Text in `messages_en.properties` und `messages_de.properties`; Englisch ist der Fallback.
- **Kommentare:** nur das Warum, 1 bis 3 Zeilen.
- **Commits:** Conventional Commits, je Typ ein Commit; jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`.

## 1. Seitenmathematik in `core` (Typ `refactor`, eigener Commit)

- [x] 1.1 Test zuerst (Unit, `core`, `PaginationTest`): Seitenzahl für 0, 1, 45, 46 und 90 Einträge; Ausschnitt einer vollen, einer halben und einer über dem Ende liegenden Seite; Klammerung der Seitennummer. Verifikation: `./gradlew :core:test --tests '*PaginationTest'` zeigt die Fehlschläge vor 1.2.
- [x] 1.2 `net.onelitefeather.titan.core.paging.Pagination` als reine Klasse (`PAGE_SIZE = 45`, `pageCount`, `slice`, Klammerung). Verifikation: `./gradlew :core:test --tests '*PaginationTest'` grün.
- [x] 1.3 Characterization: `SwitcherPagingTest` und `SwitcherLayoutTest` vor der Umstellung grün notieren. `SwitcherLayout` nutzt `Pagination` für `PAGE_ENTRIES` und das Abschneiden einer Seite; Verhalten und Schwelle 54 bleiben. Verifikation: `./gradlew :features:lobbyswitcher:test` grün, unverändert zur Baseline.
- [x] 1.4 Commit `refactor(core): share page slicing with the lobby switcher`. Verifikation: `./gradlew :core:build :features:lobbyswitcher:build` grün.

## 2. Ordnung und Layout (reine Logik, `navigator`)

- [x] 2.1 Test zuerst (Unit, `PickerSortTest`): NAME (A–Z); MOST_PLAYERS absteigend mit Gleichstand nach Name; MOST_FREE mit Diensten ohne Limit zuletzt und Gleichstand nach Name; STATUS mit JOINABLE, dann NOT_READY, dann FULL und Gleichstand nach Name; `next()` von STATUS führt zu NAME, vier Schritte führen zurück. Verifikation: rot vor 2.2.
- [x] 2.2 `PickerSort` (Enum mit Komparatoren und `next()`); `ServerPicker.stateOf` wird paketsichtbar; `STATUS_ORDER = List.of(JOINABLE, NOT_READY, FULL)` in `ServerPicker`; `ServerPicker.entries` ohne `limit` und mit Ordnung als Parameter. Verifikation: `./gradlew :features:navigator:test --tests '*PickerSortTest'` grün.
- [x] 2.3 Test zuerst (Unit, `PickerLayoutTest`): Zeilen für 0, 1, 9, 10, 45 und 46 Dienste; Seiten für 45, 46 und 90; Slots der Navigationszeile je Seite (45 Vorige nur ab Seite 2, 49 Sortier-Item immer, 53 Nächste nur wenn eine Seite folgt) und Zentrierung nicht voller Reihen (`serviceSlot`). Dann `PickerLayout(rows, pages)` mit `Pagination`. Verifikation: `./gradlew :features:navigator:test --tests '*PickerLayoutTest'` grün.
- [x] 2.4 Test zuerst (Unit, `ServerPickerStyleTest` erweitert): die Lore des Sortier-Items nennt alle vier Ordnungen, markiert genau die aktive mit Pfeil und Farbe, die übrigen grau; Name des Items enthält die aktive Ordnung. Verifikation: rot, dann grün.
- [x] 2.5 Verifikation der Welle: `./gradlew :features:navigator:test` für die reinen Klassen grün; Prüfung von Sleeps und Systemzeit in den neuen Tests (keine Treffer).

## 3. Ansichten und Inventare (Integration, `navigator`)

- [x] 3.1 Test zuerst (Env, `NavigatorPickerViewsTest`): 100 Dienste, Seite 1 zeigt die ersten 45 nach Name, Anzeige „Seite 1/3“, Pfeil nur nach rechts; Seite 2 mit Pfeil links und rechts; Seite 3 nur mit Pfeil links. Verifikation: rot vor 3.2.
- [x] 3.2 `SharedServerPicker` hält `Map<PickerView, GlobalInventoryBuilder>` je Ziel und Sprache; `PickerView(PickerSort, page, rows)`; eine Ansicht entsteht beim ersten Bedarf und wird danach wiederverwendet; `unregister()` entfernt alle Ansichten. Verifikation: 3.1 grün.
- [x] 3.3 Test zuerst (Env, `NavigatorPickerLifecycleTest` erweitert): 20 Öffnen und Schließen ändern die Listenerzahl nicht; ein Durchlauf aller vier Ordnungen, danach ein zweiter, ändert sie nicht mehr; Stoppen setzt die Klick-Listener jeder geöffneten Ansicht auf 0. Verifikation: rot, dann grün.
- [x] 3.4 Test zuerst (Env, `NavigatorServerPickerTest` erweitert): Spieler A stellt Status ein und blättert auf Seite 2; Spieler B öffnet danach die Serverauswahl und sieht Name auf Seite 1. Verifikation: rot, dann grün.

## 4. Klicks, Seiten und Neulesen (Integration, `navigator`)

- [x] 4.1 Test zuerst (Env, `NavigatorServerPickerTest`): Klick auf das Sortier-Item liest die Dienste genau einmal neu (Fake-`PlayerCounts` zählt), wechselt die Ordnung, springt auf Seite 1 und markiert die neue Ordnung in der Lore. Verifikation: rot, dann grün mit der Navigator-Modul-Änderung (`readServices`).
- [x] 4.2 Test zuerst (Env): Pfeil rechts und links wechseln die Seite; ein fehlender Pfeil tut nichts; eine Seite, die nach dem Neulesen nicht mehr existiert, zeigt die letzte vorhandene Seite; ein beim Öffnen laufender Dienst, der vor dem Seitenwechsel gestoppt wurde, erscheint danach nicht mehr. Verifikation: rot, dann grün.
- [x] 4.3 Test zuerst (Env): Zahlentaste und Mittelklick auf das Sortier-Item und auf die Pfeile ändern weder Ordnung noch Seite; das Inventar bleibt offen. Verifikation: rot, dann grün.
- [x] 4.4 Test zuerst (Env): 200 Dienste, alle fünf Seiten nacheinander aufrufen; jeder Dienst erscheint genau einmal. Verifikation: rot, dann grün.
- [x] 4.5 Test zuerst (Env): ein Klick auf einen Eintrag auf Seite 2 wählt genau diesen Dienst (bestehender Weg `pick` → `decidePick`). Verifikation: grün ohne neue Logik, sonst Fehler reproduzieren.

## 5. Texte (i18n)

- [x] 5.1 Test zuerst (Unit, `NavigatorMessagesBundleTest` erweitert): alle neuen Schlüssel (`titan.navigator.picker.sort.*`, `…page.*`) stehen in `KEYS` und in beiden Bundles. Verifikation: rot, dann Bundles ergänzt, grün.
- [x] 5.2 Test zuerst (Env): ein Spieler mit deutschem Client sieht „Freie Plätze“ in der Lore des Sortier-Items, ein Spieler mit englischem Client „Free slots“; eine unbekannte Sprache sieht Englisch. Verifikation: rot, dann grün.

## 6. Dokumentation und Nachweise

- [x] 6.1 `docs/lobby-modules.md` im Abschnitt zum Navigator: Seiten zu 45 Einträgen, Navigationszeile mit Sortier-Item, die vier Ordnungen, Neulesen auf Klick. Commit `docs(navigator): describe paging and sorting of the server picker`. Verifikation: Werte (45, Slot 49, Schlüsselnamen, Ordnungsnamen) stimmen mit Code und Bundles überein.
- [x] 6.2 Review gegen die Spec (Haiku, effort medium, ohne Anteil am Code): jeder Szenario-Titel aus `specs/lobby-navigator/spec.md` ist durch einen Test abgedeckt; keine Sleeps und keine Systemzeit in den neuen Tests. Verifikation: Liste im Review-Ergebnis, keine offenen Punkte.
- [x] 6.3 `./gradlew build` grün; `ColumnArchitectureTest` grün (die Column hängt nur an `core`); `openspec validate navigator-picker-pages --strict` grün. Der Archiv-Schritt (`docs(openspec): archive navigator-picker-pages`) folgt nach dem Merge durch den Nutzer und ist nicht Teil dieser Liste.

## 7. Pull Request

- [ ] 7.1 Pull Request öffnen mit dem Titel `feat(navigator): page and sort the server picker` (Squash-Commit des Titels, Conventional Commit). Beschreibung auf Englisch: Zusammenfassung (Seiten, Sortierung, Ansichten, Begrenzung der Inventare), Testnachweis (`./gradlew build`), Verweis auf `openspec/changes/navigator-picker-pages`, abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
