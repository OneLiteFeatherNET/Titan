# Proposal

## Why

Slender hängt im Navigator an der Feature-Flag `NAVIGATOR_SLENDER`. Eine Flag gilt für alle Spieler gleichzeitig: Wer Slender sehen soll, dem sieht es jeder sehen. Build zeigt dagegen schon heute nur Spielern mit dem Recht `titan.navigator.buildserver`. Slender soll dasselbe Muster nutzen, damit die Sichtbarkeit pro Spieler über LuckPerms gesteuert wird und nicht über einen lobbyweiten Schalter.

## What Changes

- **BREAKING:** Das Ziel Slender ist an das Recht `titan.navigator.slender` gebunden, wie Build an `titan.navigator.buildserver`. `Destination.SLENDER` verliert die Flag `NAVIGATOR_SLENDER`, `Destination.SLENDER_FLAG` entfällt, und der Navigator hängt nicht mehr an `FeatureFlags`. Die Flag fällt aus `runtime/src/main/resources/titan/defaults/features.yaml` heraus.
- Betreiber mit `NAVIGATOR_SLENDER: true` verlieren Slender, bis Spieler das Recht `titan.navigator.slender` haben. Der Eintrag wird nicht mehr gelesen.
- Der Navigator zeigt je Spieler das Menü für genau die Rechte, die er hat. Dafür gibt es je Sprache vier geteilte Inventare (ohne Recht, nur Slender, nur Build, beide), also acht insgesamt, angelegt beim Start.
- Der Klick prüft das Recht wie bisher erneut. Ein Linksklick auf Slender, das auf mehreren Diensten läuft, öffnet weiterhin die Dienstauswahl.
- Der CloudNet-Task des Slender-Ziels bleibt `Slender`. Der Stand auf `main` nutzt diesen Namen bereits, es ist keine Änderung nötig.
- Die Konfigurationstests (runtime, cloudnet, buildSrc) nutzen für ihre Flag-Mechanik `LOBBYSWITCHER` statt des entfernten `NAVIGATOR_SLENDER`.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: Slender wird über das Recht statt über die Flag gesteuert. Der Navigator hat keine Feature-Flag mehr. Der Verlauf des Slender-Namens bleibt unverändert und wird als eigene Anforderung weitergeführt.

## Impact

- **Code**: `features/navigator` (`Destination`, `NavigatorModule`, `SharedNavigator`, `package-info`), Tests dort, `runtime` (`features.yaml`, `ConfigFeatureFlagsTest`), `apps/cloudnet` und `buildSrc` (Test-Fixtures mit `NAVIGATOR_SLENDER`).
- **Docs**: `README.md`, `docs/lobby-modules.md`.
- **Abhängigkeiten**: keine neuen. `FeatureFlags` bleibt im Core und in der Lobby für `LOBBYSWITCHER`.
- **Betrieb**: Vor dem Rollout `titan.navigator.slender` den Gruppen erteilen, die Slender sehen sollen (LuckPerms), und `NAVIGATOR_SLENDER` aus `application*.yaml` bzw. der Umgebung entfernen. Die lokale Variante nutzt `DenyAllPermissionService`, dort sieht niemand Slender.
- **Nutzertexte**: keine neuen oder geänderten Texte.

## Delivery

PR-Titel: `feat(navigator)!: gate slender behind a permission`
