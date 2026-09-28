# Tasks

## Execution Plan

Voraussetzung: `split-titan-into-columns` ist auf `main` gemergt. Integrationszweig: `refactor/permission-spi` von `origin/main`. Welle 2 verzweigt nach dem Merge von Welle 1 vom Integrationszweig. Jede Welle endet mit grünem `./gradlew build` auf dem Integrationszweig und geprüften Diffs.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | contract | 1.1–1.2 | sonnet | `core/**`, `runtime/src/**/permission/**` | `features/**`, `platform/**`, `apps/**` |
| 2 | luckperms-platform | 2.1–2.4 | sonnet | `platform/luckperms/**`, `settings.gradle.kts` (Scan `platform/*`; Katalog nur Guava/LuckPerms) | `runtime/**`, `features/**`, `apps/**` |
| 2 | runtime-wiring | 3.1–3.4 | sonnet | `runtime/**`, `apps/*/build.gradle.kts` (nur LuckPerms-/Gson-Einträge), `settings.gradle.kts` (Katalog: nur Butterfly entfernen) | `platform/**`, `features/**`, `core/src/main/**` |
| 3 | variants-docs | 4.1–4.4 | sonnet | `buildSrc/**`, `apps/**`, `README.md`, `docs/**` | `features/**`, `core/src/main/**` |
| 3 | verify | 4.5 | haiku | read-only | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Code |

Konflikt-Hinweis Welle 2: Beide Agents ändern den Versionskatalog in `settings.gradle.kts`, aber an verschiedenen Einträgen (Butterfly raus bzw. Guava/LuckPerms unverändert). Der Hauptkontext merged nacheinander.

Jeder Agent-Prompt nennt die Regeln, die für seinen Task gelten: erst Vorhandenes nutzen (Avaje `@Secondary`, LuckPerms-API, Minestom-ExtensionManager), Java 25 ohne Preview, keine neuen Nutzertexte, Logging über SLF4J mit Parametern (INFO für den aktiven Dienst, eine Ausnahme wird einmal geloggt), keine neuen Metriken/Spans, F.I.R.S.T., Test zuerst, schlanke Kommentare, Conventional Commits `refactor(permission): …`.

## 1. Vertrag und Fallback (Welle 1)

- [x] 1.1 `PermissionService` und `PermissionResult` in `core` anlegen (D1). Nachweis: `./gradlew :core:build` grün.
- [x] 1.2 Test zuerst (Unit, `runtime`): `DenyAllPermissionService` liefert für jede UUID/jedes Recht `NOT_SET`, `name()` ist `deny-all`. Danach die Bean als `@Singleton @Secondary` umsetzen (D2). Nachweis: Test grün, `./gradlew :runtime:build` grün.

## 2. `platform/luckperms` (Welle 2)

- [x] 2.1 Modul `platform/luckperms` anlegen, `settings.gradle.kts` scannt `platform/*`; Abhängigkeiten laut D4 (LuckPerms-API compileOnly, Loader runtimeOnly, Guava, Gson-Ausschluss auf dem Testpfad); `package-info.java` mit `@InjectModule(name = "luckpermsPlatform", provides = PermissionService.class)`. Nachweis: `./gradlew :platform:luckperms:build` grün.
- [x] 2.2 Test zuerst (Unit): `CompatibilityUtil` bildet `Tristate.TRUE/FALSE/UNDEFINED` auf `ALLOWED/DENIED/NOT_SET` ab; danach `CompatibilityUtil` aus `runtime` hierher verschieben und anpassen. Nachweis: Test grün.
- [x] 2.3 Test zuerst (Unit): Die Prüfung auf doppeltes Laden wirft bei einer geladenen Extension `LuckPerms` eine Ausnahme mit der Extension im Text und lässt eine Liste ohne sie durch. Danach `LuckPermsPermissionService` umsetzen: Prüfung, Start über den Loader in `@PostConstruct`, `check` mit Kontexten laut D4. Nachweis: Tests grün.
- [x] 2.4 Der Integrationstest „Scope mit `platform/luckperms` wählt `LuckPermsPermissionService` statt Fallback“ ersetzt LuckPerms-Start und Loader durch eine Test-Bean, sofern Avaje das erlaubt; sonst ist der Nachweis die manuelle Abnahme in 4.4. Nachweis: Test grün bzw. dokumentierte Begründung im Agent-Ergebnis.

## 3. `runtime` umstellen und Butterfly entfernen (Welle 2)

- [x] 3.1 Test zuerst (Integration, Cyano-Env): Ein `TitanPlayer` mit Fake-Dienst liefert für `ALLOWED`/`DENIED`/`NOT_SET` die Ergebnisse `hasPermission` true/false/false (D3). Danach den Konstruktor von `TitanPlayer` umstellen, LuckPerms-Importe entfernen und den Player-Provider in `Titan` nach dem Aufbau des Scopes setzen. Nachweis: Test grün.
- [x] 3.2 Test zuerst (Unit): `PermissionBridgeResolver.of(service)` liefert `true` genau bei `ALLOWED`. Danach die Bean setzt den Resolver in `@PostConstruct` und entfernt ihn in `@PreDestroy` (D5). LuckPerms-Start und Resolver in `TitanApplication.main` entfernen. Nachweis: Test grün, `grep -n LuckPerms runtime/src/main` ohne Treffer.
- [x] 3.3 INFO-Log `Permissions resolved by {}` nach dem Aufbau des Scopes. Test zuerst (Unit mit erfasstem Appender): Zeile mit `deny-all` bei Fallback. Nachweis: Test grün.
- [x] 3.4 Butterfly entfernen (D7): Abhängigkeit, Katalogeintrag, Laden, Shutdown-Task; LuckPerms-Abhängigkeiten, Guava und Gson-Ausschluss aus `runtime` entfernen. Nachweis: `./gradlew build` grün, `grep -rni butterfly --include=*.kts --include=*.java .` ohne Treffer.

## 4. Varianten, Doku und Abnahme (Welle 3)

- [ ] 4.1 `titan.app-variant` um `platform("…")` erweitern: hängt `:platform:luckperms` an und ergänzt `luckpermsPlatform` in `expectedModules` (D6). Test zuerst (Unit, `buildSrc`): Die erzeugten `variant.properties` enthalten die Plattform-Module. Nachweis: Test grün.
- [ ] 4.2 `apps/cloudnet` setzt `platform("luckperms")`; `platform("luckperms")` schließt `minestom-loader` vom Test-Runtime-Classpath der Variante aus (Gson), und jeder Scope-Test in `apps/cloudnet` (`VariantStartTest`, `WiringTest`, `PlatformBeansWiringTest`, `StandardLoadoutTest`, `NavigatorProtectionOrderingTest`) ersetzt `PermissionService` per Avaje-Mock, damit kein Test LuckPerms startet; `apps/local` nur mit `-Ptitan.luckperms`. Starttests (Integration): `apps/local` ohne Schalter meldet `deny-all`; in `apps/cloudnet` steht `luckpermsPlatform` in den erwarteten Modulen. Nachweis: `./gradlew build` und `./gradlew :apps:local:build -Ptitan.luckperms` grün.
- [ ] 4.3 README und Deploy-Doku: `-Ptitan.luckperms`, Butterfly entfällt, `extensions/luckperms.jar` und `butterfly.jar` entfernen, `data/` behalten, AOT-Cache neu trainieren. Erinnerung/Doku zur überholten LuckPerms-Extension 6.0.1 korrigieren. Nachweis: `grep -rni "butterfly\|luckperms.jar" README.md docs` nennt nur noch den Hinweis zum Entfernen.
- [ ] 4.4 Manuelle E2E-Abnahme mit `titan-cloudnet.jar` und bestehendem `data/`: Das Start-Log nennt `luckperms`. Ein Spieler mit `titan.command.stop` kann `/stop` ausführen, ohne das Recht nicht. Ein Recht nur im Kontext wirkt. Mit einem eingelegten `extensions/luckperms.jar` bricht der Start mit Meldung ab. Wenn eine CloudNet-Testumgebung verfügbar ist, liefert eine CloudNet-Rechteabfrage dasselbe Ergebnis. `apps/local` ohne Schalter startet ohne LuckPerms. Nachweis: Protokoll der Abnahme im PR.
- [ ] 4.5 Verifikation (Haiku, read-only): Szenarien der Spec `lobby-permissions` Test für Test bzw. Abnahmepunkt zuordnen; F.I.R.S.T.-Check (kein statischer Zustand in Tests außer dokumentiertem Zurücksetzen, keine Sleeps). Nachweis: Bericht ohne Lücken.

## 5. Pull Request

- [ ] 5.1 Den Pull Request vom Integrationszweig auf `main` unter dem Titel `refactor(permission)!: resolve permissions through an optional di module` öffnen, mit dem BREAKING-CHANGE-Footer aus dem Proposal, dem Hinweis, dass CloudNet-Rechteabfragen jetzt die LuckPerms-Kontexte nutzen, dem Deploy-Hinweis und dem Abnahmeprotokoll aus 4.4 in der englischen Beschreibung. Nachweis: PR-URL, CI grün.
