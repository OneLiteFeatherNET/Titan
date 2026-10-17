# Tasks

Zweig: `feat/navigator-refresh`, gestapelt auf `feat/navigator-slender-permission` (PR #425). Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

## 1. Tests zuerst (rot)

- [ ] 1.1 `QuickJoinTest` (rein): bereit und nicht voll, meiste Spieler zuerst, Gleichstand nach Name, volle und nicht bereite Dienste werden übersprungen, keiner passt zu leer. Nachweis: Test schlägt fehl, bevor `QuickJoin` existiert.
- [ ] 1.2 `ClickActionTest` (rein): Links, Rechts, Shift-Links und Shift-Rechts für Minispiel und Server, mit und ohne Portal; die Hinweisliste je Kombination in Reihenfolge.
- [ ] 1.3 `NavigatorLayoutTest` (rein): Rahmenplätze, Parität der Farben, Innenplätze, Ziele 11, 15, 20, 22, 24 und 31 liegen richtig, Spawn ersetzt die Rahmenscheibe auf 31.
- [ ] 1.4 `NavigatorLoreTest` (Env): Lore von ElytraRace und Survival auf Deutsch und Englisch, Reihenfolge der Zeilen, Trennlinie, Spielerzeile mit und ohne bekanntes Maximum, Leerzeile, Portal-Zeile nur mit Portal.
- [ ] 1.5 `ServerPickerStyleTest`: Titel übersetzt, Eintrag im Verlauf des Ziels, Spielerzeile, Zustandszeile in Rot, Gelb oder Grün, Ablehnung übersetzt.
- [ ] 1.6 Bestehende Tests an Plätze und Lore anpassen (Destination, Telemetry, Portal, Permission, Picker, Player-Count, Click-Hint, Leak). Nachweis: Kompilierfehler oder rote Assertions auf neue Plätze vor der Umsetzung.

## 2. Umsetzung

- [ ] 2.1 `NavigatorLayout` (Rahmen, Parität, Größe) und `CHEST_4_ROW` in `SharedNavigator`; Plätze in `Destination` auf 11, 15, 20, 22, 24.
- [ ] 2.2 `ClickAction` (Klickzuordnung und Hinweisliste) und `QuickJoin` (Auswahl) als reine Klassen; `NavigatorModule` nutzt sie in der Klickbehandlung.
- [ ] 2.3 `NavigatorMessages`: Schlüssel für Tasten, Aktionen, Spielerzeile, Titel und Serverauswahl; `modeLore` über den GlobalTranslator; `Destination.item` bekommt die fertige Lore.
- [ ] 2.4 `TaskPlayers.capacity` (Summe der Maxima, nur wenn alle Dienste ein Limit melden).
- [ ] 2.5 `ServerPicker` und `SharedServerPicker`: übersetzter Titel, Einträge im Verlauf, Zustandszeile; je Ziel und Sprache ein geteiltes Inventar; `opensFor` entfällt.
- [ ] 2.6 Bundles `messages_de.properties` und `messages_en.properties` mit identischen Schlüsseln.
- [ ] 2.7 Menü: je Kombination aus Rechten und Sprache beim Start mit Titel der Sprache eingerichtet (acht Inventare bleiben).
- [ ] 2.8 `NavigatorItems`: Verlauf für den Feather-Namen; Spawn-Name im Verlauf der Lobby.

## 3. Doku

- [ ] 3.1 `docs/lobby-modules.md` und `README.md`: Layout, Klickzuordnung, Schnellbeitritt, Shift-Klick auf das Portal statt Rechtsklick.

## 4. Verifikation

- [ ] 4.1 `./gradlew spotlessApply` und `./gradlew build` sind grün.

## 5. Pull Request

- [ ] 5.1 Pull Request auf die Basis `feat/navigator-slender-permission` mit dem Titel `feat(navigator): refresh the navigator with gradients, rows and quick join` (Titel und Beschreibung Englisch). Die Beschreibung nennt den gestapelten Stand auf #425, das Layout, die Klicktabelle, die Regel für den Schnellbeitritt, die Verhaltensänderung des Rechtsklicks und den Testplan. Die CI läuft grün.
