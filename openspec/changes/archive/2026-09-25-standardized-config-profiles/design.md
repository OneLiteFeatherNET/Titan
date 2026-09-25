# Design

## Context

Den Anlass beschreibt `proposal.md` (Why), die Anforderungen stehen in `specs/lobby-module-config/spec.md` und `specs/lobby-navigator/spec.md`. Diese Change baut auf `avaje-dependency-injection` auf (Avaje Inject und `PlatformBeans`).

Der Stand heute:
- **`common/.../config/ConfigStore`** hat mehrere Aufgaben:
  - `open(Path)` liest `app.json` und migriert v1 → v2 mit `.bak`.
  - `section(id, type, defaults)` führt den Abschnitt tief mit den Standardwerten zusammen, deserialisiert ihn per Gson in den Record, packt eine `ConfigException` aus dem Compact Constructor aus, meldet falsche Typen mit Feld und warnt bei unbekannten Schlüsseln.
  - `set`/`setSection`/`save` schreiben atomar zurück.
- **Nutzer:**
  - `ModuleContext.config(...)`, gebaut von `ModuleRegistry`/`Titan`,
  - fünf Module über `ctx.config(XConfig.class, XConfig.DEFAULTS)`,
  - der Setup-Server über `SetupConfigEditor`, `AppCommand` und `*SectionConfig`.
- **`NavigatorConfig(String title, List<Entry> entries)`**: Die Einträge sind eine Liste ohne Namen.
- **Feature-Flags** laufen getrennt über Togglz (`flags.properties`) und sind hier nicht betroffen.

## Goals / Non-Goals

**Goals:**
- Konfiguration aus Standardquellen: YAML, Profile, Env-Variablen und System-Properties, mit der Rangfolge aus der Spec.
- Die API für Module (`ctx.config(Typ, DEFAULTS)`) bleibt unverändert, Features merken vom Wechsel nichts.
- Der getestete Binding-Kern bleibt erhalten (Standardwerte, Validierung, Meldung falscher Typen, Warnung bei unbekannten Schlüsseln).

**Non-Goals:**
- Feature-Flags nach avaje-config holen (späterer Schritt).
- Config-Records per Konstruktor injizieren (Option B, späterer Schritt).
- Neuladen zur Laufzeit per `onChange`. Die Module lesen ihre Config in `enable()`. Neu laden würde bedeuten, sie neu zu starten, das ist eine eigene Change.
- Die Map-Daten des Setup-Servers (`map.json`) umstellen.

## Decisions

### 1. avaje-config als Quelle, als `Configuration`-Instanz statt statischem `Config`

**Spike-Ergebnis (Task 1.1, empirisch geprüft):**
- Aktuelle Version: **`io.avaje:avaje-config:5.2`**, veröffentlicht **2026-06-11** (Maven Central `maven-metadata.xml`, `last-modified`-Header des POM). Vorgänger `5.1`, `5.0`; keine Release Candidates aktiv.
- **Java 25:** Die Jar-Klassen sind auf Bytecode-Level 55 kompiliert (Java 11), tragen ein `module-info.class` (Modul `io.avaje.config`) und laufen im Spike anstandslos mit Temurin 25.0.3 (mehrere echte Prozessläufe, siehe unten). Keine Reflection, keine Agenten, keine Java-Versionsprobleme beobachtet.
- Abhängigkeiten laut POM: `org.jspecify:jspecify:1.0.0` (Pflicht), `io.avaje:avaje-applog:1.2` (Pflicht), `io.avaje:avaje-spi-service:2.17` (optional), **`org.yaml:snakeyaml:2.6` (optional!)** — dazu mehr in Entscheidung 4.
- **`Configuration`-Instanz ohne die statische `Config`-Fassade:**
  ```java
  Configuration config = Configuration.builder()
      .includeResourceLoading()   // application.yaml, Profile, CONFIG_FILE, Env, -D
      .build();
  ```
  Für Tests ohne Dateisystem: `Configuration.builder().putAll(map).build()` baut eine `Configuration` direkt aus einer `Map`, ganz ohne Ressourcen-Suche.
- **Lädt der Builder von sich aus etwas?** Nein. `Configuration.builder().build()` ohne weitere Aufrufe lädt **nichts** (leere Konfiguration, empirisch geprüft: `keys()` bleibt praktisch leer). Erst `.load(File)`/`.load(String resource)` (einzelne Datei/Resource) oder `.includeResourceLoading()` (die volle Standard-Pipeline) füllen sie. Die statische `Config`-Fassade (`Config.asConfiguration()`) ruft intern dieselbe Standard-Pipeline auf wie `.includeResourceLoading()` — der Unterschied ist nur globaler Zustand vs. eigene Instanz.
- **`includeResourceLoading()` lädt, empirisch bestätigt und im Quelltext (`InitialLoader`) nachvollzogen, in dieser Reihenfolge:** `application.{properties,yaml}` von Classpath und Arbeitsverzeichnis, dann je aktivem Profil `application-<profil>.{properties,yaml}` (Classpath, dann Datei), dann eine externe Datei (siehe unten), dann `-P`/`-p` Kommandozeilen-Argumente, dann (nur im Testscope) `application-test.*`.
- **Arbeitsverzeichnis:** Die Dateisuche nutzt das echte Prozess-Arbeitsverzeichnis (`new File(relativerPfad)`), **nicht** `System.getProperty("user.dir")` zur Laufzeit gesetzt — das wurde im Spike ausprobiert und ignoriert. Für Tests muss der Prozess tatsächlich im Zielverzeichnis laufen (`@TempDir` plus `ProcessBuilder`/Gradle-Test mit `workingDir(...)`), ein bloßes Umsetzen der System-Property reicht nicht.
- **Korrektur zu `PROPS_FILE`:** Der Name aus `proposal.md`/`design.md` ist **nicht korrekt**. Empirisch geprüft: Eine gesetzte Env-Variable `PROPS_FILE` hat keinerlei Wirkung. Der tatsächliche Mechanismus für eine externe Datei (laut `InitialLoader.configFile()` und empirisch bestätigt) ist:
  1. System-Property `props.file` (funktioniert noch, aber deprecated, avaje-config schreibt dazu eine Warnung auf `System.err`),
  2. System-Property `config.file`, sonst Env-Variable **`CONFIG_FILE`**.

  Für CloudNet-Templates bzw. eine Kubernetes-ConfigMap heißt die Env-Variable also `CONFIG_FILE`, nicht `PROPS_FILE`. `proposal.md` muss das bei Gelegenheit nachziehen; für diesen Spike-Bericht genügt die Korrektur hier.

**Entscheidung:** `io.avaje:avaje-config:5.2` in `common`. `PlatformBeans` stellt einen `io.avaje.config.Configuration`-Bean bereit, der aus avaje-config gebaut ist (Arbeitsverzeichnis, Profile über `AVAJE_PROFILES`/`avaje.profiles`, externe Datei über `CONFIG_FILE`/`config.file`, Env-Variablen und System-Properties). Plattform-Code nutzt diese Instanz, nie die statische `Config`-Fassade. So bleibt die Config testbar (in Tests wird eine `Configuration` aus einer Map gebaut) und frei von globalem Zustand (F.I.R.S.T.).

**Built-in first:** Die eingebaute Alternative wäre `java.util.Properties` plus eigene Logik für Profile und Overrides. Verworfen, weil das genau die Infrastruktur ist, die avaje-config fertig und getestet liefert (Profile, Rangfolge der Quellen, Abbildung auf Env-Variablen). SmallRye und Configurate sind in `proposal.md` begründet verworfen.

**Test:** Unit-Tests mit einer `Configuration` aus einer Map. Ein Integrationstest mit echten Dateien und Profilen in `@TempDir` sichert die Rangfolge aus der Spec ab.

**SOLID:** DIP, weil der Binder von der `Configuration`-Abstraktion abhängt und nicht von Dateien.

### 2. `ConfigSections`: Den Binding-Kern aus `ConfigStore` weiterverwenden

**Spike-Ergebnis (Task 1.2, empirisch mit einer echten `application.yaml` geprüft, Abschnitte `sit`, `tickle`, `spawn`, `navigator`):**
- **Verschachtelte Maps → Punkt-Schlüssel:** `sit: {offset: {x: 0.5, y: 0.25, z: 0.5}}` wird zu `sit.offset.x=0.5`, `sit.offset.y=0.25`, `sit.offset.z=0.5`.
- **Map von Objekten → Punkt-Schlüssel pro Eintrag:** `navigator.entries.survival: {slot: 3, destination: survival-lobby}` wird zu `navigator.entries.survival.slot=3`, `navigator.entries.survival.destination=survival-lobby` (ebenso für weitere Einträge wie `elytrarace`). Das trägt Entscheidung 3 (`Map<String, Entry>`) direkt: Ein Profil kann `navigator.entries.survival.slot` gezielt überschreiben.
- **Listen von Skalaren → EIN Schlüssel mit kommagetrennten Werten:** `sit.allowedBlocks: [stone, "dirt,special", grass]` wird zu **einem** flachen Schlüssel `sit.allowedBlocks = stone,dirt,special,grass` (mit SnakeYAML als Parser). Der eingebaute Fallback-Parser (ohne SnakeYAML) liefert stattdessen `sit.allowedBlocks = stone,"dirt,special",grass` (er lässt die Anführungszeichen aus der YAML-Quelle im String stehen). **Keine der beiden Varianten liefert eine sauber wieder auftrennbare Liste**, wenn ein Element selbst ein Komma enthält: Bei SnakeYAML ist das dreielementige Ergebnis von einer vierelementigen Liste `[stone, dirt, special, grass]` nicht mehr zu unterscheiden. Für `allowedBlocks` (Block-IDs wie `minecraft:stone`) ist das in der Praxis unkritisch, weil Block-IDs keine Kommas enthalten — der `SectionBinder` darf sich aber nicht darauf verlassen, dass ein einfaches `split(",")` allgemein verlustfrei ist. Bestätigt damit das in „Risks / Trade-offs" bereits benannte Risiko.
- **Listen von Objekten** (falls es sie gäbe) würden zu Index-Schlüsseln `path[0].feld`, `path[1].feld`, … — ein weiterer Beleg dafür, dass Entscheidung 3 (Map statt Liste für Navigator-Einträge) richtig liegt, weil Listen-Indizes sich nicht gezielt per Profil überschreiben lassen.
- **`forPath(id).keys()`** liefert die Schlüssel relativ zum Präfix, z. B. `forPath("sit").keys()` → `[allowedBlocks, offset.x, offset.y, offset.z]` (Unterebenen bleiben mit Punkt erhalten, nur das Präfix `sit.` fällt weg).
- **`asProperties()`** liefert dieselben flachen Schlüssel/Werte als `java.util.Properties`, ohne Herkunfts-Metadaten (Quelle, ob per Env überschrieben, etc. — die stecken nur in `Configuration.Entry`, nicht in `Properties`).

**Entscheidung:** Der neue `common/.../config/ConfigSections` bekommt eine `Configuration` und bietet `<R extends Record> R section(String id, Class<R> type, R defaults)`. Intern passiert Folgendes:
1. Die flachen Schlüssel unter `id.` werden aus `Configuration.forPath(id)` bzw. aus der Liste der Schlüssel gelesen.
2. Daraus wird ein `JsonObject`-Baum gebaut: `offset.x` → `{offset:{x:…}}`, und Listen werden anhand des Typs der Record-Komponente gespalten.
3. Dieser Baum geht **durch den bestehenden, getesteten Kern** aus `ConfigStore.section`: Zusammenführen mit den Standardwerten, Gson, Auspacken der `ConfigException`, Meldung falscher Typen, Warnung bei unbekannten Schlüsseln.

Der Kern wird dafür aus `ConfigStore` in eine package-private Klasse `SectionBinder` herausgezogen. `ConfigStore` selbst (Datei, Speichern, `set`) entfällt.

`ModuleContext.config(...)` delegiert an `ConfigSections`. Die API für Module bleibt gleich.

**Built-in first:** avaje-config hat kein Binding an Records (laut Faktenprüfung nur einzelne Getter und `forPath`). Das eigene Binding existiert schon und ist getestet, deshalb wird es weiterverwendet (DRY) und nicht neu geschrieben.

**Test:** Unit-Tests zuerst, mit einer `Configuration` aus einer Map:
- fehlender Abschnitt oder Einzelwert ergibt den Standardwert,
- verschachtelter Record (`offset`),
- Liste von Strings (`allowedBlocks`),
- Map von Records (Navigator-Einträge),
- falscher Typ aus einem Override meldet Modul und Feld,
- Warnung bei unbekannten Schlüsseln, einmal pro Abschnitt (per abgefangenem Appender).

Die bestehenden Tests für den Binding-Kern ziehen mit um.

**SOLID:** SRP. Die Quelle (avaje-config) und das Binding (`SectionBinder`) sind getrennt.

### 3. Navigator-Einträge als Map mit Namen

**Entscheidung:** `NavigatorConfig(String title, Map<String, Entry> entries)`. Der Schlüssel ist der Name des Ziels (`elytrarace`, `survival`, `slender`, `creative`). Damit kann ein Profil bzw. eine Env-Variable genau einen Eintrag ändern (`navigator.entries.survival.destination`), wie es die Spec verlangt. Die Reihenfolge ergibt sich aus `slot`, also zählt die Reihenfolge der Map nicht. Die Standardwerte bleiben inhaltlich gleich. Fehlermeldungen nennen weiter `navigator.entries`, bei Bedarf mit dem Namen.

**Built-in first:** YAML-Maps werden in avaje-config zu Schlüsseln mit Punkten. Eine Liste von Objekten ließe sich über flache Schlüssel nicht einzeln überschreiben.

**Test:** Unit-Test auf `NavigatorConfig` (Standardwerte, Validierung), und ein Integrationstest „Profil ändert ein einzelnes Ziel“.

**SOLID:** OCP. Neue Ziele kommen über die Config dazu.

### 4. Einmalige Umstellung von `app.json` beim Start

**Entscheidung:** `common/.../config/AppJsonMigration` läuft in der Bootstrap-Phase, **bevor** die `Configuration` gebaut wird:
- **`app.json` vorhanden, keine `application.yaml`:**
  1. Einlesen (v1 flach über die bestehende `LegacyConfigMigration`-Abbildung nach v2, v2 direkt).
  2. Verworfene Schlüssel im Log nennen (WARN).
  3. `application.yaml` schreiben.
  4. `app.json` in `app.json.migrated` umbenennen.
  5. Eine WARN-Zeile zur Umstellung ausgeben.
- **Navigator-Einträge:** In `app.json` ist `navigator.entries` eine Liste, das Ziel ist eine Map mit Namen (Entscheidung 3). Die Umstellung vergibt jedem Eintrag einen Namen aus seinem `displayName`: MiniMessage-Tags entfernen, Kleinbuchstaben, nur Buchstaben und Ziffern behalten (`<!i><green>Survival` → `survival`). Das ergibt für die Standardeinträge genau `elytrarace`, `survival`, `slender`, `creative`. Ist ein Name leer oder schon vergeben, wird `slot<N>` bzw. `-<slot>` angehängt, damit kein Eintrag verloren geht.
- **Beide vorhanden:** `app.json` bleibt unangetastet, eine WARN-Zeile meldet „wird ignoriert“.
- **Keine von beiden:** Es passiert nichts.
- **Syntaktisch kaputte `app.json`:** Der Start bricht mit Datei und Stelle des Fehlers ab, nichts wird umbenannt.

Die YAML-Ausgabe übernimmt ein kleines, getestetes Schreibwerkzeug für den JSON-Baum (Maps, Listen, Skalare, Strings in Anführungszeichen), gebaut auf SnakeYAMLs `Dump` (siehe Spike-Ergebnis unten).

**Spike-Ergebnis (Task 1.2, korrigiert gegenüber der ursprünglichen Annahme):** SnakeYAML liegt **nicht automatisch** im Klassenpfad. `org.yaml:snakeyaml` ist in der `avaje-config`-POM als `<optional>true</optional>` deklariert (Kommentar im POM: „If snakeyaml detected it will be used rather than built in simple parser"), Gradle zieht optionale Maven-Abhängigkeiten nicht transitiv. Ohne eigene Deklaration fällt avaje-config auf seinen eingebauten, ca. 400 Zeilen kleinen Fallback-Parser (`YamlLoaderSimple`) zurück (per `try { new YamlLoaderSnake() } catch (Throwable e) { … }`, empirisch reproduziert: Lauf ohne `snakeyaml-2.6.jar` im Klassenpfad lädt trotzdem fehlerfrei, aber mit dem Fallback-Parser). **Konsequenz:** `common/build.gradle.kts` muss `org.yaml:snakeyaml` explizit als eigene Laufzeit-Abhängigkeit deklarieren, in der von `avaje-config:5.2` gepinnten Version **2.6** (aus dessen POM-Property `snakeyaml.version`), sowohl für den robusteren YAML-Parser beim Lesen als auch für `Yaml#dump` beim einmaligen Schreiben in Entscheidung 4. Das ist eine zusätzliche, direkt deklarierte Abhängigkeit — kein Selbstläufer, wie ursprünglich angenommen.

**Built-in first:** Java bringt `java.util.Properties#store` mit, das würde aber `application.properties` statt YAML ergeben. Die Spec verlangt YAML, und Betreiber sollen ein einziges Format sehen. Die neue Abhängigkeit (SnakeYAML) wird bewusst in Kauf genommen, weil der Spike gezeigt hat, dass sie nicht „ohnehin da" ist, sondern von uns explizit gezogen werden muss — sie lohnt sich trotzdem, weil sie sowohl beim Lesen (robusterer, standardkonformer Parser statt des Fallbacks) als auch beim Schreiben (`Dump`) gebraucht wird.

**Test:** Unit-Tests mit `@TempDir`: v1 (echte alte `app.json` als Fixture), v2, beide Dateien vorhanden, keine Datei, kaputtes JSON. Außerdem ein Rundlauf: die geschriebene YAML durch `ConfigSections` lesen und dieselben Records erhalten.

**SOLID:** SRP. Die Umstellung ist von Laden und Binden getrennt und läuft genau einmal.

### 5. Setup-Server nur noch für Map-Daten

**Entscheidung:** `AppCommand`, `SetupConfigEditor` und `*SectionConfig` samt Tests werden entfernt, der `app`-Zweig von `SetupCommand` ebenso. Der Setup-Server liest nur noch `spawn.simulationDistance` für seinen `PlayerSpawnListener`, und zwar über dieselbe `Configuration`/`ConfigSections` (Standardwert 2). Er schreibt keine Config mehr.

**Built-in first:** Nicht anwendbar, Code wird entfernt.

**Test:** Die bestehenden Setup-Tests bleiben für die Map-Befehle erhalten. Ein Unit-Test prüft, dass die Simulationsdistanz aus der Config gelesen wird.

**SOLID:** SRP. Der Setup-Server ist nur für Map-Daten zuständig.

### 6. Schlüssel, Env-Variablen und Beispiel-Datei

**Spike-Ergebnis (Task 1.3, empirisch mit echten Dateien in `@TempDir`, echter Prozess-Env über `env VAR=… java …` und echten System-Properties über `-D…` geprüft):**

- **Rangfolge bestätigt, deckt sich exakt mit der Spec.** Reihenfolge niedrig → hoch, jede Stufe einzeln durchgespielt:
  1. `application.yaml`: `tickle.cooldownMillis=4000`.
  2. Profil `dev` aktiviert über `AVAJE_PROFILES=dev`, `application-dev.yaml` setzt `tickle.cooldownMillis=1000` und `navigator.entries.survival.slot=9` → beide Werte gelten (Profil schlägt Basis).
  3. Externe Datei über `CONFIG_FILE=../external.yaml` (siehe Entscheidung 1 zur Korrektur `PROPS_FILE` → `CONFIG_FILE`) setzt `spawn.simulationDistance=3` (Basis war `2`) → externe Datei schlägt Basis/Profil, wenn Letztere den Schlüssel nicht setzen.
  4. Env-Variable `TICKLE_COOLDOWNMILLIS=7777` schlägt den Profil-Wert `1000` → Ergebnis `7777`.
  5. Env-Variable `SPAWN_SIMULATIONDISTANCE=5` schlägt den Wert `3` aus der externen Datei → Ergebnis `5`.
  6. System-Property `-Dspawn.simulationDistance=9` gemeinsam mit Env-Variable `SPAWN_SIMULATIONDISTANCE=4` gesetzt → Ergebnis `9`, System-Property gewinnt.
  7. Ein Schlüssel ganz ohne Datei-Eintrag (`only.env.value`), nur über `ONLY_ENV_VALUE=fromEnv` gesetzt, wird trotzdem korrekt aufgelöst (`fromEnv`) — Env-Overrides wirken auch für Schlüssel, die in keiner Datei vorkommen (`CoreConfiguration` löst pro Zugriff verzögert auf: erst die Map, dann `DefaultValues.fallbackValue` = System-Property, dann Env-Variable, erst dann der vom Aufrufer übergebene Standardwert).
  8. Kein `application.yaml` vorhanden: `Configuration.builder().includeResourceLoading().build()` startet fehlerfrei mit leerer Konfiguration, und es wird **keine Datei angelegt** (Verzeichnisinhalt vor/nach dem Lauf identisch geprüft).

  **Ergebnis: Die tatsächliche Rangfolge von avaje-config entspricht bereits `Standardwert < application.yaml < Profil-Datei < externe Datei < Env-Variable < System-Property` — keine Abweichung von der Spec, also ist die in dieser Entscheidung ursprünglich vorgesehene Ausgleichsschicht in `PlatformBeans` NICHT nötig.** Mechanisch funktioniert das, weil `DefaultValues.overrideValue(key, value, source)` bei **jedem** `put()` erneut System-Property und Env-Variable prüft — unabhängig davon, aus welcher Datei gerade geladen wird —, sodass ein später geladener Dateiwert (z. B. aus einem Profil) durch einen passenden Override wieder verdrängt wird, sobald dessen Schlüssel erneut geschrieben wird.

- **Abbildung Schlüssel → Env-Variable, aus dem Quelltext (`DefaultValues.toEnvKey`) gelesen und empirisch bestätigt:** Großschreiben, `.` → `_`, `-` wird ganz entfernt (`key.replace('.', '_').replace("-", "").toUpperCase()`). `tickle.cooldownMillis` → `TICKLE_COOLDOWNMILLIS` (bestätigt), `spawn.simulationDistance` → `SPAWN_SIMULATIONDISTANCE` (bestätigt). Deckt sich mit der in `proposal.md`/`design.md` schon verwendeten Namenskonvention.

- **Profil-Aktivierung:** `AVAJE_PROFILES` (Env) wird von avaje-config selbst in die System-Property/den Konfigurationsschlüssel `avaje.profiles` übernommen (nur falls `avaje.profiles` nicht schon direkt gesetzt ist), empirisch bestätigt (`AVAJE_PROFILES=dev` aktiviert `application-dev.yaml`). Es gibt daneben `CONFIG_PROFILES`/`config.profiles` als zweiten, gleichwertigen Mechanismus, der Vorrang hätte, falls beide gesetzt sind — für Titan bleibt es bei `AVAJE_PROFILES`, wie in `proposal.md` festgelegt.

- **Kein injizierbarer Env-Provider.** `DefaultValues`/`InitialLoadContext` rufen `System.getenv(...)` direkt und statisch auf (im Quelltext geprüft), es gibt keine SPI/`ConfigurationSource`, die die Env-Auflösung abfangen ließe. **Env-Rangfolge kann deshalb in einem reinen Unit-Test nicht simuliert werden** — sie braucht einen echten Prozess mit echter Env (z. B. Gradle-`Test`-Task mit `environment(...)` auf einer geforkten Test-JVM, oder den E2E-Smoke-Test aus Task 5.1). System-Property-Rangfolge lässt sich dagegen in-process testen (`System.setProperty(...)` vor dem Bau der `Configuration`), weil System-Properties zur Laufzeit veränderbar sind.

**Entscheidung:** Die Schlüssel in YAML heißen wie die Felder der Records (`cooldownMillis`, `minHeight`), also genau wie heute in `app.json`. Das hält die Umstellung verlustfrei.

Die Abbildung auf Env-Variablen folgt avaje-config (Großbuchstaben, `.` wird zu `_`, `-` entfällt, z.B. `TICKLE_COOLDOWNMILLIS`), siehe Spike-Ergebnis oben. Die Rangfolge von avaje-config entspricht bereits der Spec, eine Ausgleichsschicht mit expliziten Overrides in `PlatformBeans` entfällt.

Mit der Distribution (`app/src/dist` bzw. README) wird eine kommentierte `application.example.yaml` mitgeliefert. Das Log listet beim Start die aktiven Profile auf INFO: `Active configuration profiles: {}`.

**Test:** Ein Integrationstest der Rangfolge mit `@TempDir`, Profil-Datei, gesetzter System-Property und einer Env-Variable — Letztere **nicht** über einen injizierten Provider (den gibt es laut Spike nicht), sondern über einen echten, mit gesetzter Env gestarteten Prozess bzw. über den E2E-Smoke-Test (Task 5.1). Die Log-Zeile wird per abgefangenem Appender geprüft.

**SOLID:** Nicht zutreffend (Konvention).

## Risks / Trade-offs

- **[Risiko, durch Spike ausgeräumt] Die Rangfolge von avaje-config weicht von der Spec ab.** → Empirisch widerlegt (Task 1.3, Entscheidung 6): Die Rangfolge entspricht exakt der Spec (`Standardwert < application.yaml < Profil < externe Datei < Env < System-Property`), auch für Schlüssel, die in keiner Datei vorkommen. Die ursprünglich vorgesehene Ausgleichsschicht in `PlatformBeans` entfällt.
- **[Risiko, durch Spike bestätigt] Listen und Maps gehen beim Abflachen verloren**, etwa bei Komma-Listen oder Strings mit Kommas. → Empirisch bestätigt (Task 1.2, Entscheidung 2): Eine Liste mit einem Element, das selbst ein Komma enthält, ist nach dem Abflachen nicht mehr von einer entsprechend längeren, kommafreien Liste zu unterscheiden. Für `allowedBlocks` (Block-IDs ohne Kommas) unkritisch; der `SectionBinder` darf sich aber nicht auf ein verlustfreies `split(",")` verlassen.
- **[Risiko, neu durch Spike] SnakeYAML ist keine transitive Selbstverständlichkeit.** → `org.yaml:snakeyaml` ist in der avaje-config-POM als `optional` deklariert, Gradle zieht es nicht automatisch. Ohne explizite Abhängigkeit läuft der eingebaute Fallback-Parser (`YamlLoaderSimple`), der YAML nur eingeschränkt versteht. Mitigation: `org.yaml:snakeyaml:2.6` (die von `avaje-config:5.2` gepinnte Version) wird in `common/build.gradle.kts` explizit deklariert.
- **[Risiko, korrigiert] `PROPS_FILE` existiert nicht.** → Empirisch widerlegt: Die Env-Variable für eine externe Datei heißt `CONFIG_FILE` (bzw. System-Property `config.file`, deprecated `props.file`). `proposal.md` und Betriebsdokumentation müssen den Namen `PROPS_FILE` durch `CONFIG_FILE` ersetzen.
- **[Risiko] Umbenannte Env-Variablen**, weil camelCase-Schlüssel wie `TICKLE_COOLDOWNMILLIS` schwer lesbar sind. → Das ist bewusst hingenommen, zugunsten verlustfreier Umstellung und gleicher Namen in Code und Datei. Die README enthält eine Tabelle aller Env-Namen.
- **[Risiko] Rollback nach der Umstellung:** Ein altes Jar findet nur `app.json.migrated`. → Die Migration-Hinweise sagen: vor dem Rollback `app.json.migrated` zurück nach `app.json` umbenennen. Die Umstellung ist verlustfrei und umkehrbar.
- **[Trade-off] Kein Neuladen zur Laufzeit.** Änderungen wirken erst nach einem Neustart, wie heute.
- **[Trade-off] Neue Abhängigkeiten** `avaje-config:5.2` und, explizit dazu deklariert, `org.yaml:snakeyaml:2.6` (siehe Risiko oben) — beide aus der Avaje- bzw. der von avaje-config selbst gepinnten Familie, keine Reflection, Java-25-tauglich.

## Migration Plan

1. Das neue Jar ausrollen. Beim ersten Start wird `app.json` zu `application.yaml` plus `app.json.migrated`, das Log meldet es.
2. CloudNet-Template bzw. Deployment umstellen: `application.yaml` (bzw. per `PROPS_FILE`) ausliefern und `AVAJE_PROFILES` setzen. Ist das erledigt, kann `app.json` im Template entfallen.
3. Den Setup-Server gemeinsam mit der Lobby ausrollen, weil die `/setup app`-Befehle wegfallen.
4. **Rollback:** Das alte Jar zurück und `app.json.migrated` → `app.json` umbenennen.

## Open Questions

Alle drei Spike-Fragen (Task 1.1–1.3) sind geklärt, der Ansatz ändert sich dadurch **nicht** — die Entscheidungen 1, 2, 4 und 6 enthalten die Details, hier nur die Kurzfassung:

- **Version/Java 25 (1.1):** `io.avaje:avaje-config:5.2` (2026-06-11), läuft empirisch anstandslos auf Java 25 (Temurin 25.0.3), Bytecode-Level 11, JPMS-Modul. `Configuration.builder()...build()` lädt ohne `.load(...)`/`.includeResourceLoading()` **nichts**; `.includeResourceLoading()` reproduziert die volle Standard-Pipeline der statischen `Config`-Fassade.
- **YAML (1.2):** avaje-config hat zwei Parser: einen SnakeYAML-Wrapper (SnakeYAML als **optionale** Abhängigkeit, muss selbst deklariert werden, Version `2.6`) und einen eingebauten Fallback-Parser, der automatisch greift, wenn SnakeYAML fehlt. Verschachtelte Maps und Maps von Objekten werden zu Punkt-Schlüsseln (`sit.offset.x`, `navigator.entries.survival.slot`), Listen von Skalaren zu einem einzigen kommagetrennten Schlüssel (`sit.allowedBlocks=stone,dirt,special,grass`) — mit dem bekannten Risiko bei Kommas in den Elementen selbst.
- **Rangfolge/Env (1.3):** Die tatsächliche Rangfolge deckt sich exakt mit der Spec, **keine Ausgleichsschicht nötig**. Env-Variablen-Namen folgen `key.replace('.', '_').replace("-", "").toUpperCase()`, z. B. `TICKLE_COOLDOWNMILLIS`. `AVAJE_PROFILES` aktiviert Profile wie geplant. Einzige Korrektur: Die externe Datei wird über `CONFIG_FILE` (Env) bzw. `config.file` (System-Property) eingebunden, **nicht** über `PROPS_FILE` — dieser Name aus `proposal.md` ist falsch und sollte dort nachgezogen werden. Ein injizierbarer Env-Provider für Tests existiert nicht; Env-Rangfolge lässt sich nur mit echter Prozess-Env testen (E2E-Smoke-Test, Task 5.1).
