# Tasks

## Execution Plan

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | navigator | 1.1–1.2, 2.1–2.3, 3.1–3.2, 4.1–4.2 | sonnet | `app/src/main/java/**` (navigator, `module/`, `Titan.java`), `app/src/test/java/**`, `app/src/main/resources/application.yaml`, `docs/lobby-modules.md`, `README.md` | `common/**`, `openspec/**`, andere Feature-Module |
| 2 | review | 5.1 | haiku | – (read-only) | alles |
| 2 | pr | 6.1 | sonnet | Git/GitHub | Quellcode |

Ein einzelner Sonnet-Agent in einem Worktree vom aktuellen `origin/main`. Der Change ist klein und die Schritte hängen voneinander ab. Der Agent-Prompt nennt: KISS, eine Produktivklasse plus package-privates enum; built-in first (Aves `GlobalInventoryBuilder`/`InventoryLayout`, Java-`enum`, `Material` statt String); Java-25-Features; keine neuen Logzeilen, Metriken oder Spans; keine neuen Nutzertexte (MiniMessage-Strings unverändert übernehmen, i18n ist Non-Goal); Tests nach F.I.R.S.T.; `./gradlew build` vor jedem Abhaken.

## 1. Verhalten festhalten

- [x] 1.1 Prüfen, dass `NavigatorModuleTest`, `NavigatorFeatureFlagTest` und `NavigatorModuleLeakTest` auf `main` grün sind. Fehlende Integrationstests (Cyano-Env) ergänzen: Weiterleitung für ElytraRace/Survival/Slender/Creative zu „ElytraRace“/„Survival“/„cygnus“/„MemberBuild“, Glasscheiben auf 1/2/3/6/7, Klick auf Platz 2 löst keine Weiterleitung aus und schließt den Navigator nicht. Nachweis: `./gradlew :app:test --tests '*Navigator*'` grün auf dem alten Code.
- [x] 1.2 ArchUnit-Test (rot): Klassen in `..feature.navigator..` hängen nicht von `io.avaje.config..` ab. Er deckt das Szenario „Navigator-Werte in der Konfiguration werden ignoriert“ ohne globalen Config-Zustand ab (F.I.R.S.T.). Nachweis: Der Test schlägt auf dem alten Code fehl.

## 2. Navigator als ein Modul

- [x] 2.1 Unit-Test zuerst (`NavigatorDestinationTest`), dann package-privates `enum Destination` mit den vier Zielen, `Material`, MiniMessage-Namen, Task und Flag (nur Slender) sowie `static List<Destination> visible(FeatureFlags)`. Nachweis: Der Test prüft Plätze 0/4/5/8, paarweise verschiedene Plätze und `visible` bei Slender an/aus mit `FakeFeatureFlags`.
- [x] 2.2 `NavigatorModule` neu schreiben (design.md, Entscheidungen 1 und 3): `GlobalInventoryBuilder` einmal in `enable()`, Layout mit Glasscheiben plus Klick-Handlern für `visible(...)`, `synchronized` Neu-Legen nur bei geänderter Sichtbarkeit beim Öffnen, `unregister()` in `disable()`, keine `Config`-Lesung. Konstruktor nimmt nur `Deliver` und `FeatureFlags`. Nachweis: 1.1-Tests grün, 1.2 wird grün, `NavigatorModuleLeakTest` grün.
- [x] 2.3 `NavigatorInventory`, `NavigatorLayout`, `NavigatorVisibility`, `NavigatorEntryKeys`, `NavigatorEntryValidation` und ihre Tests löschen. `DefaultNavigatorFeatureFlagsTest` auf `Destination` umstellen: Jede `feature` steht in den mitgelieferten `features.*` (Unit). Nachweis: `ls app/src/main/java/**/feature/navigator` zeigt nur `NavigatorModule.java` und `Destination.java`, `./gradlew :app:test` grün.

## 3. Andockpunkt entfernen

- [x] 3.1 Paket `module/navigator` (`NavigatorEntries`, `NavigatorEntry`, `NavigatorConflictException`) samt Tests löschen. `ModulePlatform#navigator()`, `ModuleContext#navigator()` (Feld und Abmeldung), die Navigator-Validierung in `ModuleRegistry#enableAll()` und die Verdrahtung in `Titan.java` entfernen. Nicht mehr genutzte Parameter (z. B. `featureFlags` im Registry-Builder, falls nur dafür) ebenfalls entfernen. Nachweis: `rg "module\.navigator|NavigatorEntries|\.navigator\(\)" app` findet nichts, `./gradlew :app:compileJava` grün.
- [x] 3.2 Tests anpassen: `ModuleHarness#navigator()`, `ModulePlatformFixture`, Navigator-Teile von `ModuleRegistryTest` und `ModuleHarnessTest` entfernen, ohne dass andere Assertions verloren gehen. Nachweis: `./gradlew :app:test` grün (inkl. ArchUnit).

## 4. Konfiguration und Doku

- [x] 4.1 Abschnitt `navigator:` aus `app/src/main/resources/application.yaml` entfernen. Die Kommentare zu `features` und `config.watch` ohne Verweis auf `navigator.entries` bzw. „re-read on every open“ umformulieren. Nachweis: `rg "navigator\.(title|entries)" app/src/main` findet nichts, `./gradlew :app:test` grün.
- [x] 4.2 `docs/lobby-modules.md` (Andockpunkt `navigator` streichen, Navigator als fest verdrahtetes Modul beschreiben, Fehlermeldung zu `navigator.entries` streichen) und `README.md` (Beispiel-Eintrag) anpassen. Danach `./gradlew build` (Spotless, ArchUnit, Tests). Nachweis: `rg "navigator\.entries|context\.navigator" docs README.md` findet nichts, Build grün.

## 5. Review

- [x] 5.1 Review-Agent (Haiku, read-only) prüft den Diff gegen die Deltas von `lobby-navigator`, `lobby-module-config` und `lobby-modules`, gegen KISS (keine übrig gebliebenen Abstraktionen) und gegen F.I.R.S.T. (keine Sleeps, keine Systemzeit, kein geteilter statischer Zustand, jeder Test mit Assertion). Nachweis: Bericht ohne offene Befunde oder Befunde behoben.

## 6. Pull Request

- [x] 6.1 Pull Request auf `main` öffnen mit dem Titel `refactor(navigator)!: hard-code navigator destinations in a single aves module`, Beschreibung auf Englisch, mit dem `BREAKING CHANGE:`-Footer aus proposal.md. Nachweis: PR-URL, CI grün.
- [ ] 6.2 Beim Archivieren den `## Purpose`-Absatz in `openspec/specs/lobby-navigator/spec.md` anpassen („Ziele stehen fest im Navigator-Modul; Öffnen häuft nichts an“), Commit `docs(openspec): archive navigator-entries-in-code`. Nachweis: `openspec validate --specs` ohne Fehler.
