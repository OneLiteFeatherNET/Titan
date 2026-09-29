# Proposal

## Why

Mit unquotiertem `from: 2026-09-29T13:56:00` bricht die Lobby den Start mit `seasons.halloween.from: is required` ab, obwohl der Wert dasteht. `avaje-config` 5.2 lädt YAML über SnakeYAML, sobald es im Klassenpfad liegt (in `apps/cloudnet` transitiv der Fall). SnakeYAML macht aus dem unquotierten Wert ein `java.util.Date`, und der Lader übernimmt nur Text, Zahlen und Wahrheitswerte: Der Schlüssel verschwindet still. README, `docs/lobby-modules.md` und die Vorlage `season.yaml` zeigen genau diese unquotierte Form. Die Tests haben es nicht gefunden, weil `features/season` kein SnakeYAML im Test-Klassenpfad hat und `Config.setProperty` benutzt.

## What Changes

- Datum-Zeit-Werte (`from`, `to`) MÜSSEN in der YAML-Datei in Anführungszeichen stehen; das wird dokumentiert statt still hingenommen.
- Fehlt `from` oder `to` bei einer Saison, die sonst Schlüssel hat, nennt die Startfehlermeldung den Schlüssel und den Hinweis auf Anführungszeichen, z. B. `seasons.halloween.from: is required (quote date-times: from: "2026-12-01T00:00:00")`. Derselbe Hinweis steht in der WARN-Zeile beim Live-Lesen.
- Alle Beispiele in README, `docs/lobby-modules.md` und `season.yaml` zeigen die quotierte Form.
- `features/season` bekommt SnakeYAML (`libs.snakeyaml`) im Test-Laufzeit-Klassenpfad; ein Regressionstest lädt echte YAML-Dateien (quotiert: gelesen; unquotiert: Startfehler mit Hinweis).
- Verworfen: `Date` selbst annehmen. `avaje-config` bietet dafür keinen Einhängepunkt, ein eigener YAML-Lader wäre ein zweiter Konfigurationsweg, und die Umrechnung von `Date` (UTC) auf die lokale Zeit in `seasons.zone` wäre fehleranfällig.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-seasons`: Die Anforderung „Ungültige Saison-Konfiguration verhindert den Start“ bekommt ein Szenario für einen unquotierten Datum-Zeit-Wert und verlangt den Quoting-Hinweis in der Fehlermeldung.

**Abhängigkeit:** `openspec/specs/lobby-seasons` gibt es auf `main` noch nicht, weil der Change `seasonal-lobby-world` nicht archiviert ist. Ein `MODIFIED`-Delta lässt sich beim Archivieren nur auf eine vorhandene Anforderung anwenden. `seasonal-lobby-world` MUSS daher vor diesem Change archiviert werden (`/opsx:archive seasonal-lobby-world`); das ist die erste Aufgabe in `tasks.md`.

## Impact

- **Code**: `SeasonConfigReader` (Meldung in `value(...)` für `from`/`to`), `features/season/build.gradle.kts` (`testRuntimeOnly(libs.snakeyaml)`), neuer Test in `features/season`.
- **Abhängigkeiten**: keine neue Produktionsabhängigkeit; `libs.snakeyaml` (2.7, wie in `common` und `navigator`) nur im Test-Klassenpfad von `features/season`.
- **Doku**: README (Abschnitt „Seasonal lobby world“), `docs/lobby-modules.md` (Abschnitt „Saison-Welt“), Kommentar in `features/season/src/main/resources/titan/defaults/season.yaml`.
- **Nutzertexte**: keine; nur Log- und Fehlerzeilen für Betreiber (Englisch).
- **Betrieb**: Bestehende `application.yaml` mit unquotierten Werten funktionieren weiterhin nicht, melden aber jetzt den Grund; Betreiber setzen Anführungszeichen.
- **Berührt**: `seasonal-lobby-world` (Archivierung zuerst, siehe oben).

## Delivery

PR-Titel: `fix(season): explain that season date-times must be quoted`
