# Design

## Context

Motivation und Anforderungen stehen in `proposal.md` und `specs/lobby-navigator/spec.md`. Ausgangslage im Code:

- `ServerPicker.entries` sortiert nach Name und schneidet mit `limit(COLUMNS * MAX_ROWS)` auf 54 ab. Die Reihenzahl kommt aus `rows(entries)`, es gibt keine Seiten.
- `SharedServerPicker` hält je Ziel und Sprache ein Array von `GlobalInventoryBuilder`, eines je Zeilenzahl. `show(...)` schreibt bei jedem Öffnen Layout und Inhalt in den geteilten Builder. Ein geteiltes Inventar zeigt allen Betrachtern denselben Inhalt; eine Ordnung je Spieler lässt sich darin nicht abbilden.
- `NavigatorModule` liest die Dienste für eine Serverauswahl über `readServices` (auf dem Executor, Entscheidung auf dem nächsten Tick, Schutz `choosing` gegen Doppelklicks). Der periodische `refreshCounts` füllt nur die Zeilen der Navigator-Menüs, nicht die Serverauswahl. Die Serverauswahl liest also heute beim Klick und nicht periodisch.
- `features/lobbyswitcher` hat eigenes Seitenrechnen (`SwitcherLayout`: 45 je Seite, Navigationszeile in der sechsten Reihe mit Pfeilen bei 45, 49 und 53, Seiten erst über 54 Einträge). Der Switcher hält keinen Zustand je Spieler: die Seite ist das Inventar, das der Spieler offen hat.
- Feature-Columns hängen nur an `core` (`ColumnArchitectureTest`). Ein Navigator-Code darf den Switcher nicht importieren; gemeinsame Logik muss in `core`.
- Aves 1.16.7 liefert `GlobalInventoryBuilder` (ein geteiltes Inventar), `PersonalInventoryBuilder` (je Spieler) und `PageableInventory`. Das Repo nutzt `PageableInventory` nicht.

## Goals / Non-Goals

**Goals:**
- Alle laufenden Dienste sind erreichbar, auf Seiten zu 45 Einträgen, mit Navigationszeile unten.
- Vier Ordnungen per Klick auf das Sortier-Item; Ordnung und Seite gelten je Spieler, ohne dass die Lobby sie je Spieler speichert.
- Inventare und Listener wachsen nur mit neuen Ansichten, nicht mit Öffnungen; Stoppen entfernt alle.
- Die Serverauswahl liest weiter nur auf Klick, nicht periodisch.

**Non-Goals:**
- Der Zustand „ingame“ (Change `navigator-ingame-servers`). Die Status-Ordnung ist so gebaut, dass er hinten angefügt wird.
- Suche, Filter, periodische Aktualisierung der offenen Serverauswahl, Telemetrie für Sortier- und Seitenklicks.
- Änderungen am Lobbyswitcher-Verhalten.

## Decisions

### D1 Paging: Aves `PageableInventory` wird nicht genutzt

- **Geprüft:** Aves `PageableInventory` (1.16.7) mit `Builder` (`layout`, `controls`, `slotRange`, `values`, `titleData`). Der Builder hat `player(Player)`: ein Inventar je Spieler, das bei jedem Öffnen eigene Listener erzeugt.
- **Verworfen, weil:**
  - Die Anforderung „Öffnen DARF KEIN neues Inventar und keinen neuen Listener erzeugen“ (`NavigatorPickerLifecycleTest`) würde gebrochen, wenn das Inventar je Spieler entsteht.
  - Die Steuerung kennt nur Zurück und Weiter (`PageableControls`). Das Sortier-Item müsste trotzdem selbst gebaut und verdrahtet werden.
  - Die Einträge sind feste `ISlot`-Werte je Instanz. Die Serverauswahl muss aber bei jedem Klick neu lesen und die Einträge neu schreiben (wie heute `show`).
  - Das Repo nutzt die Klasse nirgends; Einarbeitung und Tests kosten mehr als das Eigenbau-Rechnen.
- **Gewählt:** `GlobalInventoryBuilder` wie bisher, eines je Ansicht (D3). Die Seitenmathematik wird selbst gerechnet, und zwar in `core` (D2).
- **SOLID:** SRP, weil Seitenrechnen (rein) und Rendern (Aves) getrennt sind. Getestet: Seitenrechnen mit einfachen JUnit-Tests, Verhalten mit Cyano-`Env`.

### D2 Gemeinsame Seitenmathematik in `core`, Schwellen bleiben lokal

- **Extrahiert** wird, was dieselbe Bedeutung hat: Seitengröße `PAGE_SIZE = 45`, Anzahl der Seiten für eine Liste, der Ausschnitt einer Seite mit Klammerung der Seitennummer. Ort: `core` als `net.onelitefeather.titan.core.paging.Pagination` (rein, ohne Minestom, JDK-Typen).
- **Lokal** bleiben die Schwellen und die Zeilenregel, weil sie verschiedene Bedeutung haben:
  - Der Switcher paged erst über 54 Einträge und hat ohne Seiten keine Navigationszeile. Das bleibt unverändert.
  - Die Serverauswahl paged schon über 45 Einträge, weil die Navigationszeile mit dem Sortier-Item immer da ist (sonst ließe sich ab 46 Diensten nicht sortieren; mit 54 ohne Navigationszeile gäbe es keinen Platz dafür).
- **Alternativen:** Kopieren (wären rund zehn Zeilen in zwei Features, das Seitenende würde auseinanderlaufen). Gemeinsame Zeilenregel (hätte verschiedene Bedeutung, würde den Switcher verändern). Verworfen.
- **Commit:** `refactor(core): share page slicing with the lobby switcher` vor dem Feature-Commit; Verhalten des Switchers bleibt, bestehende `SwitcherPagingTest` ist das Regressionsnetz.
- **Test:** `PaginationTest` (JUnit, rein): Seitenzahl für 0, 1, 45, 46 und 90 Einträge; Ausschnitt der letzten, halben und einer über dem Ende liegenden Seite; Klammerung der Seitennummer.

### D3 Ansichten statt Zustand je Spieler

- Eine **Ansicht** ist `PickerView(PickerSort sort, int page, int rows)`. `SharedServerPicker` hält je Ziel und Sprache eine `Map<PickerView, GlobalInventoryBuilder>`. Jede Ansicht hat ein eigenes geteiltes Inventar mit Titel in der Sprache.
- **Kein Zustand je Spieler.** Der Spieler sieht die Ordnung und Seite des Inventars, das er offen hat. Der Klick-Handler der Navigationszeile kennt Ordnung und Seite der Ansicht, auf der er liegt, und berechnet daraus die nächste Ansicht. Die Lobby speichert nichts, und beim Verlassen bleibt nichts zurück (Anforderung „Ordnung und Seite gelten nur für das offene Inventar“).
- **Alternativen:**
  - Map `Spieler-UUID -> Zustand`: braucht Aufräumen bei Verlassen und Schließen, und die Anforderung „keine Referenz nach dem Verlassen“ wäre nur mit Zusatzlogik zu halten.
  - Je Spieler ein Inventar (`PersonalInventoryBuilder`): ein Listener je Öffnung, bricht die Lifecycle-Anforderung.
  - Ein geteiltes Inventar je Ziel mit Inhalt für alle Ordnungen: der Inhalt ist geteilt, also würde die Ordnung eines Spielers die eines anderen überschreiben.
- **Begrenzung:** Die Zahl der Inventare je Ziel und Sprache ist durch Ordnungen mal Zeilen- und Seitenzahl begrenzt, nicht durch Öffnungen. Unseitige Ansichten gibt es höchstens 4 × 5 (Zeilen 2 bis 6), je Seite einer langen Liste eine weitere. Eine Ansicht wird beim ersten Bedarf registriert und danach wiederverwendet; `stop()` entfernt alle.
- **Test:** Cyano-`Env` (`NavigatorPickerLifecycleTest` erweitert): Öffnen 20 Mal bleibt die Zahl der Listener gleich; nach einem Durchlauf aller Ordnungen bleibt sie beim zweiten Durchlauf gleich; Stoppen setzt alle Klick-Listener auf 0. SOLID: OCP, weil neue Ansichten als Schlüssel dazukommen, ohne den Pfad zu ändern.

### D4 Ordnungen als Enum mit Vergleichern

- `PickerSort` (Enum, Reihenfolge = Klickfolge): `NAME`, `MOST_PLAYERS`, `MOST_FREE`, `STATUS`. `next()` zählt eins weiter und springt von `STATUS` auf `NAME`. Jede Ordnung hat einen `Comparator<ServiceCount>`:
  - `NAME`: `Comparator.comparing(ServiceCount::name)`.
  - `MOST_PLAYERS`: Spielerzahl absteigend, dann Name.
  - `MOST_FREE`: zuerst Dienste mit gemeldetem Limit (`limitAnnounced`), dann freie Plätze (`max - online`) absteigend, dann Name.
  - `STATUS`: Rang nach Zustand, dann Name. Die Zustandsreihenfolge steht als explizite Liste `STATUS_ORDER = List.of(JOINABLE, NOT_READY, FULL)` in `ServerPicker`, nicht über die Enum-Reihenfolge von `State`. `navigator-ingame-servers` hängt `INGAME` hinten an diese Liste.
- **Alternativen:** Sortierung per Zahl im Item-Namen oder per Textschlüssel (verworfen, ohne Typsicherheit). Konfigurierbare Ordnungen (verworfen, Nicht-Ziel).
- **SOLID:** OCP, weil eine neue Ordnung eine Zeile im Enum ist. Der Klick-Pfad ruft nur `next()` auf.
- **Test:** `PickerSortTest` (JUnit, rein): jede Ordnung mit Gleichstand; „ohne Limit zuletzt“ bei `MOST_FREE`; `STATUS`-Rang für alle drei Zustände; vier Schritte von `next()` führen zu `NAME`.

### D5 Lesen nur auf Klick, kein Timer

- Öffnen, Ordnungswechsel, Seitenwechsel und Klick auf einen Eintrag lesen die Dienste über denselben Weg wie heute `readServices`: auf dem Executor lesen, Entscheidung und Anzeige auf dem nächsten Tick, Doppelklick-Schutz `choosing`.
- **Keine periodische Aktualisierung** der Serverauswahl: kein `Task`, keine Zählung der Betrachter. Die Anforderung verlangt das ausdrücklich, und die Klicks lesen ohnehin neu.
- **Alternative:** Wechsel aus dem Schnappschuss des Öffnens, ohne Lesen. Verworfen: der Klick auf einen Eintrag liest ohnehin neu, aber Blättern und Sortieren würden auf veraltete Dienste zeigen (die Anforderung „Seitenwechsel liest neu“).
- **Schutz:** Während ein Lesen läuft, wird ein weiterer Klick des Spielers ignoriert (wie heute).
- **Test:** Cyano-`Env` mit Fake-`PlayerCounts` und explizitem `env.tick()`: Klick auf Sortier-Item liest einmal neu; Klick vor Abschluss des Lesens wird ignoriert.

### D6 Layout

- `PickerLayout(int rows, int pages)` (rein): `pages = Pagination.pageCount(n)` wenn `n > 45`, sonst `1`. Zeilen: mit mehreren Seiten immer 6; ohne Seiten `min(5, max(1, ceil(n / 9))) + 1`, also Inhaltsreihen plus Navigationszeile. Ein leerer Dienststand bekommt eine Inhaltsreihe mit dem Hinweis in der Mitte (`EMPTY_SLOT`) und die Navigationszeile.
- Navigationszeile (Startslot `(Zeilen − 1) × 9`): Vorige-Pfeil im ersten Slot (nur ab Seite 2), Sortier-Item in der Mitte, also Start + 4 (immer), Nächste-Pfeil im letzten Slot, also Start + 8 (nur wenn eine Seite folgt). Auf einer Seite mit sechs Reihen sind das Slots 45, 49 und 53; eine Seitenanzeige als eigenes Item gibt es nicht mehr, die Seite steht in der Lore des Sortier-Items und in den Pfeilnamen. Freie Plätze der Navigationszeile sind graue Glasscheiben ohne Namen.
- **Zentrierung:** `PickerLayout.serviceSlot(index, count)` ist die einzige Stelle, die den Slot eines Eintrags auf seiner Seite bestimmt; Rendern und Klicks nutzen denselben Wert. Eine Reihe mit `n` Einträgen beginnt in Spalte `(9 − n) / 2` (ganzzahlig), volle Reihen bleiben unverändert. Die Zählung gilt je Seite, also auch für die letzte Reihe einer Seite. Beispiel: ein Dienst in Spalte 4, zwei in den Spalten 3 und 4.
- **Test:** `PickerLayoutTest` (JUnit, rein): Zeilen für 0, 1, 9, 10, 45 und 46 Dienste; Seiten für 45, 46, 90; Slots der Navigation je Seite; Zentrierung für 1, 2, 8, 9, 10, 45 Einträge und für eine letzte Seite mit 14 Einträgen. Env-Tests: ein Dienst in Slot 4 und Sortier-Item in Slot 13 ohne Pfeile; Pfeile an 45 und 53 mit Sortier-Item in 49; Klick auf den zentrierten Eintrag verbindet mit dem richtigen Dienst.

### D7 Klick-Behandlung

- **Sortier-Item:** benannter Klick → Lesen → nächste Ordnung, Seite 0 → Ansicht zeigen (Inhalt und Layout des Inventars neu setzen, wie heute `show`).
- **Pfeile:** benannter Klick → Lesen → Zielseite auf den Bereich der neuen Seitenzahl klammern → Ansicht zeigen.
- **Eintrag:** wie heute (`pick` → `readServices` → `decidePick`), unverändert.
- Anzeige: `player.openInventory(...)` mit dem Inventar der Ziel-Ansicht. Weil die Serverauswahl keine Betrachter zählt, braucht sie keine Logik für den Inventarwechsel innerhalb desselben Menüs.
- **Fehler:** Schlägt das Lesen fehl, zeigt die Lobby die bestehende Meldung „nicht verfügbar“ und lässt das Inventar offen. Sie tauscht es nicht.

### D8 Texte

- Neue Schlüssel in `NavigatorMessages` (in `KEYS` aufgenommen), übersetzt in `messages_en.properties` und `messages_de.properties`:
  - `titan.navigator.picker.sort.item` (Name des Items mit aktiver Ordnung), `…sort.name`, `…sort.most_players`, `…sort.most_free`, `…sort.status`.
  - `…sort.hint` (Klickzeile in der Art der Klickzeilen des Navigators).
  - `…page.previous`, `…page.next`: Pfeilnamen mit „· Seite x/y“ der Zielseite; `…page.indicator` (Platzhalter für Seite und Gesamtzahl): „Seite x/y“ in der Lore des Sortier-Items und als Teil der Pfeilnamen. Keine Schlüssel entfallen.
- Die aktive Ordnung in der Lore: Pfeilmarkierung und Farbe; die übrigen grau.
- Der Test `NavigatorMessagesBundleTest` prüft Gleichheit der Schlüssel; `ServerPickerStyleTest` die Lore-Zeilen.

### D9 Telemetrie und Logs

Keine neuen Spans, Zähler oder Log-Zeilen. Das Öffnen der Serverauswahl und der Eintrag bleiben wie bisher in `NavigatorTelemetry`. Sortier- und Seitenklicks sind ein Nicht-Ziel (siehe proposal).

## Risks / Trade-offs

- **[Viele Ansichten]** → Begrenzt auf Ordnungen mal Zeilen- und Seitenzahl je Ziel und Sprache; beim ersten Bedarf angelegt, bei `stop()` entfernt. Die Zahl wächst mit der Dienstzahl nur in Seiten (ab 46 Diensten je 45).
- **[Eine Zeile mehr]** → Die Navigationszeile ist immer da. Sichtbare Änderung, die die Anforderung ausdrücklich verlangt.
- **[Geteilter Inhalt je Ansicht]** → Zwei Spieler auf derselben Ansicht sehen denselben Stand, den der letzte Klick gelesen hat. Das gleicht der heutige Pfad, und jeder Eintrag prüft beim Klick neu.
- **[Lesen je Klick]** → Jeder Ordnungs-, Seiten- oder Eintragsklick liest die Dienste einmal im Speicher (Bridge-Snapshot wie heute beim Öffnen). Kein zusätzlicher Netzwerkzugriff gegenüber dem bisherigen Klick.
- **[Schwelle 45 in der Serverauswahl, 54 im Switcher]** → Bewusst verschieden (D2). Die Seitengröße ist gleich, damit die Bedienung gleich bleibt.
- **[Geteilte Ansichten, Klick während des Umbaus]** → Ein Inventar gehört allen Betrachtern derselben Ansicht. Öffnet ein anderer Spieler dieselbe Ansicht, nachdem sich die Dienstliste geändert hat, zeichnet Aves sie für alle Betrachter neu. Innerhalb einer Umlaufzeit kann ein Klick deshalb auf den verschobenen Platz treffen und einen Nachbardienst desselben Minispiels verbinden. Der Eintrag prüft beim Klick die Beitrittsfähigkeit neu; ein Dienst eines anderen Minispiels ist ausgeschlossen, weil jede Liste nur ihr Minispiel zeigt. Akzeptiert: das Fenster ist eine Umlaufzeit, und das Ergebnis ist harmlos.
- **[Verschieden lange Zeilen je Ansicht]** → Eine Ansicht mit gleichem Namen aber anderer Zeilenzahl ist ein anderes Inventar. Ein Spieler, der während eines Blätterns eine andere Zeilenzahl sieht, wechselt beim nächsten Klick das Inventar; das ist sichtbar, aber korrekt.
