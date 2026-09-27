# Design

## Context

Der Navigator ist heute auf zwei Pakete verteilt:

- `app/.../feature/navigator/`: `NavigatorModule` (346 Zeilen, liest Config beim Start und bei jedem Öffnen), `NavigatorInventory` (Aves-`GlobalInventoryBuilder`, baut neu, sobald sich die sichtbaren Einträge ändern), `NavigatorLayout`, `NavigatorVisibility`, `NavigatorEntryKeys`, `NavigatorEntryValidation`.
- `app/.../module/navigator/`: plattformweite Registry `NavigatorEntries` mit Versionszähler, Modul-Views und Validierung, dazu `NavigatorEntry` und `NavigatorConflictException`. Verdrahtet über `ModulePlatform#navigator()`, `ModuleContext#navigator()`, `ModuleRegistry#enableAll()` (`validate(FeatureFlags)`) und `Titan.java`.

In Produktion trägt nur `NavigatorModule` selbst Einträge in die Registry ein. Die ursprüngliche Umsetzung (`NavigatorFeature`, vor dem Umbau auf Module) war eine Klasse mit einem fest verdrahteten `GlobalInventoryBuilder`, festen Plätzen und je einem Klick-Handler.

`FeatureFlags` liest live über die `Config`-Fassade, also wirkt `isActive("NAVIGATOR_SLENDER")` nach einem Config-Reload sofort.

Motivation: siehe proposal.md, Why.

## Goals / Non-Goals

**Goals:**
- Ein Modul, eine Produktivklasse. Fest verdrahtete Ziele. Das Aves-Inventar wird einmal gebaut und nur neu gelegt, wenn die Slender-Flag umschaltet.
- Spielerverhalten und Flag-Verhalten bleiben gleich (Specs `lobby-navigator`, `lobby-hotbar`).
- Der Modul-Kontext verliert den Andockpunkt `navigator`, sonst ändert sich an der Modul-Plattform nichts.

**Non-Goals:**
- Permission-Gate für einzelne Ziele wie „Team Build“. Das kommt als eigener `feat(navigator)`-Change danach.
- Übersetzung von Titel und Zielnamen. Die Texte werden verschoben, nicht neu eingeführt. Auf `main` gibt es noch keinen `TranslationStore`.
- Aufräumen der ungenutzten Flags `NAVIGATOR_CREATIVE`, `NAVIGATOR_MANIS`, `NAVIGATOR_SURVIVAL` und `NAVIGATOR_ELYTRA`.
- Eine Warnung für übrig gebliebene `navigator.*`-Schlüssel.

## Decisions

### 1. `NavigatorModule` baut das Aves-Inventar direkt, ohne eigene Registry oder Layout-Schicht

`NavigatorModule` hält einen `GlobalInventoryBuilder` (Titel `<yellow>Navigator`, `InventoryType.CHEST_1_ROW`). In `enable()` legt es das Layout an, registriert den Builder und meldet die Feder in Hotbar-Slot 4 an. In `disable()` meldet es den Builder wieder ab. Das Layout entsteht in einer Methode: `InventoryLayout.fromType(CHEST_1_ROW)`, jeder Platz zuerst eine graue Glasscheibe, dann je sichtbares Ziel `setItem(slot, icon, handler)`. Der Handler bricht den Klick ab (`ClickHolder.cancelClick()`), ruft `deliver.sendPlayer(...)` mit dem Task-Namen auf und schließt das Inventar. Das entspricht dem heutigen `toAvesLayout(...)`.

- **Built-in first**: Aves' `GlobalInventoryBuilder` und `InventoryLayout` decken ein gemeinsames Inventar mit festen Plätzen und Klick-Handlern vollständig ab. Die eigene Registry `NavigatorEntries` samt `View`, Versionszähler und `NavigatorLayout` war nur nötig, weil mehrere Quellen Einträge liefern sollten. Diese Quellen gibt es nicht (YAGNI). Verworfen: die Registry behalten und nur die Config-Lesung streichen. Dann blieben rund 600 Zeilen für einen einzigen Beitragenden.
- **SOLID**: SRP auf Modulebene. Der Navigator besitzt seine Ziele und sein Inventar. OCP wird bewusst aufgegeben: Ein neues Ziel ist eine Änderung am Navigator-Modul. Das ist der Zweck dieses Changes. DIP: Das Modul hängt über den Konstruktor von `Deliver` und `FeatureFlags` ab, nicht mehr von der statischen `Config`-Fassade.
- **Test**: Integration mit Cyano-Env. `NavigatorModuleTest` prüft Plätze, Glasscheiben, Weiterleitung je Ziel, dass ein Klick auf eine Glasscheibe nichts auslöst, und dass gesetzte `navigator.*`-Werte ignoriert werden. `NavigatorModuleLeakTest` prüft, dass 50× Öffnen keine neuen Listener und keine Spielerreferenzen erzeugt.
- **Logging/Metriken/Spans**: keine neuen.

### 2. Ziele als package-privates `enum Destination` im selben Paket

`enum Destination { ELYTRA_RACE, SURVIVAL, SLENDER, CREATIVE }` mit den Feldern `int slot`, `Material icon`, `String displayName` (MiniMessage, Werte wie heute in `application.yaml`), `String task` und `@Nullable String feature` (nur `SLENDER` = `NAVIGATOR_SLENDER`). Dazu eine reine statische Methode `List<Destination> visible(FeatureFlags)`, die ungegatete Ziele und Ziele mit aktiver Flag liefert. Das `ItemStack` baut das Modul einmal pro Layoutaufbau aus `icon` und `displayName`.

- **Built-in first**: Ein Java-`enum` ist das eingebaute Mittel für eine feste, benannte Menge. `Material` als Typ macht die Laufzeitprüfung „bekanntes Material“ überflüssig. Verworfen: den Record `NavigatorEntry` weiter nutzen. Er gehört zur entfallenden Registry und validiert zur Laufzeit, was der Compiler bzw. ein Unit-Test abdeckt.
- **SOLID**: SRP. Das enum hält nur Daten, die Sichtbarkeitsregel ist eine reine Funktion.
- **Test**: Unit ohne Server (`NavigatorDestinationTest`): Plätze wie in der Spec, paarweise verschiedene Plätze (ersetzt die Konfliktprüfung beim Start), `visible(...)` mit Fake-`FeatureFlags` bei Slender an/aus. Dazu `DefaultNavigatorFeatureFlagsTest` auf das enum umgestellt: Jede `feature` steht in den mitgelieferten `features.*` (ersetzt die Startprüfung unbekannter Flags).

### 3. Layout nur neu legen, wenn sich die sichtbaren Ziele ändern

Beim Benutzen der Feder berechnet das Modul `Destination.visible(featureFlags)` und vergleicht mit der zuletzt angewendeten Liste. Nur bei einer Abweichung setzt es `builder.setLayout(...)` und `builder.invalidateLayout()`. Danach öffnet es `builder.getInventory()`. Die Methode ist `synchronized`, weil Item-Events auf den Tick-Threads verschiedener Spieler laufen können. Das ist dieselbe Strategie wie heute in `NavigatorInventory#applyLayoutIfChanged()`, nur ohne Registry.

- **Built-in first**: Aves bietet keine Push-Benachrichtigung bei Flag-Änderungen, avaje-config liefert kein Ereignis pro Schlüssel an Module. Der Vergleich beim Öffnen ist der einfachste Weg und kostet einen `Config`-Lookup pro gegatetem Ziel.
- **Test**: Integration mit Cyano-Env (`NavigatorFeatureFlagTest`, bestehend): Slender erscheint und verschwindet nach Umschalten der Flag beim nächsten Öffnen, ohne dass das Modul neu startet.

### 4. Andockpunkt `navigator` aus der Modul-Plattform entfernen

Das Paket `app/.../module/navigator/` wird gelöscht, samt `NavigatorEntries`, `NavigatorEntry`, `NavigatorConflictException` und Tests. Entfernt werden außerdem `ModulePlatform#navigator()`, `ModuleContext#navigator()` samt Feld und Abmeldung in `onDisable`, der `validate(...)`-Aufruf in `ModuleRegistry#enableAll()` (inkl. `featureFlags`-Parameter des Builders, falls er nur dafür existiert) und die Bean-Verdrahtung in `Titan.java`. In den Tests fallen `ModuleHarness#navigator()`, `ModulePlatformFixture` und der Navigator-Teil von `ModuleRegistryTest`/`ModuleHarnessTest` weg.

- **SOLID**: ISP. Der Modul-Kontext bietet nur noch Andockpunkte mit echten Nutzern.
- **Test**: `./gradlew build` inkl. ArchUnit grün. `rg "module\.navigator|navigator\(\)" app` findet nichts mehr.

### 5. Konfiguration und Doku

Der Abschnitt `navigator:` fällt aus `app/src/main/resources/application.yaml`. Die Kommentare zu `features` und `config.watch` verweisen nicht mehr auf `navigator.entries` bzw. „re-read on every open“. `docs/lobby-modules.md` verliert den Andockpunkt `navigator` und beschreibt den Navigator als fest verdrahtetes Modul. `README.md` verliert den Beispiel-Eintrag.

- **Test**: `rg "navigator\.(title|entries)" app/src/main docs README.md` findet nichts.

## Risks / Trade-offs

- [Ein künftiges Feature will ein Ziel beisteuern] → Es trägt das Ziel im Navigator-Modul ein. Kommen später tatsächlich mehrere Quellen, lässt sich ein Andockpunkt dann mit echtem Bedarf wieder einführen.
- [Betreiber setzen `navigator.*` weiter] → `BREAKING CHANGE`-Footer und Doku. Die Werte sind wirkungslos, aber harmlos.
- [Permission-Gate braucht Sichtbarkeit pro Spieler, das gemeinsame Inventar kennt keinen Spieler] → Wird im Folge-Change entschieden, z. B. ein `GlobalInventoryBuilder` je Kombination sichtbarer Ziele. `Destination` und `visible(...)` lassen sich dafür um ein Permission-Feld und einen Spielerparameter erweitern.

## Migration Plan

1. Release ausrollen. Übrig gebliebene `navigator.*`-Schlüssel sind wirkungslos.
2. Danach `navigator.*` aus Betreiberdateien, Profilen und Env-Variablen entfernen.
3. Rollback: vorheriges Release.
