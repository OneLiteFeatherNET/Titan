# Proposal

## Why

Das Build-Team pflegt Portale je Welt in der `map.json`. Wird eine Welt kopiert oder ersetzt (z. B. die Saison-Lobby aus der Standard-Lobby, siehe `seasonal-lobby-world`), stehen die Portale nur in der alten Welt und müssten Stück für Stück nachgebaut werden. Der Setup-Server soll sie stattdessen aus einer anderen Welt übernehmen können, ohne blind zu überschreiben: erst prüfen, dann speichern.

## What Changes

- **Voraussetzungen:** Der Change baut auf den Befehlen aus PR #329 (`feat/setup-portals`, Change `setup-portal-command`) auf; #329 MUSS zuerst in `main` sein. Außerdem MUSS `setup-portal-command` archiviert sein, damit die Capability `setup-portals` in `openspec/specs` existiert (siehe Abhängigkeit unten und Aufgabe 1.1).
- Neu: `/setup portal copy <world>`. Ziel ist die Welt, die der Setup-Server geladen hat; `<world>` nennt ein anderes Verzeichnis unter `worlds/` mit `map.json`. Die Quelle wird nur gelesen, nie geschrieben, und die aktive Welt wechselt nicht.
- Jedes Portal der Quelle wird ein Entwurf des ausführenden Spielers (ein Entwurf je Portal-Id, Koordinaten unverändert). Der Chat nennt die kopierten Ids und getrennt die Ids, die beim Speichern ein vorhandenes Portal der Ziel-Welt ersetzen. Hat der Spieler zu einer Id schon einen offenen Entwurf, bleibt dieser unangetastet und die Id wird als übersprungen gemeldet.
- Gespeichert wird weiter nur per `save`: einzeln mit `/setup portal <id> save`, oder für alle offenen Entwürfe des Spielers mit dem neuen `/setup portal save-all` (nach dem Kopieren auch als anklickbares [save all]). `save-all` prüft jeden Entwurf wie `save` mit `PortalValidator`, meldet das Ergebnis je Id, und ein Entwurf mit Fehler bleibt offen, ohne die übrigen zu blockieren.
- Nach erfolgreichem Kopieren läuft das vorhandene `show` einmal über die kopierten Portale (Partikel nur für den ausführenden Spieler, wie bei `/setup portal show`; ein weiteres `show` ersetzt das erste). So lassen sich die Positionen in der Ziel-Welt vor dem Speichern prüfen (die Koordinaten der Quelle passen nicht zwingend zur Ziel-Welt). Die Live-Vorschau aus `setup-portal-command` bleibt unverändert ein Task je Spieler; das Kopieren startet keine Vorschau-Tasks je Entwurf.
- Fehlerfälle sind reine Chat-Meldungen ohne Änderung: unbekannte Quell-Welt, Quelle gleich Ziel, Quelle ohne Portale, unlesbare Quell-`map.json`.
- Tab-Vervollständigung für `<world>`: Verzeichnisse unter `worlds/` mit `map.json`, ohne die aktive Welt. `copy` und `save-all` werden als Portal-Ids reserviert.
- Lesen der Quelle ohne Weltwechsel: `MapProvider` (`common`) bekommt zwei kleine öffentliche Methoden (`activeMap()` und `readMap(MapEntry)`); `MapGson` und `PortalGsonAdapter` bleiben paketintern (siehe design.md D1).
- Kein Zusammenführen einzelner Portale, kein Umbenennen beim Kopieren, kein Verschieben der Koordinaten, kein Kopieren in eine nicht geladene Welt.

Lieferung: Pull Request mit dem Titel `feat(setup): copy portals from another world`. Ein Typ (`feat`), Scope `setup`; die kleine Ergänzung in `common` dient nur diesem Feature.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `setup-portals`: Neue Anforderungen (Kopieren aus anderer Welt, `save-all`, reservierte Wörter `copy`/`save-all`, Vervollständigung der Quell-Welt) als `ADDED`-Delta; bestehende Anforderungen bleiben unverändert.

**Abhängigkeit:** `openspec/specs/setup-portals` gibt es auf `main` noch nicht, weil `setup-portal-command` nicht archiviert ist (und dessen Code erst mit #329 in `main` kommt). Das Delta lässt sich beim Archivieren nur auf die vorhandene Capability anwenden. `setup-portal-command` MUSS daher vor diesem Change archiviert werden (`/opsx:archive setup-portal-command`); das ist Aufgabe 1.1 in `tasks.md`, wie bei `season-quoted-dates` und `seasonal-lobby-world`.

## Impact

- **Code**: `setup` (`PortalCommand`: zwei Syntaxen und ein Vorschlags-Callback; neu `PortalCopier` und die Naht `PortalSources` samt `MapProviderPortalSources`; `PortalEditor`: `adopt` und `saveAll`, reservierte Wörter; `PortalShow` und `DraftPreview` unverändert; `PortalMessages`, `PortalCompletions`; Verdrahtung in `Titan`). `common`: `MapProvider.activeMap()` und `MapProvider.readMap(MapEntry)`, sonst keine Änderung; `core` unverändert (`PortalValidator` wird nur benutzt).
- **Abhängigkeiten**: keine neuen.
- **Tests**: Unit-Tests ohne Server für Kopieren, `saveAll` und Vervollständigung (fester `PortalSources`-Ersatz); ein Persistenztest mit zwei `@TempDir`-Welten (Quelle bleibt byte-gleich, Ziel behält Spawn/Name); ein Test in `common` für `readMap`/`activeMap`; Cyano-`Env` mit `env.tick()` für das einmalige `show` nach dem Kopieren.
- **Nutzertexte**: neue Chat-Rückmeldungen, englisch im Stil von `PortalMessages` (das Repository hat keine Übersetzungsinfrastruktur, siehe `setup-portal-command` D8).
- **Doku**: Abschnitt „Setup server“ in `README.md` nennt `copy` und `save-all` (README gehört bestätigt zur Doku-Welle).
- **Betrieb**: keine Konfiguration.
