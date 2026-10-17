# Tasks

## 1. Umsetzung (in PR #421)

- [x] 1.1 Moduswechsel: ein Schleichklick innerhalb von 5 Ticks nach dem letzten Wechsel wechselt nichts, und ignorierte Klicks verlängern das Fenster nicht. Nachweis: `JumprunModeTest` (Doppelklick, Fenster, ignorierter Klick).
- [x] 1.2 Startsperre: 20 Ticks ab dem Start, Ende mit dem Lauf. Nachweis: `JumprunStopTest`, `JumprunClickTest` (Tod und Rückkehr zum Spawn in der Sperre).
- [x] 1.3 Blockklick: Folgebenutzung nur im Lauf unterdrückt, außerhalb startet oder wechselt sie. Nachweis: `JumprunClickTest` (Start und Schleichklick nach einem Klick auf einen Lobby-Block rot vor dem Fix).

## 2. Verifikation

- [x] 2.1 `openspec validate jumprun-click-debounce` ohne Fehler.
- [x] 2.2 `./gradlew build` grün.

## 3. Pull Request

- [ ] 3.1 Pull Request #421 auf `main` mit dem Titel `fix(jumprun): debounce the mode switch and accidental stops` (Titel und Beschreibung Englisch) aktualisieren: Fix, Spec-Änderung, Tests. CI abwarten. Nachweis: PR-URL, CI grün.
