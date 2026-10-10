# Tasks

## Execution Plan

Zweig: `fix/navigator-dead-flags` von `origin/main`. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | navigator | 1.1–1.3, 2.1–2.2 | sonnet | `features/navigator/**` | `core/**`, `runtime/**`, `platform/**`, `apps/**`, `buildSrc/**`, andere `features/**`, `docs/**`, `README.md` |
| 1 | docs | 3.1 | sonnet | `README.md`, `docs/lobby-modules.md` | Code, `features/**`, `openspec/**` |
| 2 | verify | 3.2 | haiku | read-only | alles |
| 3 | pr | 4.1 | sonnet | Git/GitHub | Code |

Regeln in jedem Agent-Prompt: Test zuerst (rot, dann grün), Java 25 ohne Preview, schlanke Kommentare nur fürs Warum, keine neuen Logs, Metriken oder Spans, kein neuer Nutzertext, kein `io.avaje.config` im Navigator (`ColumnArchitectureTest`), Conventional Commits `fix(navigator): …`. F.I.R.S.T.: je Test frische `FakeFeatureFlags` und eigene `Configuration`-Instanz, kein geteilter statischer Zustand, kein `Thread.sleep`, keine Systemzeit, Erfolg nur über Assertions mit Meldung.

## 1. Tests zuerst und Code (Welle 1, Agent navigator)

- [ ] 1.1 Tests zuerst (Unit, rot): `DefaultNavigatorFeatureFlagsTest` um die Gegenrichtung erweitern (die Schlüssel von `features` in `titan/defaults/navigator.yaml` sind genau die Flags der `Destination`-Werte, also `{NAVIGATOR_SLENDER}`); `NavigatorDestinationTest` prüft am `customName` von `Destination.SLENDER.item()` Buchstabe für Buchstabe, dass der erste `#616161` und der letzte `#e80000` ist. Nachweis: beide Tests schlagen vor der Umsetzung fehl (vier Extra-Flags, letzter Buchstabe `#80000C`).
- [ ] 1.2 Konstante `Destination.SLENDER_FLAG = "NAVIGATOR_SLENDER"` einführen (design.md D1), in `SLENDER` einsetzen und in allen Navigator-Tests statt des Literals verwenden (`NavigatorDestinationTest`, `NavigatorFeatureFlagTest`, `NavigatorModuleTest`, `NavigatorModuleLeakTest`, `NavigatorBuildDestinationTest`). Nachweis: `grep -rn '"NAVIGATOR_SLENDER"' features/navigator` findet nur die Konstante und `navigator.yaml`; `./gradlew :features:navigator:test` grün.
- [ ] 1.3 `#e80000c` zu `#e80000` korrigieren und in `navigator.yaml` die vier Flags `NAVIGATOR_CREATIVE`, `NAVIGATOR_MANIS`, `NAVIGATOR_SURVIVAL`, `NAVIGATOR_ELYTRA` samt Kommentar „complete list“ auf die eine Flag anpassen. Nachweis: die Tests aus 1.1 sind grün, `./gradlew :features:navigator:build` und `:runtime:test` grün (`ConfigFeatureFlagsTest` und `DefaultsMergerTest` unverändert grün).

## 2. Betreiber-Kompatibilität (Welle 1, Agent navigator)

- [ ] 2.1 Test zuerst (Unit, `runtime` ist tabu, deshalb im Navigator): `NavigatorFeatureFlagTest` bekommt einen Fall, in dem die Fake-Flags zusätzlich `NAVIGATOR_CREATIVE` als aktiv führen; das Menü ist identisch mit dem ohne diese Flag (Survival auf Platz 4, Platz 5 nur bei `SLENDER_FLAG`); grün von Anfang an (Charakterisierung des Verhaltens „unbekannte Flag ist wirkungslos“). Nachweis: `./gradlew :features:navigator:test` grün.
- [ ] 2.2 Optional, nur wenn `apps/cloudnet` ohnehin gebaut wird: Rauchprüfung, dass `FEATURES_NAVIGATOR_CREATIVE=true` den Start nicht bricht (bestehender Muster-Test `ConfigFileWatchIntegrationTest`); sonst als manuelle Abnahme in 3.2 vermerken. Nachweis: kein Startfehler, keine WARN-Zeile.

## 3. Doku und Abnahme

- [ ] 3.1 `README.md`: Beispiel `application.yaml` im Abschnitt „Configuration“, der Satz „Only Slender is gated…“, der Abschnitt „Feature flags“ (nur noch `NAVIGATOR_SLENDER`; ein Satz, dass früher ausgelieferte Flags `NAVIGATOR_CREATIVE`, `NAVIGATOR_MANIS`, `NAVIGATOR_SURVIVAL`, `NAVIGATOR_ELYTRA` nie etwas geschaltet haben und wirkungslos sind) und „Local testing with every flag on“ (nur `NAVIGATOR_SLENDER: true` plus `config.watch.enabled: true`, Überschrift entsprechend). `docs/lobby-modules.md`: Abschnitt „Standardwerte je Column“ (`navigator` liefert die Flag `NAVIGATOR_SLENDER` statt „allen `NAVIGATOR_*`-Flags“). Nachweis: `grep -rn 'NAVIGATOR_\(CREATIVE\|MANIS\|SURVIVAL\|ELYTRA\)' README.md docs` findet nur die eine Erklärung.
- [ ] 3.2 Verifikation (Haiku, read-only): Jedes Szenario aus `specs/lobby-navigator` einem Test zuordnen (Standardwerte listen nur Slender: 1.1; entfernte Flag in Datei und per Env: 2.1/2.2; Verlauf: 1.1); F.I.R.S.T.-Check der geänderten Tests; prüfen, dass `core/**`, `runtime/**`, `ConfigFeatureFlags` und `FeatureFlags` nicht angefasst wurden; `./gradlew build` grün. Nachweis: Bericht ohne Lücken.

## 4. Pull Request

- [ ] 4.1 Pull Request von `fix/navigator-dead-flags` auf `main` unter dem Titel `fix(navigator): drop dead feature flags and fix the slender gradient` öffnen (Titel und Beschreibung Englisch), mit Hinweis auf die Betreiber-Kompatibilität (entfernte Flags sind wirkungslos, kein Startfehler) und der offenen Frage, ob das `FeatureFlags`-SPI für eine Flag bleiben soll. Nachweis: PR-URL, CI grün.
