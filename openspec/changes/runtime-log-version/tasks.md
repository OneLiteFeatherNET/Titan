# Tasks

Jeder Schritt: Test zuerst (rot), dann Code (grün). Ein Commit pro Typ.

## 1. Build-Info

- [x] 1.1 Test zuerst (Unit, `TitanBuildTest`): Fehlende oder leere Version ergibt `dev`, fehlender oder leerer Commit ergibt `unknown`; vorhandene Werte bleiben unverändert. Rot, dann `TitanBuild`.
- [x] 1.2 Test zuerst (Unit, `TitanBuildTest`): Die Build-Info liest `Implementation-Version` und `Git-Commit` aus einem Manifest. Ohne Manifest (Klassenverzeichnis) ergibt sich `dev` und `unknown`. Rot, dann Manifest-Lesung.
- [x] 1.3 Review-Härtung: Jar-Lesung über `fromLocation(Path)` getestet (Jar mit Manifest, Jar ohne Manifest, fehlendes Jar, Klassenverzeichnis, Klasse ohne Code-Source); jeder Fehler fällt auf `dev`/`unknown` zurück. `TitanBuild` liegt in `common`, damit `TitanObservability` dieselbe Version nutzt.

## 2. Start-Log

- [x] 2.1 Test zuerst (Unit, `StartupVersionLogTest`, Logback-`ListAppender`): genau eine INFO-Zeile mit Template `Titan {} (commit {}) starting` und den beiden Werten als Argumenten. Rot, dann `StartupVersionLog`.
- [x] 2.2 `TitanApplication.main` ruft das Start-Log direkt nach `TitanObservability.bootstrap()` auf.
- [x] 2.3 Der `ListAppender` wird in `@BeforeEach` angehängt und in `@AfterEach` gestoppt und abgehängt.

## 3. Gradle

- [x] 3.1 `titan.app-variant` (`titan-cloudnet`, `titan-local`) und `:setup` (`setup-titan`): `Git-Commit` aus einem `ValueSource` (kurzer HEAD-Hash, `unknown` bei Fehlschlag) in das Manifest der geschatteten Anwendungs-Jars. `titan.java-conventions` setzt nur noch `Implementation-Version`.
- [x] 3.2 Lokal prüfen: `unzip -p apps/cloudnet/build/libs/titan-cloudnet.jar META-INF/MANIFEST.MF` zeigt `Implementation-Version` und `Git-Commit`; `runtime`-Jar ohne `Git-Commit`.
- [x] 3.3 `GitCommitValueSource` führt Git nur aus, wenn das Projektwurzel-Verzeichnis eine `.git`-Angabe hat, und begrenzt den Prozess auf zehn Sekunden mit `destroyForcibly()` in jedem Pfad.

## 4. Abnahme

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.

## 5. Pull Request

- [ ] 5.1 Pull Request mit dem Titel `feat(runtime): log the titan version and commit at startup` nach `main` öffnen, englische Beschreibung, CI abwarten.
