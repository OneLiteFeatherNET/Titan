# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/setup-portals`. **Voraussetzung:** `lobby-portals` ist in `main` (die Typen `Portal`, `PortalShape` mit `Box`/`Disc` in `net.onelitefeather.titan.core.portal`, `PortalValidator`/`PortalProblem`, `LobbyMap.portals` samt Builder-Methode und Gson-Format sind Arbeitsnamen aus dem Entwurf dieses Changes; Aufgabe 1.1 gleicht sie mit dem gelieferten Code ab).

Ist-Zustand des Setup-Servers:

- Eigene, DI-freie `Titan`-Klasse (`setup`): privater Konstruktor legt Instanz und `MapProvider` an, liest `SetupSpawnConfig`, registriert `BlockHandlerHelper` sowie `SetupCommand(mapProvider)` und die Listener von Hand. Kein Avaje, keine Column, keine Module.
- `SetupCommand` (`/setup`, `Conditions::playerOnly`) hängt `MapCommand` als Unterbefehl ein. `MapCommand` (`setspawn`, `setname`, `setauthor`) speichert mit `mapProvider.saveMap(LobbyMap.lobbyMapBuilder(mapProvider.getActiveLobby()).x(...).build())`. Es gibt kein Recht auf den `/setup`-Befehlen; der Setup-Server ist ein internes Werkzeug des Build-Teams.
- `MapProvider.saveMap` schreibt die `map.json` der aktiven Welt (`GsonFileHandler` von Aves, Gson mit `PositionGsonAdapter`) und lädt sie neu (`loadMapData`). `LobbyMap.lobbyMapBuilder(map)` kopiert Spawn, Name und Autoren in den Builder; Felder, die dort fehlen, gehen beim Speichern verloren.
- Nachrichten sind englische MiniMessage-Strings mit `<prefix>`, aufgelöst durch `TitanMiniMessageImpl` (`common`). Das Repository hat keine `TranslationStore`-Infrastruktur.
- Der Setup-Server nutzt Aves nur für Map und Gson, keine Inventare, kein Wand-Item, keine Partikel. Tests: JUnit ohne Cyano (`TitanLauncherTest`, `SetupSpawnSettingsTest`).

## Goals / Non-Goals

**Goals:**
- Portale ohne JSON-Handarbeit anlegen, nie ungültig speichern.
- Bearbeitungslogik, Ringbildung, Umriss, Führungsschritte, Vorschau-Geometrie und Vervollständigung ohne Minestom-Server testbar.
- Den Nutzer aktiv unterstützen: geführter Ablauf mit Knöpfen, Live-Vorschau des Entwurfs, Tab-Vervollständigung.

**Non-Goals:**
- Rückgängig-Stapel, Wand-Item (mögliches Folge-Change), Bearbeiten anderer Welten, Vorschau in der Lobby (siehe proposal.md).
- Die Ladelogik und Validierung der Lobby (liegt in `lobby-portals`).

## Decisions

### D1: `PortalCommand` als Unterbefehl von `SetupCommand`

`/setup portal ...` ist ein weiterer Unterbefehl neben `MapCommand`; `SetupCommand` bekommt eine Zeile, `Titan` keine (der `MapProvider` wird schon durchgereicht). Syntaxen (Minestom `addSyntax`), `<id>` ist `ArgumentType.Word` mit Prüfung `[a-z0-9_-]+`:

- `list`, `show` als Literale allein
- `<id> pos1`, `<id> pos2`, `<id> remove`
- `<id> disc <radius>` (`ArgumentType.Double`)
- `<id> task <task>` (`ArgumentType.StringArray`, zusammengefügt; Aufgaben können Leerzeichen enthalten, leer wird abgelehnt)
- `<id> permission <recht>` (`Word`; `none` entfernt)

`list` und `show` sind als Id gesperrt, damit `/setup portal list` nie doppeldeutig ist. Die Prüfung der Id sitzt in der Logik (D2), nicht im Argument, damit der Chat den Grund nennen kann.

- Built-in first: Minestoms Command-Framework, so wie `MapCommand` es nutzt. Kein eigenes Parsing.
- Neue kleine Verben für den geführten Ablauf (D9): `create <id>`, `<id> shape box|ring`, `<id> centre`, `<id> radius <radius>`, `<id> save`, `<id> cancel`; `create` ist wie `list`/`show` als Id gesperrt. `centre` setzt Mittelpunkt und Normale wie `disc`, ohne Radius; `radius` ändert nur den Radius (sonst risse ein Klick auf einen Radius-Knopf den Mittelpunkt an die neue Position des Spielers). `disc <r>` bleibt als Kurzform für beides.
- Test: Der Befehl ist eine dünne Hülle ohne Logik; getestet über D2. Ein Integrationstest mit Cyano prüft, dass jede Syntax ihre Methode erreicht (Aufgabe 3.3), soweit Cyano im Setup-Modul verfügbar ist.
- SOLID: Open/Closed, ein neuer Unterbefehl statt Änderung an `MapCommand`; Single Responsibility, der Befehl parst und rendert, die Logik liegt woanders.

### D2: `PortalEditor` als reine Logik hinter der Naht `PortalStore`

`PortalEditor` (Paket `net.onelitefeather.titan.setup.portal`) hält die Entwürfe je Spieler-UUID und führt jede Änderung aus. Er bekommt `PortalStore` (`List<Portal> portals()`, `void save(List<Portal>)`) und gibt ein sealed Ergebnis zurück (`Saved`, `Updated`, `Pending(fehlt)`, `Complete`, `Removed`, `Rejected(Grund)`, `Cancelled`, `Unknown`), das der Befehl per `switch` in Text übersetzt. Die Standardimplementierung `MapProviderPortalStore` liest `getActiveLobby().portals()` und speichert per `saveMap(LobbyMap.lobbyMapBuilder(active).portals(list).build())` (D6).

- Built-in first: Records, sealed Interfaces und Pattern-Matching-`switch` (Java 25) statt Exceptions als Steuerfluss oder Statuscodes. Kein Framework nötig.
- Test (Unit, ohne Server): `PortalEditorTest` mit einem In-Memory-`PortalStore`.
- SOLID: Dependency Inversion (`PortalStore`), Single Responsibility (Logik getrennt von Befehl und Datei).

### D3: Entwurf je Spieler, gespeichert wird nur per `save`

Ein Entwurf (`PortalDraft`, veränderlich: Ecke 1, Ecke 2, Ring (Mittelpunkt, Normale, Radius oder nichts) oder nichts, gewählte Form, Aufgabe, Recht, Flag `guided`) gehört einem Spieler und einer Id. Beim ersten Zugriff wird er aus dem gespeicherten Portal mit dieser Id befüllt (Quader: Ecken aus `min`/`max`), sonst leer. Jeder Bearbeitungsbefehl ändert nur den Entwurf und liefert `Pending(fehlt)` bzw. `Complete` (Form und Aufgabe vorhanden; der Chat zeigt „vollständig" mit anklickbarem [save]). Gespeichert wird ausschließlich durch `save`, für Befehle und geführten Ablauf gleich: `save` prüft Vollständigkeit, Regeln des Befehls und `PortalValidator.problems(...)` für die Id (D5); ist alles gut, ersetzt der Entwurf ein Portal mit derselben Id an dessen Listenposition, sonst wird er angehängt (`Saved`/`Updated`); sonst `Pending`/`Rejected`, der Entwurf bleibt offen. Das Flag `guided` entfällt als Speicherregel; es steuert nur, ob nach einem Befehl der nächste Schritt des Ablaufs gezeigt wird (D9). Der Entwurf eines gespeicherten Portals bleibt auch nach einer Änderung Entwurf, bis `save` läuft. Bei `disc` ersetzt der Ring die Form und verwirft die Ecken; bei `pos1`/`pos2` verwirft der Quader-Entwurf den Ring, das gespeicherte Portal bleibt, bis der Quader vollständig ist. `cancel` verwirft den Entwurf. `list` zeigt die gespeicherten Portale und darunter getrennt die offenen Entwürfe des Spielers mit Stand (KISS: ein zweiter Abschnitt in `PortalMessages`, keine eigene Ansicht). Entwürfe leben nur im Speicher und werden beim Verlassen des Spielers verworfen (ein `PlayerDisconnectEvent`-Listener in `Titan`).

Verworfen: Die Reihenfolge „erst `task`, dann Form“ zu erzwingen (lästig) und ein einziger globaler Entwurf je Id (zwei Builder überschreiben einander die Ecken). Verworfen: Portale mit leerer Aufgabe zu speichern (die Lobby bräche beim Start ab) und Auto-Speichern bei Vollständigkeit (unbeabsichtigte Zwischenstände in der `map.json`, zwei Speicherwege).

- Test (Unit): jede Szene aus `specs/setup-portals` als Test in `PortalEditorTest`, auch zwei Spieler, dieselbe Id.
- SOLID: Single Responsibility; der Entwurf kennt die Regeln nicht, die prüft D5.

### D4: Ring aus Augenposition und Blickrichtung

`DiscPlacement.of(eye, direction, radius)` (reine Funktion, Records `Vec`/`Pos` aus Minestom) liefert `Disc`: Mittelpunkt = **Augenposition**, jede Koordinate auf 0,5 gerundet (`Math.round(v * 2) / 2.0`); Normale = normierte Blickrichtung, eingerastet auf die nächste der sechs Achsen, wenn der Winkel höchstens 5 Grad beträgt.

- Augenposition statt Fußposition: Mittelpunkt und Blickrichtung teilen dann denselben Ursprung, die Normale steht wirklich senkrecht auf dem, was der Builder anpeilt. Wer den Ring „durchschaut“, steht in dessen Mitte; die Fußposition läge 1,62 Blöcke daneben. Rundung auf 0,5 gibt saubere Werte wie im Beispiel (`0.5, 72, 40.5`), die man von Hand nicht trifft. Nachteil: Die Höhe hängt von der Haltung (Schleichen, Fliegen) ab; die Rückmeldung nennt den gespeicherten Mittelpunkt, damit man ihn ablesen kann.
- Einrasten (5 Grad, Konstante): Wer „geradeaus“ schaut, trifft die Achse selten exakt; ohne Einrasten stünden krumme Normalen wie (0,003, 0,01, 0,99996) in der Datei, und ein Quader-Test der Lobby wäre schwerer nachzuvollziehen. Schrägen Ringen (Diagonale, Neigung) bleibt die genaue Richtung. Verworfen: Einrasten abschaltbar zu machen (mehr Oberfläche ohne Bedarf).
- Test (Unit): Achsen, 2 Grad neben +Z, 6 Grad neben +Z, Diagonale, gerundeter Mittelpunkt, Blick nach oben/unten, negativer Radius wird von D5 abgelehnt.
- SOLID: Reine Funktion, keine Abhängigkeiten.

### D5: Validierung ist die der Lobby, nicht eine Kopie

Vor dem Speichern läuft dieselbe Prüfung wie beim Lobby-Start. Das Ziel ist, dass keine zweite Regelkopie entsteht (DRY): `lobby-portals` liefert `net.onelitefeather.titan.core.portal.PortalValidator` mit `static List<PortalProblem> problems(List<Portal>)` (`PortalProblem(String portalId, String reason)`) und `requireValid(String world, List<Portal>)`; `Box`, `Disc` und `Portal` sind reine Daten ohne Konstruktorprüfung. `PortalEditor` baut die Liste, wie sie nach dem Speichern in der Datei stünde (Ersetzen an der Listenposition oder Anhängen), ruft `PortalValidator.problems(...)`, nimmt die Probleme mit `portalId` gleich der bearbeiteten Id und macht daraus `Rejected(Grund)`; `PortalMessages` bildet sie auf Chat-Meldungen ab (der `reason` als `Placeholder.unparsed`). Probleme anderer, schon vorher ungültiger Portale blockieren die Bearbeitung dieses Portals nicht. `requireValid` bleibt dem Lobby-Start. Fehlt dort eine Regel, die die Spec hier nennt, wandert sie in `PortalValidator` (Anpassung in `lobby-portals`, nicht hier kopiert). Nur die Regeln des Befehls selbst liegen im Setup-Modul: Id-Muster, gesperrte Ids (`list`, `show`, `create`), Radius als Zahl, leere Aufgabe vor dem Trimmen sowie die Vollständigkeit des Entwurfs (beide Ecken, Radius, Aufgabe), weil `Box` ohne Ecken gar nicht existiert.

- Test (Unit): jede Regel aus der Spec als Ablehnungstest; ein Test, der `PortalValidator.requireValid` und den Editor an derselben ungültigen Liste vergleicht, hält beide im Gleichschritt.
- SOLID: Dependency Inversion auf die Regeln in `core`; Open/Closed.

### D6: Speichern über den vorhandenen Weg, ohne Datenverlust

Gespeichert wird `LobbyMap.lobbyMapBuilder(active).portals(list).build()` über `MapProvider.saveMap`. Das setzt voraus, dass `lobby-portals` `lobbyMapBuilder(map)` die Portale mitkopieren lässt, sonst löschte `/setup map setspawn` alle Portale. Aufgabe 1.1 prüft das; fehlt es, ergänzt `lobby-portals` es (kleine Korrektur dort, mit Test), nicht dieser Change. Die Liste ist unveränderlich (`List.copyOf`).

Nebenwirkung, unverändert vom Bestand: `saveMap` lädt die Map neu (`loadMapData`), auch für Portale. Das ist derselbe Pfad wie bei `setspawn` und kostet ein Neuladen der Map-Datei.

- Built-in first: `MapProvider`/Aves-`GsonFileHandler` und das Gson-Format von `lobby-portals`, keine eigene Serialisierung.
- Test: Unit über `PortalStore`; Persistenztest (Integration, `@TempDir`): `MapProvider.create(tempDir, instance)` über einer `worlds/world/map.json` mit Spawn, Name, Autor und einem Portal, Portal über `MapProviderPortalStore` speichern, mit einem frischen `MapProvider` neu laden und Spawn, Name, Autoren, alte und neue Portale vergleichen; danach `MapCommand`-Pfad (`setspawn`) ausführen und prüfen, dass die Portale bleiben.
- SOLID: Dependency Inversion; der Setup-Server ändert `MapProvider` nicht.

### D7: Umriss als Partikel, Punkte als reine Funktion

`PortalOutline.points(PortalShape)` liefert die Punkte: für einen Quader die zwölf Kanten von `min` bis `max + 1` (blockinklusive Hüllkörper) mit 0,5 Blöcken Abstand; für einen Ring `max(16, ceil(2·π·r / 0,5))` Punkte auf dem Kreis, aufgespannt aus zwei Vektoren senkrecht zur Normalen. `show` startet je Spieler einen wiederholenden Scheduler-Task (`player.scheduler()`, alle 5 Ticks, 8 Sekunden = 32 Durchläufe), der die Punkte per `ParticlePacket` nur an diesen Spieler sendet; ein zweites `show` bricht den ersten Task ab. Ein Spieler-Trennen stoppt den Task, weil der Spieler-Scheduler mit dem Spieler endet.

- Built-in first: Minestoms `ParticlePacket` und der Spieler-Scheduler; kein Partikel-Helfer im Repository (geprüft), keine neue Bibliothek. Verworfen: Glas- oder Block-Platzhalter (verändern die Welt, brauchen Aufräumen).
- Test: Unit für `PortalOutline` (Punktzahl, jeder Punkt auf der Kante bzw. im Abstand `radius` vom Mittelpunkt und senkrecht zur Normalen, Achsen-Normalen und Diagonale); der Senderteil ist ein Einzeiler und wird nur im Integrationstest (Cyano, `env.tick()`) geprüft: nach 32 Ticks kein Task mehr, ein zweites `show` ersetzt den ersten.
- SOLID: Single Responsibility; Geometrie getrennt vom Versand.

### D8: Nachrichten englisch im Stil von `MapCommand`; Logging

Rückmeldungen sind englische MiniMessage-Strings mit `<prefix>` wie in `MapCommand` (grün für Erfolg, rot für Fehler, Platzhalter über `Placeholder.parsed`/`component`), gesammelt in einer Klasse `PortalMessages`. Das Repository hat weder `TranslationStore` noch Bundles, der Setup-Server ist ein internes Team-Werkzeug ohne Übersetzung; der Change führt keine i18n-Infrastruktur ein (eigener Change, wenn gewünscht). Die Rückmeldung nennt bei Ringen den gespeicherten Mittelpunkt und die Normale. Eingaben der Spieler (Aufgabe, Recht) gehen über `Placeholder.unparsed`, damit MiniMessage-Tags im Text nicht ausgewertet werden.

Logging: SLF4J, eine Zeile INFO je gespeichertem oder entferntem Portal (`Saved portal {} in world {}`); keine Spielernamen oder Chattexte. Keine Metriken, keine Spans.

- Test: Unit für `PortalMessages` (Platzhalter, Escapen von `<` in der Aufgabe); INFO-Zeile über einen aufgefangenen Appender.
- SOLID: Single Responsibility.

### D9: Geführter Ablauf als dünne Schicht

`PortalFlow.render(FlowState, List<Portal>)` ist eine reine Funktion: aus Entwurfszustand (gewählte Form, gesetzte Ecken, Mittelpunkt, Radius, Aufgabe, Recht) und den Portalen der Welt liefert sie den nächsten Schritt als Komponente samt der Befehlstexte ihrer Knöpfe (`FlowStep(Component, List<FlowButton(label, command, kind)>)`, `kind` = `RUN` oder `SUGGEST`). Der Befehlshandler sendet den Schritt nach jedem Unterbefehl, der einen geführten Entwurf ändert, und bei `create`; der Ablauf endet in der Zusammenfassung mit [save] und [cancel], `save` ist derselbe Befehl wie von Hand (D3). Knöpfe tragen nur bestehende Unterbefehle (`ClickEvent.runCommand`) oder schlagen im Eingabefeld vor (`ClickEvent.suggestCommand`, für Aufgabe, Recht, Radius); es gibt keinen zweiten Änderungspfad. Der Zustand ist der vorhandene Entwurf (D3, Flag `guided`), kein eigener Zustandsautomat: der nächste Schritt folgt aus dem, was im Entwurf fehlt. `cancel` verwirft, das Trennen verwirft (D3). Aufgaben-Knöpfe kommen aus den `task`-Werten der Portale der Welt (`distinct`, Reihenfolge der Liste, höchstens 8); die Navigator-Spalte wird nicht gelesen, `setup` hängt nicht von `features/*` ab. Radius-Knöpfe sind feste Vorschläge (2, 3, 5, 8) plus Vorschlag für freie Eingabe; eine Abstands-Heuristik ist nicht nötig (KISS).

- Klick-Komponenten baue ich im Code mit der Adventure-API (`Component.text(...).clickEvent(...)`) statt mit `<click:...>`-Tags, sobald Spielertext im Befehl vorkommt (Aufgaben): Anführungszeichen oder `<` in einer Aufgabe würden einen MiniMessage-Tag brechen oder Befehle einschleusen. Statische Texte dürfen MiniMessage mit `<prefix>` bleiben (D8). Verworfen: eigener Zustandsautomat im Ablauf (zweite Wahrheit neben dem Entwurf); Chat-Eingabe abfangen (Dialog-Modus, fehleranfällig).
- Test (Unit, ohne Server): `PortalFlowTest` prüft je Zustand den Schritt (Knopfbeschriftungen, Befehlstexte, `RUN`/`SUGGEST`), Aufgaben aus Portalen ohne Doppelte, Aufgabe mit `<`/`'` bleibt wörtlich im Befehl.
- SOLID: Single Responsibility (Darstellung getrennt von Logik), Open/Closed (neuer Schritt = neuer Fall).

### D10: Live-Vorschau: reine Punkte, ein Task je Spieler

`DraftOutline.points(PortalDraft, Pos eye, Vec look, Point block)` ist eine reine Funktion und liefert die Vorschau-Punkte: Ecken als Blockmitten, Quader (bei einer Ecke von dort bis zu `block`, dem aktuellen Block des Spielers), Ring (Kreis wie `PortalOutline` in D7; ohne Radius Standardradius 3 um `eye` in der Ebene senkrecht zu `look`). Die Kantenerzeugung teilt sich der Code mit `PortalOutline` (DRY). Obergrenze: `MAX_POINTS = 256` je Vorschau (Konstante, im Test gepinnt); bei Überschreitung wird der Abstand vergrößert statt Punkte abzuschneiden, damit die Form vollständig bleibt.

`DraftPreview` (Paket `portal`) hält je Spieler-UUID höchstens einen Scheduler-Task (`player.scheduler().buildTask(...).repeat(TaskSchedule.tick(5))`). Er wird gestartet, sobald ein Spieler einen offenen Entwurf hat, und läuft, bis der Entwurf endet: `start(player)` ersetzt einen laufenden Task, `stop(uuid)` bricht ihn ab; `stop` rufen der Befehl bei den Ergebnissen `Saved`, `Removed`, `Cancelled` und der `PlayerDisconnectEvent`-Listener (D3) auf. Jeder Durchlauf liest den Entwurf frisch, berechnet die Punkte mit der aktuellen Position und sendet `ParticlePacket`s nur an diesen Spieler; ist der Entwurf weg, beendet er sich selbst. Der Hinweis „Standardradius“ geht einmal beim Wechsel in diesen Zustand in den Chat, nicht bei jedem Durchlauf. Kein Leck: der Spieler-Scheduler endet mit dem Spieler, zusätzlich stoppt der Listener ausdrücklich, und die Map der Tasks wird bei `stop` bereinigt; keine statischen Felder, frische `DraftPreview`-Instanz je Test. `show` (D7) bleibt für gespeicherte Portale unverändert.

- Warum 5 Ticks: 4 Aktualisierungen pro Sekunde wirken flüssig, Partikel leben etwa eine Sekunde, und mit `MAX_POINTS = 256` sind es höchstens rund 1000 Partikel pro Sekunde je Spieler; die Konstanten stehen an einer Stelle und sind änderbar.
- Test: Unit für `DraftOutline` (Ecken, Quader mit einer und zwei Ecken, Ring mit und ohne Radius, Achsen und Diagonale, Obergrenze bei riesigem Quader und Radius). Lebenszyklus mit Cyano-`Env` und `env.tick()`: Pakete nur an den Entwurfsspieler, Quader folgt nach Bewegen, Task endet nach `save`, `cancel` und Trennen, zweites `start` ersetzt den Task; keine Uhr, kein Warten.
- SOLID: Geometrie, Planung und Versand getrennt.

### D11: Tab-Vervollständigung aus einer reinen Quelle

`PortalCompletions` (rein) liefert die Vorschläge aus den gespeicherten Portalen, den Entwurfs-Ids des Spielers und festen Listen (Verben, Radius-Hinweise `1`, `2`, `3`, `5`, `8`, `none`, `box`, `ring`): `ids(portals, drafts)`, `verbs()`, `tasks(portals)` (distinct), `radii()`. `PortalCommand` hängt sie über `setSuggestionCallback` der Minestom-Argumente an; das ist Minestoms eingebaute Vervollständigung, kein eigenes Parsing. Die Quelle für Aufgaben sind wie in D9 die vorhandenen Portale.

- Test (Unit): `PortalCompletionsTest` (Ids aus Welt und Entwurf ohne Doppelte, Verben, Aufgaben distinct, Radien); ein Integrationstest mit Cyano prüft, dass die Callbacks registriert sind, soweit Cyano das erlaubt.
- SOLID: Single Responsibility.

## Risks / Trade-offs

- [`lobby-portals` liefert andere Namen, Feldtypen oder ein anderes JSON-Format] → Aufgabe 1.1 gleicht ab und passt dieses Dokument an, bevor Code entsteht.
- [`lobby-portals` kopiert Portale nicht in `lobbyMapBuilder(map)`, `setspawn` löscht sie] → Persistenztest deckt das ab; Korrektur gehört nach `lobby-portals` (D6).
- [`PortalValidator` fehlt eine Regel, die die Spec nennt] → in `lobby-portals` ergänzen statt kopieren (D5).
- [Augenhöhe schwankt mit der Haltung] → Rückmeldung zeigt den Mittelpunkt; `disc` lässt sich wiederholen (ersetzt die Form).
- [Ein Builder setzt Ecken, verlässt den Server, Entwurf weg] → bewusst: nichts halb Gespeichertes; der Chat sagt bei jedem Schritt, was fehlt.
- [Kein Recht auf `/setup portal`] → wie alle `/setup`-Befehle; wer den Setup-Server betreten darf, darf bauen. Ein Recht wäre ein eigener Change für alle `/setup`-Befehle.
- [Viele Partikel bei großen Quadern] → 0,5 Blöcke Abstand, nur der ausführende Spieler, `show` 8 Sekunden; `PortalOutline` und `DraftOutline` begrenzen die Punktzahl je Portal bzw. Vorschau (`MAX_POINTS = 256`, im Test gepinnt).
- [Klickbare Knöpfe hängen an Adventure-`ClickEvent` bzw. MiniMessage-Klick-Tags und deren Verhalten in Minestom 26.1/Client] → Komponenten mit Spielertext per API statt Tags (D9); reine Tests prüfen Befehlstexte, nicht das Klickverhalten; ein manueller Abnahmelauf klickt jeden Knopf im Client (Aufgabe 4.2). Läuft `run_command` im Client anders (z. B. Bestätigung), bleibt der Befehl tippbar, weil jeder Knopf nur einen normalen Befehl trägt.
- [Vorschau-Task leckt oder läuft nach Speichern weiter] → höchstens ein Task je UUID, ausdrückliches `stop` bei Speichern, Abbrechen, Entfernen, Trennen; Lebenszyklustest mit `env.tick()` (D10).
- [Mehr neue Verben (`shape`, `centre`, `radius`, `save`, `cancel`) vergrößern die Befehlsoberfläche] → nötig, damit Knöpfe nur bestehende Befehle tragen; sie sind auch von Hand nutzbar und werden vervollständigt.
