# Tasks

## Execution Plan

Integrationszweig: `feat/navigator-build-destination` von `origin/main`. Jede Welle endet mit grünem `./gradlew build` und geprüften Diffs. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | navigator | 1.1–1.2, 2.1–2.6 | sonnet | `features/navigator/**` | `core/**`, `runtime/**`, `platform/**`, `apps/**`, `buildSrc/**`, andere `features/**` |
| 1 | docs | 3.1 | sonnet | `docs/lobby-modules.md` | Code, `features/**`, `openspec/**` |
| 2 | verify | 3.2 | haiku | read-only | alles |
| 3 | pr | 4.1 | sonnet | Git/GitHub | Code |

Der Agent `navigator` arbeitet in einem Modul und in dieser Reihenfolge, weil `Destination`, `NavigatorModule` und die Fixtures sich gegenseitig bedingen. Jeder Agent-Prompt nennt die Regeln, die für seinen Task gelten: erst Vorhandenes nutzen (`PermissionService` aus `core`, Aves' `GlobalInventoryBuilder`, `Deliver`), Java 25 ohne Preview, Nutzertext wie die übrigen Ziele als MiniMessage-Literal (Abweichung siehe design.md D5), keine neuen Logs, Metriken oder Spans, Test zuerst, schlanke Kommentare nur fürs Warum, Conventional Commits `feat(navigator): …`. Kein `io.avaje.config` im Navigator (`ColumnArchitectureTest`). F.I.R.S.T.: je Test eine frische `FakePermissionService`, `FakeFeatureFlags` und `NavigatorFixture` (kein geteilter statischer Zustand), `RecordingDeliver` für Weiterleitungen, `env.tick()` statt Warten, kein `Thread.sleep`, keine Systemzeit, Erfolg nur über Assertions mit Meldung.

## 1. Grundlage: Dienst, Ziel und Fixtures (Welle 1)

- [x] 1.1 Test zuerst (Unit, `NavigatorDestinationTest`): `BUILD` liegt auf Platz 7, hat den Task `Build`, kein Feature-Flag und das Recht `titan.navigator.buildserver`; Plätze bleiben paarweise verschieden; `visible(flags, false)` enthält `BUILD` nie, `visible(flags, true)` enthält es; das bestehende „alle Ziele bei Flag an“ gilt nun für `visible(flags, true)`; rot. Dann `Destination` um Recht und `BUILD` erweitern; grün. Nachweis: `./gradlew :features:navigator:test` grün.
- [x] 1.2 Test-Fixtures ohne Produktionsänderung vorbereiten: `FakePermissionService` (Ergebnis je UUID einstellbar, Standard `NOT_SET`, frisch je Test) anlegen und `NavigatorFixture.start(env, deliver, flags, permissions)` um den vierten Parameter erweitern; alle bestehenden Aufrufer bekommen eine `FakePermissionService` ohne Rechte. Nachweis: Testquellen kompilieren, sobald 2.3 den Konstruktor liefert (bis dahin bewusst rot).

## 2. Zwei Inventare und Rechteprüfung (Welle 1)

- [x] 2.1 Charakterisierung zuerst (Integration, Cyano-`Env`): Die bestehenden Tests `NavigatorModuleTest`, `NavigatorFeatureFlagTest` und `NavigatorModuleLeakTest` pinnen das heutige Menü ohne Recht (Plätze 0, 4, 5, 8, Glas auf 1, 2, 3, 6, 7; Klicks; Flag-Wechsel; Listener-Zahl). Nachweis: alle grün auf dem unveränderten Stand von `origin/main`, bevor 2.3 beginnt.
- [x] 2.2 Test zuerst (Integration, erster Test dieser Gruppe): Zwei angemeldete `GlobalInventoryBuilder` ordnen Klicks je ihrem eigenen Inventar zu und stören sich nicht (D2). Schlägt das fehl, Umsetzung anhalten und dem Nutzer melden. Nachweis: Test grün oder Meldung an den Nutzer.
- [x] 2.3 Tests zuerst (Integration, `NavigatorBuildDestinationTest` oder Erweiterung von `NavigatorModuleTest`), je ein Verhalten pro Test: Recht erteilt → Platz 7 zeigt `SCAFFOLDING`, übrige Plätze wie öffentlich; `NOT_SET` → Glas auf Platz 7, Menü gleich dem öffentlichen; `DENIED` → wie `NOT_SET`; Klick mit Recht → `RecordingDeliver` hält genau eine Weiterleitung an `Build`, Klick abgebrochen, Inventar geschlossen; Recht zwischen Öffnen und Klick entzogen → keine Weiterleitung, Inventar geschlossen, Klick abgebrochen, kein Chat; Klick auf Glas auf Platz 7 im öffentlichen Menü → nichts; rot. Dann `NavigatorModule` umsetzen: `PermissionService` im Konstruktor, Hülle `SharedNavigator` je Inventar (Builder, Flag `withPermissioned`, `appliedVisible`, `synchronized` je Hülle), beide in `start()` angemeldet und in `stop()` abgemeldet, Wahl des Inventars beim Öffnen, erneute Prüfung im Klick-Handler eines Ziels mit Recht; grün. Nachweis: `./gradlew :features:navigator:test` grün.
- [x] 2.4 Test zuerst (Integration): Flag-Wechsel wirkt beim nächsten Öffnen in beiden Menüs (Slender an/aus mit und ohne Recht); nach `stopModule()` reagiert keines der beiden Inventare mehr (kein Abbruch des Klicks, keine Weiterleitung). Nachweis: Tests grün.
- [x] 2.5 `NavigatorModuleLeakTest` erweitern (Integration): Listener-Zahl am Modulknoten und an den `eventNode()`s **beider** geteilter Inventare bleibt über 50 Öffnungen abwechselnd mit und ohne Recht sowie über 100 Spieler unverändert; das Test-Zugangsmittel `sharedInventory()` wird zu einem Zugang je Inventar. Nachweis: Tests grün, Meldungen nennen das jeweilige Inventar.
- [x] 2.6 `package-info.java`: `PermissionService.class` in `requires` ergänzen; `ColumnArchitectureTest` bleibt unverändert. Nachweis: `./gradlew build` grün, einschließlich der Verdrahtungstests in `apps/cloudnet` und des Starttests von `apps/local` (kein Wiring-Fehler, `local` zeigt `deny-all`).

## 3. Doku und Abnahme

- [x] 3.1 `docs/lobby-modules.md`: Tabellenzeile `navigator` um `PermissionService.class` ergänzen und im Abschnitt zum Navigator (Aves-Inventar) beschreiben, dass es zwei geteilte Inventare gibt, das Recht `titan.navigator.buildserver`, den Task `Build` und den Betriebsschritt (CloudNet-Task, LuckPerms-Recht). Nachweis: Doku nennt Recht, Task, Platz 7 und beide Inventare; kein anderer Abschnitt geändert.
- [x] 3.2 Verifikation (Haiku, read-only): Jedes Szenario aus `specs/lobby-navigator` (ADDED und MODIFIED) Test für Test zuordnen; F.I.R.S.T.-Check (keine Sleeps, keine Systemzeit, keine geteilten statischen Zustände, frische Fakes je Test, Assertions mit Meldungen); prüfen, dass `Deliver`/`GuardedDeliver`/FeatureGate/`io.avaje.config` nicht angefasst wurden. Nachweis: Bericht ohne Lücken.

## 4. Pull Request

- [ ] 4.1 Pull Request vom Integrationszweig auf `main` unter dem Titel `feat(navigator): show the build server destination to the team` öffnen (Titel und Beschreibung Englisch), mit dem Hinweis, dass er PR #219 ersetzt (Variante: festes Ziel `Build` statt Serverliste, keine Abhängigkeit von #216); danach #219 mit einem Kommentar schließen, der auf den neuen PR verweist (superseded). Nachweis: PR-URL, CI grün, #219 geschlossen.
