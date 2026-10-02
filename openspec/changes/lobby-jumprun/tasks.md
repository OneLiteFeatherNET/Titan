# Tasks

## Execution Plan

Integrationszweig: `feat/jumprun` von `origin/main`. Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig und werden nach Review gemergt. Jede Welle endet mit grünem `./gradlew build` und geprüften Diffs. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | scaffold | 1.1 | sonnet | `features/jumprun/build.gradle.kts`, `features/jumprun/src/main/java/**/package-info.java`, `features/jumprun/src/test/**/ColumnArchitectureTest.java` | `core/**`, `runtime/**`, `apps/**`, andere `features/**` |
| 2 | course-logic | 2.1–2.6 | sonnet | `features/jumprun/src/{main,test}/java/**` (nur reine Klassen: `Surface`, `JumpRules`, `Difficulty`, `SpaceProbe`, `CourseGenerator`, `Course`, `Phase`) | `build.gradle.kts`, `package-info.java`, alles außerhalb `features/jumprun` |
| 2 | messages-records | 3.1–3.3 | sonnet | `features/jumprun/src/{main,test}/java/**` (nur `RunMessages`, `RunRecords`, `InMemoryRunRecords`), `features/jumprun/src/main/resources/titan/jumprun/**` | wie oben |
| 3 | wiring | 4.1–4.7 | sonnet | `features/jumprun/**` | alles außerhalb `features/jumprun` |
| 4 | variants-docs | 5.1–5.2 | sonnet | `apps/cloudnet/src/test/**`, `apps/local/src/test/**`, `docs/lobby-modules.md`, `README.md` | `features/**`, `core/**`, `runtime/**` |
| 4 | smoke | 5.3 | sonnet | nur lokale Läufe, Ergebnis in den PR-Text | Code |
| 5 | verify | 5.4 | haiku | read-only | alles |
| 5b | variety-openness | 7.1–7.5 | sonnet | `features/jumprun/**` | alles außerhalb `features/jumprun` |
| 5c | spectators-distance | 8.1–8.4 | sonnet | `features/jumprun/**` | alles außerhalb `features/jumprun` |
| 5d | item-label-sound | 9.1–9.3 | sonnet | `features/jumprun/**`, `apps/*/src/test/**` | alles andere |
| 5d | click-fix | 10.1 | sonnet | `features/jumprun/**`, D14 in `design.md` | alles andere |
| 6 | pr | 6.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seine Aufgabe gelten. Erst Vorhandenes nutzen: `FeatureNode`, `LobbyItem`/`ItemSlot`, `LobbySpawn` aus `core`, Minestom-`BlockChangePacket` und Events, `java.util.random.RandomGenerator`, Adventure `TranslationStore`/`GlobalTranslator`. Java 25 ohne Preview: Records, sealed Types mit `switch`, `_`. Nutzertexte nur über die Bundles (D8), Schlüssel `titan.jumprun.*`, Englisch als Fallback. SLF4J mit Parametern, Lauf-Ereignisse nur auf DEBUG, keine Metriken oder Spans (D10). Test zuerst, schlanke Kommentare nur fürs Warum, Conventional Commits `feat(jumprun): …`. F.I.R.S.T.: fester Seed statt Zufall, kein `Thread.sleep`, keine Systemzeit, frische `Env`/Fixtures je Test, `env.tick()` statt Warten, Erfolg nur über Assertions, kein geteilter statischer Zustand (Rekorde, Registry und Translator-Registrierung je Test neu bzw. im `@AfterEach` entfernt).

## 1. Column anlegen (Welle 1)

- [x] 1.1 `features/jumprun` mit `build.gradle.kts` (`titan.column`, `libs.adventure.minimessage` falls nicht über die Convention vorhanden, `libs.slf4j.api`) und `package-info.java` (`@InjectModule(name = "jumprunColumn", provides = {LobbyItem.class}, requires = {EventNode.class, LobbySpawn.class, LobbyItems.class}, requiresString = {"…EventNode<…Event>:titan"})` nach dem Muster von `features/elytra`) anlegen, dazu `ColumnArchitectureTest` mit `ColumnArchitectureRules`. Nachweis: `./gradlew :features:jumprun:build` grün, `settings.gradle.kts` unverändert.

## 2. Kurslogik, rein (Welle 2)

- [x] 2.1 Test zuerst (Unit, `SurfaceTest`): Vollblock, Stufe (unten), Zaun, Glasscheibe haben Block, Oberkante (1.0 / 0.5 / 1.5 / 1.0) und Kosten (0/1/2/3) aus D3; rot. Dann `Surface` als enum oder sealed Type; grün.
- [x] 2.2 Test zuerst (Unit, `DifficultyTest`): `d(0) = 0`, `d` streng monoton, `d(n) < 1` für n bis 1000 (darüber rundet double auf 1.0), `d(28) ≈ 0.5` (K = 40), Zielkosten im Bereich `[0, C_max]` auch mit Rauschen (fester Seed); rot. Dann `Difficulty`; grün.
- [x] 2.3 Test zuerst (Unit, `JumpRulesTest`, `FakeSpaceProbe` als Set belegter Positionen): Oberkantendifferenz ≤ 1.0, Lücke ≤ 3 bei Aufstieg und ≤ 4 sonst, Zaun nach Vollblock nur bei dy ≤ 0 (Spec-Szenario „Zaun nach Vollblock“), Ziel muss Luft sein, 2 Blöcke Kopffreiheit über der Oberkante, Flugbahn frei inklusive beider Nachbarzellen bei diagonalen Schritten, Worldborder und `MAX_Y_MARGIN`; rot. Dann `SpaceProbe` und `JumpRules`; grün.
- [x] 2.4 Test zuerst (Unit, `CourseGeneratorTest`): wählt den Kandidaten mit der kleinsten `|Kosten - Ziel|`, verwirft Sackgassen (Vorausschau Tiefe 1), meldet „kein Kandidat“ als leeres Ergebnis, gleicher Seed ergibt denselben Kurs, Wand in Laufrichtung führt vorbei statt hindurch; Statistik mit festem Seed über 1000 Sprünge: bei n = 0 sind über 80 % Vollblöcke mit Lücke ≤ 2, die mittleren Kosten bei n = 80 liegen deutlich über denen bei n = 0; 10 000 Sprünge über mehrere Seeds verletzen keine Grenze aus 2.3; rot. Dann `CourseGenerator`; grün.
- [x] 2.5 Test zuerst (Unit, `AscentPhaseTest`): 5 Aufstiegssprünge mit Vollblock und Oberkante +1, Richtung bei freiem Raum vom Spawn weg (Skalarprodukt > 0), Spieler genau auf dem Spawn → Blickrichtung, niedrige Decke → Vorab-Prüfung scheitert; rot. Dann `Phase` (sealed: `Ascent`, `Scored`) und die Aufstiegsstrategie im Generator; grün.
- [x] 2.6 Test zuerst (Unit, `CourseTest`): Das Fenster zeigt nach dem Start Startblock plus 2 voraus, nach Vorrücken höchstens 2 zurück, den aktuellen und 2 voraus; Landen auf +2 zählt beide Sprünge; Aufstiegssprünge zählen nicht (Score 0 nach dem Aufstieg); Absturz-Schwelle `Oberkante(letzter) - 3`; `advanceTo` liefert entfernte und neue Positionen für die Pakete; rot. Dann `Course`; grün.

## 3. Texte und Rekorde (Welle 2)

- [x] 3.1 Prüfen, ob die mitgelieferte Adventure-Version `MiniMessageTranslationStore` hat, und das Ergebnis in D8 eintragen. Test zuerst (Unit, `RunMessagesBundleTest`): Jeder in `RunMessages` verwendete Schlüssel steht in `messages_en.properties`, und `messages_de.properties` hat genau dieselben Schlüssel; rot. Dann die Bundles (UTF-8) mit `start.no_space`, `score.actionbar`, `end.score`, `end.record` anlegen; grün.
- [x] 3.2 Test zuerst (Unit, `RunMessagesTest`): Die Locale `de_DE` rendert die deutsche Endmeldung mit dem Score als Argument, `ja_JP` fällt auf Englisch zurück, Registrieren/Entfernen beim `GlobalTranslator` hinterlässt keinen Store (Aufräumen im `@AfterEach`); rot. Dann `RunMessages` (registriert/entfernt den Store, rendert explizit per `GlobalTranslator.render`); grün.
- [x] 3.3 Test zuerst (Unit, `InMemoryRunRecordsTest`): erster Lauf ist Rekord, 12 → 15 ist neuer Rekord, danach 9 kein Rekord und Rekord bleibt 15, verschiedene Spieler unabhängig; rot. Dann `RunRecords` und `InMemoryRunRecords`; grün.

## 4. Verdrahtung in Minestom (Welle 3)

- [x] 4.1 Test zuerst (Integration, Cyano-`Env`, Testverbindung schneidet eingehende Pakete mit): `FakeBlocks` schickt `BlockChangePacket`s nur an den Zielspieler, beim Entfernen den echten Block aus der Instanz, `instance.getBlock` bleibt unverändert; rot. Dann `FakeBlocks`; grün.
- [x] 4.2 Test zuerst (Unit): `InstanceSpaceProbe` meldet Luft/belegt und Grenzen einer Test-Instanz korrekt (sonst Integration mit `Env`, falls `Instance` nicht ohne Server baubar ist); rot. Dann `InstanceSpaceProbe`; grün.
- [x] 4.3 Test zuerst (Integration): Benutzt man das Item `titan:jumprun` in Hotbar-Slot 0 im Freien, startet ein Lauf, und A bekommt 2 Block-Pakete (der Startblock ist ein echter Block), B keins. Unter niedriger Decke oder in der Luft startet kein Lauf, und A bekommt die Meldung `start.no_space`. Erneute Benutzung beendet den Lauf und setzt die Blöcke zurück; rot. Dann `JumprunItems` (`@Factory`, `@Bean LobbyItem`, Name „Jump & Run“ ohne Beschreibung), `RunRegistry` und `JumprunModule` (Priorität 1000, Seed-Lieferant per paketinternem Konstruktor, D7); grün.
- [x] 4.4 Test zuerst (Integration, `env.tick()`, Spieler über die Testverbindung bewegen): Landen auf +1 rückt vor (ältester Block weg, neuer +2 da, Score in der Action Bar), Landen auf +2 zählt 2, Fall um mehr als 3 beendet den Lauf, setzt die Blöcke zurück und teleportiert zum Startpunkt, kein Kandidat mehr beendet mit dem erreichten Score; rot. Dann die `PlayerMoveEvent`-Behandlung; grün.
- [x] 4.5 Test zuerst (Integration): `PlayerStartFlyingWithElytraEvent` beendet ohne Teleport, `PlayerDeathEvent` und `PlayerDisconnectEvent` beenden und leeren die Registry, Rekord-Meldung bei neuem Rekord, der Rekord bleibt nach Disconnect erhalten; rot. Dann die Behandlung dieser Events; grün.
- [x] 4.6 Test zuerst (Integration): `PlayerChunkLoadEvent` für einen Chunk mit Laufblöcken sendet sie nach dem Chunk erneut (Reihenfolge der Pakete prüfen, sonst `scheduleNextTick`, D1 Risiko). Rechtsklick (`PlayerBlockInteractEvent`) und Abbauversuch (`PlayerStartDiggingEvent`) auf einen Laufblock lassen den Block für den Spieler sichtbar; rot. Dann das Neusenden; grün.
- [x] 4.7 Test zuerst (Integration): Nach `stop()` (`@PreDestroy`) zählt `EventListenerCounter` 0 Listener der Column, laufende Läufe sind beendet und ihre Blöcke zurückgesetzt, der Translation-Store ist entfernt; rot. Dann den Shutdown; grün. Nachweis: `./gradlew :features:jumprun:build` grün, keine Datei außerhalb `features/jumprun/**` geändert.

## 5. Varianten, Doku und Abnahme (Welle 4–5)

- [x] 5.1 Test zuerst (Integration in `apps/cloudnet`, z. B. `StandardLoadoutTest`/`WiringTest`): Die Standardausstattung enthält „Jump & Run“ in Hotbar-Slot 0 neben Navigator (Slot 4) und Elytra, `jumprunColumn` wird geladen und im Startcheck erwartet, es gibt keinen Slot- oder Prioritätskonflikt; rot, dann grün durch die Column aus Welle 3 (keine Änderung an `build.gradle.kts` der Varianten). Nachweis: Tests grün.
- [x] 5.2 `docs/lobby-modules.md` (Tabelle der Columns, Prioritäten, Hotbar-Slots) und README (Abschnitt Lobby-Features) um `jumprun` ergänzen: Was es tut, Slot 0, Priorität 1000, Rekorde nur im Speicher, eigene Übersetzungen ohne globales Flag. Nachweis: Doku nennt Slot, Priorität und Einschränkungen.
- [ ] 5.3 Smoke-Test mit dem Shaded-Jar `titan-local.jar` und echtem Client: Start im Freien, Start unter Decke, 30+ Sprünge (sichtbar schwerer), Absturz, Elytra-Ende, Rechtsklick auf Laufblock, Chunk-Grenze überqueren, deutscher und englischer Client (Action Bar nicht leer, Minestom-26.1-Falle). Dabei auch beobachten, wie oft die Leertaste versehentlich das Gleiten auslöst (Risiko in design.md). Nachweis: Checkliste mit Ergebnis im PR-Text.
- [x] 5.4 Verifikation (Haiku, read-only): alle Szenarien aus `specs/lobby-jumprun` und `specs/lobby-hotbar` Test für Test zuordnen; F.I.R.S.T.-Check (fester Seed, keine Sleeps, keine Systemzeit, kein geteilter statischer Zustand, insbesondere `GlobalTranslator` im Test aufgeräumt). Nachweis: Bericht ohne Lücken.

## 7. Variation und offener Raum (Nachtrag nach lokalem Test)

- [x] 7.1 Test zuerst (Unit, `SurfaceTest`): neue Formen Falltür (Oberkante 0.1875, Kosten 1) und Pfosten (1.0, Kosten 4), Mauer als Material von Zaun/Mauer; jedes Palettenmaterial hat die Oberkante seiner Form (gegen Minestoms Kollisionsform, wo verfügbar). Dann `Surface` und Paletten (D11); `CourseBlock` trägt das gewählte `Block`. Nachweis: Tests grün; Schaffbarkeits-Tests (Oberkantendifferenz ≤ 1.0, z. B. Falltür → Vollblock +1 unzulässig) grün.
- [x] 7.2 Test zuerst (Unit, `CourseGeneratorTest`): mit festem Seed haben 10 Vollblöcke nacheinander mehr als ein Material; gleicher Seed → gleiche Materialien. Dann Materialwahl im Generator. Nachweis: Tests grün.
- [x] 7.3 Test zuerst (Unit, `JumpRulesTest`/`CourseGeneratorTest`, `FakeSpaceProbe`): nach der Aufstiegsphase kein Ziel mit weniger als 4 Blöcken Luft darunter (Spec „Nicht über Wegen“); bei zwei gleich teuren Kandidaten gewinnt der offenere (Spec „Ins Leere bevorzugt“); die Statistik-Tests aus 2.4 bleiben grün. Dann `MIN_AIR_BELOW`, Offenheit und Ranking (D4). Nachweis: Tests grün.
- [x] 7.4 Test zuerst (Unit, `AscentPhaseTest`/`CourseTest`): Aufstieg auf flachem Boden endet erst mit ≥ 4 Blöcken Luft unter dem Block (Spec „Aufstieg bis ins Freie“), mindestens 5 Sprünge, kein Start, wenn das in 20 Sprüngen nicht gelingt; Score 0 nach dem Aufstieg. Dann die dynamische Aufstiegsphase (D5). Nachweis: Tests grün.
- [ ] 7.5 Integration: Die Pakete beim Start und Vorrücken tragen das gewählte Material; Neusenden nutzt dasselbe Material. Nachweis: `./gradlew build` grün, danach erneuter lokaler Test (5.3).

## 8. Sichtbar für andere, mehr Abstand und Luft (Nachtrag nach zweitem lokalen Test)

- [x] 8.1 Test zuerst (Unit): Ziele nach der Aufstiegsphase brauchen ≥ 6 Blöcke Luft darunter und ≥ 16 Blöcke waagrechten Abstand zum Spawn (Spec „Nicht über Wegen“, „Nicht zurück zum Spawn“); Offenheit `0.7 · Säule (16 tief) + 0.3 · Nachbarn`; Statistik-Tests bleiben grün. Dann Generator und `Openness` anpassen (D4). Nachweis: Tests grün.
- [x] 8.2 Test zuerst (Unit): Aufstieg endet erst bei ≥ 16 Blöcken Abstand zum Spawn und ≥ 8 Blöcken Luft darunter, mindestens 5, höchstens 30 Sprünge, sonst kein Start (Spec „Aufstieg bis ins Freie“). Dann die Aufstiegsphase anpassen (D5). Nachweis: Tests grün.
- [x] 8.3 Test zuerst (Integration, Cyano, zwei Spieler): B sieht beim Start 2 Block-Displays im Material der Laufblöcke, A keins; Vorrücken entfernt und spawnt je eins; jedes Laufende und der Shutdown entfernen alle Displays des Laufs; B kann nicht auf einem Display stehen (keine Kollision, z. B. B fällt durch die Position). Dann `Spectators` (D12). Nachweis: Tests grün.
- [x] 8.4 Test zuerst (Integration): Text-Display als Passagier des Läufers zeigt „Jump & Run · <Score>“ nach jedem Sprung, ist für den Läufer unsichtbar und verschwindet mit dem Laufende. Dann umsetzen. Nachweis: `./gradlew build` grün, danach erneuter lokaler Test.

## 9. Item, Kopfanzeige und Ton (Nachtrag nach drittem lokalen Test)

- [x] 9.1 Test zuerst (Unit/Integration): Das Item `titan:jumprun` ist ein Schleimblock mit MiniMessage-gestaltetem Namen „Jump & Run“; `StandardLoadoutTest` in `apps/cloudnet` erwartet Slot 0 mit `SLIME_BLOCK`. Dann `JumprunItems` umstellen (D13). Nachweis: Tests grün.
- [x] 9.2 Prüfen, ob MiniMessage in der mitgelieferten Adventure-Version den `<sprite>`-Tag hat (Ergebnis in D13 eintragen). Test zuerst (Unit): Die Vorlage rendert für Score 7 Symbol, „Jump & Run“ und 7; ohne Sprite-Tag zeigt der Integrationstest ein `ITEM_DISPLAY` mit Schleimblock neben dem Text, unsichtbar für den Läufer. Dann `ScoreLabel` auf die Vorlage umstellen. Nachweis: Tests grün.
- [x] 9.3 Test zuerst (Unit `RunSoundsTest`, Integration): Shepard-Skala nach D13 (Teiltöne steigen je Punkt um einen Halbton, Lautstärke 0 an den Rändern, Summe der Lautstärken konstant); nach einem Punkt bekommt nur der Läufer die Sound-Pakete; Aufstiegssprünge bleiben stumm. Dann `RunSounds` und Aufruf beim Vorrücken. Nachweis: `./gradlew build` grün, danach erneuter lokaler Test.

## 10. Bugfix: Anklicken lässt Blöcke verschwinden

- [x] 10.1 Reproduktion zuerst (Integration, echte Client-Pakete über `processClientPacket`): Links- und Rechtsklick auf einen Laufblock, mit und ohne Jump-and-Run-Item in der Hand, enden mit einem Block-Paket, das den Laufblock zeigt, und der Lauf läuft weiter (Spec „Block anklicken“); rot. Ursache und Fix in D14 nachtragen, dann beheben; grün. Nachweis: Test grün, erneuter lokaler Test.

## 6. Pull Request

- [ ] 6.1 Pull Request vom Integrationszweig `feat/jumprun` auf `main` unter dem Titel `feat(jumprun): add a random single-player jump and run to the lobby` öffnen (Titel und Beschreibung Englisch), mit Smoke-Test-Ergebnis und dem Hinweis auf die Elytra-Abwägung. Nachweis: PR-URL, CI grün.
