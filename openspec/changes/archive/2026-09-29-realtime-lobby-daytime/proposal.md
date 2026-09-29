# Proposal

## Why

`MapProvider` friert die Lobby auf Mittag ein. Spieler sehen dadurch nie Tag und Nacht, und die Lobby wirkt statisch. PR #217 wollte das mit Jahreszeiten, Sonnenstand-Strategie und `time.json` lösen, ist aber veraltet und für den Nutzen zu groß. Dieser Change ersetzt ihn durch das Kleinste, das trägt: die Tageszeit der Lobby folgt der echten Uhrzeit.

## What Changes

- Neues Column-Modul `features/daytime`: setzt die Instanzzeit einmal pro Sekunde linear aus der Wanduhr einer konfigurierten Zeitzone (06:00 = Sonnenaufgang, 12:00 = Mittag, 18:00 = Sonnenuntergang, 00:00 = Mitternacht).
- Der Minestom-Zeitzyklus der Lobby-Instanz wird angehalten, damit nur die Wanduhr die Zeit bestimmt.
- Zwei Schlüssel unter `daytime.*`: `daytime.enabled` (Abschalter) und `daytime.zone` (Standard `Europe/Berlin`). Beide wirken ohne Neustart.
- Ist das Modul abgeschaltet, steht die Lobby wieder auf Mittag.
- `MapProvider` bleibt unverändert: Sein Mittags-Stand ist der Rückfall für Varianten ohne dieses Modul.
- Kein PR-Ballast: keine Strategie-Schnittstelle, kein Sonnenstand, keine Jahreszeiten, keine `time.json`.

## Capabilities

### New Capabilities

- `lobby-daytime`: Die Tageszeit der Lobby folgt der echten Uhrzeit einer konfigurierbaren Zeitzone, lässt sich abschalten und verhält sich bei Zeitumstellungen nachvollziehbar.

### Modified Capabilities

_Keine._ Die Notiz `docs/spec-lobby-saison-events.md` bleibt unberührt.

## Impact

- **Code**: neues Modul `features/daytime` (wird von `settings.gradle.kts` automatisch erfasst), Standardwerte in `titan/defaults/daytime.yaml`. Kein bestehender Code ändert sich.
- **Abhängigkeiten**: keine neuen. `avaje-config` ist bereits im Katalog.
- **Tests**: Unit-Tests für die Zeitabbildung, ein Integrationstest mit Cyano-Env für das Modul, `ColumnArchitectureTest`.
- **Nutzertexte**: keine.
- **Betrieb**: neue optionale Schlüssel `daytime.enabled`, `daytime.zone`. Nichts zu migrieren.
- **Ersetzt**: PR #217 wird nach dem Merge dieses Changes als überholt geschlossen.

## Delivery

PR-Titel: `feat(daytime): follow the real wall clock in the lobby`

Nicht-Ziele, jeweils eigener Change: Sonnenstand-Abbildung (späteres `feat`), Jahreszeiten, Aktualisierung von `docs/spec-lobby-saison-events.md`.
