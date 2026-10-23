# Proposal

## Why

Der Navigator kennt Minispiele mit Schnellbeitritt: ElytraRace ist für alle sichtbar, Slender liegt hinter dem Recht `titan.navigator.slender`. Ein drittes Minispiel „Bounce“ soll dieselbe Bedienung bekommen, aber nur für Spieler sichtbar sein, denen die Gruppe das Recht `titan.navigator.bounce` erteilt. So steuert LuckPerms die Sichtbarkeit pro Spieler, und niemand sieht einen Eintrag, dessen Dienst er nicht betreten darf.

## What Changes

- Neuer Modus **Bounce** im Navigator, gebunden an das Recht `titan.navigator.bounce`. Ohne erteiltes Recht (`NOT_SET` oder `DENIED`) erscheint er nicht, und auf seinem Platz liegt eine graue Glasscheibe.
- Platz 13 in der zweiten Reihe, zwischen ElytraRace (11) und Slender (15). Die Plätze der übrigen Ziele bleiben unverändert.
- CloudNet-Task `Bounce`, Symbol `Material.SLIME_BALL`, Anzeigename mit Farbverlauf von Grün nach Türkis (`<gradient:#00FA9A:#40E0D0>`).
- Bounce ist ein Minispiel: Linksklick tritt schnell der besten bereiten Runde bei, Rechtsklick öffnet die Serverauswahl. Die Klickhinweise in der Lore und die Spielerzahl folgen den Regeln von ElytraRace und Slender.
- Die Menü-Varianten wachsen von vier auf acht Teilmengen je Sprache (alle Kombinationen aus den drei Rechten), also von acht auf sechzehn geteilte Inventare beim Start.
- **Nicht brechend**: Bestehende Ziele, Plätze, Rechte und Klicks ändern sich nicht. Ohne das neue Recht sieht kein Spieler etwas Neues.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: Bounce wird als drittes rechtegebundenes Minispiel aufgenommen. Die Anforderungen zu Zielen und Plätzen, Klickhinweisen, Rechte-Bindung, Namensfarbe und Menü-Varianten (sechzehn Inventare) ändern sich entsprechend. Siehe `specs/lobby-navigator/spec.md`.

## Impact

- **Code**: `features/navigator` (`Destination` mit neuem Eintrag `BOUNCE`, `ServerPicker.DESTINATIONS`, `NavigatorModule` und `SharedNavigator` durch die größere Menge der Varianten, Tests im Modul). Keine Änderung an `runtime`, `apps/cloudnet` oder `buildSrc`.
- **Abhängigkeiten**: keine neuen Laufzeit- oder Build-Abhängigkeiten.
- **Betrieb**: Der CloudNet-Task `Bounce` muss existieren. Das Recht `titan.navigator.bounce` wird in LuckPerms den Gruppen erteilt, die Bounce sehen sollen. Ohne Recht bleibt der Platz grau. Die lokale Variante (`DenyAllPermissionService`) zeigt Bounce niemandem.
- **Nutzertexte**: Der Name „Bounce“ ist ein Eigenname und in beiden Sprachen gleich. Die Klickhinweise nutzen die bestehenden Schlüssel für Minispiele; neue Schlüssel entstehen nicht.
- **Speicher**: Das Navigator-Modul legt sechzehn statt acht kleine geteilte Inventare einmal beim Start an.

## Delivery

PR-Titel (Conventional Commit, Typ `feat`, Scope `navigator`, nicht brechend):

`feat(navigator): add bounce behind a permission`
