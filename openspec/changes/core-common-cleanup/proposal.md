# Proposal

## Why

`core` und `common` haben Altlasten aus der Zeit vor den Columns: Das Paket `net.onelitefeather.deliver` (ohne `titan`-Präfix) liegt neben `net.onelitefeather.titan.api.deliver.Deliver`, obwohl `Deliver` den `DeliverComponent` in seiner Signatur trägt. `EntityDismountEvent` liegt in `core`, obwohl nur die Column `sit` ihn feuert und hört. `PosTagSerializer` und `ArgumentMaterialType` haben keinen Aufrufer außerhalb ihrer eigenen Klasse und Tests. Das Javadoc von `LobbyItems` verweist noch auf `:app` „in this wave". Das sind tote Codepfade und falsch platzierte Andockpunkte.

**Auslieferung:** `refactor(core): remove dead classes and rename the deliver package` (Conventional Commit, PR-Titel). Nicht breaking, siehe Impact.

## What Changes

- `net.onelitefeather.deliver` (`DeliverComponent`, `DeliverType`, `TaskBuilderImpl`, `ServerBuilderImpl`, `TaskComponentImpl`, `ServerDeliverComponentImpl`) zieht in `net.onelitefeather.titan.api.deliver`, das Paket von `Deliver`. Nur Paketname und Imports ändern sich; die versiegelte Hierarchie bleibt in einem Paket.
- `EntityDismountEvent` zieht von `core` in die Column `features/sit` (`net.onelitefeather.titan.feature.sit`). `core` verliert einen Andockpunkt, den nur eine Column nutzt.
- `PosTagSerializer` (`common`) entfällt ersatzlos.
- `ArgumentMaterialType` samt `ArgumentMaterialTypeTest` und `ArgumentMaterialTypeIntegrationTest` (`common`) entfällt ersatzlos.
- Das Javadoc von `LobbyItems` nennt `features/hotbar` als Heimat von `HotbarLobbyItems` ohne den `:app`-Verweis.
- `docs/lobby-modules.md` (Abschnitt über `core`) nennt `EntityDismountEvent` nicht mehr als Inhalt von `core`.
- Kein Verhalten ändert sich.

Bei der Prüfung als falsch verworfen (bleiben unangetastet):
- `Cancelable` bleibt in `core`: `ProtectionModule` und `setup/Titan` nutzen es.
- Die Block-Handler in `common` bleiben: `BlockHandlerHelper.registerAll()` wird von `runtime/Titan` und `setup/Titan` aufgerufen.
- `TitanMiniMessageImpl` bleibt: Das Service-File macht das Tag `<prefix>` verfügbar, das `DebugDeliver` und `MapCommand` nutzen.

Bewusst draußen, als Folgearbeit (eigene Changes): `runtime` gibt `common` als `api` weiter und leakt damit Aves- und avaje-config-Typen an jede Variante (die Umstellung auf `implementation` braucht eine Prüfung aller Consumer); zwei Klassen `Titan` in `runtime` und `setup` (getrennte Module, nur ein Namensproblem).

## Capabilities

### New Capabilities
Keine.

### Modified Capabilities
Keine. Es ändert sich kein Spielerverhalten und keine Anforderung in `openspec/specs/`; kein Spec beschreibt den Inhalt von `core` oder `common`. `.openspec.yaml` setzt deshalb `skip_specs: true`.

## Impact

- **Code:** `core/src/main/java/net/onelitefeather/deliver/**` (6 Dateien, Paketwechsel), `core/.../core/event/EntityDismountEvent.java` (Umzug nach `features/sit`), `common/.../utils/tags/PosTagSerializer.java` und `common/.../argument/ArgumentMaterialType.java` (gelöscht), `LobbyItems.java` (Javadoc). Imports von `DeliverComponent`/`DeliverType` ändern sich in `Deliver`, `common` (`DebugDeliver`, `MessageChannelDeliver`, `DebugDeliverTest`), `core` testFixtures (`DummyDeliver`), `features/navigator` (`NavigatorModule`, `RecordingDeliver`) und `apps/cloudnet` (`NavigatorProtectionOrderingTest`).
- **Tests:** `ArgumentMaterialTypeTest` und `ArgumentMaterialTypeIntegrationTest` entfallen mit ihrer Klasse; sonst keine neuen Tests, das Verhalten sichert die bestehende Suite.
- **Breaking:** Nein. `core` und `common` werden nicht veröffentlicht; `buildSrc/.../titan.publish-conventions` publiziert nur `apps/*` (Shadow-Jars), `setup` und `bridge`. Das `bridge`-Modul (eigener Classloader, `compileOnly` auf `common`) nutzt nur `ServerConnector`, `TitanServerConnector` und `TitanPermissionBridge`; keine davon wird verschoben oder gelöscht, und `bridge` importiert weder das Deliver-Paket noch eine gelöschte Klasse. Keine Service-Files, Reflection- oder Relocation-Verweise auf das alte Deliver-Paket (per Suche geprüft).
- **Konfiguration, Abhängigkeiten, Nutzertext:** keine Änderung.
