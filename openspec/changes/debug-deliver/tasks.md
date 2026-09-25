# Tasks

## Execution Plan

Kleine, in sich geschlossene Änderung an genau einer Klasse plus ihrem Erzeuger; keine Wellen mit parallelen Worktree-Agents nötig. Implementiert und lokal abgenommen auf `feat/debug-deliver` (zwei Commits: `feat(common): report navigator deliveries in local runs`, `refactor(common): show debug deliver targets unparsed`).

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1    | (direkt, kein Subagent) | 1.1–1.4, 2.1 | sonnet | `common/src/main/java/net/onelitefeather/titan/common/deliver/**`, `common/src/test/java/net/onelitefeather/titan/common/deliver/**` | `app/**`, `setup/**`, Config-Dateien |

## 1. DebugDeliver test-first umsetzen

- [x] 1.1 Roten Unit-Test `DebugDeliverTest` gegen die noch nicht existierende `DebugDeliver` schreiben: Chatnachricht und eine `INFO`-Log-Zeile für `TaskComponent` und für `ServerDeliverComponent`, je mit Ziel-Namen im Text. Testpyramide: Unit-Test mit `MicrotusExtension`/`Env` (Cyano), keine echte CloudNet-Umgebung nötig. Verifikation: Test schlägt fehl, weil `DebugDeliver` fehlt.
- [x] 1.2 `DebugDeliver` implementieren (`sendPlayer` sendet die Chatzeile per Inline-MiniMessage und loggt genau eine `INFO`-Zeile), bis 1.1 grün ist. Verifikation: `./gradlew :common:test --tests "*DebugDeliverTest*"` ist grün.
- [x] 1.3 Null-Sicherheit testen und sicherstellen (`sendPlayer(null, …)`, `sendPlayer(player, null)` tun nichts und loggen nichts), analog zu `MessageChannelDeliver`. Verifikation: `DebugDeliverTest#sendPlayerWithNullPlayerDoesNothing` und `#sendPlayerWithNullComponentDoesNothing` sind grün.
- [x] 1.4 Konfigurierte Ziele dürfen nicht als MiniMessage interpretiert werden: Test mit einem Zielnamen wie `<red>evil`, danach `Placeholder.unparsed("target", …)` statt Konkatenation verwenden. Verifikation: `DebugDeliverTest#sendPlayerWithMiniMessageLikeTargetShowsItLiterally` ist grün.

## 2. DeliverProvider umstellen und NoopDeliver entfernen

- [x] 2.1 `DeliverProvider.create()` liefert ohne CloudNet `DebugDeliver` statt `NoopDeliver`; `NoopDeliver.java` und seine Tests entfernen. Verifikation: `./gradlew :common:build` ist grün, `grep -rn "NoopDeliver" --include=*.java .` findet nichts mehr außerhalb der Git-Historie.

## 3. Manuelle Abnahme im Client

- [x] 3.1 Lobby lokal ohne CloudNet starten und den Navigator in vier laufenden Server-Setups anklicken: ElytraRace, Survival, cygnus, MemberBuild. Erwartung je Klick: Chatnachricht nennt das angeklickte Ziel, kein Fehler, kein Absturz. Verifikation: In allen vier Fällen erschien die erwartete Nachricht (manuelle Abnahme durch den Maintainer, hier dokumentiert statt im PR-Body).

## 4. Pull Request

- [x] 4.1 Pull Request von `feat/debug-deliver` nach `main` mit dem Titel `feat(common): report navigator deliveries in local runs` öffnen. Beschreibung auf Englisch, nennt: den fixen Zustand vorher (`NoopDeliver`, Klicks taten nichts, lokales Testen unmöglich), was sich ändert (`DebugDeliver` mit Chatnachricht + `INFO`-Log ohne CloudNet, `MessageChannelDeliver` mit CloudNet unverändert, kein neuer Schalter), und die manuelle Abnahme aus 3.1 (ElytraRace, Survival, cygnus, MemberBuild). Verifikation: Der PR existiert, und der CI-Lauf ist grün.
