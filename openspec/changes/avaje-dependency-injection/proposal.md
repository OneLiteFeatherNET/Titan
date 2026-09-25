# Proposal

## Why

Seit `lobby-feature-modules` ist jedes Lobby-Feature ein Modul. Neue Features und neue gemeinsame Dienste müssen aber weiterhin von Hand in `Titan.java` verdrahtet werden: Dienste erzeugen, Konstruktoren bedienen, das Modul in die Liste eintragen. Mit den anstehenden Features (Freunde über Ploceus, Stats, Voyager-Teaser) kommen eigene Clients und Dienste hinzu, die mehrere Module teilen. Jeder davon würde die Composition Root weiter aufblähen.

Ziel ist eine Arbeitsweise wie in Micronaut: Klasse annotieren, Abhängigkeiten im Konstruktor anfordern, fertig. Das soll die Wartbarkeit verbessern und neue Features schneller machen. Avaje Inject bietet genau das per Compile-Time-DI, ohne Reflection zur Laufzeit. Das passt zum AOT-Cache (JDK 25), mit dem Titan produktiv läuft.

**Auslieferung:** `feat(app): discover lobby modules with Avaje Inject` (Conventional Commit, PR-Titel). Das Verhalten für Spieler und Betreiber ändert sich nicht, daher gibt es kein `BREAKING CHANGE`.

## What Changes

- **Avaje Inject als DI-Container:** `io.avaje:avaje-inject` plus Annotation-Processor `avaje-inject-generator` in `app` (und in `common`, falls dort Beans liegen). Die Verdrahtung wird beim Kompilieren erzeugt.
- **Module werden automatisch gefunden:** Feature-Module sind `@Singleton`-Beans mit Konstruktor-Injektion. `Titan` holt nach dem Aufbau des Scopes alle Module per `BeanScope.listByPriority(LobbyModule.class)` und übergibt sie dem `ModuleRegistry`. Die zentrale Modulliste in `Titan.java` entfällt.
- **Feste Startreihenfolge:** Die Reihenfolge wird ausdrücklich über `@Priority` (`io.avaje.inject.Priority`) festgelegt und ist deterministisch, niedrige Werte zuerst (Spike).
- **Plattform-Dienste als Beans:** Eine `@Factory` stellt Minestom-Objekte und Plattform-Dienste bereit: `InstanceContainer`, `MapProvider`, `LobbySpawn`, `Deliver`, `ConfigStore`, den `titan`-EventNode, `ItemRegistry`, `NavigatorEntries`, `FeatureFlags`, `Clock`. Module fordern sie per Konstruktor an.
- **Schlanke Composition Root:** `Titan.java` baut nur noch den `BeanScope`, baut daraus den `ModuleRegistry` (sortierte Module plus Plattform-Beans) und schließt den Scope beim Herunterfahren.
- **Der Lebenszyklus bleibt beim `ModuleRegistry`:** `enable`/`disable`, der eigene EventNode pro Modul, die Aufräumregeln und `ModuleContext` bleiben unverändert. Avaje baut die Objekte, die Registry schaltet sie an und ab.
- **Config bleibt wie heute:** Module lesen ihren Abschnitt mit `ctx.config(...)` in `enable()`. Config per Konstruktor im Micronaut-Stil ist bewusst **nicht** Teil dieser Change (siehe Nicht-Ziele).
- **Tests:** Unit-Tests bauen Module weiter per `new …(…)`. Dazu kommen ein Test „der Container verdrahtet sich vollständig“ (alle Module gefunden, keine Bindung fehlt, Reihenfolge stimmt) und eine ArchUnit-Regel, dass Features keinen `BeanScope` direkt nutzen (kein Service Locator).
- **Spike zuerst:** Drei Punkte sind in der Recherche unsicher geblieben und werden vor dem Umbau geklärt:
  - aktuelle Version und Java-25-Unterstützung,
  - die Richtung von `@Priority`,
  - ob das Shadow-Plugin die `META-INF/services`-Dateien von Avaje und Togglz zusammenführt.

  Außerdem werden Startzeit und AOT-Cache vorher und nachher gemessen.

**Nicht-Ziele:**
- Die Config umstellen: Die Umstellung auf `application.yaml` mit Profilen per avaje-config folgt in der eigenen Change `standardized-config-profiles`, die auf dieser aufbaut. Config-Records per Konstruktor zu injizieren (Option B) ist auch dort ein späterer Schritt.
- Den Setup-Server auf DI umstellen.
- Feature-Code ändern, abgesehen von Annotationen und Konstruktoren.

## Capabilities

### New Capabilities
<!-- keine -->

### Modified Capabilities
- `lobby-modules`: Ein neues Feature braucht keinen Eintrag mehr in einer zentralen Modulliste, ein neues Paket genügt. Die Startreihenfolge folgt einer ausdrücklich deklarierten Priorität statt der Position in der Liste. Dazu kommt, dass fehlende Abhängigkeiten eines Moduls beim Build bzw. Start auffallen und nicht erst im Betrieb.

## Impact

- **Code:**
  - `app`: `Titan.java` wird zur Bootstrap-Klasse für den `BeanScope`, dazu kommt eine neue `@Factory` für die Plattform-Beans.
  - `ModuleRegistry`: unverändert, `Titan` baut ihn über den bestehenden Builder mit der Liste aus `listByPriority`. Der Builder bleibt für Tests und den `ModuleHarness`.
  - Alle 7 Feature-Module bekommen `@Singleton` und `@Priority` und einen `@Inject`-Konstruktor.
- **Neue Abhängigkeiten:**
  - Laufzeit: `io.avaje:avaje-inject:12.7` (Spike).
  - Build: `io.avaje:avaje-inject-generator:12.7` als `annotationProcessor`.
  - Test: `io.avaje:avaje-inject-test:12.7`, nur falls gebraucht.
  - `jakarta.annotation-api` ist nicht nötig, `@Priority` kommt aus `io.avaje.inject`.
- **Build:** Das Shadow-Plugin muss die `META-INF/services`-Dateien zusammenführen (`mergeServiceFiles()`), damit Avaje und Togglz beide im Jar landen.
- **Laufzeit:** Es gibt keine Reflection, der AOT-Cache bleibt nutzbar. Die Startzeit wird im Spike gemessen.
- **Texte für Nutzer:** keine Änderung.
- **Doku:** `docs/lobby-modules.md` erklärt, wie ein neues Modul entsteht (Annotation statt Listeneintrag, Dienste per Konstruktor).
