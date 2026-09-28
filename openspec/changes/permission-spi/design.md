# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/lobby-permissions`. Setzt den Modulgraphen aus `split-titan-into-columns` voraus (gemergt in #312: `core`, `common`, `runtime` im Paket `net.onelitefeather.titan.runtime`, `features/*`, `apps/*`, Convention `titan.app-variant` mit `expectedModules`, `variant.properties` `modules`, `VariantStartupCheck`). `TitanPlayer`, `CompatibilityUtil`, der LuckPerms-Start und der Brücken-Resolver liegen heute in `runtime` (`runtime/player/*`, `TitanApplication`), Butterfly in `runtime/Titan.java`.

Ist-Zustand im Code:

- `TitanApplication.main` startet LuckPerms mit `MinestomLoader.get().load().registerShutdownHook().start()` (`net.luckperms:minestom-loader:5.6-SNAPSHOT`, JarInJar) und setzt direkt danach den Resolver in `TitanPermissionBridge`.
- Der Loader bietet nur `load()`, `start()` und `registerShutdownHook()`. Letzteres registriert einen JVM-Shutdown-Hook, der `onDisable()` aufruft. Eine öffentliche Stop-Methode gibt es nicht. Das Datenverzeichnis ist fest `data/` (H2-Datenbank, `config.yml`, `contexts.json`).
- `TitanPlayer.value(permission)` fragt `LuckPermsProvider` mit `ContextManager.getQueryOptions(this)`, `CompatibilityUtil` übersetzt `Tristate` in Adventure-`TriState`. `Titan` setzt `setPlayerProvider(TitanPlayer::new)`, bevor der `BeanScope` gebaut wird.
- Butterfly wird in `Titan.initialize()` erzeugt und geladen und per Shutdown-Task beendet. Kein Titan-Code nutzt es darüber hinaus.
- Guava steht nur für LuckPerms und Butterfly auf dem Classpath. Der Testpfad schließt `minestom-loader` wegen seines gebündelten Gson aus.
- Die Erinnerung, LuckPerms laufe als Extension 6.0.1, stimmt nicht mehr mit dem Code überein. Maßgeblich ist der Loader-Stand oben. Eine lokal liegende `extensions/luckperms.jar` bzw. `butterfly.jar` ist ein Überbleibsel, das die Anforderung „genau einmal“ abfängt.
- `/stop` erlaubt der Konsole unabhängig von Rechten (`StopCommand`, künftig `features/admin`).

## Goals / Non-Goals

**Goals:**
- Ein Vertrag in `core`, zwei Implementierungen (Fallback in `runtime`, LuckPerms in `platform/luckperms`), Auswahl per Avaje (`@Secondary`).
- LuckPerms-Start im Bean-Lifecycle, vor dem ersten Spieler, ohne Änderung am Datenort.

**Non-Goals:**
- Umstieg auf die LuckPerms-Extension 6.x oder ein Update des Loaders.
- Rechteverwaltung in Titan selbst (Befehle zum Setzen von Rechten).
- Änderungen an `:bridge` außer, dass der Resolver eine andere Quelle hat.

## Decisions

### D1: Vertrag `PermissionService` in `core`

```java
public interface PermissionService {
    PermissionResult check(UUID playerId, String permission);
    String name(); // für das Start-Log, z. B. "luckperms", "deny-all"
}
public enum PermissionResult { ALLOWED, DENIED, NOT_SET }
```

Die Signatur verwendet nur JDK-Typen und ein eigenes Enum. So kann derselbe Dienst ohne Adventure den Resolver der CloudNet-Brücke speisen, und LuckPerms-Typen verlassen `platform/luckperms` nicht.
Built-in geprüft: Adventure `PermissionChecker`/`TriState` als Vertrag. Verworfen, weil der Dienst pro UUID fragt (auch für die Brücke ohne `Player`-Objekt) und Adventure-Typen den Vertrag an einen Spieler binden würden. Adventure bleibt dort, wo es hingehört: `TitanPlayer` übersetzt `PermissionResult` in `TriState`.
SOLID: DIP, ISP. Test: kein eigener; der Vertrag wird über die Implementierungen getestet.

### D2: Fallback `DenyAllPermissionService` in `runtime` als `@Secondary`

`@Singleton @Secondary` liefert immer `NOT_SET`. Ist `platform/luckperms` im Scope, gewinnt dessen `@Singleton`-Bean ohne weitere Konfiguration. Die Konsole ist davon nicht berührt, denn Befehle prüfen die Konsole schon heute gesondert.
Log: INFO beim Start `Permissions resolved by {}` mit `PermissionService.name()` aus `Titan` nach dem Aufbau des Scopes.
Built-in: Avaje `@Secondary`. Verworfen wurden `@RequiresBean`/`@RequiresProperty`: Sie wären eine zweite Stelle, die über die Plattform entscheidet; die Variante entscheidet bereits durch ihre Abhängigkeiten.
SOLID: OCP (eine neue Plattform ist ein neues Modul). Test: Unit (Fallback liefert `NOT_SET`), Integration (Starttest `apps/local`: ohne Schalter ist der aktive Dienst `deny-all`).

### D3: `TitanPlayer` fragt den injizierten Dienst

`TitanPlayer(PlayerConnection, GameProfile, PermissionService)` bildet `check(getUuid(), permission)` auf `TriState` ab (`ALLOWED`→`TRUE`, `DENIED`→`FALSE`, `NOT_SET`→`NOT_SET`). Bisher lieferte ein unbekannter LuckPerms-User `FALSE`; künftig liefert der Adapter dafür `NOT_SET`. `PermissionChecker.test` wertet beides als „nicht erteilt“. `Titan` setzt den Player-Provider erst nach dem Aufbau des Scopes mit dem `PermissionService` aus dem Scope; Spieler können sich ohnehin erst nach `bootstrap.start()` verbinden. `CompatibilityUtil` zieht nach `platform/luckperms` (`Tristate`→`PermissionResult`).
Built-in: Minestoms `setPlayerProvider` und Adventure-Pointer wie heute.
SOLID: DIP. Test: Integration mit Cyano-Env (Spieler mit Fake-Dienst: `ALLOWED`/`DENIED`/`NOT_SET` ergeben die erwarteten `hasPermission`-Ergebnisse).

### D4: `platform/luckperms`

- Neues Gradle-Modul (`java-library`, Avaje-Generator) mit `LuckPermsPermissionService implements PermissionService` als `@Singleton`. Es hängt an `core`, `luckperms.api` (compileOnly wie heute), `luckperms.minestom.loader` (runtimeOnly), Guava und Minestom. Den Gson-Ausschluss auf dem Testpfad übernimmt dieses Modul; die Ausschlüsse in `apps/cloudnet`/`apps/local` entfallen, wo sie nur für den Loader nötig waren.
- `@PostConstruct`: Zuerst prüft der Dienst, ob die Extension-Verwaltung von minestom-extensions eine Extension namens `LuckPerms` geladen hat. Wenn ja, bricht er mit `IllegalStateException("LuckPerms is loaded twice: remove the LuckPerms extension from extensions/")` ab. Danach folgt `MinestomLoader.get().load().registerShutdownHook().start()`. Weil `@PostConstruct` beim Aufbau des Scopes läuft, ist LuckPerms vor `bootstrap.start()` und damit vor dem ersten Spieler bereit.
- `check`: Der User kommt aus `getUserManager().getUser(uuid)`, bei `null` ist das Ergebnis `NOT_SET`. Ist der Spieler online (`ConnectionManager.getOnlinePlayerByUuid`), gelten `ContextManager.getQueryOptions(player)`, sonst `getStaticQueryOptions()`. Danach `checkPermission(permission)` über `CompatibilityUtil`.
- **Beenden:** Der Loader bietet nur den JVM-Shutdown-Hook, also beendet dieser LuckPerms nach dem Schließen des Scopes. Ein `@PreDestroy` gibt es bewusst nicht. Die Alternative, per Reflection auf das private `plugin`-Feld zuzugreifen und `onDisable()` aufzurufen, ist verworfen, weil sie an Interna des Loaders hängt. Die Spec-Anforderung „beim Herunterfahren beendet“ erfüllt der Hook, weil `System.exit`/`stopCleanly` die JVM beenden.
- Datenort `data/` bleibt, weil der Loader ihn festlegt. Titan ändert daran nichts.
- `@InjectModule(name = "luckpermsPlatform", provides = PermissionService.class)`; die Startprüfung der Variante erkennt es damit als Modul `luckpermsPlatform`.
- Querbezug `optional-extensions-bootstrap` D7: Ist jener Change schon gemergt, fragt die Prüfung auf doppeltes Laden `ServerBootstrap.loadedExtensions()` statt `ExtensionBootstrap.getExtensionManager()`, denn `local` hat dann keinen Extension-Loader.
Built-in: der vorhandene Loader samt LuckPerms-API und der ExtensionManager des minestom-extensions-Forks; kein eigenes Laden von Jars.
SOLID: SRP (nur Anbindung an LuckPerms). Test: Unit für `CompatibilityUtil` (alle drei `Tristate`-Werte). Die Prüfung auf eine doppelte Extension ist als reine Funktion über die Liste der geladenen Extension-Namen per Unit getestet. LuckPerms selbst läuft nur in einer manuellen Abnahme, weil der JarInJar-Loader einen echten Start mit `data/` braucht und im Testpfad wegen Gson ausgeschlossen ist.

### D5: Resolver der CloudNet-Brücke aus dem Dienst

In `runtime` erzeugt die reine Funktion `PermissionBridgeResolver.of(PermissionService)` ein `BiPredicate<UUID,String>`, das genau bei `ALLOWED` `true` liefert. Ein `@Singleton` setzt es in `@PostConstruct` mit `TitanPermissionBridge.setResolver(...)` und in `@PreDestroy` wieder auf `null`. Damit liefern Brücke und Lobby dasselbe Ergebnis. Ohne Plattform ist es der Fallback, und die Brücke meldet „nicht erteilt“.
Built-in: bestehender Holder `TitanPermissionBridge` (nötig wegen der Classloader-Grenze zur `:bridge`-Extension).
SOLID: SRP. Test: Unit für `PermissionBridgeResolver` mit einem Fake-Dienst; der Holder selbst wird nicht getestet, weil er statischen Zustand hat.

### D6: Varianten

- `settings.gradle.kts` bindet `platform/*` per Scan ein.
- `titanVariant { platform("luckperms") }` in `titan.app-variant` hängt `:platform:luckperms` an und ergänzt `luckpermsPlatform` in der vorhandenen Liste `expectedModules`, die in `variant.properties` (`modules`) landet. Fehlt das Modul beim Start, bricht die vorhandene Startprüfung (`VariantStartupCheck`) mit `luckpermsPlatform` in der Meldung ab.
- `apps/cloudnet` setzt `platform("luckperms")` fest.
- `apps/local` setzt `platform("luckperms")` nur, wenn die Gradle-Property `titan.luckperms` gesetzt ist.
- Der AOT-Cache von `apps/cloudnet` wird nach dem Change neu trainiert, weil sich der Classpath ändert.
Built-in: Gradle-Properties und die Convention aus dem Vorgänger-Change.
Test: Starttests beider Varianten (Integration). Der Starttest von `apps/cloudnet` prüft `luckpermsPlatform` in den erwarteten Modulen über `variant.properties`, ohne LuckPerms zu starten. Die Klasse mit der Prüfung auf fehlende Module ist schon im Vorgänger-Change per Unit getestet.

### D7: Butterfly entfernen

Abhängigkeit, Katalogeintrag `butterfly`, das Laden in `Titan.initialize()` und der Shutdown-Task entfallen. Guava zieht mit LuckPerms nach `platform/luckperms`. `runtime` prüft beim Bauen, dass Guava nicht mehr auf seinem eigenen Compile-Classpath gebraucht wird.
Test: `./gradlew build`; `grep -r butterfly` ohne Treffer außer im CHANGELOG.

## Risks / Trade-offs

- [LuckPerms-Daten gehen beim Deploy verloren] → Der Datenort `data/` bleibt unverändert. Der Deploy-Hinweis nennt, dass `data/` erhalten bleiben und `extensions/luckperms.jar` sowie `butterfly.jar` weg müssen.
- [In `cloudnet` fehlt LuckPerms, und alle Spieler sind ohne Rechte] → Prüfung der erwarteten Module beim Start (D6).
- [Ein alter `extensions/luckperms.jar` startet LuckPerms doppelt] → Abbruch mit klarer Meldung (D4).
- [Kein eigenes `@PreDestroy` für LuckPerms] → Der JVM-Hook beendet LuckPerms zuverlässig bei `System.exit`. Bleibt ein nicht-daemoner LuckPerms-Thread hängen, beendet `main` den Prozess wie heute mit `System.exit(1)`.
- [Zwei unkoordinierte Shutdown-Hooks: `PermissionBridgeConnector` räumt beim Schließen des Scopes auf, LuckPerms über den JVM-Hook des Loaders] → Ein Wettlauf endet in beiden Richtungen bei „nicht erteilt“, also sicher.
- [CloudNet-Abfragen nutzen jetzt Kontexte: online → `QueryOptions` des Spielers, sonst statische `QueryOptions`; vorher immer ohne Optionen] → Gewollt (Spec „CloudNet prüft Rechte wie die Lobby“), im PR ausdrücklich genannt.
- [Scope-Tests und AOT-Training in `apps/cloudnet` starten mit `platform/luckperms` echtes LuckPerms; der Loader bringt ein altes Gson auf den Test-Classpath] → Scope-Tests ersetzen `PermissionService` per Avaje-Mock; `titan.app-variant` schließt den Loader bei `platform("luckperms")` vom Test-Runtime-Classpath aus. Das AOT-Training startet LuckPerms wie bisher mit einem frischen `data/` im Trainingsverzeichnis.
- [Der Adapter ist nur manuell getestet] → Die Zuordnung (`CompatibilityUtil`) und die Prüfung auf doppeltes Laden sind per Unit getestet. Die manuelle Abnahme deckt Start, Rechte, Kontext und Brücke ab (Task 4.x).

## Migration Plan

1. Deploy: `titan-cloudnet.jar` plus neu trainierter AOT-Cache; aus dem CloudNet-Template `extensions/luckperms.jar` und `extensions/butterfly.jar` entfernen; `data/` bleibt.
2. Rollback: Revert des Squash-Commits und die beiden Extension-Jars wieder einlegen. Die LuckPerms-Daten sind in beiden Versionen dieselben.
