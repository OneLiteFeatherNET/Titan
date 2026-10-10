# Design

## Context

`origin/main`, `runtime/src/main/java/net/onelitefeather/titan/runtime/Titan.java`:

- Der Konstruktor ruft `this.lifecycle.startup(() -> start(loader, profiles))`; `start` ist `private static` und baut `BeanScope.builder().profiles(profiles).build()`, dann `VariantStartupCheck.verify(...)`, dann `return scope`.
- `startup` ist `Telemetry.inSpan("titan.startup", ...)`: Wirft `start`, trägt der Span Ausnahme und ERROR und die Ausnahme geht weiter. Der Scope ist an dieser Stelle aber schon gebaut und nur in der lokalen Variable erreichbar.
- Der einzige Aufruf von `beanScope::close` hängt in `initialize()` am Shutdown-Task, und `initialize()` läuft nur nach einem erfolgreichen Konstruktor.
- `VariantStartupCheckTest` deckt `verify` selbst ab (wirft mit Namen der fehlenden Column). Dass der Scope danach geschlossen wird, ist ungetestet.

## Goals / Non-Goals

**Goals:**
- Nach jedem Fehler zwischen erfolgreichem `build()` und Ende des Konstruktors ist der Scope geschlossen (alle `@PreDestroy` gelaufen), und der Aufrufer sieht die ursprüngliche Ausnahme.
- Ein Test, der ohne Minestom-Server und ohne Warten läuft.

**Non-Goals:** Fehler im `build()` selbst, Änderung der Fehlermeldungen, Neustart-Logik.

## Decisions

### D1 Eine Hilfsmethode `closeOnFailure`, kein try/catch an jeder Stelle

```java
static <T> T closingOnFailure(BeanScope scope, Supplier<T> body)   // Name zur Wahl bei der Umsetzung
```

Sie führt `body` aus; bei jeder `RuntimeException`/`Error` ruft sie `scope.close()` in einem eigenen try/catch (`failure.addSuppressed(closeFailure)`) und wirft `failure` weiter. Sie lebt als paketprivate statische Methode in `runtime` (oder als kleine Klasse `ScopeGuard`), hat keinen Zustand und braucht keinen `BeanScope`-Mock: Ein Test baut sich einen kleinen echten Scope mit einer Bean, deren `@PreDestroy` ein Flag setzt (Avaje `BeanScope.builder().bean(...)`/Test-Modul, `runtime` hat schon `InjectExtension`-Testressourcen), oder eine Handvoll-Zeilen-Implementierung von `BeanScope`, die `close()` zählt. Welche der beiden Varianten, entscheidet die Umsetzung; Hauptsache kein Mockito-Framework nur dafür.

**Verworfen:** `try/finally` mit Flag im Konstruktor (verteilt die Logik auf zwei Stellen, `beanScope` ist `final`); `VariantStartupCheck.verify` vor `build()` aufrufen (geht nicht, der Check prüft die geladenen Module, nicht den Scope; und er soll nach den `@PostConstruct` laufen, damit auch deren Fehler zuerst auffallen).

### D2 Wo sie greift

1. In `start`: `closingOnFailure(scope, () -> { verify(...); return scope; })` statt des nackten `verify`-Aufrufs.
2. Im Konstruktor: der Block nach dem Aufbau (`PermissionService` holen, `setPlayerProvider`, `PermissionStartupLog`, `FeatureStartupLog`) läuft ebenfalls in `closingOnFailure`. `setPlayerProvider` ist ein statischer Minestom-Zustand; er wird bei einem Fehler nicht zurückgesetzt, weil die JVM nach dem Startfehler ohnehin endet (bewusst nicht Teil des Changes).

Der Span `titan.startup` endet, nachdem `start` zurückkehrt: Das Schließen im Fehlerfall passiert also **innerhalb** des Spans, und seine `feature.stopped`-Events (aus `FeatureNode.close`) landen am fehlgeschlagenen Startspan. Das ist erwünscht, siehe Risiko.

### D3 Reihenfolge der Ausnahmen

Die ursprüngliche Ausnahme gewinnt, weil sie die Ursache ist (`IllegalStateException` mit dem Namen der fehlenden Column; Tests und Betreiber suchen genau diese Meldung). Eine Ausnahme aus `close()` wird `suppressed` angehängt und taucht im Stacktrace trotzdem auf.

## Risks / Trade-offs

- `close()` kann bei einem halb gebauten Zustand selbst scheitern (z. B. ein `@PreDestroy`, das ein nicht initialisiertes Feld erwartet). Das fängt D3 ab; der Startfehler bleibt lesbar.
- Offene Frage: Wirft `BeanScope.builder().build()` selbst (ein `@PostConstruct` wirft), räumt Avaje die schon gebauten Beans nicht zwingend auf. Das ist ein getrennter Befund und wird hier nur geprüft (Task 1.4), nicht behoben.

## Migration Plan

Keine. Ein Fehlerpfad ändert sich; der Normalfall nicht.
