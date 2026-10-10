# Tasks

## Execution Plan

Reines Refactoring ohne neues Verhalten: Es entsteht kein neuer Test, die bestehende Suite ist die Charakterisierung. Ein Sonnet-Agent in einem Worktree führt die Umzüge aus (Deliver-Paket und Sit-Event berühren dieselben Import-Zeilen, deshalb keine parallelen Agents auf denselben Dateien); ein Haiku-Agent verifiziert danach read-only.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | Baseline | 1.1 | haiku | nichts (nur `./gradlew build`) | alle Dateien |
| 2 | Umbau | 2.1–2.5 | sonnet | `core/src/main/java/net/onelitefeather/deliver/**`, `core/src/main/java/net/onelitefeather/titan/api/deliver/**`, `core/src/main/java/net/onelitefeather/titan/core/event/**`, `core/src/main/java/net/onelitefeather/titan/core/module/item/LobbyItems.java`, `core/src/testFixtures/**/DummyDeliver.java`, `common/src/main/java/net/onelitefeather/titan/common/{deliver,argument,utils/tags}/**`, `common/src/test/java/net/onelitefeather/titan/common/{deliver,argument}/**`, `features/sit/src/main/**`, `features/navigator/src/**`, `apps/cloudnet/src/test/**/NavigatorProtectionOrderingTest.java`, `docs/lobby-modules.md` | `bridge/**`, alle `build.gradle.kts`, `buildSrc/**`, `runtime/**`, `setup/**`, Service-Files, `openspec/specs/**` |
| 3 | Verifikation | 3.1–3.3 | haiku | nichts (nur lesen und `./gradlew build`) | alle Dateien |
| 4 | PR | 4.1 | sonnet | Git/GitHub | Quelldateien |

## 1. Charakterisierung vor dem Umbau

- [x] 1.1 Auf `origin/main` `./gradlew build` ausführen (Ebene: gesamte Testpyramide, unit + Cyano-Integration) und die Anzahl der Tests je Modul notieren. Verifikation: Build ist grün; die Zahlen dienen 3.1 als Vergleich (`common` minus die Tests von `ArgumentMaterialType`).

## 2. Umbau

- [x] 2.1 `net.onelitefeather.deliver.*` (6 Dateien) per `git mv` nach `core/src/main/java/net/onelitefeather/titan/api/deliver/` verschieben, `package`-Zeilen und alle Imports in `Deliver`, `DebugDeliver`, `MessageChannelDeliver`, `DebugDeliverTest`, `DummyDeliver`, `NavigatorModule`, `RecordingDeliver` und `NavigatorProtectionOrderingTest` anpassen; das Verzeichnis `net/onelitefeather/deliver` ist danach leer und entfernt. Ebene: unit + Integration (bestehend, kein neuer Test). Verifikation: `./gradlew :core:build :common:test :features:navigator:test :apps:cloudnet:test` ist grün. Nachtrag: bereits durch #379 auf `origin/main` erledigt; in diesem PR ohne Änderung.
- [x] 2.2 `EntityDismountEvent` per `git mv` nach `features/sit/src/main/java/net/onelitefeather/titan/feature/sit/` verschieben, Paket und Import in `SitModule` anpassen (der Import entfällt, gleiches Paket). Ebene: unit + Integration (bestehende `SitModuleTest`, `SitModuleIntegrationTest`, `ColumnArchitectureTest`). Verifikation: `./gradlew :features:sit:test` ist grün.
- [x] 2.3 `PosTagSerializer` löschen. Ebene: Compiler. Verifikation: `./gradlew :common:build` ist grün und `grep -rn "PosTagSerializer" apps bridge common core features platform runtime setup docs` findet nichts.
- [x] 2.4 `ArgumentMaterialType`, `ArgumentMaterialTypeTest` und `ArgumentMaterialTypeIntegrationTest` löschen; leere Pakete `argument` und `utils/tags` entfernen. Verifikation: `./gradlew :common:test` ist grün und `grep -rn "ArgumentMaterialType" apps bridge common core features platform runtime setup docs` findet nichts.
- [x] 2.5 Javadoc von `LobbyItems` korrigieren (Halbsatz `in this wave, temporarily in {@code :app}` streichen) und in `docs/lobby-modules.md` `EntityDismountEvent` aus der Aufzählung des `core`-Inhalts nehmen. Verifikation: `grep -n ":app" core/src/main/java/net/onelitefeather/titan/core/module/item/LobbyItems.java` findet nichts; `grep -n "EntityDismountEvent" docs/lobby-modules.md` findet nichts.

## 3. Verifikation

- [x] 3.1 Vollständigen Build ausführen: `./gradlew build` ist grün; Testzahlen wie in 1.1, außer `common` (minus die entfallenen `ArgumentMaterialType`-Tests) und `sit` (unverändert).
- [x] 3.2 Suche nach entfernten oder verschobenen Symbolen: `grep -rn "net.onelitefeather.deliver\|titan.core.event.EntityDismountEvent\|PosTagSerializer\|ArgumentMaterialType" apps bridge buildSrc common core features platform runtime setup docs *.kts *.md` findet nichts (Treffer nur unter `openspec/changes/archive` sind erlaubt). Zusätzlich: `git diff origin/main --stat -- bridge buildSrc runtime setup` ist leer (der Classloader-Vertrag der `bridge` und die Build-Dateien sind unberührt).
- [x] 3.3 Bytecode-Stichprobe des `bridge`-Vertrags: `./gradlew :bridge:build` ist grün und `git diff origin/main --stat -- common/src/main/java/net/onelitefeather/titan/common/deliver common/src/main/java/net/onelitefeather/titan/common/permission` zeigt nur die Import-Änderung in `DebugDeliver` und `MessageChannelDeliver`, keine Änderung an `ServerConnector`, `TitanServerConnector`, `TitanPermissionBridge`.

## 4. Pull Request

- [x] 4.1 Pull Request von `refactor/core-common-cleanup` nach `main` mit dem Titel `refactor(core): remove dead classes and rename the deliver package` öffnen. Beschreibung auf Englisch, nennt: die verschobenen Pakete (`net.onelitefeather.deliver` nach `net.onelitefeather.titan.api.deliver`, `EntityDismountEvent` nach `features/sit`), die gelöschten Klassen (`PosTagSerializer`, `ArgumentMaterialType` mit Tests), das korrigierte Javadoc, dass es sich um einen reinen Refactor ohne Verhaltensänderung und ohne Breaking Change handelt (`core`/`common` nicht veröffentlicht, `bridge`-Vertrag unberührt), sowie die Folgearbeit (`api` -> `implementation` bei `runtime` -> `common`, doppelte `Titan`-Klasse). Verifikation: Der PR existiert und der CI-Lauf ist grün.
