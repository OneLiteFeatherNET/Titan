# Tasks

Zweig: `feat/navigator-grouped-design` von `origin/main`, nachdem `navigator-staging-on-demand` in `main` steht. Vor dem Abhaken einer Aufgabe läuft der Modultest `./gradlew :features:navigator:test`; vor dem Abschluss `./gradlew build`. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, Env-Ticks explizit.

## 1. Tests zuerst (rot)

- [ ] 1.1 Unit (`NavigatorLayoutTest`): Zentrierung der Gruppen. Minispiele auf 10 bis 16 für n = 1 (Platz 13), n = 2 (12, 13), n = 3 (12 bis 14), n = 4 (11 bis 14); Server auf 19 bis 25 für n = 1 (Platz 22), n = 2 (21, 22), n = 3 (21 bis 23); n = 0 belegt nichts. Nachweis: rot vor 2.2.
- [ ] 1.2 Unit (`NavigatorLayoutTest`): Rahmen. Reihe 0, Reihe 3 und die Spalten 0 und 8 haben dasselbe Material (schwarze Glasscheibe) mit leerem Namen; keine helle oder cyane Scheibe mehr. Nachweis: rot vor 2.2.
- [ ] 1.3 Unit (`NavigatorLayoutTest`): innere Plätze ohne Ziel sind leer, es gibt keine graue Füllscheibe. Spawn auf 31 und Hub auf 35 ersetzen ihre Rahmenscheibe. Nachweis: rot vor 2.2.
- [ ] 1.4 Unit (`NavigatorMessagesTest`): Statuszeile mit grünem Punkt bei mehr als null Spielern, grauem Punkt bei null; Beschreibungszeile je Ziel in beiden Sprachen nicht leer; keine Trennlinie; Reihenfolge Statuszeile, Beschreibung, Leerzeile, Klickzeilen. Nachweis: rot vor 2.3.
- [ ] 1.5 Unit (`NavigatorItemsTest`): Stapelanzahl ist die Spielerzahl, geklemmt auf 1 bis 64 (0 gibt 1, 100 gibt 64); Glanz nur bei mehr als null Spielern. Nachweis: rot vor 2.3.
- [ ] 1.6 Integration (`NavigatorRenderedLayoutTest`, Env mit `FakeTaskPlayers`, Ticks explizit): ein Takt aktualisiert Zahl, Anzahl und Glanz; zwischen zwei Takten bleibt der Stand; Hub auf 35 nur mit Staging-Recht; Titel „✦ Navigator ✦“. Nachweis: rot vor 2.4.

## 2. Umsetzung (grün)

- [ ] 2.1 `Destination`: Hauptmenü-Ziele tragen eine Gruppe (MINIGAME, SERVER) statt eines festen Platzes; die Staging-Einträge behalten ihre Plätze im Untermenü. Prüfung: bestehende Destination-Tests grün, soweit sie nicht die alten Hauptmenü-Plätze prüfen.
- [ ] 2.2 `NavigatorLayout`: reine Berechnung der Gruppenplätze nach der Zentrierungsformel, Rahmenmaterial `BLACK_STAINED_GLASS_PANE` ohne Namen, leere innere Plätze. Prüfung: 1.1 bis 1.3 grün.
- [ ] 2.3 `NavigatorMessages` und `NavigatorItems`: Statuszeile, Beschreibungszeile, Trennlinie entfällt, Stapelanzahl und Glanz aus derselben Spielerzahl. Schlüssel für Beschreibungen und Statuszeile in `messages_de.properties` und `messages_en.properties`; der Titel „✦ Navigator ✦“ als MiniMessage-Verlauf. Prüfung: 1.4 und 1.5 grün.
- [ ] 2.4 `NavigatorModule` und `SharedNavigator`: der bestehende Takt setzt Zahl, Anzahl und Glanz neu; kein neuer Listener, kein neuer Takt. Prüfung: 1.6 grün; `NavigatorModuleLeakTest` grün.
- [ ] 2.5 Refactor: alte feste Plätze der Hauptmenü-Ziele (11, 13, 14, 15, 16, 20, 22, 24) und die grauen Füllscheiben aus Tests und Code entfernen; alle Modultests grün.

## 3. Verifikation und Doku

- [ ] 3.1 `./gradlew spotlessApply` und `./gradlew build` sind grün; der Build ist die Grundlage für das Abhaken der Aufgaben in 1 und 2.
- [ ] 3.2 Manuelle Abnahme auf einem lokalen Testserver mit einer Kopie des Jars (nicht im laufenden Worktree gebaut): Gruppen zentriert bei wechselnden Rechten, Anzahl und Glanz nach einem Takt, Hub auf Platz 35.
- [ ] 3.3 `README.md` und `docs/lobby-modules.md`: Abschnitt zum Navigator beschreibt das neue Layout (Gruppen, Rahmen, Hub auf 35). Prüfung: `grep -n "Platz 35" docs/lobby-modules.md` trifft den Abschnitt.
- [ ] 3.4 Commits je Typ: Umsetzung als `feat(navigator)`, Doku als `docs(navigator)`. Keine Datei unter `openspec/` im Code-Pull-Request.

## 4. Pull Request öffnen

- [ ] 4.1 Code-Pull-Request mit dem Titel `feat(navigator): group the navigator menu and show live activity` von einem Zweig aus dem aktuellen `origin/main` (der die gemergte Vorschlags-PR enthält) öffnen. Die Änderung ist nicht brechend.
