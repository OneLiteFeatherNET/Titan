## ADDED Requirements

### Requirement: Start-Log nennt Version und Build-Commit
Beim Start MUSS Titan genau eine INFO-Zeile `Titan <version> (commit <commit>) starting` loggen. Die Version MUSS aus dem Manifest-Attribut `Implementation-Version` des geladenen Jars kommen, der Commit aus dem Manifest-Attribut `Git-Commit`. Fehlt `Implementation-Version`, MUSS die Version `dev` sein; fehlt `Git-Commit`, MUSS der Commit `unknown` sein.

#### Scenario: Ausgeliefertes Jar
- **WHEN** Titan aus einem Jar mit `Implementation-Version: 3.2.1` und `Git-Commit: 99a7f62` startet
- **THEN** loggt Titan genau eine INFO-Zeile `Titan 3.2.1 (commit 99a7f62) starting`

#### Scenario: Start ohne Manifest
- **WHEN** Titan aus einem Klassenverzeichnis ohne Manifest startet, etwa in der IDE oder in Tests
- **THEN** loggt Titan `Titan dev (commit unknown) starting`

### Requirement: Build schreibt den Commit ins Manifest
Der Gradle-Build MUSS `Git-Commit` mit dem kurzen Git-Hash in das Manifest jedes Jars schreiben. Ist Git nicht verfügbar oder kein `.git`-Verzeichnis vorhanden, DARF der Build nicht fehlschlagen und MUSS `unknown` schreiben.

#### Scenario: Build ohne Git
- **WHEN** der Build ohne Git oder ohne `.git`-Verzeichnis läuft
- **THEN** wird das Jar gebaut, und sein Manifest enthält `Git-Commit: unknown`
