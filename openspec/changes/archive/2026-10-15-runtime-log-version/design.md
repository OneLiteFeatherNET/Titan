# Design

## Ansatz

Eine kleine Build-Info-Klasse `TitanBuild` in `common` (Paket `observability`) liest `Implementation-Version` und `Git-Commit` aus dem Manifest des Jars, aus dem sie geladen wurde. Sie liegt in `common`, weil `TitanObservability` (Sentry-Release) dieselbe Version braucht und `common` nicht von `runtime` abhängen kann. Ohne Jar (IDE, Tests, Klassenverzeichnis) und bei jedem Lesefehler liefert sie `dev` und `unknown`; der Start darf daran nie scheitern. Eine Start-Log-Klasse im Paket `bootstrap` schreibt die Zeile, so wie `FeatureStartupLog` und `ConfigurationStartupLog` ihre Zeilen schreiben.

## Commit im Manifest

Der Commit wird im Gradle-Build einmal ermittelt und nur als `Git-Commit` in die ausgelieferten, geschatteten Anwendungs-Jars geschrieben: `titan-cloudnet` und `titan-local` über `titan.app-variant`, `setup-titan` in `:setup`. Alle anderen Jars (Bibliotheken, Extensions, Plattformen) bekommen ihn nicht, damit ein neuer Commit nicht jedes Jar neu baut. Eine Einschränkung kommt vom Shadow-Plugin: Sein `shadowJar`-Manifest hüllt das Manifest-Objekt des `jar`-Tasks desselben Projekts ein. Deshalb trägt auch das ungeschattete Zwischen-Jar (`*-unshaded.jar`) der drei App-Projekte den Commit. Es wird nicht ausgeliefert, und es sind nur drei Jars statt rund 22. `Implementation-Version` bleibt in `titan.java-conventions` auf allen Jars.

Die Ermittlung ist ein `ValueSource`, lazy über `orElse("unknown")` und damit erst beim Schreiben des Jars ausgeführt. Git läuft nur, wenn das Projektwurzel-Verzeichnis selbst eine `.git`-Angabe hat (Verzeichnis, oder Datei bei einem Worktree). Ohne sie würde Git in ein umgebendes Repository hochlaufen, und ein Quellbaum, der in einem fremden Checkout liegt, bekäme dessen HEAD. Der Git-Prozess hat ein Zeitlimit von 10 Sekunden und wird in jedem Pfad beendet, damit ein hängender Git den Build nicht blockiert. Jeder Fehlschlag ergibt `unknown`.

## Nicht im Umfang

- Keine OTel-Resource-Attribute.
- Keine Versions-Anzeige im Spiel oder Befehl.
