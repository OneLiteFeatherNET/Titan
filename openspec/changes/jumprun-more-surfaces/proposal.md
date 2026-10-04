# Proposal

## Why

Ein Lauf zeigt bisher sechs Formen: Vollblock, Falltür, Platte, Zaun/Mauer, Scheibe/Gitter und Pfosten. Nach einigen Dutzend Sprüngen wiederholt sich das Bild, und die Schwierigkeit hat kaum Stellschrauben jenseits von Lückenbreite und Höhe. Minecraft bietet viele weitere begehbare Formen, die sich unterschiedlich anfühlen: Treppen mit zwei Stufenhöhen, ganz flache Teppiche und Schneeschichten sowie kleine Dinge wie Köpfe, Blumentöpfe und Kerzen, auf denen man nur auf einem schmalen Fleck steht.

Zwei Lücken im Code verhindern, diese Formen einfach als weitere `Surface`-Konstanten zu ergänzen:

- Eine Treppe hat zwei Oberkanten (0,5 und 1,0). `Course.isStandingOn` kennt je Block genau eine Oberkante und prüft die Position gegen die ganze Zelle mit 0,3 Toleranz.
- Köpfe, Töpfe und Kerzen kollidieren nur in einem Teil der Zelle. Die Zellen-Toleranz würde Läufer als gelandet zählen, die neben dem Block in der Luft stehen.

## What Changes

- Sechs neue Formen als weitere Landeflächen, jede mit Palette, Schwierigkeitskosten und Freischaltung je Modus:
  - Treppe (`stairs`): zufällige Ausrichtung, untere Hälfte. Landen zählt auf beiden Stufen.
  - Teppich (`carpet`) und Schneeschicht (`snow`): sehr niedrige Oberkante auf voller Fläche.
  - Kopf (`head`), Blumentopf (`flower_pot`) und Kerze (`candle`): schmale Fläche, niedrige Oberkante.
- Eine Landefläche besteht künftig aus einer oder mehreren Stufen (Oberkante plus Rechteck in der Zelle). Die Landeprüfung des Kurses, der Absturz-Schwellwert und die Erreichbarkeit leiten sich daraus ab. Die bisherigen sechs Formen behalten genau ihr heutiges Verhalten (eine Stufe über die ganze Zelle).
- Die Palettenprüfung beim Start verlangt zusätzlich, dass alle Materialien einer Form dieselbe Grundfläche haben, nicht nur dieselbe Höhe.
- Neue Konfigurationsabschnitte `jumprun.palettes.stairs`, `carpet`, `snow`, `head`, `flower_pot`, `candle` mit Standardwerten.
- Köpfe zeigen Spielerköpfe des Teams: `jumprun.heads.profiles` ist eine Liste von Spieler-UUIDs, live gelesen wie die Paletten. Jeder Kopf-Block ist ein `player_head` mit einem zufällig gezogenen Profil aus der Liste, im Fake-Block, in der fallenden Darstellung und in der Umrandung. Ist die Liste leer, gelten die einfachen Köpfe der Palette `head`.
- Die Skins löst ein austauschbarer Auflöser (`HeadSkins`) einmal beim Lesen der Liste auf und hält sie im Speicher; Tests nutzen eine Attrappe, kein Netz.
- Falleffekt, Darstellung und Umrandung der übrigen neuen Formen gelten unverändert, weil sie das Material (den Blockzustand) des Laufblocks anzeigen. Köpfe mit Profil brauchen einen eigenen Weg dafür: ein Blockdisplay kann kein Profil tragen.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-jumprun`: „Schwierigkeit steigt mit dem Score“ und „Schwierigkeitsmodi“ nennen die neuen Formen und ihre Freischaltung. „Optische Vielfalt der Blöcke“ listet die neuen Palettenabschnitte und die Grundflächenprüfung. Neu ist die Anforderung „Landen auf Formen mit mehreren Oberkanten und schmaler Fläche“.

## Impact

- **Code:** nur `features/jumprun`: `Surface`, `Placement`/`CourseBlock`, `Jump`, `Course` (Landeprüfung, Absturz-Schwellwert), `CourseGenerator` (Ausrichtung und Kopfprofil ziehen), `FakeBlocks`/`AnimatedBlock`/`Outline` (Kopf mit Profil), `JumprunConfig` (Profilliste), neuer `HeadSkins`, `Mode` (Freischaltung), `JumprunSettings` (Prüfung), `JumprunModule`/`Palettes` (keine neue Verdrahtung), Standard-Palette `titan/defaults/jumprun.yaml`.
- **Abhängigkeiten:** keine neuen.
- **Konfiguration:** sechs neue Abschnitte unter `jumprun.palettes`. Betreiber-Overrides, die nur einzelne Materialien setzen, bleiben gültig, weil die Standardwerte die neuen Abschnitte mitbringen.
- **Nutzertexte:** keine.
- **Netz:** Beim Lesen der Profilliste fragt die Lobby die Skins der UUIDs einmal bei Mojang ab (nicht auf dem Tick-Thread, Ergebnis im Speicher). Ohne Netz oder ohne gültigen Eintrag gelten die einfachen Köpfe.
- **Spielerverhalten:** mehr Abwechslung ab Score 10 (Medium) bzw. 5 (Hard); Easy bleibt unverändert. Läufe sind nicht schwerer als vorher, die Kosten der neuen Formen liegen im bisherigen Bereich.
- **Abgrenzung:** Klettern (Leitern, Ranken) ist ein eigener Change, `jumprun-climbing`.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(jumprun): add stairs, carpet, snow, heads, flower pots and candles as landing shapes`
