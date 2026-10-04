# Tasks

## Execution Plan

Integrationszweig: `feat/jumprun-more-surfaces` von `origin/main`. Der Change berührt nur `features/jumprun`. `jumprun-climbing` baut auf `Placement`/`CourseBlock` auf und wird nach diesem Change gemergt (rebase auf den dann aktuellen `main`). Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | steps-model | 1.1–1.3 | sonnet | `Surface`, `Placement`, `CourseBlock`, `Spot`, `Jump`, `Course` und ihre Tests | Palette, Config, `Mode`, `CourseGenerator` |
| 2 | palette-config | 3.1–3.3 | sonnet | `JumprunSettings`, `titan/defaults/jumprun.yaml`, `PalettesTest`, `JumprunSettingsTest`, `JumprunConfigTest` | `Course`, `Jump`, `Mode` |
| 3 | heads | 3.4–3.7 | sonnet | `JumprunConfig`, neues `HeadSkins`/`MojangHeadSkins`, `CourseGenerator` (Profil ziehen), `FakeBlocks`, `AnimatedBlock`, `Outline` und Tests | `Course`, `Jump`, `Mode` |
| 2 | generation | 2.1–2.3 | sonnet | `Mode`, `CourseGenerator`, `Difficulty`-Tests, `ModeGenerationTest`, `CourseGeneratorTest` | `Course`, `JumprunSettings` |
| 3 | integration | 4.1–4.2 | sonnet | `features/jumprun/src/test/**` (Integration mit Cyano-`Env`) | Produktionscode |
| 4 | smoke | 4.3 | sonnet + Mensch | nur lokale Läufe (Jar kopieren, nicht im Worktree des laufenden Jars bauen) | Code |
| 4 | verify | 4.4 | haiku | read-only | alles |
| 5 | pr | 5.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seine Aufgabe gelten:
- **Built-in first:** Köpfe über `BlockEntityDataPacket` (SKULL, `profile`) und `DataComponents.PROFILE`, kein Blockdisplay mit Profil. Kollisionsform und Blockzustände aus Minestom lesen, keine eigenen Höhentabellen, wo Minestom sie liefert.
- **Java 25 ohne Preview.**
- **Test zuerst (Red, Green, Refactor).** Jede neue Form beginnt mit einem roten Test.
- **F.I.R.S.T.:** feste Seeds, kein `Thread.sleep`, keine Systemzeit (`Clock` einspritzen), frische `Env` je Test mit `env.tick()`, ein Verhalten je Test, Erfolg nur über Assertions mit Meldung, keine Abhängigkeit zwischen Tests, kein geteilter statischer Zustand. Reine Logik (Stufen, Landeprüfung, Kosten) läuft als Unit-Test ohne Minestom-Server.
- **Kommentare und Log:** schlanke Kommentare nur fürs Warum, keine neuen Nutzertexte.
- **Commits:** Conventional Commits `feat(jumprun): …`, ein Typ je Commit.

## 1. Landefläche als Stufen (Welle 1)

- [x] 1.1 Test zuerst (Unit, `SurfaceStepsTest`): Die sechs alten Formen haben genau eine Stufe `(top, 0, 1, 0, 1)`. Die neuen Formen liefern die Stufen aus D1 (Treppe je `facing` zwei Stufen). Die Treppen-Stufen stimmen für alle vier Ausrichtungen mit der Kollisionsform von `oak_stairs[half=bottom,shape=straight]` überein (Punkte abtasten, `Shape.intersectBox`). Rot. Dann `Step`, `Surface.steps(Block)`, `Surface.lowTop()` und die neuen Konstanten `STAIRS`, `CARPET`, `SNOW`, `HEAD`, `FLOWER_POT`, `CANDLE` mit Oberkante, Kosten (1, 1, 1, 2, 2, 3) und Zuständen (`half=bottom shape=straight waterlogged=false`, `layers=3`, `candles=1 lit=false`). Grün. Nachweis: Test grün, Surface-Test der alten Formen unverändert grün.
- [x] 1.2 Test zuerst (Unit, `JumpTest`, `JumpRulesTest`, `SurfaceTest`): `Jump.rise()` = Ziel-`topY` minus Start-`lowTopY` (D2): Vollblock auf Treppe gleiche Höhe 0; Treppe auf Vollblock gleiche Höhe 0,5 und damit Aufstieg; Treppe auf Vollblock eine Höhe darüber 1,5 und unerreichbar; alte Formen unverändert. `jumpRoomTop` der Treppe gleich Vollblock; Teppich, Schnee, Kopf, Topf, Kerze nach Formel. Rot. Dann `Placement.lowTopY()`, `Jump.rise()` und `jumpRoomTop` umstellen. Grün.
- [x] 1.3 Test zuerst (Unit, `StepLandingTest`, `CourseTest`): Landeprüfung gegen Stufen (D3): Treppe unten, oben und dazwischen für je vier Ausrichtungen; Kerze mit den Grenzen 0,1375 und 0,8625 (0,14 landet, 0,13 nicht); Teppich ganze Zelle bei Höhe 0,0625; Pfosten mit Mitte 0,3 neben der Zelle landet weiter (Regression); `fallThreshold` nutzt `lowTopY`. Rot. Dann `Course.isStandingOn` über `block.steps()` und `fallThreshold` über `lowTopY`. Grün. Nachweis: Test grün.

## 2. Erzeugung und Schwierigkeit (Welle 2)

- [x] 2.1 Test zuerst (Unit, `ModeTest`): Freischaltung aus D5: Easy unverändert (keine neue Form); Medium `stairs`/`carpet`/`snow` 10, `head`/`flower_pot` 25, `candle` 40; Hard 5, 10, 20. Unter Score 10 in Medium und unter 5 in Hard keine neue Form. Rot, dann `Mode.Params`. Grün.
- [x] 2.2 Test zuerst (Unit, `CourseGeneratorTest`, feste Seeds): Die Ausrichtung einer Treppe kommt über viele Seeds in allen vier Werten vor; `redrawn` behält `facing`; Köpfe bekommen eine `rotation` von 0 bis 15. Rot. Dann `CourseGenerator.withDrawnMaterial` und `redrawn` für orientierte Formen. Grün.
- [x] 2.3 Test zuerst (Unit, `ModeGenerationTest`, 200 feste Seeds): In Medium bei Score 80 kommt jede neue Form vor, in Easy keine; kein erzeugter Sprung überschreitet Lücke und Rise, auch nicht von oder auf die neuen Formen; `Jump.MAX_COST` bleibt 2·4 + 1,5·3. Rot, dann die verbleibenden Anpassungen (`Difficulty`/`Jump.maxCost` brauchen keine, nur Nachweis). Grün.

## 3. Palette, Konfiguration, Prüfung (Welle 2, nach 1.1)

- [x] 3.1 Test zuerst (Unit, `JumprunSettingsTest`): Die mitgelieferten Standardwerte aller zwölf Formen bestehen die Prüfung. Abgelehnt mit vollem Schlüssel und Grund: `oak_slab` in `stairs` (Höhe), `carpet` in `snow` (Höhe), ein Block mit anderer Grundfläche in `head` (D6), unbekannter Block, Gewicht −1, alle Gewichte 0. Rot. Dann die Grundflächenprüfung in `JumprunSettings.block`. Grün.
- [x] 3.2 Test zuerst (Unit, `PalettesTest`, `JumprunConfigTest`): `Palettes` verlangt für jede der zwölf Konstanten eine Palette. Ein Betreiber-Override, der nur `stairs.oak_stairs: 0` setzt, lässt die übrigen Treppen; ein Override mit einem Material je Abschnitt ergibt genau diese Palette. Rot. Dann die sechs Abschnitte in `titan/defaults/jumprun.yaml` nach D7, samt Kommentaren zu den erzwungenen Zuständen. Grün.
- [x] 3.3 Nachweis: `./gradlew build` grün, ein Lauf mit Startkonfiguration (`JumprunStartTest`) startet mit den Standardwerten.

- [x] 3.4 Test zuerst (Unit, `HeadProfilesSettingsTest`, Attrappe `HeadSkins`, `CapturedLog`): Eine gültige UUID-Liste wird gelesen. Eine Nicht-UUID macht die Änderung unwirksam, die letzte Liste bleibt, das Log nennt `jumprun.heads.profiles` und den Grund. Eine nicht auflösbare UUID wird übersprungen und mit WARN genannt; nur nicht auflösbare Einträge ergeben die leere Liste; der Start bricht nie ab. Rot. Dann `jumprun.heads.profiles` (Standard leer) in `JumprunConfig` über `LiveSetting`, das Interface `HeadSkins` und `MojangHeadSkins` (`PlayerSkin.fromUuid`, nicht auf dem Tick-Thread, Treffer im Speicher). Grün.
- [x] 3.5 Test zuerst (Unit, `HeadDrawTest`, feste Seeds): Bei zwei Profilen kommen beide vor und nie dasselbe zweimal hintereinander; bei einem Profil wiederholt es sich; `redrawn` liefert ein anderes Profil; das Profil steht im `CourseBlock` (`HeadSkin`), nicht im Blockzustand. Rot. Dann `CourseGenerator`/`CourseBlock`. Grün.
- [x] 3.6 Test zuerst (Unit, `HeadFallbackTest`): Bei leerer Liste ist der Kopf ein Block der Palette `head` ohne Profil; Oberkante, Grundfläche und Kosten der Form ändern sich mit Profil nicht. Rot, dann Grün.
- [x] 3.7 Test zuerst (Integration, `Env`, `env.tick()`): `FakeBlocks.show` sendet für einen Teamkopf `BlockChangePacket` und danach `BlockEntityDataPacket` (SKULL) mit `profile` (UUID und Textur); bei leerer Liste kein `BlockEntityDataPacket`; Falleffekt und Umrandung eines Teamkopfes sind Itemdisplays mit `PROFILE`, übrige Formen Blockdisplays; `reset` sendet den echten Block. Rot, dann `FakeBlocks`, `AnimatedBlock`, `Outline`. Grün. Nachweis: Tests grün, kein Test greift auf das Netz zu.

## 4. Abnahme (Welle 3–4)

- [x] 4.1 Test zuerst (Integration, Cyano-`Env`, `env.tick()`, Muster von `JumprunLandingTest`): Läufer landet auf der unteren und auf der oberen Stufe einer erzeugten Treppe (feste Seeds, Treppe per Test-Generator vorgegeben): Score steigt, kein Absturz. Läufer in Höhe der Kerze neben ihr: kein Score. Rot, dann nötige Verdrahtung. Grün.
- [x] 4.2 Test zuerst (Integration, `Env`): Darstellung der neuen Formen. `BlockChangePacket` trägt den Blockzustand mit `facing` (Treppe) und `layers=3` (Schnee); Rainbow-Wechsel behält `facing`; Falleffekt-Display und Umrandung tragen dasselbe Material. Rot, dann Grün. Nachweis: Tests grün.
- [ ] 4.3 Smoke-Test mit dem Shaded-Jar auf der echten Lobby-Map, echter Client:
  - Jede der sechs Formen erscheint in Medium und Hard und trägt den Spieler.
  - Treppe: Landen auf unterer und oberer Stufe zählt; die Ausrichtung variiert.
  - Kopf mit Teamprofil: richtige Haut im Fake-Block, im fallenden Itemdisplay und in der Umrandung; Größe und Versatz des Itemdisplays passen zur Zelle; nie derselbe Kopf hintereinander; Rückfall bei leerer Liste und bei nicht erreichbarem Mojang zeigt einfache Köpfe.
  - Topf, Kerze: sichtbar als Fake-Block und als fallende Darstellung, Umrandung erkennbar, Landung am Rand fühlt sich fair an.
  - Schnee: das scheinbare Einsinken stört nicht (sonst `layers=2`).
  - Rainbow und Ultra mit den neuen Formen.

  Nachweis: Checkliste im PR-Text.
- [x] 4.4 Verifikation (read-only): Jedes Szenario des Spec-Deltas ist einem Test oder Smoke-Punkt zugeordnet, und die Tests erfüllen F.I.R.S.T. (keine Sleeps, keine Systemzeit, frische `Env`, ein Verhalten je Test). Nachweis: Zuordnungstabelle im PR-Text.

## 5. Pull Request

- [x] 5.1 Pull Request vom Integrationszweig `feat/jumprun-more-surfaces` auf `main` unter dem Titel `feat(jumprun): add stairs, carpet, snow, heads, flower pots and candles as landing shapes` öffnen (Titel und Beschreibung Englisch), mit Smoke-Checkliste und Szenario-Zuordnung. Nachweis: PR-URL, CI grün.
