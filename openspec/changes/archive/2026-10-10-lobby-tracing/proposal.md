# Proposal

## Why

Live läuft jeder CloudNet-Service mit dem OpenTelemetry-Java-Agent 2.16.0 (`-Dotel.service.name=Lobby`, `-Dotel.traces.exporter=otlp`, `-Dotel.metrics.exporter=otlp`, Tempo und Mimir dahinter). Tempo zeigt heute nur automatisch erzeugte Spans (Hibernate, JDBC). Die Lobby selbst hat weder Spans noch eigene Metriken:

- Warum ein Lauf endete, ein Portal-Transfer scheiterte, ein Spieler zum Spawn zurückgesetzt wurde oder eine Saison wechselte, steht höchstens in DEBUG-Logs, die live nicht nach Loki gehen.
- Fehler in Feature-Listenern landen nur im Log, nicht als Zähler oder am Span.
- Start, Bean-Aufbau, Join und Disconnect sind unsichtbar. Die Hibernate-Spans hängen an keinem Elternspan.

Das Team will Traces und Metriken für **alles** in der Lobby.

## Aufteilung in mehrere Changes

Ein Pull Request für alle 14 Module wäre zu groß zum Prüfen. Die Arbeit teilt sich daher in ein Fundament und sechs Modul-Changes, jeder mit eigenem `feat(telemetry)`-Titel und eigenem PR:

| Change | Inhalt | PR-Titel |
| --- | --- | --- |
| `lobby-tracing` (dieser) | Fundament: `Telemetry`-Bean, `FeatureNode.onTraced`, Fehlerzähler im `ListenerGuard`, Testhilfen, `DatabaseWriter`-Kontext, Lifecycle (Start, Stop, Join, Configure, Disconnect) | `feat(telemetry): add tracing and metrics foundation with lifecycle spans` |
| `lobby-tracing-jumprun` | `jumprun` | `feat(telemetry): trace jump and run starts, ends and leaderboard refreshes` |
| `lobby-tracing-portal` | `portal`, `Deliver` (CloudNet-Transfer), Spielerzahl-Abfrage | `feat(telemetry): trace portal transfers and player count lookups` |
| `lobby-tracing-spawn-navigator` | `spawn`, `navigator` | `feat(telemetry): trace spawn returns and navigator use` |
| `lobby-tracing-movement` | `sit`, `elytra`, `tickle`, `respawn` | `feat(telemetry): trace sit, elytra, tickle and respawn` |
| `lobby-tracing-world` | `season`, `daytime`, `protection` | `feat(telemetry): trace season changes, daytime and protection denials` |
| `lobby-tracing-admin-permissions` | `admin`, `hotbar`, `platform/luckperms` | `feat(telemetry): trace admin commands, hotbar items and permission checks` |

Rollout: Zuerst das Fundament und `lobby-tracing-jumprun`, die anderen fünf später. Das Fundament muss zuerst auf `main`. Danach sind die sechs Modul-Changes unabhängig, jeder berührt nur sein Modul, und sie können parallel in eigenen Worktrees entstehen (Berührungspunkte nur die eigene `FeatureNode.attach`-Zeile). Bis ein Modul migriert ist, läuft es unverändert, weil `FeatureNode.attach` ohne `Telemetry` ein No-op nutzt.

## What Changes (dieser Change: das Fundament)

- `core` hängt an `opentelemetry-api` und bekommt `Telemetry` (Tracer + Meter, `noop()`, `of(OpenTelemetry)`, `inSpan(...)`). `runtime` stellt `OpenTelemetry` und `Telemetry` als Beans bereit, aus `GlobalOpenTelemetry` (der Agent liefert das SDK, ohne Agent ist alles No-op).
- `FeatureNode` bekommt `attach(..., Telemetry)` und `onTraced(type, spanName, listener)`: ein Listener, der in einem kurzen Span läuft, die Spieler-UUID trägt und Ausnahmen mit `recordException` und Status ERROR festhält. Hochfrequente Event-Typen (`PlayerMoveEvent`, Packet-, Chunk-, Tick-Events) lehnt `onTraced` beim Registrieren ab.
- `ListenerGuard` zählt jeden gefangenen Fehler eines Listeners (`titan.listener.failures{titan.feature}`) und kennt den `Tracer` ohne statischen Zustand, weil `FeatureNode` ihn pro Instanz hineinreicht.
- Lifecycle in `runtime`: Spans `titan.startup`, `titan.shutdown`, `player.configure`, `player.join`, `player.disconnect`, Span-Events je gestartetem/gestopptem Feature, Zähler und ein Gauge für die Spielerzahl.
- `persistence`: `DatabaseWriter` reicht den Kontext des Auftraggebers an seinen Virtual-Thread-Executor weiter (`Context.current().wrap`), damit Hibernate-Spans unter dem auslösenden Span hängen.
- Testhilfen in `core` (`testFixtures`): `TestTelemetry` mit frischem `InMemorySpanExporter` und `InMemoryMetricReader` je Test.
- Dokumentation und ArchUnit-Regel (`GlobalOpenTelemetry` nur in `runtime`).

Nicht Teil dieser Änderung: Spans der einzelnen Module (die sechs Folge-Changes), Spans über mehrere Minuten, Spans je Tick oder je Bewegung, Logs über OTel.

## Capabilities

### New Capabilities
- `lobby-tracing`: Grundregeln für Spans und Metriken der Lobby (No-op ohne Agent, nur UUID, Hochfrequenz-Ausschluss, Fehler am Span, Kontext über Executor) und die Lifecycle-Spans. Die Modul-Changes ergänzen dieselbe Capability um ihre eigenen Anforderungen.

### Modified Capabilities

## Impact

- **Code:** `core` (`Telemetry`, `FeatureNode`, `ListenerGuard`, testFixtures), `runtime` (`PlatformBeans`, `package-info.java`, Lifecycle-Klasse, `Titan`), `persistence` (`DatabaseWriter`), Versionskatalog in `settings.gradle.kts`.
- **Abhängigkeiten:** `opentelemetry-api` (Laufzeit, in `core` als `api`, in `persistence` nur für `Context`), `opentelemetry-sdk-testing` (nur Test), über `opentelemetry-bom` Version 1.50.0, im Gleichklang mit dem Agent 2.16.0. Renovate hält sie aktuell.
- **Betrieb:** keine neue Konfiguration. Der Agent, die Exporter und der Dienstname stehen live schon. Nach dem Deploy erscheinen Spans in Tempo und Zähler in Mimir unter `Lobby`.
- **Spielerverhalten:** keines. **Nutzertexte:** keine. **Logs:** unverändert.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): add tracing and metrics foundation with lifecycle spans`

Ein Typ je Change: Die Abhängigkeiten dienen nur der Telemetrie und gehören in den `feat`-Commit, nicht in einen eigenen `build`-Commit. Das Scope-Wort `telemetry` steht fest, weil die Änderungen mehrere Module berühren.
