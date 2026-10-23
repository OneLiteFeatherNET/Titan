# Tasks

Commits: `docs(openspec): propose lobbyswitcher-back` (diese Artefakte), danach `feat(lobbyswitcher): add a back button to the switcher` (Code, Tests, Texte, Dokumentation). Ein Typ je Commit.

Vorbedingung fürs Archivieren: `lobby-switcher` ist zuerst archiviert. Erst dann existiert `openspec/specs/lobby-switcher/spec.md`, auf das `MODIFIED` und `RENAMED` zeigen (`openspec validate --strict` meldet das bis dahin als Archivfehler).

## 1. Layout-Mathematik

- [x] 1.1 Test zuerst (Unit, `SwitcherLayoutTest`): Zeilen gesamt (inkl. Navigationszeile) und Seiten für 0, 1, 9, 10, 45, 46 und 90 Einträge; Navigationszeile liegt in der letzten Zeile. Rot.
- [x] 1.2 Test zuerst (Unit, `SwitcherLayoutTest`): Slots der Navigationszeile je Zeilenzahl (Schließen Start + 0, Vorige + 3, Seitenanzeige + 4, Nächste + 5); bei einer Seite ohne Anzeige. Rot.
- [x] 1.3 `SwitcherLayout` anpassen: Schwelle über `Pagination.PAGE_SIZE`, Zeilen inklusive Navigationszeile, Slot-Methoden statt fester Konstanten. Grün.

## 2. Navigationszeile im Inventar

- [x] 2.1 Test zuerst (Integration, Cyano-`Env`, `SwitcherNavigationTest`): Eine Seite mit drei Lobbys hat den Schließen-Knopf in Slot 9, keine Pfeile, keine Seitenanzeige. Rot.
- [x] 2.2 Test zuerst (Integration): Klick auf den Schließen-Knopf schließt das Inventar; danach läuft die Aktualisierung nicht weiter (keine Abfrage über mehrere Perioden). Rot.
- [x] 2.3 Test zuerst (Integration): Auf Seite 2 von 3 liegen Vorige in Start + 3, Seitenanzeige in Start + 4 und Nächste in Start + 5; auf der letzten Seite fehlt Nächste. Rot.
- [x] 2.4 Test zuerst (Integration): Pfeil-Klicks in Start + 3 und Start + 5 wechseln die Seite; ein Klick auf Start + 3 auf Seite 1 tut nichts. Rot.
- [x] 2.5 Test zuerst (Integration): Ein Klick auf einen Eintrag der zweiten Seite joint genau diesen Dienst; die Aktualisierung läuft weiter. Rot, falls die Verschiebung der Pfeile die Zuordnung verschiebt.
- [x] 2.6 `SwitcherInventory` anpassen: Navigation immer, Schließen-Knopf mit `closeInventory()`, Pfeile über die Layout-Slots, Unavailable-Zustand mit Navigationszeile. Grün.

## 3. Texte

- [x] 3.1 Test zuerst (Unit, `LobbySwitcherMessagesBundleTest`): `titan.lobbyswitcher.close` steht in `KEYS` und in beiden Bundles. Rot.
- [x] 3.2 `messages_en.properties` (`Close`) und `messages_de.properties` (`Schließen`) ergänzen, `LobbySwitcherMessages.CLOSE` aufnehmen. Grün.
- [x] 3.3 Test zuerst (Integration): Der Knopf erscheint deutsch für `de_DE`. Rot, dann grün.

## 4. Dokumentation und Nachweise

- [x] 4.1 `README.md`: Beschreibung des Switchers um Navigationszeile und Schließen-Knopf ergänzen.
- [x] 4.2 `docs/lobby-modules.md`: Abschnitt zum Switcher-Layout (Zeilen, Slots, Schwelle 45) ergänzen; Verweis auf `Pagination`.
- [x] 4.3 `./gradlew spotlessApply` und `./gradlew build` grün.
- [x] 4.4 `openspec validate lobbyswitcher-back --strict` grün; `openspec validate lobby-switcher --strict` grün.
- [ ] 4.5 PR öffnen: Titel `feat(lobbyswitcher): add a back button to the switcher`; englische Beschreibung mit den ASCII-Layouts; CI bis grün beobachten. Nicht mergen.
