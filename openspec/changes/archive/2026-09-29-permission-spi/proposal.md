# Proposal

## Why

Titan hängt fest an LuckPerms: `TitanApplication` startet es über den `MinestomLoader`, `TitanPlayer` fragt `LuckPermsProvider` direkt, und `app` bündelt dafür Guava sowie einen Gson-Konflikt, der aus dem Testpfad ausgeschlossen werden muss. Butterfly wird nur geladen und beendet, Titan nutzt nichts davon; Butterfly verlangt aber selbst LuckPerms. Ohne LuckPerms lässt sich Titan heute nicht starten, auch lokal nicht. Setzt dieser Change auf den Columns aus `split-titan-into-columns` auf, wird das Permission-System zu einem austauschbaren Vertrag, den eine Plattform-Schicht per DI erfüllt.

## What Changes

- **`core`**: Neuer Vertrag `PermissionService` (Spieler-UUID plus Permission-Knoten, Ergebnis mit „gesetzt/nicht gesetzt/erlaubt“; nur JDK-Typen, keine Adventure-Typen über die Classloader-Grenze).
- **`runtime`**: `@Secondary`-Fallback `DenyAllPermissionService`: Die Konsole darf alles, Spieler nichts. Er greift nur, wenn keine Plattform einen `PermissionService` liefert.
- **`TitanPlayer`** beantwortet `PermissionChecker` über den injizierten `PermissionService` statt über `LuckPermsProvider`.
- **`platform/luckperms`**: Neues Modul mit `LuckPermsPermissionService`. Er startet LuckPerms im Bean-Lifecycle (`@PostConstruct`) und beendet es über `@PreDestroy` beim Schließen des Scopes. Die Kontexte (`QueryOptions`) ermittelt er über den Online-Spieler.
- **CloudNet-Brücke**: `TitanPermissionBridge` bleibt als JDK-only-Holder für die `:bridge`-Extension. `runtime` setzt den Resolver aus dem `PermissionService`-Bean statt aus LuckPerms.
- **Varianten**: `apps/cloudnet` bindet `platform/luckperms` immer ein. `apps/local` bindet es nur mit `-Ptitan.luckperms` ein; ohne den Schalter gilt der Fallback.
- **BREAKING**: Butterfly wird ersatzlos entfernt (Abhängigkeit, Laden in `Titan.initialize()`, Shutdown-Task).
- **BREAKING**: LuckPerms wird nicht mehr aus `main()` und nicht mehr als `extensions/luckperms.jar` geladen, sondern nur über `platform/luckperms`. Ein `luckperms.jar` im `extensions/`-Ordner ist zu entfernen, damit LuckPerms nicht doppelt startet.
- **BREAKING**: Ohne `platform/luckperms` haben Spieler keine Rechte (z. B. `titan.command.stop`).

## Capabilities

### New Capabilities

- `lobby-permissions`: Permission-Abfragen für Spieler und Konsole laufen über einen austauschbaren Dienst. Ohne Permission-Plattform sind Spieler ohne Rechte und die Konsole mit allen Rechten. Mit LuckPerms gelten die LuckPerms-Rechte samt Kontext. CloudNet-Permission-Abfragen liefern dasselbe Ergebnis wie Abfragen in Titan.

### Modified Capabilities

_Keine._

## Impact

- **Code**: `TitanApplication` (LuckPerms-Start und Resolver entfallen), `TitanPlayer`, `CompatibilityUtil`, `Titan` (Butterfly entfällt), neues `platform/luckperms`, `core` (`PermissionService`), `runtime` (Fallback, Brücken-Resolver).
- **Abhängigkeiten (entfernt aus `runtime`/`apps/local`)**: `luckperms.api`, `luckperms.minestom.loader`, `butterfly-minestom`, dazu das explizite Guava, falls nur LuckPerms es braucht. Der Gson-Exclude im Testpfad zieht nach `platform/luckperms` um. Der Katalogeintrag `butterfly` entfällt.
- **Tests**: Unit-Tests für den Fallback und für `TitanPlayer` mit einem Fake-`PermissionService`. Der LuckPerms-Adapter bekommt einen Integrationstest, soweit LuckPerms ohne Server startbar ist, sonst eine manuelle Abnahme.
- **Nutzertexte**: keine.
- **Betrieb (BREAKING)**: `extensions/luckperms.jar` und `extensions/butterfly.jar` entfernen. Die LuckPerms-Daten (Speicherort, Konfiguration) müssen am selben Ort bleiben; das klärt design.md.
- **Voraussetzung**: `split-titan-into-columns`.

## Delivery

PR-Titel: `refactor(permission)!: resolve permissions through an optional di module`

`BREAKING CHANGE: Butterfly is removed and LuckPerms is no longer loaded from main() or the extensions folder but only through platform/luckperms; without it players have no permissions.`
