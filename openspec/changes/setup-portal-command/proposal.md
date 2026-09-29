# Proposal

## Why

Der Change `lobby-portals` lässt die Lobby Portale aus der `map.json` der Welt lesen (Quader und Ringe, je mit Aufgabe und optionalem Recht). Das Build-Team müsste diese Einträge von Hand in JSON schreiben: Blockkoordinaten abtippen, für Ringe Mittelpunkt und Normale ausrechnen. Fehler fallen erst beim Lobby-Start auf, weil ein ungültiges Portal ihn abbricht. Der Setup-Server, in dem die Welt ohnehin gebaut und gespeichert wird, soll Portale deshalb aus dem Spiel heraus anlegen, prüfen und speichern.

## What Changes

- **Voraussetzung:** Dieser Change baut auf `lobby-portals` auf und darf erst umgesetzt werden, wenn dessen Code in `main` ist. Er verwendet dessen Typen unter den Arbeitsnamen `net.onelitefeather.titan.core.portal.PortalShape` (sealed, `Box(min, max)` und `Disc(center, radius, normal)`) und `Portal(String id, PortalShape shape, String task, @Nullable String permission)` sowie die Liste `portals` an `LobbyMap` samt Builder-Methode. Die Validierung liefert `lobby-portals` als `net.onelitefeather.titan.core.portal.PortalValidator` (`static List<PortalProblem> problems(List<Portal>)`, `PortalProblem(String portalId, String reason)`, `requireValid(String world, List<Portal>)`); die Records `Box`/`Disc`/`Portal` sind reine Daten ohne Konstruktorprüfung. Die Namen MÜSSEN mit dem übereinstimmen, was `lobby-portals` am Ende ausliefert; Aufgabe 1.1 prüft das und passt design.md und tasks.md sonst an.
- Neuer Unterbefehl `/setup portal ...` im Setup-Server (neben `/setup map ...`):
  - `/setup portal <id> pos1` und `pos2`: Blockposition des Spielers als Quader-Ecke; der Quader ist die blockinklusive Spanne.
  - `/setup portal <id> disc <radius>`: Ring um die Augenposition des Spielers, Normale ist die Blickrichtung, nahe an einer Achse wird sie auf die Achse eingerastet.
  - `/setup portal <id> task <task>`, `/setup portal <id> permission <recht|none>`, `/setup portal <id> remove`.
  - `/setup portal list`: alle gespeicherten Portale der Welt, darunter getrennt die offenen Entwürfe des Spielers. `/setup portal show`: Umrisse gespeicherter Portale als Partikel für wenige Sekunden, nur für den ausführenden Spieler.
- **Geführter Ablauf:** `/setup portal create <id>` startet eine Schritt-für-Schritt-Führung im Chat mit anklickbaren Knöpfen (Form Box oder Ring, Ecken bzw. Mittelpunkt und Radius, Aufgabe, optionales Recht, Zusammenfassung mit Speichern und Abbrechen). Jeder Knopf führt einen der Unterbefehle aus (`pos1`, `pos2`, `disc`, `task`, `permission` sowie die neuen kleinen Verben `shape`, `centre`, `radius`, `save`, `cancel`); der Ablauf ist eine dünne Schicht über den Befehlen, kein zweiter Codepfad. Der geführte Ablauf und die einzelnen Befehle bearbeiten denselben Entwurf.
- **Live-Vorschau:** Solange ein Spieler einen offenen Entwurf hat, zeigen Partikel nur ihm den Entwurf (gesetzte Ecken, Quader bis zum aktuellen Block des Spielers, Ring mit gewähltem Radius, vorher mit Blickrichtung und Standardradius, mit Hinweis), alle 5 Ticks aktualisiert, mit Obergrenze der Punktzahl; sie endet bei Speichern, Abbrechen, Entfernen und Trennen.
- **Tab-Vervollständigung:** Vorschläge für Portal-Ids (gespeichert und eigene Entwürfe), Verben, bereits verwendete Aufgaben und Radius-Hinweise.
- **Speichern nur per `save`:** Jeder Bearbeitungsbefehl (`pos1`, `pos2`, `shape`, `centre`, `radius`, `disc`, `task`, `permission`) ändert nur den Entwurf und antwortet mit dessen Stand (was noch fehlt, oder „vollständig" mit anklickbarem [save]). Ein Portal wird ausschließlich durch `/setup portal <id> save` gespeichert, im geführten Ablauf wie bei Befehlen; `cancel` verwirft den Entwurf. `save` prüft Vollständigkeit und `PortalValidator.problems(...)` aus `lobby-portals`; die Prüfung ist `PortalValidator.problems(...)` aus `lobby-portals`, die Probleme werden auf Chat-Meldungen abgebildet; dazu Regeln nur des Befehls (Id-Muster, gesperrte Ids `list`/`show`/`create`, Radius als Zahl, leere Aufgabe). Bei Problemen bleibt der Entwurf offen und der Chat nennt sie; ein ungültiges Portal wird nie gespeichert.
- Gespeichert wird über den bestehenden Weg (`MapProvider.saveMap` mit `LobbyMap`-Builder) in die `map.json` der aktuellen Welt; Spawn, Name und Autoren bleiben erhalten.
- Kein Rückgängig-Stapel, kein Wand-Item (der Setup-Server nutzt keins; ein Wand-Item ist ein mögliches Folge-Change).

## Capabilities

### New Capabilities

- `setup-portals`: Das Build-Team legt im Setup-Server Portale per Befehl an, ändert, listet, entfernt und zeigt sie an; nur gültige Portale werden in die Map-Datei der Welt geschrieben.

### Modified Capabilities

Keine. Es gibt keine Setup-Spec; die Lobby-Seite (Laden, Auslösen) liegt in `lobby-portals`.

## Impact

- **Code**: `setup` (neuer Befehl `PortalCommand` unter `SetupCommand`, reine Bearbeitungslogik, Partikel-Umriss, Anbindung an `MapProvider`). Neu sind außerdem reine Klassen für Führung (`PortalFlow`), Vorschau-Geometrie (`DraftOutline`) und Vervollständigung (`PortalCompletions`) sowie ein Vorschau-Dienst mit Scheduler-Task je Spieler. Kein Code in `common` oder `core`, sofern `lobby-portals` `PortalValidator` und Builder-Methode wie beschrieben liefert; `setup` hängt nicht von `features/*` ab (Aufgabennamen kommen aus den vorhandenen Portalen, nicht aus der Navigator-Spalte).
- **Abhängigkeiten**: keine neuen Bibliotheken; `setup/build.gradle.kts` bekommt für den Persistenztest `testImplementation(libs.cyano)` (wie `common`).
- **Tests**: Unit-Tests für Bearbeitung, Abbildung der `PortalValidator`-Probleme, Ringbildung, Umriss, Führungsschritte (Komponenten und Befehlstexte als reine Funktion), Vorschau-Geometrie samt Obergrenze und Vervollständigung ohne Server; ein Persistenztest mit `@TempDir`; Cyano-`Env` mit `env.tick()` für den Vorschau-Lebenszyklus.
- **Nutzertexte**: neue Chat-Rückmeldungen des Setup-Servers, englisch im Stil von `MapCommand` (das Repository hat keine Übersetzungsinfrastruktur, siehe design.md D8).
- **Doku**: Abschnitt „Setup server“ in `README.md` nennt die Befehle.
- **Betrieb**: keine Konfiguration.

## Delivery

PR-Titel: `feat(setup): create lobby portals from in-game commands`

Umfang: größer als zuvor (Führung, Vorschau, Vervollständigung kommen zu den Befehlen hinzu), aber ein Typ (`feat`) und ein Modul (`setup`), daher ein PR. Übersteigt die Umsetzung grob 900 geänderte Zeilen Produktivcode, wird an der Wellengrenze geteilt: erst Befehle, Speichern, `show`; dann Führung, Vorschau, Vervollständigung (beide `feat(setup): …`).

Nicht-Ziele, jeweils eigener Change: Rückgängig-Stapel, Wand-Item (mögliches Folge-Change), Ändern von Portalen anderer Welten als der aktiven, Vorschau in der Lobby, Bearbeiten der Form per Griffe.
