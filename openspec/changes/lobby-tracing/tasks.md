# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing` von `origin/main`. Dieser Change ist das **Fundament**; die sechs Modul-Changes (`lobby-tracing-jumprun`, `-portal`, `-spawn-navigator`, `-movement`, `-world`, `-admin-permissions`) starten erst, wenn dieser auf `main` ist, und laufen dann parallel (siehe proposal.md). Agents, die schreiben, arbeiten in eigenen Worktrees vom Integrationszweig. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | core-api | 1.1–1.5 | sonnet | `settings.gradle.kts` (Katalog), `core/**` | `runtime/**`, `persistence/**`, `features/**` |
| 1 | db-context | 2.1–2.2 | sonnet | `persistence/**` | alles andere |
| 2 | runtime-beans | 3.1–3.2 | sonnet | `runtime/**` (Beans, `package-info.java`) | `core/**`, `features/**` |
| 3 | lifecycle | 4.1–4.4 | sonnet | `runtime/**` (Lifecycle-Klasse, `Titan`) | `core/**`, `features/**` |
| 3 | arch-docs | 5.1–5.2 | sonnet | ArchUnit-Regel in `core/testFixtures`, `docs/lobby-modules.md` | Produktionscode |
| 4 | smoke | 5.3 | sonnet + Mensch | nur lokale Läufe (Jar kopieren) | Code |
| 4 | verify | 5.4 | haiku | read-only | alles |
| 5 | pr | 6.1 | sonnet | Git/GitHub | Code |

Wave 2 wartet auf 1.x (`Telemetry` muss stehen), Wave 3 auf 3.x. 2.x ist unabhängig und läuft mit Wave 1.

Jeder Agent-Prompt nennt die Regeln:
- **Built-in first:** OTel-API plus Agent, kein SDK in der Lobby, kein `@WithSpan`, kein eigenes Framework.
- **Java 25 ohne Preview.**
- **Test zuerst:** erst der rote Test, dann der Code.
- **F.I.R.S.T.:** je Test ein frisches `TestTelemetry` (nie `GlobalOpenTelemetry`, nie `OpenTelemetryExtension.create()`), `SimpleSpanProcessor`, kein `Thread.sleep`, `Clock` injiziert, frische Minestom-`Env` je Test mit `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick), dort Zähler. Metriken ohne `user.id`.
- **Kommentare:** schlank, nur fürs Warum. **Logs:** unverändert.
- **Commits:** Conventional Commits `feat(telemetry): …`, ein Typ.

## 1. `Telemetry`, `FeatureNode`, Testhilfen (Welle 1)

- [x] 1.1 `opentelemetry-bom` im Versionskatalog (`settings.gradle.kts`) mit `opentelemetry-api` und `opentelemetry-sdk-testing`, BOM-Version 1.50.0 (die API-Version, die der Agent 2.16.0 bündelt), Kommentar dazu. `core`: `api(libs.opentelemetry.api)`; `testFixturesApi` bekommt `opentelemetry-sdk-testing`. Nachweis: `./gradlew :core:build`.
- [x] 1.2 Test zuerst (Unit, `TestTelemetry` und `TelemetryTest`): `TestTelemetry` liefert je Aufruf einen unabhängigen Satz aus Spans und Zählern; zwei Instanzen sehen sich nicht. `Telemetry.inSpan` setzt Attribute, endet im `finally`, zeichnet eine Ausnahme mit Status ERROR auf und wirft sie weiter; `Telemetry.noop()` wirft nie und erzeugt nichts. Rot. Dann `Telemetry` (`core.telemetry`) und `TestTelemetry` (`core/testFixtures`: `spans()`, `span(name)`, `attribute(...)`, `counter(name, attributes)`). Grün.
- [x] 1.3 Test zuerst (Unit, `FeatureNodeTracingTest`): `onTraced` führt den Listener in einem Span mit `titan.feature` und, bei `PlayerEvent`, `user.id` aus; wirft er, trägt der Span Ausnahme und Status ERROR, `titan.listener.failures` steigt, und die Ausnahme erreicht den Guard (bestehendes Log-Verhalten bleibt, wie in `ListenerGuardTest` per Appender geprüft). Rot. Dann `FeatureNode.attach(…, Telemetry)`, `onTraced`, `onTracedIncludingCancelled` und `ListenerGuard.guard(featureId, telemetry, listener)`. Grün.
- [x] 1.4 Test zuerst (Unit, parametrisiert): `onTraced` lehnt `PlayerMoveEvent`, `PlayerPacketEvent`, `PlayerPacketOutEvent`, `PlayerChunkLoadEvent`, `PlayerChunkUnloadEvent`, `EntityTickEvent`, `InstanceTickEvent` und `ServerTickMonitorEvent` beim Registrieren mit `IllegalArgumentException` ab (und eine Unterklasse davon); ein normales Event wird angenommen. Rot, dann Sperrliste in `FeatureNode`. Grün.
- [x] 1.5 Test zuerst (Unit): Der alte `attach(parent, id, priority)` funktioniert unverändert mit `Telemetry.noop()`; `attach` und `close` hängen die Span-Events `feature.started`/`feature.stopped` mit `titan.feature` und `titan.feature.priority` an den aktuellen Span, und ohne aktuellen Span passiert nichts. Rot, dann grün. Nachweis: `./gradlew :core:build`, `FeatureNodeTest` und `ListenerGuardTest` bleiben grün.

## 2. Kontext im `DatabaseWriter` (Welle 1)

- [x] 2.1 Test zuerst (Unit, persistence): Ein Task, den der Writer erst später ausführt, sieht den Span, der beim `execute` aktuell war (`Span.current()` im Task hat `traceId` und `spanId` des Elternspans). Rot. Dann `DatabaseWriter.execute` mit `Context.current().wrap(task)`; `persistence` bekommt `implementation(libs.opentelemetry.api)`. Grün.
- [x] 2.2 Nachweis: persistence-Tests inklusive Testcontainers-Integration bleiben grün, `./gradlew :persistence:build`; `close()` leert weiterhin die offenen Tasks.

## 3. Beans in `runtime` (Welle 2)

- [x] 3.1 Test zuerst (Unit, runtime): Der `BeanScope` der Variant-Tests enthält `OpenTelemetry` und `Telemetry`; ohne Agent sind sie No-op (ein Span wirft nicht, hat keine gültige Kontext-Id). Rot. Dann `PlatformBeans` (`@Bean OpenTelemetry` aus `GlobalOpenTelemetry.get()`, `@Bean Telemetry` mit Scope `net.onelitefeather.titan`) und beide in `provides` von `package-info.java`. Grün.
- [x] 3.2 Nachweis: `VariantStartupCheck`-Tests und `WiringTest` der Apps bleiben grün (`./gradlew :runtime:build :apps:cloudnet:build :apps:local:build`).

## 4. Lifecycle (Welle 3)

- [x] 4.1 Test zuerst (Unit, runtime): `titan.startup` umspannt den Aufbau, trägt `titan.variant`, `titan.profiles`, `titan.modules.loaded` und die `feature.started`-Events in Startreihenfolge; bei einer Ausnahme im Aufbau Ausnahme und ERROR, und sie geht weiter. Rot, dann `Titan()` auf `Telemetry.inSpan` umstellen. Grün.
- [x] 4.2 Test zuerst (Unit): `titan.shutdown` umspannt `beanScope.close()` mit `feature.stopped`-Events. Rot, dann grün.
- [x] 4.3 Test zuerst (Integration, Cyano-`Env`, `env.tick()`): Konfiguration, Beitritt und Disconnect eines Test-Spielers erzeugen `player.configure`, `player.join`, `player.disconnect` mit `user.id`; `player.joins` und `player.disconnects` stehen auf eins; das Gauge `titan.players.online` zeigt die aktuelle Zahl; kein Attributwert enthält den Spielernamen. Rot. Dann die Lifecycle-Klasse in `runtime` (ein `FeatureNode` mit `onTraced`, Gauge über den `ConnectionManager`). Grün.
- [x] 4.4 Nachweis: Ohne Agent läuft der Test aus 4.3 mit `Telemetry.noop()` ohne Ausnahme; `./gradlew :runtime:build`.

## 5. Regeln, Doku, Abnahme (Welle 3–4)

- [x] 5.1 Test zuerst (ArchUnit in `ColumnArchitectureRules`/runtime): Nur `runtime` darf `GlobalOpenTelemetry` aufrufen, und `features/**` dürfen `io.opentelemetry.sdk` nicht verwenden. Rot (mit einer verletzenden Fixture), dann Regel. Grün.
- [x] 5.2 `docs/lobby-modules.md`: Abschnitt „Traces und Metriken“: `Telemetry` als Bean, `FeatureNode.attach(…, telemetry)`, `onTraced` und die Sperrliste, die Leitlinie Span / Span-Event / Zähler (D4), Datenschutz, Attributnamen, Testen mit `TestTelemetry`, lokal sichtbar machen mit `-javaagent` und dem `logging`-Exporter. Nachweis: Doku nennt alle genannten Punkte.
- [ ] 5.3 Smoke-Test mit dem Shaded-Jar: ohne Agent starten, beitreten, gehen (keine Fehler). Mit Agent und `logging`-Exporter: `titan.startup` mit den Feature-Events, `player.*`-Spans, Zähler `player.joins`. Nachweis: Konsolenauszug im PR-Text. Nach dem Deploy (Mensch): in Tempo `{ name = "titan.startup" }`, in Mimir `titan_players_online`.
- [x] 5.4 Verifikation (read-only): Jedes Szenario des Spec-Deltas ist einem Test oder Smoke-Punkt zugeordnet, Tests erfüllen F.I.R.S.T. (kein globaler OTel-Zustand, keine Sleeps). Nachweis: Zuordnungstabelle im PR-Text.

## 6. Pull Request

- [ ] 6.1 Pull Request vom Integrationszweig `feat/lobby-tracing` auf `main` unter dem Titel `feat(telemetry): add tracing and metrics foundation with lifecycle spans` öffnen (Titel und Beschreibung Englisch), mit Smoke-Checkliste und Szenario-Zuordnung. Nachweis: PR-URL, CI grün.
