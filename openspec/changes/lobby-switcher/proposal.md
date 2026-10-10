# Proposal

## Why

Ein Spieler sieht in der Lobby heute nur die Portale mit ihren Zahlen (`portal`) und den Navigator mit festen Zielen. Welche anderen Lobbys gerade laufen und wie voll sie sind, erfährt er nicht; wer die Lobby wechseln will, muss den Navigator-Eintrag kennen oder ein Portal finden. Die Lobbys desselben CloudNet-Tasks sind austauschbare Instanzen derselben Lobby. Eine Liste mit Spielerzahlen, aus der man direkt in eine andere Instanz wechselt, macht das sichtbar und verteilt Spieler auf volle und leere Instanzen.

Die Zahlen gibt es bereits: Die Portal-Labels lesen sie über den `PlayerCounts`-Provider aus der CloudNet-Bridge. Diese Schnittstelle liefert aber nur Summen je Task, Gruppe oder Dienst. Eine Liste einzelner Lobbys fehlt, und mit ihr die Namen, nach denen die Lobby wechselt.

## What Changes

- Neue Column `features/lobbyswitcher` (Paket `net.onelitefeather.titan.feature.lobbyswitcher`, Scope `lobbyswitcher`).
- Ein Hotbar-Item (Slot 8, frei; Slots 0 und 4 sind belegt) öffnet ein Aves-Inventar mit allen laufenden Lobbys des CloudNet-Tasks der eigenen Lobby, sortiert nach Dienstnamen. Jeder Eintrag zeigt Namen und Spielerzahl (`online/max`).
- Die eigene Lobby ist markiert und nicht anklickbar. Volle Lobbys sind als voll gekennzeichnet und nicht anklickbar. Ein Klick auf eine andere beitretbare Lobby leitet den Spieler über den bestehenden `Deliver` an genau diesen Dienst weiter.
- Die Zahlen kommen aus derselben CloudNet-Anbindung wie die Portal-Labels. Die Anbindung wird um eine Auflistung der laufenden Dienste eines Tasks erweitert; es entsteht kein zweiter Zugriffspfad.
- Das offene Inventar aktualisiert sich periodisch und nur, solange es offen ist.
- Feature-Flag `LOBBYSWITCHER` (Standard `true`, wirkt beim Start, Umschalten braucht Neustart). Ohne CloudNet-Profil (lokale Variante) gibt es weder Item noch Inventar.
- Telemetrie: Spans `lobbyswitcher.open` und `lobbyswitcher.select`, Zähler `titan.lobbyswitcher.selections{result}`.
- Konfiguration `lobbyswitcher.refreshSeconds` (Standard `5`, 1 bis 3600).
- Texte in `titan/lobbyswitcher/messages_{en,de}.properties`, Englisch als Fallback.

## Non-Goals

- Kein Wechsel zwischen Tasks (z. B. zu Survival oder Build). Das bleibt Aufgabe des Navigators und der Portale.
- Keine Paginierung. Mehr als 54 Lobbys (sechs Zeilen) werden gekürzt und protokolliert.
- Keine Warteschlange und keine Reservierung eines Platzes in einer Lobby. Volle Lobbys sind nicht beitretbar; der Server bleibt die letzte Instanz.
- Kein Befehl (`/lobbys`). Der Einstieg ist nur das Hotbar-Item.
- Keine Änderung am Portal-Verhalten und an den Portal-Zahlen.

## Capabilities

### New Capabilities
- `lobby-switcher`: Anzeige der laufenden Lobbys des eigenen Tasks mit Spielerzahlen und Wechsel per Klick.

### Modified Capabilities
- Keine. Die Anbindung an CloudNet (`PlayerCounts`, Bridge) bekommt eine zusätzliche Auflistung, aber das Verhalten der bestehenden Anforderungen von `lobby-portal-labels` bleibt unverändert.

## Impact

- **Code:**
  - `features/lobbyswitcher` (neu): Modul, Item-Bean, Inventar, Einträge, Aktualisierung, Telemetrie, Einstellungen, Texte.
  - `core`: Auflistungsmethode am Zähler-Provider (`PlayerCounts`) mit `ServiceCount`-Record, und Identität der eigenen Lobby (Task, Dienstname).
  - `common/deliver`: `HolderPlayerCounts` und `TitanPlayerCountLookup` reichen die Auflistung durch.
  - `bridge`: `TitanBridgePermissionExtension` implementiert die Auflistung über dieselben `servicesByTask`-Aufrufe und `ServiceReadings`, und liefert die eigene Identität.
  - `runtime`: `PlatformBeans` nur im CloudNet-Profil (wie bisher `PlayerCounts`).
  - Kein Refactor von `LabelRefresh` (design.md D5b, Q3): der Switcher hat eine eigene kleine Aktualisierung nach demselben Muster.
- **Konfiguration:** `features/lobbyswitcher/src/main/resources/titan/defaults/lobbyswitcher.yaml` mit `lobbyswitcher.refreshSeconds: 5`. Der Flag-Abschnitt `features` wandert aus `features/navigator/.../navigator.yaml` nach `runtime/.../titan/defaults/features.yaml` und bekommt `LOBBYSWITCHER: true`, weil `DefaultsMerger` nur eine Datei pro Top-Level-Abschnitt erlaubt.
- **Abhängigkeiten:** keine neuen Bibliotheken. Aves ist für Columns bereits Pflicht.
- **Nutzertexte:** neu, Englisch und Deutsch.
- **Spielerverhalten:** nur in der CloudNet-Variante mit eingeschaltetem Flag (Standard); Uhr (`CLOCK`) auf Slot 8 bei Lobby-Beitritt.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(lobbyswitcher): show other lobbies with player counts and switch between them`

Dieser PR ist ein einziger Typ (`feat`). Dokumentation (README, `docs/lobby-modules.md`) gehört in denselben PR, ist aber kein eigener Typ. Das Verschieben des Abschnitts `features` nach `runtime` ist ein eigener `refactor(runtime)`-Commit im selben PR, weil `DefaultsMerger` ohne ihn das neue Flag nicht zulässt; der Squash-Commit trägt dann trotzdem nur den Titel-Typ `feat`.
