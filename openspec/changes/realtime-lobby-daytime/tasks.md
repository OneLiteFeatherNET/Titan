# Tasks

## Execution Plan

Integrationszweig: `feat/daytime` von `origin/main`. Eine Welle, weil das Modul klein ist. Endet mit grünem `./gradlew build` und geprüftem Diff.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | daytime | 1.1–3.4 | sonnet | `features/daytime/**` | `common/**`, `runtime/**`, `core/**`, andere `features/**`, `docs/**` |
| 2 | verify | 4.1 | haiku | read-only | alles |

## 1. Zeitabbildung (unit)

- [ ] 1.1 Modul `features/daytime` mit `build.gradle.kts` (`titan.column`, `avaje-config`) und `package-info.java` (`@InjectModule`, `requires = {Instance.class, Scheduler.class, Clock.class}` wie bei `tickle`, denn `runtime` stellt nur `Instance` bereit) anlegen; `./gradlew :features:daytime:build` läuft durch
- [ ] 1.2 `DayTimeMappingTest` zuerst (unit): Ankerwerte, beide Berliner Umstellungen, Sonnenwenden, Bereich [0, 24000), Determinismus, kein Rückwärtssprung über das Vorstellen, genau ein Umlauf je 24 h; rot
- [ ] 1.3 `DayTimeMapping` implementieren, `DayTimeMappingTest` grün

## 2. Konfiguration (unit)

- [ ] 2.1 Test zuerst: `daytime.yaml` liefert `enabled=true` und `zone=Europe/Berlin`, ungültige Zone wird abgelehnt (unit, kein Server); rot
- [ ] 2.2 `titan/defaults/daytime.yaml` und die Zonen-Prüfung umsetzen, Test grün

## 3. Modul (integration)

- [ ] 3.1 `DaytimeModuleTest` zuerst (Cyano `Env`, einstellbare `Clock`, `env.tick()`): `rate` ist 0, Zeit folgt der Wanduhr, höchstens ein Setzen je 20 Ticks, `enabled=false` ergibt 6000, Umschalten live, Zonenwechsel live, ungültige Zone beim Start bricht ab, ungültige Zone zur Laufzeit behält die letzte und loggt WARN (aufgefangener Appender), `null`-Uhr loggt WARN; rot
- [ ] 3.2 `DaytimeModule` implementieren (`@Singleton`, `@PostConstruct` mit `rate(0f)` und 20-Tick-Aufgabe samt Sofortlauf, `@PreDestroy` bricht ab), Test grün
- [ ] 3.3 `ColumnArchitectureTest` nach dem Muster von `features/tickle` ergänzen und grün
- [ ] 3.4 `./gradlew build` läuft durch, Commits je Typ (`feat(daytime): ...`), keine Datei außerhalb von `features/daytime/**` geändert

## 4. Abnahme

- [ ] 4.1 Haiku-Review des Diffs gegen `specs/lobby-daytime` und F.I.R.S.T. (keine Sleeps, keine Systemzeit, keine Reihenfolgenabhängigkeit)

## 5. Pull Request

- [ ] 5.1 Pull Request mit dem Titel `feat(daytime): follow the real wall clock in the lobby` eröffnen (Titel und Beschreibung Englisch); nach dessen Merge PR #217 mit einem Kommentar schließen, der auf den neuen PR verweist (superseded); falls das Archiv nicht mitgeliefert wird, Archiv-Commit `docs(openspec): archive realtime-lobby-daytime`
