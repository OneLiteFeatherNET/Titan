# Tasks

## Execution Plan

Voraussetzung: `avaje-dependency-injection` ist gemergt (`PlatformBeans` und `BeanScope` existieren). Integrations-Branch: `feat/standardized-config-profiles`, abgezweigt vom aktuellen `origin/main`. Jeder Agent beginnt mit `git reset --hard feat/standardized-config-profiles`. Nach jeder Welle wird zusammengeführt und gebaut, danach laufen ein Sonnet-Review und ein Haiku-Check, Befunde behebt ein Sonnet-Agent.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| A | spike | 1.1–1.3 | sonnet | Scratch-Worktree; Ergebnisse nur in `openspec/changes/standardized-config-profiles/design.md` | Produktionscode |
| B | binder | 2.1–2.3 | sonnet | `common/src/{main,test}/.../config/**` (neu: `ConfigSections`, `SectionBinder`), `settings.gradle.kts`, `common/build.gradle.kts` | `app/**`, `setup/**`, Umstellungs-Klassen |
| B | migration | 3.1–3.2 | sonnet | `common/src/{main,test}/.../config/AppJsonMigration*`, YAML-Schreibwerkzeug, Test-Fixtures | `ConfigSections`, `app/**`, `setup/**` |
| C | wiring | 4.1–4.4 | sonnet | `app/src/{main,test}/**` (`bootstrap/`, `module/`, `feature/navigator/NavigatorConfig`, `Titan`), `common/.../config/ConfigStore*` (entfernen), `app.json` → `app/src/dist/application.example.yaml` | `setup/**` |
| C | setup | 4.5 | sonnet | `setup/**` | `app/**`, `common/**` |
| D | docs + Hauptkontext | 5.1–5.3, 6.1 | sonnet / – | `README.md`, `docs/**` | Code |

## 1. Spike (Welle A)

- [ ] 1.1 Die aktuelle Version von `io.avaje:avaje-config` bestimmen (Maven Central, Datum) und Java 25 bestätigen. Außerdem prüfen, wie eine `Configuration`-Instanz ohne die statische Fassade gebaut wird (Builder bzw. `Configuration.builder()`, Quellen aus Arbeitsverzeichnis, Profilen, `PROPS_FILE`). Verifikation: Ergebnis und Codeschnipsel stehen in design.md.
- [ ] 1.2 Die YAML-Unterstützung von avaje-config prüfen: Eigener Parser oder SnakeYAML? Ist SnakeYAML transitiv im Klassenpfad? Außerdem: Wie werden YAML-Listen (Strings mit Komma) und verschachtelte Maps abgeflacht? Verifikation: Ein Wegwerf-Test mit `allowedBlocks`-Liste, `offset`-Map und `navigator.entries.<name>` zeigt die flachen Schlüssel, das Ergebnis steht in design.md Entscheidung 2 bzw. 4.
- [ ] 1.3 Rangfolge und Abbildung auf Env-Variablen empirisch prüfen: `application.yaml` < Profil < `PROPS_FILE` < Env < System-Property. Welche Env-Variable trifft `tickle.cooldownMillis`? Gibt es einen injizierbaren Env-Provider für Tests? Verifikation: Ein Wegwerf-Test bzw. ein Lauf mit echter Env ist dokumentiert. Bei Abweichung von der Spec wird die Ausgleichsschicht aus design.md Entscheidung 6 bestätigt.

## 2. Binder (Welle B, test-first)

- [ ] 2.1 **Unit-Tests zuerst**, in `common`, mit einer `Configuration` aus einer Map: fehlender Abschnitt bzw. Einzelwert ergibt den Standardwert, verschachtelter Record, Liste von Strings, Map von Records, falscher Typ aus einem Override (Modul und Feld in der Meldung), ungültiger Wert aus dem Compact Constructor, Warnung bei unbekannten Schlüsseln einmal pro Abschnitt (per abgefangenem Appender). Verifikation: Die Tests sind rot, bevor `ConfigSections` existiert.
- [ ] 2.2 Den Binding-Kern aus `ConfigStore.section` in das package-private `SectionBinder` herausziehen (verhaltensgleich, die bestehenden Tests ziehen mit um). `ConfigSections` baut aus der `Configuration` den `JsonObject`-Baum und delegiert an den `SectionBinder`. Verifikation: 2.1 ist grün, die bisherigen Tests des Binding-Kerns sind grün.
- [ ] 2.3 Abhängigkeit `avaje-config` (Version aus 1.1) in den Versionskatalog und nach `common` aufnehmen. Verifikation: `./gradlew :common:build` ist grün.

## 3. Umstellung von app.json (Welle B, test-first)

- [ ] 3.1 **Unit-Tests zuerst** mit `@TempDir`:
  - v1-Fixture (echte alte `app.json`) ergibt `application.yaml` plus `app.json.migrated`, und das Log nennt die verworfenen Schlüssel,
  - v2 ergibt dieselben Werte,
  - sind beide Dateien vorhanden, bleibt `app.json` unverändert, mit einer Warnung,
  - ohne Datei passiert nichts,
  - kaputtes JSON bricht ab, nichts wird umbenannt,
  - Rundlauf: Die geschriebene YAML, gelesen durch `ConfigSections`, ergibt identische Records.

  Verifikation: Die Tests sind zuerst rot.
- [ ] 3.2 `AppJsonMigration` und das YAML-Schreibwerkzeug (bzw. SnakeYAML `Dump`, falls laut 1.2 vorhanden) umsetzen. Die Abbildung v1 → v2 aus `LegacyConfigMigration` wiederverwenden. Verifikation: 3.1 ist grün.

## 4. Verdrahtung (Welle C)

- [ ] 4.1 **Integrationstest zuerst**, Rangfolge mit echten Dateien in `@TempDir`: Basis, Profil `dev`, `PROPS_FILE`, System-Property und Env (per Provider aus 1.3, sonst in 5.1). Verifikation: Er deckt die Spec-Szenarien „Profil überschreibt“, „Profil ohne Datei“, „Env schlägt Datei“, „System-Property schlägt Env“ und „Erster Start ohne Datei“ ab (keine Datei entsteht).
- [ ] 4.2 In `PlatformBeans`: zuerst `AppJsonMigration` ausführen, dann den `Configuration`-Bean bauen (bei Bedarf mit der Ausgleichsschicht für Overrides) und den `ConfigSections`-Bean. `ModuleRegistry`/`ModuleContext.config` auf `ConfigSections` umstellen, `ConfigStore` samt Speicherlogik und Tests entfernen. Eine INFO-Zeile `Active configuration profiles: {}`, die per abgefangenem Appender getestet wird. Verifikation: Alle Modul-Tests sind grün, der `ModuleWiringTest` ist grün.
- [ ] 4.3 `NavigatorConfig` auf `Map<String, Entry>` umstellen (Namen `elytrarace`, `survival`, `slender`, `creative`, Standardwerte inhaltlich gleich). **Test zuerst:** „Profil ändert ein einzelnes Ziel“ und „unbekannte Flag in application.yaml bricht ab“. Verifikation: Die Navigator-Tests und die Feature-Flag-Tests sind grün.
- [ ] 4.4 Die Repo-Datei `app.json` durch `app/src/dist/application.example.yaml` (kommentiert, mit allen Abschnitten und Standardwerten) ersetzen, und prüfen, dass sie über `ConfigSections` fehlerfrei geladen wird. Verifikation: Ein Unit-Test lädt die Beispiel-Datei, ohne Warnung zu unbekannten Schlüsseln.
- [ ] 4.5 Setup-Server: `AppCommand`, `SetupConfigEditor`, `*SectionConfig` und den `app`-Zweig von `SetupCommand` samt Tests entfernen. Die Simulationsdistanz wird über `Configuration`/`ConfigSections` gelesen (Standardwert 2), **Unit-Test zuerst**. Verifikation: `./gradlew :setup:build` ist grün, und `grep -rn "SetupConfigEditor\|ConfigStore" setup/` findet nichts.

## 5. Doku und Abnahme (Welle D)

- [ ] 5.1 **E2E-Smoke-Test** mit dem Shaded-Jar in Scratch-Verzeichnissen:
  - (a) alte flache `app.json`: Umstellung, `application.yaml` entsteht, `app.json.migrated` auch, WARN-Zeilen im Log, sauberer Start,
  - (b) `application.yaml` plus `application-dev.yaml` mit `AVAJE_PROFILES=dev` und einer echten Env-Variable für `spawn.simulationDistance`: Die Log-Zeile nennt das Profil, der Wert greift,
  - (c) kaputte `application.yaml`: Abbruch mit Datei und Stelle des Fehlers.

  Verifikation: Die wichtigsten Log-Zeilen stehen im PR.
- [ ] 5.2 README und `docs/lobby-modules.md` anpassen: Konfiguration mit `application.yaml`, Profilen, Rangfolge, einer Tabelle der Env-Variablen, der Umstellung von `app.json` samt Rollback-Hinweis, und dem Wegfall von `/setup app`. Verifikation: Ein Review-Agent prüft alle Schlüssel- und Env-Namen gegen den Code.
- [ ] 5.3 Lokale Abnahme im Client durch den Maintainer: Die Lobby verhält sich mit der umgestellten Config unverändert, und ein Wert aus einem `dev`-Profil greift. Verifikation: Die Checkliste im PR ist abgehakt.

## 6. Pull Request

- [ ] 6.1 Den Pull Request `feat(config)!: load lobby configuration from application.yaml with profiles` von `feat/standardized-config-profiles` nach `main` öffnen. Die Beschreibung enthält den `BREAKING CHANGE`-Footer aus proposal.md, die Migrations- und Rollback-Hinweise aus design.md und die Checkliste zur Abnahme. Verifikation: Der PR existiert, die CI ist grün.
