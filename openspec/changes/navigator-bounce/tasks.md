# Tasks

Zweig: `feat/navigator-bounce` von `origin/main`. Vor dem Abhaken einer Aufgabe läuft `./gradlew build` (bzw. der Modultest `./gradlew :features:navigator:test` für die Modul-Aufgaben). Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, Env-Ticks explizit.

## Ausführungsplan

| Welle | Agent | Aufgaben | Modell | darf ändern | darf nicht ändern |
| --- | --- | --- | --- | --- | --- |
| 1 | Test-Agent (Worktree) | 1.1 bis 1.4 | haiku, effort medium | `features/navigator/src/test/**` | `features/navigator/src/main/**`, `README.md`, `docs/**` |
| 2 | Umsetzungs-Agent (Worktree, nach Welle 1) | 2.1 bis 2.4 | haiku, effort medium | `features/navigator/src/main/java/**`, Tests aus 2.4 | `README.md`, `docs/**`, `runtime`, `apps/cloudnet`, `buildSrc` |
| 3 | Doku-Agent (Worktree, nach Welle 2) | 3.1, 3.2 | haiku, effort low | `README.md`, `docs/lobby-modules.md` | jeder Java-Code |
| 4 | Hauptsitzung | 4.1 bis 4.3 | - | - | - |
| 5 | Hauptsitzung | 5.1 | - | - | - |

Welle 1 bis 3 laufen nacheinander, weil sie dieselben Navigator-Dateien berühren. Die Hauptsitzung prüft jeden Diff und jedes Testergebnis, bevor die nächste Welle startet.

## 1. Tests zuerst (rot)

- [x] 1.1 Unit (`NavigatorDestinationTest`): `BOUNCE` liegt auf Platz 13, Icon `SLIME_BALL`, Aufgabe `Bounce`, Recht `titan.navigator.bounce`; der erste Buchstabe des Verlaufs ist `#00FA9A`, der letzte genau `#40E0D0`; `permissioned()` enthält Slender, Build und Bounce; `visible(Set.of())` enthält Bounce nicht, `visible(Set.of(BOUNCE))` enthält ihn. Nachweis: die Tests schlagen auf dem Stand vor 2.1 fehl.
- [x] 1.2 Unit (`ServerPickerTest`, `NavigatorClickHintTest`): `ServerPicker.DESTINATIONS` enthält `BOUNCE`; die Lore von Bounce zeigt „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“ und ohne Portal keine Shift-Zeile. Nachweis: rot vor 2.2.
- [x] 1.3 Integration (`NavigatorPermissionTest`, Env mit `FakePermissionService`, Ticks explizit): `ALLOWED` zeigt Bounce auf Platz 13, und der Linksklick übergibt an die beste bereite Bounce-Runde; `DENIED` und `NOT_SET` zeigen auf Platz 13 eine graue Glasscheibe und lösen beim Klick keine Weiterleitung und keine Chat-Nachricht aus; Recht zwischen zwei Öffnungen erteilt; Recht zwischen Öffnen und Klick entzogen; Start legt sechzehn Inventare an (acht je Sprache). Nachweis: rot vor 2.3.
- [x] 1.4 Unit/Integration (`NavigatorLayoutTest`, `NavigatorRenderedLayoutTest`): gerendertes Menü mit Bounce-Recht zeigt Bounce auf Platz 13 zwischen ElytraRace (11) und Slender (15); ohne Recht ist Platz 13 eine graue Glasscheibe und der Rahmen unverändert. Nachweis: rot vor 2.3.

## 2. Umsetzung (grün)

- [x] 2.1 `Destination`: Eintrag `BOUNCE(13, Material.SLIME_BALL, "<gradient:#00FA9A:#40E0D0>", "Bounce", "Bounce", "titan.navigator.bounce")`; die Klassen-Javadoc nennt Reihe zwei für die Minispiele und die drei gebundenen Ziele. Prüfung: 1.1 grün.
- [x] 2.2 `ServerPicker.DESTINATIONS` um `Destination.BOUNCE` erweitern. Prüfung: 1.2 grün.
- [x] 2.3 `NavigatorModule` und `SharedNavigator`: die Teilmengen-Schleife bleibt unverändert und erzeugt sechzehn Inventare; die Javadoc von `NavigatorModule` nennt Bounce neben Slender und Build. Keine neue Klasse, kein Listener, kein Lazy-Cache. Prüfung: 1.3 und 1.4 grün, `NavigatorModuleLeakTest` grün.
- [x] 2.4 Refactor: die Tests mit festen Slot-Listen (`NavigatorModuleTest` Zeile 75, `NavigatorLayoutTest` Zeile 37) an den Bounce-Slot anpassen, ohne die Aussage für das öffentliche Menü zu ändern; alle Modultests grün (`./gradlew :features:navigator:test`).

## 3. Doku

- [x] 3.1 `README.md`, Abschnitt zum Navigator: Bounce mit Slot 13, Recht `titan.navigator.bounce`, die Betreiber-Schritte (CloudNet-Task `Bounce`, Recht in LuckPerms den Gruppen erteilen), sechzehn Inventare je Sprache mit acht Rechte-Kombinationen. Prüfung: `grep -n "titan.navigator.bounce" README.md` trifft den Abschnitt.
- [x] 3.2 `docs/lobby-modules.md`, Navigator-Abschnitt: Liste der gebundenen Ziele (Slender, Build, Bounce), Liste der Rechte, Varianten (acht je Sprache, sechzehn gesamt), Task `Bounce`. Prüfung: `grep -n "titan.navigator.bounce" docs/lobby-modules.md` trifft den Abschnitt, und die Zahl der Varianten stimmt mit dem Spec-Requirement überein.

## 4. Verifikation

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` sind grün; der Build ist die Grundlage für das Abhaken aller Aufgaben in 1 bis 3.
- [ ] 4.2 Manuelle Abnahme auf einem lokalen Testserver: mit LuckPerms das Recht `titan.navigator.bounce` für einen Testspieler setzen, den Navigator öffnen und prüfen, dass Bounce auf Platz 13 steht, Links- und Rechtsklick wie beschrieben wirken und ohne Recht der Platz grau bleibt. Das lokale Jar wird aus einer Kopie gebaut, nicht im laufenden Worktree (siehe Betriebsnotiz zu `titan-local.jar`).
- [ ] 4.3 Commits je Typ: Tests und Umsetzung als `feat(navigator): add bounce behind a permission`, Doku als `docs(navigator): document the bounce permission`. Jeder Commit hat einen Typ und endet mit der Zeile `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Das Archiv der Change folgt nach dem Merge als `docs(openspec): archive navigator-bounce`.

## 5. Pull Request

- [ ] 5.1 Pull Request auf `main` mit dem Titel `feat(navigator): add bounce behind a permission`. Beschreibung auf Englisch: Zweck (Bounce hinter `titan.navigator.bounce`), Slot 13, Schnellbeitritt und Serverauswahl, sechzehn statt acht Inventare, Betreiber-Schritte (CloudNet-Task `Bounce`, LuckPerms-Recht), Testnachweis (`./gradlew build` grün) und die Schlusszeile `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Nachweis: `gh pr view` zeigt den Titel und die Beschreibung, und die CI läuft.
