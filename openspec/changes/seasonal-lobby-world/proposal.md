# Proposal

## Why

Die Lobby soll zu Events und Jahreszeiten anders aussehen (Winter, Halloween, ...). PR #225 ("feat: run seasons from configuration") wollte das mit JSON-definierten Effekten (Blöcke, Displays, Sounds, Navigator-Icons, Chat-Präfix) über einen Undo-Stapel auf der laufenden Welt lösen. Der PR hängt gestapelt an #219 und #216, die nur in den Branch `docs/lobby-season-spec` gemergt wurden, nie in `main`, und braucht FeatureGate/Togglz und Release-Stufen, die es auf `main` nicht gibt. Dieser Change ersetzt ihn durch das Kleinste, das trägt: Eine Saison ist eine eigene Welt, die das Build-Team baut, und die Lobby startet in Saisonzeit in dieser Welt neu.

## What Changes

- Neue Column `features/season` (nur `apps/cloudnet`): liest `seasons.<id>.*`, bestimmt die Welt der aktiven Saison und veranlasst einen Neustart, sobald sie von der gestarteten Welt abweicht.
- Eine Saison ist ein eigenes Weltverzeichnis unter `worlds/` mit eigener Map-Datei (Spawn), gebaut über den Setup-Server. Keine Effekte auf einzelnen Blöcken.
- Konfiguration je Saison: `seasons.<id>.world`, `.from`, `.to` (lokales Datum mit Uhrzeit), `.enabled` (Abschalter, live über die Konfigurationsüberwachung).
- Weltwahl beim Start: neue Schnittstelle `LobbyWorldChoice` in `core`; `PlatformBeans` reicht die gewählte Welt an `MapProvider`/`MapPool` weiter. Ohne Saison-Column oder ohne aktive Saison gilt wie heute `-DTITAN_LOBBY_MAP` bzw. `world`.
- Kein Umschalten zur Laufzeit: Jede Minute vergleicht die Column die gewünschte Welt mit der gestarteten. Weichen sie ab, ist ein Neustart vorgemerkt; nur bei leerer Lobby wird `MinecraftServer.stopCleanly()` aufgerufen (geprüft im Minutentakt und beim Verlassen eines Spielers). CloudNet startet den Dienst neu, die Weltwahl greift. Saisonende und Abschalter nutzen denselben Weg.
- **Akzeptierter Kompromiss:** Es gibt keine Obergrenze für das Warten. Eine dauerhaft belegte Lobby verzögert den Wechsel; Betreiber können das vorhandene `/stop` nutzen.
- Die Variante `local` schließt die Column aus (`titanVariant { exclude("season") }`), weil kein Supervisor sie neu startet. Sie verhält sich wie heute.

## Capabilities

### New Capabilities

- `lobby-seasons`: Die Lobby startet in der Welt der aktiven Saison, merkt einen Neustart vor, sobald sich die gewünschte von der gestarteten Welt unterscheidet, und stoppt nur bei leerer Lobby.

### Modified Capabilities

- `app-variants`: Die Anforderungen „Je Betriebsumgebung eine startbare Variante“ und „Varianten verhalten sich wie die bisherige Lobby“ verlangen heute dieselben Columns und Funktionen in `cloudnet` und `local`; `season` gibt es nur in `cloudnet`.

## Impact

- **Code**: neues Modul `features/season` (von `settings.gradle.kts` automatisch erfasst); neue Schnittstelle in `core`; `PlatformBeans.mapProvider(...)` und `MapProvider`/`MapPool` in `common`/`runtime` nehmen einen optionalen Weltnamen entgegen; `apps/local/build.gradle.kts` schließt die Column aus.
- **Abhängigkeiten**: keine neuen (`avaje-config`, Minestom-`Scheduler`, `java.time.Clock` sind da).
- **Tests**: Unit-Tests für Auswahl und Entscheidung (ohne Server), Integrationstest mit Cyano-Env für Minutentakt und Disconnect, `ColumnArchitectureTest`, Starttest je Variante.
- **Nutzertexte**: keine; nur Log-Zeilen für Betreiber (Englisch).
- **Betrieb**: neue optionale Schlüssel `seasons.*`. Saisonwelten müssen vorab als `worlds/<name>/` mit Map-Datei auf dem Dienst liegen. Ein Saisonwechsel kostet einen Neustart der Lobby.
- **Ersetzt**: PR #225 wird nach dem Merge als überholt geschlossen.
- **Berührt**: der offene Change `optional-extensions-bootstrap` ändert ebenfalls `apps/*/build.gradle.kts` (andere Zeilen), siehe design.md.

## Delivery

PR-Titel: `feat(season): restart the lobby into a seasonal world during its window`

Nicht-Ziele, jeweils eigener Change: Navigator-Icons, Chat-Präfix, Block-/Display-/Sound-Effekte, Release-Stufen/Vorschau-Recht, `/season list`, Spielerhinweis, Saisonen in der Variante `local`.
