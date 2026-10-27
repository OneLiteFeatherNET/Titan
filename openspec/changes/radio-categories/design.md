# Design

## Kontext

Das Radio aus `lobby-radio` (4.9.0) besteht aus einem Hotbar-Item und zwei festen Menüs. Jedes Menü ist ein geteiltes Aves-Inventar je Modus, also sechs Inventare, die für alle Spieler gleich aussehen (`lobby-radio`, D12 und D14). Dieses Modell trägt keine Seiten, keine Kategorien und keine Lautstärke. `lobby-radio` lädt außerdem alle MIDI-Dateien beim Start in den Speicher, mit Grenzen für Datei, Noten und Länge.

Die Owner-Entscheidungen zu diesem Vorschlag (PR #511, `proposal.md`, Entschieden) ändern drei Dinge: Es gibt keine Grenzen mehr, die Lautstärke wird gespeichert, und der laufende Titel glänzt. Die Begründungen stehen in den einzelnen R-Abschnitten.

Geprüft in den Quellen:

- **Aves 1.16.7** (Sources): `PersonalTranslatedInventoryBuilder(type, player)` ist ein `GlobalTranslatedInventoryBuilder` für einen Spieler, geöffnet mit `open(locale)`. `invalidateDataLayout()` ruft die Datenebenen-Funktion erneut auf und aktualisiert offene Inventare in place. `unregister()` entfernt die Listener vom Inventar-Knoten und schließt die Betrachter. `TranslatedItem.of(ItemStack.Builder)` existiert. `PageableInventory` ist `@ApiStatus.Experimental` und kennt nur Zurück und Weiter.
- **Minestom** (lokaler Checkout): `ItemStack.Builder.glowing()` setzt `DataComponents.ENCHANTMENT_GLINT_OVERRIDE`. Ob der Glint nach `TranslatedItem.get(locale)` erhalten bleibt, prüft ein Test (Aufgabe 1.1).
- **Otis** (`origin/main`, Einstellungs-API aus PR #181): `value` ist ein beliebiger JSON-Wert, höchstens 64 KiB, versioniert, ohne Schlüsselregistrierung und ohne Standardwerte. `PUT` nimmt jeden JSON-Körper an.
- **Titan-Spielereinstellungen:** `OtisJson` liest und schreibt nur Wahrheitswerte. `SettingsStore`, `WriteBehind`, `PlayerSettingsService`, `SettingChangeListener` und `SettingsRegistry` sind boolesch typisiert. Die Otis-Seite ist also offen, die Titan-Seite ist eng.

## Ziele / Nicht-Ziele

**Ziele**

- Kategorien aus `songs.yaml`, Vanilla als eingebaute Kategorie, Sonstige als Ziel für alles Unbekannte.
- Seiten mit festen Plätzen, Seite je Spieler, Steuerzeile ohne Verschiebung.
- Lautstärke je Spieler, in Otis gespeichert, für MIDI und Vanilla, mit einer ehrlichen Regel für laufende Schallplatten.
- Beim Start nur Metadaten im Speicher; Noten erst beim Abspielen, außerhalb des Takts.
- Glanz auf dem laufenden Titel, nur im Fenster des Spielers.

**Nicht-Ziele**

- Lautstärke im Einstellungsmenü der Lobby.
- Zufall über alle Kategorien.
- Vorladen des nächsten Titels.
- Eine Otis-Änderung.

## Entscheidungen

### R1 Basis: Delta zu lobby-radio, ersetzt die Menüstruktur

Diese Änderung ist ein Delta zu `openspec/changes/lobby-radio/specs/lobby-radio/spec.md`. Die Umsetzung von `lobby-radio` ist in 4.9.0 ausgeliefert, aber der Change-Ordner ist noch nicht archiviert. Deshalb gilt die Reihenfolge: `lobby-radio` zuerst archivieren, dann `radio-categories`. Vorher verweist das MODIFIED- und RENAMED-Delta auf einen Hauptbestand, den es noch nicht gibt.

Ersetzt werden:

- `lobby-radio`, D12 (zwei feste Menüs mit Umschalter) und die Umschalter-Szenarien.
- `lobby-radio`, D14 (sechs geteilte Inventare je Menü und Modus).
- `lobby-radio`, D8 (Grenzen für Datei, Noten, Länge und Titelzahl). Die Grenzen entfallen; nur die Paketgrenze je Tick bleibt (R5).
- `lobby-radio`, D1 für die Lautstärke: Sie wird jetzt gespeichert (R9). Für den Modus und die Stoppgründe gilt D1 weiter.

### R2 Kategorien in songs.yaml

Die Kategorien stehen in derselben Datei wie die Songs. Eine Kategorie ist ein Eintrag mit Id, Anzeigename, Symbol und Reihenfolge.

```yaml
categories:
  - id: games
    name: { de: Spiele, en: Games }
    icon: EMERALD
    order: 10
  - id: anime
    name: Anime
    icon: FIREWORK_ROCKET
    order: 20
  - id: classic
    name: { de: Klassik, en: Classic }
    icon: CLOCK
    order: 30
  - id: halloween
    name: { de: Halloween, en: Halloween }
    icon: JACK_O_LANTERN
    order: 40
songs:
  - id: sunrise
    title: Sunrise
    artist: Beispiel
    disc: MUSIC_DISC_CAT
    file: sunrise.mid
    category: games
```

Regeln:

- **Id:** `[a-z0-9_-]{1,32}`. Die Ids `minecraft` und `other` sind reserviert; ein Eintrag mit einer dieser Ids wird übersprungen (WARN).
- **Name:** entweder ein Text für beide Sprachen oder eine Zuordnung `{ de, en }`. Fehlt eine Sprache, gilt die andere. Fehlt der Name ganz, zeigt die Übersicht die Id und protokolliert WARN.
- **Symbol:** ein Vanilla-`Material`. Unbekannt oder fehlend: WARN, Ersatz `PAPER`.
- **Reihenfolge:** Ganzzahl, aufsteigend. Gleiche Werte sortieren nach Id.
- **Obergrenze der Kategorien:** 20 deklarierte. Der Überschuss wird übersprungen (WARN). Mit „Minecraft“ und „Sonstige“ sind es höchstens 22 Einträge, also passt die Übersicht ohne eigene Seiten in 45 Plätze. Diese Obergrenze begrenzt auch die Telemetrie (R10). Sie ist die einzige Zahl dieser Art, weil sie die Oberfläche begrenzt.

Verworfene Alternative: eine Kategorie je Ordner unter `radio/`. Die Id käme dann aus dem Pfad, Namen und Reihenfolge hätten keinen Platz, und es gäbe zwei Stellen für die Konfiguration.

Die Kategorie „Minecraft“ ist eingebaut, mit festem Namen „Minecraft“ in beiden Sprachen, Symbol `MUSIC_DISC_13` und fester Position vorn. Sie enthält genau die 22 Vanilla-Platten in Menüreihenfolge. „Sonstige“ (Deutsch) bzw. „Other“ (Englisch), Symbol `PAPER`, steht hinten und erscheint nur, wenn sie Titel hat.

### R3 Unbekannte und fehlende Kategorie landet in Sonstige

- Ein Song ohne `category` gehört zu „Sonstige“. Die Lobby protokolliert WARN mit Dateiname und dem Hinweis „no category, using other“, je Song einmal beim Start.
- Ein Song mit einer nicht deklarierten Id gehört ebenfalls zu „Sonstige“, mit WARN und der Id.
- Eine Kategorie ohne Titel erscheint nicht in der Übersicht. Das ist kein Fehler und wird nicht protokolliert.
- Der Song wird wegen seiner Kategorie nie übersprungen.

Die Standardkategorie ist fest „Sonstige“. Eine Konfigurationsoption dafür wird nicht gebaut (Entschieden 10).

### R4 Zufall und Weiter bleiben in der Kategorie des laufenden Titels

Der Zufallsmodus und „Weiter“ wählen aus der Kategorie des Titels, der läuft. Läuft keiner, gilt die zuletzt geöffnete Kategorie.

| Modus | Titelende | Weiter |
| --- | --- | --- |
| Einmal | kein Nachfolger, Grund `end` | nächster Titel der Kategorie in Reihenfolge, nach dem letzten der erste |
| Schleife | derselbe Titel, `trigger=auto` | nächster Titel der Kategorie; die Schleife gilt für ihn weiter |
| Zufall | gleichverteilt aus der Kategorie ohne den eben gespielten Titel, `trigger=auto` | gleichverteilt aus der Kategorie ohne den aktuellen Titel |

Eine Kategorie mit einem einzigen Titel wiederholt ihn im Zufallsmodus. Ohne laufenden Titel startet „Weiter“ den ersten Titel der zuletzt geöffneten Kategorie. Ist im Überblick keine Kategorie geöffnet gewesen, ist „Weiter“ ausgegraut.

Begründung:

1. **Die Stimmung bleibt.** Wer „Anime“ startet, hört danach Anime und nicht Halloween.
2. **Das Verhältnis bleibt.** Die 22 Vanilla-Platten machen den größten Teil des Katalogs aus. Eine globale Gleichverteilung würde MIDI-Stücke fast nie spielen lassen.
3. **Das Blättern ändert nichts.** Wer eine andere Kategorie ansieht, ändert den Bereich nicht. Nur ein Start ändert ihn.

Verworfen durch den Owner: Zufall über alle Titel, auch als fünfter Modus („Alle“). Ebenfalls verworfen: eine Kategorie-Auswahl pro Zufallsziehung, weil sie einen Zustand mehr in der Steuerzeile verlangt.

### R5 Keine Grenzen, verzögertes Laden und ein kleiner Cache

**Keine Inhaltsgrenzen.** Es gibt keine Obergrenze für Titelzahl, Dateigröße, Noten oder Länge. Die Grenzen aus `lobby-radio`, D8, entfallen. Die einzige verbleibende Grenze ist die Paketgrenze, und die ist Spielqualität, keine Inhaltsgrenze:

- **16 Noten je Tick und Spieler** bleiben. Sie begrenzen Sound-Pakete je Spieler und Tick (höchstens 320 je Sekunde). Eine Datei mit mehr Noten gleichzeitig spielt die 16 lautesten und verwirft den Rest (`titan.radio.notes.dropped`). Die Grenze schützt die Verbindung und die Client-Leistung je Spieler; dichte Stücke klingen dann lückenhafter, statt die Leitung zu überlasten. Sie begrenzt nicht, wie groß eine Datei sein darf.

**Beim Start nur Metadaten.** `SongCatalog` liest `songs.yaml` und prüft je Eintrag:

- Pflichtfelder, Platte, Id-Eindeutigkeit, Kategorie (R2, R3).
- Die Datei ist eine reguläre Datei unter `radio/`.
- Der Kopf der Datei ist ein MIDI-Kopf (`MThd`, die ersten vier Bytes). Dafür wird nur der Kopf gelesen.

Keine Noten, keine Länge, keine Sequenz. Der Speicher beim Start ist damit die Metadaten, also wenige Kilobyte für 200 Einträge.

**Noten beim Start eines Titels.** Ein MIDI-Titel wird beim Start geladen: der Parser (`MidiSongParser`) liest die Datei und liefert die Notenliste. Das passiert nicht auf dem Tick-Thread (R5, Threading unten).

**Cache.** Die Lobby hält die Notenlisten in einem `SongNotesCache`:

- Jeder laufende Titel hat einen Halt (Lease). Ein Halt wird beim Start geholt und beim Ende, Stopp, Weiter oder Abmelden freigegeben. Ein laufender Titel wird nie entfernt, auch nicht bei vollem Cache.
- Freigegebene Titel bleiben als Leerlauf-Einträge in einer LRU-Liste. **Größe: 8.** Ein Schleifen- oder Zufallsabend in einer Kategorie mit einer Seite pendelt zwischen wenigen Titeln, und 8 Plätze decken das ab. Ein Schleifen-Neustart trifft damit immer den Cache, weil der eben beendete Titel gerade freigegeben wurde.
- Der Speicher pro Titel ist bei typischen Stücken (2 000 bis 5 000 Noten, Schätzung) etwa 65 KB bis 160 KB bei rund 32 Byte je Note. Acht Leerlauf-Einträge kosten damit etwa 1 MB. Ein extrem großes Stück belegt entsprechend mehr. Die Obergrenze wirkt über die Anzahl, nicht über das Gewicht. Falls die Speichermessung (Aufgabe 10.4) bei 256 MB Heap unruhig wird, ist ein Cache nach Notenzahl die nächste Stufe (Offen 2).
- **Fehlgeschlagene Dateien** werden als Leerlauf-Eintrag gemerkt, damit ein wiederholter Klick die Datei nicht erneut parst. Der Eintrag verschwindet mit dem Neustart oder wenn er aus der LRU fällt.
- **Dedup:** Zwei Spieler, die denselben Titel gleichzeitig starten, lösen nur einen Parse aus. Beide bekommen denselben Halt.

**Threading** (5 Zeilen):

1. Ein Klick oder ein Titelende auf dem Tick-Thread ruft `cache.acquire(song)` auf. Ein Treffer liefert die Noten sofort, ein Fehlgriff startet den Parse auf dem IO-Executor und setzt den Spieler in den Zustand „lädt“ (Aktionsleiste „Lädt …“).
2. Der Parse läuft auf dem IO-Executor, also virtuellen Threads, nie auf dem Tick-Thread.
3. Das Ergebnis geht über `scheduler.scheduleNextTick` zurück auf den Tick-Thread. Dort prüft `RadioModule` die Generation der Sitzung und ob der Spieler noch online ist, dann startet die Wiedergabe oder verwirft das Ergebnis und gibt den Halt frei.
4. Der Cache-Zustand (Map, LRU, Halte) liegt unter einem Lock; der Parse läuft außerhalb davon.
5. Tests injizieren zwei Executors: `Runnable::run` für IO und eine Warteschlange für den Tick-Übergang, die der Test explizit leert. Damit ist jede Reihenfolge deterministisch.

**Lebenszyklus:** Ein Stopp, ein Weiter oder ein Abmelden während des Ladens erhöht die Generation und gibt den Halt sofort frei. Das Ergebnis des Parse-Laufs kommt später an und wird verworfen. `@PreDestroy` fährt den IO-Executor herunter und gibt alle Halte frei.

### R6 Aufbau des Menüs: ein Inventar je Spieler, in place aktualisiert

Das ist die zweite Kernentscheidung. Vier Wege standen zur Wahl:

| Option | Inventare | Kosten | Modus- und Lautstärke-Klick | Entscheidung |
| --- | --- | --- | --- | --- |
| A. Geteilt für jeden Zustand | (1 + Seiten) × 3 Modi × 4 Lautstärken × 2 Sprachen: rund 340 typisch (sechs Kategorien mit je zwei Seiten), rund 550 an der Obergrenze (22 Kategorien mit je einer Seite) | Aufbau beim Start und bei jedem Neuladen | öffnet ein neues Fenster, bei jedem Lautstärkeschritt ein Flackern | verworfen |
| B. Geteilt, Zustand als Item-Daten | wie A, mit Seiten | wie A | nicht möglich: geteilte Items sehen für alle gleich aus (`lobby-radio`, D14) | verworfen |
| C. Ein Inventar je Spieler, beim Öffnen gebaut, in place aktualisiert | eines je offenem Fenster | je Öffnen ein Builder mit 54 Plätzen; je Klick eine Neuberechnung der Datenebene und ein Fensterupdate | kein Neuöffnen | **gewählt** |
| D. Aves `PageableInventory` | eines je Spieler | wie C | nur Zurück und Weiter, experimentell, nicht übersetzt | verworfen; die Seitenlogik ist eine eigene reine Klasse (R7) |

Mit vier Stufen statt der zuerst geplanten zehn schrumpft A, bleibt aber verworfen, weil der Klick ein Neuöffnen auslöst.

**Kosten von C, ehrlich:**

- Je Öffnen entsteht ein Builder und ein Inventar mit 54 Plätzen. Nach Schätzung ein Bruchteil eines Ticks (50 ms); die Messung ist Aufgabe 15.2.
- Je Klick berechnet Aves die Datenebene neu, und Minestom aktualisiert das Fenster. Minestom schickt dabei das ganze Fenster mit 54 Plätzen. Bei menschlicher Klickrate ist das unkritisch.
- Der Speicher wächst nur mit offenen Fenstern, nicht mit der Zahl der Zustände.
- Der Lebenszyklus verlangt `register()` beim Öffnen und `unregister()` beim Schließen. `unregister()` schließt die Betrachter; die Ansicht muss deshalb vorher aus der Map entfernt werden, sonst gibt es eine Rekursion (Test).

**Abweichung von der Projektregel „ein Builder pro Modul“:** Ein Builder je Fenster ist die einzige Form, in der ein Spielerstand in einem Aves-Inventar liegen kann. Die Regel bleibt für die Definition: `RadioMenuBuilder` ist die einzige Klasse, die das Layout beschreibt, und sie erzeugt je Fenster einen Builder. `RadioViews` hält die offenen Fenster.

**Öffnen, aktualisieren, schließen:**

1. **Öffnen** (Hotbar-Klick): Die Sitzung setzt Ansicht auf Übersicht und Seite auf 1. Der Modus bleibt. `RadioViews.open` baut einen `PersonalTranslatedInventoryBuilder` mit einer Datenebenen-Funktion, die Sitzung und Lautstärke liest, ruft `register()` und `open(locale)` auf und merkt sich die Ansicht. Die Sprache ist die des Clients.
2. **Klick im Fenster:** Der Modul-Listener auf `InventoryPreClickEvent` bricht das Ereignis ab und ordnet es über die Inventar-Identität der Ansicht zu. Die Steuerung ändert Sitzung oder Einstellung, dann ruft die Ansicht `invalidateDataLayout()`.
3. **Seitenwechsel und Kategorie:** dasselbe wie Klick. Der Fenstertitel („Radio · Spiele · 1/2“) soll mit wechseln. Ob Aves das für übersetzte Builder in place kann, prüft Aufgabe 1.1. Wenn nicht, öffnet genau dieser Wechsel das Fenster neu.
4. **Tick:** Startet, endet oder lädt ein Titel, und hat der Spieler ein Fenster offen, aktualisiert der Tick das Fenster. „Läuft jetzt“ und der Glanz bleiben so aktuell.
5. **Schließen** (`InventoryCloseEvent` oder Abmelden): Die Ansicht wird aus der Map entfernt, dann `unregister()`. Die Sitzung bleibt bis zum Abmelden.
6. **Herunterfahren** (`@PreDestroy`): alle offenen Ansichten werden entfernt und unregistriert.

Aves füllt Inhalte auf dem nächsten Tick. Das Fenster ist also für einen Tick leer, wie schon in `lobby-radio`.

### R7 Seiten: 45 Titel je Seite, feste Plätze, Seite je Spieler

- **Layout:** 6 Reihen, 54 Plätze. Plätze 0 bis 44 sind der Inhalt (45 je Seite). Plätze 45 bis 53 sind die Steuerzeile (R8). Die Steuerzeile verschiebt sich nie.
- **Seitenzahl:** `max(1, ceil(n / 45))`. Ein Titel an Index `i` in der Kategorie steht auf Seite `i / 45 + 1` an Platz `i % 45`.
- **Reihenfolge:** Konfigurationsreihenfolge innerhalb der Kategorie. „Minecraft“ folgt der Menüreihenfolge. Ein Platz ändert sich nur beim Neuladen beim Start.
- **Leere Plätze:** Füllglas (`GRAY_STAINED_GLASS_PANE`, ohne Name).
- **Seite je Spieler:** Die Seite gehört zur Sitzung. Sie bleibt beim Blättern, Stopp, Weiter, Modus und Lautstärke. Sie geht auf 1 zurück, wenn der Spieler eine Kategorie öffnet oder das Radio neu öffnet.
- **Klammer:** Eine Seite über der letzten wird auf die letzte geklemmt.
- **Übersicht:** nicht seitenweise, weil sie höchstens 22 Einträge hat.

Die Seitenlogik ist eine reine Klasse ohne Aves und ohne Minestom.

### R8 Steuerzeile, Übersicht und Mock-ups

Die untere Reihe hat neun feste Plätze. Entfällt ein Knopf, bleibt sein Platz als Füllglas.

| Platz | Element | Material | Klick |
| --- | --- | --- | --- |
| 45 | Zurück | `OAK_DOOR` | Kategorieseite → Übersicht; in der Übersicht Füllglas |
| 46 | Seite zurück | `SPECTRAL_ARROW` | eine Seite zurück; auf Seite 1 Füllglas |
| 47 | Seitenanzeige | `BOOK` | keine; Name „Seite 1 von 2“, in der Übersicht „Kategorien: 5“ |
| 48 | Stopp | `BARRIER` | stoppt mit Grund `user` |
| 49 | Läuft jetzt | Schallplatte des laufenden Titels, sonst `JUKEBOX` | keine Aktion |
| 50 | Weiter | `ARROW` | R4; ohne laufenden Titel und ohne vorher geöffnete Kategorie Füllglas |
| 51 | Modus | `REPEATER` | zyklisch Einmal → Schleife → Zufall |
| 52 | Lautstärke | `NOTE_BLOCK` | Linksklick lauter, Rechtsklick leiser; Stufen 25, 50, 75, 100 (R9) |
| 53 | Seite weiter | `SPECTRAL_ARROW` | eine Seite vor; auf der letzten Seite Füllglas |

Inhalt auf Plätzen 0 bis 44: in der Übersicht ein Symbol je Kategorie, das die Kategorie öffnet. Auf einer Kategorieseite eine Schallplatte je Titel, deren Klick den Titel startet. Der Titel, der für diesen Spieler läuft, glänzt (R12). Ein Titel, der gerade lädt, zeigt in der Lore „Lädt …“.

**„Läuft jetzt“** (Platz 49) zeigt den Titel als Name und in der Lore Künstler, Kategorie, Modus und Lautstärke. Beispiel:

```
Läuft: Sunrise
  Künstler: Beispiel
  Kategorie: Spiele
  Modus: Schleife
  Lautstärke: 75 %
```

Ohne laufenden Titel: „Kein Titel läuft“, Modus und Lautstärke. Spielt eine Schallplatte mit einer anderen Lautstärke als der jetzt eingestellten, steht zusätzlich „Lautstärke wirkt ab dem nächsten Titel“ (R9).

**Mock-up der Übersicht (6 Reihen, Kategorien ab Platz 10, sieben je Reihe).** Die Zahlen sind die Plätze. `.` ist Füllglas. Kürzel: `Mc` Minecraft, `Sp` Spiele, `An` Anime, `Kl` Klassik, `Ha` Halloween, `Kat` Kategorienzahl, `St` Stopp, `Ls` Läuft jetzt, `Wt` Weiter, `Mo` Modus, `Lv` Lautstärke.

```
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  9  | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  |
|  .  | Mc  | Sp  | An  | Kl  | Ha  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 27  | 28  | 29  | 30  | 31  | 32  | 33  | 34  | 35  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 36  | 37  | 38  | 39  | 40  | 41  | 42  | 43  | 44  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 45  | 46  | 47  | 48  | 49  | 50  | 51  | 52  | 53  |
|  .  |  .  | Kat | St  | Ls  | Wt  | Mo  | Lv  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
```

Die Übersicht zeigt fünf Kategorien (Minecraft, Spiele, Anime, Klassik, Halloween). Kommt „Sonstige“ dazu, steht sie auf Platz 15. Die Plätze 45 und 46 sind Füllglas, weil es in der Übersicht kein Zurück und keine Seite gibt. Platz 50 („Weiter“) ist mit laufendem Titel gezeigt; ohne Titel und ohne vorher geöffnete Kategorie wird er Füllglas.

**Mock-up einer Kategorieseite (6 Reihen, „Spiele“, Seite 1 von 2, 14 Titel).** `S1` bis `S14` sind die Titel in Reihenfolge; `S3` glänzt, weil er läuft. Dieselben Kürzel wie oben, `Zu` ist Zurück, `>` Seite weiter.

```
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |
| S1  | S2  | S3* | S4  | S5  | S6  | S7  | S8  | S9  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  9  | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  |
| S10 | S11 | S12 | S13 | S14 |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 27  | 28  | 29  | 30  | 31  | 32  | 33  | 34  | 35  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 36  | 37  | 38  | 39  | 40  | 41  | 42  | 43  | 44  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 45  | 46  | 47  | 48  | 49  | 50  | 51  | 52  | 53  |
| Zu  |  .  | 1/2 | St  | Ls  | Wt  | Mo  | Lv  |  >  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
```

Auf Seite 2 wird Platz 46 zu `<` (aktiv) und Platz 53 zu Füllglas, sobald es keine weitere Seite gibt. Die Steuerzeile bleibt an denselben Plätzen.

### R9 Lautstärke: gespeicherte Spielereinstellung in Otis

**Wert:** Stufen 25, 50, 75 und 100 Prozent, Standard 100. Kein 0: Stopp deckt das ab, und ein stummes Radio, das weiter Pakete schickt, wäre verwirrend. Linksklick erhöht um eine Stufe, Rechtsklick senkt um eine Stufe. An den Rändern bleibt der Wert stehen, statt umzulaufen: ein Umlauf würde einen Linksklick bei 100 Prozent als plötzlich leise Stufe zeigen.

**Speicherort:** Otis, über die bestehende Spielereinstellung (`PlayerSettings`). Schlüssel `titan:radio.volume`, Wert eine JSON-Zahl (25, 50, 75 oder 100). Die Lautstärke gilt über den Logout hinaus. Das ändert `lobby-radio`, D1 für diesen Wert, und ist die Entscheidung des Owners.

**Otis-Seite (Prüfung):** Die Einstellungs-API auf `origin/main` (PR #181) speichert den Wert ohne Auslegung. Erlaubt ist jeder JSON-Wert bis 64 KiB, die Antwort enthält `key`, `value`, `version` und `updatedAt`. Eine Zahl ist damit ohne Änderung an Otis speicherbar. Otis kennt keine Standardwerte und keine Schlüsselregistrierung; die Lobby legt die Stufen fest.

**Titan-Seite (das eigentliche Hindernis):** Die Lobby kann heute nur Schalter speichern:

- `OtisJson.booleanOf` liest nur Wahrheitswerte. Ein Eintrag mit einer Zahl wird verworfen.
- `OtisJson.valueBody(boolean)` schreibt nur Wahrheitswerte.
- `SettingsStore`, `WriteBehind`, `PlayerSettingsService`, `InMemorySettingsStore` und `SettingChangeListener` haben einen booleschen Wert.
- `SettingsRegistry` nimmt nur `BooleanSetting`.

**Kleinste Titan-Änderung (Gruppe 2 in `tasks.md`):**

1. In `core/settings` ein kleiner Werttyp `SettingValue` mit zwei Fällen: `Flag(boolean)` und `Level(int)`. Ein `SettingsStore` nimmt und liefert `SettingValue`. Die booleschen Aufrufer übergeben `Flag`.
2. Ein neues Record `LevelSetting(SettingKey key, List<Integer> levels, int defaultLevel)`. Es prüft, dass der Standard in der Liste steht.
3. `PlayerSettings` erhält `int level(UUID, LevelSetting)` und `void setLevel(UUID, LevelSetting, int)`. Ein Wert außerhalb der Liste ist ein Programmierfehler und wirft.
4. `SettingsRegistry` erhält `registerLevel(owner, LevelSetting)` in einer zweiten Map. Die Menü-Liste `definitions()` bleibt boolesch, damit die Zeile der Einstellungen unberührt bleibt. Eine Schlüsselkollision über beide Arten wird abgelehnt.
5. `OtisJson` liest ganze Zahlen zusätzlich zu Wahrheitswerten und schreibt `{"value": 75}`.
6. Beim Laden prüft `SettingsModule`, ob eine geladene Zahl in der Stufenliste steht. Sonst bleibt der Standard, und es gibt eine WARN-Zeile mit dem Schlüssel, nicht mit dem Wert.

Das Laden, das Schreiben und der Offline-Fall bleiben wie bei Schaltern (`lobby-settings`):

- Der Beitritt wartet nicht auf Otis. Bis das Laden fertig ist, gilt der Standard 100.
- Eine lokale Änderung vor dem Laden gewinnt: die Ladung überschreibt einen angefassten Wert nicht (`touched`, wie bei Schaltern).
- Schreiben ist verzögert, mit Wiederholung und Circuit Breaker. Ohne `otis.url` bleibt der Wert im Speicher.
- Ist Otis nicht erreichbar, bleibt die zuletzt bekannte Stufe im Prozess erhalten, und der Start scheitert nie.

**Nicht im Einstellungsmenü.** Die Lobby-Einstellungen sind eine Zeile mit höchstens 9 Schaltern, und `lobby-settings` baut die Inventare aus allen Zustandskombinationen (2 · 2^n). Vier Stufen würden die Kombinationen verdoppeln, bei gleicher Bedienung. Außerdem hört der Spieler die Lautstärke im Radio. Der Knopf steht deshalb nur im Radio (Entschieden 6).

**Vanilla und MIDI** (unverändert zu der Lösung mit Sitzung):

- **MIDI:** Die Lautstärke jeder Note ist `velocity / 127 × Stufe / 100`. Sie wirkt ab der nächsten Note, also sofort. Die Rangfolge für das Budget von 16 Noten je Tick bleibt gleich.
- **Vanilla:** Die Schallplatte startet mit der Stufe, die im Startmoment gilt. Eine Änderung während des Laufs sendet nichts, weil der Client keine Lautstärke während eines Sounds ändern lässt. Ein Neustart mit neuer Lautstärke wurde verworfen: der Client startet die Datei von vorn, die Länge läuft neu, und das Titelende springt. Die Anzeige „Läuft jetzt“ sagt das im Lore.

**Klangebene:** `Sound.Source.RECORD` folgt zusätzlich dem Platten-Regler des Spielers im Client. Die Lobby-Stufe multipliziert sich damit.

### R10 Telemetrie

- `titan.radio.plays{kind, mode, trigger, category}`. `category` ist die Id: `minecraft` für Vanilla, `other` für Sonstige, sonst die deklarierte Id. Die Kardinalität ist bis 2 × 3 × 2 × 22 = 264 Reihen, weil die Obergrenze von 20 Kategorien (R2) die Menge begrenzt.
- `titan.radio.stops{reason, kind}` bleibt unverändert. Eine Kategorie dort würde jeden Start doppelt zählen.
- `titan.radio.songs{result}` misst den Start-Check: `loaded`, wenn Metadaten und MIDI-Kopf passen, `rejected`, wenn nicht. Zusätzlich zählt `rejected` einen Parse-Fehler beim Abspielen, einmal je Ladevorgang. Ein erfolgreicher Parse beim Abspielen zählt nicht, sonst würde jeder Titelstart den Zähler erhöhen.
- `titan.radio.notes.dropped` unverändert.
- Lautstärkeänderungen zählen nicht im Telemetrie-Zähler `titan.settings.toggled`, weil das eine Umschaltung ist (Schalter). Eine eigene Zählung für Stufen ist nicht Teil dieser Änderung.
- Kein Attribut mit Spieler-Id.
- Der Start protokolliert INFO mit Anzahl Kategorien und Titeln.

### R11 Migration

- **`songs.yaml` ohne `category` und ohne `categories`:** gültig. Alle Titel landen in „Sonstige“. Je Titel ein WARN beim Start. Die Übersicht zeigt „Minecraft“ und „Sonstige“.
- **Einträge mit `id, title, artist, disc, file`:** bleiben gültig.
- **Mehr als 18 Titel:** bisher abgelehnt, jetzt ohne Grenze (R5).
- **Große Dateien:** bisher über 512 KiB abgelehnt, jetzt angenommen. Wer eine solche Datei hat, bekommt sie beim Start als Metadatum geprüft und beim Abspielen geladen.
- **Hotbar-Item:** öffnet die Übersicht statt der Vanilla-Liste. Das ist die sichtbare Änderung und steht im PR und in `docs/lobby-modules.md`.
- **Zurückrollen:** Die alte Version liest nur `songs` und die bekannten Felder. Die neuen Felder `categories` und `category` ignoriert sie. Mehr als 18 Titel lehnt sie ab. Eine Rückrollung ist ohne Datenänderung möglich.
- **Gespeicherte Lautstärke:** Vorhandene Spieler ohne gespeicherten Wert erhalten den Standard 100. Der Wert kommt erst mit dem ersten Verstellen in Otis an.
- **Otis:** keine Schema- oder Konfigurationsänderung in Otis.

## Test-Strategie

Alle Tests folgen F.I.R.S.T. (`openspec/config.yaml`, Titan-Regeln):

- **Rein (Unit, ohne Server):** `SongCatalogTest` mit `@TempDir` und Logback-`ListAppender` für WARN (Kategorien, Reservierung, Sonstige, Symbolfehler, Kopfprüfung, Duplikate). `RadioCategoriesTest` (Reihenfolge, leere Kategorie, Sonstige hinten). `PageLayoutTest` (Seiten, Plätze, Steuerzeile, Klammer). `RadioSelectionTest` mit `java.util.Random(seed)` (Bereich je Modus, Wrap, eine Kategorie mit einem Titel, leere Kategorie). `SongNotesCacheTest` mit inline-Executors (Halt, LRU mit 8 Plätzen, laufende Titel nie entfernt, Dedup gleichzeitiger Starts, Fehler-Eintrag, Freigabe ist idempotent). `RadioLoadTest` mit Warteschlange für den Tick-Übergang (Generation verwirft Ergebnis, Abmelden während des Ladens, Stopp während des Ladens). `MidiSongParserTest` mit einer großen synthetischen Sequenz (keine Grenze mehr). `LevelSettingTest` (Standard in der Liste, Schritte, Ränder). `VolumeTest` (Stufen, Rechts- und Linksklick, Ränder bleiben). `NoteMapperTest`, `RadioPlaybackTest` und `VanillaDiscPlaybackTest` mit der Lautstärke. `RadioSessionTest` (Seite bleibt beim Blättern, Kategoriewechsel setzt Seite 1, Modus bleibt).
- **Spielereinstellung (`core`, `features/settings`, `platform/otis`):** `SettingValueTest`, `SettingsRegistryTest` (Kollision über beide Arten), `PlayerSettingsServiceTest` (Stufe setzen, Laden, lokale Änderung gewinnt, unbekannte Zahl bleibt Standard), `OtisJsonTest` (Zahl lesen, Zahl schreiben, Text und Gleitkomma werden verworfen), `OtisSettingsStoreIntegrationTest` mit `FakeOtis` (Körper `{"value":75}`).
- **Telemetrie und Meldungen:** `RadioTelemetryTest` prüft `category` bei `plays`, keines bei `stops`, keine Spieler-Id. `songs{result=rejected}` beim Abspielen. `RadioMessagesTest` prüft die neuen Schlüssel in beiden Sprachen.
- **Env (`env.tick()`, Testverbindung):** `RadioMenuIntegrationTest`: Hotbar öffnet die Übersicht, Klick auf Kategorie, Blättern, Zurück, Modusklick und Lautstärkeklick aktualisieren dasselbe Inventar, Schließen entfernt die Ansicht ohne Rekursion, `@PreDestroy` schließt offene Fenster. Lautstärke wird über den Port geschrieben und überlebt einen Logout. Ein Parse-Fehler im Abspielen sendet die Meldung und zählt `rejected`. Der laufende Titel trägt den Glanz, andere nicht, und nach dem Stopp ist der Glanz weg.
- **Glanz (Unit und Env):** `RadioGlintTest` prüft, dass `DataComponents.ENCHANTMENT_GLINT_OVERRIDE` im Item des laufenden Titels gesetzt ist, auch nach `TranslatedItem.get(locale)`.
- **Architektur:** `ColumnArchitectureTest` wie bisher. `features/radio` hängt weiter nur von `core` ab.
- **Keine** Sleeps, keine Systemzeit, keine geteilten statischen Zustände, Zufall nur mit festem Seed, Executors inline.

## Offene Fragen

Die Liste steht in `proposal.md`, Offen.
