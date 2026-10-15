# Tasks

Jeder Schritt: Test zuerst (rot), dann Code (grün). Ein Commit pro Typ.

## 1. Build-Info

- [ ] 1.1 Test zuerst (Unit, `TitanBuildTest`): Fehlende oder leere Version ergibt `dev`, fehlender oder leerer Commit ergibt `unknown`; vorhandene Werte bleiben unverändert. Rot, dann `TitanBuild`.
- [ ] 1.2 Test zuerst (Unit, `TitanBuildTest`): Die Build-Info liest `Implementation-Version` und `Git-Commit` aus einem Manifest. Ohne Manifest (Klassenverzeichnis) ergibt sich `dev` und `unknown`. Rot, dann Manifest-Lesung.

## 2. Start-Log

- [ ] 2.1 Test zuerst (Unit, `StartupVersionLogTest`, Logback-`ListAppender`): genau eine INFO-Zeile mit Template `Titan {} (commit {}) starting` und den beiden Werten als Argumenten. Rot, dann `StartupVersionLog`.
- [ ] 2.2 `TitanApplication.main` ruft das Start-Log direkt nach `TitanObservability.bootstrap()` auf.

## 3. Gradle

- [ ] 3.1 `titan.java-conventions`: `Git-Commit` aus einem `ValueSource` (kurzer HEAD-Hash, `unknown` bei Fehlschlag) in das Manifest jedes Jars.
- [ ] 3.2 Lokal prüfen: `unzip -p apps/cloudnet/build/libs/titan-cloudnet.jar META-INF/MANIFEST.MF` zeigt `Implementation-Version` und `Git-Commit`.

## 4. Abnahme

- [ ] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.

## 5. Pull Request

- [ ] 5.1 Pull Request mit dem Titel `feat(runtime): log the titan version and commit at startup` nach `main` öffnen, englische Beschreibung, CI abwarten.
