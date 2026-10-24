# Proposal

## Why

Titans Wartbarkeit hängt heute an Konvention. Die Module sind sauber getrennt, aber nichts im Build verhindert eine Abweichung: Eine Column könnte im Build `project(":features:navigator")` deklarieren, ohne dass etwas bricht. Die Regeln, die es gibt, sind über 16 Column-Tests verteilt, und ein neues Modul bekommt keinen Architekturtest, wenn niemand daran denkt. Statische Analyse, Größengrenzen und Javadoc der öffentlichen API fehlen ganz.

Die Messung am Stand von `origin/main` (siehe `design.md`, "Ist-Stand") zeigt: Die meisten Regeln werden heute schon eingehalten (0 Verstöße), aber nur durch Disziplin. Wartbarkeit soll daher als Regel gelten, die der Build prüft, nicht als Hinweis im README.

## What Changes

- **Modulgrenzen im Build:** Eine Erlaubnisliste je Modulart (Column, Plattform, `common`). Abweichungen brechen die Konfiguration ab (Gradle-Prüfung im Convention-Plugin).
- **Gemeinsame Architekturregeln:** Die bestehenden Column-Regeln in `ColumnArchitectureRules` werden um Projektregeln ergänzt (Logger, Konsolenausgabe, Reflexion, Systemuhr, `Thread.sleep` in Tests). Jedes Modul mit Produktionscode bekommt einen Architekturtest; ein Guard erzwingt das.
- **Paketzyklen:** ArchUnit-Regel mit eingebauter Freeze-Funktion.
- **Statische Analyse:** PMD (Gradle-Plugin, bereits in Gradle enthalten) mit einer kleinen, versionierten Regelmenge für Komplexität, Größe, Logger, Konsolenausgabe, ungenutzten Code und Fehlerbehandlung. Formatierung bleibt bei Spotless.
- **Größengrenzen:** Methoden höchstens 15 zyklomatische Komplexität und 60 NCSS, Klassen höchstens 500 NCSS. Startwerte, nach Messung zu bestätigen.
- **Baseline statt Blockade:** Bestandsverstöße stehen in einer versionierten Datei je Modul, Datei und Regel. Neue Verstöße brechen den Build, die Baseline wächst nur mit Begründung und Freigabe durch Maintainer.
- **Ausnahmen:** Nur im Code und begründet (`@SuppressWarnings("PMD.<Regel>")` mit Kommentar, ArchUnit-Ausnahme mit `because`), im Review geprüft.
- **Javadoc der öffentlichen API in `core`:** `:core:javadoc` mit doclint ohne Warnungen; 3 Bestandslücken in der Baseline.
- **Column-Beschreibung:** Jede Column beschreibt sich in `package-info.java`. Keine README je Modul.
Nicht Teil dieser Änderung: Testpyramide und CI-Testauswahl (eigener Change "module-based tests", der parallel entsteht), Coverage-Schwellen (JaCoCo bleibt beim Bericht), Error Prone, NullAway, Checkstyle, SonarQube, Änderungen an Produktivcode außer den Stellen, die die Baseline ausdrücklich führt. Ungenutzte Abhängigkeiten (Dependency-Analysis-Plugin, SOLLTE) folgen als eigener Change, nachdem ein Dry-Run den Bestand gemessen hat (`design.md`, D7).

## Capabilities

### New Capabilities
- `maintainability`: Die Wartbarkeitsregeln, die der Build prüft (Modulgrenzen, Architektur- und Code-Regeln, Größen, Javadoc der API, Baseline und Ausnahmen).

### Modified Capabilities

Keine. Die Anforderung "Module sind voneinander unabhängig" in `lobby-modules` bleibt unverändert; diese Änderung erzwingt sie nur im Build.

## Impact

- **Code:** `buildSrc` (Erlaubnisliste, PMD-Ratchet, Guards; Tests in `buildSrc/src/test`), `titan.java-conventions`, `titan.column`, `core/build.gradle.kts` (Javadoc), `core/src/testFixtures` (gemeinsame Regeln), je Modul ein Architekturtest, `config/quality/` (PMD-Regeln, Baselines).
- **Dependencies:** PMD (Version im Versionskatalog gepinnt; Gradle bringt das Plugin mit). ArchUnit ist bereits vorhanden. Kein Dependency-Analysis-Plugin in dieser Änderung.
- **Betrieb:** Keine. Keine Laufzeitänderung, keine Konfiguration.
- **Nutzertexte:** keine.
- **CI:** Die Gates laufen im bestehenden `build`-Lauf (`build` hängt an `check`), also auf allen drei Runnern des Matrix-Jobs. Ein Docker-Bedarf entsteht nicht.

## Delivery

Pull-Request-Titel und Squash-Commit: `build(quality): enforce maintainability rules in the build`

Ein Typ: Die Gates sind Build-Konfiguration und Architekturtests (einschließlich ArchUnit), die dieselbe Prüfung ausführen; kein Verhalten der Lobby ändert sich. Der Typ `build` erscheint nach release-please-Standard nicht im Changelog (in `CHANGELOG.md` gibt es dafür keinen Eintrag, z. B. für `build: run module tests in parallel`) und löst keinen Versionssprung aus. Diese Änderung behebt keine Bestandsverstöße, sie stehen in der Baseline. Die einzige Ausnahme sind die Javadoc-Lücken der drei öffentlichen `core`-Typen: Sie kommen als eigener `docs(core)`-Commit im selben PR, weil sie vor der Aktivierung des Javadoc-Gates geschlossen sein müssen.

## Open Questions

Alle Open Questions aus `design.md` sind entschieden (Abschnitt "Open Questions"). Blockierend waren die Erlaubnis von `persistence` für Columns (erlaubt) und die Aufteilung der ArchUnit-Regeln (ein Change `build(quality)`).
