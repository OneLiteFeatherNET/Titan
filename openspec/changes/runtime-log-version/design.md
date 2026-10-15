# Design

## Ansatz

Eine kleine Build-Info-Klasse liest `Implementation-Version` und `Git-Commit` aus dem Manifest des Jars, aus dem `TitanApplication` geladen wurde. Ohne Manifest (IDE, Tests) liefert sie `dev` und `unknown`. Eine Start-Log-Klasse im Paket `bootstrap` schreibt die Zeile, so wie `FeatureStartupLog` und `ConfigurationStartupLog` ihre Zeilen schreiben.

## Commit im Manifest

Der Commit wird im Gradle-Build einmal ermittelt und als Manifest-Attribut in jedes Jar geschrieben. Er kommt aus einem `ValueSource`, damit der Build auch ohne Git oder ohne `.git` (Quelltarball) läuft und ein Fehlschlag nur `unknown` ergibt. Die Ermittlung steht neben dem bestehenden `Implementation-Version`-Eintrag in `titan.java-conventions`.

## Nicht im Umfang

- Keine OTel-Resource-Attribute.
- Keine Versions-Anzeige im Spiel oder Befehl.
