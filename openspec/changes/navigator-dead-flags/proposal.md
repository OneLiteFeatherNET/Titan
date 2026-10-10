# Proposal

## Why

`features/navigator/src/main/resources/titan/defaults/navigator.yaml` deklariert fünf Feature-Flags, aber nur `NAVIGATOR_SLENDER` hängt an einem Ziel (`Destination.SLENDER`). `NAVIGATOR_CREATIVE`, `NAVIGATOR_MANIS`, `NAVIGATOR_SURVIVAL` und `NAVIGATOR_ELYTRA` schalten nichts; `MANIS` hat nicht einmal ein Ziel. README und `docs/lobby-modules.md` führen sie als „bekannte“ Flags, Betreiber setzen sie in der Erwartung, etwas zu ändern. Außerdem hat der Anzeigename von Slender den ungültigen Farbwert `#e80000c` (sieben Hex-Stellen). MiniMessage wirft dabei nicht, sondern liest die Zahl 0xE80000C und schneidet auf 24 Bit ab: Der Verlauf endet in `#80000C` statt in `#e80000`, also merklich dunkler als gemeint.

## What Changes

- Die vier ungenutzten Deklarationen fallen aus `navigator.yaml`; `NAVIGATOR_SLENDER` bleibt. Die mitgelieferten Standardwerte kennen damit genau eine Flag.
- README (Abschnitt „Feature flags“, Beispiel `application.yaml`, „Local testing with every flag on“) und `docs/lobby-modules.md` (Abschnitt „Standardwerte je Column“) nennen nur noch `NAVIGATOR_SLENDER`.
- Der Flag-Name `NAVIGATOR_SLENDER` steht als Konstante im Navigator-Modul, `Destination.SLENDER` und die Tests verweisen darauf statt auf ein Literal.
- Der Verlauf von Slender endet in `#e80000`.
- **Kompatibilität (kein BREAKING):** Setzt ein Betreiber eine entfernte Flag (`features.NAVIGATOR_CREATIVE: true`, `FEATURES_NAVIGATOR_MANIS=true`, …), ist sie „unbekannt“: `ConfigFeatureFlags.isActive` liefert `false`, `exists` ebenfalls, es gibt weder Warnung noch Startfehler, und ohnehin hing an den Flags nichts. Der Eintrag ist ein toter Konfigurationsschlüssel und kann gelöscht werden.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: neue Anforderung „Der Navigator kennt genau eine Feature-Flag“ (Standardwerte, ignorierte entfernte Flags, Verlaufsfarbe von Slender). Bestehende Anforderungen bleiben wörtlich unverändert; `lobby-module-config` gilt weiter (jede *bekannte* Flag steht in den Standardwerten), nur die Menge der bekannten Flags schrumpft.

## Impact

- **Code**: `features/navigator` (`Destination`, neue Konstante, `navigator.yaml`); Tests dort (`DefaultNavigatorFeatureFlagsTest`, `NavigatorDestinationTest`, `NavigatorFeatureFlagTest`, `NavigatorModuleTest`, `NavigatorModuleLeakTest`, `NavigatorBuildDestinationTest` nutzen die Konstante). `runtime`, `core`, `ConfigFeatureFlags` und `FeatureFlags` bleiben unverändert.
- **Docs**: `README.md`, `docs/lobby-modules.md`.
- **Abhängigkeiten**: keine neuen.
- **Nutzertexte**: der Anzeigename „Slender“ ändert nur die Endfarbe seines Verlaufs; kein neuer oder geänderter Text, kein i18n.
- **Betrieb**: bestehende `features.NAVIGATOR_{CREATIVE,MANIS,SURVIVAL,ELYTRA}`-Einträge in `application*.yaml` oder als Env-Variable sind wirkungslos und dürfen stehen bleiben.

## Delivery

PR-Titel: `fix(navigator): drop dead feature flags and fix the slender gradient`
