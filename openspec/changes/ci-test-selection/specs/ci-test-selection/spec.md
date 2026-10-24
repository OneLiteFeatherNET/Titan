# Spec Delta

## Purpose

Legt fest, welche Tests der Build auf einem Pull Request, auf `main` und nachts ausführt, und wie lange ein Lauf dauern darf. Die Auswahl folgt den Abhängigkeiten, die Gradle kennt, und führt den vollen Scope nur einmal aus.

## ADDED Requirements

### Requirement: Ein PR-Lauf führt nur die betroffenen Tests aus

Der Build eines Pull Requests MUSS einen Testtask eines Moduls nur dann neu ausführen, wenn sich eine Eingabe dieses Moduls oder eines Moduls geändert hat, von dem es abhängt. Unveränderte Testtasks MÜSSEN ihr Ergebnis aus dem Gradle-Build-Cache übernehmen. Die Auswahl DARF NICHT aus einer manuell gepflegten Modulliste stammen.

#### Scenario: Änderung in einer Column
- **WHEN** ein Pull Request nur Dateien in `features/sit` ändert
- **THEN** laufen die Tests von `features/sit` und die Smoke-Tests der Varianten neu, alle übrigen Testtasks kommen aus dem Cache

#### Scenario: Änderung in core
- **WHEN** ein Pull Request Dateien in `core` ändert
- **THEN** laufen alle Testtasks neu, weil jedes Modul von `core` abhängt

#### Scenario: Neue Column
- **WHEN** ein Pull Request eine neue Column unter `features/` anlegt
- **THEN** wird sie ohne Änderung an CI-Dateien getestet, weil `settings.gradle.kts` sie findet

#### Scenario: Nur Dokumentation
- **WHEN** ein Pull Request nur Markdown- oder OpenSpec-Dateien ändert
- **THEN** wird der Build übersprungen, wie bisher durch den Job „Detect changes“

### Requirement: Build-Logik-Änderungen führen zu einem vollständigen Lauf

Eine Änderung an `buildSrc/`, `gradle/libs.versions.toml`, `settings.gradle.kts` oder an der Wurzel-`build.gradle.kts` MUSS dazu führen, dass alle Testtasks auf allen Betriebssystemen neu laufen. Diese Regel MUSS durch die Eingaben von Gradle entstehen, nicht durch eine Sonderregel im Workflow.

#### Scenario: Versionskatalog geändert
- **WHEN** ein Pull Request eine Version in `gradle/libs.versions.toml` anhebt
- **THEN** laufen alle Testtasks auf allen Betriebssystemen neu

#### Scenario: buildSrc geändert
- **WHEN** ein Pull Request ein Convention-Plugin in `buildSrc/` ändert
- **THEN** laufen alle Testtasks neu

### Requirement: Smoke-Tests laufen einmal je Pull Request auf Ubuntu

Der Pull Request MUSS den Task `smokeTest` genau einmal ausführen, auf Ubuntu. Auf macOS und Windows DARF `smokeTest` NICHT laufen.

#### Scenario: Pull Request mit Ubuntu-Lauf
- **WHEN** ein Pull Request den Build startet
- **THEN** läuft der Smoke-Test im Ubuntu-Job, und der Job für macOS und Windows führt nur den Task `test` aus

### Requirement: macOS und Windows führen im Pull Request den Task test aus

Die Jobs für macOS und Windows MÜSSEN im Pull Request den Task `test` ausführen, also Unit- und Modul-Tests ohne Smoke- und Sweep-Tests. Die Jobs DÜRFEN keine Jar-Artefakte für Smoke-Tests erzeugen.

#### Scenario: Windows-Job im Pull Request
- **WHEN** der Windows-Job eines Pull Requests startet
- **THEN** führt er `test` aus und endet, ohne `smokeTest` oder `sweepTest` zu starten

### Requirement: Main führt den vollen Lauf mit Sweeps aus und schreibt den Cache

Ein Push auf `main` MUSS den Build auf allen drei Betriebssystemen ausführen, einschließlich `sweepTest`. Dieser Lauf MUSS den Gradle-Build-Cache schreiben, aus dem Pull Requests lesen.

#### Scenario: Merge auf main
- **WHEN** ein Pull Request auf `main` gemergt wird
- **THEN** läuft der Build auf Ubuntu, macOS und Windows mit `build` und `sweepTest`, und der nächste Pull Request findet den Cache

### Requirement: Der Nachtlauf prüft ohne Build-Cache

Ein nächtlicher Lauf auf `main` MUSS den Build auf allen drei Betriebssystemen ohne Gradle-Build-Cache ausführen, mit `build` und `sweepTest`. So erkennt er Tests, deren Ergebnis nur durch einen gecachten Stand bestanden wird.

#### Scenario: Nachtlauf findet einen Test, der nur mit Cache besteht
- **WHEN** ein Test auf einem frischen Build ohne Cache fehlschlägt, im Cache-Lauf aber bestanden hat
- **THEN** meldet der Nachtlauf den Fehler, und das Team erfährt ihn vor dem nächsten Release

### Requirement: Laufzeitziele sind gemessen und im Pull Request genannt

Die Laufzeit eines Pull Requests MUSS mit folgenden Zielen gemessen werden: ein Docs-Lauf höchstens 30 Sekunden, ein Feature-Lauf mit warmem Cache höchstens 5 Minuten, ein Lauf mit Änderung in `core` oder `runtime` höchstens 10 Minuten. Jede Messung MUSS aus den Job-Zeiten des Laufs stammen, nicht aus Schätzungen.

#### Scenario: Feature-Lauf mit warmem Cache
- **WHEN** ein Pull Request eine einzelne Column ändert und der Cache von `main` vorliegt
- **THEN** dauert der Lauf auf jedem Betriebssystem höchstens 5 Minuten

#### Scenario: Lauf mit Änderung in core
- **WHEN** ein Pull Request `core` ändert und der Cache nicht greift
- **THEN** dauert der Lauf höchstens 10 Minuten, gemessen am langsamsten Job

#### Scenario: Ziel verfehlt
- **WHEN** ein Lauf ein Ziel verfehlt
- **THEN** nennt der nächste Pull Request die Ursache, und die Abweichung wird in der Beschreibung dokumentiert
