# Proposal

## Why

Ein Spielmodus, der nur über den CloudNet-Task `SkyGrid` erreichbar ist, fehlt im Navigator. Spieler, die dort spielen dürfen, müssen ihn bisher kennen oder anderswo suchen. Der Navigator ist der Ort, an dem Spieler Server wählen; dort soll SkyGrid für Spieler mit Recht erscheinen, und für alle anderen bleibt das Menü unverändert.

## What Changes

- **Neuer Eintrag**: `SKYGRID` in `Destination` nach `BUILD`, Gruppe `SERVER`, Symbol Diamanterz, Verlauf Hellblau nach Hellcyan, Name „SkyGrid“, Weiterleitungsziel Task `SkyGrid`, Recht `titan.navigator.skygrid`.
- **Serverreihe**: Die Reihe zeigt bis zu vier Server, zentriert. Hat ein Spieler das Recht, steht SkyGrid nach Build; Survival, Creative und Build rücken dann einen Platz nach links.
- **Beschreibung**: neuer Schlüssel `titan.navigator.description.skygrid` in beiden Bundles („Überleben im Block-Raster“ bzw. „Survive in a grid of blocks“).
- **Klick**: wie bei Build prüft die Lobby das Recht beim Klick erneut und leitet nur bei Erteilung weiter.
- **Menü-Varianten**: Das Recht ist ein weiteres Bit der Rechte-Kombinationen. Die Zahl der Hauptmenü-Inventare steigt von 64 auf 128 (bei zwei Sprachen). Die Anforderung „Menü-Varianten bleiben zweiunddreißig geteilte Inventare“ wird hier nicht geändert; ihre Zahl stimmt bereits seit früheren Änderungen nicht mehr mit dem Code überein (96 Inventare beim Start) und ist eine Folgearbeit.

## Capabilities

### New Capabilities

_Keine._

### Modified Capabilities

- `lobby-navigator`: Neue Anforderung „Das Ziel SkyGrid erscheint nur mit Recht“ (ADDED).

## Impact

- **Code**: `features/navigator`: `Destination` (ein Eintrag), `NavigatorMessages` (Konstante und Listen), `messages_de.properties` und `messages_en.properties`. Die Platzrechnung in `NavigatorLayout` bleibt unverändert; sie zentriert die Serverreihe für eine beliebige Anzahl.
- **Tests**: Tests, die die Zahl der Navigator-Inventare (96 zu 160) zählen, werden angepasst; neue Tests für Sichtbarkeit, Platz und Klick.
- **Spec-Zahl**: Die Zahl in `lobby-navigator` („zweiunddreißig“) bleibt bis zu einer Folgeänderung falsch, wie schon vor dieser Änderung.
- **Konfiguration**: keine. `navigator.yaml` kennt keine Ziele, daher kein Eintrag.
- **Dokumentation**: Die Rechteliste in `README.md` (Abschnitte zu Navigator und Rechten) nennt SkyGrid nicht. Die Anpassung ist nicht Teil dieser Änderung.
- **Abhängigkeiten**: keine neuen.
- **Nicht brechend**: Ohne das Recht ändert sich nichts. Mit dem Recht verschieben sich die Plätze der Server nur für Spieler, die SkyGrid sehen.

## Delivery

Typ `feat`, Scope `navigator`, nicht brechend.

PR-Titel: `feat(navigator): add a permission-gated skygrid entry`

Nicht-Ziele: kein Staging-Eintrag für SkyGrid, keine Chat-Nachricht bei verpasster Weiterleitung, keine Konfiguration des Eintrags, keine Änderung der README-Rechteliste.
