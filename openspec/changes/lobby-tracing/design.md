# Design

## Context

Ausgangslage (`origin/main`):

- Live: Agent 2.16.0, Traces und Metriken per OTLP (Tempo, Mimir), `-Dotel.service.name=Lobby`, dazu die Agent-Extension `MinecraftOTEL.jar`. Der Wurzelspan `titan` ist der Hikari-Poolname und bleibt unangetastet.
- Titan hat kein OpenTelemetry im Code (`archive/…-avaje-dependency-injection/design.md` hat es als eigenen Change vorgemerkt).
- Columns hängen nur an `core`. Plattform-Beans (`Clock`, `Scheduler`, `Deliver`, …) liefert `runtime` in `PlatformBeans` und listet sie in `provides` von `package-info.java`; Columns bekommen sie per Konstruktor und `requires`.
- Alle Listener laufen über `FeatureNode.on`/`onIncludingCancelled` → `ListenerGuard.guard(featureId, …)`. `ListenerGuard` ist statisch, paketprivat, ohne DI und ordnet Fehler per MDC/Log einem Feature und Spieler zu. Er fängt `Throwable`, merkt Spieler und Modul in `ThreadLocal`s und wirft weiter; `handleException` loggt später.
- Gestartet wird in `Titan()` (runtime): `BeanScope.builder()…build()` führt alle `@PostConstruct` aus (jedes Feature ruft darin `FeatureNode.attach`), danach `VariantStartupCheck`, `FeatureStartupLog`. `initialize()` hängt `beanScope::close` an den Shutdown-Task (jedes `@PreDestroy`, `FeatureNode.close`).
- `DatabaseWriter` (persistence) besitzt einen `Executors.newVirtualThreadPerTaskExecutor()`. `persistence` hängt nicht an `core`.
- Module in der Lobby (live gestartet): `adminColumn`, `daytimeColumn`, `elytraColumn`, `hotbarColumn`, `jumprunColumn`, `luckpermsPlatform`, `navigatorColumn`, `portalColumn`, `protectionColumn`, `respawnColumn`, `seasonColumn`, `sitColumn`, `spawnColumn`, `tickleColumn`. Nicht in der Lobby: `setup` (eigener Setup-Server mit eigenem Launcher) und `butterfly` (externe Extension). `bridge` ist eine eigene Minestom-Extension mit isoliertem Classloader (siehe D8).

## Entscheidungen des Teams

- CloudNet-Transfers werden über einen `TracedDeliver`-Dekorator in `runtime` getraced; die statischen `TitanServerConnector`/`TitanPlayerCountLookup` bleiben unberührt.
- `bridge` bekommt keine OTel-Abhängigkeit und keine eigenen Spans.
- `daytime` und `tickle` bleiben nur mit Metriken; ihre Ausnahmen deckt `titan.listener.failures` ab.
- Kein Span `persistence.migrate`, auch nicht als Folgearbeit: Flyway loggt schon nach Loki.
- OTel-API/BOM 1.50.0 (siehe D1). Scope `telemetry`, nur UUID als `user.id`, Metriken werden live exportiert (Mimir), der Wurzelspan `titan` (Hikari-Poolname) bleibt unangetastet.

## Rollout-Reihenfolge

Zuerst das Fundament (`lobby-tracing`) und `lobby-tracing-jumprun`: Das Fundament muss zuerst auf `main`, jumprun ist das Referenzmodul und der Anlass. Die übrigen fünf Modul-Changes (`-portal`, `-spawn-navigator`, `-movement`, `-world`, `-admin-permissions`) folgen später, unabhängig voneinander.

## Goals / Non-Goals

**Goals:**
- Jedes Modul bekommt kurze Spans für seine bedeutsamen, seltenen Operationen und Zähler für häufige.
- Fehler in Listenern sind als Zähler und (bei `onTraced`) am Span sichtbar.
- Ohne Agent: unverändert, Kosten nahe null.

**Non-Goals:**
- Eigenes Telemetrie-Framework, eigene Exporter, SDK in der Lobby.
- Spans für `PlayerMoveEvent`, Packet-, Chunk- oder Tick-Events, Spans über Minuten.
- Logs über OTel.

## Decisions

### D1 Nur die OTel-API im Code, der Agent liefert das SDK

`core` bekommt `api(libs.opentelemetry.api)` über die `opentelemetry-bom` im Versionskatalog mit **BOM-Version 1.50.0**: Der Agent 2.16.0 (Instrumentation 2.16.0) bündelt OpenTelemetry Java SDK/API 1.50.0, und die Live-Resource-Attribute zeigen `telemetry.sdk.version 1.50.0`. Ein kompatibler Patch-Stand 1.50.x ist zulässig. Renovate hebt API und Agent gemeinsam an; die Versionen laufen nicht auseinander. `@WithSpan` ist verworfen: Es wirkt nur mit Agent, ist ohne ihn nicht testbar und kann spät bekannte Attribute nicht setzen. Ein eigenes SDK neben dem Agent ist verworfen (doppelte Konfiguration).

### D2 `Telemetry` als eine kleine Bean aus `core`

`net.onelitefeather.titan.core.telemetry.Telemetry` ist ein Record aus `Tracer` und `Meter` mit `noop()`, `of(OpenTelemetry)` und `inSpan(name, attributes, body)`. `inSpan` kapselt Start, `makeCurrent`, `recordException` + Status ERROR bei Ausnahme (die weitergeworfen wird) und `end` im `finally`. Das ist die einzige Stelle mit dieser Schleife, 14 Module kopieren sie nicht (DRY). Eine Bean statt zwei, damit `FeatureNode.attach` nur ein Argument mehr bekommt.

`runtime` liefert `@Bean OpenTelemetry` (aus `GlobalOpenTelemetry.get()`, beim Aufruf hat der Agent es gesetzt) und `@Bean Telemetry` (Instrumentation-Scope `net.onelitefeather.titan`), beide in `provides`. Module bekommen `Telemetry` per Konstruktor, Tests bauen `TestTelemetry`. Kein Feature ruft `GlobalOpenTelemetry` (ArchUnit-Regel), kein statischer Zustand leckt zwischen Tests.

### D3 `FeatureNode.onTraced` und der Zugriff des `ListenerGuard` auf den Tracer

`FeatureNode.attach(parent, id, priority, telemetry)` hält die `Telemetry` pro Instanz. Der alte Dreiargument-`attach` bleibt und nutzt `Telemetry.noop()`; Module migrieren in ihrem eigenen Change. So braucht der statische `ListenerGuard` keinen globalen Tracer: `FeatureNode` reicht ihn bei jedem `guard(featureId, telemetry, listener)` hinein.

- `on(...)` bleibt wie heute, der `ListenerGuard` zählt zusätzlich jeden gefangenen Fehler: Zähler `titan.listener.failures{titan.feature}` (ein Zähleraufruf, keine Allokation im Normalfall).
- `onTraced(Class<E>, String spanName, Consumer<E>)` und `onTracedIncludingCancelled(...)`: Der Listener läuft in einem Span (`titan.feature`, bei `PlayerEvent` `user.id`). Wirft er, trägt der Span die Ausnahme und Status ERROR, danach geht sie weiter an den bestehenden Guard (Log, MDC wie bisher). Reihenfolge: Guard außen, Span innen.
- **Hochfrequenz-Sperre:** `onTraced` wirft beim Registrieren `IllegalArgumentException` für `PlayerMoveEvent`, `PlayerPacketEvent`, `PlayerPacketOutEvent`, `PlayerChunkLoadEvent`, `PlayerChunkUnloadEvent`, `EntityTickEvent`, `InstanceTickEvent`, `ServerTickMonitorEvent` und jede Unterklasse. Für solche Events gibt es nur Zähler (`on` plus Meter) oder Span-Events auf einem schon laufenden Span.
- **Verworfen:** ein Span in jedem `on`. Er würde pro Spieler und Tick tausende Spans erzeugen.

### D4 Leitlinie: Span, Span-Event oder Zähler

| Operation | Mittel |
| --- | --- |
| selten (höchstens ein paar je Spieler und Minute), hat Ergebnis oder Dauer, Fehler möglich | Span, Name `<modul>.<operation>` |
| häufig, nur das Zählen zählt (Bewegung, Schaden, Rechteprüfung, Block-Abbau) | Zähler `<modul>.<größe>{ergebnis}` |
| häufig, aber im Lauf eines schon bestehenden Spans | Span-Event am aktuellen Span (`Span.current().addEvent`, No-op ohne Span) |
| Verteilung (Score, Dauer) | Histogramm |

Zähler-Attribute haben eine kleine, feste Wertemenge (Grund, Ergebnis, Modus, Portal-ID). **Nie** `user.id` oder Namen an Metriken (Kardinalität).

### D5 Attribute und Datenschutz

Eigene Schlüssel unter `<modul>.*`, gemeinsam `titan.feature` und `user.id` (Semantic Conventions). `user.id` ist die **Spieler-UUID** (vom Team bestätigt). Kein Spielername, keine IP, keine Locale, kein Chat. Spannamen sind feste Strings. Ergebnis-Attribute sind kleine Aufzählungen (`ok`, `denied`, `error`, …).

### D6 Kontext über Threads: `DatabaseWriter`

Der Executor wird in `DatabaseWriter` gebaut (`Executors.newVirtualThreadPerTaskExecutor()`). Dort wird der Kontext des Aufrufers an den Task gehängt: `tasks.execute(Context.current().wrap(task))`. Das ist die eine Stelle für jeden Datenbank-Auftrag aller Module, `persistence` braucht dafür nur `opentelemetry-api` (`Context`), keinen `Tracer`. Der Agent trägt den Kontext über `Executor` nicht von selbst mit (`otel.instrumentation.executors.include` live nicht gesetzt). Der Minestom-`Scheduler` ist die zweite Stelle: Ein Scheduler-Task ist eine Wurzel, Spans dort starten im Task selbst, ohne Übergabe.

### D7 Lifecycle in `runtime`

- `titan.startup`: Span um den Aufbau des `BeanScope` und `VariantStartupCheck` in `Titan()`. Attribute: `titan.variant` (cloudnet/local), `titan.profiles`, `titan.modules.loaded`. Jedes `FeatureNode.attach` hängt ein **Span-Event** `feature.started` (`titan.feature`, `titan.feature.priority`) an den aktuellen Span, so sieht man die Startreihenfolge und die Dauer bis zum Feature, ohne in 14 Modulen einen Span einzubauen. Eine Ausnahme beim Aufbau setzt Status ERROR und geht weiter.
- `titan.shutdown`: Span um `beanScope.close()`; `FeatureNode.close` hängt `feature.stopped` an. `close` leert den `DatabaseWriter`. Der Agent flusht seine Exporter im JVM-Shutdown-Hook; die letzten Spans können trotzdem verloren gehen (hinnehmbar).
- `player.configure`, `player.join`, `player.disconnect`: eine Runtime-Klasse registriert über `FeatureNode.onTraced` je einen Span für `AsyncPlayerConfigurationEvent`, `PlayerSpawnEvent` und `PlayerDisconnectEvent`. Diese Events sind einmal je Verbindung, also selten. Disconnect läuft auf dem virtuellen Read-Loop-Thread: Der Span startet und endet dort. Zähler `player.joins`, `player.disconnects`, ein Gauge `titan.players.online` (observable, aus dem `ConnectionManager`).

### D8 Was nicht in der Lobby läuft oder nicht direkt tracebar ist

- `setup` ist ein eigener Server (`TitanLauncher`), nicht Teil der Lobby-Varianten: nicht Teil.
- `butterfly` ist eine externe Extension: nicht Teil.
- `bridge` (CloudNet-Extension) läuft in einem isolierten Classloader (siehe Notiz zu `TitanPermissionBridge`). **Entschieden:** Sie bekommt keine OTel-Abhängigkeit und keine eigenen Spans, wegen des isolierten Classloaders und der Gefahr von Klassenkonflikten mit dem Agent. Ihre Arbeit (Transfer, Spielerzahl, Rechte) wird auf der Titan-Seite umspannt: `Deliver` und die Zählerabfrage laufen über Klassen im Hauptclasspath (siehe `lobby-tracing-portal`). Die Rechteprüfung der Brücke ist im Modul `platform/luckperms` abgedeckt.

### D9 Metriken

Alle Zähler und Histogramme gehen über denselben `Telemetry.meter()`. Live ist `-Dotel.metrics.exporter=otlp` gesetzt, Mimir nimmt sie an. Benennung `<modul>.<größe>`, Einheiten per `setUnit`. Tests lesen sie mit `InMemoryMetricReader`.

### D10 Test-Infrastruktur (F.I.R.S.T.)

`core/testFixtures`: `TestTelemetry` baut je Aufruf einen eigenen `SdkTracerProvider` (`SimpleSpanProcessor`, `InMemorySpanExporter`) und `SdkMeterProvider` (`InMemoryMetricReader`), ohne `GlobalOpenTelemetry`, ohne `OpenTelemetryExtension.create()` (das registriert global). Hilfen: `spans()`, `span(name)`, `attribute(span, key)`, `counter(name, attributes)`. Kein `Thread.sleep`: `SimpleSpanProcessor` und der Reader sind synchron. Zeit über `Clock`. Minestom-Tests nutzen eine frische `Env` und `env.tick()`.

### D11 Lokal sichtbar machen

Ohne Agent: No-op. Zum Ansehen: Agent-Jar plus `JAVA_TOOL_OPTIONS="-javaagent:opentelemetry-javaagent.jar -Dotel.traces.exporter=logging -Dotel.metrics.exporter=logging -Dotel.logs.exporter=none -Dotel.service.name=Lobby-local"` für `./gradlew :apps:local:run`. Dazu ein kurzer Abschnitt in `docs/lobby-modules.md`, den dieser Change anlegt und die Modul-Changes um ihre Spans ergänzen.

## Risks / Trade-offs

- **API/Agent-Version läuft auseinander:** Die Brücke ist kompatibel; Renovate soll beide über die Bom heben.
- **Span-Menge:** Nur seltene Operationen sind Spans, Zähler sind billig. Sampling regelt der Agent (`otel.traces.sampler`).
- **Wachsende `Telemetry`-Signatur in Konstruktoren:** Eine Bean statt zweier. Module, die keine Telemetrie brauchen, nehmen sie nicht.
- **`onTraced`-Sperre ist eine Liste:** Neue hochfrequente Event-Typen müssen dort ergänzt werden; ein Test prüft die bekannten.
