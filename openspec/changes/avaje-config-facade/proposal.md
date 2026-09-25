# Proposal

## Why

Nach `standardized-config-profiles` lädt avaje-config die Konfiguration, aber Titan legt eine eigene Schicht darüber: `ConfigSections` und `SectionBinder` bauen aus den flachen Schlüsseln einen Gson-Baum, binden ihn an Config-Records mit `DEFAULTS`, warnen bei unbekannten Schlüsseln, und `ModuleContext.config(...)`, `PlatformBeans` und `ConfigurationPropertyPlugin` reichen die `Configuration` durch. Das sind rund 1.000 Zeilen eigene Infrastruktur für etwas, das avaje-config direkt kann.

Künftig lesen Module ihre Werte so, wie avaje-config es vorsieht: `Config.get("sit.offset.y")`, `Config.getInt(...)`, `Config.list().of(...)`. Die Standardwerte stehen in einer mitgelieferten `application.yaml` im Classpath, statt doppelt in Records und Beispieldatei.

**Auslieferung:** `refactor(config)!: read configuration through the avaje config facade`. Der PR trägt diesen Footer:

`BREAKING CHANGE: an existing app.json is no longer converted; run the previous release once or convert it to application.yaml by hand before upgrading.`

Dateinamen, Schlüssel, Profile und Env-Variablen bleiben gleich. Breaking ist nur der Wegfall der einmaligen Umstellung von `app.json`: Wer das Release mit `standardized-config-profiles` überspringt, startet sonst mit den Standardwerten. Die interne Modul-API ändert sich (`ModuleContext.config` fällt weg), Module sind aber nur Titan-intern.

**Voraussetzung:** `standardized-config-profiles` ist gemergt und archiviert. Diese Change zweigt danach von `main` ab.

## What Changes

- **Zugriff über die statische Fassade:** Module lesen Werte direkt über `io.avaje.config.Config`. Die Schlüssel bleiben unverändert (`<modul>.<feld>`, z. B. `tickle.cooldownMillis`). Das ist eine bewusste Abweichung von der Projektregel „keine statischen Singletons“. Der Zugriff bleibt deshalb auf den Rand des Moduls beschränkt (`enable()`), die Logik darunter bekommt fertige Werte per Konstruktor.
- **Standardwerte im Classpath:** Eine `application.yaml` in `app/src/main/resources` enthält alle Abschnitte mit ihren Standardwerten. avaje-config lädt sie vor der Datei im Arbeitsverzeichnis, die sie überschreibt. Der Setup-Server liefert entsprechend eine eigene mit `spawn.simulationDistance: 2` aus.
- **Config-Records entfallen:** `SitConfig`, `SpawnConfig`, `TickleConfig`, `ElytraConfig` und `NavigatorConfig` samt `DEFAULTS` und Tests werden entfernt, ebenso das Test-Beispiel `ExampleConfig`.
- **Validierung bleibt:** Ungültige Werte brechen den Start weiterhin mit Schlüssel und Grund ab. Die Prüfungen wandern aus den Compact Constructors in die Module bzw. in die Klassen, die die Werte verwenden.
- **Entfällt:**
  - `ConfigSections`, `SectionBinder`, `RecordFields` und `KeyGsonAdapter`, soweit sie nur dem Binding dienen,
  - `ModuleContext.config(...)` und der Config-Parameter in `ModulePlatform` bzw. `ModuleRegistry`,
  - der `ConfigSections`-Bean in `PlatformBeans`,
  - `ConfigurationPropertyPlugin`, weil Avaje Inject ohnehin die statische Fassade nutzt,
  - die Warnung bei unbekannten Schlüsseln, weil es ohne Records kein Schema bekannter Schlüssel mehr gibt,
  - die Kommentar-Beispieldatei `app/src/dist/application.example.yaml`, weil die Classpath-Datei ihre Rolle übernimmt. Das Design legt fest, wie Betreiber sie trotzdem einsehen können,
  - **BREAKING:** die einmalige Umstellung von `app.json` (`AppJsonMigration`, `LegacyConfigMigration`) und im Setup-Server die Warnung zu einer übrig gebliebenen `app.json` (`LegacyAppJsonWarning`). Eine vorhandene `app.json` wird künftig nicht mehr gelesen.
- **Bleibt:** die Rangfolge der Quellen, die Profil-Aktivierung und der saubere Abbruch bei kaputter `application.yaml` mit Datei und Stelle des Fehlers.

## Capabilities

### New Capabilities
<!-- keine -->

### Modified Capabilities
- `lobby-module-config`: Die Anforderungen aus `standardized-config-profiles` ändern sich an fünf Stellen:
  - Die einmalige Umstellung von `app.json` entfällt.
  - Ein Modul ist nicht mehr technisch auf seinen Abschnitt beschränkt, die Schlüssel folgen aber weiter dem Schema `<modul>.<feld>`.
  - Die unterste Stufe der Rangfolge sind die mitgelieferten Standardwerte statt der Standardwerte des Records.
  - Die Meldung unbekannter Schlüssel entfällt.
  - Ungültige Werte werden weiterhin mit Schlüssel und Grund gemeldet, die Formulierung löst sich vom Config-Record.

## Impact

- **Code:**
  - `common/config`: `ConfigSections`, `SectionBinder`, `RecordFields`, `KeyGsonAdapter`, `AppJsonMigration` und `LegacyConfigMigration` werden entfernt, samt Tests und den `app.json`-Fixtures unter `common/src/test/resources/config/`. Es gibt keine eigene `ConfigurationFactory` mehr; der erste Zugriff auf die statische Fassade ist der ohnehin vorhandene erste, beabsichtigte Zugriff beim Start (Startup-Log bzw. erster Read), ein Ladefehler taucht unübersetzt als `ExceptionInInitializerError` auf, mit Datei und Stelle bereits in der Ursachenkette, und läuft bis zum bestehenden Abbruchpfad durch. `ConfigException.malformed(...)` entfällt entsprechend, `invalid`/`withSection` bleiben.
  - `app`: `Titan`, `PlatformBeans`, `ConfigurationLoader` (ohne Migrationsschritt), `ConfigurationPropertyPlugin`, `ConfigurationStartupLog`, `ModuleContext`, `ModulePlatform`, `ModuleRegistry`, die fünf Feature-Module und deren Tests, sowie `ModuleHarness`.
  - `setup`: `SetupSpawnConfig` liest über `Config`. `LegacyAppJsonWarning` und ihr Aufruf in `setup/.../Titan` entfallen.
- **Abhängigkeiten:** keine neuen. `avaje-config` und `snakeyaml` sind schon da. Gson bleibt für `MapProvider`, `snakeyaml` bleibt als YAML-Parser für avaje-config.
- **Tests:** Weil die Fassade globaler Zustand ist, lesen Unit-Tests keine Config mehr. Sie bekommen Werte per Konstruktor. Tests, die das Lesen selbst prüfen, stützen sich auf `application-test.yaml` bzw. einen eigenen Prozess (siehe Design).
- **Texte für Nutzer:** keine. Für Betreiber entfallen die Log-Warnungen zu unbekannten Schlüsseln, zur Umstellung von `app.json` und zu einer übrig gebliebenen `app.json`.
- **Betrieb (BREAKING):** Vor dem Update muss das Release mit `standardized-config-profiles` einmal gelaufen sein, oder `app.json` wird von Hand in `application.yaml` übertragen.
- **Doku:** README und `docs/lobby-modules.md` beschreiben den neuen Zugriff für Modul-Autoren und wo die Standardwerte stehen.
