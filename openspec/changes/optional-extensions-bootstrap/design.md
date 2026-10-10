# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/server-bootstrap`. Voraussetzung: der Modulgraph aus `split-titan-into-columns` (`core`, `common`, `runtime`, `apps/*`, `variant.properties` mit Prüfung der erwarteten Module).

Ist-Zustand:

- `TitanApplication.main` ruft `ExtensionBootstrap.init()` bzw. `init(new Auth.Velocity(secret))` auf (das Secret kommt aus `forwarding.secret`, sonst aus `-Dminestom.velocity.secret`). Danach folgen `installExceptionHandler`, LuckPerms, `new Titan()` (Aufbau des `BeanScope`), `bootstrap.start(service.bind.host|localhost, service.bind.port|25565)`, Konsole und AOT-Training.
- `ExtensionBootstrap` (minestom-extensions 2.2.0) bietet `init()`, `init(Auth)`, `start(String,int)`, `shutdown()` und statisch `getExtensionManager()`.
- `PlatformBeans` erzeugt `InstanceContainer` und `Scheduler` aus dem schon initialisierten `MinecraftServer`. Die Reihenfolge „erst Server, dann Scope“ steht nur implizit in `main`.
- CloudNet-Wissen außerhalb von `main`: `DeliverProvider` in `common` wählt über `CloudNetEnvironment.isPresent()` (Verzeichnis `.wrapper`) zwischen `MessageChannelDeliver` und `DebugDeliver`. `MessageChannelDeliver` spricht über den JDK-Holder `TitanServerConnector`, den die `:bridge`-Extension befüllt.
- Die CloudNet-Minestom-Bridge ist selbst eine Minestom-Extension (`CloudNet_Bridge`). `:bridge` sieht `MinestomPermissionChecker` nur, weil der Extension-Classloader die Classloader seiner Abhängigkeiten durchsucht.

## Goals / Non-Goals

**Goals:**
- Die Reihenfolge Server-Init → Features → Start ist in `runtime` ausdrücklich festgelegt und getestet.
- `minestom-extensions` gibt es nur in `platform/extensions`.

**Non-Goals:**
- `:bridge` in ein DI-Modul umbauen (siehe D5).
- Neue Konfigurationsschlüssel für Host/Port oder Secret.

## Decisions

### D1: Vertrag `ServerBootstrap` in `core`

```java
public interface ServerBootstrap {
    String name();                         // "minestom", "extensions" – Start-Log und erwartete Module
    void init(@Nullable Auth auth);        // null = Standard-Auth
    void start(String host, int port);
    Set<String> loadedExtensions();        // leer ohne Extension-Loader
}
```

`loadedExtensions()` ersetzt Zugriffe auf `ExtensionBootstrap.getExtensionManager()` außerhalb von `platform/extensions`, etwa die Prüfung auf doppeltes LuckPerms aus `permission-spi` (D6).
Built-in: Minestoms `Auth` bleibt der Parametertyp, weil `core` ohnehin an Minestom hängt.
SOLID: DIP, ISP. Test: über die Implementierungen.

### D2: Auswahl per `ServiceLoader`, kein zweiter Avaje-Scope (Abweichung vom Proposal)

Das Proposal schlug einen Boot-Scope und einen Child-Scope mit `BeanScope.builder().parent(...)` vor. Verworfen, weil:
- Ein Avaje-Custom-Scope erzeugt je Scope-Annotation genau eine Modulklasse im Paket der Annotation. Liegen Boot-Beans in `runtime` und `platform/extensions`, erzeugen beide Compilations dieselbe Klasse, was im Shadow-Jar kollidiert.
- Der Server muss initialisiert sein, bevor irgendeine Avaje-Bean entsteht (`PlatformBeans`). Ein DI-Container nur für eine einzige Wahl ist mehr Mechanik als nötig.

Stattdessen gilt: `platform/extensions` meldet `ExtensionServerBootstrap` über `META-INF/services/…ServerBootstrap` an. `runtime` ruft die reine Funktion `ServerBootstraps.select(List<ServerBootstrap>)` auf. Bei keinem Kandidaten wird `PlainMinestomBootstrap` gewählt, bei genau einem dieser, bei mehreren bricht der Start mit `IllegalStateException` ab und nennt alle Namen. Die gewählte Instanz kommt mit `BeanScope.builder().bean(ServerBootstrap.class, chosen)` in den Scope, damit Columns und Plattformen sie injizieren können.
Ablauf in `runtime`: `select` → `init(auth)` → `installExceptionHandler` → `BeanScope` bauen → `start(host, port)` → Konsole/AOT. Diese Reihenfolge ist in einer Klasse `TitanStartup` mit injizierbaren Schritten gekapselt, damit sie ohne Server testbar ist.
Log: INFO `Server bootstrap: {}` mit `name()`. ERROR einmal in `main`, wenn der Start scheitert (wie heute).
Built-in: `java.util.ServiceLoader` (Shadow `mergeServiceFiles()` ist bereits aktiv) und Avaje `builder().bean(...)`.
SOLID: OCP (neue Bootstrap-Plattform = neues Modul), SRP. Test: Unit für `select` (0/1/2 Kandidaten). Unit für `TitanStartup` mit Fake-Schritten (aufgezeichnete Reihenfolge: init vor Scope, Scope vor start).

### D3: `PlainMinestomBootstrap` in `runtime`

`init` ruft `MinecraftServer.init()` bzw. `MinecraftServer.init(auth)` auf, `start` ruft `server.start(host, port)` auf, `loadedExtensions()` ist leer. Es wird nicht per ServiceLoader angemeldet, sondern nur als Rückfall aus D2 verwendet.
Built-in: Minestom-API direkt.
Test: Integration über den Starttest von `apps/local` (aktiver Bootstrap `minestom`).

### D4: `platform/extensions`

Das Modul hängt an `core` und `minestom-extensions`. `ExtensionServerBootstrap` delegiert an `ExtensionBootstrap.init(...)`/`start(...)`. `loadedExtensions()` liefert die Namen aus `ExtensionBootstrap.getExtensionManager()`. `runtime` hängt nicht mehr an `minestom-extensions`.
Built-in: minestom-extensions unverändert.
Test: Unit für die Namensliste über eine kleine Hilfsfunktion (Extensions → Namen). Integration: Starttest von `apps/cloudnet` prüft den aktiven Bootstrap `extensions` mit leerem `extensions/` im Temp-Arbeitsverzeichnis, wenn Cyano das mit `ExtensionBootstrap` zulässt. Sonst deckt es die manuelle Abnahme ab (Task 4.4).

### D5: `:bridge` bleibt eine Extension (kein Spike nötig)

Die CloudNet-Minestom-Bridge wird von CloudNet als Minestom-Extension in `extensions/` gelegt. `bridge-impl` (`MinestomPermissionChecker`, `PlayerManager`-Umschaltung) ist nur im Extension-Classloader sichtbar. Ein DI-Modul im Anwendungs-Classloader bekäme `NoClassDefFoundError`, wie bei der ersten Umsetzung von PR #162. Deshalb bleibt `:bridge` Extension. Die JDK-Holder `TitanPermissionBridge` und `TitanServerConnector` bleiben in `common`.

### D6: `platform/cloudnet` übernimmt die Wahl des Deliver

`platform/cloudnet` hängt an `core`, `common` und `platform/extensions` (Gradle-Abhängigkeit, denn ohne Extensions keine Bridge). Es enthält `MessageChannelDeliver` (aus `common` verschoben) und `CloudNetEnvironment` (aus `common` verschoben) und liefert einen `Deliver`-Bean, der wie heute bei vorhandenem `.wrapper` `MessageChannelDeliver` nimmt, sonst `DebugDeliver`. `runtime` liefert `DebugDeliver` als `@Secondary`-Fallback. `DeliverProvider` entfällt. So gilt das Szenario „Weiterleitung ohne Cloud“ aus `lobby-navigator` weiter, auch wenn das `cloudnet`-Jar ohne CloudNet läuft.

**Abweichung vom Proposal:** Host und Port bleiben in `runtime`. Die Spec verlangt, dass beide Varianten `service.bind.*` wie heute lesen, und in `platform/cloudnet` wäre die Bind-Adresse in `local` verloren. Das CloudNet-Wissen in `platform/cloudnet` besteht damit aus Deliver und `.wrapper`-Erkennung.
Built-in: Avaje `@Secondary`.
SOLID: SRP, DIP. Test: Die bestehenden Deliver-Tests ziehen mit. Neu: Unit für die Wahl (`.wrapper` vorhanden bzw. fehlend, im `@TempDir` statt im Arbeitsverzeichnis; dafür bekommt `CloudNetEnvironment` den Pfad injiziert).

### D7: Varianten und Querbezug zu `permission-spi`

- `apps/cloudnet`: `platform("extensions")`, `platform("cloudnet")`. `apps/local`: keins von beiden.
- Die DSL `platform("…")` und das Eintragen in `variant.properties` beschreibt `permission-spi` D6. Wer zuerst landet, führt sie ein.
- Die Prüfung auf erwartete Module zählt neben den Avaje-Modulen auch `ServerBootstrap.name()` als geladenes Modul. Fehlt `platform/extensions` in `cloudnet`, bricht der Start ab.
- Ist `permission-spi` schon gemergt, stellt dieser Change dessen Prüfung auf doppeltes LuckPerms von `ExtensionBootstrap.getExtensionManager()` auf `ServerBootstrap.loadedExtensions()` um. Sonst darf `platform/luckperms` in `local` (ohne Extension-Loader) nicht an minestom-extensions hängen. Landet `permission-spi` danach, übernimmt es diesen Weg.
Test: Starttests beider Varianten (Integration) prüfen den aktiven Bootstrap.

## Risks / Trade-offs

- [ServiceLoader findet `ExtensionServerBootstrap` im Shadow-Jar nicht] → Die Prüfung der erwarteten Module (D7) bricht `cloudnet` ab, statt still ohne Extensions zu starten.
- [Lokale Setups, die Extensions nutzten, gehen kaputt] → BREAKING-Hinweis. Wer lokal Extensions braucht, startet das `cloudnet`-Jar.
- [`TitanStartup` wird ein neues Stück eigener Infrastruktur] → Es kapselt nur die Reihenfolge, die heute in `main` steht. Ohne diese Klasse wäre die Reihenfolge nicht testbar.
- [Zwei Changes berühren die LuckPerms-Prüfung] → D7 legt fest, wer sie wann umstellt. Der Hauptkontext prüft das beim Mergen.

## Migration Plan

1. Deploy: `titan-cloudnet.jar` (AOT-Cache neu trainieren). `extensions/` im CloudNet-Template bleibt wie gehabt.
2. Lokal: `titan-local.jar` ignoriert `extensions/`, das Verzeichnis kann weg.
3. Rollback: Revert des Squash-Commits.
