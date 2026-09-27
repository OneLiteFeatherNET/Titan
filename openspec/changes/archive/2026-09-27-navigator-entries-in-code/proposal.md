# Proposal

## Why

Die Ziele des Navigators ändern sich selten, und wenn doch, dann zusammen mit einem Release. Trotzdem ist der Navigator heute auf zwei Pakete und neun Klassen verteilt, mit über 1.100 Zeilen Produktivcode: Konfigurationsparser, Validierung, eine plattformweite Eintrags-Registry mit Versionszähler, Konfliktprüfung und Sichtbarkeitslogik. Ausgelegt ist das darauf, dass Betreiber Ziele per `application.yaml` pflegen und andere Module Ziele über `context.navigator()` beisteuern. Beides wird nicht genutzt: Kein anderes Modul steuert ein Ziel bei, und die Konfiguration entspricht den Standardwerten. Ursprünglich war der Navigator eine einzige Klasse mit einem fest verdrahteten Aves-Inventar. Zu dieser Einfachheit (KISS) soll er zurück.

## What Changes

- **BREAKING**: Titel und Ziele des Navigators (ElytraRace, Survival, Slender, Creative) werden fest im Code des Navigator-Moduls festgelegt. `navigator.title` und `navigator.entries.*` werden nicht mehr gelesen und aus der mitgelieferten `application.yaml` entfernt.
- **BREAKING**: Der Andockpunkt `context.navigator()` für Navigator-Beiträge anderer Module entfällt samt Registry (`NavigatorEntries`, `NavigatorEntry`, `NavigatorConflictException`) und Konfliktprüfung beim Start. Ein neues Ziel ist eine Codeänderung am Navigator-Modul.
- Der Navigator besteht aus genau einem Modul (`NavigatorModule`). Es baut ein gemeinsames Aves-Inventar (`GlobalInventoryBuilder`) einmal beim Start mit festen Plätzen und Klick-Handlern. Die Hilfsklassen `NavigatorInventory`, `NavigatorLayout`, `NavigatorVisibility`, `NavigatorEntryKeys` und `NavigatorEntryValidation` entfallen.
- Slender bleibt an die Feature-Flag `NAVIGATOR_SLENDER` (`features.*` in der Konfiguration) gebunden. Ein Umschalten zur Laufzeit wirkt weiterhin beim nächsten Öffnen.
- Für Spieler ändert sich nichts: gleiche Plätze, Symbole, Namen, Weiterleitungsziele und dieselbe Feder in Hotbar-Slot 4.

## Capabilities

### New Capabilities

_Keine._

### Modified Capabilities

- `lobby-navigator`: Die Ziele stehen fest im Navigator-Modul statt in der Konfiguration. Module können keine Ziele mehr beisteuern, und die Prüfung doppelt belegter Plätze entfällt. Die Flag-Anforderung gilt nur noch für Slender.
- `lobby-module-config`: Das Navigator-Beispiel („Liste von Einträgen“) und der Sonderfall „kaputter Navigator-Eintrag“ entfallen.
- `lobby-modules`: „Abschalten hinterlässt keine Reste“ nennt keine Navigator-Einträge mehr als Kontext-Ressource.

## Impact

- **Code (Produktion)**: `app/.../feature/navigator/` schrumpft auf `NavigatorModule`. `app/.../module/navigator/` wird gelöscht. `ModuleContext#navigator()`, `ModulePlatform#navigator()`, die Validierung in `ModuleRegistry#enableAll()` und die Verdrahtung in `Titan.java` entfallen.
- **Code (Tests)**: Die Tests der gelöschten Klassen werden gelöscht, darunter `NavigatorEntriesTest`, `NavigatorEntriesWiringTest`, `NavigatorEntryTest`, `NavigatorLayoutTest`, `NavigatorVisibilityTest`, `NavigatorEntryKeysTest` und `NavigatorEntryValidationTest`. `ModuleHarness#navigator()`, `ModulePlatformFixture` und `ModuleRegistryTest` werden angepasst. `NavigatorModuleTest`, `NavigatorFeatureFlagTest` und `NavigatorModuleLeakTest` bleiben als Verhaltensnachweis.
- **Konfiguration**: Der Abschnitt `navigator` in `app/src/main/resources/application.yaml` entfällt, ebenso die Verweise darauf in den Kommentaren zu `features` und `config.watch`. `navigator.*` in Betreiberdateien, Profilen oder Env-Variablen bleibt ohne Meldung wirkungslos.
- **Doku**: `docs/lobby-modules.md` (Andockpunkt `navigator`, Navigator-Abschnitte), `README.md` (Beispiel-Eintrag).
- **Abhängigkeiten**: keine neuen. Aves ist bereits eingebunden.
- **Nutzertexte**: Wortlaut und Formatierung bleiben gleich. Die MiniMessage-Texte wandern aus der YAML-Datei in den Code. Übersetzt werden sie in diesem Change nicht (siehe design.md, Non-Goals).
- **Folge-Change**: Ein Permission-Gate, z. B. für „Team Build“, kommt als eigener `feat(navigator)`-Change auf diesen einfachen Stand obendrauf.

## Delivery

PR-Titel: `refactor(navigator)!: hard-code navigator destinations in a single aves module`

Footer:

```
BREAKING CHANGE: the navigator no longer reads `navigator.title` and `navigator.entries.*`; its title and destinations are defined in `NavigatorModule`. The `ModuleContext#navigator()` extension point and the navigator entry registry are removed; modules can no longer contribute navigator entries. Feature flags under `features.*` are unchanged.
```
