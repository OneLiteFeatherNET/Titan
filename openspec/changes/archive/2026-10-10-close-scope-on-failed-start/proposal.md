# Proposal

## Why

`Titan.start` baut den `BeanScope` (alle `@PostConstruct` laufen) und ruft danach `VariantStartupCheck.verify`. Fehlt eine erwartete Column, wirft `verify` eine `IllegalStateException`, und der gerade gebaute Scope wird **nie geschlossen**:

```java
BeanScope scope = BeanScope.builder().profiles(profiles).build();
variant.ifPresent(descriptor -> VariantStartupCheck.verify(descriptor, loadedModules));   // wirft
return scope;                                                                            // nie erreicht
```

Alle `@PreDestroy` der schon gestarteten Beans bleiben aus: Datenbank-Pools (Hikari), der virtuelle Executor des `DatabaseWriter`, LuckPerms und die `FeatureNode`s (Listener bleiben am Event-Node). Ein Pool oder Executor mit Nicht-Daemon-Threads hält die JVM am Leben: Der Prozess meldet den Startfehler, endet aber nicht, und CloudNet sieht einen halb toten Service statt eines sauber gescheiterten Starts.

Dieselbe Lücke gibt es danach im Konstruktor von `Titan`: `beanScope.get(PermissionService.class)`, `PermissionStartupLog.activeService` und `FeatureStartupLog.startedInEventOrder` laufen nach dem Aufbau und können ebenfalls werfen, ohne dass der Scope geschlossen wird.

## What Changes

- Scheitert der Start nach dem erfolgreichen `build()`, schließt die Lobby den `BeanScope` und wirft die ursprüngliche Ausnahme unverändert weiter. Wirft `close()` dabei selbst, wird diese Ausnahme per `addSuppressed` an die ursprüngliche gehängt, nie umgekehrt.
- Das gilt für den Variantencheck in `Titan.start` **und** für die Schritte danach im Konstruktor (`PermissionService`, Startlogs).
- Die Logik ist eine kleine, ohne Minestom testbare Hilfsmethode in `runtime` (kein statischer Zustand).

Nicht Teil dieser Änderung: ein Fehler **während** `BeanScope.builder().build()` (Avaje rollt dort selbst nicht zurück; das ist ein eigener Befund, siehe Open Questions im Design), Änderungen am Shutdown-Pfad, Telemetrie.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-modules`: neue Anforderung, dass ein gescheiterter Start den Scope schließt (neben „Herunterfahren hinterlässt keine Reste“).

## Impact

- **Code:** `runtime` (`Titan`, neue Hilfsmethode). Keine neuen Abhängigkeiten.
- **Betrieb:** Bei einem Startfehler beendet sich der Service jetzt wirklich; die Fehlermeldung bleibt dieselbe. Sichtbar als Log-Zeilen der `@PreDestroy`-Methoden vor dem Stacktrace.
- **Spielerverhalten, Nutzertexte:** keine.

## Delivery

Pull-Request-Titel und Squash-Commit: `fix(runtime): close the bean scope when the startup check fails`
