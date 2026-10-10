# Tasks

## Execution Plan

Integrationszweig: `fix/jumprun-height-and-jump-room` von `origin/main`. Unabhängig von `feat/jumprun-persistent-scores`: Die beiden berühren in `features/jumprun` verschiedene Klassen. Wer später mergt, rebased auf den dann aktuellen `main`. Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | bounds-bean | 1.1–1.2 | sonnet | `core/src/**` (nur das neue Interface), `features/spawn/**` | `features/jumprun/**`, `runtime/**`, `apps/**` |
| 1 | jump-room | 2.1–2.3 | sonnet | `features/jumprun/src/**` (`Surface`, `Placement`, `JumpRules`, `CourseGenerator`, Tests) | `core/**`, `features/spawn/**` |
| 2 | height-band | 3.1–3.3 | sonnet | `features/jumprun/**` | `core/**`, `features/spawn/**` |
| 3 | cross-column | 4.1 | sonnet | `apps/cloudnet/src/test/**`, `apps/local/src/test/**` | Produktionscode |
| 3 | docs | 4.2 | sonnet | `docs/lobby-modules.md` | Code |
| 4 | smoke | 4.3 | sonnet + Mensch | nur lokale Läufe (Jar kopieren) | Code |
| 4 | verify | 4.4 | haiku | read-only | alles |
| 5 | pr | 5.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seine Aufgabe gelten:
- **Built-in first:** Avaje-Beans für die Grenzen, keine fremden Konfigurationsabschnitte.
- **Java 25 ohne Preview.**
- **Test zuerst:** Ein Bug wird zuerst durch einen roten Test belegt.
- **F.I.R.S.T.:** fester Seed, kein `Thread.sleep`, keine Systemzeit, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kommentare und Log:** schlanke Kommentare nur fürs Warum, keine neuen Nutzertexte, Logs nur auf DEBUG.
- **Commits:** Conventional Commits `fix(jumprun): …`.

## 1. Höhengrenzen als Bean (Welle 1)

- [x] 1.1 Test zuerst (Unit, spawn): Die Bean `LobbyHeightBounds` liefert `spawn.minHeight`/`maxHeight` aus der Konfiguration und nach einer Änderung die neuen Werte. Rot. Dann das Interface `LobbyHeightBounds` in `core` (`net.onelitefeather.titan.core.module`) und die `@Bean` in der Spawn-Factory aus derselben Quelle wie `HeightBounds`. `@InjectModule provides` der Spawn-Column ergänzen. Grün.
- [x] 1.2 Nachweis: `./gradlew build` grün, die Variant-Startprüfung der Apps (`VariantStartupCheck`-Tests) bleibt grün.

## 2. Sprungfreiheit (Welle 1)

- [x] 2.1 Bug zuerst belegen (Unit, `JumpRulesTest`, Fake-`SpaceProbe`): Decke 3 Blöcke über einer Vollblock-Oberkante ist heute ein gültiges Ziel. Rot (zeigt den Bug). Dann `Surface.JUMP_HEIGHT`, `jumpRoomTop()`, `Placement.jumpRoomTopY()` und `hasRoomAtTarget` darauf umstellen. Grün. Weitere Fälle: 4 Blöcke frei ist gültig, Zaun und Stufe brauchen ihre eigene Oberkante.
- [x] 2.2 Bug zuerst belegen (Unit): Ein Überhang, der zwischen Absprung und Ziel die Scheitelsäule schneidet, ist heute gültig. Rot. Dann `isFlightPathFree` bis `highestBlockReached(fromTop + JUMP_HEIGHT)`. Grün.
- [x] 2.3 Test zuerst (Unit, `CourseGeneratorTest`): Kein neuer Block in der Sprungfreiheit eines sichtbaren Blocks. Dazu ein Eigenschaftstest über 200 feste Seeds in einer Fake-Welt mit Überhängen: Jeder erzeugte Block hat Luft bis `jumpRoomTopY()`. Rot, dann `CourseGenerator` umstellen, grün.

## 3. Höhenband (Welle 2)

- [x] 3.1 Test zuerst (Unit, `HeightBandTest`): Grenzwerte nach D3 bei `minHeight` −64 und 0 sowie `maxHeight` 310 und 100, knapp drin und knapp draußen, für Vollblock, Stufe und Zaun. Rot. Dann `HeightBand` (rein, aus `LobbyHeightBounds` gebaut, `FALL_DISTANCE` aus `Course` geteilt, `MAX_FALL_PER_TICK = 5`). Grün.
- [x] 3.2 Test zuerst (Unit, `JumpRulesTest`): Eine sonst gültige Stelle unterhalb bzw. oberhalb des Bands ist ungültig. Rot. Dann `JumpRules` nutzt `HeightBand`, `MAX_Y_MARGIN` entfällt, `LobbyHeightBounds` wird über `@InjectModule requires` und den Konstruktor von `JumprunModule` bis zu `JumpRules` gereicht. Grün.
- [x] 3.3 Test zuerst (Unit, `AscentPhaseTest`, und Integration): Start 6 Blöcke unter `maxHeight` und knapp über der Untergrenze → kein Start. Integration (`env.tick()`): Item nahe der Obergrenze → Meldung „kein Platz“, kein Lauf. Rot. Dann Startprüfung und Aufstieg mit `HeightBand` und Sprungfreiheit. Grün.

## 4. Abnahme und Doku (Welle 3–4)

- [x] 4.1 Test zuerst (Integration in `apps/cloudnet`, alle Columns): Mit `spawn.minHeight` so gesetzt, dass ein Lauf nahe der Grenze liegt, fällt ein Läufer vom tiefsten erlaubten Block. Erwartet: Der Lauf endet mit `FALL`, und der Spieler steht am Startpunkt des Laufs, nicht am Lobby-Spawn. Nachweis: Test grün.
- [x] 4.2 `docs/lobby-modules.md`: Bei jumprun ergänzen, dass Kurse innerhalb von `spawn.minHeight`/`maxHeight` mit Abstand bleiben und immer Sprungfreiheit haben, und dass ein sehr enges Band Läufe verhindert. Nachweis: Doku nennt Band und Folge.
- [x] 4.3 Smoke-Test mit dem Shaded-Jar auf der echten Lobby-Map, echter Client:
  - Lauf unter bzw. neben der schwebenden Insel: kein Block unter der Decke.
  - 50+ Sprünge in Easy, Medium und Hard.
  - Absturz vom tiefsten Block: Reset an den Startpunkt, nicht an den Spawn.
  - Start nahe der Obergrenze: „kein Platz“.

  Nachweis: Checkliste im PR-Text.
- [x] 4.4 Verifikation (read-only): Jedes Szenario des Spec-Deltas ist einem Test oder Smoke-Punkt zugeordnet, und die Tests erfüllen F.I.R.S.T. Nachweis: Zuordnungstabelle im PR-Text.

## 5. Pull Request

- [x] 5.1 Pull Request vom Integrationszweig `fix/jumprun-height-and-jump-room` auf `main` unter dem Titel `fix(jumprun): keep runs inside the lobby height and leave room to jump` öffnen (Titel und Beschreibung Englisch), mit Smoke-Checkliste und Szenario-Zuordnung. Nachweis: PR #352 (gemergt, `9833c30`).
