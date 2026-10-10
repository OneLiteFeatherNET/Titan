# Proposal

## Why

Die Tests laufen nicht auf dem Weg, den der Betrieb geht. Produktiv liest das Jar `application.yaml` über avaje-config und SnakeYAML, baut daraus `PlatformBeans`, wählt die Welt per `ServiceLoader<LobbyWorldChoice>` und startet die Columns im echten `BeanScope`. Die Tests ersetzen genau diese Kette: `VariantStartTest` und `WiringTest` mocken `MapProvider`, `FeatureFlags` und `PermissionService`; die Season-Tests in `apps/cloudnet` setzen Werte mit `Config.setProperty` und umgehen den YAML-Parser; die Column-Tests laufen ohne SnakeYAML, weil `titan.column` es nicht mitbringt (nur `features/navigator` ergänzt es, Produktion bekommt es transitiv über `common`). So blieb ein echter Fehler unbemerkt: unquotierte YAML-Datum-Uhrzeit-Werte verwirft SnakeYAML still. Der AOT-Trainingslauf bootet das Shadow-Jar bereits, prüft aber nichts.

## What Changes

- **Column-Tests mit Produktions-Parser**: `titan.column` ergänzt `testRuntimeOnly(libs.snakeyaml)`; die nun überflüssige Zeile in `features/navigator/build.gradle.kts` entfällt.
- **Boot-Smoketest je Variante**: Ein Integrationstest startet das echte Shadow-Jar von `cloudnet` und `local` in einem `@TempDir` mit minimaler `worlds/`-Fixture und einer echten `application.yaml`, wartet mit Zeitgrenze auf die Startmeldung der Variante, prüft, dass der Prozess mit Exit-Code 0 endet und kein ERROR loggt.
- **YAML-Parität**: Ein Test in `apps/cloudnet` lädt die zusammengeführte `application.yaml` (Standardwerte aller Columns) und eine Betreiberdatei über avaje-config mit SnakeYAML und prüft, dass jeder Abschnitt jeder Column typgerecht ankommt.
- Kein Produktionscode ändert sich. Der Fehler unquotierter Datum-Uhrzeit-Werte wird in `season-quoted-dates` behoben, nicht hier.

## Capabilities

### New Capabilities

_Keine._

### Modified Capabilities

- `app-variants`: Neue Anforderung, dass jede Variante aus ihrem ausgelieferten Jar mit einer echten Konfigurationsdatei und Welt startet (ADDED, keine bestehende Anforderung ändert sich).
- `lobby-module-config`: Neue Anforderung, dass die ausgelieferten Standardwerte und eine Betreiberdatei mit demselben Parser wie im Betrieb vollständig gelesen werden (ADDED).

## Impact

- **Code**: keiner in `src/main`. `buildSrc/.../titan.column.gradle.kts`, `buildSrc/.../titan.app-variant.gradle.kts` (Task `bootSmokeTest`, hängt an `check`), `features/navigator/build.gradle.kts`, neue Tests in `apps/cloudnet/src/test` und `apps/local/src/test`, ein gemeinsamer Prozess-Helfer in `core/src/testFixtures`.
- **Abhängigkeiten**: keine neuen; `libs.snakeyaml` (schon im Katalog) kommt in die Test-Laufzeit aller Columns.
- **Tests**: Neue Integrationstests (Kindprozess), ein Unit-artiger Paritätstest. Die Boot-Tests laufen im eigenen Task und verlängern `./gradlew check`, nicht `test`.
- **Nutzertexte**: keine.
- **Berührt**: `season-quoted-dates` ergänzt SnakeYAML nur in `features/season`; mit diesem Change wird das redundant, wer zweiter landet, streicht die Zeile. `optional-extensions-bootstrap` verlegt Bootstrap-Code aus `TitanApplication`; die Boot-Tests hängen nur an der Startmeldung und am Exit-Code und bleiben davon unberührt. `seasonal-lobby-world` ändert zwei andere Anforderungen von `app-variants`; die neue Anforderung hier trägt einen eigenen Titel.

## Delivery

PR-Titel: `test(runtime): boot the shipped jar and parse a real application.yaml`
