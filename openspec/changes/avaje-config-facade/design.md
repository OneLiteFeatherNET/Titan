# Design

## Context

Zum Anlass siehe `proposal.md`, die Anforderungen stehen in `specs/lobby-module-config/spec.md`. Diese Change setzt auf den Stand nach `standardized-config-profiles` auf (gemergt und archiviert).

Der Stand danach:
- **Laden:** `ConfigurationLoader` führt `AppJsonMigration` aus (einmalige Umstellung von `app.json`, dazu im Setup-Server `LegacyAppJsonWarning`) und baut dann über `ConfigurationFactory.load()` eine eigene `Configuration`-Instanz (`Configuration.builder().includeResourceLoading().build()`). Ladefehler werden in `ConfigException.malformed(datei, detail)` übersetzt.
- **Durchreichen:** `Titan` gibt die Instanz als `@External`-Bean an Avaje Inject. `ConfigurationPropertyPlugin` sorgt dafür, dass Inject nicht über die statische `Config`-Fassade eine zweite Instanz baut. `PlatformBeans` erzeugt daraus `ConfigSections`, das über `ModuleRegistry` und `ModulePlatform` bei `ModuleContext.config(Typ, DEFAULTS)` ankommt.
- **Binden:** `ConfigSections` und `SectionBinder` (rund 570 Zeilen) bauen aus flachen Schlüsseln einen Gson-Baum, führen ihn mit `DEFAULTS` zusammen, deserialisieren ihn in Records (`RecordFields`, `KeyGsonAdapter`), melden falsche Typen und warnen bei unbekannten Schlüsseln.
- **Records:** Fünf Config-Records (`sit`, `spawn`, `tickle`, `elytra`, `navigator`) tragen Standardwerte und prüfen im Compact Constructor mit `ConfigException.invalid(feld, grund)`.
- **Setup-Server:** `SetupSpawnConfig` liest `spawn.simulationDistance` über `ConfigSections`.
- **Doku für Betreiber:** Die kommentierte `app/src/dist/application.example.yaml` wiederholt die Standardwerte der Records.

**Faktenprüfung avaje-config 5.2** (Quelltext gelesen):
- `io.avaje.config.Config` baut seine `Configuration` **im statischen Initialisierer** (`Config.data = CoreConfiguration.initialise()`), also beim ersten Zugriff auf die Klasse. Die Pipeline ist dieselbe wie bei `includeResourceLoading()`: `application.{properties,yaml}` aus Classpath, dann aus dem Arbeitsverzeichnis, Profile, `CONFIG_FILE`, Env und System-Properties.
- Die Instanz hinter der Fassade lässt sich **nicht austauschen**, es gibt kein `setConfiguration`. `Config.asConfiguration()` liefert genau diese Instanz.
- Ein Fehler beim Laden (kaputte YAML) wirft `IllegalStateException("Error loading properties - <datei>", cause)`. Im statischen Initialisierer kommt sie als `ExceptionInInitializerError` an, danach ist die Klasse unbrauchbar (`NoClassDefFoundError`).
- `Config.getInt/getLong` auf einen nicht-numerischen Wert werfen eine `NumberFormatException` **ohne Schlüssel**. `Config.get("fehlt")` ohne Standardwert wirft `IllegalStateException("Missing required configuration parameter [fehlt]")`.
- Listen: `Config.list().of(key)` trennt an Kommas. Eine YAML-Liste von Skalaren kommt als eine kommagetrennte Zeichenkette an.
- `application-test.{yaml,properties}` wird automatisch geladen, wenn sie im Classpath liegt (also nur in Tests). Dateiüberwachung läuft nur mit `config.watch.enabled=true`, standardmäßig also nicht.
- Die Fassade hat Mutatoren (`setProperty`, `putAll`, `clearProperty`), die global wirken.

## Goals / Non-Goals

**Goals:**
- Module lesen Werte über `Config` und nicht mehr über eine eigene Binding-Schicht.
- Standardwerte stehen an genau einer Stelle, in der mitgelieferten Classpath-`application.yaml`.
- Das Verhalten für Betreiber bleibt gleich: Schlüssel, Env-Namen, Profile, Abbruch bei ungültigen Werten mit Schlüssel und Grund. Einzige Ausnahme ist die entfallende Umstellung von `app.json` (Entscheidung 8).
- Tests bleiben F.I.R.S.T.-konform, obwohl die Fassade globaler Zustand ist.

**Non-Goals:**
- Neuladen zur Laufzeit (`onChange`, `config.watch.enabled`).
- Feature-Flags aus `flags.properties` nach avaje-config holen.
- Einen Standard-Navigatoreintrag per Konfiguration entfernen können (heute auch nicht möglich, siehe Risiken).
- Ein Werkzeug, das `app.json` außerhalb der Lobby umstellt. Wer es braucht, startet einmal das vorherige Release.

## Decisions

### 1. Die statische Fassade `Config` ist die einzige Quelle, einmal früh beim Start initialisiert

**Entscheidung:** Titan und der Setup-Server bauen keine eigene `Configuration` mehr. `ConfigurationFactory.load()` wird zu `ConfigurationFactory.initialise()`. Die Methode ist der erste Schritt in `Titan` bzw. im Setup-Server und läuft vor dem Aufbau des `BeanScope`. `ConfigurationLoader` entfällt, weil ihm ohne Migration nur noch dieser eine Aufruf bliebe. Sie greift einmal auf `Config.asConfiguration()` zu und löst damit den statischen Initialisierer aus. Einen `ExceptionInInitializerError` packt sie aus und übersetzt dessen Ursache mit den bestehenden, getesteten Hilfsmethoden `fileNameFrom`/`detailFrom` in `ConfigException.malformed(...)`. So bleibt der saubere Abbruch mit Datei und Stelle aus `standardized-config-profiles` erhalten.

Danach entfallen:
- `ConfigurationPropertyPlugin`: Avaje Inject nutzt ohne ihn die Fassade, und das ist jetzt dieselbe und einzige Instanz. Der Grund für das Plugin (zweite Instanz, spät gebaut) fällt weg.
- der `@External Configuration`-Bean in `Titan`/`PlatformBeans`.

`ConfigurationStartupLog.activeProfiles(...)` bekommt `Config.asConfiguration()`. Die INFO-Zeile `Active configuration profiles: {}` bleibt unverändert.

**Früh initialisieren:** Weil die Fassade beim ersten Zugriff lädt, soll dieser Zugriff an einer bekannten Stelle mit Fehlerübersetzung geschehen. Sonst träfe ein Ladefehler als roher `ExceptionInInitializerError` irgendwo im Start auf, etwa beim Aufbau des `BeanScope`.

**Built-in first:** Das ist das eingebaute Mittel von avaje-config. Die Alternative, die injizierte `Configuration`-Instanz mit derselben API zu nutzen, wurde angeboten und vom Maintainer zugunsten der Fassade verworfen. Die Mehrarbeit für Tests (Entscheidung 5) wird bewusst in Kauf genommen.

**Test:**
- Unit: `ConfigurationFactoryTest` behält die Tests für `fileNameFrom`/`detailFrom` und bekommt einen Fall, in dem ein `ExceptionInInitializerError` samt Ursache ausgepackt wird. Dieser Fall arbeitet mit einer selbst gebauten Exception, nicht mit der echten Fassade.
- Integration: Der bestehende `ConfigurationPrecedenceTest` startet für jeden Fall eine Kind-JVM. Deren `ConfigurationPrintMain` liest künftig über `Config` statt über eine eigene Instanz. Ein neuer Fall prüft: Eine kaputte `application.yaml` beendet die Kind-JVM mit einer Meldung, die Datei und Zeile nennt.

**SOLID:** Verletzt DIP bewusst. Module hängen von einer konkreten, statischen Quelle ab statt von einer injizierten Abstraktion. Das widerspricht der Projektregel „Abhängigkeiten über Konstruktor und Abstraktionen, nicht über statische Singletons“ und ist eine Entscheidung des Maintainers. Entscheidung 3 begrenzt die Folgen, indem die Fassade nur am Rand des Moduls gelesen wird.

**Log:** unverändert (INFO `Active configuration profiles: {}`, ERROR über den bestehenden Abbruchpfad).

### 2. Standardwerte als Classpath-`application.yaml`

**Entscheidung:**
- `app/src/main/resources/application.yaml` enthält alle Abschnitte mit allen Schlüsseln und Standardwerten, kommentiert. Den Inhalt liefert die heutige `application.example.yaml`, abgeglichen mit den `DEFAULTS` der Records (Charakterisierungstest, siehe Test).
- `setup/src/main/resources/application.yaml` enthält nur `spawn.simulationDistance: 2`.
- `common` liefert **keine** `application.yaml` aus. Sonst lägen im Lobby-Jar zwei Ressourcen gleichen Namens, und das Shadow-Jar würde eine davon stillschweigend verwerfen.
- avaje-config lädt die Classpath-Datei vor der Datei im Arbeitsverzeichnis. Die Rangfolge aus der Spec ergibt sich damit von selbst.
- Die Beispieldatei für Betreiber wird nicht mehr von Hand gepflegt. Ein Gradle-`Copy`-Schritt legt die Classpath-Datei als `application.example.yaml` in die Distribution. Die Ressource im Jar ist die einzige Quelle der Standardwerte, die Spec-Anforderung „für Betreiber einsehbar“ ist so erfüllt.
- Module lesen **ohne** Standardwert im Code (`Config.get(key)`, nicht `Config.get(key, "…")`). Fehlt ein Schlüssel in der Classpath-Datei, ist das ein Programmierfehler, und der Start bricht mit `Missing required configuration parameter [key]` ab.

**Built-in first:** Das Laden von `application.yaml` aus dem Classpath ist eingebautes Verhalten von avaje-config. Standardwerte im Code (`Config.get(key, default)`) wurden verworfen: Sie stünden verteilt über die Module und doppelt zur Beispieldatei.

**Test:**
- Charakterisierung (Unit, **vor** dem Entfernen der Records): Ein Test lädt die neue Classpath-Datei mit `Configuration.builder().load("application.yaml")` als eigene Instanz, nicht über die Fassade. Er vergleicht jeden Wert mit dem entsprechenden Feld der heutigen `DEFAULTS`. Danach ersetzt er `ApplicationExampleYamlTest`.
- Integration: Der `ModuleWiringTest` aktiviert alle Module nur mit den Classpath-Standardwerten. Fehlt ein Schlüssel, schlägt er fehl.

**SOLID:** SRP. Die Standardwerte sind Daten und stehen in einer Datei, nicht im Code.

### 3. Lesen am Rand, prüfen in reinen Funktionen

**Entscheidung:** Jedes Modul liest seine Werte in `enable()` über `Config`. Schlüssel sind `private static final String`-Konstanten im Modul (`"tickle.cooldownMillis"`). Die Prüfung liegt in reinen, statischen und package-privaten Funktionen, die Werte bekommen und fertige Werte zurückgeben oder `ConfigException.invalid(schlüssel, grund)` werfen. Beispiel:

```java
// TickleModule.enable()
Duration cooldown = TickleSettings.cooldown(Config.getAs(COOLDOWN_KEY, Long::parseLong));
```

`TickleSettings.cooldown(long)` prüft auf „nicht negativ“ und hat mit `Config` nichts zu tun. Die Klassen darunter (Listener, Cooldowns, Navigator-Einträge) bekommen fertige Werte per Konstruktor wie heute.

Welche Prüfungen wohin wandern, ergibt sich 1:1 aus den heutigen Compact Constructors. Sie werden vorher durch die bestehenden `*ConfigTest`-Fälle festgehalten, die auf die neuen Funktionen umziehen. Die Meldung nennt künftig den vollen Schlüssel (`tickle.cooldownMillis` statt `cooldownMillis`), wie es die geänderte Spec verlangt. Bei zwei Feldern (`spawn.minHeight` > `spawn.maxHeight`) nennt sie beide.

Dazu kommt `ConfigException.invalid` mit vollem Schlüssel. Heute setzt `ModuleRegistry` den Modulnamen davor. Das entfällt, weil der Schlüssel das Modul schon enthält, und wird dort angepasst.

**Built-in first:** avaje-config hat keine Validierung. Jakarta Bean Validation (Hibernate Validator) wurde verworfen, weil sie Reflection und eine schwere Abhängigkeit mitbringt und ohne Records bzw. Beans nichts zu annotieren hätte.

**Test:** Unit-Tests auf die reinen Funktionen, ohne `Config` und ohne Server: gültige Grenzwerte, ungültige Werte, Text der Meldung. Das sind die umgezogenen Fälle aus `SitConfigTest`, `SpawnConfigTest`, `TickleConfigTest`, `ElytraConfigTest` und `NavigatorConfigTest`.

**SOLID:** SRP (Lesen und Prüfen getrennt), DIP bleibt für alles unterhalb von `enable()` erhalten, weil Werte per Konstruktor kommen.

### 4. Zahlen lesen: `Config.getAs(key, fn)`, kein eigenes Hilfsmittel

**Entscheidung:** Ein Modul liest einen numerischen Wert mit `Config.getAs(key, Integer::parseInt)` (entsprechend `Long::parseLong`, `Double::parseDouble`) statt mit einer eigenen Klasse. Strings, Listen und Wahrheitswerte lesen Module weiterhin direkt mit `Config.get`, `Config.list().of` und `Config.getBool`.

**Built-in first:** `Config.getInt/getLong/getDecimal` wurden geprüft und für Zahlen verworfen: Sie werfen bei einem ungültigen Wert eine bloße `NumberFormatException` ohne Schlüssel, die Spec-Szenarien „Ungültiger Override“ verlangen aber den Schlüssel in der Meldung. `Config.getAs(key, fn)` dagegen ist genau dafür gebaut: Schlägt `fn` fehl, fängt avaje-config selbst die Exception, benennt den Schlüssel einmal in einer neuen `IllegalStateException("Failed to convert key: <key> sourced from: <quelle> with the provided function", ursprünglicheException)` und hängt die ursprüngliche Exception (z. B. die `NumberFormatException`, deren eigene Meldung den Rohwert wie `abc` trägt) als `cause` an. Damit nennt der eingebaute Weg selbst den Schlüssel einmal und behält den Grund in der Ursache - eine frühere Version dieser Entscheidung ging (ungeprüft) davon aus, `getAs` kenne den Schlüssel nicht; das stimmt nicht (verifiziert gegen den avaje-config-5.2-Quellcode, `CoreConfiguration#getAs`, und empirisch über einen Kind-JVM-Testfall). Eine eigene Hilfsklasse fürs Zahlenlesen ist damit unnötig und entfällt ersatzlos.

**Test:** Kein eigener Unit-Test hier: Es gibt keine eigene Parsing-Logik mehr zu testen. Die Kind-JVM-Fälle in `ConfigurationPrecedenceTest` (`--validate-tickle` mit `TICKLE_COOLDOWNMILLIS=abc`) decken den eingebauten Pfad end-to-end ab und prüfen, dass der ausgegebene Text den Schlüssel genau einmal und den Grund (`abc`, aus der `cause`-Kette) enthält.

### 5. Tests und globaler Zustand

**Entscheidung:** Die Fassade ist pro JVM genau einmal geladen und danach unveränderlich, solange niemand ihre Mutatoren aufruft. Daraus folgen Regeln:
- **Kein Test** ruft `Config.setProperty`, `Config.putAll`, `Config.clearProperty` oder `Config.eventBuilder` auf. Das wäre gemeinsamer, veränderlicher Zustand und bricht „Independent“. Ein Grep über alle Testquellen in der Verifikation der Tasks und das Review setzen das durch.
- **Unit-Tests** lesen keine Config. Sie testen die reinen Funktionen aus Entscheidung 3 und 4 und die Klassen darunter mit Werten per Konstruktor.
- **Modul-Integrationstests** (Cyano, `ModuleHarness`) aktivieren Module mit den Classpath-Standardwerten. Es gibt **keine** `application-test.yaml`, damit die Tests die ausgelieferten Standardwerte prüfen und nicht eine eigene Testwelt.
- **Tests, die andere Werte brauchen** (Rangfolge, Profile, Env, kaputte Datei, ungültiger Override), laufen in einer Kind-JVM mit `@TempDir` als Arbeitsverzeichnis, wie der bestehende `ConfigurationPrecedenceTest`. Das ist bei avaje-config ohnehin nötig, weil Arbeitsverzeichnis und Env nur pro Prozess gelten.
- Heutige Modultests, die einen abweichenden Config-Wert setzen (etwa über `ModuleHarness` mit eigenem `ConfigSections`), stellen auf das Testen der Klasse unterhalb von `enable()` mit dem Wert per Konstruktor um. Oder sie prüfen den Standardwert.
- `ModuleHarness` und `ModulePlatformFixture` verlieren den Config-Parameter. Im Test-Beispiel `ExampleModule` entfällt `ExampleConfig`. Das Beispiel zeigt stattdessen das Muster aus Entscheidung 3 mit einer reinen Prüffunktion, die ohne `Config` getestet wird.

**Built-in first:** `application-test.yaml` ist der eingebaute Test-Mechanismus von avaje-config. Er wurde verworfen, weil er eine zweite Wertewelt neben den ausgelieferten Standardwerten schafft, die nur in Tests gilt.

**Test:** Diese Entscheidung ist selbst eine Testregel. Die Verifikation ist der Grep-Check in den Tasks und die Review-Prüfung auf F.I.R.S.T.

**SOLID:** Nicht zutreffend (Testregel).

### 6. Navigator-Einträge über `forPath`

**Entscheidung:** Der Navigator ermittelt seine Einträge aus den Schlüsseln unter `navigator.entries`: `Config.asConfiguration().forPath("navigator.entries").keys()` liefert z. B. `survival.slot`, `survival.icon`. Das erste Segment ist der Name des Eintrags. Pro Name liest er `slot`, `icon`, `displayName`, `destination` und optional `feature`. Weil die Classpath-Standardwerte und die Datei im Arbeitsverzeichnis zusammengeführt werden, erscheinen Einträge aus der Datei zusätzlich zu den Standardeinträgen. Das entspricht dem Spec-Szenario „Liste von Einträgen“ und dem heutigen Verhalten.

Die Prüfungen aus `NavigatorConfig.Entry` (Platz 0–8, bekanntes Material, Ziel nicht leer) wandern in eine reine Funktion, die aus den gelesenen Werten ein `NavigatorEntry` baut. Die Prüfung auf doppelte Plätze und unbekannte Flags bleibt in `NavigatorEntries#validate(...)`.

**Built-in first:** `forPath(...).keys()` ist eingebaut. Eine Liste von Einträgen (`entries[0].slot`) wurde schon in `standardized-config-profiles` verworfen, weil sie sich nicht gezielt per Profil überschreiben lässt.

**Test:** Unit-Test der reinen Funktion „Namen aus Schlüsseln ermitteln“ (`survival.slot`, `survival.icon`, `parkour.slot` → `{survival, parkour}`) und der Eintrag-Prüfung. Das Szenario „Liste von Einträgen“ und „Profil ändert ein einzelnes Ziel“ als Kind-JVM-Fall im `ConfigurationPrecedenceTest`.

**SOLID:** OCP. Neue Ziele kommen weiter nur über Konfiguration dazu.

### 7. Was entfernt wird

**Entscheidung:** Entfernt werden, jeweils samt Tests:
- aus `common`: `ConfigSections`, `SectionBinder`, `RecordFields`, `KeyGsonAdapter` und die Test-Records `*TestConfig`, dazu `AppJsonMigration` und `LegacyConfigMigration` mit den Fixtures unter `common/src/test/resources/config/` (siehe Entscheidung 8),
- aus `app`: die fünf Config-Records, `ModuleContext.config(...)`, der Config-Parameter von `ModulePlatform` und `ModuleRegistry.Builder#config`, `PlatformBeans#configSections`, `ConfigurationPropertyPlugin`, `ConfigurationLoader`, die handgepflegte `app/src/dist/application.example.yaml` (wird generiert, siehe Entscheidung 2),
- aus `setup`: der `ConfigSections`-Weg in `SetupSpawnConfig` und `LegacyAppJsonWarning` samt Aufruf.

`ConfigException` bleibt (`invalid` für Prüfungen, `malformed` für kaputte YAML). Der `CapturingLogger` unter `common/src/test/.../config/testing` bleibt, weil `DebugDeliverTest` ihn nutzt.

**Built-in first:** Nicht anwendbar, Code wird entfernt.

**Test:** `./gradlew build` ist grün, und `grep -rnE "ConfigSections|SectionBinder|AppJsonMigration|LegacyConfigMigration|LegacyAppJsonWarning|\.config\(.*DEFAULTS" --include=*.java app common setup` findet nichts.

**SOLID:** Nicht zutreffend.

### 8. Die Umstellung von `app.json` entfällt

**Entscheidung:** `AppJsonMigration`, `LegacyConfigMigration` und `LegacyAppJsonWarning` werden ersatzlos entfernt. Eine `app.json` im Arbeitsverzeichnis wird nicht mehr gelesen, umgestellt oder gemeldet. Die Umstellung war für den Übergang gebaut und ist mit dem Release von `standardized-config-profiles` ausgeliefert. Sie weiter mitzuschleppen hieße, ein zweites Format (JSON v1/v2) und SnakeYAML-`Dump` dauerhaft zu pflegen.

**Built-in first:** Nicht anwendbar, Code wird entfernt.

**Test:** Die Tests der Umstellung entfallen mit ihr. Der Smoke-Test prüft, dass eine übrig gebliebene `app.json` den Start nicht stört und die Standardwerte gelten.

**SOLID:** Nicht zutreffend.

## Risks / Trade-offs

- **[Risiko, BREAKING] Eine nicht umgestellte `app.json` wird stumm ignoriert.** Wer das Release mit `standardized-config-profiles` überspringt, startet mit den Standardwerten. → Mitigation: `BREAKING CHANGE`-Footer, Hinweis in Release Notes und README. Beim eigenen Deployment ist die Umstellung bereits gelaufen (Migrationsplan Schritt 2).
- **[Risiko] `Config` wird vor `ConfigurationFactory.initialise()` berührt**, etwa durch einen statischen Initialisierer. Dann kommt ein Ladefehler ohne Übersetzung an. → Mitigation: `initialise()` ist der erste Schritt im Start, und der Kind-JVM-Test zur kaputten YAML prüft die Meldung.
- **[Risiko] Globaler Zustand in Tests.** Ein einzelner Aufruf von `Config.setProperty` in einem Test macht andere Tests abhängig von der Reihenfolge. → Mitigation: Verbot und Grep-Check (Entscheidung 5), Kind-JVMs für abweichende Werte.
- **[Trade-off] Weniger Isolation zwischen Modulen.** Technisch kann jedes Modul jeden Schlüssel lesen. → Die Spec verlangt es weiter als Regel. Die Schlüssel-Konstanten stehen im Modul selbst, das Review prüft das Präfix.
- **[Trade-off] Keine Warnung mehr bei unbekannten oder vertippten Schlüsseln.** Ein Tippfehler in der eigenen `application.yaml` bleibt stumm, es gilt der Standardwert. → Die Classpath-Datei und die generierte Beispieldatei zeigen alle gültigen Schlüssel. Die README weist darauf hin.
- **[Trade-off] Kind-JVM-Tests sind langsamer** als die bisherigen In-Process-Tests mit `Configuration` aus einer Map. → Nur für die Fälle mit abweichenden Werten, die breite Basis bleibt bei schnellen Unit-Tests auf reine Funktionen.
- **[Risiko] Listen mit Kommas.** `Config.list().of` trennt an Kommas. Für `sit.allowedBlocks` (Block-IDs) unkritisch, wie schon in `standardized-config-profiles` festgestellt.
- **[Trade-off] Standard-Navigatoreinträge lassen sich nicht entfernen**, nur ändern, weil die Classpath-Datei immer mitgeladen wird. Das ist heute durch das Zusammenführen mit `DEFAULTS` genauso. Wird es gebraucht, ist das eine eigene Change (z. B. ein `enabled`-Schlüssel pro Eintrag).

## Migration Plan

1. Voraussetzung: `standardized-config-profiles` ist gemergt und archiviert. Diese Change zweigt danach vom aktuellen `origin/main` ab.
2. **Vor dem Update:** Das Release mit `standardized-config-profiles` muss auf jedem Server einmal gestartet sein, damit `app.json` in `application.yaml` umgestellt ist. Alternativ wird `app.json` von Hand übertragen (gleiche Abschnitte und Schlüssel). Übrig gebliebene `app.json` bzw. `app.json.migrated` können danach gelöscht werden.
3. Ausrollen wie gewohnt. Sonst ändert sich für Betreiber nichts: gleiche Dateien, Schlüssel, Profile und Env-Variablen. Die Warnungen zu unbekannten Schlüsseln und zu `app.json` erscheinen nicht mehr.
4. **Rollback:** das vorherige Jar zurück. Es liest dieselbe `application.yaml`. Weil `application.yaml` schon existiert, lässt es eine eventuell vorhandene `app.json` unangetastet.
