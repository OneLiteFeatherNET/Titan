# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/lobby-daytime`.

Ist-Zustand:

- `MapProvider` (`common`) setzt die Lobby-Instanz auf `setTime(6000)` und stoppt die Uhr mit `defaultClock().rate(0f)`. Die Uhr kann `null` sein.
- `PlatformBeans` stellt `Clock` (`Clock.systemUTC()`), `InstanceContainer` (auch als `Instance` auffindbar) und `Scheduler` als Beans bereit. Die Zeitzone steckt daher nicht in der `Clock`.
- Ein Column ist ein Verzeichnis unter `features/` mit `titan.column`; `settings.gradle.kts` erfasst es automatisch. Vorlage ist `features/tickle` (`@Singleton` mit `@PostConstruct`/`@PreDestroy`, Standardwerte in `titan/defaults/<id>.yaml`, Werte über die `Config`-Fassade).
- `lobby-module-config` verlangt: ungültige Werte brechen den Start ab, kein stilles Ersetzen.

## Goals / Non-Goals

**Goals:**
- Lineare Echtzeit-Abbildung, abschaltbar, ohne Neustart konfigurierbar.
- Reine Rechenlogik ohne Server testbar.

**Non-Goals:**
- Sonnenstand, Jahreszeiten, Strategie-Schnittstelle, eigene Konfigurationsklassen-Hierarchie (siehe proposal.md, Nicht-Ziele).
- Änderungen an `MapProvider` oder `runtime`.

## Decisions

### D1: Reine Abbildung `DayTimeMapping`

Finale Klasse mit `long ticksAt(Instant, ZoneId)`: `floorMod(secondOfDay - 6h, 86400) * 24000 / 86400`. Ergebnis im Bereich [0, 24000). 06:00 → 0, 12:00 → 6000, 18:00 → 12000, 00:00 → 18000. `secondOfDay` kommt aus `instant.atZone(zone).toLocalTime()`, also folgt die Abbildung der Wanduhr inklusive Sommerzeit: Beim Vorstellen springt sie vorwärts, beim Zurückstellen wiederholt sie die Werte der doppelten Stunde. Sonst gibt es keinen Rückwärtssprung, und ein Neustart verschiebt nichts, weil nur `Instant` und Zone eingehen.
Built-in: `java.time` (`ZoneId`, `ZonedDateTime`), keine eigene Zeitzonen- oder Kalenderlogik.
Verworfen: eine Strategie-Schnittstelle. Es gibt genau eine Abbildung; eine Schnittstelle wäre totes Gerüst, bis ein zweiter Fall existiert.
SOLID: SRP. Test: Unit (`DayTimeMappingTest`): Ankerwerte, beide Berliner Umstellungen, Sonnenwenden, Bereich, Determinismus, kein Rückwärtssprung über das Vorstellen, genau ein Umlauf je 24 h.

### D2: `DaytimeModule` als `@Singleton`

Konstruktor: `Instance` (nicht `InstanceContainer`, `runtime` stellt nur `Instance` bereit), `Scheduler`, `Clock` (alles Plattform-Beans, siehe Kontext), `DayTimeMapping` wird direkt erzeugt. `@PostConstruct`: `defaultClock()` holen; bei `null` WARN loggen und ohne `rate(0f)` weitermachen, sonst `rate(0f)`. Danach eine wiederkehrende Aufgabe alle 20 Ticks über `Scheduler.buildTask(...).repeat(TaskSchedule.tick(20))` einplanen, außerdem sofort einmal ausführen, damit die Zeit ab dem Start stimmt. Jeder Lauf: `enabled ? mapping.ticksAt(clock.instant(), zone) : 6000` → `lobby.setTime(...)`. `@PreDestroy`: Aufgabe abbrechen (`Task.cancel()`).
Ein Takt von 20 Ticks erfüllt „höchstens einmal pro Sekunde“ ohne Sperre gegen häufigere Aufrufe: Die Methode ist nicht öffentlich aufrufbar, anders als in PR #217.
Built-in: Minestoms `Scheduler`. Verworfen: `java.util.concurrent`-Timer, weil der Scheduler auf dem Server-Takt läuft und beim Herunterfahren mit dem Server endet.
SOLID: SRP, DIP (Plattform-Beans über Konstruktor). Test: Integration (`DaytimeModuleTest`).

### D3: Konfiguration `daytime.*`, jeden Lauf neu gelesen

`titan/defaults/daytime.yaml` mit `daytime.enabled: true` und `daytime.zone: Europe/Berlin`. Beide Werte liest der Lauf jedes Mal über die `Config`-Fassade (`getBool`, `get`), sodass Live-Reload ohne Neustart wirkt, wie bei `tickle`. Die `Clock`-Bean ist UTC, deshalb braucht es die Zeitzone als eigenen Schlüssel.
Ungültige Zone: Beim Start wird sie einmal geparst, ein Fehler bricht den Start ab (Regel aus `lobby-module-config`, kein stilles Ersetzen). Zur Laufzeit hält eine ungültige Änderung die Lobby nicht an: Sie behält die zuletzt gültige Zone und loggt WARN mit dem Schlüssel `daytime.zone`. Ein WARN pro Lauf wäre Log-Spam, daher wird nur beim Wechsel des ungültigen Werts geloggt.
Verworfen: Rückfall auf die Mittags-Starre oder Berlin bei ungültigem Wert. Das würde die Konfiguration stillschweigend ersetzen und die Betreiberin nicht am Start scheitern lassen, wo sie es sofort sieht.
Built-in: `avaje-config` (schon im Stack), `ZoneId.of`. Keine eigenen Config-Klassen wie in PR #217.
SOLID: SRP. Test: Integration (Zonenwechsel, ungültige Zone beim Start und zur Laufzeit, WARN über einen aufgefangenen Appender).

### D4: `MapProvider` bleibt, keine Startreihenfolge

Der Mittags-Stand aus `MapProvider` ist der Rückfall für Varianten ohne dieses Modul. Das Modul überschreibt ihn innerhalb einer Sekunde, egal in welcher Reihenfolge die Beans entstehen, denn `setTime` und `rate(0f)` sind idempotent. Deshalb braucht es weder `@Order`-artige Abhängigkeiten noch eine Änderung an `MapProvider`.
Test: Integration (Zeit stimmt nach dem ersten Lauf, auch wenn zuvor `setTime(6000)` gesetzt war).

### D5: Wartung und Tests ohne Warten

`DaytimeModuleTest` nutzt `MicrotusExtension`/`Env`, eine einstellbare `Clock` und `env.tick()`. Der Test schaltet `daytime.*` über `Config.setProperty` und liest die Ticks der Instanz nach dem Tick, der den Lauf auslöst. `ColumnArchitectureTest` folgt der Vorlage aus `tickle`. Keine Sleeps, keine Systemzeit.

## Risks / Trade-offs

- [Kein Sonnenaufgang wie in der Realität, nur linear] → Bewusst; eine Sonnenstand-Abbildung wäre ein eigener `feat`-Change.
- [Zwei Schreiber auf die Instanzzeit: `MapProvider` und Modul] → Nur beim Start; das Modul gewinnt beim ersten Lauf (D4).
- [Zeitumstellung erzeugt einen sichtbaren Sprung um eine Stunde] → Gewollt wie die Wanduhr, festgelegt in der Spec.
- [Live-Reload hängt an `avaje-config`] → Gleicher Weg wie `tickle`; ohne Reload gilt der Wert vom Start.

## Migration Plan

1. Deploy: Nichts zu konfigurieren; Standard ist an, Zone Berlin. AOT-Cache der Lobby neu trainieren.
2. Abschalten: `daytime.enabled: false`, wirkt ohne Neustart.
3. Rollback: Revert des Squash-Commits; `MapProvider` friert die Lobby wieder auf Mittag.
