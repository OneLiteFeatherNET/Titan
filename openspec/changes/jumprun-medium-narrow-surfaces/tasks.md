# Tasks

## 1. Tests zuerst (rot)

- [x] 1.1 Generator-Test: Medium erzeugt über 12 feste Seeds keine schmale Form (Zaun, Mauer, Scheibe, Gitter, Pfosten, Kopf, Blumentopf, Kerze). Nachweis: Test ist vor dem Fix rot.
- [x] 1.2 Generator-Test: Hard erzeugt über dieselben Seeds alle schmalen Formen. Nachweis: Test ist grün vor und nach dem Fix.
- [x] 1.3 Modus-Test: Medium hat für jede schmale Form keinen Schwellwert und lässt sie bei keinem Score frei. Nachweis: rot vor dem Fix.
- [x] 1.4 Schwierigkeits-Test: Die schwerste freigeschaltete Medium-Form ist bei großem Score eine Stufe (Maximum 6,5). Nachweis: rot vor dem Fix.

## 2. Umsetzung

- [x] 2.1 `Mode`: Medium setzt `narrow` und `narrowest` auf nie (`Params.NEVER`). Hard, Ultra und Rainbow-Parameter bleiben. Nachweis: Tests aus 1.x grün.
- [x] 2.2 Bestehende Tests anpassen, die Medium-Schwellen 25 und 40 oder schmale Formen in Medium erwarten (Surface-, Difficulty-, Course-, Head-, Landing-Tests). Die Tests, die schmale Formen brauchen, laufen in Hard. Nachweis: vollständiger Lauf von `:features:jumprun:test` grün.
- [x] 2.3 Prüfen, ob Dokumentation Schwellen je Modus nennt: `docs/lobby-modules.md` und README beschreiben nur, dass der Modus die Freischaltung steuert; keine Änderung nötig.

## 3. Verifikation

- [x] 3.1 Vorher/Nachher-Anteil schmaler Formen in Medium messen (20 Seeds, 300 Sprünge, Score 40 und 80) und in den PR-Text schreiben.
- [x] 3.2 `./gradlew spotlessApply build --no-daemon -Dorg.gradle.jvmargs=-Xmx2g` grün.
- [x] 3.3 Spec-Delta: jedes Szenario einem Test zugeordnet. Nachweis: Zuordnung im PR-Text.

## 4. Archiv und Pull Request

- [ ] 4.1 `openspec archive jumprun-medium-narrow-surfaces -y`, dann `openspec validate --all` ohne Fehler. Commit `docs(openspec): archive jumprun-medium-narrow-surfaces`.
- [ ] 4.2 Pull Request auf `main` unter dem Titel `fix(jumprun): keep narrow surfaces out of medium courses` öffnen (Titel und Beschreibung Englisch), mit Feedback, Ursache, Fix, Liste der nur-Hard-Formen, Vorher/Nachher-Tabelle und Tests. CI abwarten. Nachweis: PR-URL, CI grün.
