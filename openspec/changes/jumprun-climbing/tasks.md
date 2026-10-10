# Tasks

## Execution Plan

Integrationszweig: `feat/jumprun-climbing` von `origin/main`, nach dem Merge von `jumprun-more-surfaces` (beide Changes berühren `Placement`, `CourseBlock`, `Jump`, `CourseGenerator`; wer später mergt, rebased). Die Anforderungen „Sprünge nur, wo Platz ist“ und „Kein Teleport durch die Höhengrenzen“ stammen aus `jumprun-height-and-jump-room`; ihr Code ist auf `main`, das Spec-Delta ist vor dem Archivieren dieses Changes zu archivieren. Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | climb-model | 1.1–1.3 | sonnet | neue `Climb`-Klasse, `Placement`, `CourseBlock`, `Spot`, `Course` (nur Tests und Fortschritt), zugehörige Tests | `Mode`, Config, `FakeBlocks`, `AnimatedBlock` |
| 1 | climb-config | 4.1–4.2 | sonnet | `JumprunSettings`, `JumprunConfig`, `titan/defaults/jumprun.yaml`, zugehörige Tests | `Course`, `Jump`, Darstellung |
| 2 | generation | 2.1–2.4 | sonnet | `Jump`, `JumpRules`, `Clearance`, `CourseGenerator`, `Mode`, zugehörige Tests | Darstellung, Config |
| 3 | display | 3.1–3.3 | sonnet | `FakeBlocks`, `AnimatedBlock`, `Outline`, `Reroller`-Anbindung, zugehörige Tests | `Jump`, `JumpRules`, `Mode` |
| 4 | integration | 5.1–5.2 | sonnet | `features/jumprun/src/test/**` (Cyano-`Env`) | Produktionscode |
| 5 | smoke | 5.3 | sonnet + Mensch | nur lokale Läufe (Jar kopieren, nicht im Worktree des laufenden Jars bauen) | Code |
| 5 | verify | 5.4 | haiku | read-only | alles |
| 6 | pr | 6.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seine Aufgabe gelten:
- **Built-in first:** Minestom kennt `climbable` über Tags und die Blockzustände von Leiter und Ranke; keine eigene Tabelle, wo Minestom sie liefert. Minestom simuliert das Klettern nicht, daher keine Annahmen über „klettert“ im Server.
- **Java 25 ohne Preview.**
- **Test zuerst (Red, Green, Refactor).**
- **F.I.R.S.T.:** feste Seeds, kein `Thread.sleep`, keine Systemzeit, frische `Env` je Test mit `env.tick()`, ein Verhalten je Test, Erfolg nur über Assertions mit Meldung, keine Abhängigkeit zwischen Tests, kein geteilter statischer Zustand. Positionen des Läufers kommen als Positions-Updates, eines je Tick, nicht per Wartezeit. Reine Logik (Zellen, Kosten, Fortschritt, Absturz) läuft als Unit-Test ohne Server.
- **Kommentare und Log:** schlanke Kommentare nur fürs Warum, keine neuen Nutzertexte.
- **Commits:** Conventional Commits `feat(jumprun): …`, ein Typ je Commit.

## 1. Modell des Turms und Fortschritt (Welle 1)

- [x] 1.1 Test zuerst (Unit, `ClimbTowerTest`): Für jede der vier Achsenrichtungen und `H` 3 bis 5 liefert `Climb.cells` die Zellen aus D1: `H` Leiterzellen in der Spalte über `A`, `H − 1` Pfeilerzellen und den Zielblock bei `A.pos + d + (0, H, 0)`; `facing` der Leiter ist `−d`, die Ranke trägt die Seite zum Pfeiler. Rot. Dann `Climb` (Richtung, Höhe, Art) mit `cells`. Grün.
- [x] 1.2 Test zuerst (Unit, `CourseTest`): Ein Kurs mit Turm zeigt im Fenster einen Block mit `2H − 1` Anbauten (Leiter und Pfeiler); `window()` zählt Anbauten nicht als Blöcke; `Course.recolor` tauscht Pfeiler und Zielblock gemeinsam und lässt die Leiter. Rot. Dann `Placement.attachments()`, `CourseBlock` und `Spot` mit `Climb` und `Spot.withMaterial` mit Zellen. Grün.
- [x] 1.3 Test zuerst (Unit, `ClimbProgressTest`, `CourseTest`): Läufer auf `B` bei `B.topY` zählt einen Sprung (für `H` 3, 4, 5); auf halber Höhe der Leiterspalte, auf `B.topY − 0,2` und auf dem Ausgangsblock zählt nichts; `hasFallen` ist auf jeder Fußhöhe von `A.topY` bis `B.topY` falsch und bei `A.topY − 3,01` wahr (D3, D4). Rot, dann Nachweis, dass `advanceTo` und `fallThreshold` ohne Änderung genügen (sonst minimal anpassen). Grün. Nachweis: Test grün.

## 2. Erzeugung (Welle 2)

- [x] 2.1 Test zuerst (Unit, `JumpTest`): Erreichbarkeit eines Turms nur bei Lücke 0 in genau einer Achsenrichtung und `H` im Bereich über `MAX_RISE`; Kosten für `H` 3, 4, 5 (2,0 / 2,5 / 3,0); `Jump.MAX_COST` umfasst den schwersten Turm. Rot, dann `Jump`. Grün.
- [x] 2.2 Test zuerst (Unit, `JumpRulesTest` mit Fake-`SpaceProbe`, `ClearanceTest`): Block in Pfeilerzelle, in Leiterzelle und in der Sprungfreiheit über der Leiterspalte → ungültig; Turm direkt unter dem Höhenband gültig, eine Zelle darüber ungültig; sichtbarer Block innerhalb von zwei Zellen neben den Spalten verletzt den Abstand. Rot, dann `JumpRules` und `Clearance` mit den Zellen aus `Climb`. Grün.
- [x] 2.3 Test zuerst (Unit, `CourseGeneratorTest`): `occupiedBy` enthält alle Anbauzellen eines sichtbaren Turms, und kein neuer Block entsteht in ihnen; Kandidaten für Türme gibt es nur in der Scored-Phase und nur mit Richtung nicht gegen das Heading. Rot, dann `CourseGenerator` (Kandidaten, `occupiedBy`). Grün.
- [ ] 2.4 Test zuerst (Unit, `ModeTest`, `ModeGenerationTest`, 200 feste Seeds): Freischaltung nach D5 (Easy nie; Medium Leiter 30 und Ranke 40; Hard 15 und 25); in Medium bei Score 29 kein Turm; in Medium bei Score 80 kommen Türme vor; jede erzeugte Strecke hat bei jedem Turm Lücke 0 und `H` im Bereich; der Schwierigkeitswert überschreitet seine Obergrenze nicht. Rot, dann `Mode`. Grün.

## 3. Darstellung (Welle 3)

- [x] 3.1 Test zuerst (Integration, `Env`, `env.tick()`): `FakeBlocks.show` sendet für Zielblock und alle Anbauten je einen `BlockChangePacket`; `reset` sendet die echte Welt für alle Zellen; ein anderer Spieler bekommt keine Fake-Blöcke. Rot. Dann `FakeBlocks` über `cells()`. Grün.
- [ ] 3.2 Test zuerst (Integration, `Env`): `AnimatedBlock` zeigt für jede Zelle ein Display (`2H − 1` Anbauten plus Block), alle fallen gemeinsam, landen gemeinsam und steigen gemeinsam auf; die Umrandung sitzt nur am Zielblock; nach Ende und Shutdown bleibt kein Display zurück. Rot, dann `AnimatedBlock`, `Outline`. Grün.
- [ ] 3.3 Test zuerst (Integration, `Env`, Rainbow und Ultra): Rainbow tauscht Material von Pfeiler und Zielblock, die Leiter bleibt; Ultra erzeugt einen Turm voraus neu, solange der Läufer auf dem Ausgangsblock steht, und lässt ihn, wenn der Läufer mit `y > A.topY` in der Leiterspalte gemeldet wird. Rot, dann `Reroller`-Anbindung. Grün.

## 4. Konfiguration und Prüfung (Welle 1)

- [x] 4.1 Test zuerst (Unit, `JumprunSettingsTest`, `JumprunConfigTest`): Standardwerte `jumprun.climb.minHeight` 3, `maxHeight` 5 und die Paletten `ladder` und `vine` sind gültig. Abgelehnt mit Schlüssel und Grund: `stone` in `ladder`, `minHeight: 1`, `maxHeight` kleiner als `minHeight`, `maxHeight: 9`, nicht numerisch, Gewicht −1, alle Gewichte 0. Eine gültige Änderung der Höhen wird beim nächsten Start gelesen, eine ungültige bleibt wirkungslos und wird protokolliert (Muster `rainbow.rerollTicks`, `LiveSetting`). Rot. Dann `JumprunSettings` (Prüfung `climbable` über die Tag-Registry, sonst feste Erlaubnisliste), `JumprunConfig` und `titan/defaults/jumprun.yaml` mit Kommentaren. Grün.
- [x] 4.2 Nachweis: `./gradlew build` grün, `JumprunStartTest` startet mit den Standardwerten.

## 5. Abnahme (Welle 4–5)

- [ ] 5.1 Test zuerst (Integration, `Env`, `env.tick()`, Muster `JumprunMoveTest`): Ein Läufer klettert per Positions-Updates (je Tick eine Position) von `A` über die Leiterzellen auf `B`: Score ist währenddessen 0 und danach 1, das Fenster rückt um einen Block. Rot, dann nötige Verdrahtung. Grün.
- [ ] 5.2 Test zuerst (Integration, `Env`): Läufer klettert halb hoch und wird auf `A.topY − 5` gemeldet: Lauf endet mit `FALL`, der Spieler steht am Startpunkt. Läufer auf halber Höhe: kein Absturz. Rot, dann Grün.
- [ ] 5.3 Smoke-Test mit dem Shaded-Jar auf der echten Lobby-Map, echter Client (Pflicht, weil der Client klettert):
  - Leiter: Der Läufer steht auf dem Ausgangsblock, klettert bei Vorwärtsdruck sofort los, kommt oben auf den Zielblock, der Sprung zählt.
  - Das Brett der Leiter stört das Stehen auf dem Ausgangsblock nicht.
  - Beim Verlassen der Leiter liegt die gemeldete Fußhöhe im Band der Landeprüfung (sonst Toleranz anpassen, siehe D3).
  - Ranke: dasselbe; sie ist erkennbar, ihre fehlende Kollision stört nicht.
  - Fall vom halben Turm endet den Lauf; Absturz neben dem Turm ebenso.
  - Rainbow und Ultra mit Türmen; Zuschauer sieht Darstellung, kann nicht klettern.
  - Mehrere Läufer gleichzeitig, Displays und Pakete bleiben ruhig.

  Nachweis: Checkliste im PR-Text.
- [ ] 5.4 Verifikation (read-only): Jedes Szenario des Spec-Deltas ist einem Test oder Smoke-Punkt zugeordnet, und die Tests erfüllen F.I.R.S.T. Nachweis: Zuordnungstabelle im PR-Text.

## 6. Pull Request

- [ ] 6.1 Pull Request vom Integrationszweig `feat/jumprun-climbing` auf `main` unter dem Titel `feat(jumprun): add climbing segments with ladders and vines` öffnen (Titel und Beschreibung Englisch), mit Smoke-Checkliste, Szenario-Zuordnung und den offenen Fragen aus `design.md`. Nachweis: PR-URL, CI grün.
