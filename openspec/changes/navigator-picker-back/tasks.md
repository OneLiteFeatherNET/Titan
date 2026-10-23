# Tasks

Basis: `origin/main` (Release 4.1.1), Branch `feat/navigator-picker-back`. Jeder Commit trägt nur einen Typ. Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`.

## 1. Tests zuerst (rot)

- [x] 1.1 Unit (`PickerLayoutTest`): Slots der Navigationszeile für eine Inhaltsreihe (zwei Reihen gesamt, Zurück 9, Sortier 13), zwei Inhaltsreihen (drei Reihen, Zurück 18, Vorige 21, Sortier 22, Nächste 23) und sechs Reihen mit Seiten (Zurück 45, Vorige 48, Sortier 49, Nächste 50). Verifikation: `./gradlew :features:navigator:test --tests '*PickerLayoutTest'` zeigt die Fehlschläge.
- [x] 1.2 Env (`NavigatorPickerViewsTest`, `NavigatorPickerPagingTest`, `NavigatorPickerLifecycleTest`): Pfeile auf 48 und 50, Sortier-Item auf 49 und 13; Zurück-Taste sichtbar auf Seite 1, 2 und 3, Zurück auf Slot 9 bei einer Reihe, mit Name „Zurück zum Navigator“. Verifikation: rot.
- [x] 1.3 Env (`NavigatorServerPickerTest`): Zurück auf Seite 1 öffnet den Navigator mit dem Menü der Rechte; Zurück auf Seite 2; Zurück bei leerer Liste; entzogenes Recht vor dem Zurück zeigt das Menü ohne Slender; deutscher Client erhält das deutsche Menü; ein Dienst bleibt in Slot 4 und die Zurück-Taste erscheint. Verifikation: rot.
- [x] 1.4 Env (`NavigatorPickerLifecycleTest`): 20 Mal Serverauswahl öffnen und per Zurück-Taste den Navigator öffnen; jedes Mal ist der Navigator offen, und die Listener am Modul-Knoten und an der Serverauswahl bleiben gleich. Verifikation: rot, weil es die Zurück-Taste noch nicht gibt.
- [x] 1.5 Unit (`NavigatorMessagesBundleTest`): `titan.navigator.picker.back` steht in `KEYS` und in beiden Bundles. Verifikation: rot.

## 2. Umsetzung (grün)

- [x] 2.1 `PickerLayout`: `backSlot()` (Start + 0); `previousSlot()` auf Start + 3, `nextSlot()` auf Start + 5; `sortSlot()` bleibt Start + 4. Verifikation: 1.1 grün.
- [x] 2.2 `NavigatorMessages.PICKER_BACK` in `KEYS`; `messages_en.properties` „Back to the navigator“, `messages_de.properties` „Zurück zum Navigator“; `ServerPicker.backItem(locale)` mit `Material.DARK_OAK_DOOR`. Verifikation: 1.5 grün.
- [x] 2.3 `SharedServerPicker`: `Clicks.back(Player)`; die Zurück-Taste auf `backSlot` in jeder Ansicht, auch bei leerer Liste, über `button` (nur benannte Klicks). Verifikation: 1.2 grün.
- [x] 2.4 `NavigatorModule.PickerClicks.back` ruft `open(player)`; keine eigene Rechte- oder Sprachauswahl. Verifikation: 1.3 und 1.4 grün.
- [x] 2.5 Prüfung: kein `Thread.sleep`, keine Systemzeit in den neuen Tests; `./gradlew :features:navigator:test` grün.

## 3. Pfeil-Verschiebung in `navigator-picker-pages`

- [x] 3.1 `navigator-picker-pages`: Spec („Serverauswahl blättert durch alle Dienste“, „Nur benannte Klicks wirken“, neues Szenario zur Zurück-Taste), `proposal.md`, `design.md` (D6, Test) und `tasks.md` (2.3) auf die Slots 45/48/49/50 und die Zurück-Taste gebracht. Die Hinweise in `design.md` dieses Changes (D5) nennen die Archivreihenfolge.

## 4. Dokumentation und Prüfung

- [x] 4.1 `docs/lobby-modules.md` im Abschnitt zur Serverliste: Zurück-Taste auf Start + 0, Pfeile auf Start + 3 und Start + 5 (Slots 45, 48, 49, 50 bei sechs Reihen; 9 und 13 bei zwei Reihen). Commit `docs(navigator): describe the back button of the server picker`.
- [x] 4.2 `./gradlew spotlessApply` und `./gradlew build` grün; `openspec validate navigator-picker-back --strict` grün.

## 5. Pull Request

- [x] 5.1 Pull Request öffnen mit dem Titel `feat(navigator): add a back button to the server picker` (Squash-Commit ist der Titel, Conventional Commit). Beschreibung auf Englisch: Zusammenfassung, Layout als ASCII-Skizze (eine Reihe und mehrere Seiten), Testnachweis, Verweis auf `openspec/changes/navigator-picker-back` und den Hinweis zur Archivreihenfolge mit `navigator-picker-pages`; abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
