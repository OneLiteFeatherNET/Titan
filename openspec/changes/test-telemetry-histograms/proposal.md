# Proposal

## Why

`TestTelemetry` (`core/testFixtures`, mit dem Fundament `lobby-tracing`, #362) liest Zähler (`counter`) und Gauges (`gauge`), aber keine Histogramme. Der Jump-and-Run-Tracing-Change (#363, **auf `origin/main`**, Commit `b2f7d28`) braucht ein Histogramm (`titan.jumprun.run.score`, je Modus). Sein Test `RunTelemetryTest.theScoreGoesIntoAHistogramPerMode` baut deshalb einen eigenen `SdkMeterProvider` mit `InMemoryMetricReader` und umgeht das Fixture:

```java
InMemoryMetricReader reader = InMemoryMetricReader.create();
try (SdkMeterProvider meters = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
    RunTelemetry own = new RunTelemetry(new Telemetry(testTelemetry.telemetry().tracer(), meters.get("test")));
    ...
    var points = reader.collectAllMetrics().stream().filter(...).flatMap(metric -> metric.getHistogramData().getPoints().stream()).toList();
```

Das ist doppelter Aufbau (DRY), mischt zwei Telemetry-Quellen in einem Test (Tracer aus dem Fixture, Meter aus dem Eigenbau) und wird jeder weitere Modul-Change wiederholen, der ein Histogramm einführt (portal-Dauer, Spielerzahl-Abfrage).

## What Changes

- `TestTelemetry` bekommt Lesehelfer für Histogramme, im Stil von `counter`: `histogramPoints(name)` (alle Punkte, je Attributsatz einer) und `histogram(name, attributes)` mit Anzahl und Summe für genau diesen Attributsatz (0/0, wenn nichts aufgezeichnet wurde).
- `RunTelemetryTest.theScoreGoesIntoAHistogramPerMode` wird auf das Fixture umgestellt; der Eigenbau (`SdkMeterProvider`, `InMemoryMetricReader`, handgebaute `Telemetry`) entfällt.
- Ein Test für die neuen Helfer in `core`.

Abhängigkeit: #363 ist gemergt (`origin/main`, `b2f7d28`), der Change hat also keine offene Abhängigkeit. Ein Eigenbau in einem noch nicht gemergten Branch müsste beim Rebase umgestellt werden.

Nicht Teil dieser Änderung: Änderungen an `Telemetry`, Produktionscode, neue Metriken, Änderung der Aussage des jumprun-Tests.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: Anforderung an die Testhilfe `TestTelemetry`, jetzt auch Histogramme zu lesen. (Die Capability liegt noch als Change `lobby-tracing` vor und ist erst nach dessen Archivierung in `openspec/specs/`; das Delta ist bis dahin ADDED.)

## Impact

- **Code:** `core/src/testFixtures` (`TestTelemetry`), `core/src/test` (ein Test), `features/jumprun/src/test` (`RunTelemetryTest`). Kein Produktionscode, keine neuen Abhängigkeiten (`opentelemetry-sdk-testing` ist schon `testFixturesApi`).
- **Betrieb, Spielerverhalten, Nutzertexte:** keine.

## Delivery

Pull-Request-Titel und Squash-Commit: `test(core): let TestTelemetry read histograms`

Ein Typ: Fixture und Migration des Tests sind beide `test`, kein Produktionscode.
