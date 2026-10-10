# Design

## Context

Siehe proposal.md - Why. Die Altlasten liegen in `core` (API-Modul, das jede Column sieht) und `common` (Bibliothek für `runtime`, `setup`, `bridge`). Zwei Randbedingungen bestimmen die Reihenfolge der Schritte:

- `bridge` ist eine Minestom-Extension mit eigenem Classloader und `compileOnly` auf `common`. Nur JDK-Typen kreuzen die Grenze über `ServerConnector`, `TitanServerConnector` und `TitanPermissionBridge` (alle in `common`). Diese drei bleiben, wo sie sind.
- `Deliver.sendPlayer(Player, DeliverComponent)` ist die einzige Signatur, die den `DeliverComponent` nach außen trägt. Verbraucher: `common` (`DebugDeliver`, `MessageChannelDeliver`), `features/navigator`, testFixtures und ein Test in `apps/cloudnet`.

## Goals / Non-Goals

**Goals:**
- Jede Klasse liegt in dem Modul und Paket, das ihre tatsächlichen Nutzer widerspiegelt.
- Tote Klassen sind weg, ohne dass ein Build- oder Laufzeitpfad sie vermisst.

**Non-Goals:**
- Kein Wechsel von `api` zu `implementation` bei `runtime` -> `common` (Folgearbeit, Aves- und avaje-config-Typen sind Teil der Signaturen einiger Beans; jeder Consumer müsste einzeln geprüft werden).
- Keine Umbenennung der beiden `Titan`-Klassen.
- Keine Verhaltensänderung, keine neuen Klassen außer dem Umzug.

## Decisions

**1. Deliver-Paket wird `net.onelitefeather.titan.api.deliver`.** Statt eines neuen Pakets (z. B. `...titan.core.deliver`) geht es in das Paket von `Deliver`: Interface und Argumenttyp gehören zusammen, und die versiegelten Interfaces mit ihren Implementierungen (`permits`) bleiben in einem Paket. Ein reiner Paketumzug ändert weder Typen noch Sichtbarkeit. Getestet durch die bestehenden Tests (`DebugDeliverTest`, `RecordingDeliver`-Nutzer, `NavigatorProtectionOrderingTest`); der Compiler fängt jeden vergessenen Import. SOLID: kohäsiveres Paket (SRP), keine Abhängigkeitsänderung. Built-in: IDE-/Gradle-Refactoring genügt, keine eigene Infrastruktur.

**2. `EntityDismountEvent` wandert in `features/sit`.** Nur `SitModule` feuert und hört es (`EventDispatcher.call` und `node.on`). Column-lokal bedeutet: `core` bleibt schmal, die Column besitzt ihr Event (das Modul-Unabhängigkeits-Gebot aus `lobby-modules` bleibt erfüllt, da keine andere Column es braucht). Das Event bleibt `public` in `net.onelitefeather.titan.feature.sit`, damit `SitModule` es ohne Sichtbarkeitsänderung nutzt. Getestet durch die vorhandenen `sit`-Tests (Cyano-Integration: absteigen vom Sitz) plus die ArchUnit-Regeln der Column. SOLID: SRP und ISP - `core` bietet nur, was mehrere Columns teilen. `Cancelable` bleibt in `core`, weil `protection` und `setup` es nutzen.

**3. Löschen statt Aufheben von `PosTagSerializer` und `ArgumentMaterialType`.** Beide haben keinen produktiven Aufrufer in irgendeinem Modul (Suche über `apps`, `bridge`, `common`, `core`, `features`, `platform`, `runtime`, `setup`, `docs`; keine Service-Files, kein Reflection-Zugriff). `ArgumentMaterialType` hat nur zwei eigene Tests, die mit ihm entfallen. Wer sie wieder braucht, holt sie aus der Git-Historie. Getestet durch einen vollständigen Build und eine Suche nach den Symbolen. SOLID/Clean Code: keine toten Codepfade (Projektregel).

**4. Javadoc von `LobbyItems` wird korrigiert.** Nur der Halbsatz „in this wave, temporarily in `:app`" entfällt; `HotbarLobbyItems` liegt in `features/hotbar`. Kein Test nötig (Kommentar).

## Risks / Trade-offs

- [Vergessener Import des alten Deliver-Pakets kompiliert erst spät] -> `./gradlew build` über alle Module vor dem PR; abschließende Suche nach `net.onelitefeather.deliver` in Quellen, Docs und Service-Files findet nichts.
- [Ein extern gebautes Artefakt referenziert `DeliverComponent`] -> `core`/`common` sind nicht veröffentlicht (nur Shadow-Jars der `apps`, `setup`, `bridge`); die drei veröffentlichten Jars enthalten keinen Consumer von außen. Kein Breaking Change.
- [`bridge` bricht zur Laufzeit wegen Classloader-Vertrag] -> `bridge` referenziert nur `ServerConnector`, `TitanServerConnector`, `TitanPermissionBridge`; nichts davon wird angefasst. Verifikation greift die `bridge`-Importe ab.
- [`EntityDismountEvent` wird später von einer zweiten Column gebraucht] -> Dann wandert es bewusst zurück nach `core`; heute ist es ein einzelner Nutzer.

## Migration Plan

Ein PR, ein Merge, kein Datenumzug. Rollback ist `git revert`.
