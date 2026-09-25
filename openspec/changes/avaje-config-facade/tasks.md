# Tasks

## Execution Plan

Voraussetzung: `standardized-config-profiles` ist gemergt und archiviert. Integrations-Branch: `refactor/avaje-config-facade`, abgezweigt vom aktuellen `origin/main`. Jeder Agent arbeitet in einem eigenen Worktree und beginnt mit `git reset --hard refactor/avaje-config-facade`. Nach jeder Welle wird zusammengeführt und `./gradlew build` ausgeführt. Danach prüfen ein Sonnet-Review und ein Haiku-Check (F.I.R.S.T., keine `Config`-Mutatoren in Tests), und Befunde behebt ein Sonnet-Agent.

Jeder Agent-Prompt wiederholt die Regeln, die für ihn gelten:
- Built-in first: Die `Config`-Fassade direkt nutzen, eigene Hilfen nur nach Design-Entscheidung 4.
- Java-25-Features.
- Logging nur über SLF4J mit Parametern.
- Keine neuen Texte für Spieler, also kein i18n-Bedarf.
- Keine neuen Metriken oder Spans.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| A | foundation | 1.1–1.4 | sonnet | `app/src/main/resources/application.yaml`, `setup/src/main/resources/application.yaml`, `app/build.gradle.kts`, `app/src/test/.../bootstrap/**`, `app/src/main/.../Titan.java`, `app/src/main/.../bootstrap/ConfigurationLoader.java`, `common/src/{main,test}/.../config/{ConfigValues,ConfigurationFactory}*`, `setup/src/main/.../Titan*.java`, `setup/src/{main,test}/.../config/LegacyAppJsonWarning*` | `app/src/main/.../feature/**`, `app/src/main/.../module/**`, `ConfigSections`, `SectionBinder` |
| B1 (parallel zu A) | sit-spawn, tickle-elytra, navigator | 2.1–2.5, erster Teil: Prüfungen als reine Funktionen, Record delegiert | sonnet | je `app/src/{main,test}/.../feature/<modul>/**` | `module/**`, `bootstrap/**`, `common/**`, andere Features |
| B1 (parallel zu A) | docs | 4.3 (Entwurf) | sonnet | `README.md`, `docs/**` | Code |
| B2 (nach A) | sit-spawn, tickle-elytra, navigator | 2.1–2.5, zweiter Teil: `enable()` liest über `Config`, Record entfällt | sonnet | wie B1, Navigator zusätzlich `app/src/{main,test}/.../module/navigator/**` | wie B1 |
| C | platform | 3.1–3.4 | sonnet | `app/src/{main,test}/.../{module,bootstrap}/**`, `app/src/test/.../feature/example/**`, `app/src/main/.../Titan.java`, `app/src/dist/**`, `common/src/{main,test}/.../config/**` | `setup/**`, `feature/{sit,spawn,tickle,elytra,navigator}/**` außer Anpassungen an entfernte Harness-Parameter |
| C | setup | 3.5 | sonnet | `setup/**` | `app/**`, `common/**` |
| D | docs, Hauptkontext | 4.1–4.4, 5.1 | sonnet / – | `README.md`, `docs/**` | Code |

## 1. Grundlage (Welle A)

- [x] 1.1 **Charakterisierungstest zuerst (Unit):** `app/src/main/resources/application.yaml` aus der heutigen `app/src/dist/application.example.yaml` anlegen (kommentiert, alle Abschnitte). Ein Test lädt sie mit `Configuration.builder().load("application.yaml")` als eigene Instanz (nicht über die Fassade) und vergleicht jeden Schlüssel mit dem Feld der heutigen `DEFAULTS` von `SitConfig`, `SpawnConfig`, `TickleConfig`, `ElytraConfig` und `NavigatorConfig`. Dazu `setup/src/main/resources/application.yaml` mit `spawn.simulationDistance: 2`. Verifikation: Der Test ist grün, und `common` enthält keine `application.yaml`.
- [x] 1.2 **Unit-Tests zuerst:** `ConfigValues` in `common` mit `parseInt`/`parseLong`/`parseDouble(key, raw)` (gültig, `abc`, leer, Überlauf; die Meldung nennt den vollen Schlüssel) und dünnen Methoden `intValue`/`longValue`/`doubleValue(key)` über `Config.get`. Verifikation: Die Tests sind zuerst rot, dann grün. Kein Test ruft `Config` auf.
- [x] 1.3 **Unit-Test zuerst:** `ConfigurationFactory.load()` wird zu `initialise()`. Die Methode löst die Fassade über `Config.asConfiguration()` aus, packt einen `ExceptionInInitializerError` aus und übersetzt ihn mit `fileNameFrom`/`detailFrom` in `ConfigException.malformed`. Der neue Testfall arbeitet mit einer selbst gebauten Exception. Verifikation: `ConfigurationFactoryTest` ist grün.
- [x] 1.4 Lobby und Setup-Server auf die Fassade umstellen: `ConfigurationFactory.initialise()` ist der erste Schritt beim Start. `ConfigurationLoader` und damit der Aufruf von `AppJsonMigration` entfallen, im Setup-Server entfallen `LegacyAppJsonWarning` samt Test und Aufruf. `Titan` reicht übergangsweise `Config.asConfiguration()` als `@External`-Bean weiter, damit `ConfigSections` bis Welle C funktioniert. Der Gradle-Schritt kopiert die Classpath-Datei als `application.example.yaml` in die Distribution. **Integration zuerst**, `ConfigurationPrecedenceTest`: `ConfigurationPrintMain` liest über `Config`, und es gibt neue Kind-JVM-Fälle „Datei schlägt Standardwert“, „kaputte YAML nennt Datei und Zeile“ und „eine vorhandene `app.json` wird ignoriert, es gelten die Standardwerte“. Bestehende Fälle zur Umstellung von `app.json` entfallen. Verifikation: `./gradlew build` ist grün, `./gradlew :app:installDist` legt `application.example.yaml` in die Distribution, und `grep -rn "AppJsonMigration\|LegacyAppJsonWarning" app/src setup/src` findet nichts.

## 2. Module auf die Fassade (Welle B, parallel, test-first)

- [ ] 2.1 **sit, Unit zuerst:** Die Fälle aus `SitConfigTest` ziehen auf eine reine, package-private Prüffunktion um. Deren Meldungen nennen den vollen Schlüssel (`sit.offset.y`, `sit.allowedBlocks`). `SitModule.enable()` liest über `ConfigValues`/`Config.list().of`, `SitConfig` und `SitConfigTest` entfallen. Modultests, die abweichende Werte über `ModuleHarness` setzen, prüfen den Standardwert oder die Klasse unterhalb von `enable()` mit dem Wert per Konstruktor. Verifikation: Die Tests unter `feature/sit` sind grün, und `grep -rn "SitConfig" app/src` findet nichts.
- [ ] 2.2 **spawn, Unit zuerst:** wie 2.1 für `SpawnConfig`. Die Meldung zu `minHeight` > `maxHeight` nennt `spawn.minHeight` und `spawn.maxHeight`. Verifikation: Die Tests unter `feature/spawn` sind grün, und `grep -rn "SpawnConfig\b" app/src` findet nichts.
- [ ] 2.3 **tickle, Unit zuerst:** wie 2.1 für `TickleConfig`. Die Meldung lautet „`tickle.cooldownMillis` must not be negative“. Verifikation: Die Tests unter `feature/tickle` sind grün, und `grep -rn "TickleConfig" app/src` findet nichts.
- [ ] 2.4 **elytra, Unit zuerst:** wie 2.1 für `ElytraConfig`. Verifikation: Die Tests unter `feature/elytra` sind grün, und `grep -rn "ElytraConfig" app/src` findet nichts.
- [ ] 2.5 **navigator, Unit zuerst:** eine reine Funktion „Namen aus Schlüsseln unter `navigator.entries`“ (`survival.slot`, `parkour.icon` → `{survival, parkour}`) und eine reine Funktion, die aus gelesenen Werten einen Eintrag baut und prüft (Platz 0–8, bekanntes Material, Ziel nicht leer, Meldung mit `navigator.entries.<name>.<feld>`). `NavigatorModule.enable()` liest Titel und Einträge über `Config` bzw. `Config.asConfiguration().forPath(...)`. `NavigatorConfig` samt Test entfällt. Die Tests zu Feature-Flags und doppelten Plätzen in `NavigatorEntries` bleiben grün. Verifikation: Die Navigator-Tests sind grün, und `grep -rn "NavigatorConfig" app/src` findet nichts.

## 3. Aufräumen der Plattform (Welle C)

- [ ] 3.1 `ModuleContext.config(...)`, den Config-Parameter von `ModulePlatform`, `ModuleRegistry.Builder#config`, `PlatformBeans#configSections`, den `@External Configuration`-Bean in `Titan` und `ConfigurationPropertyPlugin` samt Tests entfernen. `ModuleHarness` und `ModulePlatformFixture` verlieren den Config-Parameter. `ModuleRegistry` setzt vor eine `ConfigException` nicht mehr den Modulnamen, weil der Schlüssel das Modul schon nennt, und der Test dazu wird angepasst. `ConfigurationStartupLog` liest über `Config.asConfiguration()`, der Appender-Test bleibt grün. Verifikation: Der `ModuleWiringTest` (Integration) aktiviert alle Module nur mit den Classpath-Standardwerten und ist grün.
- [ ] 3.2 `ExampleConfig` entfernen, und `ExampleModule` zeigt das Muster „Lesen am Rand, Prüfen in reiner Funktion“ mit einem Unit-Test ohne `Config`. Verifikation: Die Tests unter `feature/example` sind grün.
- [ ] 3.3 In `common` `ConfigSections`, `SectionBinder`, `RecordFields`, `KeyGsonAdapter`, die `*TestConfig`-Records, `AppJsonMigration`, `LegacyConfigMigration`, ihre Tests (auch `AppJsonMigrationAtomicWriteTest` und `AppJsonMigrationRoundTripTest`) und die Fixtures unter `common/src/test/resources/config/` entfernen. `ConfigException` und der `CapturingLogger` bleiben (`DebugDeliverTest`). `ApplicationExampleYamlTest` wird durch den Test aus 1.1 ersetzt, der nur noch prüft, dass die Classpath-Datei ohne Fehler lädt und die erwarteten Schlüssel enthält. Verifikation: `./gradlew :common:build` ist grün, und `grep -rnE "ConfigSections|SectionBinder|RecordFields|KeyGsonAdapter|AppJsonMigration|LegacyConfigMigration" --include=*.java app common setup` findet nichts.
- [ ] 3.4 **Integration, Kind-JVM:** Im `ConfigurationPrecedenceTest` kommen die Spec-Szenarien „Ungültiger Override“ (Env `TICKLE_COOLDOWNMILLIS=abc` beendet den Start mit `tickle.cooldownMillis`), „Negative Dauer“, „Profil ändert nur einen Wert eines Abschnitts“, „Datei setzt nur einzelne Werte“ und „Liste von Einträgen“ (Eintrag `parkour` erscheint zusätzlich) dazu. Die bestehenden Fälle zu Profilen, Env und System-Property bleiben. Verifikation: Die Tests sind grün, und jeder Fall läuft in einer eigenen Kind-JVM mit `@TempDir`.
- [ ] 3.5 **Setup, Unit zuerst:** `SetupSpawnConfig` liest `spawn.simulationDistance` über `ConfigValues` aus der Fassade (Standardwert aus `setup/src/main/resources/application.yaml`). Die Prüfung liegt in einer reinen Funktion mit Unit-Test, `SetupSpawnConfigTest` testet sie ohne `ConfigSections`. Verifikation: `./gradlew :setup:build` ist grün, und `grep -rn "ConfigSections" setup/` findet nichts.

## 4. Abnahme und Doku (Welle D)

- [ ] 4.1 **F.I.R.S.T.-Check:** `grep -rnE "Config\.(setProperty|putAll|clearProperty|eventBuilder)" --include=*.java */src/test` findet nichts, und es existiert keine `application-test.yaml` bzw. `application-test.properties`. Ein Haiku-Agent prüft zusätzlich alle geänderten Tests auf Sleeps, Systemzeit und Abhängigkeit von der Reihenfolge. Verifikation: Die Ausgaben stehen im PR.
- [ ] 4.2 **E2E-Smoke-Test** mit dem Shaded-Jar in Scratch-Verzeichnissen:
  - (a) Start ohne `application.yaml`: Die Standardwerte greifen, und es entsteht keine Datei.
  - (b) `application.yaml` mit nur `tickle.cooldownMillis: 1000` plus `AVAJE_PROFILES=dev` und `application-dev.yaml`: Die Log-Zeile nennt das Profil, und die Werte greifen.
  - (c) Env `TICKLE_COOLDOWNMILLIS=abc`: Der Start bricht mit Schlüssel und Grund ab.
  - (d) übrig gebliebene `app.json` ohne `application.yaml`: Der Start läuft mit den Standardwerten, und die Datei bleibt unverändert.

  Verifikation: Die wichtigsten Log-Zeilen stehen im PR.
- [ ] 4.3 README und `docs/lobby-modules.md` anpassen:
  - Für Modul-Autoren: das Muster mit `Config`, `ConfigValues` und reinen Prüffunktionen, die Testregeln aus Design-Entscheidung 5, und dass Standardwerte in die Classpath-`application.yaml` gehören.
  - Für Betreiber: Wo die Standardwerte stehen (`application.example.yaml` in der Distribution), und dass unbekannte Schlüssel nicht mehr gemeldet werden. Der Abschnitt zur Umstellung von `app.json` wird ersetzt durch den Upgrade-Hinweis: vorher einmal das Release mit `standardized-config-profiles` starten oder `app.json` von Hand übertragen.

  Verifikation: Ein Review-Agent prüft alle Schlüssel gegen die Classpath-Datei.
- [ ] 4.4 Lokale Abnahme im Client durch den Maintainer: Die Lobby verhält sich unverändert, und ein Wert aus einem `dev`-Profil greift. Verifikation: Die Checkliste im PR ist abgehakt.

## 5. Pull Request

- [ ] 5.1 Den Pull Request `refactor(config)!: read configuration through the avaje config facade` von `refactor/avaje-config-facade` nach `main` öffnen. Titel und Beschreibung sind auf Englisch. Die Beschreibung enthält den `BREAKING CHANGE`-Footer aus proposal.md, den Upgrade-Hinweis aus dem Migrationsplan in design.md, die entfernten Klassen, die Testregeln zur Fassade, die Ausgaben aus 4.1 und 4.2 und die Abnahme-Checkliste. Verifikation: Der PR existiert, und die CI ist grün.
