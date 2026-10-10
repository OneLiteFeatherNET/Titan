# Tasks

## Execution Plan

Integrationszweig: `feat/spawn-return` von `origin/main`. Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig und starten mit `git reset --hard <Commit-ID des Integrationszweigs>`; nur die explizite ID, nie `HEAD` eines anderen Checkouts. Jede Welle endet mit grünem `./gradlew build`, zwei Reviews (Sonnet Design, Haiku mechanisch) und gefixten Funden.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | core-api | 1.1 | sonnet | `core/src/**` (nur `SpawnReturn`, `LobbyReturnToSpawnEvent`) | `features/**`, `runtime/**`, `apps/**` |
| 2 | spawn | 2.1–2.4 | sonnet | `features/spawn/**` | `core/**`, andere `features/**` |
| 2 | jumprun | 4.1 | sonnet | `features/jumprun/**` | `core/**`, andere `features/**` |
| 3 | navigator | 3.1–3.2 | sonnet | `features/navigator/**` | `core/**`, andere `features/**` |
| 4b | glide-fix | 4.2 | sonnet | `features/spawn/**`, `features/elytra/**` | `core/**`, andere `features/**` |
| 4 | cross-column | 5.1 | sonnet | `apps/cloudnet/src/test/**` | Produktionscode |
| 4 | docs | 5.2 | sonnet | `docs/lobby-modules.md`, `README.md` | Code |
| 5 | smoke | 5.3 | sonnet + Mensch | nur lokale Läufe (Jar kopieren) | Code |
| 5 | verify | 5.4 | haiku | read-only | alles |
| 6 | pr | 6.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seine Aufgabe gelten:

- **Built-in first:** Minestom `Command`/`CommandManager`, `EventDispatcher`/`PlayerEvent`, Aves-Layout; Adventure `TranslationStore`/`GlobalTranslator` mit explizitem `render`; Avaje `Provider` gegen den Modul-Zyklus.
- **Java 25 ohne Preview:** Records, sealed/enums, `_`.
- **Nutzertexte nur über Bundles:** Schlüssel `titan.spawn.*`, Englisch als Fallback.
- **Logging:** SLF4J auf DEBUG mit Key-Values, keine Metriken/Spans.
- **Tests und Kommentare:** Test zuerst; schlanke Kommentare nur fürs Warum.
- **F.I.R.S.T.:**
  - frische `Env` je Test, `env.tick()`;
  - keine Sleeps, keine Systemzeit, keine globale `Config`-Mutation;
  - Assertions mit Meldung;
  - Logs über einen Captured Appender.
- **Commits:** Conventional Commits `feat(spawn): …`.

## 1. Schnittstelle in core (Welle 1)

- [x] 1.1 Test zuerst (Unit, core): `LobbyReturnToSpawnEvent` liefert seinen Spieler über `PlayerEvent#getPlayer`; rot. Dann `SpawnReturn` (`Result sendToSpawn(Player)`, `void sendToSpawnAndTell(Player)`, `enum Result { RETURNED, NO_SPAWN }`) und `record LobbyReturnToSpawnEvent(Player player) implements PlayerEvent` in `net.onelitefeather.titan.core.module`, mit Javadoc zum Zweck (Andockpunkt zum Aufräumen vor dem Teleport). Grün. Nachweis: `./gradlew :core:build` grün.

## 2. Spawn-Column: Rückkehr, Texte, Befehl (Welle 2)

- [x] 2.1 Test zuerst (Unit, `LobbySpawnReturnTest`):
  - Mit Spawn-Position wird das Event genau einmal ausgelöst, und zwar vor dem Teleport. Ein Listener hält dazu die Position beim Event fest; Rückgabe `RETURNED`.
  - Ohne Spawn-Position gibt es kein Event, keinen Teleport und die Rückgabe `NO_SPAWN`.

  Rot. Dann `LobbySpawnReturn` (`@Singleton`, `provides = SpawnReturn.class`). Grün.
- [x] 2.2 Test zuerst (Unit, `SpawnMessagesBundleTest`): Beide Bundles haben dieselben Schlüssel, und jeder verwendete Schlüssel steht im englischen Bundle. Rot. Dann `titan/spawn/messages_en.properties` und `messages_de.properties` mit `titan.spawn.return.done` und `titan.spawn.return.no_spawn` im MiniMessage-Stil der Lobby, dazu `SpawnMessages` (Store registrieren/entfernen, explizit rendern). `sendToSpawnAndTell` sendet den passenden Text. Grün.
- [x] 2.3 Test zuerst (Integration, Cyano-`Env`):
  - Ein Spieler auf einem Dach führt `/spawn` aus und steht danach an `LobbySpawn.position()`; die Bestätigung kam an.
  - `de_DE` bekommt den Text auf Deutsch, `ja_JP` auf Englisch.
  - Ohne Spawn-Punkt kommt die Meldung, und der Spieler wird nicht versetzt.
  - Ein gleitender Spieler gleitet danach nicht mehr.
  - Nach `@PreDestroy` ist `/spawn` nicht registriert.

  Rot. Dann `SpawnCommand` (Minestom `Command`, ohne Bedingung) und die Registrierung im Lebenszyklus der Spawn-Column, `requires = CommandManager.class`. Grün.
- [x] 2.4 Nachweis: `./gradlew build` grün, die Verdrahtungstests der App-Varianten bleiben grün.

## 3. Navigator (Welle 3)

- [x] 3.1 Test zuerst (bestehende Navigator-Tests erweitern bzw. anpassen):
  - Platz 2 zeigt in beiden Menüs (mit und ohne Build-Recht) einen Kompass „Spawn“.
  - „Klick auf leeren Platz“ prüft jetzt Platz 1, der Konfigurationstest nutzt Platz 3.
  - Ein Klick auf Spawn schließt den Navigator, stößt keine `Deliver`-Weiterleitung an und ruft `sendToSpawnAndTell` genau einmal auf (Fake-`SpawnReturn`).

  Rot. Dann der feste Eintrag in `NavigatorModule` (`Provider<SpawnReturn>`, in `@PostConstruct` aufgelöst). Grün.
- [x] 3.2 Test zuerst: Fehlt `SpawnReturn`, scheitert der Start des Navigators mit klarer Meldung; rot. Dann umsetzen; grün. Nachweis: `./gradlew build` grün.

## 4. Jump and Run (Welle 2)

- [x] 4.1 Test zuerst (Integration, Cyano-`Env`, `env.tick()`):
  - Ein Läufer mit Score 12 löst `LobbyReturnToSpawnEvent` aus. Danach ist der Lauf mit Score 12 beendet, die Endmeldung kam, es sind keine Laufblöcke mehr sichtbar, Elytra und Hotbar sind zurück und die Sidebar ist weg. Der Spieler wird nicht an den Startpunkt gesetzt.
  - Ein Spieler ohne Lauf ist unbeeinflusst.

  Rot. Dann `EndReason.SPAWN_RETURN` (Pflichten wie `ABORT`, ohne Startpunkt-Teleport) und der Listener am `FeatureNode`. Grün.

- [x] 4.2 Fix nach Smoke-Test, Test zuerst:
  - **Integration (spawn):** Ein gleitender Spieler mit Schwung hat nach `/spawn` die Geschwindigkeit null und bleibt am Spawn, auch nach einigen `env.tick()`. Dazu `setVelocity(Vec.ZERO)` vor dem Teleport und nach dessen Abschluss in `LobbySpawnReturn`.
  - **Integration (elytra):** `LobbyReturnToSpawnEvent` leert die Nebenhand (Rakete) und vergisst den Boost-Zähler; ohne Gleiten bleibt alles unverändert. Dazu ein Listener in `ElytraModule` am `FeatureNode`.

## 5. Abnahme und Doku (Welle 4–5)

- [x] 5.1 Test zuerst (Integration in `apps/cloudnet`, alle Columns über Avaje):
  - Die Verdrahtung gelingt, `SpawnReturn` ist im Navigator aufgelöst und `/spawn` ist registriert.
  - `/spawn` während eines Laufs beendet den Lauf und versetzt den Spieler an den Spawn.
  - Der Navigator-Klick auf Platz 2 tut dasselbe.

  Nachweis: Test grün.
- [x] 5.2 Doku: `docs/lobby-modules.md` beschreibt `/spawn`, den Navigator-Eintrag und `LobbyReturnToSpawnEvent` als Andockpunkt für Features mit Spielerzustand. Das README listet `/spawn` unter den Spielerbefehlen. Nachweis: Doku nennt Befehl, Platz und Event.
- [x] 5.3 Smoke-Test mit dem Shaded-Jar und echtem Client, deutsch und englisch:
  - `/spawn` vom Dach, beim Gleiten und im Jump-and-Run-Lauf (Score kommt in die Datenbank);
  - Navigator Platz 2;
  - nach Möglichkeit über den Proxy prüfen, dass `/spawn` die Lobby erreicht.

  Nachweis: Checkliste im PR-Text.
- [x] 5.4 Verifikation (read-only): Jedes Szenario der drei Spec-Deltas ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. ist geprüft. Nachweis: Zuordnungstabelle im PR-Text.

## 6. Pull Request

- [x] 6.1 Pull Request vom Integrationszweig `feat/spawn-return` auf `main` unter dem Titel `feat(spawn): let players return to spawn with /spawn and the navigator` öffnen (Titel und Beschreibung Englisch), mit Smoke-Checkliste und Szenario-Zuordnung. Nachweis: PR #354 (gemergt, `16b3635`).
