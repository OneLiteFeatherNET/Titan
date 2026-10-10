# Tasks

## Execution Plan

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | prerequisites | 1.1 | sonnet | `openspec/**`, Git/GitHub (lesen) | Produktionscode, Tests, Doku |
| 2 | common-read | 2.1–2.2 | sonnet | `common/src/main/java/**/map/MapProvider.java`, `common/src/test/**` | `setup/**`, `core/**`, `features/**`, `apps/**`, `buildSrc/**` |
| 3 | setup-copy | 3.1–3.6, 4.1–4.3 | sonnet | `setup/src/**`, `README.md` | `common/**`, `core/**`, `features/**`, `apps/**`, `buildSrc/**` |
| 4 | verify | 5.1 | haiku | read-only | alles |
| 5 | pr | 6.1 | sonnet | Git/GitHub | Code |

Wave 3 beginnt erst nach Wave 2 (nutzt `activeMap()`/`readMap`). Innerhalb von Wave 3 sind 3.x nacheinander (gleiche Dateien).

## 1. Voraussetzungen

- [x] 1.1 PR #329 (`feat/setup-portals`) ist in `main` gemergt (`gh pr view 329 --json state` zeigt `MERGED`), und `setup-portal-command` ist archiviert (`/opsx:archive setup-portal-command`), sodass `openspec list --specs` die Capability `setup-portals` zeigt; sonst lässt sich das `ADDED`-Delta dieses Changes nicht anwenden. Branch danach von aktuellem `origin/main` neu abzweigen.

## 2. Quelle ohne Weltwechsel lesen (`common`, unit)

- [x] 2.1 Test zuerst (unit, F.I.R.S.T., zwei `@TempDir`-Welten, kein Server-Start über Cyano-`Env` wie `MapProviderPortalTest`): `readMap` liest die Portale einer zweiten Welt, liefert leer ohne `map.json`, wirft `IllegalStateException` bei ungültigem JSON oder ungültigen Portalen; `activeLobby()` und `activeMap()` bleiben unverändert; die Quell-Datei ist danach byte-gleich; rot
- [x] 2.2 `MapProvider.activeMap()` und `MapProvider.readMap(MapEntry)` ergänzen, `readLobbyData` ruft `readMap(activeMap())` (ein Lesepfad); Test aus 2.1 grün, bestehende `MapProviderPortalTest`/`GsonFileHandlerLoadTest` grün, `./gradlew :common:build`

## 3. Kopieren und save-all (`setup`, unit)

- [x] 3.1 Charakterisierungstest für `PortalEditor.save` (Saved/Updated/Pending/Rejected, Position beim Ersetzen) falls nicht schon durch `PortalEditorTest`/`PortalEditorValidatorParityTest` gedeckt; dann Refactoring von `save` in `stage(...)` ohne Verhaltensänderung; alle bisherigen Tests bleiben grün
- [x] 3.2 Test zuerst: `saveAll` (Alle gültig, Teilweiser Fehlschlag, Unvollständig, Ersetzen wie `save` an gleicher Position, keine Entwürfe, Entwürfe anderer Spieler unberührt, genau ein `store.save`-Aufruf, Paritätstest gegen nacheinander ausgeführtes `save`, bei Ausnahme im Speichern bleiben alle Entwürfe offen; `InMemoryPortalStore` mit Zähler, frische Fixtures); rot, dann `PortalEditor.saveAll` und `SaveAllResult`; grün
- [x] 3.3 Test zuerst: `PortalEditor.adopt(player, portal)` (legt Entwurf mit den Werten an; `false`, wenn der Spieler die Id schon als Entwurf hat, Entwurf unverändert; lehnt reservierte Ids ab); `RESERVED_IDS` um `copy` und `save-all` erweitern (`create copy` und `create save-all` → `Invalid`); grün
- [x] 3.4 Test zuerst: `PortalCopier` mit `InMemoryPortalSources` (Kopie legt Entwürfe an und lässt den Store unberührt, Überschreiben wird als ersetzend gemeldet, offener Entwurf bleibt und wird übersprungen gemeldet, unbekannte Welt mit Liste der verfügbaren, Quelle gleich Ziel auch bei anderer Groß-/Kleinschreibung, Quelle ohne Portale, `PortalSourceException` → `Unreadable`, nicht übernehmbare Id übersprungen); WARN-Zeile bei unlesbarer Quelle über aufgefangenen Appender geprüft; dann `PortalSources`, `PortalSourceException`, `CopyResult` (`Copied` trägt die übernommenen Portale), `PortalCopier`; grün
- [x] 3.5 Test zuerst: `PortalMessages` für `CopyResult` und `SaveAllResult` (Ids, Ersetzen-Hinweis, Übersprungene, Fehlergründe je Id, Knopf [save all] mit `run_command` `/setup portal save-all` nur bei angelegten Entwürfen; reine Funktion) und `PortalCompletions.worlds(...)`; dann Umsetzung; grün
- [x] 3.6 `MapProviderPortalSources` (über `getAvailableMaps()` mit `hasMapFile()`, `activeMap()`, `readMap`), `PortalCommand` (Syntaxen `copy <world>` und `save-all`, Vorschlags-Callback für `world`, nach erfolgreichem Kopieren einmal `PortalShow.start(player, copied.portals())`, keine `DraftPreview`-Tasks je Entwurf; Live-Vorschau für gespeicherte Ids stoppen), Verdrahtung in `Titan`; Persistenztest (integration, zwei `@TempDir`-Welten, Quelle byte-gleich, Ziel behält Spawn/Name, Verzeichnisse ohne `map.json` und die aktive Welt fehlen in `worlds()`) und Cyano-`Env`-Test mit `env.tick()`: nach `copy` läuft genau eine Show-Aufgabe für den Spieler (`PortalShow.running() == 1`) und zeigt die Formen aller kopierten Portale, keine Vorschau-Aufgabe je Entwurf, ein zweites `show` ersetzt sie; Ende der Live-Vorschau nach `save-all` (kein Sleep); `./gradlew :setup:test` grün

## 4. Doku und Logging

- [x] 4.1 `README.md`, Abschnitt „Setup server“: `copy <world>`, `save-all`, Hinweis auf Vorschau vor dem Speichern und darauf, dass die Quelle nur gelesen wird; Nachweis: `grep -n "save-all" README.md`
- [x] 4.2 Logging prüfen: INFO in `PortalCopier` (Anzahl, Quell-Welt, Ziel-Welt), INFO je gespeichertem Portal in `saveAll`, WARN einmal bei unlesbarer Quelle, kein Spielername; Nachweis: Logging-Test mit aufgefangenem Appender grün (wie `PortalEditorLoggingTest`)
- [x] 4.3 `./gradlew build` läuft durch; Commits je Typ (`feat(setup): ...`, Test- und Doku-Anteile nach Repositorykonvention)

## 5. Abnahme

- [ ] 5.1 Haiku-Review (read-only): Szenarien des Deltas `setup-portals` (Kopieren, save-all, reservierte Ids, Vervollständigung) dem jeweiligen Test zuordnen; F.I.R.S.T.-Check (`@TempDir`, keine Sleeps, keine Systemzeit, kein geteilter statischer Zustand, Reihenfolge egal, Quelle nie geschrieben); prüfen, dass `MapGson`/`PortalGsonAdapter` paketintern blieben. Nachweis: Bericht ohne Lücken

## 6. Pull Request

- [ ] 6.1 Pull Request (englisch) mit dem Titel `feat(setup): copy portals from another world` eröffnen; Beschreibung nennt die Voraussetzungen (#329 gemergt, `setup-portal-command` archiviert) und die zwei neuen `MapProvider`-Methoden
