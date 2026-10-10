# Tasks

## Execution Plan

Kleine Änderung an drei Klassen in `common` und einem Aufruf in `runtime`; eine Welle, ein Agent, kein Parallelisieren. Zweig `feat/bridge-connector-warning` von `origin/main`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | connector-warning | 1.1–2.3 | sonnet | `common/src/main/java/**/deliver/**`, `common/src/test/java/**/deliver/**`, `runtime/src/main/java/**/TitanApplication.java` | `bridge/**`, `features/**`, `core/**`, Build-Dateien, `openspec/specs/**` |
| 2 | verify | 3.1 | haiku | read-only | alles |
| 3 | pr | 4.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt: SLF4J mit `{}`-Parametern, eine `private static final Logger` je Klasse, Logback-`ListAppender` zum Prüfen (kein Konsolen-Lesen), Test zuerst, F.I.R.S.T. (kein Sleep, keine Systemzeit, statischen Halter in `@AfterEach` mit `setConnector(null)` zurücksetzen), keine neuen Nutzertexte, keine Metriken/Spans, schlanke Kommentare, Java 25 ohne Preview, Conventional Commit `feat(bridge): …`.

## 1. Verpasste Weiterleitung melden

- [ ] 1.1 Test zuerst (Unit, `common`, `MessageChannelDeliverTest` mit `ListAppender`): Ohne installierten Connector loggt `sendPlayer` für `TaskComponent` und für `ServerDeliverComponent` je genau eine `WARN`-Zeile mit Spielername, UUID, `task`/`server` und Ziel und wirft nichts; `sendPlayer(null, …)` und `sendPlayer(player, null)` loggen nichts. Nachweis: Test rot.
- [ ] 1.2 Test zuerst (Unit): Mit einem Fake-`ServerConnector` erreicht der Aufruf den Connector, und es wird nichts geloggt; `TitanServerConnector.connectToTask/connectToServer` liefern `true` mit und `false` ohne Connector. Nachweis: Test rot.
- [ ] 1.3 `TitanServerConnector`: `connectToTask`/`connectToServer` liefern `boolean`, `isInstalled()` ergänzen; `MessageChannelDeliver` loggt bei `false` die `WARN`-Zeile aus design.md D1. Halter bleibt statisch. Nachweis: 1.1 und 1.2 grün, `./gradlew :common:test` grün.

## 2. Startprüfung

- [ ] 2.1 Test zuerst (Unit, `common`, `ConnectorStartupCheckTest` mit `ListAppender`): Von den vier Kombinationen `(cloudNetPresent, connectorInstalled)` loggt nur `(true, false)` genau einen `ERROR`, der `TitanCloudNetPermissions` und `CloudNet_Bridge` nennt; die anderen drei loggen nichts. Nachweis: Test rot.
- [ ] 2.2 `ConnectorStartupCheck` mit `verify(boolean, boolean)` und argumentloser Überladung über `CloudNetEnvironment.isPresent()` und `TitanServerConnector.isInstalled()` (design.md D2). Nachweis: 2.1 grün.
- [ ] 2.3 In `TitanApplication.main` `ConnectorStartupCheck.verify()` unmittelbar nach `bootstrap.start(...)` aufrufen, mit Kommentar zum Warum (Extensions initialisieren erst in `start`). Nachweis: `./gradlew build` grün; `grep -rn ConnectorStartupCheck runtime/src/main` trifft nur `TitanApplication.java`, nicht `Titan.java`.

## 3. Verifikation und Abnahme

- [ ] 3.1 Verifikation (Haiku, read-only): Jedes Szenario aus `specs/lobby-navigator` einem Test zuordnen; F.I.R.S.T.-Check (kein Sleep, keine Systemzeit, `setConnector(null)` und Appender-Detach in jedem Test, keine Abhängigkeit zwischen Tests). Nachweis: Bericht ohne Lücken.
- [ ] 3.2 Manuelle Abnahme (Maintainer, Lobby mit `.wrapper`): Ohne `TitanCloudNetPermissions` im `extensions/`-Ordner zeigt das Log nach dem Start einmal den ERROR, ein Navigator-Klick erzeugt die WARN-Zeile; mit der Extension erscheinen beide nicht und die Weiterleitung klappt. Nachweis: Protokoll im PR.

## 4. Pull Request

- [ ] 4.1 Pull Request von `feat/bridge-connector-warning` nach `main` unter dem Titel `feat(bridge): warn when the server connector is missing` öffnen; englische Beschreibung nennt den stillen Ausfall vorher, die zwei neuen Logzeilen (WARN je verpasstem Klick, ERROR einmal nach dem Laden der Extensions), warum der Check nach `start` steht, die Nicht-Ziele (kein Chat, kein Abbruch) und das Abnahmeprotokoll aus 3.2. Nachweis: PR-URL, CI grün.
