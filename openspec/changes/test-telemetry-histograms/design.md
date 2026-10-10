# Design

## Context

`origin/main`:

- `TestTelemetry` hält je Instanz `InMemorySpanExporter`, `InMemoryMetricReader`, `SdkTracerProvider`, `SdkMeterProvider` und die `Telemetry`. `counter(name, attributes)` summiert `getLongSumData().getPoints()` mit gleichen Attributen; `gauge(name)` summiert `getLongGaugeData().getPoints()`.
- `titan.jumprun.run.score` ist `histogramBuilder(...).ofLongs()`; Long- und Double-Histogramme liefern beide `getHistogramData()` (`HistogramPointData` mit `getCount()`, `getSum()`, `getAttributes()`).
- `RunTelemetryTest` nutzt sonst durchgehend `testTelemetry` (Spans, Zähler); nur dieser eine Test baut sich einen eigenen Provider, weil das Fixture kein Histogramm las.
- #363 ist in `origin/main` gemergt: der Eigenbau ist bereits dort und wird umgestellt.

## Goals / Non-Goals

**Goals:** Ein Test liest ein Histogramm genauso wie einen Zähler, ohne eigenen Provider; der jumprun-Test sagt dasselbe aus wie vorher.

**Non-Goals:** Buckets/Grenzen prüfen (kein Test braucht sie), Exemplars, Double-Summen als Zähler.

## Decisions

### D1 Zwei Helfer, kein Datentyp-Zoo

```java
/** Every recorded point of a histogram, one per attribute set. */
public List<HistogramPointData> histogramPoints(String name)

/** Count and sum recorded for exactly these attributes; 0 and 0.0 if nothing was recorded. */
public HistogramReading histogram(String name, Attributes attributes)   // record HistogramReading(long count, double sum)
```

`histogramPoints` ist der rohe Zugriff (der jumprun-Test braucht ihn, um zu prüfen, dass es genau einen Punkt je Modus gibt und dass `user.id` fehlt). `histogram` ist der bequeme Fall, spiegelt `counter` (gleiche Attribute-Gleichheit, gleiche Null-Semantik) und summiert über Punkte mit gleichen Attributen. `HistogramReading` ist ein verschachtelter Record in `TestTelemetry`; das SDK-Typ-Leck von `histogramPoints` ist akzeptabel, weil `spans()` bereits `SpanData` zurückgibt.

**Verworfen:** nur ein `List<HistogramPointData>` (jeder Test würde filtern und summieren, DRY); `min`/`max`/Buckets (YAGNI); Umbau auf eine allgemeine `metric(name)`-API (größerer Umbau als nötig).

### D2 Lesen sammelt, ohne zu verändern

`InMemoryMetricReader.collectAllMetrics()` ist kumulativ (Temporalität des Readers ist kumulativ); mehrfaches Lesen im selben Test liefert wachsende Summen statt Deltas, wie bei `counter`. Der Test in 1.x belegt das (zweimal aufzeichnen, zweimal lesen).

### D3 Migration des jumprun-Tests

`theScoreGoesIntoAHistogramPerMode` benutzt `testTelemetry` und `histogramPoints("titan.jumprun.run.score")`; die Importe `SdkMeterProvider`, `InMemoryMetricReader` und `Telemetry` (nur dafür) entfallen, falls sonst ungenutzt (`Telemetry.USER_ID` bleibt in Gebrauch). Die drei Aussagen bleiben (ein Punkt je Modus, `mode=MEDIUM`, Summe 12, kein `user.id`), plus `histogram(...)` für Anzahl 1.

## Risks / Trade-offs

- Beim Rebase: Landen vor diesem Change weitere Tests mit eigenem Reader (andere Modul-Changes), kommen sie bei dessen Review dazu. Zum Stichtag ist `RunTelemetryTest` der einzige Fund (`git grep InMemoryMetricReader origin/main`).
