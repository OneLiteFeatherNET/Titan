# Proposal

## Why

Ohne laufendes CloudNet lieferte `DeliverProvider.create()` bisher `NoopDeliver`: Ein Klick auf ein Navigator-Ziel tat schlicht nichts. Lokal - beim manuellen Testen der Lobby auf dem eigenen Rechner, ohne CloudNet-Wrapper - sah das aus wie ein kaputter Navigator, obwohl nur die Weiterleitung fehlt, die es ohne CloudNet gar nicht geben kann. Wer den Navigator lokal abnehmen wollte, konnte Klicks nicht von einem stillen Fehler unterscheiden.

**Auslieferung:** `feat(common): report navigator deliveries in local runs` (Conventional Commit, PR-Titel). Nicht breaking.

## What Changes

- `NoopDeliver` entfällt, ersatzlos ersetzt durch die neue `DebugDeliver`.
- `DeliverProvider.create()` liefert ohne CloudNet jetzt `DebugDeliver` statt `NoopDeliver`. Mit CloudNet bleibt `MessageChannelDeliver` unverändert; es gibt keinen Schalter und keine neue Property.
- `DebugDeliver` schickt dem klickenden Spieler beim Klick auf ein Navigator-Ziel eine Inline-MiniMessage-Chatzeile, die Art (Task oder Server) und Ziel unverändert (nicht als MiniMessage geparst) nennt, und schreibt dazu eine `INFO`-Zeile ins Log.
- Es entstehen keine neuen Übersetzungsdateien: Das Projekt hat keine i18n-Bundles, Inline-MiniMessage ist die bestehende Konvention (siehe `TitanMiniMessageImpl`, `<prefix>`-Tag).

## Capabilities

### Modified Capabilities
- `lobby-navigator`: Die Anforderung „Auswahl eines Ziels leitet weiter“, Szenario „Weiterleitung ohne Cloud“, gilt weiterhin (keine Weiterleitung, kein Fehler), ergänzt um sichtbares Feedback: Der Spieler bekommt eine Chatnachricht mit dem angeklickten Ziel, und die Lobby loggt die würde-gewesene Weiterleitung.

## Impact

- **Code:** `common/src/main/java/net/onelitefeather/titan/common/deliver/`
  - Neu: `DebugDeliver.java`
  - Entfernt: `NoopDeliver.java`
  - Geändert: `DeliverProvider.java` (liefert `DebugDeliver` statt `NoopDeliver`)
- **Tests:** Neu `common/src/test/java/net/onelitefeather/titan/common/deliver/DebugDeliverTest.java` (Chatnachricht, Log-Zeile, Null-Sicherheit, unverändertes Zeigen des Ziels für beide `DeliverComponent`-Arten).
- **Konfiguration:** keine. Kein neuer Schalter, keine neue Property.
- **Abhängigkeiten:** keine neuen.
- **Nutzertext:** eine neue Chatzeile (Inline-MiniMessage, keine Bundles vorhanden, siehe What Changes).
- **Laufzeit:** betrifft nur Läufe ohne CloudNet (lokal, Tests, AOT-Training). Mit CloudNet ändert sich nichts.
