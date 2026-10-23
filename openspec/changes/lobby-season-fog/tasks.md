# Tasks

## 1. Vorlauf

- [x] 1.1 OpenSpec-Artefakte (`proposal.md`, `specs/lobby-seasons/spec.md`, `design.md`, `tasks.md`) schreiben und mit `openspec validate lobby-season-fog --strict` prüfen. Commit `docs(openspec): propose lobby-season-fog`.
- [x] 1.2 Quellwerte prüfen: Mischung aus `BRIGHT` und der Granskoga-Atmosphäre mit `share 0.3` in `float`-Arithmetik nachrechnen (Ergebnis in `design.md` D4 und in der Spec-Tabelle).

## 2. Konfiguration und Auswahl (`core`, `features/season`)

- [x] 2.1 Test zuerst (Unit, `LobbyAtmosphereTest` in `core`): `fromKey("slender-fog")` liefert `SLENDER_FOG`; unbekannte und leere Schlüssel liefern leer. Verifikation: rot, dann grün mit dem Enum.
- [x] 2.2 Test zuerst (Unit, `SeasonAtmospheresTest`): ohne Feld leer; `slender-fog` liefert den Wert; Leerzeichen werden entfernt; unbekannter Wert liefert leer und loggt genau eine WARN-Zeile mit `seasons.halloween.atmosphere` und `slender-fog` (ListAppender). Verifikation: rot, dann grün.
- [x] 2.3 Test zuerst (Unit, `SeasonWorldChoiceTest` erweitert): aktive Saison mit Atmosphäre liefert sie; aktive Saison ohne Feld liefert leer; inaktive Saison mit Atmosphäre liefert leer; die Welt bleibt wie bisher. Verifikation: rot, dann grün.
- [x] 2.4 `LobbyWorldChoice` um die Default-Methode `atmosphere()` erweitern; `SeasonWorldChoice` implementiert sie über `SeasonAtmospheres`. Die Column bleibt bei `core` (`ColumnArchitectureTest` grün).

## 3. Dimension (`common`)

- [x] 3.1 Test zuerst (Unit, `LobbyDimensionsTest`): die Konstanten entsprechen der Mischung aus den Quellwerten (BRIGHT und Granskoga, `share 0.3f`, Ambient aus `dim(fog, 0.10)`), mit `float`-Arithmetik nachgerechnet im Test. Verifikation: rot, dann grün.
- [x] 3.2 `LobbyDimensions` mit den Konstanten und `register(Optional<LobbyAtmosphere>)`: ohne Atmosphäre `DimensionType.OVERWORLD`, sonst die registrierte Dimension mit `defaultClock` der Overworld und `SUN_ANGLE`/`MOON_ANGLE` = 180.

## 4. Start (`runtime`)

- [x] 4.1 Test zuerst (Env, `LobbyDimensionRegistrationTest` in `runtime`; Saisonfenster per `SeasonLobbyWorldTest` in `apps/cloudnet`): mit aktiver Saison und `slender-fog` nutzt die Lobby-Instanz eine registrierte Dimension mit dem Nebel und der Overworld-Uhr; ohne Atmosphäre oder außerhalb des Fensters die Overworld. Verifikation: rot (Kompilierfehler der neuen Klassen), dann grün.
- [x] 4.2 Die bestehenden Tests zur Weltwahl (`LobbyWorldResolutionTest`, `LobbyPortalsBeanTest`) bleiben grün; mehr als eine Auswahl bricht weiter ab, die Atmosphäre nutzt denselben Weg (`onlyChoice`).
- [x] 4.3 `PlatformBeans`: `onlyChoice(...)` herausziehen; `instanceContainer()` liest die Atmosphäre der Auswahl und legt die Instanz mit `LobbyDimensions.register(...)` an.

## 5. Dokumentation und Konfiguration

- [x] 5.1 `features/season/src/main/resources/titan/defaults/season.yaml`: Kommentar für `atmosphere` mit den gültigen Werten.
- [x] 5.2 `README.md` (Abschnitt „Seasonal lobby world“) und `docs/lobby-modules.md` (Abschnitt „Saison-Welt“): Beispiel `halloween` mit `atmosphere: slender-fog`, Hinweis auf eigene Welt unter `worlds/`, Start-Verhalten (Atmosphäre wirkt beim Start), unbekannter Wert wird geloggt.
- [x] 5.3 Spec-Delta mit der Implementierung abgleichen (Szenario-Titel, Werte, Reihenfolge der Registrierung).

## 6. Prüfung

- [x] 6.1 `./gradlew spotlessApply` und `./gradlew build` grün; `openspec validate lobby-season-fog --strict` grün.
- [ ] 6.2 Sichtprüfung auf einem Testserver mit einer Saison und `slender-fog`: Nebel sichtbar, Tageszeit wie ohne Nebel. Nicht ausgeführt (kein Testserver in dieser Sitzung); im PR als offen vermerkt.

## 7. Commits und Pull Request

- [x] 7.1 Commit `feat(season): add the slender fog as a season atmosphere` (Code, Tests, Dokumentation, abgehakte Aufgaben).
- [ ] 7.2 Pull Request öffnen mit dem Titel `feat(season): add the slender fog as a season atmosphere`. Beschreibung auf Englisch: Quelle der Werte, Konfigurationsbeispiel für `halloween`, Aktivierung auf dem Server, Verhalten bei Saisonwechsel, Testnachweis. Abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
