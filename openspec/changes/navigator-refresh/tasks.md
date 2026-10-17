# Tasks

Zweig: `feat/navigator-refresh`, Basis `main` (der Stapel auf `feat/navigator-slender-permission` / PR #425 ist als `1b3adae` in `main` gelandet). Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

## 1. Tests zuerst (rot)

- [x] 1.1 `QuickJoinTest` (rein): bereit und nicht voll, meiste Spieler zuerst, Gleichstand nach Name, volle und nicht bereite Dienste werden übersprungen, keiner passt zu leer. Nachweis: Test schlägt fehl, bevor `QuickJoin` existiert.
- [x] 1.2 `ClickActionTest` (rein): Links, Rechts, Shift-Links und Shift-Rechts für Minispiel und Server, mit und ohne Portal; die Hinweisliste je Kombination in Reihenfolge.
- [x] 1.3 `NavigatorLayoutTest` (rein): Rahmenplätze, Parität der Farben, Innenplätze, Ziele 11, 15, 20, 22, 24 und 31 liegen richtig, Spawn ersetzt die Rahmenscheibe auf 31.
- [x] 1.4 `NavigatorLoreTest` (Env): Lore von ElytraRace und Survival auf Deutsch und Englisch, Reihenfolge der Zeilen, Trennlinie, Spielerzeile mit und ohne bekanntes Maximum, Leerzeile, Portal-Zeile nur mit Portal.
- [x] 1.5 `ServerPickerStyleTest`: Titel übersetzt, Eintrag im Verlauf des Ziels, Spielerzeile, Zustandszeile in Rot, Gelb oder Grün, Ablehnung übersetzt.
- [x] 1.6 Bestehende Tests an Plätze und Lore anpassen (Destination, Telemetry, Portal, Permission, Picker, Player-Count, Click-Hint, Leak). Nachweis: Kompilierfehler oder rote Assertions auf neue Plätze vor der Umsetzung.

## 2. Umsetzung

- [x] 2.1 `NavigatorLayout` (Rahmen, Parität, Größe) und `CHEST_4_ROW` in `SharedNavigator`; Plätze in `Destination` auf 11, 15, 20, 22, 24.
- [x] 2.2 `ClickAction` (Klickzuordnung und Hinweisliste) und `QuickJoin` (Auswahl) als reine Klassen; `NavigatorModule` nutzt sie in der Klickbehandlung.
- [x] 2.3 `NavigatorMessages`: Schlüssel für Tasten, Aktionen, Spielerzeile, Titel und Serverauswahl; `modeLore` über den GlobalTranslator; `Destination.item` bekommt die fertige Lore.
- [x] 2.4 `TaskPlayers.capacity` (Summe der Maxima, nur wenn alle Dienste ein Limit melden).
- [x] 2.5 `ServerPicker` und `SharedServerPicker`: übersetzter Titel, Einträge im Verlauf, Zustandszeile; je Ziel und Sprache ein geteiltes Inventar; `opensFor` entfällt.
- [x] 2.6 Bundles `messages_de.properties` und `messages_en.properties` mit identischen Schlüsseln.
- [x] 2.7 Menü: je Kombination aus Rechten und Sprache beim Start mit Titel der Sprache eingerichtet (acht Inventare bleiben).
- [x] 2.8 `NavigatorItems`: Verlauf für den Feather-Namen; Spawn-Name im Verlauf der Lobby.

## 3. Doku

- [x] 3.1 `docs/lobby-modules.md` und `README.md`: Layout, Klickzuordnung, Schnellbeitritt, Shift-Klick auf das Portal statt Rechtsklick.

## 4. Verifikation

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` sind grün.

## 5. Pull Request

- [ ] 5.1 Pull Request auf die Basis `main` mit dem Titel `feat(navigator): refresh the navigator with gradients, rows and quick join` (Titel und Beschreibung Englisch). Die Beschreibung nennt das Layout, die Klicktabelle, die Regel für den Schnellbeitritt, die Verhaltensänderung des Rechtsklicks und den Testplan. Die CI läuft grün.

## 6. Korrekturen aus dem Review

- [x] 6.1 Nur Links- und Rechtsklick, mit oder ohne Shift, wirken (`ClickAction.of` liefert ein `Optional`, `ClickAction.isNamed`). Alle anderen Klicktypen werden abgebrochen und tun nichts, auch die Einträge der Serverauswahl. Nachweis: Zahlentaste, Q, F, Mittelklick und Doppelklick schlugen auf den Assertions fehl, bevor die Umsetzung existierte.
- [x] 6.2 Lesefehler und leere Liste sind getrennt (`TaskPlayers.services` liefert ein `Optional`). Ein Lesefehler wird im Chat abgelehnt und öffnet keine Serverauswahl; eine leere Serverauswahl zeigt „Kein Server läuft gerade.“.
- [x] 6.3 Die Auswahl liest den Dienst beim Klick neu und lehnt ab, was inzwischen voll, nicht bereit oder nicht mehr gelistet ist.
- [x] 6.4 Lebenszyklus-Tests der Serverauswahl: wiederholtes Öffnen registriert keine Listener, `stop()` entfernt jede geöffnete Serverauswahl (`NavigatorPickerLifecycleTest`).
- [x] 6.5 Testnamen und Doku: `navigatorSpawnEntryEndsTheRunAndReturnsThePlayer`, `shiftClickOnMinigameWithoutPortalActsLikeTheUnshiftedClick`, Feather-Name in `docs/lobby-modules.md`, Javadoc-Umbrüche, Klickbeschreibung in `README.md`.
- [x] 6.6 `./gradlew spotlessApply` und `./gradlew build` sind grün.
