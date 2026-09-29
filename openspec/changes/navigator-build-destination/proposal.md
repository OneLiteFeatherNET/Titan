# Proposal

## Why

Teammitglieder des Build-Teams müssen vom Navigator aus auf die laufenden Build-Server kommen, ohne dass Spieler ohne dieses Recht davon etwas sehen. PR #219 („show build servers in navigator“) wollte das mit einer Liste einzelner CloudNet-Dienste, FeatureGate und einem `GuardedDeliver` lösen. Er hängt gestapelt an #216, das nie in `main` gemergt wurde. Dieser Change ersetzt ihn durch das Kleinste, das trägt: ein festes Ziel, das nur mit Recht erscheint, und CloudNets Task-Routing wählt den Server.

## What Changes

- Neues festes Ziel `BUILD` im `Destination`-Enum des Navigators (im Code, nicht in der Konfiguration): Platz 7, Weiterleitung an den CloudNet-Task `Build` (getrennt vom Task `MemberBuild` hinter dem öffentlichen Creative auf Platz 8). Kein Feature-Flag.
- Sichtbar nur, wenn `PermissionService.check(uuid, "titan.navigator.buildserver")` `ALLOWED` ergibt (`NOT_SET` und `DENIED` blenden aus).
- Zwei geteilte Aves-Inventare, je einmal im Modul-Lebenszyklus gebaut, angemeldet und beim Herunterfahren abgemeldet: das öffentliche (unverändert) und das Team-Inventar (öffentliche Ziele plus `BUILD`). Beim Öffnen entscheidet das Recht, welches der Spieler bekommt.
- Klick auf `BUILD` prüft das Recht erneut; ist es weg, gibt es keine Weiterleitung, das Inventar schließt, keine Chat-Nachricht.
- Die Navigator-Column verlangt zusätzlich `PermissionService` (aus `core`; `runtime` liefert ihn, `DenyAllPermissionService` als Fallback).
- Variante `local` ohne Permission-Plattform: `NOT_SET`, also sieht niemand `BUILD`; nichts weiter zu ändern.
- Kein Anzeigen einzelner Server, keine CloudNet-Diensterkennung, kein `:bridge`-Halter, kein FeatureGate, kein `GuardedDeliver`. „Nur laufende Server“ (US-5.04) leistet CloudNets Task-Routing.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: neue Anforderung „Das Ziel Build erscheint nur mit Recht“; die Anforderung „Navigator-Ziele sind im Navigator-Modul festgelegt“ ändert ihren Wortlaut, weil Platz 7 für Berechtigte kein Glas mehr ist und das Ziel Build zu den festen Zielen zählt.

`lobby-permissions` bleibt unverändert: Die Anforderungen dort regeln, wie der Dienst Rechte liefert (inklusive „nicht gesetzt gilt als nicht erteilt“ und Verhalten ohne Plattform), nicht, wer ihn wofür abfragt.

## Impact

- **Code**: `features/navigator` (`Destination`, `NavigatorModule`, `package-info.java` mit `requires += PermissionService.class`); Tests und Fixtures dort (`NavigatorFixture` bekommt einen `PermissionService`-Parameter, neue `FakePermissionService`). `docs/lobby-modules.md` (Tabelle: `navigator` verlangt zusätzlich `PermissionService`).
- **Abhängigkeiten**: keine neuen; `PermissionService` liegt in `core`, die Column hängt bereits daran.
- **Nutzertexte**: ein neuer Anzeigename „Build“ im Navigator, wie die übrigen Ziele als MiniMessage-Literal im Code (kein i18n, wie bei den bestehenden Zielen); keine Chat-Nachricht.
- **Betrieb**: der CloudNet-Task `Build` muss existieren; das Recht `titan.navigator.buildserver` wird in LuckPerms an die Build-Gruppe vergeben.
- **Ersetzt**: PR #219 wird nach dem Merge als überholt geschlossen.

## Delivery

PR-Titel: `feat(navigator): show the build server destination to the team`

Nicht-Ziele: Liste einzelner Build-Server, Chat-Meldung bei entzogenem Recht, Konfigurierbarkeit von Recht, Platz oder Task, Feature-Flag für `BUILD`, weitere rechtegebundene Ziele.
