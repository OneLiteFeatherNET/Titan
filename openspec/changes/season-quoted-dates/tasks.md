# Tasks

## Execution Plan

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | archive-first | 1.1 | sonnet | `openspec/**` | Produktionscode, Tests, Doku |
| 2 | season-fix | 2.1–2.4, 3.1–3.3 | sonnet | `features/season/**`, `README.md`, `docs/lobby-modules.md` | `core/**`, `common/**`, `runtime/**`, `apps/**`, andere `features/**`, `buildSrc/**` |
| 3 | verify | 4.1 | haiku | read-only | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Code |

## 1. Voraussetzung

- [x] 1.1 Prüfen, ob `openspec/specs/lobby-seasons/spec.md` auf `main` liegt; falls nicht, zuerst `seasonal-lobby-world` archivieren (`/opsx:archive seasonal-lobby-world`), sonst lässt sich das `MODIFIED`-Delta dieses Changes beim Archivieren nicht anwenden. Nachweis: `openspec list --specs` zeigt `lobby-seasons`.

## 2. Regressionstest und Meldung (unit)

- [ ] 2.1 `testRuntimeOnly(libs.snakeyaml)` in `features/season/build.gradle.kts` ergänzen; Nachweis: `./gradlew :features:season:test` bleibt grün (bestehende Tests unverändert)
- [ ] 2.2 Test zuerst (unit, F.I.R.S.T.): YAML-Datei im `@TempDir` schreiben, per `Configuration.builder().load(...)` laden und mit einem `SeasonConfigReader` lesen, der die `Configuration` übergeben bekommt; quotiert: Saison gelesen; unquotiert: `readAtStartup()` wirft und nennt `seasons.<id>.from` samt Quoting-Hinweis; `readLive()` ergibt leer und loggt den Hinweis einmal (aufgefangener `ListAppender`, kein Sleep, keine Systemzeit, globaler `Config`-Zustand unverändert); rot
- [ ] 2.3 `SeasonConfigReader` nimmt eine `Configuration` per Konstruktor (Produktion: `Config.asConfiguration()`), `SeasonSchedule` reicht sie durch; bestehende `SeasonConfigReaderTest`/`SeasonModuleTest` bleiben grün
- [ ] 2.4 Meldung für fehlendes `from`/`to` um `(quote date-times: from: "2026-12-01T00:00:00")` erweitern (nicht für `world`); Test aus 2.2 grün, `./gradlew :features:season:build` läuft durch

## 3. Doku und Vorlage

- [ ] 3.1 `README.md` (Abschnitt „Seasonal lobby world“): `from`/`to` quotiert zeigen und einen Satz ergänzen, dass Datum-Zeit-Werte in Anführungszeichen stehen müssen
- [ ] 3.2 `docs/lobby-modules.md` (Abschnitt „Saison-Welt“): dasselbe auf Deutsch
- [ ] 3.3 Kommentar in `features/season/src/main/resources/titan/defaults/season.yaml` mit quotierten Beispielen; Nachweis: `grep -rn "from: 20" README.md docs features/season/src/main` findet nur quotierte Werte, `./gradlew build` läuft durch, Commits je Typ (`fix(season): ...`)

## 4. Abnahme

- [ ] 4.1 Haiku-Review (read-only): Szenarien von `lobby-seasons` (quotiert, unquotiert, Laufzeit) dem jeweiligen Test zuordnen; F.I.R.S.T.-Check (`@TempDir`, keine Sleeps, keine Systemzeit, `Config` nicht verändert, Reihenfolge egal). Nachweis: Bericht ohne Lücken

## 5. Pull Request

- [ ] 5.1 Pull Request mit dem Titel `fix(season): explain that season date-times must be quoted` eröffnen (Titel und Beschreibung Englisch); Archiv-Commit `docs(openspec): archive season-quoted-dates`, falls das Archiv nicht mitgeliefert wird
